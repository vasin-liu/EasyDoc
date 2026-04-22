/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import com.deepoove.poi.data.TextRenderData;
import com.intellij.database.model.ObjectKind;
import lombok.Data;
import lombok.experimental.Accessors;
import org.gensokyo.plugin.easydoc.kit.DocRenderKit;
import org.gensokyo.plugin.easydoc.plan.DocPlan;
import org.gensokyo.plugin.easydoc.plan.PlanRenderContextBuilder;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档生成配置项
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/14 , Version 1.0.0
 */
@Data
@Accessors(chain = true, fluent = true)
public class DocOptions {

    private InputStream template;
    private String title;
    private String version = "1.0.0";
    private String author = "Gensokyo V.L.";
    private String savePath;
    private Collection<DataSourceDTO> dataSources;
    private Collection<NamespaceDTO> namespaces;
    private DocPlan docPlan;

    // 映射阿拉伯数字到中文数字
    private static final Map<Character, String> CHINESE_NUMBER_MAP = new HashMap<>();

    static {
        CHINESE_NUMBER_MAP.put('0', new String(new byte[]{(byte) 0x81}, java.nio.charset.StandardCharsets.US_ASCII));
        CHINESE_NUMBER_MAP.put('1', "一");
        CHINESE_NUMBER_MAP.put('2', "二");
        CHINESE_NUMBER_MAP.put('3', "三");
        CHINESE_NUMBER_MAP.put('4', "四");
        CHINESE_NUMBER_MAP.put('5', "五");
        CHINESE_NUMBER_MAP.put('6', "六");
        CHINESE_NUMBER_MAP.put('7', "七");
        CHINESE_NUMBER_MAP.put('8', "八");
        CHINESE_NUMBER_MAP.put('9', "九");
    }

    private DocOptions() {

    }

    public static DocOptions of() {
        return new DocOptions();
    }

    public Map<String, Object> toMap() {
        List<DataSourceDTO> dsFull = dataSources == null ? List.of() : List.copyOf(dataSources);
        List<DataSourceDTO> dsTables = DocRenderKit.dataSourcesOfKind(dsFull, ObjectKind.TABLE);
        List<DataSourceDTO> dsViews = DocRenderKit.dataSourcesOfKind(dsFull, ObjectKind.VIEW);
        List<DataSourceDTO> dsMaterialized = DocRenderKit.dataSourcesOfKind(dsFull, ObjectKind.MAT_VIEW);

        List<NamespaceDTO> nsTables = DocRenderKit.flattenNamespaces(dsTables);
        List<NamespaceDTO> nsViews = DocRenderKit.flattenNamespaces(dsViews);
        List<NamespaceDTO> nsMaterialized = DocRenderKit.flattenNamespaces(dsMaterialized);

        List<NamespaceDTO> nsFull = namespaces == null ? List.of() : List.copyOf(namespaces);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("savePath", savePath);
        map.put("title", title);
        map.put("version", version);
        map.put("author", author);
        map.put("toc", "toc");
        map.put("date", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        // 全量（兼容旧模板）
        map.put("dataSources", dsFull);
        map.put("ds4table", dsFull);
        map.put("ns4table", nsFull);
        // 分章节：表
        map.put("dataSourcesTables", dsTables);
        map.put("namespacesTables", nsTables);
        map.put("ds4tableTables", dsTables);
        map.put("ns4tableTables", nsTables);
        // 分章节：视图
        map.put("dataSourcesViews", dsViews);
        map.put("namespacesViews", nsViews);
        map.put("ds4tableViews", dsViews);
        map.put("ns4tableViews", nsViews);
        // 分章节：物化视图
        map.put("dataSourcesMaterializedViews", dsMaterialized);
        map.put("namespacesMaterializedViews", nsMaterialized);
        map.put("ds4tableMaterializedViews", dsMaterialized);
        map.put("ns4tableMaterializedViews", nsMaterialized);
        map.putAll(PlanRenderContextBuilder.build(docPlan));
        return map;
    }

    private static TextRenderData convertToChineseNumbers(String date) {
        StringBuilder chineseDate = new StringBuilder();
        for (char ch : date.toCharArray()) {
            String chineseChar = CHINESE_NUMBER_MAP.getOrDefault(ch, String.valueOf(ch));
            chineseDate.append(chineseChar);
        }
        return new TextRenderData(chineseDate.toString());
    }
}
