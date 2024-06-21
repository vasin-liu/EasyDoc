/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.plugin.easydoc.kit;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * 项目工具类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/14 , Version 1.0.0
 */
public class ProjectKit {

    private ProjectKit() {
        throw new UnsupportedOperationException();
    }

    /**
     * 进行旧版本兼容，该方法已经存在 @see {@link com.intellij.openapi.project.ProjectUtil#guessProjectDir(Project)}
     *
     * @param project 项目对象
     * @return 基本目录
     */
    public static VirtualFile getBaseDir(Project project) {
        if (project.isDefault()) {
            return null;
        }
        Module[] modules = ModuleManager.getInstance(project).getModules();
        Module module = null;
        if (modules.length == 1) {
            module = modules[0];
        } else {
            for (Module item : modules) {
                if (item.getName().equals(project.getName())) {
                    module = item;
                    break;
                }
            }
        }
        if (module != null) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            for (VirtualFile contentRoot : moduleRootManager.getContentRoots()) {
                if (contentRoot.isDirectory() && contentRoot.getName().equals(module.getName())) {
                    return contentRoot;
                }
            }
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            throw new NullPointerException();
        }
        return LocalFileSystem.getInstance().findFileByPath(basePath);
    }
}
