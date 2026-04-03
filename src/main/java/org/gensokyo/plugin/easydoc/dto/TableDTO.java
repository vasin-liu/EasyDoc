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
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 表数据对象
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

    private String comment;

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
}
