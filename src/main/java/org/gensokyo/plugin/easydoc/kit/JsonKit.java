/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/19 , Version 1.0.0
 */
public class JsonKit {

    private static final ObjectMapper OM = new ObjectMapper();

    public static <T> T copy(T src) {
        try {
            // 将对象序列化为 JSON 字符串
            String json = OM.writeValueAsString(src);
            // 反序列化 JSON 字符串为对象
            return OM.readValue(json, new TypeReference<T>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to copy object", e);
        }
    }

    public static <T> T read(String json) {
        try {
            // 反序列化 JSON 字符串为对象
            return OM.readValue(json, new TypeReference<T>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to read json", e);
        }
    }

    public static <T> String write(T obj) {
        try {
            // 序列化对象为 JSON 字符串
            return OM.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to write object", e);
        }
    }
}
