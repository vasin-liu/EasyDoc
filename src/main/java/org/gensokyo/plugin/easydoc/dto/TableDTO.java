/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import com.intellij.database.model.DasTable;
import com.intellij.database.model.DasTableKey;
import com.intellij.database.psi.DbTable;
import com.intellij.database.util.DasUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        DasTableKey dtk = DasUtil.getPrimaryKey(table);
        if (Objects.nonNull(dtk)) {
            this.primaryKey = dtk.getName();
        }
        // 处理所有列
        this.columns = DasUtil.getColumns(table)
                .toStream()
                .map(ColumnDTO::new)
                .toList();
    }

    private DasTable das;

    private String name;

    private String comment;

    private String schema;

    private String primaryKey;

    private List<ColumnDTO> columns = new ArrayList<>();
}
