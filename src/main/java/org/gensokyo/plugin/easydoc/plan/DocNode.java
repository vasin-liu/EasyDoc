package org.gensokyo.plugin.easydoc.plan;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class DocNode {
    private String id = UUID.randomUUID().toString();
    private NodeType type = NodeType.GROUP;
    private String displayName;
    private String variableName;
    private int order;
    private List<DocNode> children = new ArrayList<>();
    private DbRef ref;
    private boolean enabled = true;

    public boolean isGroupNode() {
        return type == NodeType.GROUP || type == NodeType.DATABASE || type == NodeType.NAMESPACE;
    }

    @Override
    public String toString() {
        return displayName == null ? type.name() : displayName;
    }
}
