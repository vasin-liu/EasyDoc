package org.gensokyo.plugin.easydoc.kit;

import org.gensokyo.plugin.easydoc.dto.DataSourceDTO;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.dto.TableDTO;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TableOrderInputKitClearTest {

    @Test
    public void clearSortDisplayOverrides_clearsAll() {
        TableDTO t = new TableDTO();
        t.setName("T");
        t.setComment("c");
        t.setSortDisplayOverride("override");

        NamespaceDTO ns = new NamespaceDTO();
        ns.setTables(List.of(t));

        DataSourceDTO ds = new DataSourceDTO();
        ds.setNamespaces(List.of(ns));

        TableOrderInputKit.clearSortDisplayOverrides(List.of(ds));

        assertNull(t.getSortDisplayOverride());
        assertEquals("c", t.getComment());
    }
}
