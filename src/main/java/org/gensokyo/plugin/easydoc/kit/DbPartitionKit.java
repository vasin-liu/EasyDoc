/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.psi.DbTable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * 识别数据库「物理分区子表」。在 JPMS 下对实现类（如 {@code PgImplModel$LocalTable}）直接用
 * {@link Class#getMethod(String, Class[])} 取方法再 {@link Method#invoke} 会触发
 * {@link IllegalAccessException}；应优先通过导出 API（接口）上的 {@link Method} 调用，并在必要时
 * {@link Method#trySetAccessible()}。
 */
public final class DbPartitionKit {

    private DbPartitionKit() {
        throw new UnsupportedOperationException();
    }

    /**
     * 是否为物理分区子表（叶子分区等）。父级「分区表」本身一般返回 false。
     */
    public static boolean isPhysicalPartitionTable(DbTable table) {
        if (table == null) {
            return false;
        }
        DasObject das = readDasObject(table);
        if (das instanceof DasTable) {
            return invokeIsPartitionOnImplementation(table.getDelegate());
        }
        return false;
    }

    /**
     * 选中叶子分区时，尽量解析为应对外文档化的根表（分区父表）。无法解析则返回原表。
     */
    public static DbTable resolveRootTableForDocumentation(@Nullable Project project, DbTable table) {
        if (project == null || !isPhysicalPartitionTable(table)) {
            return table;
        }
        DasObject current = readDasObject(table);
        for (DasObject p = parentDasObject(current); p != null; p = parentDasObject(p)) {
            if (p instanceof DasTable parentTable && !invokeIsPartitionOnImplementation(parentTable)) {
                DbTable psi = findDbTablePsi(project, parentTable);
                return psi != null ? psi : table;
            }
        }
        return table;
    }

    private static @Nullable DasObject readDasObject(DbTable table) {
        try {
            Method m = DbTable.class.getMethod("getDasObject");
            m.trySetAccessible();
            Object o = m.invoke(table);
            return o instanceof DasObject d ? d : null;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            try {
                Method m = table.getClass().getMethod("getDasObject");
                m.trySetAccessible();
                Object o = m.invoke(table);
                return o instanceof DasObject d ? d : null;
            } catch (ReflectiveOperationException | SecurityException ignored2) {
                return null;
            }
        }
    }

    private static @Nullable DasObject parentDasObject(@Nullable DasObject das) {
        if (das == null) {
            return null;
        }
        try {
            Method m = DasObject.class.getMethod("getDasParent");
            m.trySetAccessible();
            Object p = m.invoke(das);
            return p instanceof DasObject d ? d : null;
        } catch (ReflectiveOperationException | SecurityException ignored1) {
            try {
                Method m = das.getClass().getMethod("getDasParent");
                m.trySetAccessible();
                Object p = m.invoke(das);
                return p instanceof DasObject d ? d : null;
            } catch (ReflectiveOperationException | SecurityException ignored2) {
                try {
                    Method m = das.getClass().getMethod("getParent");
                    m.trySetAccessible();
                    Object p = m.invoke(das);
                    return p instanceof DasObject d ? d : null;
                } catch (ReflectiveOperationException | SecurityException ignored3) {
                    return null;
                }
            }
        }
    }

    private static boolean invokeIsPartitionOnImplementation(Object target) {
        if (target == null) {
            return false;
        }
        try {
            Method m = target.getClass().getMethod("isPartition");
            if (m.getParameterCount() != 0) {
                return false;
            }
            if (m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class) {
                return false;
            }
            m.trySetAccessible();
            Object v = m.invoke(target);
            return Boolean.TRUE.equals(v);
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return false;
        }
    }

    private static @Nullable DbTable findDbTablePsi(Project project, DasTable dasTable) {
        try {
            var facade = DbPsiFacade.getInstance(project);
            try {
                Method m = DbPsiFacade.class.getMethod("findElement", DasObject.class);
                m.trySetAccessible();
                Object psi = m.invoke(facade, dasTable);
                if (psi instanceof DbTable t) {
                    return t;
                }
            } catch (ReflectiveOperationException ignored) {
                for (Method m : facade.getClass().getMethods()) {
                    if (!"findElement".equals(m.getName()) || m.getParameterCount() != 1) {
                        continue;
                    }
                    try {
                        m.trySetAccessible();
                        Object psi = m.invoke(facade, dasTable);
                        if (psi instanceof DbTable t) {
                            return t;
                        }
                    } catch (ReflectiveOperationException ignored2) {
                        // try next overload
                    }
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return null;
    }
}
