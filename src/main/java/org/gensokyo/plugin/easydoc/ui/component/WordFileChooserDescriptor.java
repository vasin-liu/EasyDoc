/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.ui.component;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * word文档文件选择器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/20 , Version 1.0.0
 */
public class WordFileChooser {
    public static void chooseWordFile() {
        // 创建文件选择描述符，只允许选择 Word 文档 (.doc 和 .docx)
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
            @Override
            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                if (!file.isDirectory()) {
                    String extension = file.getExtension();
                    return extension != null && (extension.equalsIgnoreCase("doc") || extension.equalsIgnoreCase("docx"));
                }
                return super.isFileVisible(file, showHiddenFiles);
            }

            @Override
            public boolean isFileSelectable(VirtualFile file) {
                if (!file.isDirectory()) {
                    String extension = file.getExtension();
                    return extension != null && (extension.equalsIgnoreCase("doc") || extension.equalsIgnoreCase("docx"));
                }
                return super.isFileSelectable(file);
            }
        };

        descriptor.setTitle("Select Word Document");
        descriptor.setDescription("Please select a Word document (.doc or .docx)");

        // 打开文件选择器
        VirtualFile file = FileChooser.chooseFile(descriptor, null, null);
        if (file != null) {
            // 处理用户选择的文件
            String filePath = file.getPath();
            Messages.showInfoMessage("You selected: " + filePath, "File Selected");
        } else {
            Messages.showWarningDialog("No file selected", "Warning");
        }
    }
}
