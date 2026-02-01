package com.tanghui.dev.idea.plugin.devserver.utils;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.TerminalColorPalette;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.createRemoteNewTerminalComponent;

public class TerminalUtil {

    /**
     * 创建远程连接终端
     *
     * @param project        项目
     * @param hostModel      远程服务器信息
     * @param executeCommand 执行命令列表
     *
     */
    public static void createRemoteConnectionNewTerminal(Project project, ServerHostModel hostModel, List<String> executeCommand) {
        // 获取终端工具窗口
        JComponent remoteNewTerminalComponent = createRemoteNewTerminalComponent(project, hostModel, executeCommand);
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        // 打开终端窗口
        Disposable disposable = toolWindow.getDisposable();
        Disposable tempDisposable = Disposer.newDisposable();
        Disposer.register(disposable, tempDisposable);
        // 创建新的Content对象，并添加进工具窗口
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(remoteNewTerminalComponent, hostModel.getHost(), false);
        remoteNewTerminalComponent.setVisible(true);
        toolWindow.getContentManager().addContent(content);
        toolWindow.show();
        toolWindow.getContentManager().setSelectedContent(content);
    }


    @NotNull
    public static JBTerminalWidget getJbTerminalWidget(Project project, Disposable tempDisposable) {
        JBTerminalSystemSettingsProviderBase jbTerminalSystemSettingsProvider = new JBTerminalSystemSettingsProviderBase() {
            @Override
            public float getColumnSpacing() {
                return 0.0f;
            }

            @NotNull
            @Override
            public TerminalColorPalette getTerminalColorPalette() {
                return new MyColorPalette();
            }
        };
        return new JBTerminalWidget(project, 201, 24, jbTerminalSystemSettingsProvider, null, tempDisposable);
    }
}
