package org.gensokyo.plugin.easydoc.actions;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.intellij.database.model.DasNamespace;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbNamespace;
import com.intellij.database.psi.DbTable;
import com.intellij.database.util.DasUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.gensokyo.plugin.easydoc.constant.Const;
import org.gensokyo.plugin.easydoc.dto.DataSourceDTO;
import org.gensokyo.plugin.easydoc.dto.NamespaceDTO;
import org.gensokyo.plugin.easydoc.dto.TableDTO;
import org.gensokyo.plugin.easydoc.ui.MainDialog;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 代码生成菜单
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/11 , Version 1.0.0
 */
public class MainAction extends AnAction {

    public MainAction() {
        super(Const.TITLE);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        PsiElement[] psiElements = event.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
        boolean isRightElementSelected = isRightElementSelected(psiElements);
        presentation.setEnabled(isRightElementSelected);
        presentation.setVisible(isRightElementSelected);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        PsiElement[] psiElements = event.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
        if (psiElements == null || psiElements.length == 0) {
            return;
        }

        Table<DbDataSource, String, NamespaceDTO> cache = HashBasedTable.create();

        for (PsiElement element : psiElements) {
            if (element instanceof DbTable dbTable) {
                NamespaceDTO ns = toNamespace(dbTable);
                addOrMerge(cache, dbTable.getDataSource(), ns);
            }

            if (element instanceof DbNamespace dbNamespace) {
                addOrMerge(cache, dbNamespace.getDataSource(), new NamespaceDTO(dbNamespace));
            }
        }

        if (cache.isEmpty()) {
            return;
        }

        new MainDialog(project, toDataSources(cache)).show();
    }

    private boolean isRightElementSelected(PsiElement[] psiElements) {
        return Arrays.stream(psiElements)
                .allMatch(psiElement -> psiElement instanceof DbTable || psiElement instanceof DbNamespace);

    }

    private void addOrMerge(Table<DbDataSource, String, NamespaceDTO> cache, DbDataSource ds, NamespaceDTO newNs) {
        if (cache.contains(ds, newNs.getName())) {
            NamespaceDTO oldNs = cache.get(ds, newNs.getName());
            if (Objects.isNull(oldNs)) {
                return;
            }
            oldNs.merge(newNs);
        } else {
            cache.put(ds, newNs.getName(), newNs);
        }
    }

    private NamespaceDTO toNamespace(DbTable dbTable) {
        NamespaceDTO dto = new NamespaceDTO();
        DasNamespace ns = DasUtil.getNamespace(dbTable);
        if (Objects.isNull(ns)) {
            dto.setName("Unknown");
            dto.setComment("");
        } else {
            dto.setName(ns.getName());
            dto.setComment(ns.getComment());
        }
        dto.setDas(ns);
        dto.getTables().add(new TableDTO(dbTable));
        return dto;
    }

    private List<DataSourceDTO> toDataSources(Table<DbDataSource, String, NamespaceDTO> cache) {
        List<DataSourceDTO> list = new ArrayList<>();
        for (DbDataSource ds : cache.rowKeySet()) {
            DataSourceDTO dto = new DataSourceDTO(ds);
            dto.setNamespaces(cache.row(ds).values().stream().toList());
            list.add(dto);
        }
        return list;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
