package org.gensokyo.plugin.easydoc.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.constant.Const;
import org.gensokyo.plugin.easydoc.dto.DefaultTemplateTypeDTO;
import org.gensokyo.plugin.easydoc.dto.DocOptions;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.kit.DocKit;
import org.gensokyo.plugin.easydoc.kit.ProjectKit;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collection;

/**
 * 选择保存路径
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/11 , Version 1.0.0
 */
public class SelectSavePath extends DialogWrapper {
    /**
     * 主面板
     */
    private JPanel contentPane;
    /**
     * 输入面板
     */
    private JPanel inputPanel;
    /**
     * 模板面板
     */
    private JPanel templatePanel;
    /**
     * 输出面板
     */
    private JPanel outputPanel;
    /**
     * 路径选择按钮
     */
    private JButton pathChooseButton;
    /**
     * 文件读取路径字段
     */
    private JTextField fromTextField;
    /**
     * 文件保存路径字段
     */
    private JTextField toTextField;

    /**
     * 默认模板类型按钮
     */
    private JRadioButton templateTypeDefaultRadio;
    /**
     * 自定义模板类型按钮
     */
    private JRadioButton templateTypeCustomRadio;
    /**
     * 默认模板下拉框
     */
    private JComboBox<DefaultTemplateTypeDTO> defaultTemplateCombo;

    /**
     * 项目对象
     */
    private final Project project;

    /**
     * 选择表列表
     */
    private final Collection<NamespaceDTO> namespaces;


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.contentPane;
    }

    /**
     * 构造方法
     */
    public SelectSavePath(Project project, Collection<NamespaceDTO> namespaces) {
        super(project);
        this.project = project;
        this.namespaces = namespaces;
        createInputPanel();
        this.initEvent();
        init();
        setTitle(Const.TITLE);
    }

    private void createInputPanel() {
        JPanel north = new JPanel(new GridLayoutManager(1, 2, new Insets(10, 10, 10, 10), 10, 10));
        templatePanel = new JPanel(new BorderLayout());
        inputPanel.add(north, BorderLayout.NORTH);
        inputPanel.add(templatePanel, BorderLayout.CENTER);

        templateTypeDefaultRadio = new JRadioButton("Default");
        templateTypeDefaultRadio.setSelected(true);
        templateTypeCustomRadio = new JRadioButton("Custom");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(templateTypeDefaultRadio);
        buttonGroup.add(templateTypeCustomRadio);

        north.add(templateTypeDefaultRadio, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null,
                0, false));
        north.add(templateTypeCustomRadio, new GridConstraints(0, 1, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null,
                0, false));
        createDefaultTemplatePanel();
    }

    private void createDefaultTemplatePanel() {
        templatePanel.removeAll();
        templatePanel.add(new JLabel("Template Type："), BorderLayout.WEST);
        defaultTemplateCombo = new JComboBox<>();
        templatePanel.add(defaultTemplateCombo, BorderLayout.CENTER);
        inputPanel.add(templatePanel);
        this.contentPane.updateUI();
    }

    private void createCustomTemplatePanel() {
        templatePanel.removeAll();
        templatePanel.add(new JLabel("Template Path："), BorderLayout.WEST);
        fromTextField = new JTextField();
        templatePanel.add(fromTextField, BorderLayout.CENTER);
        JButton button = new JButton("Choose");
        //选择路径
        button.addActionListener(e -> {
            //将当前选中的model设置为基础路径
            VirtualFile path = ProjectKit.getBaseDir(project);
            VirtualFile virtualFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, path);
            if (virtualFile != null) {
                fromTextField.setText(virtualFile.getPath());
            }
        });

        templatePanel.add(button, BorderLayout.EAST);
        inputPanel.add(templatePanel);
        this.contentPane.updateUI();
    }

    private void initEvent() {
        //默认模板
        templateTypeDefaultRadio.addActionListener(e -> {
            createDefaultTemplatePanel();
        });
        //自定义模板
        templateTypeCustomRadio.addActionListener(e -> {
            createCustomTemplatePanel();
        });
        //选择路径
        pathChooseButton.addActionListener(e -> {
            //将当前选中的model设置为基础路径
            VirtualFile path = ProjectKit.getBaseDir(project);
            VirtualFile virtualFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, path);
            if (virtualFile != null) {
                toTextField.setText(virtualFile.getPath());
            }
        });
    }

    @Override
    protected void doOKAction() {
        ok();
        super.doOKAction();
    }

    /**
     * 确认按钮回调事件
     */
    private void ok() {
        String savePath = toTextField.getText();
        if (StringUtils.isEmpty(savePath)) {
            Messages.showWarningDialog("Can't select save path!", Const.TITLE);
            return;
        }

        if (CollectionUtils.isEmpty(namespaces)) {
            Messages.showWarningDialog("Please select at least one table or database!", Const.TITLE);
            return;
        }

        savePath = savePath + File.separator + "1.docx";

        DocOptions opts = DocOptions.of()
                        .savePath(savePath);
        DocKit.create(namespaces, opts);
    }
}
