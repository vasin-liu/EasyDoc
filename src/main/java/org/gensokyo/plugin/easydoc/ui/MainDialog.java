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
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.dto.DataSourceDTO;
import org.gensokyo.plugin.easydoc.dto.DefaultTemplateTypeDTO;
import org.gensokyo.plugin.easydoc.dto.DocOptions;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.dto.TableDTO;
import org.gensokyo.plugin.easydoc.factory.AbstractCellEditorFactory;
import org.gensokyo.plugin.easydoc.kit.DerivedCommentInheritanceKit;
import org.gensokyo.plugin.easydoc.kit.DocKit;
import org.gensokyo.plugin.easydoc.kit.I18nKit;
import org.gensokyo.plugin.easydoc.kit.ProjectKit;
import org.gensokyo.plugin.easydoc.kit.TableKit;
import org.gensokyo.plugin.easydoc.ui.component.CommonTableModel;
import org.gensokyo.plugin.easydoc.ui.component.JListReorderUtil;
import org.gensokyo.plugin.easydoc.ui.component.WordFileChooserDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    private JLabel languageLabel;
    private JComboBox<String> languageCb;
    private JLabel templateTypeLabel;
    private JLabel templateFileLabel;
    private JLabel databaseLabel;
    private JLabel docNameLabel;
    private JLabel authorLabel;
    private JLabel versionLabel;
    private JLabel savePathLabel;
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
    private JSplitPane splitPane;
    private JPanel tableOrderPanel;
    private JRadioButton orderDisabledRb;
    private JRadioButton orderFromInputRb;
    private JRadioButton orderFromFileRb;
    private JRadioButton orderFromDragRb;
    private JTextArea orderInputTa;
    private JTextField orderFileTf;
    private JPanel orderModePanel;
    private JScrollPane orderInputScrollPane;
    private JPanel orderFilePanel;
    private JButton orderFileChooseBtn;
    private JPanel orderCenterCard;
    private JScrollPane orderDragScrollPane;
    private JBList<String> orderTableList;
    private DefaultListModel<String> orderListModel;
    private boolean orderPanelExpanded;
    private static final int ORDER_PANEL_BASE_WIDTH = 360;
    private static final int COLLAPSED_DIALOG_WIDTH = 780;
    private static final int DIALOG_HEIGHT = 500;
    private int collapsedDialogWidth = -1;
    private Action sortToggleAction;
    private Timer panelAnimationTimer;
    private boolean languageUpdating;


    @Override
    protected @Nullable JComponent createCenterPanel() {
        if (splitPane == null) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setBorder(BorderFactory.createEmptyBorder());
            splitPane.setResizeWeight(0);
            splitPane.setContinuousLayout(true);
            splitPane.setLeftComponent(buildOrderConfigPanel());
            splitPane.setRightComponent(this.contentPane);
            splitPane.setDividerSize(0);
            splitPane.setDividerLocation(0);
            orderPanelExpanded = false;
        }
        return splitPane;
    }

    @Override
    public @Nullable Dimension getInitialSize() {
        return new Dimension(JBUI.scale(COLLAPSED_DIALOG_WIDTH), JBUI.scale(DIALOG_HEIGHT));
    }

    /**
     * 构造方法
     */
    public MainDialog(Project project, Collection<DataSourceDTO> dataSources) {
        super(project);
        I18nKit.setLocale(Locale.SIMPLIFIED_CHINESE);
        this.project = project;
        this.dataSources = dataSources;
        // CRITICAL: Release DB PSI/model references BEFORE UI initialization to allow
        // ModelMemoryManager to compact memory early, especially for 100+ tables/views.
        dataSources.forEach(DataSourceDTO::prepareForRender);
        this.initEvent();
        this.initUi();
        init();
        applyTexts();
        setTitle(I18nKit.t("app.title"));
    }

    private void initUi() {
        initLanguageSelector();
        createHelpButton();
        showDefaultTemplatePanel();
        showDatabasePanel();
    }

    private void initLanguageSelector() {
        languageUpdating = true;
        languageCb.removeAllItems();
        languageCb.addItem(I18nKit.t("lang.zh"));
        languageCb.addItem(I18nKit.t("lang.en"));
        languageCb.setSelectedIndex(0);
        languageUpdating = false;
    }

    private void createHelpButton() {
        // 设置 ? 图标
        Icon icon = UIManager.getIcon("OptionPane.questionIcon");
        Icon scaledIcon = IconUtil.scale(icon, null, 0.8f);
        helpBtn.setIcon(scaledIcon);
        helpBtn.setHorizontalAlignment(SwingConstants.RIGHT);
        helpBtn.setToolTipText(I18nKit.t("tooltip.help"));
        helpBtn.setPreferredSize(new Dimension(scaledIcon.getIconWidth(), scaledIcon.getIconHeight()));
        helpBtn.setBorderPainted(false);
        helpBtn.setContentAreaFilled(false);
    }

    private void showDefaultTemplatePanel() {
        templatePanel.removeAll();
        defaultTemplateCb = new ComboBox<>(defaultTemplates());
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

    private DefaultTemplateTypeDTO[] defaultTemplates() {
        return new DefaultTemplateTypeDTO[]{
                DefaultTemplateTypeDTO.of()
                        .name(I18nKit.t("template.default.name"))
                        .path("/default_v1.0.0.docx")
                        .build()
        };
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
        JButton button = new JButton(I18nKit.t("button.choose"));
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
        databasePanel.removeAll();
        // 第一列，数据库名称
        TableCellEditor nameValueEditor = AbstractCellEditorFactory.createTextFieldEditor(false);
        CommonTableModel.Column<NamespaceDTO> nameValueColumn =
                new CommonTableModel.Column<>(I18nKit.t("table.db.name"), NamespaceDTO::getName, NamespaceDTO::setName, nameValueEditor, null);
        // 第二列，数据库描述
        TableCellEditor commentValueEditor = AbstractCellEditorFactory.createTextFieldEditor(true);
        CommonTableModel.Column<NamespaceDTO> commentValueColumn =
                new CommonTableModel.Column<>(I18nKit.t("table.db.comment"), NamespaceDTO::getComment, NamespaceDTO::setComment, commentValueEditor, null);

        List<CommonTableModel.Column<NamespaceDTO>> columns = Arrays.asList(nameValueColumn, commentValueColumn);
        CommonTableModel<NamespaceDTO> tableModel = new CommonTableModel<>(columns,
                dataSources.stream().flatMap(ds -> ds.getNamespaces().stream()).toList(), NamespaceDTO.class);
        table = tableModel.getTable();
        this.databasePanel.add(tableModel.createPanel(), new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        this.databasePanel.updateUI();
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
        languageCb.addActionListener(e -> switchLanguage());
    }
    @Override
    protected Action[] createLeftSideActions() {
        sortToggleAction = new AbstractAction(I18nKit.t("button.sort.config")) {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleOrderPanel();
            }
        };
        return new Action[]{sortToggleAction};
    }

    private void switchLanguage() {
        if (languageUpdating) {
            return;
        }
        Locale locale = languageCb.getSelectedIndex() == 1 ? Locale.ENGLISH : Locale.SIMPLIFIED_CHINESE;
        I18nKit.setLocale(locale);
        applyTexts();
        showDatabasePanel();
        if (defaultTemplateRb.isSelected()) {
            showDefaultTemplatePanel();
        } else {
            showCustomTemplatePanel();
        }
    }

    private void applyTexts() {
        setTitle(I18nKit.t("app.title"));
        setOKButtonText(I18nKit.t("button.ok"));
        setCancelButtonText(I18nKit.t("button.cancel"));
        languageLabel.setText(I18nKit.t("lang.label"));
        templateTypeLabel.setText(I18nKit.t("label.template.type"));
        templateFileLabel.setText(I18nKit.t("label.template.file"));
        databaseLabel.setText(I18nKit.t("label.database"));
        docNameLabel.setText(I18nKit.t("label.doc.name"));
        authorLabel.setText(I18nKit.t("label.author"));
        versionLabel.setText(I18nKit.t("label.version"));
        savePathLabel.setText(I18nKit.t("label.save.path"));
        defaultTemplateRb.setText(I18nKit.t("radio.default"));
        customTemplateRb.setText(I18nKit.t("radio.custom"));
        pathChooseButton.setText(I18nKit.t("button.choose"));
        helpBtn.setToolTipText(I18nKit.t("tooltip.help"));
        languageUpdating = true;
        languageCb.removeAllItems();
        languageCb.addItem(I18nKit.t("lang.zh"));
        languageCb.addItem(I18nKit.t("lang.en"));
        languageCb.setSelectedIndex(Locale.ENGLISH.equals(I18nKit.getLocale()) ? 1 : 0);
        languageUpdating = false;
        refreshSortPanelTexts();
        refreshSortActionText();
    }


    @Override
    protected void doOKAction() {
        String savePath = savePathTf.getText();
        if (StringUtils.isEmpty(savePath)) {
            Messages.showWarningDialog(I18nKit.t("dialog.warn.save.path"), I18nKit.t("app.title"));
            return;
        }

        if (CollectionUtils.isEmpty(dataSources)) {
            Messages.showWarningDialog(I18nKit.t("dialog.warn.db.empty"), I18nKit.t("app.title"));
            return;
        }

        String docTitle = docTitleTf.getText();
        if (StringUtils.isEmpty(docTitle)) {
            Messages.showWarningDialog(I18nKit.t("dialog.warn.title.empty"), I18nKit.t("app.title"));
            docTitleTf.requestFocus();
            return;
        }

        String docAuthor = docAuthorTf.getText();
        if (StringUtils.isEmpty(docAuthor)) {
            Messages.showWarningDialog(I18nKit.t("dialog.warn.author.empty"), I18nKit.t("app.title"));
            docAuthorTf.requestFocus();
            return;
        }

        String docVersion = docVersionTf.getText();
        if (StringUtils.isEmpty(docVersion)) {
            Messages.showWarningDialog(I18nKit.t("dialog.warn.version.empty"), I18nKit.t("app.title"));
            docVersionTf.requestFocus();
            return;
        }

        InputStream is = getTemplateFileStream();
        if (Objects.isNull(is)) {
            return;
        }

        savePath = savePath + File.separator + docTitle + ".docx";

        fillDatabaseComments();

        List<NamespaceDTO> namespaces = dataSources.stream()
                .flatMap(ds -> ds.getNamespaces().stream())
                .toList();
        // 列注释：跨 schema 从同一数据源内所有物理表按列名（忽略大小写）回填到视图/物化视图
        DerivedCommentInheritanceKit.applyToDataSources(dataSources);
        if (!applyTableOrderFromPanel()) {
            return;
        }

        DocOptions opts = DocOptions.of()
                .template(is)
                .title(docTitle)
                .author(docAuthor)
                .version(docVersion)
                .savePath(savePath)
                .dataSources(dataSources)
                .namespaces(namespaces);

        if (createDoc(opts)) {
            Messages.showInfoMessage(I18nKit.t("dialog.success"), I18nKit.t("app.title"));
            close(OK_EXIT_CODE);
        } else {
            Messages.showWarningDialog(I18nKit.t("dialog.failed"), I18nKit.t("app.title"));
        }
    }

    private boolean createDoc(DocOptions opts) {
        setOKActionEnabled(false);
        final AtomicBoolean success = new AtomicBoolean(false);
        ProgressManager.getInstance().run(new Task.Modal(project, I18nKit.t("dialog.progress"), true) {
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

    private boolean applyTableOrderFromPanel() {
        if (orderDisabledRb == null || orderDisabledRb.isSelected()) {
            return true;
        }

        List<String> orderedTableNames;
        if (orderFromInputRb.isSelected()) {
            orderedTableNames = readTableOrderFromInput();
        } else if (orderFromFileRb.isSelected()) {
            orderedTableNames = readTableOrderFromTxt();
        } else if (orderFromDragRb != null && orderFromDragRb.isSelected()) {
            orderedTableNames = readTableOrderFromDrag();
        } else {
            return true;
        }

        if (orderedTableNames == null) {
            Messages.showWarningDialog(I18nKit.t("dialog.warn.order.invalid"), I18nKit.t("app.title"));
            return false;
        }
        if (orderedTableNames.isEmpty()) {
            Messages.showWarningDialog(I18nKit.t("dialog.warn.order.empty"), I18nKit.t("app.title"));
            return false;
        }
        applyTableOrder(orderedTableNames);
        return true;
    }

    private List<String> readTableOrderFromInput() {
        return parseTableOrderLines(splitLines(orderInputTa == null ? "" : orderInputTa.getText()));
    }

    private @NotNull List<String> readTableOrderFromDrag() {
        if (orderListModel == null) {
            return new ArrayList<>();
        }
        int n = orderListModel.getSize();
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(orderListModel.getElementAt(i));
        }
        return out;
    }

    private List<String> splitLines(String value) {
        return Arrays.asList(value.split("\\R"));
    }

    private List<String> readTableOrderFromTxt() {
        if (StringUtils.isBlank(orderFileTf == null ? null : orderFileTf.getText())) {
            return null;
        }
        try {
            List<String> lines = Files.readAllLines(Paths.get(orderFileTf.getText().trim()), StandardCharsets.UTF_8);
            return parseTableOrderLines(lines);
        } catch (Exception e) {
            LOG.error("读取表顺序文件失败: " + orderFileTf.getText(), e);
            Messages.showWarningDialog(I18nKit.t("dialog.warn.order.file"), I18nKit.t("app.title"));
            return null;
        }
    }

    private JPanel buildOrderConfigPanel() {
        tableOrderPanel = new JPanel(new BorderLayout(0, 8));
        tableOrderPanel.setBorder(JBUI.Borders.empty(8));

        orderModePanel = new JPanel(new GridLayout(4, 1, 0, 4));
        orderModePanel.setBorder(BorderFactory.createTitledBorder(I18nKit.t("sort.source")));
        orderDisabledRb = new JRadioButton(I18nKit.t("sort.disabled"), true);
        orderFromInputRb = new JRadioButton(I18nKit.t("sort.input"));
        orderFromFileRb = new JRadioButton(I18nKit.t("sort.file"));
        orderFromDragRb = new JRadioButton(I18nKit.t("sort.drag"));
        ButtonGroup sourceGroup = new ButtonGroup();
        sourceGroup.add(orderDisabledRb);
        sourceGroup.add(orderFromInputRb);
        sourceGroup.add(orderFromFileRb);
        sourceGroup.add(orderFromDragRb);
        orderModePanel.add(orderDisabledRb);
        orderModePanel.add(orderFromInputRb);
        orderModePanel.add(orderFromFileRb);
        orderModePanel.add(orderFromDragRb);

        orderInputTa = new JTextArea(10, 20);
        orderInputTa.setLineWrap(true);
        orderInputTa.setWrapStyleWord(true);
        orderInputScrollPane = new JScrollPane(orderInputTa);
        orderInputScrollPane.setBorder(BorderFactory.createTitledBorder(I18nKit.t("sort.input.title")));

        orderListModel = new DefaultListModel<>();
        for (String name : defaultTableNameOrder()) {
            orderListModel.addElement(name);
        }
        orderTableList = new JBList<>(orderListModel);
        orderTableList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String name = value == null ? "" : value.toString();
                String text = (index + 1) + ". " + name;
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        JListReorderUtil.install(orderTableList, orderListModel);
        orderDragScrollPane = new JScrollPane(orderTableList);
        orderDragScrollPane.setBorder(BorderFactory.createTitledBorder(I18nKit.t("sort.drag.title")));

        orderFilePanel = new JPanel(new BorderLayout(6, 0));
        orderFilePanel.setBorder(BorderFactory.createTitledBorder(I18nKit.t("sort.file.title")));
        orderFileTf = new JTextField();
        orderFileChooseBtn = new JButton(I18nKit.t("button.choose"));
        orderFileChooseBtn.addActionListener(e -> chooseOrderFile());
        orderFilePanel.add(orderFileTf, BorderLayout.CENTER);
        orderFilePanel.add(orderFileChooseBtn, BorderLayout.EAST);

        orderCenterCard = new JPanel(new CardLayout());
        orderCenterCard.add(orderInputScrollPane, "input");
        orderCenterCard.add(orderDragScrollPane, "drag");

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.add(orderModePanel, BorderLayout.NORTH);
        body.add(orderCenterCard, BorderLayout.CENTER);
        body.add(orderFilePanel, BorderLayout.SOUTH);
        tableOrderPanel.add(body, BorderLayout.CENTER);
        refreshOrderInputsEnabled();

        orderDisabledRb.addActionListener(e -> refreshOrderInputsEnabled());
        orderFromInputRb.addActionListener(e -> refreshOrderInputsEnabled());
        orderFromFileRb.addActionListener(e -> refreshOrderInputsEnabled());
        orderFromDragRb.addActionListener(e -> refreshOrderInputsEnabled());
        return tableOrderPanel;
    }

    /**
     * 数据源/命名空间/表遍历中的首次出现顺序，同名表去重，与多行/文件排序语义一致。
     */
    private @NotNull List<String> defaultTableNameOrder() {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (DataSourceDTO ds : dataSources) {
            if (ds == null || CollectionUtils.isEmpty(ds.getNamespaces())) {
                continue;
            }
            for (NamespaceDTO ns : ds.getNamespaces()) {
                if (ns == null || CollectionUtils.isEmpty(ns.getTables())) {
                    continue;
                }
                for (TableDTO t : ns.getTables()) {
                    if (t == null || StringUtils.isBlank(t.getName())) {
                        continue;
                    }
                    unique.add(t.getName());
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private void chooseOrderFile() {
        VirtualFile base = ProjectKit.getBaseDir(project);
        VirtualFile file = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFileDescriptor("txt"),
                project,
                base
        );
        if (file != null) {
            orderFileTf.setText(file.getPath());
        }
    }

    private void refreshOrderInputsEnabled() {
        boolean enableInput = orderFromInputRb != null && orderFromInputRb.isSelected();
        boolean enableFile = orderFromFileRb != null && orderFromFileRb.isSelected();
        boolean enableDrag = orderFromDragRb != null && orderFromDragRb.isSelected();
        if (orderInputTa != null) {
            orderInputTa.setEnabled(enableInput);
        }
        if (orderFileTf != null) {
            orderFileTf.setEnabled(enableFile);
        }
        if (orderFileChooseBtn != null) {
            orderFileChooseBtn.setEnabled(enableFile);
        }
        if (orderCenterCard != null) {
            CardLayout cl = (CardLayout) orderCenterCard.getLayout();
            if (enableDrag) {
                cl.show(orderCenterCard, "drag");
            } else {
                cl.show(orderCenterCard, "input");
            }
        }
        if (orderTableList != null) {
            orderTableList.setEnabled(enableDrag);
            orderTableList.setDragEnabled(enableDrag);
        }
    }

    private void toggleOrderPanel() {
        if (orderPanelExpanded) {
            animateOrderPanel(false);
        } else {
            animateOrderPanel(true);
        }
    }

    private void animateOrderPanel(boolean expand) {
        if (splitPane == null) {
            return;
        }
        if (panelAnimationTimer != null && panelAnimationTimer.isRunning()) {
            panelAnimationTimer.stop();
        }
        int targetPanelWidth = calculateOrderPanelWidth();
        Window window = SwingUtilities.getWindowAncestor(splitPane);
        if (window == null) {
            return;
        }
        if (collapsedDialogWidth <= 0) {
            collapsedDialogWidth = JBUI.scale(COLLAPSED_DIALOG_WIDTH);
        }
        int startDivider = splitPane.getDividerLocation();
        int endWidth = expand ? (collapsedDialogWidth + targetPanelWidth) : collapsedDialogWidth;
        int endDivider = expand ? targetPanelWidth : 0;
        splitPane.setDividerSize(8);

        // Avoid per-frame window resize jitter: resize once, animate divider only.
        if (expand && window.getWidth() != endWidth) {
            window.setSize(endWidth, window.getHeight());
            window.validate();
        }

        int steps = 8;
        final int[] current = {0};
        panelAnimationTimer = new Timer(12, e -> {
            current[0]++;
            double progress = Math.min(1.0d, current[0] / (double) steps);
            int nextDivider = (int) Math.round(startDivider + (endDivider - startDivider) * progress);
            splitPane.setDividerLocation(nextDivider);
            if (progress >= 1.0d) {
                panelAnimationTimer.stop();
                orderPanelExpanded = expand;
                if (!expand) {
                    splitPane.setDividerSize(0);
                    splitPane.setDividerLocation(0);
                    if (window.getWidth() != endWidth) {
                        window.setSize(endWidth, window.getHeight());
                        window.validate();
                    }
                }
                refreshSortActionText();
            }
        });
        panelAnimationTimer.start();
    }

    private void refreshSortPanelTexts() {
        if (orderModePanel != null) {
            orderModePanel.setBorder(BorderFactory.createTitledBorder(I18nKit.t("sort.source")));
        }
        if (orderDisabledRb != null) {
            orderDisabledRb.setText(I18nKit.t("sort.disabled"));
        }
        if (orderFromInputRb != null) {
            orderFromInputRb.setText(I18nKit.t("sort.input"));
        }
        if (orderFromFileRb != null) {
            orderFromFileRb.setText(I18nKit.t("sort.file"));
        }
        if (orderFromDragRb != null) {
            orderFromDragRb.setText(I18nKit.t("sort.drag"));
        }
        if (orderInputScrollPane != null) {
            orderInputScrollPane.setBorder(BorderFactory.createTitledBorder(I18nKit.t("sort.input.title")));
        }
        if (orderDragScrollPane != null) {
            orderDragScrollPane.setBorder(BorderFactory.createTitledBorder(I18nKit.t("sort.drag.title")));
        }
        if (orderFilePanel != null) {
            orderFilePanel.setBorder(BorderFactory.createTitledBorder(I18nKit.t("sort.file.title")));
        }
        if (orderFileChooseBtn != null) {
            orderFileChooseBtn.setText(I18nKit.t("button.choose"));
        }
    }

    private void refreshSortActionText() {
        if (sortToggleAction != null) {
            sortToggleAction.putValue(Action.NAME, orderPanelExpanded ? I18nKit.t("button.sort.collapse") : I18nKit.t("button.sort.config"));
        }
    }

    private int calculateOrderPanelWidth() {
        int dpiScaledBase = JBUI.scale(ORDER_PANEL_BASE_WIDTH);
        int contentMinimumWidth = tableOrderPanel == null ? dpiScaledBase : tableOrderPanel.getMinimumSize().width;
        return Math.max(dpiScaledBase, contentMinimumWidth);
    }

    private List<String> parseTableOrderLines(List<String> lines) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String tableName = line.trim();
            if (tableName.isEmpty() || tableName.startsWith("#")) {
                continue;
            }
            names.add(tableName);
        }
        return new ArrayList<>(names);
    }

    private void applyTableOrder(List<String> orderedTableNames) {
        Map<String, Integer> orderIndex = new HashMap<>();
        for (int i = 0; i < orderedTableNames.size(); i++) {
            orderIndex.putIfAbsent(orderedTableNames.get(i).toLowerCase(Locale.ROOT), i);
        }
        for (DataSourceDTO ds : dataSources) {
            if (CollectionUtils.isEmpty(ds.getNamespaces())) {
                continue;
            }
            List<NamespaceDTO> namespaces = ds.getNamespaces();
            sortTablesInNamespaces(namespaces, orderIndex);
            List<NamespaceDTO> sortedNamespaces = namespaces.stream()
                    .sorted((a, b) -> compareNamespace(a, b, orderIndex))
                    .collect(Collectors.toCollection(ArrayList::new));
            ds.setNamespaces(sortedNamespaces);
        }
    }

    private void sortTablesInNamespaces(List<NamespaceDTO> namespaces, Map<String, Integer> orderIndex) {
        for (NamespaceDTO ns : namespaces) {
            if (CollectionUtils.isEmpty(ns.getTables())) {
                continue;
            }
            List<TableDTO> sorted = ns.getTables().stream()
                    .sorted(Comparator.comparingInt(t -> tableOrder(t.getName(), orderIndex)))
                    .collect(Collectors.toCollection(ArrayList::new));
            ns.setTables(sorted);
        }
    }

    private int compareNamespace(NamespaceDTO left, NamespaceDTO right, Map<String, Integer> orderIndex) {
        int leftOrder = namespaceOrder(left, orderIndex);
        int rightOrder = namespaceOrder(right, orderIndex);
        if (leftOrder != rightOrder) {
            return Integer.compare(leftOrder, rightOrder);
        }
        return StringUtils.defaultString(left.getName()).compareToIgnoreCase(StringUtils.defaultString(right.getName()));
    }

    private int namespaceOrder(NamespaceDTO namespace, Map<String, Integer> orderIndex) {
        if (namespace == null || CollectionUtils.isEmpty(namespace.getTables())) {
            return Integer.MAX_VALUE;
        }
        int min = Integer.MAX_VALUE;
        for (TableDTO table : namespace.getTables()) {
            min = Math.min(min, tableOrder(table.getName(), orderIndex));
        }
        return min;
    }

    private int tableOrder(String tableName, Map<String, Integer> orderIndex) {
        if (StringUtils.isBlank(tableName) || orderIndex.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return orderIndex.getOrDefault(tableName.toLowerCase(Locale.ROOT), Integer.MAX_VALUE);
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
            Messages.showWarningDialog(I18nKit.t("dialog.warn.template"), I18nKit.t("app.title"));
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
