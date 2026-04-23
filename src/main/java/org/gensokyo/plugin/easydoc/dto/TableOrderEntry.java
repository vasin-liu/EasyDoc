/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 */
package org.gensokyo.plugin.easydoc.dto;

import lombok.Data;

/**
 * 排序面板上「拖曳列表」中的一项：表名 + 本次生成时可选的文档显示说明。
 * 多 schema 下同名表共享同一表名键，不区分 — 与排序侧一致。
 */
@Data
public class TableOrderEntry {

    private String tableName;
    private String displayComment;

    public TableOrderEntry() {
    }

    public TableOrderEntry(String tableName) {
        this.tableName = tableName;
    }

    public TableOrderEntry(String tableName, String displayComment) {
        this.tableName = tableName;
        this.displayComment = displayComment;
    }
}
