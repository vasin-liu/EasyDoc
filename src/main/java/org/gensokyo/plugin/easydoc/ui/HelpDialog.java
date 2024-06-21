/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.ui.component;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 帮助界面
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/20 , Version 1.0.0
 */
public class HelpDialog extends DialogWrapper {
    private JPanel contentPane;
    private JEditorPane editorPane;

    public HelpDialog() {
        super(true);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return null;
    }

    protected HtmlViewerDialog() {
        super(true); // use current window as parent
        init();
        setTitle("HTML Viewer");

        editorPane = new JEditorPane();
        editorPane.setEditable(false);

        try {
            URL url = getClass().getResource("/help.html");
            if (url != null) {
                editorPane.setPage(url);
            } else {
                editorPane.setText("<html><body>File not found: help.html</body></html>");
            }
        } catch (IOException e) {
            editorPane.setText("<html><body>Error loading help.html</body></html>");
        }

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        contentPane = new JPanel(new BorderLayout());
        contentPane.add(scrollPane, BorderLayout.CENTER);
    }
}
