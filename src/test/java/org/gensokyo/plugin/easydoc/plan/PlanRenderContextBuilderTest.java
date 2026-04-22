package org.gensokyo.plugin.easydoc.plan;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class PlanRenderContextBuilderTest {

    @Test
    public void should_collect_group_variable_as_enabled_descendants() {
        DocNode table = new DocNode()
                .setType(NodeType.TABLE)
                .setDisplayName("t_user")
                .setVariableName("table_user")
                .setEnabled(true);
        DocNode view = new DocNode()
                .setType(NodeType.VIEW)
                .setDisplayName("v_user")
                .setVariableName("view_user")
                .setEnabled(false);
        DocNode group = new DocNode()
                .setType(NodeType.GROUP)
                .setDisplayName("core")
                .setVariableName("core_group")
                .setChildren(List.of(table, view));
        DocPlan plan = new DocPlan().setRoots(List.of(group));

        Map<String, Object> ctx = PlanRenderContextBuilder.build(plan);

        Map<String, Object> vars = (Map<String, Object>) ctx.get("vars");
        Map<String, DocNode> varNodes = (Map<String, DocNode>) ctx.get("varNodes");
        Map<String, List<DocNode>> varItems = (Map<String, List<DocNode>>) ctx.get("varItems");
        Assert.assertNotNull(vars.get("table_user"));
        Assert.assertNull(vars.get("view_user"));
        Assert.assertTrue(vars.get("core_group") instanceof List<?>);
        List<?> groupList = (List<?>) vars.get("core_group");
        Assert.assertEquals(1, groupList.size());
        Assert.assertEquals("core", varNodes.get("core_group").getDisplayName());
        Assert.assertEquals(1, varItems.get("core_group").size());
        Assert.assertEquals(1, varItems.get("table_user").size());
    }

    @Test
    public void should_skip_disabled_group_and_descendants() {
        DocNode table = new DocNode()
                .setType(NodeType.TABLE)
                .setDisplayName("t_user")
                .setVariableName("table_user")
                .setEnabled(true);
        DocNode group = new DocNode()
                .setType(NodeType.GROUP)
                .setDisplayName("core")
                .setVariableName("core_group")
                .setEnabled(false)
                .setChildren(List.of(table));
        DocPlan plan = new DocPlan().setRoots(List.of(group));

        Map<String, Object> ctx = PlanRenderContextBuilder.build(plan);
        Map<String, Object> vars = (Map<String, Object>) ctx.get("vars");
        Map<String, DocNode> varNodes = (Map<String, DocNode>) ctx.get("varNodes");
        Map<String, List<DocNode>> varItems = (Map<String, List<DocNode>>) ctx.get("varItems");

        Assert.assertTrue(vars.isEmpty());
        Assert.assertTrue(varNodes.isEmpty());
        Assert.assertTrue(varItems.isEmpty());
    }
}
