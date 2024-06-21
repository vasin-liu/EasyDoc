/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import com.intellij.database.psi.DbDataSource;
import lombok.Data;

import java.util.List;

/**
 * 数据源对象对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/6 , Version 1.0.0
 */
@Data
public class DataSourceDTO {

    public DataSourceDTO() {
    }

    public DataSourceDTO(DbDataSource dataSource) {
    }

    private String name;

    private String comment;

    private List<NamespaceDTO> namespaces;
}
