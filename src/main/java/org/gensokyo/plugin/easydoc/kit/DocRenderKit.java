/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.intellij.database.model.ObjectKind;
import org.gensokyo.plugin.easydoc.dto.DataSourceDTO;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.dto.TableDTO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 按 {@link TableDTO#getObjectKind()} 拆渲染模型，供模板分章节绑定不同变量。
 */
public final class DocRenderKit {

    private DocRenderKit() {
        throw new UnsupportedOperationException();
    }

    public static List<DataSourceDTO> dataSourcesOfKind(Collection<DataSourceDTO> sources, ObjectKind objectKind) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        List<DataSourceDTO> out = new ArrayList<>();
        for (DataSourceDTO ds : sources) {
            List<NamespaceDTO> nsFiltered = namespacesOfKind(ds.getNamespaces(), objectKind);
            if (nsFiltered.isEmpty()) {
                continue;
            }
            DataSourceDTO copy = new DataSourceDTO();
            copy.setName(ds.getName());
            copy.setComment(ds.getComment());
            copy.setDialect(ds.getDialect());
            copy.setVersion(ds.getVersion());
            copy.setNamespaces(nsFiltered);
            out.add(copy);
        }
        return out;
    }

    public static List<NamespaceDTO> namespacesOfKind(Collection<NamespaceDTO> namespaces, ObjectKind objectKind) {
        if (namespaces == null || namespaces.isEmpty()) {
            return List.of();
        }
        List<NamespaceDTO> out = new ArrayList<>();
        for (NamespaceDTO ns : namespaces) {
            List<TableDTO> tables = ns.getTables() == null ? List.of() : ns.getTables().stream()
                    .filter(t -> t.isKind(objectKind))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (tables.isEmpty()) {
                continue;
            }
            NamespaceDTO n = new NamespaceDTO();
            n.setName(ns.getName());
            n.setComment(ns.getComment());
            n.setTables(tables);
            out.add(n);
        }
        return out;
    }

    public static List<NamespaceDTO> flattenNamespaces(Collection<DataSourceDTO> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream().flatMap(ds -> ds.getNamespaces().stream()).collect(Collectors.toCollection(ArrayList::new));
    }
}
