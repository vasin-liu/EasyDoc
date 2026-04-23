package org.gensokyo.plugin.easydoc.dto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TableDtoCommentTest {

    @Test
    public void getComment_usesOverrideWhenNonBlank() {
        TableDTO t = new TableDTO();
        t.setComment("from metadata");
        t.setSortDisplayOverride("this run only");
        assertEquals("this run only", t.getComment());
    }

    @Test
    public void getComment_fallsBackWhenOverrideBlank() {
        TableDTO t = new TableDTO();
        t.setComment("from metadata");
        t.setSortDisplayOverride(null);
        assertEquals("from metadata", t.getComment());

        t.setSortDisplayOverride("   ");
        assertEquals("from metadata", t.getComment());
    }

    @Test
    public void setComment_doesNotSetOverride() {
        TableDTO t = new TableDTO();
        t.setComment("a");
        assertNull(t.getSortDisplayOverride());
    }
}
