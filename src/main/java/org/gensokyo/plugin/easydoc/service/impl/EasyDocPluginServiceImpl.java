/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package com.sjhy.plugin.service.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.messages.MessageBusConnection;
import com.sjhy.plugin.constant.Const;
import com.sjhy.plugin.service.EasyCodePluginService;

import java.io.File;

/**
 * 插件服务实现类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/6/17 , Version 1.0.0
 */
public class EasyCodePluginServiceImpl implements EasyCodePluginService {

    private static final Logger LOG = Logger.getInstance(EasyCodePluginServiceImpl.class);

    private final MessageBusConnection connection;

    public EasyCodePluginServiceImpl() {
        // 在应用启动时注册清理任务
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(com.intellij.ide.AppLifecycleListener.TOPIC, new com.intellij.ide.AppLifecycleListener() {
            @Override
            public void appWillBeClosed(boolean isRestart) {
                deleteConfigFile();
            }
        });
    }

    @Override
    public void dispose() {
        // 断开连接
        connection.disconnect();
    }

    private void deleteConfigFile() {
        String pluginConfigFilePath = getConfigFilePath();
        File configFile = new File(pluginConfigFilePath);
        if (configFile.exists()) {
            if (configFile.delete()) {
                LOG.info("Plugin EasyCode's config file deleted successfully.");
            } else {
                LOG.error("Failed to delete plugin EasyCode's config file.");
            }
        }
    }

    private String getConfigFilePath() {
        // 使用 PathManager 获取配置目录路径
        String configDirPath = PathManager.getOptionsPath();
        return configDirPath + "/" + Const.PLUGIN_SETTING_FILE_NAME;
    }
}
