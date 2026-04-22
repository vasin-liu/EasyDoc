package org.gensokyo.plugin.easydoc.plan;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class DocPlan {
    private String version = "v1";
    private List<DocNode> roots = new ArrayList<>();
    private Map<String, Object> meta = new HashMap<>();
}
