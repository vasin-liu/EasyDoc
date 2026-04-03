/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import com.intellij.database.model.DasNamespace;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbNamespace;
import com.intellij.database.psi.DbTable;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.kit.DbPartitionKit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 命名空间对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/6 , Version 1.0.0
 */
@Data
public class NamespaceDTO implements Item<NamespaceDTO> {

    public NamespaceDTO() {
    }

    public NamespaceDTO(DbNamespace namespace) {
        this.das = namespace;
        this.name = namespace.getName();
        this.comment = namespace.getComment();
        List<TableDTO> list = new ArrayList<>();
        appendObjectsByKind(namespace, "TABLE", list);
        // 整库扫描时 IDE 会把分区物理表（如 dwa_xxx_20260403）与父表并列返回；isPartition() 在 Hive 等方言常不可用，按名称去重
        list = filterLikelyPartitionChildTablesByName(list);
        appendObjectsByKind(namespace, "VIEW", list);
        appendObjectsByKind(namespace, "MATERIALIZED_VIEW", list);
        if (!list.isEmpty()) {
            tables.addAll(list);
        }
    }

    public void merge(NamespaceDTO newNs) {
        if (Objects.isNull(newNs)) {
            return;
        }

        newNs.getTables()
                .stream()
                .filter(t -> tables.stream().noneMatch(o -> sameLogicalTable(o, t)))
                .forEach(t -> tables.add(t));
    }

    private static boolean sameLogicalTable(TableDTO existing, TableDTO candidate) {
        return Objects.equals(existing.getName(), candidate.getName())
                && Objects.equals(existing.getSchema(), candidate.getSchema())
                && Objects.equals(existing.getObjectKind(), candidate.getObjectKind());
    }

    private DasNamespace das;

    private String name;

    private String comment;

    private List<TableDTO> tables = new ArrayList<>();

    private static void appendObjectsByKind(DbNamespace namespace, String kindName, List<TableDTO> output) {
        ObjectKind kind = resolveObjectKind(kindName);
        if (Objects.isNull(kind)) {
            return;
        }
        namespace.getDasChildren(kind)
                .toStream()
                .filter(t -> t instanceof DbTable)
                .map(t -> (DbTable) t)
                .filter(t -> !DbPartitionKit.isPhysicalPartitionTable(t))
                .map(TableDTO::new)
                .forEach(output::add);
    }

    /**
     * 仅处理 {@link ObjectKind#TABLE} 已收集进 list 的条目：若存在同名父表 {@code base}，且另一表名为 {@code base_分区后缀}
     * （如 {@code dwa_ana_car_travel_feature_rank_20260403}），则去掉子表。单独只选子表时列表中无父表，不会误删。
     */
    private static List<TableDTO> filterLikelyPartitionChildTablesByName(List<TableDTO> tables) {
        if (tables == null || tables.isEmpty()) {
            return tables == null ? new ArrayList<>() : tables;
        }
        Set<String> tableNamesLower = new HashSet<>();
        for (TableDTO t : tables) {
            if (t.isKind("TABLE") && StringUtils.isNotBlank(t.getName())) {
                tableNamesLower.add(t.getName().toLowerCase(Locale.ROOT));
            }
        }
        List<TableDTO> out = new ArrayList<>(tables.size());
        for (TableDTO t : tables) {
            if (!t.isKind("TABLE")) {
                out.add(t);
                continue;
            }
            if (isLikelyPartitionChildTableName(t.getName(), tableNamesLower)) {
                continue;
            }
            out.add(t);
        }
        return out;
    }

    private static boolean isLikelyPartitionChildTableName(String name, Set<String> allTableNamesLower) {
        if (StringUtils.isBlank(name) || name.indexOf('_') < 0) {
            return false;
        }
        for (int i = name.length() - 1; i > 0; i--) {
            if (name.charAt(i) != '_') {
                continue;
            }
            String base = name.substring(0, i);
            String suffix = name.substring(i + 1);
            if (StringUtils.isBlank(base) || StringUtils.isBlank(suffix)) {
                continue;
            }
            if (!looksLikePartitionTableSuffix(suffix)) {
                continue;
            }
            if (allTableNamesLower.contains(base.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** 常见分区物理表后缀：日期/批次纯数字（6～14 位）、p+数字 */
    private static boolean looksLikePartitionTableSuffix(String suffix) {
        return suffix.matches("\\d{6,14}") || suffix.matches("(?i)p\\d+");
    }

    private static ObjectKind resolveObjectKind(String kindName) {
        try {
            Object constant = ObjectKind.class.getField(kindName).get(null);
            if (constant instanceof ObjectKind objectKind) {
                return objectKind;
            }
            return null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }

    /**
     * 视图/物化视图字段若没有注释，则按同库表字段名回填，尽量复用来源字段语义。
     */
    public void inheritColumnCommentsForDerivedObjects() {
        if (tables.isEmpty()) {
            return;
        }
        
        // Quick check: if no derived objects, skip expensive column loading
        boolean hasDerivedObjects = tables.stream().anyMatch(this::isDerivedObject);
        if (!hasDerivedObjects) {
            return;
        }
        
        String fallbackTableComment = "";
        Map<String, String> tableColumnCommentMap = new HashMap<>();
        Map<String, String> tableColumnTypeMap = new HashMap<>();
        
        // Build comment/type maps from base tables only
        for (TableDTO table : tables) {
            if (!table.isKind("TABLE")) {
                continue;
            }
            if (StringUtils.isBlank(fallbackTableComment) && StringUtils.isNotBlank(table.getComment())) {
                fallbackTableComment = table.getComment();
            }
            table.getColumns().forEach(column -> {
                if (StringUtils.isNotBlank(column.getComment())) {
                    tableColumnCommentMap.putIfAbsent(column.getName(), column.getComment());
                }
                if (StringUtils.isNotBlank(column.getType())) {
                    tableColumnTypeMap.putIfAbsent(column.getName(), column.getType());
                }
            });
        }

        // Apply inheritance to derived objects (views, materialized views)
        for (TableDTO table : tables) {
            if (!isDerivedObject(table)) {
                continue;
            }
            if (StringUtils.isBlank(table.getComment()) && StringUtils.isNotBlank(fallbackTableComment)) {
                table.setComment(fallbackTableComment);
            }
            table.getColumns().forEach(column -> {
                if (StringUtils.isBlank(column.getComment())) {
                    String inheritedComment = tableColumnCommentMap.get(column.getName());
                    if (StringUtils.isNotBlank(inheritedComment)) {
                        column.setComment(inheritedComment);
                    }
                }
                if (StringUtils.isBlank(column.getType())) {
                    String inheritedType = tableColumnTypeMap.get(column.getName());
                    if (StringUtils.isNotBlank(inheritedType)) {
                        column.setType(inheritedType);
                    }
                }
            });
        }
    }

    private boolean isDerivedObject(TableDTO table) {
        return !table.isKind("TABLE");
    }

    public void prepareForRender() {
        tables.forEach(TableDTO::prepareForRender);
        this.das = null;
    }
}
