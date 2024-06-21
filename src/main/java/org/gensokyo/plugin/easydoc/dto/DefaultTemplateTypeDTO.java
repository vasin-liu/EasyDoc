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
 * 默认模板类型
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/12 , Version 1.0.0
 */
@Data
@Builder(builderMethodName = "of")
@Accessors(fluent = true, chain = true)
public class DefaultTemplateTypeDTO {

    private String name;

    private String path;
}
