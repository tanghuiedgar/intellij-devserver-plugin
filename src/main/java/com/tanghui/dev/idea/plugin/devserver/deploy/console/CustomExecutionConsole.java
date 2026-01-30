package com.tanghui.dev.idea.plugin.devserver.deploy.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.deploy.run.DevServerRunConsoleView;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import com.tanghui.dev.idea.plugin.devserver.transfer.ui.MultipleFileTransfer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.deploy.console
 * @Author: 唐煇
 * @CreateTime: 2025-12-10-10:49
 * @Description: 自定义 ExecutionConsole 包装器。
 * @Version: v1.0
 */
public class CustomExecutionConsole implements ExecutionConsole {

    private final JPanel component;
    private final JPanel console;
    private final JTabbedPane uploadTabbedPane;
    private final Splitter splitter;

    public CustomExecutionConsole() {
        this.component = new JPanel(new BorderLayout());
        this.console = new JPanel(new BorderLayout());
        this.splitter = new Splitter(false, 0.37f);
        splitter.setFirstComponent(component);   // 左
        splitter.setSecondComponent(console); // 右
        this.uploadTabbedPane = new JBTabbedPane();
        // this.component.add(console, BorderLayout.CENTER);
        this.console.add(uploadTabbedPane, BorderLayout.CENTER);
    }

    public void printHead(ConsoleView consoleView) {
        this.console.add(consoleView.getComponent(), BorderLayout.NORTH);
        this.console.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.AFTER_LAST_LINE);
        this.console.setPreferredSize(JBUI.size(0, 50));
    }


    public void print(DevServerRunConsoleView consoleView) {
        if (consoleView != null && consoleView.getConfiguration() != null) {
            JPanel infoPanel = new JPanel(new BorderLayout());
            JPanel jPanel1 = createControlPanel(consoleView);
            infoPanel.add(jPanel1, BorderLayout.NORTH);
            infoPanel.add(consoleView.getConsoleView().getComponent(), BorderLayout.CENTER);
            AnAction[] consoleActions = consoleView.getConsoleView().createConsoleActions();
            ActionManager actionManager = ActionManager.getInstance();
            DefaultActionGroup actions = new DefaultActionGroup();
            ActionToolbar actionToolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, actions, false);
            for (AnAction action : consoleActions) {
                actions.add(action);
            }
            JPanel jPanel = new JPanel(new BorderLayout());
            jPanel.add(actionToolbar.getComponent());
            jPanel.setPreferredSize(JBUI.size(40, 0));
            infoPanel.add(jPanel, BorderLayout.EAST);
            uploadTabbedPane.addTab(consoleView.getConfiguration().getServerHost(), infoPanel);
            uploadTabbedPane.updateUI();
        }
    }

    @NotNull
    private static JPanel createControlPanel(DevServerRunConsoleView runConsoleView) {
        JPanel jPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jPanel1.setPreferredSize(JBUI.size(0, 40));
        JButton logButton = runConsoleView.getLogButton();

        JButton rollbackButton = runConsoleView.getRollbackButton();
        logButton.addActionListener(e ->
                ApplicationManager.getApplication().invokeAndWait(() -> {
                            DevServerRunConfig key = runConsoleView.getConfiguration();
                            String serverHost = key.getServerHost();
                            String serverPort = key.getServerPort();
                            String serverUser = key.getServerUser();
                            String serverPassword = key.getServerPassword();
                            runConsoleView.printLog(serverHost, serverPort, serverUser, serverPassword);
                        }
                )
        );
        rollbackButton.addActionListener(e ->
                ApplicationManager.getApplication().invokeAndWait(() -> {
                            int result = Messages.showYesNoDialog(
                                    "是否回退当前升级的版本",
                                    "版本回退",
                                    DevServerBundle.INSTANCE.message("define", ""),
                                    DevServerBundle.INSTANCE.message("cancel", ""),
                                    Messages.getQuestionIcon()
                            );
                            if (result == Messages.YES && !runConsoleView.getRollback()) {
                                DevServerRunConfig key = runConsoleView.getConfiguration();
                                String serverHost = key.getServerHost();
                                String serverPort = key.getServerPort();
                                String serverUser = key.getServerUser();
                                String controlsUser = key.getControlsUser();
                                String serverPassword = key.getServerPassword();
                                runConsoleView.versionRollback(serverHost, serverPort, serverUser, controlsUser, serverPassword);
                            } else if (result == Messages.YES) {
                                ApplicationManager.getApplication().invokeLater(() ->
                                        NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                                .createNotification("版本回退", "当前升级版本已经回退！", NotificationType.INFORMATION)
                                                .addAction(new NotificationAction("") {
                                                    @Override
                                                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                                                    }
                                                }).notify(runConsoleView.getProject())
                                );
                            }
                        }
                )
        );
        jPanel1.add(logButton);
        jPanel1.add(rollbackButton);
        return jPanel1;
    }

    public void addComponent(MultipleFileTransfer component) {
        JTabbedPane fileTransfer = component.getUploadTabbedPane();
        component.getRoot().setPreferredSize(JBUI.size(500, 0));
        this.component.setPreferredSize(JBUI.size(500, 0));
        this.component.add(component.getRoot(), BorderLayout.CENTER);
        sync(fileTransfer, this.uploadTabbedPane);
    }

    /**
     * 两个组件同步
     *
     */
    public static void sync(JTabbedPane tabA, JTabbedPane tabB) {
        AtomicBoolean syncing = new AtomicBoolean(false);

        ChangeListener listener = e -> {
            if (syncing.get()) {
                return;
            }
            syncing.set(true);
            try {
                JTabbedPane source = (JTabbedPane) e.getSource();
                JTabbedPane target = (source == tabA) ? tabB : tabA;

                int index = source.getSelectedIndex();
                if (index >= 0 && index < target.getTabCount()) {
                    target.setSelectedIndex(index);
                }
            } finally {
                syncing.set(false);
            }
        };
        tabA.addChangeListener(listener);
        tabB.addChangeListener(listener);
    }

    @Override
    public @NotNull JComponent getComponent() {
        return this.splitter;
    }

    @Override
    public JComponent getPreferredFocusableComponent() {
        return this.component;
    }

    @Override
    public void dispose() {
    }
}
