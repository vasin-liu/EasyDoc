/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.intellij.database.model.ObjectKind;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.dto.ColumnDTO;
import org.gensokyo.plugin.easydoc.dto.DataSourceDTO;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.dto.TableDTO;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 将同一数据源下物理表（含多 schema）的列注释/类型回填到视图、物化视图；列名按不区分大小写匹配。
 */
public final class DerivedCommentInheritanceKit {

    private DerivedCommentInheritanceKit() {
        throw new UnsupportedOperationException();
    }

    public static void applyToDataSources(Collection<DataSourceDTO> sources) {
        if (sources == null || sources.isEmpty()) {
            return;
        }
        for (DataSourceDTO ds : sources) {
            applyToDataSource(ds);
        }
    }

    public static void applyToDataSource(DataSourceDTO ds) {
        if (ds == null || ds.getNamespaces() == null || ds.getNamespaces().isEmpty()) {
            return;
        }
        Map<String, String> colComment = new HashMap<>();
        Map<String, String> colType = new HashMap<>();
        for (NamespaceDTO ns : ds.getNamespaces()) {
            if (ns.getTables() == null) {
                continue;
            }
            for (TableDTO table : ns.getTables()) {
                if (!isPhysicalTable(table)) {
                    continue;
                }
                for (ColumnDTO column : table.getColumns()) {
                    String key = normalizeColumnKey(column.getName());
                    if (StringUtils.isBlank(key)) {
                        continue;
                    }
                    if (StringUtils.isNotBlank(column.getComment())) {
                        colComment.merge(key, column.getComment(), (a, b) -> StringUtils.isNotBlank(a) ? a : b);
                    }
                    if (StringUtils.isNotBlank(column.getType())) {
                        colType.merge(key, column.getType(), (a, b) -> StringUtils.isNotBlank(a) ? a : b);
                    }
                }
            }
        }
        for (NamespaceDTO ns : ds.getNamespaces()) {
            if (ns.getTables() == null || ns.getTables().isEmpty()) {
                continue;
            }
            String fallbackTableComment = "";
            for (TableDTO table : ns.getTables()) {
                if (isPhysicalTable(table) && StringUtils.isNotBlank(table.getComment())) {
                    fallbackTableComment = table.getComment();
                    break;
                }
            }
            boolean anyDerived = ns.getTables().stream().anyMatch(DerivedCommentInheritanceKit::isDerivedObject);
            if (!anyDerived) {
                continue;
            }
            for (TableDTO table : ns.getTables()) {
                if (!isDerivedObject(table)) {
                    continue;
                }
                if (StringUtils.isBlank(table.getComment()) && StringUtils.isNotBlank(fallbackTableComment)) {
                    table.setComment(fallbackTableComment);
                }
                for (ColumnDTO column : table.getColumns()) {
                    String key = normalizeColumnKey(column.getName());
                    if (StringUtils.isBlank(key)) {
                        continue;
                    }
                    if (StringUtils.isBlank(column.getComment())) {
                        String inherited = colComment.get(key);
                        if (StringUtils.isNotBlank(inherited)) {
                            column.setComment(inherited);
                        }
                    }
                    if (StringUtils.isBlank(column.getType())) {
                        String inheritedType = colType.get(key);
                        if (StringUtils.isNotBlank(inheritedType)) {
                            column.setType(inheritedType);
                        }
                    }
                }
            }
        }
    }

    private static String normalizeColumnKey(String name) {
        return StringUtils.lowerCase(StringUtils.trimToEmpty(name), Locale.ROOT);
    }

    private static boolean isPhysicalTable(TableDTO t) {
        return t != null && t.isKind(ObjectKind.TABLE);
    }

    private static boolean isDerivedObject(TableDTO t) {
        return t != null && !isPhysicalTable(t);
    }

    /**
     * 单命名空间场景（测试或仅一个 schema）仍可通过 {@link NamespaceDTO#inheritColumnCommentsForDerivedObjects()} 调用。
     */
    public static void applyToNamespace(NamespaceDTO ns) {
        if (ns == null) {
            return;
        }
        DataSourceDTO wrap = new DataSourceDTO();
        wrap.setName("_single_");
        wrap.setNamespaces(java.util.List.of(ns));
        applyToDataSource(wrap);
    }
}
