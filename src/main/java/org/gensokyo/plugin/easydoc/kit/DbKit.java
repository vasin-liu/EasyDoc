/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.intellij.database.dataSource.DatabaseConnection;
import com.intellij.database.dataSource.connection.DGDepartment;
import com.intellij.database.psi.DbElement;
import com.intellij.database.remote.jdbc.RemoteDatabaseMetaData;
import com.intellij.database.util.DbImplUtil;
import com.intellij.database.util.GuardedRef;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.dto.DbMeta;

import java.util.Objects;

/**
 * 数据库工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/21 , Version 1.0.0
 */
public class DbKit {
    private static final Logger LOG = Logger.getInstance(DbKit.class);

    private DbKit() {
        throw new UnsupportedOperationException();
    }

    public static DbMeta resolve(DbElement element) {
        if (Objects.isNull(element)) {
            return null;
        }
        try {
            GuardedRef<DatabaseConnection> conn = DbImplUtil.getDatabaseConnection(element, DGDepartment.CODE_GENERATION);
            if (Objects.nonNull(conn)) {
                RemoteDatabaseMetaData meta = conn.get().getRemoteMetaData();
                if (Objects.nonNull(meta)) {
                    String dialect = meta.getDatabaseProductName();
                    String version = meta.getDatabaseProductVersion();
                    return DbMeta.of().dialect(dialect).version(version).build();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to resolve database name", e);
        }
        return null;
    }

    public static boolean needReResolve(String dialect) {
        return StringUtils.containsIgnoreCase(dialect, "Generic")
                || StringUtils.containsIgnoreCase(dialect, "Unknown");
    }
}
