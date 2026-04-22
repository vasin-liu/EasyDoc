package org.gensokyo.plugin.easydoc.plan;

import com.intellij.database.model.ObjectKind;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.dto.DataSourceDTO;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.dto.TableDTO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DocPlanBuilder {
    private DocPlanBuilder() {
        throw new UnsupportedOperationException();
    }

    public static DocPlan fromDataSources(Collection<DataSourceDTO> dataSources) {
        DocPlan plan = new DocPlan();
        if (dataSources == null || dataSources.isEmpty()) {
            return plan;
        }
        int dsOrder = 0;
        List<DocNode> roots = new ArrayList<>();
        for (DataSourceDTO ds : dataSources) {
            DocNode dsNode = new DocNode()
                    .setType(NodeType.DATABASE)
                    .setDisplayName(ds.getName())
                    .setVariableName(defaultVariableName(NodeType.DATABASE, ds.getName()))
                    .setOrder(dsOrder++);
            int nsOrder = 0;
            List<NamespaceDTO> namespaces = ds.getNamespaces() == null ? List.of() : ds.getNamespaces();
            for (NamespaceDTO ns : namespaces) {
                DocNode nsNode = new DocNode()
                        .setType(NodeType.NAMESPACE)
                        .setDisplayName(ns.getName())
                        .setVariableName(defaultVariableName(NodeType.NAMESPACE, ns.getName()))
                        .setOrder(nsOrder++);
                int tableOrder = 0;
                List<TableDTO> tables = ns.getTables() == null ? List.of() : ns.getTables();
                for (TableDTO table : tables) {
                    NodeType objectNodeType = toNodeType(table.getObjectKind());
                    DocNode objectNode = new DocNode()
                            .setType(objectNodeType)
                            .setDisplayName(table.getName())
                            .setVariableName(defaultVariableName(objectNodeType, table.getName()))
                            .setOrder(tableOrder++)
                            .setRef(new DbRef()
                                    .setDataSource(ds.getName())
                                    .setNamespace(ns.getName())
                                    .setObjectName(table.getName())
                                    .setObjectKind(table.getObjectKind() == null ? null : table.getObjectKind().name()));
                    nsNode.getChildren().add(objectNode);
                }
                dsNode.getChildren().add(nsNode);
            }
            roots.add(dsNode);
        }
        plan.setRoots(roots);
        return plan;
    }

    static String defaultVariableName(NodeType type, String displayName) {
        String prefix = switch (type) {
            case DATABASE -> "db";
            case NAMESPACE -> "ns";
            case TABLE -> "table";
            case VIEW -> "view";
            case MAT_VIEW -> "mat_view";
            case GROUP -> "group";
        };
        String normalized = normalizeName(displayName);
        if (StringUtils.isBlank(normalized)) {
            return prefix;
        }
        return prefix + "_" + normalized;
    }

    private static String normalizeName(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        String normalized = name.trim().replaceAll("[^a-zA-Z0-9_]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+", "");
        normalized = normalized.replaceAll("_+$", "");
        if (normalized.isEmpty()) {
            return "";
        }
        if (Character.isDigit(normalized.charAt(0))) {
            return "_" + normalized;
        }
        return normalized;
    }

    private static NodeType toNodeType(ObjectKind kind) {
        if (kind == ObjectKind.VIEW) {
            return NodeType.VIEW;
        }
        if (kind == ObjectKind.MAT_VIEW || "MATERIALIZED_VIEW".equalsIgnoreCase(kind == null ? null : kind.name())) {
            return NodeType.MAT_VIEW;
        }
        return NodeType.TABLE;
    }
}
