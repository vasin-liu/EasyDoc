/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DataType;
import com.intellij.database.util.DasUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 列对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/6 , Version 1.0.0
 */
@Data
public class ColumnDTO {

    public ColumnDTO() {
    }

    public ColumnDTO(DasColumn column) {
        this.name = column.getName();
        this.comment = StringUtils.defaultString(column.getComment(), "");
        DataType dataType = column.getDasType().toDataType();
        this.type = dataType.typeName;
        this.length = dataType.getLength();
        this.isQuoted = column.isQuoted();
        this.defaultValue = StringUtils.defaultString(column.getDefault(), "");
        this.isNotNull = column.isNotNull();
        this.isPrimaryKey = DasUtil.isPrimary(column);
        this.tableName = column.getTableName();
    }

    private String name;

    private String comment;

    private String type;

    private int length;

    private boolean isQuoted;

    private String defaultValue;

    private boolean isNotNull;

    private boolean isPrimaryKey;

    private String tableName;
}
