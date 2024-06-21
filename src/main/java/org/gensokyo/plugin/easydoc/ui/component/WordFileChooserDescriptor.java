/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.ui.component;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.constant.Const;

import java.util.Objects;

/**
 * word文档文件选择器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/20 , Version 1.0.0
 */
public class WordFileChooserDescriptor extends FileChooserDescriptor{

    public WordFileChooserDescriptor() {
        super(true, false, false, false, false, false);
    }

    @Override
    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        if (!file.isDirectory()) {
            String extension = file.getExtension();
            return isWordFile(extension);
        }
        return super.isFileVisible(file, showHiddenFiles);
    }

    @Override
    public boolean isFileSelectable(VirtualFile file) {
        if (Objects.nonNull(file) && !file.isDirectory()) {
            String extension = file.getExtension();
            return isWordFile(extension);
        }
        return super.isFileSelectable(file);
    }

    private boolean isWordFile(String extension) {
        return StringUtils.isNotBlank(extension)
                && (extension.equalsIgnoreCase(Const.FILE_EXT_DOC) || extension.equalsIgnoreCase(Const.FILE_EXT_DOCX));
    }
}
