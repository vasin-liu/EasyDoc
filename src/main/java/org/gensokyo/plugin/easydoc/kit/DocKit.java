/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.deepoove.poi.plugin.toc.TOCRenderPolicy;
import com.intellij.openapi.diagnostic.Logger;
import org.gensokyo.plugin.easydoc.dto.DocOptions;

import java.util.Objects;

/**
 * 文档生成工具
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/14 , Version 1.0.0
 */
public class DocKit {

    private DocKit() {
        throw new UnsupportedOperationException();
    }


    public static boolean create(DocOptions opts) {
        Configure config = Configure.builder()
                .bind("ds4table", new LoopRowTableRenderPolicy())
                .bind("ns4table", new LoopRowTableRenderPolicy())
                .bind("columns", new LoopRowTableRenderPolicy())
                //目录
                .bind("toc", new TOCRenderPolicy())
                .useSpringEL()
                .setValidErrorHandler(handler -> Logger.getInstance(DocKit.class).error("值校验错误：" + handler.getData()))
                .build();

        // 生成文档
        try (var stream = Objects.requireNonNull(opts.template());
             var template = XWPFTemplate.compile(stream, config)) {
            template.render(opts.toMap())
                    .writeToFile(opts.savePath());
            return true;
        } catch (Exception e) {
            Logger.getInstance(DocKit.class).error(e);
        }
        return false;
    }
}
