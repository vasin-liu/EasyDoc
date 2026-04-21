/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.ui;

import com.intellij.ide.fileTemplates.impl.UrlUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.kit.DocKit;
import org.gensokyo.plugin.easydoc.kit.I18nKit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

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
    private JScrollPane scrollPane;

    public HelpDialog() {
        super(true);
        init();
        setTitle(I18nKit.t("help.title"));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected Action @NotNull [] createActions() {
        Action closeAction = new DialogWrapperAction(I18nKit.t("help.close")) {
            @Override
            protected void doAction(ActionEvent e) {
                doOKAction();
            }
        };
        return new Action[]{closeAction};
    }

    private void createUIComponents() {
        this.editorPane = new JEditorPane();
        editorPane.setEditable(false);

        try {
            String content = UrlUtil.loadText(Objects.requireNonNull(DocKit.class.getResource(resolveHelpResource())));
            editorPane.setContentType("text/html");
            if (StringUtils.isNotBlank(content)) {
                editorPane.setText(content);
            } else {
                editorPane.setText(I18nKit.t("help.notfound"));
            }
        } catch (IOException e) {
            editorPane.setText(I18nKit.t("help.openfail"));
        }

        this.scrollPane = new JBScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        // Ensure scroll to top
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            verticalScrollBar.setValue(verticalScrollBar.getMinimum());
        });

        setSize(800, 400);
        // Ensure the dialog is packed and centered
        pack();
    }

    private String resolveHelpResource() {
        Locale locale = I18nKit.getLocale();
        if (Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
            return "/help_en.html";
        }
        return "/help_zh_CN.html";
    }
}
