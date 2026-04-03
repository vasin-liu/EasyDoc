/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import lombok.Data;

/**
 * 视图/物化视图字段来源信息：指向某个物理表列。
 */
@Data
public class ColumnSourceDTO {

    /**
     * 源表名称（必要时可包含 schema 前缀，由调用方负责约定格式）。
     */
    private String tableName;

    /**
     * 源列名称。
     */
    private String columnName;
}

