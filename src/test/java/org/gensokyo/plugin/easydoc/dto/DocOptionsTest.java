package org.gensokyo.plugin.easydoc.dto;

import org.gensokyo.plugin.easydoc.plan.DocNode;
import org.gensokyo.plugin.easydoc.plan.DocPlan;
import org.gensokyo.plugin.easydoc.plan.NodeType;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class DocOptionsTest {

    @Test
    public void should_keep_legacy_keys_and_append_plan_context() {
        DocNode group = new DocNode()
                .setType(NodeType.GROUP)
                .setDisplayName("Core")
                .setVariableName("core_group");
        DocPlan plan = new DocPlan().setRoots(List.of(group));
        DocOptions opts = DocOptions.of()
                .title("title")
                .author("author")
                .version("1.0")
                .savePath("D:/tmp/a.docx")
                .dataSources(List.of())
                .namespaces(List.of())
                .docPlan(plan);

        Map<String, Object> map = opts.toMap();

        Assert.assertTrue(map.containsKey("dataSources"));
        Assert.assertTrue(map.containsKey("ds4table"));
        Assert.assertTrue(map.containsKey("plan"));
        Assert.assertTrue(map.containsKey("vars"));
        Assert.assertTrue(map.containsKey("varNodes"));
        Assert.assertTrue(map.containsKey("varItems"));
    }
}
