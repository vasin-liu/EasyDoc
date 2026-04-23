package org.gensokyo.plugin.easydoc.kit;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TableOrderInputKitTest {

    @Test
    public void parseLines_nameOnly() {
        TableOrderInputKit.Result r = TableOrderInputKit.parseLines(Arrays.asList("a", "b", "  a  "));
        assertEquals(Arrays.asList("a", "b"), r.order());
        assertTrue(r.displayByTableNameLower().isEmpty());
    }

    @Test
    public void parseLines_nameWithDisplay() {
        TableOrderInputKit.Result r = TableOrderInputKit.parseLines(
                List.of("t1|一", "t2|二", " # skip ", "", "t1|shouldNotReplace"));
        assertEquals(Arrays.asList("t1", "t2"), r.order());
        Map<String, String> d = r.displayByTableNameLower();
        assertEquals("一", d.get("t1"));
        assertEquals("二", d.get("t2"));
    }

    @Test
    public void parseLines_displayMayContainPipe() {
        TableOrderInputKit.Result r = TableOrderInputKit.parseLines(List.of("x|a|b|c"));
        assertEquals(List.of("x"), r.order());
        assertEquals("a|b|c", r.displayByTableNameLower().get("x"));
    }
}
