package org.gensokyo.plugin.easydoc.ui.component;

import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang3.StringUtils;
import org.gensokyo.plugin.easydoc.kit.I18nKit;
import org.gensokyo.plugin.easydoc.plan.DocNode;
import org.gensokyo.plugin.easydoc.plan.DocPlan;
import org.gensokyo.plugin.easydoc.plan.NodeType;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PlanTreePanel extends JPanel {
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode root;

    private final JBTextField displayNameTf;
    private final JBTextField variableNameTf;
    private final JBCheckBox enabledCb;
    private final JLabel typeValueLabel;
    private final JLabel dragStatusLabel;
    private final JBTextField searchTf;
    private final JComboBox<String> searchScopeCb;
    private final JLabel searchCounterLabel;
    private TreePath searchMatchedPath;
    private String lastSearchKeyword = "";
    private int lastSearchIndex = -1;
    private int lastScopeIndex = 0;

    private TreePath dragTargetPath;
    private boolean dragTargetAllowed;

    public PlanTreePanel(DocPlan plan) {
        super(new BorderLayout(8, 8));
        this.root = new DefaultMutableTreeNode("ROOT");
        this.treeModel = new DefaultTreeModel(root);
        this.tree = new JTree(treeModel);
        this.tree.setRootVisible(false);
        this.tree.setShowsRootHandles(true);
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        this.tree.setDragEnabled(true);
        this.tree.setDropMode(DropMode.ON_OR_INSERT);
        this.tree.setTransferHandler(new NodeTransferHandler());
        this.tree.setCellRenderer(new DragAwareTreeCellRenderer());
        loadPlan(plan);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton addGroupBtn = new JButton(I18nKit.t("button.plan.add.group"));
        JButton renameBtn = new JButton(I18nKit.t("button.plan.rename"));
        JButton deleteBtn = new JButton(I18nKit.t("button.plan.delete.group"));
        JButton moveToGroupBtn = new JButton(I18nKit.t("button.plan.move.group"));
        JButton upBtn = new JButton(I18nKit.t("button.plan.up"));
        JButton downBtn = new JButton(I18nKit.t("button.plan.down"));
        toolbar.add(addGroupBtn);
        toolbar.add(renameBtn);
        toolbar.add(deleteBtn);
        toolbar.add(moveToGroupBtn);
        toolbar.add(upBtn);
        toolbar.add(downBtn);

        addGroupBtn.addActionListener(e -> addGroupNode());
        renameBtn.addActionListener(e -> renameNode());
        deleteBtn.addActionListener(e -> deleteSelectedGroupNode());
        moveToGroupBtn.addActionListener(e -> moveSelectedNodesToGroup());
        upBtn.addActionListener(e -> moveSelectedNode(-1));
        downBtn.addActionListener(e -> moveSelectedNode(1));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchPanel.add(new JLabel(I18nKit.t("label.plan.search")));
        searchTf = new JBTextField();
        searchTf.setColumns(16);
        searchScopeCb = new JComboBox<>(new String[]{
                I18nKit.t("label.plan.search.scope.all"),
                I18nKit.t("label.plan.search.scope.current.group")
        });
        searchScopeCb.setSelectedIndex(0);
        JButton prevBtn = new JButton(I18nKit.t("button.plan.search.prev"));
        JButton nextBtn = new JButton(I18nKit.t("button.plan.search.next"));
        JButton clearBtn = new JButton(I18nKit.t("button.plan.search.clear"));
        searchCounterLabel = new JLabel(I18nKit.t("label.plan.search.counter.empty"));
        searchPanel.add(searchTf);
        searchPanel.add(searchScopeCb);
        searchPanel.add(prevBtn);
        searchPanel.add(nextBtn);
        searchPanel.add(clearBtn);
        searchPanel.add(searchCounterLabel);
        prevBtn.addActionListener(e -> findAndSelectPrevious());
        nextBtn.addActionListener(e -> findAndSelectNext());
        clearBtn.addActionListener(e -> clearSearch());
        searchTf.addActionListener(e -> findAndSelectNext());
        searchScopeCb.addActionListener(e -> {
            if (searchScopeCb.getSelectedIndex() != lastScopeIndex) {
                lastScopeIndex = searchScopeCb.getSelectedIndex();
                lastSearchIndex = -1;
            }
        });

        JPanel editor = new JPanel(new GridLayout(4, 2, 6, 6));
        editor.setBorder(BorderFactory.createTitledBorder(I18nKit.t("label.plan.properties")));
        editor.add(new JLabel(I18nKit.t("label.plan.type")));
        typeValueLabel = new JLabel("-");
        editor.add(typeValueLabel);
        editor.add(new JLabel(I18nKit.t("label.plan.display.name")));
        displayNameTf = new JBTextField();
        editor.add(displayNameTf);
        editor.add(new JLabel(I18nKit.t("label.plan.variable")));
        variableNameTf = new JBTextField();
        editor.add(variableNameTf);
        editor.add(new JLabel(I18nKit.t("label.plan.enabled")));
        enabledCb = new JBCheckBox();
        editor.add(enabledCb);

        displayNameTf.getDocument().addDocumentListener(SimpleDocumentListeners.onChange(this::applyEditorToSelection));
        variableNameTf.getDocument().addDocumentListener(SimpleDocumentListeners.onChange(this::applyEditorToSelection));
        enabledCb.addActionListener(e -> applyEditorToSelection());

        tree.addTreeSelectionListener(this::onTreeSelectionChanged);
        if (root.getChildCount() > 0) {
            tree.setSelectionRow(0);
        }

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.7d);
        splitPane.setTopComponent(new JBScrollPane(tree));
        splitPane.setBottomComponent(editor);

        dragStatusLabel = new JLabel(I18nKit.t("label.plan.drag.ready"));
        dragStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 0));

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.add(toolbar, BorderLayout.NORTH);
        north.add(searchPanel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(dragStatusLabel, BorderLayout.SOUTH);
        installContextMenu();
        refreshEditorFromSelection();
    }

    public DocPlan toDocPlan() {
        DocPlan plan = new DocPlan();
        List<DocNode> roots = new ArrayList<>();
        Enumeration<?> children = root.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            roots.add(copyNodeRecursively(child));
        }
        sortByOrderRecursively(roots);
        plan.setRoots(roots);
        return plan;
    }

    private static void sortByOrderRecursively(List<DocNode> nodes) {
        nodes.sort(Comparator.comparingInt(DocNode::getOrder));
        for (DocNode node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortByOrderRecursively(node.getChildren());
            }
        }
    }

    private void loadPlan(DocPlan plan) {
        root.removeAllChildren();
        if (plan == null || plan.getRoots() == null) {
            return;
        }
        for (DocNode node : plan.getRoots()) {
            root.add(buildTreeNode(node));
        }
        treeModel.reload();
        expandAll();
    }

    private DefaultMutableTreeNode buildTreeNode(DocNode node) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(copyNodeShallow(node));
        if (node.getChildren() != null) {
            for (DocNode child : node.getChildren()) {
                treeNode.add(buildTreeNode(child));
            }
        }
        return treeNode;
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void addGroupNode() {
        DefaultMutableTreeNode selected = selectedNode();
        DefaultMutableTreeNode parent = selected == null ? root : selected;
        if (selected != null) {
            DocNode selectedDocNode = (DocNode) selected.getUserObject();
            if (!selectedDocNode.isGroupNode()) {
                parent = (DefaultMutableTreeNode) selected.getParent();
            }
        }
        if (parent == null) {
            parent = root;
        }
        int order = parent.getChildCount();
        DocNode group = new DocNode()
                .setType(NodeType.GROUP)
                .setDisplayName(I18nKit.t("label.plan.new.group"))
                .setVariableName("group_" + System.currentTimeMillis())
                .setOrder(order);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(group);
        treeModel.insertNodeInto(node, parent, parent.getChildCount());
        tree.setSelectionPath(new TreePath(node.getPath()));
        expandAll();
    }

    private void renameNode() {
        DefaultMutableTreeNode selected = selectedNode();
        if (selected == null) {
            return;
        }
        DocNode node = (DocNode) selected.getUserObject();
        String input = JOptionPane.showInputDialog(this, I18nKit.t("label.plan.display.name"), node.getDisplayName());
        if (StringUtils.isBlank(input)) {
            return;
        }
        node.setDisplayName(input.trim());
        treeModel.nodeChanged(selected);
        refreshEditorFromSelection();
    }

    private void deleteSelectedGroupNode() {
        DefaultMutableTreeNode selected = selectedNode();
        if (selected == null || selected.getParent() == null) {
            return;
        }
        DocNode node = (DocNode) selected.getUserObject();
        if (node.getType() != NodeType.GROUP) {
            return;
        }
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selected.getParent();
        int insertIndex = parent.getIndex(selected);
        while (selected.getChildCount() > 0) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) selected.getFirstChild();
            treeModel.removeNodeFromParent(child);
            treeModel.insertNodeInto(child, parent, insertIndex++);
        }
        treeModel.removeNodeFromParent(selected);
        resetSiblingOrder(parent);
    }

    private void moveSelectedNode(int offset) {
        DefaultMutableTreeNode selected = selectedNode();
        if (selected == null || selected.getParent() == null) {
            return;
        }
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selected.getParent();
        int oldIndex = parent.getIndex(selected);
        int newIndex = oldIndex + offset;
        if (newIndex < 0 || newIndex >= parent.getChildCount()) {
            return;
        }
        treeModel.removeNodeFromParent(selected);
        treeModel.insertNodeInto(selected, parent, newIndex);
        tree.setSelectionPath(new TreePath(selected.getPath()));
        resetSiblingOrder(parent);
    }

    private void moveSelectedNodesToGroup() {
        List<DefaultMutableTreeNode> selectedNodes = topLevelSelectedNodes();
        if (selectedNodes.isEmpty()) {
            Messages.showInfoMessage(I18nKit.t("dialog.plan.move.select"), I18nKit.t("app.title"));
            return;
        }
        List<GroupTarget> candidates = collectGroupTargets(selectedNodes);
        if (candidates.isEmpty()) {
            Messages.showWarningDialog(I18nKit.t("dialog.plan.move.no.target"), I18nKit.t("app.title"));
            return;
        }
        GroupTarget target = (GroupTarget) JOptionPane.showInputDialog(
                this,
                I18nKit.t("dialog.plan.move.choose"),
                I18nKit.t("button.plan.move.group"),
                JOptionPane.PLAIN_MESSAGE,
                null,
                candidates.toArray(),
                candidates.get(0)
        );
        if (target == null) {
            return;
        }
        DefaultMutableTreeNode targetGroup = target.node;
        Set<DefaultMutableTreeNode> oldParents = new HashSet<>();
        int insertIndex = targetGroup.getChildCount();
        for (DefaultMutableTreeNode node : selectedNodes) {
            DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) node.getParent();
            if (oldParent == null || node == targetGroup || isDescendant(node, targetGroup)) {
                continue;
            }
            oldParents.add(oldParent);
            treeModel.removeNodeFromParent(node);
            treeModel.insertNodeInto(node, targetGroup, insertIndex++);
        }
        for (DefaultMutableTreeNode oldParent : oldParents) {
            resetSiblingOrder(oldParent);
        }
        resetSiblingOrder(targetGroup);
        tree.setSelectionPath(new TreePath(targetGroup.getPath()));
        expandAll();
        Messages.showInfoMessage(I18nKit.t("dialog.plan.move.done"), I18nKit.t("app.title"));
    }

    private void findAndSelectNext() {
        findAndSelectByDirection(true);
    }

    private void findAndSelectPrevious() {
        findAndSelectByDirection(false);
    }

    private void findAndSelectByDirection(boolean forward) {
        String keyword = StringUtils.trimToEmpty(searchTf.getText());
        if (keyword.isEmpty()) {
            clearSearch();
            return;
        }
        if (!StringUtils.equalsIgnoreCase(keyword, lastSearchKeyword)) {
            lastSearchKeyword = keyword;
            lastSearchIndex = -1;
        }
        List<TreePath> scopePaths = collectSearchScopePaths();
        if (scopePaths.isEmpty()) {
            updateSearchCounter(0, 0);
            return;
        }
        String needle = keyword.toLowerCase();
        List<TreePath> matched = new ArrayList<>();
        for (TreePath path : scopePaths) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) path.getLastPathComponent();
            String text = n.getUserObject() == null ? "" : n.getUserObject().toString();
            if (StringUtils.containsIgnoreCase(text, needle)) {
                matched.add(path);
            }
        }
        if (matched.isEmpty()) {
            updateSearchCounter(0, 0);
            Messages.showInfoMessage(I18nKit.t("dialog.plan.search.notfound"), I18nKit.t("app.title"));
            return;
        }
        int count = scopePaths.size();
        int step = forward ? 1 : -1;
        int start = (lastSearchIndex + step + count) % count;
        int idx = start;
        do {
            TreePath path = scopePaths.get(idx);
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                String text = node.getUserObject() == null ? "" : node.getUserObject().toString();
                if (StringUtils.containsIgnoreCase(text, needle)) {
                    lastSearchIndex = idx;
                    searchMatchedPath = path;
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                    int current = 1;
                    for (int i = 0; i < matched.size(); i++) {
                        if (matched.get(i).equals(path)) {
                            current = i + 1;
                            break;
                        }
                    }
                    updateSearchCounter(current, matched.size());
                    tree.repaint();
                    return;
                }
            }
            idx = (idx + step + count) % count;
        } while (idx != start);
        updateSearchCounter(0, matched.size());
        Messages.showInfoMessage(I18nKit.t("dialog.plan.search.notfound"), I18nKit.t("app.title"));
    }

    private void clearSearch() {
        searchTf.setText("");
        searchMatchedPath = null;
        lastSearchKeyword = "";
        lastSearchIndex = -1;
        updateSearchCounter(0, 0);
        tree.repaint();
    }

    private void updateSearchCounter(int current, int total) {
        if (current <= 0 || total <= 0) {
            searchCounterLabel.setText(I18nKit.t("label.plan.search.counter.empty"));
            return;
        }
        String text = I18nKit.t("label.plan.search.counter.format")
                .replace("{0}", String.valueOf(current))
                .replace("{1}", String.valueOf(total));
        searchCounterLabel.setText(text);
    }

    private List<TreePath> collectSearchScopePaths() {
        List<TreePath> paths = new ArrayList<>();
        if (searchScopeCb.getSelectedIndex() == 1) {
            DefaultMutableTreeNode base = selectedNode();
            if (base == null) {
                return paths;
            }
            DocNode n = (DocNode) base.getUserObject();
            if (!n.isGroupNode()) {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) base.getParent();
                if (parent != null) {
                    base = parent;
                }
            }
            collectVisiblePathsFromNode(base, paths);
            return paths;
        }
        for (int i = 0; i < tree.getRowCount(); i++) {
            TreePath path = tree.getPathForRow(i);
            if (path != null) {
                paths.add(path);
            }
        }
        return paths;
    }

    private void collectVisiblePathsFromNode(DefaultMutableTreeNode start, List<TreePath> out) {
        if (start == null) {
            return;
        }
        TreePath path = new TreePath(start.getPath());
        out.add(path);
        Enumeration<?> en = start.depthFirstEnumeration();
        while (en.hasMoreElements()) {
            Object next = en.nextElement();
            if (!(next instanceof DefaultMutableTreeNode node) || node == start) {
                continue;
            }
            out.add(new TreePath(node.getPath()));
        }
    }

    private List<DefaultMutableTreeNode> topLevelSelectedNodes() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return List.of();
        }
        List<DefaultMutableTreeNode> raw = new ArrayList<>();
        for (TreePath path : paths) {
            if (path == null) {
                continue;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node == null || node.getParent() == null) {
                continue;
            }
            raw.add(node);
        }
        // 若父子同时选中，仅保留父节点，避免重复移动
        List<DefaultMutableTreeNode> top = new ArrayList<>();
        for (DefaultMutableTreeNode node : raw) {
            boolean hasAncestorSelected = false;
            for (DefaultMutableTreeNode other : raw) {
                if (node != other && other.isNodeDescendant(node)) {
                    hasAncestorSelected = true;
                    break;
                }
            }
            if (!hasAncestorSelected) {
                top.add(node);
            }
        }
        return top;
    }

    private List<GroupTarget> collectGroupTargets(List<DefaultMutableTreeNode> movingNodes) {
        Set<DefaultMutableTreeNode> movingSet = new LinkedHashSet<>(movingNodes);
        List<GroupTarget> targets = new ArrayList<>();
        collectGroupTargetsRecursively(root, "", movingSet, targets);
        return targets;
    }

    private void collectGroupTargetsRecursively(
            DefaultMutableTreeNode current,
            String path,
            Set<DefaultMutableTreeNode> movingSet,
            List<GroupTarget> out
    ) {
        for (int i = 0; i < current.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) current.getChildAt(i);
            DocNode docNode = (DocNode) child.getUserObject();
            String currentPath = path.isEmpty() ? StringUtils.defaultString(docNode.getDisplayName(), docNode.getType().name())
                    : path + " / " + StringUtils.defaultString(docNode.getDisplayName(), docNode.getType().name());
            if (docNode.isGroupNode()) {
                boolean invalid = movingSet.contains(child);
                if (!invalid) {
                    for (DefaultMutableTreeNode moving : movingSet) {
                        if (isDescendant(moving, child)) {
                            invalid = true;
                            break;
                        }
                    }
                }
                if (!invalid) {
                    out.add(new GroupTarget(child, currentPath));
                }
            }
            collectGroupTargetsRecursively(child, currentPath, movingSet, out);
        }
    }

    private void resetSiblingOrder(DefaultMutableTreeNode parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            ((DocNode) child.getUserObject()).setOrder(i);
        }
    }

    private static final class GroupTarget {
        private final DefaultMutableTreeNode node;
        private final String display;

        private GroupTarget(DefaultMutableTreeNode node, String display) {
            this.node = node;
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private void onTreeSelectionChanged(TreeSelectionEvent ignored) {
        refreshEditorFromSelection();
    }

    private void installContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem addGroup = new JMenuItem(I18nKit.t("button.plan.add.group"));
        JMenuItem rename = new JMenuItem(I18nKit.t("button.plan.rename"));
        JMenuItem delete = new JMenuItem(I18nKit.t("button.plan.delete.group"));
        JMenuItem move = new JMenuItem(I18nKit.t("button.plan.move.group"));
        JMenuItem enable = new JMenuItem(I18nKit.t("button.plan.enable"));
        JMenuItem disable = new JMenuItem(I18nKit.t("button.plan.disable"));
        addGroup.addActionListener(e -> addGroupNode());
        rename.addActionListener(e -> renameNode());
        delete.addActionListener(e -> deleteSelectedGroupNode());
        move.addActionListener(e -> moveSelectedNodesToGroup());
        enable.addActionListener(e -> setSelectedEnabled(true));
        disable.addActionListener(e -> setSelectedEnabled(false));
        menu.add(addGroup);
        menu.add(rename);
        menu.add(delete);
        menu.add(move);
        menu.addSeparator();
        menu.add(enable);
        menu.add(disable);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowMenu(e);
            }

            private void maybeShowMenu(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row >= 0) {
                    TreePath path = tree.getPathForRow(row);
                    if (path != null && !tree.isPathSelected(path)) {
                        tree.setSelectionPath(path);
                    }
                }
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void setSelectedEnabled(boolean enabled) {
        List<DefaultMutableTreeNode> selectedNodes = topLevelSelectedNodes();
        if (selectedNodes.isEmpty()) {
            return;
        }
        for (DefaultMutableTreeNode node : selectedNodes) {
            setEnabledRecursively(node, enabled);
            treeModel.nodeChanged(node);
        }
        refreshEditorFromSelection();
    }

    private void setEnabledRecursively(DefaultMutableTreeNode treeNode, boolean enabled) {
        DocNode node = (DocNode) treeNode.getUserObject();
        node.setEnabled(enabled);
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            setEnabledRecursively((DefaultMutableTreeNode) treeNode.getChildAt(i), enabled);
        }
    }

    private void refreshEditorFromSelection() {
        DefaultMutableTreeNode selected = selectedNode();
        if (selected == null) {
            typeValueLabel.setText("-");
            displayNameTf.setText("");
            variableNameTf.setText("");
            enabledCb.setSelected(false);
            setEditorEnabled(false);
            return;
        }
        setEditorEnabled(true);
        DocNode node = (DocNode) selected.getUserObject();
        typeValueLabel.setText(node.getType().name());
        displayNameTf.setText(StringUtils.defaultString(node.getDisplayName()));
        variableNameTf.setText(StringUtils.defaultString(node.getVariableName()));
        enabledCb.setSelected(node.isEnabled());
    }

    private void setEditorEnabled(boolean enabled) {
        displayNameTf.setEnabled(enabled);
        variableNameTf.setEnabled(enabled);
        enabledCb.setEnabled(enabled);
    }

    private void applyEditorToSelection() {
        DefaultMutableTreeNode selected = selectedNode();
        if (selected == null) {
            return;
        }
        DocNode node = (DocNode) selected.getUserObject();
        node.setDisplayName(displayNameTf.getText().trim());
        node.setVariableName(variableNameTf.getText().trim());
        node.setEnabled(enabledCb.isSelected());
        treeModel.nodeChanged(selected);
    }

    private DefaultMutableTreeNode selectedNode() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    private static DocNode copyNodeShallow(DocNode source) {
        return new DocNode()
                .setId(source.getId())
                .setType(source.getType())
                .setDisplayName(source.getDisplayName())
                .setVariableName(source.getVariableName())
                .setOrder(source.getOrder())
                .setRef(source.getRef())
                .setEnabled(source.isEnabled());
    }

    private static DocNode copyNodeRecursively(DefaultMutableTreeNode treeNode) {
        DocNode source = (DocNode) treeNode.getUserObject();
        DocNode node = copyNodeShallow(source);
        List<DocNode> children = new ArrayList<>();
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            children.add(copyNodeRecursively((DefaultMutableTreeNode) treeNode.getChildAt(i)));
        }
        node.setChildren(children);
        return node;
    }

    private static boolean isDescendant(DefaultMutableTreeNode ancestor, DefaultMutableTreeNode child) {
        if (ancestor == null || child == null) {
            return false;
        }
        return ancestor.isNodeDescendant(child);
    }

    private void updateDragStatus(boolean allowed, String message, TreePath path) {
        dragTargetAllowed = allowed;
        dragTargetPath = path;
        dragStatusLabel.setText(message);
        dragStatusLabel.setForeground(allowed ? new Color(0, 128, 0) : new Color(180, 60, 60));
        tree.setCursor(allowed ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        tree.repaint();
    }

    private void resetDragStatus() {
        dragTargetAllowed = false;
        dragTargetPath = null;
        dragStatusLabel.setText(I18nKit.t("label.plan.drag.ready"));
        dragStatusLabel.setForeground(UIManager.getColor("Label.foreground"));
        tree.setCursor(Cursor.getDefaultCursor());
        tree.repaint();
    }

    private String describeDropLocation(DefaultMutableTreeNode target, int childIndex) {
        if (target == null) {
            return I18nKit.t("label.plan.drag.invalid");
        }
        String name = target.getUserObject() == null ? "-" : target.getUserObject().toString();
        if (childIndex >= 0) {
            return I18nKit.t("label.plan.drag.to.child") + " " + name;
        }
        DocNode targetNode = (DocNode) target.getUserObject();
        if (targetNode.isGroupNode()) {
            return I18nKit.t("label.plan.drag.to.group") + " " + name;
        }
        return I18nKit.t("label.plan.drag.after.node") + " " + name;
    }

    private final class DragAwareTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus
        ) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(
                    tree, value, selected, expanded, leaf, row, hasFocus);
            setLeafIcon(null);
            setOpenIcon(null);
            setClosedIcon(null);
            if (dragTargetPath != null && dragTargetPath.equals(new TreePath(((DefaultMutableTreeNode) value).getPath()))) {
                label.setOpaque(true);
                if (dragTargetAllowed) {
                    label.setBackground(new Color(220, 245, 225));
                } else {
                    label.setBackground(new Color(250, 225, 225));
                }
            } else if (searchMatchedPath != null && searchMatchedPath.equals(new TreePath(((DefaultMutableTreeNode) value).getPath()))) {
                label.setOpaque(true);
                label.setBackground(new Color(255, 244, 204));
            }
            return label;
        }
    }

    private final class NodeTransferHandler extends TransferHandler {
        private final DataFlavor nodeFlavor;
        private DefaultMutableTreeNode draggedNode;

        private NodeTransferHandler() {
            try {
                nodeFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + DefaultMutableTreeNode.class.getName());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to initialize tree node flavor", e);
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            draggedNode = selectedNode();
            if (draggedNode == null || draggedNode.getParent() == null) {
                return null;
            }
            updateDragStatus(true, I18nKit.t("label.plan.drag.start") + " " + draggedNode.getUserObject(), new TreePath(draggedNode.getPath()));
            return new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{nodeFlavor};
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return nodeFlavor.equals(flavor);
                }

                @Override
                public Object getTransferData(DataFlavor flavor) {
                    return draggedNode;
                }
            };
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                updateDragStatus(false, I18nKit.t("label.plan.drag.invalid"), null);
                return false;
            }
            if (!support.isDataFlavorSupported(nodeFlavor)) {
                updateDragStatus(false, I18nKit.t("label.plan.drag.invalid"), null);
                return false;
            }
            if (draggedNode == null || draggedNode.getParent() == null) {
                updateDragStatus(false, I18nKit.t("label.plan.drag.invalid"), null);
                return false;
            }
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            TreePath path = dl.getPath();
            if (path == null) {
                updateDragStatus(false, I18nKit.t("label.plan.drag.invalid"), null);
                return false;
            }
            DefaultMutableTreeNode target = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (target == draggedNode) {
                updateDragStatus(false, I18nKit.t("label.plan.drag.self"), path);
                return false;
            }
            if (isDescendant(draggedNode, target)) {
                updateDragStatus(false, I18nKit.t("label.plan.drag.descendant"), path);
                return false;
            }
            int childIndex = dl.getChildIndex();
            if (childIndex < 0) {
                DocNode targetNode = (DocNode) target.getUserObject();
                if (!targetNode.isGroupNode()) {
                    boolean allowed = target.getParent() != null;
                    updateDragStatus(allowed, allowed ? describeDropLocation(target, childIndex) : I18nKit.t("label.plan.drag.invalid"), path);
                    return allowed;
                }
            }
            updateDragStatus(true, describeDropLocation(target, childIndex), path);
            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            DefaultMutableTreeNode target = (DefaultMutableTreeNode) dl.getPath().getLastPathComponent();
            DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) draggedNode.getParent();
            if (oldParent == null) {
                return false;
            }

            DefaultMutableTreeNode newParent;
            int insertIndex;
            int childIndex = dl.getChildIndex();
            if (childIndex >= 0) {
                newParent = target;
                insertIndex = Math.min(childIndex, newParent.getChildCount());
            } else {
                DocNode targetNode = (DocNode) target.getUserObject();
                if (targetNode.isGroupNode()) {
                    newParent = target;
                    insertIndex = newParent.getChildCount();
                } else {
                    newParent = (DefaultMutableTreeNode) target.getParent();
                    if (newParent == null) {
                        return false;
                    }
                    insertIndex = newParent.getIndex(target) + 1;
                }
            }

            if (newParent == oldParent) {
                int oldIndex = oldParent.getIndex(draggedNode);
                if (oldIndex < 0) {
                    return false;
                }
                if (insertIndex > oldIndex) {
                    insertIndex--;
                }
                if (insertIndex == oldIndex) {
                    return false;
                }
            }

            treeModel.removeNodeFromParent(draggedNode);
            treeModel.insertNodeInto(draggedNode, newParent, Math.min(insertIndex, newParent.getChildCount()));
            resetSiblingOrder(oldParent);
            if (newParent != oldParent) {
                resetSiblingOrder(newParent);
            }
            tree.setSelectionPath(new TreePath(draggedNode.getPath()));
            expandAll();
            updateDragStatus(true, I18nKit.t("label.plan.drag.done"), new TreePath(draggedNode.getPath()));
            return true;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if (action == NONE) {
                updateDragStatus(false, I18nKit.t("label.plan.drag.cancel"), null);
            }
            draggedNode = null;
            SwingUtilities.invokeLater(PlanTreePanel.this::resetDragStatus);
        }
    }
}
