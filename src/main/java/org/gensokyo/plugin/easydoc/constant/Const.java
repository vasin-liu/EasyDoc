/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.constant;

import org.gensokyo.plugin.easydoc.dto.DefaultTemplateTypeDTO;

/**
 * 常量类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/6 , Version 1.0.0
 */
public class Const {

    public static final String TITLE = "EasyDoc";

    public static final String VERSION = "1.0.0";

    public static final String AUTHOR = "Gensokyo V.L.";

    public static final String FILE_EXT_DOC = "doc";
    public static final String FILE_EXT_DOCX = "docx";

    /**
     * 插件配置文件名称
     */
    public static final String PLUGIN_SETTING_FILE_NAME = "easy-doc-setting.xml";

    public static final DefaultTemplateTypeDTO[] DEFAULT_TEMPLATE_TYPES = new DefaultTemplateTypeDTO[] {
            DefaultTemplateTypeDTO.of()
                    .name("默认模板")
                    .path("/default_v1.0.0.docx")
                    .build()
    };
}
