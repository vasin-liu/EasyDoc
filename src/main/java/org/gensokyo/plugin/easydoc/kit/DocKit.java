/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.deepoove.poi.plugin.toc.TOCRenderPolicy;
import com.intellij.openapi.diagnostic.Logger;
import org.gensokyo.plugin.easydoc.dto.DocOptions;
import org.gensokyo.plugin.easydoc.dto.DataSourceDTO;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.dto.TableDTO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 文档生成工具
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/14 , Version 1.0.0
 */
public class DocKit {
    private static final Logger LOG = Logger.getInstance(DocKit.class);

    private DocKit() {
        throw new UnsupportedOperationException();
    }

    public static boolean create(DocOptions opts) {
        Configure config = buildConfigure();

        byte[] templateBytes;
        try (var stream = Objects.requireNonNull(opts.template())) {
            // 分块渲染时需要重复编译模板，因此先把模板读到内存
            templateBytes = stream.readAllBytes();
        } catch (Exception e) {
            LOG.error("读取模板失败", e);
            return false;
        }

        int totalTables = 0;
        for (NamespaceDTO ns : opts.namespaces()) {
            totalTables += ns.getTables().size();
        }

        // 小数据量走原逻辑（避免额外分文件带来格式/目录变化）
        final int MAX_TABLES_PER_PART = 15;
        if (totalTables <= MAX_TABLES_PER_PART) {
            return renderSingle(templateBytes, config, opts);
        }

        // 大数据量兜底：分块渲染，避免 poi-tl/POI 在一次渲染时占用过高堆内存
        LOG.warn("表/视图数量过多（totalTables=" + totalTables + "），改为分块渲染，防止 Java heap space");
        return renderInParts(templateBytes, config, opts);
    }

    private static Configure buildConfigure() {
        return Configure.builder()
                .bind("ds4table", new LoopRowTableRenderPolicy())
                .bind("ns4table", new LoopRowTableRenderPolicy())
                .bind("ds4tableTables", new LoopRowTableRenderPolicy())
                .bind("ns4tableTables", new LoopRowTableRenderPolicy())
                .bind("ds4tableViews", new LoopRowTableRenderPolicy())
                .bind("ns4tableViews", new LoopRowTableRenderPolicy())
                .bind("ds4tableMaterializedViews", new LoopRowTableRenderPolicy())
                .bind("ns4tableMaterializedViews", new LoopRowTableRenderPolicy())
                .bind("columns", new LoopRowTableRenderPolicy())
                .bind("toc", new TOCRenderPolicy())
                .useSpringEL()
                .setValidErrorHandler(handler -> LOG.error("值校验错误：" + handler.getData()))
                .build();
    }

    private static boolean renderSingle(byte[] templateBytes, Configure config, DocOptions opts) {
        try (var stream = new ByteArrayInputStream(templateBytes);
             var template = XWPFTemplate.compile(stream, config)) {
            template.render(opts.toMap()).writeToFile(opts.savePath());
            return true;
        } catch (Exception e) {
            LOG.error("生成文档失败: " + opts.savePath(), e);
            return false;
        }
    }

