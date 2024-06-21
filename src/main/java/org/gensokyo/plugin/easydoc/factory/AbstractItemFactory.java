/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.factory;

import org.gensokyo.plugin.easydoc.dto.Item;

import java.lang.reflect.InvocationTargetException;

/**
 * 抽象条目工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/19 , Version 1.0.0
 */
public abstract class AbstractItemFactory {

    private AbstractItemFactory() {
    }

    public static <T extends Item<T>> T createDefaultVal(Class<T> cls) {
        try {
            T instance = cls.getDeclaredConstructor().newInstance();
            return instance.defaultVal();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new IllegalArgumentException("构建示例失败", e);
        }
    }
}
