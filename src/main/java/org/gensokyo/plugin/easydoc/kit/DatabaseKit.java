/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbElement;
import com.intellij.database.psi.DbNamespace;
import com.intellij.database.psi.DbTable;
import com.intellij.openapi.application.ApplicationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/12 , Version 1.0.0
 */
public class DatabaseKit {

    public static List<DasTable> getTablesFromNamespace(DbNamespace dbNamespace) {
        List<DasTable> tables = new ArrayList<>();

        ApplicationManager.getApplication().runReadAction(() -> {
            for (DasObject dasObject : dbNamespace.getDasChildren(ObjectKind.TABLE)) {
                if (dasObject instanceof DasTable) {
                    tables.add((DasTable) dasObject);
                }
            }
        });

        return tables;
    }

    public static DbDataSource getDataSourceFromTable(DbTable dbTable) {
        DbElement parent = dbTable;
        while (parent != null && !(parent instanceof DbDataSource)) {
            parent = parent.getParent();
        }
        return (DbDataSource) parent;
    }
}
