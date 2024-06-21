/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.ui.component;

import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.EditableModel;
import lombok.*;
import org.gensokyo.plugin.easydoc.dto.Item;
import org.gensokyo.plugin.easydoc.factory.AbstractItemFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.util.List;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 通用表格模型
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/19 , Version 1.0.0
 */
public class CommonTableModel<T extends Item<T>> extends DefaultTableModel implements EditableModel {
    /**
     * 列信息
     */
    private final List<Column<T>> columns;
    /**
     * 表数据
     */
    private List<T> dataList;
    /**
     * 表格
     */
    @Getter
    private JBTable table;

    private final Class<T> cls;

    @Setter
    private boolean disableAddAction = true;
    @Setter
    private boolean disableRemoveAction = true;
    @Setter
    private boolean disableUpAction = true;
    @Setter
    private boolean disableDownAction = true;

    public CommonTableModel(@NonNull List<Column<T>> columns, @NonNull List<T> dataList, Class<T> cls) {
        this.columns = columns;
        this.dataList = dataList;
        this.cls = cls;
        this.initColumnName();
        this.initTable();
        this.setDataList(dataList);
    }

    public JComponent createPanel() {
        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(this.table);
        if (disableAddAction) {
            decorator.disableAddAction();
        }
        if (disableRemoveAction) {
            decorator.disableRemoveAction();
        }
        if (disableDownAction) {
            decorator.disableDownAction();
        }
        if (disableUpAction) {
            decorator.disableUpAction();
        }
        return decorator.createPanel();
    }

    private void initColumnName() {
        for (Column<T> column : this.columns) {
            addColumn(column.name);
        }
    }

    private void initTable() {
        this.table = new JBTable(this);
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 指定编辑器
        for (Column<T> column : this.columns) {
            if (column.renderer != null) {
                this.table.getColumn(column.name).setCellRenderer(column.renderer);
            }
            if (column.editor != null) {
                this.table.getColumn(column.name).setCellEditor(column.editor);
            }
        }
    }

    private Vector<String> toObj(T e) {
        Vector<String> vector = new Vector<>();
        this.columns.stream().map(item -> item.getFun.apply(e)).forEach(vector::add);
        return vector;
    }

    public void setDataList(List<T> dataList) {
        this.dataList = dataList;
        // 清空数据
        removeAllRow();
        for (T entity : this.dataList) {
            addRow(entity);
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (row < this.dataList.size()) {
            super.setValueAt(value, row, column);
            T obj = this.dataList.get(row);
            this.columns.get(column).getSetFun().accept(obj, (String) value);
        }
    }

    /**
     * 移除所有行
     */
    public void removeAllRow() {
        int rowCount = getRowCount();
        for (int i = 0; i < rowCount; i++) {
            super.removeRow(0);
        }
    }

    @Override
    public void removeRow(int row) {
        super.removeRow(row);
        this.dataList.remove(row);
    }

    public void addRow(T entity) {
        addRow(toObj(entity));
    }

    @Override
    public void addRow() {
        T entity = AbstractItemFactory.createDefaultVal(cls);
        this.dataList.add(entity);
        addRow(entity);
    }

    @Override
    public void exchangeRows(int oldIndex, int newIndex) {
        super.moveRow(oldIndex, oldIndex, newIndex);
        T remove = this.dataList.remove(oldIndex);
        this.dataList.add(newIndex, remove);
    }

    @Override
    public boolean canExchangeRows(int oldIndex, int newIndex) {
        return true;
    }


    @Data
    @AllArgsConstructor
    public static class Column<T> {
        /**
         * 列名
         */
        private String name;
        /**
         * get方法
         */
        private Function<T, String> getFun;
        /**
         * set方法
         */
        private BiConsumer<T, String> setFun;
        /**
         * 列编辑器
         */
        private TableCellEditor editor;
        /**
         * 列展示器
         */
        private TableCellRenderer renderer;
    }
}