    private static boolean renderInParts(
            byte[] templateBytes,
            Configure config,
            DocOptions opts
    ) {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("easydoc-doc-parts-");
        } catch (IOException e) {
            LOG.error("创建分片临时目录失败", e);
            return false;
        }
        try {
        LOG.info("EasyDoc 分片临时目录: " + tempDir.toAbsolutePath());

        // 按 namespace 边界切块：避免同一章节（同一 Namespace）被拆到多个 part 里导致重复章节标题
        List<List<NamespaceDTO>> parts = new ArrayList<>();
        List<NamespaceDTO> currentPart = new ArrayList<>();
        int currentCount = 0;
        for (NamespaceDTO ns : opts.namespaces()) {
            int nsTableCount = ns.getTables() == null ? 0 : ns.getTables().size();
            if (nsTableCount == 0) {
                continue;
            }
            if (currentCount > 0 && currentCount + nsTableCount > 15) {
                parts.add(currentPart);
                currentPart = new ArrayList<>();
                currentCount = 0;
            }
            currentPart.add(ns);
            currentCount += nsTableCount;
        }
        if (!currentPart.isEmpty()) {
            parts.add(currentPart);
        }

        int partIndex = 0;
        List<String> partDocPaths = new ArrayList<>();
        List<DocOptions> partJobs = new ArrayList<>();
        for (List<NamespaceDTO> partNamespaces : parts) {
            partIndex++;

            // part 内包含哪些 tables（按 namespace 分组）
            IdentityHashMap<NamespaceDTO, List<TableDTO>> tablesByNs = new IdentityHashMap<>();
            for (NamespaceDTO ns : partNamespaces) {
                tablesByNs.put(ns, ns.getTables());
            }

            // 构造 part namespaces（用新的 NamespaceDTO 包一层，只替换 tables 列表）
            IdentityHashMap<NamespaceDTO, NamespaceDTO> partNsByOrigin = new IdentityHashMap<>();
            for (var entry : tablesByNs.entrySet()) {
                NamespaceDTO origin = entry.getKey();
                NamespaceDTO partNs = new NamespaceDTO();
                partNs.setName(origin.getName());
                partNs.setComment(origin.getComment());
                partNs.setTables(entry.getValue());
                partNsByOrigin.put(origin, partNs);
            }

            // 构造 part dataSources：保留原 dataSource 的其它字段，只替换 namespaces 子集
            List<DataSourceDTO> partDataSources = new ArrayList<>();
            Collection<DataSourceDTO> optSources = opts.dataSources();
            if (optSources == null) {
                continue;
            }
            for (DataSourceDTO ds : optSources) {
                List<NamespaceDTO> partNamespacesForDs = ds.getNamespaces().stream()
                        .filter(partNsByOrigin::containsKey)
                        .map(partNsByOrigin::get)
                        .collect(Collectors.toList());
                if (!partNamespacesForDs.isEmpty()) {
                    DataSourceDTO partDs = new DataSourceDTO();
                    partDs.setName(ds.getName());
                    partDs.setComment(ds.getComment());
                    partDs.setDialect(ds.getDialect());
                    partDs.setVersion(ds.getVersion());
                    partDs.setNamespaces(partNamespacesForDs);
                    partDataSources.add(partDs);
                }
            }

            Path partFile = tempDir.resolve(String.format("part_%02d.docx", partIndex));
            String partSavePath = partFile.toAbsolutePath().toString();
            partDocPaths.add(partSavePath);

            DocOptions partOpts = DocOptions.of()
                    .template(null)
                    .title(opts.title())
                    .author(opts.author())
                    .version(opts.version())
                    .savePath(partSavePath)
                    .dataSources(partDataSources)
                    .namespaces(
                            partNamespaces.stream()
                                    .filter(partNsByOrigin::containsKey)
                                    .map(partNsByOrigin::get)
                                    .collect(Collectors.toList())
                    );
            partJobs.add(partOpts);
        }

        if (partJobs.isEmpty()) {
            LOG.error("分片任务为空，无法生成文档");
            return false;
        }

        int nThreads = Math.min(partJobs.size(), Math.max(2, Runtime.getRuntime().availableProcessors()));
        ExecutorService pool = Executors.newFixedThreadPool(nThreads, r -> {
            Thread t = new Thread(r, "easydoc-doc-part");
            t.setDaemon(true);
            return t;
        });
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (DocOptions partOpts : partJobs) {
                tasks.add(() -> {
                    LOG.info("开始生成分片: " + partOpts.savePath());
                    return renderSingle(templateBytes, config, partOpts);
                });
            }
            List<Future<Boolean>> results = pool.invokeAll(tasks);
            for (Future<Boolean> f : results) {
                if (!f.get()) {
                    return false;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("分片渲染被中断", e);
            return false;
        } catch (ExecutionException e) {
            LOG.error("分片渲染失败", e.getCause());
            return false;
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(60, TimeUnit.MINUTES)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        String finalSavePath = opts.savePath();
        return mergeDocxParts(finalSavePath, partDocPaths);
        } finally {
            deleteTempDirQuietly(tempDir);
        }
    }

    private static void deleteTempDirQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    LOG.warn("删除临时文件失败: " + p, e);
                }
            });
        } catch (IOException e) {
            LOG.warn("清理临时目录失败: " + dir, e);
        }
    }

    /**
     * 将多个 docx 通过合并 word/document.xml 的 w:body 子节点拼成一个 docx。
     * 注意：该方法假设所有 part 来自同一个模板，且不会引入新的关系/资源（images/rel）
     * 否则可能需要更复杂的 OPC 合并。
     */
    private static boolean mergeDocxParts(String finalSavePath, List<String> partDocPaths) {
        if (finalSavePath == null || finalSavePath.isBlank()) {
            return false;
        }
        if (partDocPaths == null || partDocPaths.isEmpty()) {
            return false;
        }
        try {
            File outFile = new File(finalSavePath);
            if (outFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                outFile.delete();
            }

            // 使用第一个 part 作为“模板容器”，只替换 word/document.xml
            try (ZipFile base = new ZipFile(partDocPaths.get(0));
                 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(finalSavePath))) {
                byte[] mergedDocumentXml = buildMergedDocumentXml(partDocPaths);

                var entries = base.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if ("word/document.xml".equals(name)) {
                        continue; // skip, we will write merged content later
                    }
                    zos.putNextEntry(new ZipEntry(name));
                    try (InputStream in = base.getInputStream(entry)) {
                        in.transferTo(zos);
                    }
                    zos.closeEntry();
                }

                ZipEntry docEntry = new ZipEntry("word/document.xml");
                zos.putNextEntry(docEntry);
                zos.write(mergedDocumentXml);
                zos.closeEntry();
                zos.finish();
            }
            return true;
        } catch (Exception e) {
            LOG.error("docx 合并失败", e);
            return false;
        }
    }

    private static byte[] buildMergedDocumentXml(List<String> partDocPaths) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        Document baseDoc;
        Element baseBody;

        // 先解析第一个 part，作为 merged document 的容器；后续 part 只追加“非重复前缀”的部分
        try (ZipFile baseZip = new ZipFile(partDocPaths.get(0))) {
            byte[] firstDocXml = readZipEntryBytes(baseZip);
            baseDoc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(firstDocXml));
            baseBody = findWBody(baseDoc);
        }

        // 若有多个 part，去掉第一个 part 尾部的 sectPr（最后一个 part 会补上），避免章节/编号因 section 重新开始
        if (partDocPaths.size() > 1) {
            NodeList baseChildren = baseBody.getChildNodes();
            List<Node> toRemove = new ArrayList<>();
            for (int i = 0; i < baseChildren.getLength(); i++) {
                Node child = baseChildren.item(i);
                if (isWSectPr(child)) {
                    toRemove.add(child);
                }
            }
            for (Node n : toRemove) {
                baseBody.removeChild(n);
            }
        }

        for (int i = 1; i < partDocPaths.size(); i++) {
            boolean last = i == partDocPaths.size() - 1;
            try (ZipFile z = new ZipFile(partDocPaths.get(i))) {
                byte[] docXml = readZipEntryBytes(z);
                Document partDoc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(docXml));
                Element partBody = findWBody(partDoc);

                NodeList baseChildren = baseBody.getChildNodes();
                NodeList partChildren = partBody.getChildNodes();

                int baseLen = baseChildren.getLength();
                int partLen = partChildren.getLength();
                int commonPrefixLen = 0;

                // 找到 part 的“共同前缀”长度：假设前置内容结构相同
                while (commonPrefixLen < baseLen && commonPrefixLen < partLen) {
                    Node baseChild = baseChildren.item(commonPrefixLen);
                    Node partChild = partChildren.item(commonPrefixLen);
                    if (!sameNodeSignature(baseChild, partChild)) {
                        break;
                    }
                    commonPrefixLen++;
                }

                // 追加共同前缀之后的内容
                for (int c = commonPrefixLen; c < partLen; c++) {
                    Node child = partChildren.item(c);
                    // 后续 part 的 TOC 通常是“基于子集”的目录，合并后会重复，直接跳过
                    if (looksLikeToc(child)) {
                        continue;
                    }
                    if (isWSectPr(child)) {
                        if (last) {
                            baseBody.appendChild(baseDoc.importNode(child, true));
                        }
                        continue;
                    }
                    baseBody.appendChild(baseDoc.importNode(child, true));
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TransformerFactory tf = TransformerFactory.newInstance();
        var transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.transform(new DOMSource(baseDoc), new StreamResult(out));
        return out.toByteArray();
    }

    private static boolean sameNodeSignature(Node a, Node b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getNodeType() != b.getNodeType()) {
            return false;
        }
        if (a.getNodeType() != Node.ELEMENT_NODE) {
            // text nodes 等：用 text content 对齐
            String ta = normalizeSpaces(a.getTextContent());
            String tb = normalizeSpaces(b.getTextContent());
            return Objects.equals(ta, tb);
        }
        if (!Objects.equals(a.getLocalName(), b.getLocalName())) {
            return false;
        }
        String ta = normalizeSpaces(a.getTextContent());
        String tb = normalizeSpaces(b.getTextContent());
        return Objects.equals(ta, tb);
    }

    private static String normalizeSpaces(String s) {
        if (s == null) {
            return "";
        }
        // 快速压缩空白，避免多段空格影响签名
        return s.trim().replaceAll("\\s+", " ");
    }

    private static boolean looksLikeToc(Node node) {
        if (node == null) {
            return false;
        }
        String text = node.getTextContent();
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("toc")
                || lower.contains("table of contents")
                || lower.contains("目录");
    }

    private static byte[] readZipEntryBytes(ZipFile zipFile) throws Exception {
        ZipEntry entry = zipFile.getEntry("word/document.xml");
        if (entry == null) {
            throw new IllegalStateException("Missing zip entry: " + "word/document.xml");
        }
        try (InputStream is = zipFile.getInputStream(entry);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            is.transferTo(bos);
            return bos.toByteArray();
        }
    }

    private static Element findWBody(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "body");
        if (nodes.getLength() == 0) {
            throw new IllegalStateException("Cannot find w:body");
        }
        return (Element) nodes.item(0);
    }

    private static boolean isWSectPr(Node node) {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        return "sectPr".equals(node.getLocalName());
    }
}
