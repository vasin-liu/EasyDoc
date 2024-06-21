/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import com.intellij.database.model.DasNamespace;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbNamespace;
import com.intellij.database.psi.DbTable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 命名空间对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/6 , Version 1.0.0
 */
@Data
public class NamespaceDTO implements Item<NamespaceDTO> {

    public NamespaceDTO() {
    }

    public NamespaceDTO(DbNamespace namespace) {
        this.das = namespace;
        this.name = namespace.getName();
        this.comment = namespace.getComment();
        List<TableDTO> list = namespace.getDasChildren(ObjectKind.TABLE)
                .toStream()
                .filter(t -> t instanceof DbTable)
                .map(t -> (DbTable) t)
                .map(TableDTO::new)
                .toList();
        if (!list.isEmpty()) {
            tables.addAll(list);
        }
    }

    public void merge(NamespaceDTO newNs) {
        if (Objects.isNull(newNs)) {
            return;
        }

        newNs.getTables()
                .stream()
                .filter(t -> !tables.contains(t))
                .forEach(t -> tables.add(t));
    }

    private DasNamespace das;

    private String name;

    private String comment;

    private List<TableDTO> tables = new ArrayList<>();
}
