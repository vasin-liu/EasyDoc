/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 */
package org.gensokyo.plugin.easydoc.kit;

import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.dto.DataSourceDTO;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.dto.TableDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 解析多行 / TXT / 拖曳列表中的表顺序，以及每表可选的「表名|文档显示名」。
 * <p>
 * 解析结果在生成前作用于 {@link TableDTO#setSortDisplayOverride}；不写入数据库与项目文件。
 */
public final class TableOrderInputKit {

    private TableOrderInputKit() {
    }

    /**
     * 每行：表名，或 表名|显示名。首段 {@code |} 分割表名与显示名；显示名中可含 {@code |}。空行、# 开头行为注释行。
     */
    public static Result parseLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new Result(List.of(), Map.of());
        }
        LinkedHashSet<String> orderNames = new LinkedHashSet<>();
        Map<String, String> display = new HashMap<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int pipe = trimmed.indexOf('|');
            if (pipe < 0) {
                orderNames.add(trimmed);
            } else {
                String name = trimmed.substring(0, pipe).trim();
                String disp = pipe + 1 <= trimmed.length() ? trimmed.substring(pipe + 1).trim() : "";
                if (name.isEmpty()) {
                    continue;
                }
                orderNames.add(name);
                if (StringUtils.isNotBlank(disp)) {
                    String key = name.toLowerCase(Locale.ROOT);
                    display.putIfAbsent(key, disp);
                }
            }
        }
        return new Result(new ArrayList<>(orderNames), display.isEmpty() ? Map.of() : Collections.unmodifiableMap(display));
    }

    /**
     * 在根据排序面板重算表顺序前调用，清除所有表上的仅本次说明显示，避免同对话框内多次「生成」残留内存态（例如先启用覆写再关闭排序）。
     */
    public static void clearSortDisplayOverrides(Iterable<DataSourceDTO> dataSources) {
        if (dataSources == null) {
            return;
        }
        for (DataSourceDTO ds : dataSources) {
            if (ds == null || ds.getNamespaces() == null) {
                continue;
            }
            for (NamespaceDTO ns : ds.getNamespaces()) {
                if (ns == null || ns.getTables() == null) {
                    continue;
                }
                for (TableDTO t : ns.getTables()) {
                    if (t != null) {
                        t.setSortDisplayOverride(null);
                    }
                }
            }
        }
    }

    /**
     * @param order                   表名顺序（去重，先出现者先）
     * @param displayByTableNameLower 说明显示覆写，键为表名小写；同键不区分不同 schema
     */
    public record Result(List<String> order, Map<String, String> displayByTableNameLower) {
    }
}
