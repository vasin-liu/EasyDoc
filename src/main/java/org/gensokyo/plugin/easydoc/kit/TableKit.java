/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.intellij.ui.table.JBTable;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.ui.component.CommonTableModel;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 表格工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/19 , Version 1.0.0
 */
public class TableKit {
    private TableKit() {
        throw new UnsupportedOperationException();
    }

    public static List<Object[]> getAllRows(JBTable table) {
        List<Object[]> rows = new ArrayList<>();
        CommonTableModel<NamespaceDTO> model = (CommonTableModel<NamespaceDTO>) table.getModel();
        int rowCount = model.getRowCount();
        int colCount = model.getColumnCount();

        for (int row = 0; row < rowCount; row++) {
            Object[] rowData = new Object[colCount];
            for (int col = 0; col < colCount; col++) {
                rowData[col] = model.getValueAt(row, col);
            }
            rows.add(rowData);
        }
        return rows;
    }
}
