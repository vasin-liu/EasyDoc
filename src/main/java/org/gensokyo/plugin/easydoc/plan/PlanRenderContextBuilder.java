package org.gensokyo.plugin.easydoc.plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlanRenderContextBuilder {
    private PlanRenderContextBuilder() {
        throw new UnsupportedOperationException();
    }

    public static Map<String, Object> build(DocPlan plan) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (plan == null) {
            out.put("plan", null);
            out.put("vars", Map.of());
            out.put("varNodes", Map.of());
            out.put("varItems", Map.of());
            return out;
        }
        out.put("plan", plan);
        Map<String, Object> vars = new LinkedHashMap<>();
        Map<String, DocNode> varNodes = new LinkedHashMap<>();
        Map<String, List<DocNode>> varItems = new LinkedHashMap<>();
        if (plan.getRoots() != null) {
            for (DocNode root : plan.getRoots()) {
                collectVars(root, vars, varNodes, varItems);
            }
        }
        out.put("vars", vars);
        out.put("varNodes", varNodes);
        out.put("varItems", varItems);
        return out;
    }

    private static List<DocNode> collectVars(
            DocNode node,
            Map<String, Object> vars,
            Map<String, DocNode> varNodes,
            Map<String, List<DocNode>> varItems
    ) {
        if (node == null || !node.isEnabled()) {
            return List.of();
        }
        List<DocNode> enabledObjects = new ArrayList<>();
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (DocNode child : node.getChildren()) {
                enabledObjects.addAll(collectVars(child, vars, varNodes, varItems));
            }
            vars.put(node.getVariableName(), enabledObjects);
            varNodes.put(node.getVariableName(), node);
            varItems.put(node.getVariableName(), List.copyOf(enabledObjects));
        } else if (node.isEnabled()) {
            enabledObjects.add(node);
            vars.put(node.getVariableName(), node);
            varNodes.put(node.getVariableName(), node);
            varItems.put(node.getVariableName(), List.of(node));
        }
        return enabledObjects;
    }
}
