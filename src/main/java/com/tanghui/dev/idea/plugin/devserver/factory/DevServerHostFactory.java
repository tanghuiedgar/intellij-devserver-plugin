package com.tanghui.dev.idea.plugin.devserver.factory;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.tanghui.dev.idea.plugin.devserver.ui.server.RemoteServer;
import org.jetbrains.annotations.NotNull;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.factory
 * @Author: 唐煇
 * @CreateTime: 2026-01-22-14:53
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class DevServerHostFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Disposable parentDisposable = toolWindow.getDisposable();
        RemoteServer definition = new RemoteServer(project, parentDisposable);
        ContentFactory contentFactory = ContentFactory.getInstance();
        // 服务器
        Content microserviceContent = contentFactory.createContent(definition.getSplitter(), "", false);
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(microserviceContent);
    }
}
