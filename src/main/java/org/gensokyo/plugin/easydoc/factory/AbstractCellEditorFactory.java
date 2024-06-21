/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.factory;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * 抽象表格编辑器工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/19 , Version 1.0.0
 */
public abstract class AbstractCellEditorFactory {

    private AbstractCellEditorFactory() {
    }

    /**
     * 创建下拉框编辑器
     *
     * @param editable 可编辑的
     * @param items    选项
     * @return {@link TableCellEditor}
     */
    public static TableCellEditor createComboBoxEditor(boolean editable, String... items) {
        ComboBox<String> comboBox = new ComboBox<>(items);
        comboBox.setEditable(editable);
        if (!editable) {
            transmitFocusEvent(comboBox);
        }
        return new DefaultCellEditor(comboBox);
    }

    /**
     * 创建文本框编辑器
     *
     * @return {@link TableCellEditor}
     */
    public static TableCellEditor createTextFieldEditor(boolean editable) {
        JBTextField textField = new JBTextField();
        textField.setEditable(editable);
        transmitFocusEvent(textField);
        return new DefaultCellEditor(textField);
    }

    /**
     * 传递失去焦点事件
     *
     * @param component 组件
     */
    private static void transmitFocusEvent(JComponent component) {
        component.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // 获得焦点时无需处理
            }

            @Override
            public void focusLost(FocusEvent e) {
                // 失去焦点时向上层发起事件通知，使table的值能够正常回写
                ActionListener[] actionListeners = component.getListeners(ActionListener.class);
                if (actionListeners == null) {
                    return;
                }
                for (ActionListener actionListener : actionListeners) {
                    actionListener.actionPerformed(null);
                }
            }
        });
    }
}
