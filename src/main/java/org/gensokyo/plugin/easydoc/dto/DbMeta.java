/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 数据库元数据信息
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/21 , Version 1.0.0
 */
@Data
@Accessors(fluent = true, chain = true)
@Builder(builderMethodName = "of")
public class DbMeta {

    private String dialect;

    private String version;
}
