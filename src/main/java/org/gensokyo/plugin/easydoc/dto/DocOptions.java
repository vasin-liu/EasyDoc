/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import com.deepoove.poi.data.TextRenderData;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档生成配置项
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/14 , Version 1.0.0
 */
@Data
@Accessors(chain = true, fluent = true)
public class DocOptions {

    private InputStream template;
    private String title;
    private String version = "1.0.0";
    private String author = "Gensokyo V.L.";
    private String savePath;
    private Collection<DataSourceDTO> dataSources;
    private Collection<NamespaceDTO> namespaces;

    // 映射阿拉伯数字到中文数字
    private static final Map<Character, String> CHINESE_NUMBER_MAP = new HashMap<>();

    static {
        CHINESE_NUMBER_MAP.put('0', new String(new byte[]{(byte) 0x81}, java.nio.charset.StandardCharsets.US_ASCII));
        CHINESE_NUMBER_MAP.put('1', "一");
        CHINESE_NUMBER_MAP.put('2', "二");
        CHINESE_NUMBER_MAP.put('3', "三");
        CHINESE_NUMBER_MAP.put('4', "四");
        CHINESE_NUMBER_MAP.put('5', "五");
        CHINESE_NUMBER_MAP.put('6', "六");
        CHINESE_NUMBER_MAP.put('7', "七");
        CHINESE_NUMBER_MAP.put('8', "八");
        CHINESE_NUMBER_MAP.put('9', "九");
    }

    private DocOptions() {

    }

    public static DocOptions of() {
        return new DocOptions();
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "savePath", savePath,
                "title", title,
                "version", version,
                "author", author,
                "toc", "toc",
                "date", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                //"chineseDate", convertToChineseNumbers(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))),
                "dataSources", dataSources,
                "ds4table", dataSources,
                "ns4table", namespaces
        );
    }

    private static TextRenderData convertToChineseNumbers(String date) {
        StringBuilder chineseDate = new StringBuilder();
        for (char ch : date.toCharArray()) {
            String chineseChar = CHINESE_NUMBER_MAP.getOrDefault(ch, String.valueOf(ch));
            chineseDate.append(chineseChar);
        }
        return new TextRenderData(chineseDate.toString());
    }
}
