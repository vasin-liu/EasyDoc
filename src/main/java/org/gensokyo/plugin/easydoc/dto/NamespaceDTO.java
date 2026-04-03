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
import org.gensokyo.plugin.easydoc.kit.DerivedCommentInheritanceKit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
        appendObjectsByKind(namespace, ObjectKind.TABLE, list);
        // 整库扫描时 IDE 会把分区物理表（如 dwa_xxx_20260403）与父表并列返回；isPartition() 在 Hive 等方言常不可用，按名称去重
        list = filterLikelyPartitionChildTablesByName(list);
        appendObjectsByKind(namespace, ObjectKind.VIEW, list);
        appendObjectsByKind(namespace, ObjectKind.MAT_VIEW, list);
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

    private static void appendObjectsByKind(DbNamespace namespace, ObjectKind kind, List<TableDTO> output) {
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
            if (t.isKind(ObjectKind.TABLE) && StringUtils.isNotBlank(t.getName())) {
                tableNamesLower.add(t.getName().toLowerCase(Locale.ROOT));
            }
        }
        List<TableDTO> out = new ArrayList<>(tables.size());
        for (TableDTO t : tables) {
            if (!t.isKind(ObjectKind.TABLE)) {
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

    /**
     * 视图/物化视图字段若没有注释，则按数据源内物理表同名列回填（支持跨 schema、列名大小写不一致）。
     * @see DerivedCommentInheritanceKit#applyToNamespace(NamespaceDTO)
     */
    public void inheritColumnCommentsForDerivedObjects() {
        DerivedCommentInheritanceKit.applyToNamespace(this);
    }

    public void prepareForRender() {
        tables.forEach(TableDTO::prepareForRender);
        this.das = null;
    }
}
