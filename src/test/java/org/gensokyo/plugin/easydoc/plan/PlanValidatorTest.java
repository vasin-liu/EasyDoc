package org.gensokyo.plugin.easydoc.plan;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class PlanValidatorTest {

    @Test
    public void should_generate_default_variable_when_empty() {
        DocNode node = new DocNode()
                .setType(NodeType.GROUP)
                .setDisplayName("Sales Group")
                .setVariableName("");
        DocPlan plan = new DocPlan().setRoots(List.of(node));

        PlanValidationResult result = PlanValidator.validateAndNormalize(plan);

        Assert.assertTrue(result.isValid());
        Assert.assertEquals("group_Sales_Group", node.getVariableName());
    }

    @Test
    public void should_fail_when_variable_name_invalid() {
        DocNode node = new DocNode()
                .setType(NodeType.TABLE)
                .setDisplayName("t_order")
                .setVariableName("1invalid");
        DocPlan plan = new DocPlan().setRoots(List.of(node));

        PlanValidationResult result = PlanValidator.validateAndNormalize(plan);

        Assert.assertFalse(result.isValid());
        Assert.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void should_fail_when_variable_name_duplicated() {
        DocNode left = new DocNode()
                .setType(NodeType.TABLE)
                .setDisplayName("t_a")
                .setVariableName("same_name");
        DocNode right = new DocNode()
                .setType(NodeType.VIEW)
                .setDisplayName("v_a")
                .setVariableName("same_name");
        DocPlan plan = new DocPlan().setRoots(List.of(left, right));

        PlanValidationResult result = PlanValidator.validateAndNormalize(plan);

        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Duplicate variable name")));
    }

    @Test
    public void should_auto_dedupe_default_variable_name_when_duplicated() {
        DocNode left = new DocNode()
                .setType(NodeType.TABLE)
                .setDisplayName("t_a")
                .setVariableName("table_t_a");
        DocNode right = new DocNode()
                .setType(NodeType.TABLE)
                .setDisplayName("t_a")
                .setVariableName("table_t_a");
        DocPlan plan = new DocPlan().setRoots(List.of(left, right));

        PlanValidationResult result = PlanValidator.validateAndNormalize(plan);

        Assert.assertTrue(result.isValid());
        Assert.assertEquals("table_t_a", left.getVariableName());
        Assert.assertEquals("table_t_a_2", right.getVariableName());
    }
}
