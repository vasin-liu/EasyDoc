package org.gensokyo.plugin.easydoc.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.constant.Const;
import org.gensokyo.plugin.easydoc.dto.DataSourceDTO;
import org.gensokyo.plugin.easydoc.dto.DefaultTemplateTypeDTO;
import org.gensokyo.plugin.easydoc.dto.DocOptions;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.factory.AbstractCellEditorFactory;
import org.gensokyo.plugin.easydoc.kit.DocKit;
import org.gensokyo.plugin.easydoc.kit.ProjectKit;
import org.gensokyo.plugin.easydoc.kit.TableKit;
import org.gensokyo.plugin.easydoc.ui.component.CommonTableModel;
import org.gensokyo.plugin.easydoc.ui.component.WordFileChooserDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 选择保存路径
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/11 , Version 1.0.0
 */
public class MainDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(MainDialog.class);
    /**
     * 主面板
     */
    private JPanel contentPane;
    /**
     * 模板面板
     */
    private JPanel templatePanel;
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
    private JTextField savePathTf;
    private JRadioButton defaultTemplateRb;
    private JRadioButton customTemplateRb;
    private JTextField docTitleTf;
    private JTextField docVersionTf;
    private JTextField docAuthorTf;
    private JPanel databasePanel;
    private JButton helpBtn;
    private JBTable table;
    /**
     * 默认模板下拉框
     */
    private JComboBox<DefaultTemplateTypeDTO> defaultTemplateCb;

    /**
     * 项目对象
     */
    private final Project project;

    private final Collection<DataSourceDTO> dataSources;


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.contentPane;
    }

    /**
     * 构造方法
     */
    public MainDialog(Project project, Collection<DataSourceDTO> dataSources) {
        super(project);
        this.project = project;
        this.dataSources = dataSources;
        this.initEvent();
        this.initUi();
        init();
        setTitle(Const.TITLE);
    }

    private void initUi() {
        createHelpButton();
        showDefaultTemplatePanel();
        showDatabasePanel();
    }

    private void createHelpButton() {
        // 设置 ? 图标
        Icon icon = UIManager.getIcon("OptionPane.questionIcon");
        Icon scaledIcon = IconUtil.scale(icon, null, 0.8f);
        helpBtn.setIcon(scaledIcon);
        helpBtn.setHorizontalAlignment(SwingConstants.RIGHT);
        helpBtn.setToolTipText("点击查看模板说明");
        helpBtn.setPreferredSize(new Dimension(scaledIcon.getIconWidth(), scaledIcon.getIconHeight()));
        helpBtn.setBorderPainted(false);
        helpBtn.setContentAreaFilled(false);
    }

    private void showDefaultTemplatePanel() {
        templatePanel.removeAll();
        defaultTemplateCb = new ComboBox<>(Const.DEFAULT_TEMPLATE_TYPES);
        defaultTemplateCb.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
                    JLabel label = new JLabel();
                    if (value != null) {
                        label.setText(value.name());
                    }
                    if (isSelected) {
                        label.setBackground(list.getSelectionBackground());
                        label.setForeground(list.getSelectionForeground());
                    } else {
                        label.setBackground(list.getBackground());
                        label.setForeground(list.getForeground());
                    }
                    label.setOpaque(true);
                    return label;
                }
        );
        templatePanel.add(defaultTemplateCb, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        this.contentPane.updateUI();
    }

    private void showCustomTemplatePanel() {
        templatePanel.removeAll();
        fromTextField = new JTextField();
        fromTextField.setEditable(false);
        fromTextField.setEnabled(false);
        templatePanel.add(fromTextField, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        JButton button = getButton();

        templatePanel.add(button, new GridConstraints(0, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        this.contentPane.updateUI();
    }

    private @NotNull JButton getButton() {
        JButton button = new JButton("选择");
        //选择路径
        button.addActionListener(e -> {
            //将当前选中的model设置为基础路径
            VirtualFile path = ProjectKit.getBaseDir(project);
            VirtualFile virtualFile = FileChooser.chooseFile(new WordFileChooserDescriptor(), project, path);
            if (virtualFile != null) {
                fromTextField.setText(virtualFile.getPath());
            }
        });
        return button;
    }

    private void showDatabasePanel() {
        // 第一列，数据库名称
        TableCellEditor nameValueEditor = AbstractCellEditorFactory.createTextFieldEditor(false);
        CommonTableModel.Column<NamespaceDTO> nameValueColumn =
                new CommonTableModel.Column<>("数据库名", NamespaceDTO::getName, NamespaceDTO::setName, nameValueEditor, null);
        // 第二列，数据库描述
        TableCellEditor commentValueEditor = AbstractCellEditorFactory.createTextFieldEditor(true);
        CommonTableModel.Column<NamespaceDTO> commentValueColumn =
                new CommonTableModel.Column<>("数据库描述", NamespaceDTO::getComment, NamespaceDTO::setComment, commentValueEditor, null);

        List<CommonTableModel.Column<NamespaceDTO>> columns = Arrays.asList(nameValueColumn, commentValueColumn);
        CommonTableModel<NamespaceDTO> tableModel = new CommonTableModel<>(columns,
                dataSources.stream().flatMap(ds -> ds.getNamespaces().stream()).toList(), NamespaceDTO.class);
        table = tableModel.getTable();
        this.databasePanel.add(tableModel.createPanel(), new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private void initEvent() {
        //默认模板
        defaultTemplateRb.addActionListener(e -> showDefaultTemplatePanel());
        //自定义模板
        customTemplateRb.addActionListener(e -> showCustomTemplatePanel());
        //选择路径
        pathChooseButton.addActionListener(e -> {
            //将当前选中的model设置为基础路径
            VirtualFile path = ProjectKit.getBaseDir(project);
            VirtualFile virtualFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, path);
            if (virtualFile != null) {
                savePathTf.setText(virtualFile.getPath());
            }
        });
        //模板帮助说明
        helpBtn.addActionListener(e -> new HelpDialog().show());
    }

    @Override
    protected void doOKAction() {
        String savePath = savePathTf.getText();
        if (StringUtils.isEmpty(savePath)) {
            Messages.showWarningDialog("请选择生成文档保存路径！", Const.TITLE);
            return;
        }

        if (CollectionUtils.isEmpty(dataSources)) {
            Messages.showWarningDialog("请至少选择一个数据库！", Const.TITLE);
            return;
        }

        String docTitle = docTitleTf.getText();
        if (StringUtils.isEmpty(docTitle)) {
            Messages.showWarningDialog("请输入文档标题！", Const.TITLE);
            docTitleTf.requestFocus();
            return;
        }

        String docAuthor = docAuthorTf.getText();
        if (StringUtils.isEmpty(docAuthor)) {
            Messages.showWarningDialog("请输入文档作者！", Const.TITLE);
            docAuthorTf.requestFocus();
            return;
        }

        String docVersion = docVersionTf.getText();
        if (StringUtils.isEmpty(docVersion)) {
            Messages.showWarningDialog("请输入文档版本！", Const.TITLE);
            docVersionTf.requestFocus();
            return;
        }

        InputStream is = getTemplateFileStream();
        if (Objects.isNull(is)) {
            return;
        }

        savePath = savePath + File.separator + docTitle + ".docx";

        fillDatabaseComments();

        List<NamespaceDTO> namespaces = dataSources.stream().flatMap(ds -> ds.getNamespaces().stream()).toList();

        DocOptions opts = DocOptions.of()
                .template(is)
                .title(docTitle)
                .author(docAuthor)
                .version(docVersion)
                .savePath(savePath)
                .dataSources(dataSources)
                .namespaces(namespaces);

        if (createDoc(opts)) {
            Messages.showInfoMessage("数据库文档生成成功", Const.TITLE);
            close(OK_EXIT_CODE);
        } else {
            Messages.showWarningDialog("无法生成数据库文档", Const.TITLE);
        }
    }

    private boolean createDoc(DocOptions opts) {
        setOKActionEnabled(false);
        final AtomicBoolean success = new AtomicBoolean(false);
        ProgressManager.getInstance().run(new Task.Modal(project, "生成文档中...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                success.set(DocKit.create(opts));
            }

            @Override
            public void onSuccess() {
               //nothing
            }

            @Override
            public void onCancel() {
                ApplicationManager.getApplication().invokeLater(() -> setOKActionEnabled(true));
            }

            @Override
            public void onFinished() {
                ApplicationManager.getApplication().invokeLater(() -> setOKActionEnabled(true));
            }
        });
        return success.get();
    }

    private void fillDatabaseComments() {
        List<Object[]> allRows = TableKit.getAllRows(table);
        for (DataSourceDTO ds : dataSources) {
            for (NamespaceDTO namespace : ds.getNamespaces()) {
                for (Object[] row : allRows) {
                    if (namespace.getName().equals(row[0])) {
                        namespace.setComment(Objects.nonNull(row[1]) ? row[1].toString() : "");
                    }
                }
            }
        }
    }

    private InputStream getTemplateFileStream() {
        try {
            if (defaultTemplateRb.isSelected()) {
                DefaultTemplateTypeDTO selectedItem = (DefaultTemplateTypeDTO) defaultTemplateCb.getSelectedItem();
                return DocKit.class.getResourceAsStream(Objects.requireNonNull(selectedItem).path());
            } else {
                return new FileInputStream(fromTextField.getText());
            }
        } catch (Exception e) {
            LOG.error("获取模板文件错误，请检查模板文件是否存在或者路径是否正确！", e);
            Messages.showWarningDialog("获取模板文件错误，请检查模板文件是否存在或者路径是否正确！", Const.TITLE);
        }
        return null;
    }

    /**
     * 自定义UI创建，不能修改方法名称
     */
    private void createUIComponents() {
        this.templatePanel = new JPanel();
        this.templatePanel.setLayout(new GridLayoutManager(1, 2, JBUI.emptyInsets(), -1, -1));
    }
}
