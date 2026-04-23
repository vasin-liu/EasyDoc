/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import com.intellij.database.model.DasTable;
import com.intellij.database.model.DasTableKey;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbTable;
import com.intellij.database.util.DasUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 表 / 视图 / 物化视图数据对象。
 * <p>
 * Word 模板中的表级 {@code comment} 绑定本类的 {@link #getComment()}：当用户在排序配置里为某表名设置了仅本次生成的说明显示时，优先返回该覆写；否则返回库元数据及 {@link org.gensokyo.plugin.easydoc.kit.DerivedCommentInheritanceKit}
 * 等在生成前已合并的说明。覆写键为表名（不区分大小写），多 schema 同名表不区分。
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/6 , Version 1.0.0
 */
@Data
public class TableDTO implements Item<TableDTO> {

    public TableDTO() {
    }

    public TableDTO(DbTable table) {
        this.das = table;
        this.name = table.getName();
        this.comment = table.getComment();
        this.schema = DasUtil.getSchema(table);
        ObjectKind kind = table.getKind();
        this.objectKind = Objects.nonNull(kind) ? kind : ObjectKind.TABLE;
        DasTableKey dtk = DasUtil.getPrimaryKey(table);
        if (Objects.nonNull(dtk)) {
            this.primaryKey = dtk.getName();
        }
    }

    private DasTable das;

    private String name;

    /**
     * 库/继承等得到的表说明（不经过排序覆写）。模板中的 {@code comment} 由 {@link #getComment()} 在渲染时返回本字段或 {@link #sortDisplayOverride}。
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String comment;

    /**
     * 非持久化。本次在排序中指定的文档显示名；非空时 {@link #getComment()} 在渲染中返回此值。多 schema 同名表同键、无法区分时勿依赖本字段区分。
     */
    private transient String sortDisplayOverride;

    private String schema;

    private String primaryKey;

    private List<ColumnDTO> columns = new ArrayList<>();

    /**
     * 与 IDEA Database 元数据一致的对象类型（TABLE / VIEW / MATERIALIZED_VIEW 等）。
     */
    private ObjectKind objectKind = ObjectKind.TABLE;

    private transient boolean columnsResolved;

    public List<ColumnDTO> getColumns() {
        if (columnsResolved) {
            return columns;
        }
        if (das instanceof DbTable dbTable) {
            this.columns = DasUtil.getColumns(dbTable)
                    .toStream()
                    .map(ColumnDTO::new)
                    .collect(Collectors.toCollection(ArrayList::new));
            // Release DB PSI reference immediately after column extraction to reduce memory pressure
            this.das = null;
        } else {
            this.columns = new ArrayList<>();
        }
        this.columnsResolved = true;
        return columns;
    }

    public void setColumns(List<ColumnDTO> columns) {
        this.columns = Objects.isNull(columns) ? new ArrayList<>() : columns;
        this.columnsResolved = true;
    }

    public boolean isKind(ObjectKind kind) {
        if (kind == null || this.objectKind == null) {
            return false;
        }
        return this.objectKind == kind;
    }

    /**
     * 按枚举常量名比较（不区分大小写），兼容模板/调用方字符串。
     */
    public boolean isKind(String kindName) {
        if (kindName == null || this.objectKind == null) {
            return false;
        }
        return kindName.equalsIgnoreCase(this.objectKind.name());
    }

    public void prepareForRender() {
        getColumns().forEach(ColumnDTO::prepareForRender);
        this.das = null;
    }

    /**
     * 供 poi-tl / 模板使用的表说明：{@link #sortDisplayOverride} 非空时返回之，否则返回内部保存的元数据/继承结果（见 {@link #setComment}）。
     */
    public String getComment() {
        if (StringUtils.isNotBlank(sortDisplayOverride)) {
            return sortDisplayOverride;
        }
        return comment;
    }

    /**
     * 设置库、继承逻辑等合并后的表说明，不表示排序中的「仅本次」覆写；覆写由 {@link #setSortDisplayOverride(String)} 在生成前单独设置或清空。
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
}
