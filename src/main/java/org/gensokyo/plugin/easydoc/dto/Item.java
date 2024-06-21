/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import org.gensokyo.plugin.easydoc.kit.JsonKit;

/**
 * 条目数据接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/19 , Version 1.0.0
 */
public interface Item<T extends Item<T>> {

    /**
     * 默认值
     *
     * @return {@link T}
     */
    default T defaultVal() {
        return null;
    }

    default Item<T> copy() {
        return JsonKit.copy(this);
    }
}
