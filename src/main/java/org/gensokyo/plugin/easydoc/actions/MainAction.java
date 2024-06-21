package org.gensokyo.plugin.easydoc.actions;

import com.intellij.database.psi.DbTable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.gensokyo.plugin.easydoc.constant.Const;
import org.gensokyo.plugin.easydoc.ui.SelectSavePath;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        DataContext dataContext = event.getDataContext();
        Project project = event.getProject();
        boolean isRightElementSelected = isRightElementSelected(project, dataContext);
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

        List<DbTable> dbTableList = new ArrayList<>();
        for (PsiElement element : psiElements) {
            if (!(element instanceof DbTable dbTable)) {
                continue;
            }
            dbTableList.add(dbTable);
        }

        if (dbTableList.isEmpty()) {
            return;
        }

        new SelectSavePath(event.getProject(), dbTableList).show();
    }

    private boolean isRightElementSelected(Project project, DataContext dataContext) {
        PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        return psiElement instanceof DbTable;
    }
}
