/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import com.intellij.database.model.DasDataSource;
import com.intellij.database.psi.DbDataSource;
import lombok.Data;
import org.gensokyo.plugin.easydoc.kit.DbKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据源对象对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/6 , Version 1.0.0
 */
@Data
public class DataSourceDTO implements Item<DataSourceDTO> {

    public DataSourceDTO() {
    }

    public DataSourceDTO(DbDataSource ds) {
        this.das = ds;
        this.name = ds.getName();
        this.comment = ds.getComment();
        this.dialect = ds.getDatabaseDialect().getDisplayName();
        this.version = ds.getDatabaseVersion().version;
        resolve(ds);
    }

    public DataSourceDTO(DasDataSource ds) {
        this.das = ds;
        this.name = ds.getName();
        this.comment = ds.getComment();
        this.dialect = ds.getDbms().getDisplayName();
        this.version = ds.getDatabaseVersion().version;
    }

    private void resolve(DbDataSource ds) {
        if (DbKit.needReResolve(this.dialect)) {
            DbMeta dbMeta = DbKit.resolve(ds);
            if (Objects.nonNull(dbMeta)) {
                this.dialect = dbMeta.dialect();
                this.version = dbMeta.version();
            }
        }
    }

    private DasDataSource das;

    private String name;

    private String comment;

    private String dialect;

    private String version;

    private List<NamespaceDTO> namespaces = new ArrayList<>();

    public void prepareForRender() {
        namespaces.forEach(NamespaceDTO::prepareForRender);
        this.das = null;
    }
}
