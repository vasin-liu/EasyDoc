/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import org.gensokyo.plugin.easydoc.dto.ColumnDTO;
import org.gensokyo.plugin.easydoc.dto.TableDTO;
import org.gensokyo.plugin.easydoc.ui.MainDialog;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/7 , Version 1.0.0
 */
public class EasyDocTests {

    private static TableDTO createTableDTO() {
        TableDTO table1 = new TableDTO();
        table1.setName("test");
        table1.setComment("test");
        table1.setPrimaryKey("ID");
        table1.setSchema("test");

        List<ColumnDTO> cols = new ArrayList<>();
        ColumnDTO col1 = new ColumnDTO();
        col1.setName("ID");
        col1.setComment("ID");
        col1.setPrimaryKey(true);
        col1.setNotNull(true);
        col1.setDefaultValue(null);
        col1.setType("int");
        cols.add(col1);

        ColumnDTO col2 = new ColumnDTO();
        col2.setName("NAME");
        col2.setComment("NAME");
        col2.setPrimaryKey(false);
        col2.setNotNull(false);
        col2.setDefaultValue("");
        col2.setType("varchar");
        cols.add(col2);

        table1.setColumns(cols);
        return table1;
    }

    public static void main(String[] args) {
        List<TableDTO> tables = new ArrayList<>();
        tables.add(createTableDTO());
        tables.add(createTableDTO());

        // 生成文档
        try {
            LoopRowTableRenderPolicy policy = new LoopRowTableRenderPolicy();

            Configure config = Configure.builder()
                    .bind("columns", policy)
                    .useSpringEL()
                    .build();
            Map<String, Object> map = new HashMap<>() {{
                put("tables", tables);
            }};
            InputStream stream = MainDialog.class.getResourceAsStream("/default.docx");
            XWPFTemplate.compile(stream, config)
                    .render(map)
                    .writeToFile("E:\\Desktop\\db.docx");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
