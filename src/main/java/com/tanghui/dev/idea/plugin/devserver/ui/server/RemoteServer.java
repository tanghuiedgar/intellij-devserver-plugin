package com.tanghui.dev.idea.plugin.devserver.ui.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.tabs.JBTabsPosition;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import com.intellij.ui.treeStructure.Tree;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.data.model.FileTransferModel;
import com.tanghui.dev.idea.plugin.devserver.data.model.OperateEnum;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostFileModel;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons;
import com.tanghui.dev.idea.plugin.devserver.pool.GlobalSshPoolManager;
import com.tanghui.dev.idea.plugin.devserver.pool.SshConnectionPool;
import com.tanghui.dev.idea.plugin.devserver.renderer.ServerHostTreeCellRenderer;
import com.tanghui.dev.idea.plugin.devserver.task.FileTransferCallback;
import com.tanghui.dev.idea.plugin.devserver.transfer.ui.FileTransfer;
import com.tanghui.dev.idea.plugin.devserver.tree.ServerHostTreeNode;
import com.tanghui.dev.idea.plugin.devserver.ui.dialog.connectserver.ConnectServerDialog;
import com.tanghui.dev.idea.plugin.devserver.ui.dialog.execute.EditCommandDialog;
import com.tanghui.dev.idea.plugin.devserver.ui.dialog.upload.UploadFileActionDialog;
import com.tanghui.dev.idea.plugin.devserver.ui.server.lastright.ExecuteRightComponent;
import com.tanghui.dev.idea.plugin.devserver.ui.server.lastright.LastRightComponent;
import com.tanghui.dev.idea.plugin.devserver.utils.file.FileUtil;
import lombok.Getter;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.tanghui.dev.idea.plugin.devserver.utils.DevServerUtil.createActionToolBar;
import static com.tanghui.dev.idea.plugin.devserver.utils.JTreeUtil.*;
import static com.tanghui.dev.idea.plugin.devserver.utils.MarkdownToHtmlTool.getJBCefBrowser;
import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.*;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.ui.server
 * @Author: 唐煇
 * @CreateTime: 2026-01-22-14:45
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
@Getter
public class RemoteServer implements Disposable {
    // 上部分
    private JComponent lastComponent;
    // 下部分
    private JComponent nextComponent;

    private JBEditorTabs terminalTabs;

    // 文件上传 组件
    private JPanel jPanel;

    private final Splitter splitter;

    private final Project project;

    private final Disposable parentDisposable;

    private final LastLeftComponent lastLeft;

    private final LastRightComponent lastRight;

    public static List<ServerHostModel> hostModels = new ArrayList<>();

    private static int terminalSize = 0;

    private static Map<String, FileTransfer> fileTransfers = new LinkedHashMap<>();

    public RemoteServer(Project project, Disposable parentDisposable) {
        this.project = project;
        this.parentDisposable = parentDisposable;
        this.splitter = new Splitter(true, 0.5f);
        this.lastLeft = new LastLeftComponent(project);
        this.lastRight = new LastRightComponent(project, this.lastLeft.getServerTree(), parentDisposable);
        initLastComponent();
        initNextComponent();
        splitter.setFirstComponent(lastComponent);
        splitter.setSecondComponent(nextComponent);
        initServerOperateToolbar();
        initOperateBar();
        initServerFileOperationToolbar();
        initCommandToolbar();
        initServerHostTree(this.lastRight, this.lastLeft.getServerTree());
        initJTable(this.lastRight, this.lastLeft.getServerTree());
        initCommandTree(this.lastLeft.getServerTree(), null);
        initCommandInfo(this.lastLeft.getServerTree(), this.lastRight.getExecuteLeft().getCommandTree());
        this.lastRight.getServerPanel().setVisible(false);
        showPreview(lastRight);
        this.lastLeft.getServerTree().addTreeSelectionListener(e -> {
            // 选择服务器节点监控
            initJTable(lastRight, this.lastLeft.getServerTree());
            initCommandTree(this.lastLeft.getServerTree(), null);
            initCommandInfo(this.lastLeft.getServerTree(), this.lastRight.getExecuteLeft().getCommandTree());
            lastRight.getServerPanel().setVisible(false);
            showPreview(lastRight);
            if (lastRight.getServerInfoTabbedPane().getTabCount() > 0) {
                lastRight.getServerInfoTabbedPane().setSelectedIndex(0);
            }
        });
        ApplicationManager.getApplication().invokeLater(() -> {
            if (lastRight.getServerInfoTabbedPane().getTabCount() > 0) {
                lastRight.getServerInfoTabbedPane().setSelectedIndex(0);
            }
        });
    }

    /**
     * serverNode: 选择的节点
     * */
    public void initCommandTree(JTree serverHostTree, JSONObject serverNode) {
        if (hostModels != null && !hostModels.isEmpty()) {
            TreePath selectedPath = serverHostTree.getSelectionPath();
            String hostName;
            if (selectedPath != null) {
                ServerHostTreeNode selectedNode = (ServerHostTreeNode) selectedPath.getLastPathComponent();
                hostName = selectedNode.getName();
            } else {
                hostName = "";
            }
            hostModels.stream()
                    .filter(v -> hostName.equals(v.getHost()))
                    .findFirst()
                    .ifPresent(v -> {
                        String command = v.getCommand();
                        ServerHostTreeNode defaultMutableTreeNode = new ServerHostTreeNode("执行命令", DevServerIcons.DevServer_APP);
                        AtomicReference<ServerHostTreeNode> serverHostTreeNodeTemp = new AtomicReference<>();
                        if (StringUtils.isNotBlank(command)) {
                            JSONArray objects = JSON.parseArray(command);
                            if (!objects.isEmpty()) {
                                objects.forEach(m -> {
                                    JSONObject jsonObject = JSON.parseObject(m.toString());
                                    String path = jsonObject.getString("path");
                                    String title = jsonObject.getString("title");
                                    if (StringUtils.isNotBlank(path)) {
                                        ServerHostTreeNode treeNode = getServerHostTreeNode(path, defaultMutableTreeNode);
                                        if (treeNode != null) {
                                            ServerHostTreeNode serverHostTreeNode = new ServerHostTreeNode(title, DevServerIcons.DevServer_TOOLBAR_EXECUTE, true);
                                            treeNode.add(serverHostTreeNode);
                                            if (serverNode != null) {
                                                // 选中的节点
                                                String serverPath = serverNode.getString("path") == null ? "" : serverNode.getString("path");
                                                String serverTitle = serverNode.getString("title") == null ? "" : serverNode.getString("title");
                                                if (serverPath.equals(path) && serverTitle.equals(title)) {
                                                    serverHostTreeNodeTemp.set(serverHostTreeNode);
                                                }
                                            } else {
                                                // 设置默认选择的节点
                                                if (objects.indexOf(m) == 0) {
                                                    serverHostTreeNodeTemp.set(serverHostTreeNode);
                                                }
                                            }
                                        }
                                    } else {
                                        ServerHostTreeNode serverHostTreeNode = new ServerHostTreeNode(title, DevServerIcons.DevServer_TOOLBAR_EXECUTE, true);
                                        if (serverNode != null) {
                                            // 选中的节点
                                            String serverTitle = serverNode.getString("title") == null ? "" : serverNode.getString("title");
                                            if (serverTitle.equals(title)) {
                                                serverHostTreeNodeTemp.set(serverHostTreeNode);
                                            }
                                        } else {
                                            // 默认选择第一个节点
                                            if (objects.indexOf(m) == 0) {
                                                serverHostTreeNodeTemp.set(serverHostTreeNode);
                                            }
                                        }
                                        defaultMutableTreeNode.add(serverHostTreeNode);
                                    }
                                });
                            }
                        }
                        Tree commandTree = lastRight.getExecuteLeft().getCommandTree();
                        JLabel pathInfoLabel = lastRight.getExecuteRight().getPathInfoLabel();
                        pathInfoLabel.setText(v.getServerGroupBy().replace("//", " -> "));
                        commandTree.setModel(new DefaultTreeModel(defaultMutableTreeNode));
                        commandTree.setCellRenderer(new ServerHostTreeCellRenderer());
                        if (serverHostTreeNodeTemp.get() != null) {
                            ServerHostTreeNode serverHostTreeNode = serverHostTreeNodeTemp.get();
                            TreePath defaultSelectionPath = new TreePath(serverHostTreeNode.getPath());
                            commandTree.getSelectionModel().setSelectionPath(defaultSelectionPath);
                            // 设置默认展开的节点
                            expandAll(commandTree);
                            // commandTree.expandPath(new TreePath(serverHostTreeNode.getPath()));
                        }
                        commandTree.addTreeSelectionListener(e -> initCommandInfo(serverHostTree, commandTree));
                        commandTree.updateUI();
                    });
        }
    }

    public void initCommandInfo(JTree serverHostTree, Tree commandTree) {
        JSONObject executeJsonObject = getSelectedCommandJson(serverHostTree, commandTree);
        if (executeJsonObject != null) {
            ExecuteRightComponent executeRight = lastRight.getExecuteRight();
            executeRight.getDirectoryTextField().setText(executeJsonObject.getString("directory"));
            executeRight.getUserTextField().setText(executeJsonObject.getString("user"));
            executeRight.getIllustrateTextPane().setText(executeJsonObject.getString("illustrate"));
            executeRight.getExecuteCommand().setText(executeJsonObject.getString("executeCommand"));
            Boolean script = executeJsonObject.getBoolean("scriptType");
            executeRight.getScriptType().setSelected(script != null && script);
        }
    }


    private void initLastComponent() {
        lastComponent = new JBPanel(new BorderLayout());
        Splitter lastSplitter = new Splitter(false, 0.3f);
        lastSplitter.setFirstComponent(this.lastLeft.getRoot());
        lastSplitter.setSecondComponent(this.lastRight.getRoot());
        lastComponent.add(lastSplitter, BorderLayout.CENTER);
    }

    private void initNextComponent() {
        this.terminalTabs = new JBEditorTabs(project, this);
        // 设置 Tab 也就是每个标签页的各个方向的边距等
        this.terminalTabs.getPresentation()
                .setTabsPosition(JBTabsPosition.top) // Tab 在顶部
                .setSingleRow(true)
                .setTabLabelActionsAutoHide(false)
                .setTabDraggingEnabled(true);              // 允许拖拽排序
        // 添加监听器（可选，用于监听选中切换或移除事件）
        this.terminalTabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                // Tab 切换时触发
            }

            @Override
            public void tabRemoved(@NotNull TabInfo tabInfo) {
                // Tab 被移除后触发，用于资源回收
                // System.out.println("Tab removed: " + tabInfo.getText());
                if (terminalTabs.getTabCount() == 0) {
                    nextComponent.setVisible(false);
                }
            }
        });
        nextComponent = this.terminalTabs.getComponent();
        nextComponent.setVisible(false);
    }

    /**
     * 核心方法：动态添加一个 Tab
     */
    public void addNewTerminalTab(String title, JComponent terminal) {
        if (terminalSize >= 6) {
            NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                    .createNotification("打开终端", "当前打开终端过多，请关闭没有使用的终端", NotificationType.WARNING)
                    .notify(project);
            return;
        }
        terminalSize++;
        // 1. 创建 Tab 的内容组件 (这里演示用一个随机颜色的 Panel)
        JPanel content = new JPanel(new BorderLayout());
        // content.setBackground(getRandomColor());
        content.add(terminal, BorderLayout.CENTER);
        // 2. 创建 TabInfo (数据模型)
        TabInfo info = new TabInfo(content);
        info.setText(title);
        // info.setTooltipText("这是 " + title + " 的详细提示");
        info.setIcon(DevServerIcons.DevServer_TOOLBAR_TERMINAL); // 设置一个小图标
        // 3. 【关键】设置关闭按钮
        // 我们创建一个 ActionGroup，里面放一个关闭动作。
        // JBEditorTabs 会自动在 Tab 标题右侧渲染这个 ActionGroup。
        DefaultActionGroup closeActionGroup = new DefaultActionGroup();
        closeActionGroup.add(new AnAction("在终端打开", "在终端打开", DevServerIcons.DevServer_TOOLBAR_TERMINAL) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 执行移除逻辑
                JComponent component = info.getComponent();
                // 获取终端工具窗口
                ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
                if (toolWindow == null) {
                    return;
                }
                toolWindow.activate(() -> {
                    // 3. 获取 ToolWindowManagerEx 实例并设置最大化
                    ToolWindowManagerEx windowManager = ToolWindowManagerEx.getInstanceEx(project);
                    // 参数1: 工具窗口实例
                    // 参数2: true 表示最大化，false 表示恢复默认大小
                    windowManager.setMaximized(toolWindow, true);
                });
                // 创建新的Content对象，并添加进工具窗口
                ContentFactory contentFactory = ContentFactory.getInstance();
                Content content = contentFactory.createContent(component, info.getText(), false);
                toolWindow.getContentManager().addContent(content);
                toolWindow.show();
                toolWindow.getContentManager().setSelectedContent(content);
                terminalTabs.removeTab(info);
                terminalSize--;
            }
        });
        closeActionGroup.add(new AnAction("关闭", "关闭当前终端", DevServerIcons.DevServer_CLOSE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 执行移除逻辑
                terminalTabs.removeTab(info);
                terminalSize--;
            }
        });
        closeActionGroup.add(new AnAction("清空", "关闭所有终端", DevServerIcons.DevServer_CLEAR) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 执行移除逻辑
                terminalTabs.removeAllTabs();
                terminalSize = 0;
            }
        });
        // 将关闭动作组绑定到 TabInfo
        info.setTabPaneActions(closeActionGroup);
        // 4. 将 Tab 添加到组件并选中
        terminalTabs.addTab(info);
        terminalTabs.select(info, true);
    }


    /**
     * 初始化 serverHostTree
     */
    public static void initServerHostTree(LastRightComponent lastRight, JTree serverHostTree) {
        ServerHostTreeNode defaultMutableTreeNode = new ServerHostTreeNode("服务器", DevServerIcons.DevServer_APP);
        // 查询保存的服务器
        hostModels.clear();
        Path configDir = PathManager.getConfigDir();
        Path parent = configDir.getParent();
        if (parent != null) {
            // 有上级目录
            configDir = parent;
        }
        String mapperPath = configDir.toString();
        String mapperFileName = "ServerHost.json";
        File file = new File(mapperPath + File.separator + "config" + File.separator + mapperFileName);
        JSONArray objects = new JSONArray();
        if (file.exists()) {
            String fieldMapperJson = FileUtil.getInstance().getFileContents(file);
            objects = JSON.parseArray(fieldMapperJson);
        }
        AtomicReference<ServerHostTreeNode> serverHostTreeNodeTemp = new AtomicReference<>();
        if (!objects.isEmpty()) {
            JSONArray finalObjects = objects;
            objects.forEach(v -> {
                JSONObject serverHost = (JSONObject) v;
                String serverGroupBy = serverHost.getString("serverGroupBy");
                if (StringUtils.isNotBlank(serverGroupBy)) {
                    ServerHostTreeNode treeNode = getServerHostTreeNode(serverGroupBy, defaultMutableTreeNode);
                    if (treeNode != null) {
                        ServerHostTreeNode serverHostTreeNode = new ServerHostTreeNode(serverHost.getString("host"), DevServerIcons.DevServer_SERVER, true);
                        treeNode.add(serverHostTreeNode);
                        // 设置默认选择的节点
                        if (finalObjects.indexOf(v) == 0) {
                            serverHostTreeNodeTemp.set(serverHostTreeNode);
                        }
                    }
                } else {
                    ServerHostTreeNode serverHostTreeNode = new ServerHostTreeNode(serverHost.getString("host"), DevServerIcons.DevServer_SERVER, true);
                    if (finalObjects.indexOf(v) == 0) {
                        serverHostTreeNodeTemp.set(serverHostTreeNode);
                    }
                    defaultMutableTreeNode.add(serverHostTreeNode);
                }
                String environment = serverHost.getString("environment");
                String host = serverHost.getString("host");
                String userName = serverHost.getString("userName");
                String password = serverHost.getString("password");
                String command = serverHost.getString("command");
                String serverInfo = serverHost.getString("serverInfo");
                String path = serverHost.getString("path");
                long count = hostModels.stream().filter(m -> host.equals(m.getHost())).count();
                if (count < 1) {
                    ServerHostModel serverHostModel = new ServerHostModel();
                    serverHostModel.setEnvironment(environment);
                    serverHostModel.setHost(host);
                    serverHostModel.setUserName(userName);
                    serverHostModel.setPassword(password);
                    serverHostModel.setPath(path);
                    serverHostModel.setServerGroupBy(serverGroupBy);
                    serverHostModel.setCommand(command);
                    serverHostModel.setServerInfo(serverInfo);
                    hostModels.add(serverHostModel);
                }
            });
        }
        serverHostTree.setModel(new DefaultTreeModel(defaultMutableTreeNode));
        serverHostTree.setCellRenderer(new ServerHostTreeCellRenderer());
        if (serverHostTreeNodeTemp.get() != null) {
            ServerHostTreeNode serverHostTreeNode = serverHostTreeNodeTemp.get();
            TreePath defaultSelectionPath = new TreePath(serverHostTreeNode.getPath());
            serverHostTree.getSelectionModel().setSelectionPath(defaultSelectionPath);
            // 设置默认展开全部节点
            expandAll(serverHostTree);
        }
        serverHostTree.updateUI();
    }

    private void showPreview(LastRightComponent lastRight) {
        // 预览
        TreePath selectedPath = lastRight.getServerTree().getSelectionPath();
        String hostName;
        if (selectedPath != null) {
            ServerHostTreeNode selectedNode = (ServerHostTreeNode) selectedPath.getLastPathComponent();
            hostName = selectedNode.getName();
        } else {
            hostName = "";
        }
        hostModels.stream()
                .filter(v -> hostName.equals(v.getHost()))
                .findFirst()
                .ifPresent(v -> {
                    // 清空原来的页面
                    lastRight.getDetailPanel().removeAll();
                    lastRight.getDetailPanel().setBackground(JBColor.WHITE);
                    JBCefBrowser browser = getJBCefBrowser(v.getServerInfo());
                    BrowserPanel browserPanel = new BrowserPanel(
                            project,
                            browser,
                            parentDisposable,
                            v
                    );
                    lastRight.getDetailPanel().add(browserPanel.getComponent(), BorderLayout.CENTER);
                    lastRight.getDetailPanel().updateUI();
                });
    }


    public static void initJTable(LastRightComponent lastRight, JTree serverHostTree) {
        if (hostModels != null && !hostModels.isEmpty()) {
            TreePath selectedPath = serverHostTree.getSelectionPath();
            String hostName;
            if (selectedPath != null) {
                ServerHostTreeNode selectedNode = (ServerHostTreeNode) selectedPath.getLastPathComponent();
                hostName = selectedNode.getName();
            } else {
                hostName = "";
            }
            hostModels.stream()
                    .filter(v -> hostName.equals(v.getHost()))
                    .findFirst()
                    .ifPresent(v -> {
                        String host = v.getHost();
                        String userName = v.getUserName();
                        String password = v.getPassword();
                        lastRight.getServerHostTextField().setText(host);
                        lastRight.getServerUserTextField().setText(userName);
                        if (LastRightComponent.passwordShow) {
                            lastRight.getServerPassWordTextField().setText(password);
                        } else {
                            lastRight.getServerPassWordTextField().setText("**********");
                        }
                    });
        }
    }

    private void initServerOperateToolbar() {
        // 添加工具栏
        List<AnAction> consoleActions = new ArrayList<>();
        AnAction saveAction = new AnAction("保存", "", DevServerIcons.DevServer_TOOLBAR_SAVE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (hostModels != null && !hostModels.isEmpty()) {
                    saveServerHost(hostModels);
                } else {
                    Messages.showErrorDialog(project, "服务器信息不能为空！", "服务器保存");
                }
            }
        };
        AnAction refreshAction = new AnAction("刷新", "", DevServerIcons.DevServer_RESTART) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                nextComponent.setVisible(false);
                terminalTabs.removeAllTabs();
                terminalSize = 0;
                initJTable(lastRight, lastLeft.getServerTree());
                lastRight.getServerDirectoryTextField().setText("");
                lastRight.getServerPanel().setVisible(false);
                GlobalSshPoolManager.shutdownAll();
            }
        };
        consoleActions.add(new AnAction("新增", "", DevServerIcons.DevServer_TOOLBAR_ADD) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ServerHostModel serverHostModel = new ServerHostModel();
                ConnectServerDialog connectServerDialog = new ConnectServerDialog(project, OperateEnum.ADD, serverHostModel);
                connectServerDialog.showAndGet();
                if (connectServerDialog.isConnect()) {
                    hostModels.addFirst(connectServerDialog.getServerHostModel());
                    saveServerHost(hostModels);
                    initServerHostTree(lastRight, lastLeft.getServerTree());
                }
            }
        });
        consoleActions.add(new AnAction("修改", "", DevServerIcons.DevServer_TOOLBAR_UPDATE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreePath selectionModel = lastLeft.getServerTree().getSelectionPath();
                if (selectionModel != null) {
                    // 获取选中的节点
                    ServerHostTreeNode selectedNode = (ServerHostTreeNode) selectionModel.getLastPathComponent();
                    // 判断选中节点是否是叶子节点
                    boolean isLeaf = selectedNode.isLeaf();
                    if (isLeaf && selectedNode.getParent() != null) {
                        String name = selectedNode.getName();
                        Optional<ServerHostModel> serverHostModel = hostModels.stream().filter(v -> name.equals(v.getHost())).findFirst();
                        serverHostModel.ifPresent(hostModel -> {
                            ConnectServerDialog connectServerDialog = new ConnectServerDialog(project, OperateEnum.UPDATE, hostModel);
                            connectServerDialog.showAndGet();
                            if (connectServerDialog.isConnect()) {
                                hostModels.remove(hostModel);
                                hostModels.addFirst(connectServerDialog.getServerHostModel());
                                saveServerHost(hostModels);
                                initServerHostTree(lastRight, lastLeft.getServerTree());
                            }
                        });
                    }
                }
            }
        });
        consoleActions.add(new AnAction("删除", "", DevServerIcons.DevServer_TOOLBAR_DELETE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreePath selectionModel = lastLeft.getServerTree().getSelectionPath();
                if (selectionModel != null) {
                    // 获取选中的节点
                    ServerHostTreeNode selectedNode = (ServerHostTreeNode) selectionModel.getLastPathComponent();
                    // 判断选中节点是否是叶子节点
                    boolean isLeaf = selectedNode.isLeaf();
                    if (isLeaf && selectedNode.getParent() != null) {
                        String name = selectedNode.getName();
                        Optional<ServerHostModel> serverHostModel = hostModels.stream().filter(v -> name.equals(v.getHost())).findFirst();
                        serverHostModel.ifPresent(hostModel -> {
                            ConnectServerDialog connectServerDialog = new ConnectServerDialog(project, OperateEnum.DELETE, hostModel);
                            connectServerDialog.showAndGet();
                            if (connectServerDialog.isConnect()) {
                                initServerHostTree(lastRight, lastLeft.getServerTree());
                            }
                        });
                    }
                }
            }
        });
        consoleActions.add(saveAction);
        consoleActions.add(refreshAction);
        consoleActions.add(new AnAction("展开", "", DevServerIcons.DevServer_EXPAND_DARK) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreePath sel = lastLeft.getServerTree().getSelectionPath();
                boolean selectedFileNode = sel != null && sel.getLastPathComponent() instanceof ServerHostTreeNode;
                if (!selectedFileNode) {
                    expandAll(lastLeft.getServerTree());
                } else {
                    if (sel.getPathCount() == 1) {
                        // 选中根节点
                        expandAll(lastLeft.getServerTree());
                    } else {
                        expandNode(lastLeft.getServerTree(), sel);
                    }
                }
            }
        });
        consoleActions.add(new AnAction("折叠", "", DevServerIcons.DevServer_COLLAPSE_DARK) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreePath sel = lastLeft.getServerTree().getSelectionPath();
                boolean selectedFileNode = sel != null && sel.getLastPathComponent() instanceof ServerHostTreeNode;
                if (!selectedFileNode) {
                    collapseAllExceptRoot(lastLeft.getServerTree());
                } else {
                    if (sel.getPathCount() == 1) {
                        // 选中根节点
                        collapseAllExceptRoot(lastLeft.getServerTree());
                    } else {
                        collapseDeep(lastLeft.getServerTree(), sel);
                    }
                }
            }
        });
        ActionToolbar actionToolBar = createActionToolBar(ActionPlaces.TOOLBAR, true, consoleActions);
        lastLeft.getServerOperateToolbar().add(actionToolBar.getComponent());
        lastLeft.getServerOperateToolbar().setVisible(true);
    }

    private void initOperateBar() {
        List<AnAction> consoleActions = new ArrayList<>();
        consoleActions.add(new AnAction("连接", "", DevServerIcons.DevServer_TOOLBAR_CONNECTION) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 获取 ip 和用户名
                String serverHost = lastRight.getServerHostTextField().getText().trim();
                String userName = lastRight.getServerUserTextField().getText().trim();
                hostModels.stream()
                        .filter(v -> serverHost.equals(v.getHost()) && userName.equals(v.getUserName()))
                        .findFirst()
                        .ifPresent(v -> new Task.Backgroundable(project, "连接服务器") {
                            @Override
                            public void run(@NotNull ProgressIndicator progressIndicator) {
                                progressIndicator.setIndeterminate(true);
                                SshConnectionPool pool = GlobalSshPoolManager.getPool(
                                        v.getHost(),
                                        v.getPort(),
                                        v.getUserName(),
                                        v.getPassword()
                                );
                                SSHClient sshClient = null;
                                try {
                                    sshClient = pool.borrow();
                                    // 检查连接是否成功
                                    if (sshClient.isConnected()) {
                                        ServerHostTreeNode defaultMutableTreeNode = new ServerHostTreeNode(v.getHost(), DevServerIcons.DevServer_SERVER, true);
                                        ServerHostTreeNode rootDefaultMutableTreeNode = new ServerHostTreeNode("/", DevServerIcons.DevServer_PACKAGE);
                                        defaultMutableTreeNode.add(rootDefaultMutableTreeNode);
                                        ApplicationManager.getApplication().invokeAndWait(() -> {
                                            lastRight.getServerDirectoryTextField().setText("/");
                                            lastRight.getServerDirectoryTextField().updateUI();
                                        });
                                        ApplicationManager.getApplication().invokeAndWait(() -> {
                                            Tree serverDirectoryTree = lastRight.getDirectoryTree().getServerDirectoryTree();
                                            serverDirectoryTree.setModel(new DefaultTreeModel(defaultMutableTreeNode));
                                            // 默认展开 / 节点
                                            TreePath treePath = new TreePath(rootDefaultMutableTreeNode.getPath());
                                            serverDirectoryTree.expandPath(treePath);
                                            // 默认选择根节点
                                            serverDirectoryTree.getSelectionModel().setSelectionPath(treePath);
                                            serverDirectoryTree.updateUI();
                                        });
                                        lastRight.getServerPanel().setVisible(true);
                                        String text = "服务器连接成功！";
                                        NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                                .createNotification("服务器连接", text, NotificationType.INFORMATION)
                                                .notify(project);
                                    } else {
                                        ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(project, "服务器连接失败！", "服务器连接", Messages.getErrorIcon()));
                                    }
                                } catch (ProcessCanceledException pce) {
                                    // 必须重新抛出
                                    throw pce;
                                } catch (Exception exception) {
                                    ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(project, "服务器连接失败！", "服务器连接", Messages.getErrorIcon()));
                                } finally {
                                    pool.release(sshClient);
                                }
                            }
                        }.queue());
            }
        });
        consoleActions.add(new AnAction("终端", "", DevServerIcons.DevServer_TOOLBAR_TERMINAL) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 获取 ip 和用户名
                String serverHost = lastRight.getServerHostTextField().getText().trim();
                String userName = lastRight.getServerUserTextField().getText().trim();
                hostModels.stream()
                        .filter(v -> serverHost.equals(v.getHost()) && userName.equals(v.getUserName()))
                        .findFirst()
                        .ifPresent(v -> {
                            List<String> executeCommand = new ArrayList<>();
                            String directory = lastRight.getServerDirectoryTextField().getText().trim();
                            if (StringUtils.isNotBlank(directory)) {
                                executeCommand.add("cd " + directory);
                            }
                            addNewTerminalTab(v.getHost(), createRemoteNewTerminalComponent(project, v, executeCommand));
                            nextComponent.setVisible(true);
                            nextComponent.revalidate();  // 重新触发布局
                            nextComponent.repaint();     // 重新绘制
                        });
            }
        });
        AnAction show = getAnAction();
        consoleActions.add(show);

        /*consoleActions.add(new AnAction("保存", "", DevServerIcons.DevServer_TOOLBAR_SAVE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                        .createNotification("服务器", "修改已保存", NotificationType.INFORMATION)
                        .notify(project);
            }
        });*/

        ActionToolbar actionToolBar = createActionToolBar(ActionPlaces.TOOLBAR, true, consoleActions);
        lastRight.getConnectionOperationToolbar().add(actionToolBar.getComponent());
        lastRight.getConnectionOperationToolbar().setVisible(true);
    }

    private @NotNull AnAction getAnAction() {
        return new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                LastRightComponent.passwordShow = !LastRightComponent.passwordShow;
                initJTable(lastRight, lastLeft.getServerTree());
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                if (LastRightComponent.passwordShow) {
                    presentation.setText("隐藏");
                    presentation.setIcon(DevServerIcons.DevServer_TOOLBAR_HIDE);
                } else {
                    presentation.setText("显示");
                    presentation.setIcon(DevServerIcons.DevServer_TOOLBAR_SHOW);
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
    }

    private Optional<ServerHostModel> findSelectedHostModel() {
        // 获取 ip 和用户名
        String serverHost = lastRight.getServerHostTextField().getText().trim();
        String userName = lastRight.getServerUserTextField().getText().trim();
        // 获取目标服务器
        return hostModels.stream()
                .filter(v -> serverHost.equals(v.getHost())
                        && userName.equals(v.getUserName()))
                .findFirst();
    }

    public void initServerFileOperationToolbar() {
        List<AnAction> consoleActions = new ArrayList<>();
        AnAction openAction = new AnAction("打开文件", "", DevServerIcons.DevServer_TOOLBAR_OPENFILE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                JBList<ServerHostFileModel> directoryList = lastRight.getDirectoryList().getDirectoryList();
                int selectedIndex = directoryList.getSelectedIndex();
                if (selectedIndex < 0) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                    DevServerBundle.INSTANCE.message("server.host.open.file.choose", ""), NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                Optional<ServerHostModel> hostFirst = findSelectedHostModel();
                if (!hostFirst.isPresent()) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                    "请连接正确的服务器", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                ServerHostModel hostModel = hostFirst.get();
                // 获取路径
                Object[] path = lastRight.getDirectoryTree().getServerDirectoryTree().getSelectionModel().getLeadSelectionPath().getPath();
                if (path.length <= 1) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                    "请连接正确的文件目录", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                String filePath = getFilePath(path);
                ListModel<ServerHostFileModel> model = directoryList.getModel();
                ServerHostFileModel elementAt = model.getElementAt(selectedIndex);
                long MAX_3MB = 3L * 1024 * 1024;
                if (elementAt.getFileSize() > MAX_3MB) {
                    // 大于 3MB
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                    "文件大于3MB，不要直接打开，请先下载之后再打开", NotificationType.WARNING)
                            .notify(project);
                    return;
                }

                // 获取文件名称
                String fileName = elementAt.getFileName();
                // 获取临时目录
                String tempDir = System.getProperty("java.io.tmpdir");
                tempDir = tempDir.replaceAll("[/\\\\]+$", "");
                String localFilePath = tempDir + File.separator + "devServer" + File.separator + hostModel.getHost() + File.separator + fileName;
                String remoteFilePath = filePath + "/" + fileName;
                File local = new File(localFilePath);
                // 如果目标文件已存在，删除它
                if (local.exists()) {
                    boolean delete = local.delete();
                    if (!delete) {
                    }
                } else {
                    Path fileLocalPath = Paths.get(localFilePath);
                    // 1. 创建父目录（如果不存在）
                    try {
                        Files.createDirectories(fileLocalPath.getParent());
                    } catch (IOException ex) {
                    }
                    // 2. 创建文件（如果不存在）
                    if (Files.notExists(fileLocalPath)) {
                        try {
                            Files.createFile(fileLocalPath);
                        } catch (IOException ex) {
                        }
                    }
                }

                new Task.Backgroundable(project, "正在打开服务器文件...") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        try {
                            SshConnectionPool pool = GlobalSshPoolManager.getPool(
                                    hostModel.getHost(),
                                    hostModel.getPort(),
                                    hostModel.getUserName(),
                                    hostModel.getPassword()
                            );
                            SSHClient ssh = pool.borrow();
                            try (SFTPClient sftp = ssh.newSFTPClient()) {
                                sftp.get(remoteFilePath, localFilePath);
                                // 下载完成打开文件
                                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                                    LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
                                    // 通过路径获取 VirtualFile
                                    VirtualFile file = localFileSystem.findFileByPath(localFilePath);
                                    // 使用 OpenFileDescriptor 打开文件
                                    if (file != null) {
                                        boolean allowOpen = false;
                                        // 获取 FileTypeManager 实例
                                        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
                                        // 获取所有支持的文件类型
                                        FileType[] allFileTypes = fileTypeManager.getRegisteredFileTypes();
                                        String extension = file.getExtension();
                                        for (FileType fileTypeTemp : allFileTypes) {
                                            if (extension.equals(fileTypeTemp.getDefaultExtension())) {
                                                allowOpen = true;
                                                break;
                                            }
                                        }
                                        if (!allowOpen) {
                                            FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFile(file);
                                            if (fileType instanceof PlainTextFileType /*|| fileType instanceof IDEPlainTextFileType*/) {
                                                // 文件可以用文字打开
                                                allowOpen = true;
                                            }
                                        }
                                        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
                                        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                                        // 在编辑器中打开文件
                                        boolean finalAllowOpen = allowOpen;
                                        ApplicationManager.getApplication().invokeAndWait(() -> {
                                            if (finalAllowOpen) {
                                                fileEditorManager.openTextEditor(descriptor, true);
                                            } else {
                                                fileEditorManager.openFile(file, true);
                                            }
                                        });
                                    }
                                });
                            } catch (IOException e) {
                                NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                        .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                                "打开文件失败！", NotificationType.WARNING)
                                        .notify(project);
                            } finally {
                                pool.release(ssh);
                            }
                        } catch (Exception e) {
                            NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                    .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                            "打开文件失败！", NotificationType.WARNING)
                                    .notify(project);
                        }
                    }
                }.queue();

            }
        };
        AnAction uploadAction = new AnAction("上传文件", "", DevServerIcons.DevServer_TOOLBAR_UPLOAD) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Optional<ServerHostModel> hostFirst = findSelectedHostModel();
                if (!hostFirst.isPresent()) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                    "请连接正确的服务器", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                ServerHostModel hostModel = hostFirst.get();
                // 获取路径
                Object[] path = lastRight.getDirectoryTree().getServerDirectoryTree().getSelectionModel().getLeadSelectionPath().getPath();
                if (path.length <= 1) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                    "请连接正确的文件目录", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                String filePath = getFilePath(path);
                FileTransferModel fileTransferModel = new FileTransferModel();
                fileTransferModel.setRemoteFilesPath(filePath);
                fileTransferModel.setOffset(0L);
                fileTransferModel.setState(Boolean.TRUE);
                String uuid = UUID.randomUUID().toString().replace("-", "");
                FileTransferCallback fileTransferCallback = new FileTransferCallback() {
                    @Override
                    public void callback() {
                        // 任务成功完成时的处理逻辑
                        SshConnectionPool pool = GlobalSshPoolManager.getPool(
                                hostModel.getHost(),
                                hostModel.getPort(),
                                hostModel.getUserName(),
                                hostModel.getPassword()
                        );
                        SSHClient ssh = null;
                        Session session = null;
                        try {
                            // 连接到 SFTP 服务器
                            ssh = pool.borrow();
                            session = ssh.startSession();
                            // 获取目录所属用户和用户组
                            String output = execStdout(session, "ls -ld " + filePath);

                            // 解析输出，假设是类似于 "-rwxr-xr-x 2 user group 4096 Oct  5 12:34 directory" 的格式
                            String[] lines = output.split("\n");
                            if (lines.length > 0) {
                                String[] columns = lines[0].split("\\s+");
                                if (columns.length >= 4) {
                                    String user = columns[2];  // 用户
                                    String group = columns[3]; // 用户组
                                    // 更新文件所属用户和用户组
                                    execStdout(session, "chown " + user + ":" + group + " " + filePath);
                                }
                            }
                        } catch (Exception ignored) {
                        } finally {
                            // 关闭资源
                            if (session != null) try {
                                session.close();
                            } catch (Exception ignored) {
                            }
                            pool.release(ssh);
                        }
                        TreePath selectedPath = lastRight.getDirectoryTree().getServerDirectoryTree().getSelectionPath();
                        lastRight.refreshDirectoryList(selectedPath);

                        fileTransfers.remove(uuid);
                        refreshFileTransfersPanel(jPanel, fileTransfers, 400, 5);
                        if (fileTransfers.size() <= 0) {
                            lastRight.getServerInfoTabbedPane().remove(3);
                        }
                        lastRight.getServerInfoTabbedPane().setSelectedIndex(0);
                        NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                .createNotification(DevServerBundle.INSTANCE.message("server.host.upload.file", ""),
                                        "文件上传完成！", NotificationType.INFORMATION)
                                .notify(project);
                    }

                    @Override
                    public void stopTransfer() {
                        NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                .createNotification(DevServerBundle.INSTANCE.message("server.host.upload.file", ""),
                                        "文件上传终止！", NotificationType.WARNING)
                                .notify(project);
                        fileTransfers.remove(uuid);
                        refreshFileTransfersPanel(jPanel, fileTransfers, 400, 5);
                        if (fileTransfers.size() <= 0) {
                            lastRight.getServerInfoTabbedPane().remove(3);
                        }
                        lastRight.getServerInfoTabbedPane().setSelectedIndex(0);
                    }
                };
                UploadFileActionDialog uploadFileAction = new UploadFileActionDialog(project, hostModel, fileTransferModel, fileTransferCallback);
                uploadFileAction.showAndGet();
                if (uploadFileAction.isStart()) {
                    if (lastRight.hasTabByTitle("传输")) {
                        jPanel = new JPanel();
                        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
                        JBScrollPane jbScrollPane = new JBScrollPane();
                        jbScrollPane.setViewportView(jPanel);
                        lastRight.getServerInfoTabbedPane().add("传输", jbScrollPane);
                    }
                    FileTransfer fileTransfer = new FileTransfer(project, hostModel, fileTransferCallback, fileTransferModel);
                    fileTransfers.put(uuid, fileTransfer);
                    lastRight.getServerInfoTabbedPane().setSelectedIndex(3);
                    refreshFileTransfersPanel(jPanel, fileTransfers, 400, 5);
                }
            }
        };
        AnAction downloadAction = new AnAction("下载文件", "", DevServerIcons.DevServer_DOWNLOAD) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Optional<ServerHostModel> hostFirst = findSelectedHostModel();
                if (!hostFirst.isPresent()) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.download.file", ""),
                                    "请连接正确的服务器", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                ServerHostModel hostModel = hostFirst.get();
                // 获取路径
                Object[] path = lastRight.getDirectoryTree().getServerDirectoryTree().getSelectionModel().getLeadSelectionPath().getPath();
                if (path.length <= 1) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.download.file", ""),
                                    "请连接正确的文件目录", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                String filePath = getFilePath(path);
                String filePreviousPath = getPreviousFilePath(path);
                String fileNameTemp = String.valueOf(path[path.length - 1]) + ".tar";
                int selectedRow = lastRight.getDirectoryList().getDirectoryList().getSelectedIndex();
                String remoteFileName = UUID.randomUUID().toString().replace("-", "") + ".tar";
                String remoteFilePath;
                if (selectedRow < 0) {
                    remoteFilePath = "/root";
                    // 下载目录，打包压缩
                    SshConnectionPool pool = GlobalSshPoolManager.getPool(
                            hostModel.getHost(),
                            hostModel.getPort(),
                            hostModel.getUserName(),
                            hostModel.getPassword()
                    );
                    SSHClient ssh = null;
                    try {
                        // 连接服务器
                        ssh = pool.borrow();
                        try (Session session = ssh.startSession()) {
                            // 打包压缩
                            execStdout(session, "tar czvf /root/" + remoteFileName + " -C " + filePreviousPath + " " + String.valueOf(path[path.length - 1]));
                        }
                    } catch (Exception ignored) {
                    } finally {
                        pool.release(ssh);
                    }
                } else {
                    // 下载文件
                    ServerHostFileModel elementAt = lastRight.getDirectoryList().getDirectoryList().getModel().getElementAt(selectedRow);
                    if (elementAt != null) {
                        remoteFileName = elementAt.getFileName();
                        fileNameTemp = remoteFileName;
                        remoteFilePath = filePath;
                    } else {
                        remoteFilePath = "/root";
                    }
                }
                String finalRemoteFileName = remoteFileName;
                String finalFileNameTemp = fileNameTemp;
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    // 在后台线程执行文件查找操作
                    // 创建文件选择描述器，仅允许选择文件夹
                    FileChooserDescriptor folderChooserDescriptor = new FileChooserDescriptor(
                            false, // 允许选择文件
                            true,  // 允许选择文件夹
                            false,
                            false,
                            false,
                            false
                    );
                    // 获取当前编辑器打开的文件信息
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                    // 获取当前打开的第一个编辑器
                    FileEditor editor = fileEditorManager.getSelectedEditor();
                    VirtualFile fileByPath;
                    if (editor != null) {
                        fileByPath = editor.getFile();
                    } else {
                        fileByPath = project.getProjectFile();
                    }
                    ApplicationManager.getApplication().invokeAndWait(() -> {
                        VirtualFile virtualFile = FileChooser.chooseFile(folderChooserDescriptor, project, fileByPath);
                        if (virtualFile != null) {
                            String localFilesPath = virtualFile.getPath();
                            if (localFilesPath.contains("!/")) {
                                localFilesPath = localFilesPath.substring(0, localFilesPath.indexOf("!/"));
                            }
                            FileTransferModel fileTransferModel = new FileTransferModel();
                            fileTransferModel.setRemoteFilesPath(remoteFilePath);
                            fileTransferModel.setRemoteFilesName(finalRemoteFileName);
                            fileTransferModel.setLocalFilesName(finalFileNameTemp);
                            fileTransferModel.setLocalFilesPath(localFilesPath);
                            fileTransferModel.setOffset(0L);
                            fileTransferModel.setState(Boolean.FALSE);
                            String uuid = UUID.randomUUID().toString().replace("-", "");

                            String finalLocalFilesPath = localFilesPath;
                            FileTransferCallback fileTransferCallback = new FileTransferCallback() {
                                @Override
                                public void callback() {
                                    // 打开文件
                                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                            .createNotification(DevServerBundle.INSTANCE.message("server.host.download.file", ""),
                                                    "文件下载完成！", NotificationType.INFORMATION)
                                            .notify(project);

                                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                                        fileTransfers.remove(uuid);
                                        refreshFileTransfersPanel(jPanel, fileTransfers, 400, 5);
                                        if (fileTransfers.size() <= 0) {
                                            lastRight.getServerInfoTabbedPane().remove(3);
                                        }
                                        if (StringUtils.isNotBlank(finalRemoteFileName) && selectedRow < 0) {
                                            // 删除临时文件
                                            SshConnectionPool pool = GlobalSshPoolManager.getPool(
                                                    hostModel.getHost(),
                                                    hostModel.getPort(),
                                                    hostModel.getUserName(),
                                                    hostModel.getPassword()
                                            );
                                            SSHClient ssh = null;
                                            try {
                                                // 连接服务器
                                                ssh = pool.borrow();
                                                try (Session session = ssh.startSession()) {
                                                    // 删除临时文件
                                                    execStdout(session, "rm -rf /root/" + finalRemoteFileName);
                                                }
                                            } catch (Exception ignored) {
                                            } finally {
                                                pool.release(ssh);
                                            }
                                        }
                                        File file = new File(finalLocalFilesPath + File.separator + finalFileNameTemp);
                                        if (!file.exists()) {
                                            // System.out.println("路径不存在：" + path);
                                            return;
                                        }
                                        try {
                                            ProcessBuilder processBuilder = new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath());
                                            processBuilder.start();
                                            processBuilder.directory();
                                        } catch (IOException ignored) {
                                        }
                                    });

                                }

                                @Override
                                public void stopTransfer() {
                                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                            .createNotification(DevServerBundle.INSTANCE.message("server.host.download.file", ""),
                                                    "文件下载终止！", NotificationType.WARNING)
                                            .notify(project);
                                    fileTransfers.remove(uuid);
                                    refreshFileTransfersPanel(jPanel, fileTransfers, 400, 5);
                                    if (fileTransfers.size() <= 0) {
                                        lastRight.getServerInfoTabbedPane().remove(3);
                                    }
                                }
                            };
                            if (lastRight.hasTabByTitle("传输")) {
                                jPanel = new JPanel();
                                jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
                                JBScrollPane jbScrollPane = new JBScrollPane();
                                jbScrollPane.setViewportView(jPanel);
                                lastRight.getServerInfoTabbedPane().add("传输", jbScrollPane);
                            }
                            FileTransfer fileTransfer = new FileTransfer(project, hostModel, fileTransferCallback, fileTransferModel);
                            fileTransfers.put(uuid, fileTransfer);
                            lastRight.getServerInfoTabbedPane().setSelectedIndex(3);
                            refreshFileTransfersPanel(jPanel, fileTransfers, 400, 5);
                        }
                    });
                });
            }
        };
        AnAction renameAction = new AnAction("重命名文件", "", DevServerIcons.DevServer_TOOLBAR_RENAME) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Optional<ServerHostModel> hostFirst = findSelectedHostModel();
                if (!hostFirst.isPresent()) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                    "请连接正确的服务器", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                ServerHostModel hostModel = hostFirst.get();
                // 获取路径
                Object[] path = lastRight.getDirectoryTree().getServerDirectoryTree().getSelectionModel().getLeadSelectionPath().getPath();
                if (path.length <= 1) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.open.file", ""),
                                    "请连接正确的文件目录", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                String filePath = getFilePath(path);
                int selectedRow = lastRight.getDirectoryList().getDirectoryList().getSelectedIndex();
                if (selectedRow < 0) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.rename.file", ""),
                                    DevServerBundle.INSTANCE.message("server.host.rename.file.choose", ""), NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                ServerHostFileModel elementAt = lastRight.getDirectoryList().getDirectoryList().getModel().getElementAt(selectedRow);
                String fileName = elementAt.getFileName();
                String remoteFilePath = filePath + "/" + fileName;
                String input = Messages.showInputDialog(project, DevServerBundle.INSTANCE.message("server.host.rename.input.file.name", ""),
                        DevServerBundle.INSTANCE.message("server.host.rename.file", ""), Messages.getQuestionIcon(), fileName, null);
                if (StringUtils.isNotBlank(input) && !input.equals(fileName)) {
                    SshConnectionPool pool = GlobalSshPoolManager.getPool(
                            hostModel.getHost(),
                            hostModel.getPort(),
                            hostModel.getUserName(),
                            hostModel.getPassword()
                    );
                    SSHClient ssh = null;
                    try {
                        // 连接服务器
                        ssh = pool.borrow();
                        try (Session session = ssh.startSession()) {
                            execStdout(session, "mv " + remoteFilePath + " " + filePath + "/" + input);
                        }
                        TreePath selectedPath = lastRight.getDirectoryTree().getServerDirectoryTree().getSelectionPath();
                        lastRight.refreshDirectoryList(selectedPath);
                    } catch (Exception ignored) {
                    } finally {
                        pool.release(ssh);
                    }
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.rename.file", ""),
                                    DevServerBundle.INSTANCE.message("文件重命名完成！", ""), NotificationType.INFORMATION)
                            .notify(project);
                }
            }
        };

        AnAction correctAction = new AnAction("修改文件", "", DevServerIcons.DevServer_TOOLBAR_UPDATE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        };

        AnAction deleteAction = new AnAction("删除文件", "", DevServerIcons.DevServer_CLEAR) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Optional<ServerHostModel> hostFirst = findSelectedHostModel();
                if (!hostFirst.isPresent()) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.delete.file", ""),
                                    "请连接正确的服务器", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                ServerHostModel hostModel = hostFirst.get();
                // 获取路径
                Object[] path = lastRight.getDirectoryTree().getServerDirectoryTree().getSelectionModel().getLeadSelectionPath().getPath();
                if (path.length <= 1) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.delete.file", ""),
                                    "请连接正确的文件目录", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                String filePath = getFilePath(path);
                int selectedRow = lastRight.getDirectoryList().getDirectoryList().getSelectedIndex();
                if (selectedRow < 0) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification(DevServerBundle.INSTANCE.message("server.host.delete.file", ""),
                                    DevServerBundle.INSTANCE.message("server.host.delete.file.choose", ""), NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                ServerHostFileModel elementAt = lastRight.getDirectoryList().getDirectoryList().getModel().getElementAt(selectedRow);
                String fileName = elementAt.getFileName();
                String remoteFilePath = filePath + "/" + fileName;
                SshConnectionPool pool = GlobalSshPoolManager.getPool(
                        hostModel.getHost(),
                        hostModel.getPort(),
                        hostModel.getUserName(),
                        hostModel.getPassword()
                );
                SSHClient ssh = null;
                try {
                    // 连接到 SFTP 服务器
                    ssh = pool.borrow();
                    // 删除远程文件
                    if (StringUtils.isNotBlank(remoteFilePath) && !"/".equals(remoteFilePath.trim())) {
                        int result = Messages.showYesNoDialog(
                                DevServerBundle.INSTANCE.message("server.host.delete.file.warn", remoteFilePath),
                                DevServerBundle.INSTANCE.message("server.host.delete.file", ""),
                                DevServerBundle.INSTANCE.message("define", ""),
                                DevServerBundle.INSTANCE.message("cancel", ""),
                                Messages.getQuestionIcon()
                        );
                        if (result == Messages.YES) {
                            if (fileExists(ssh, remoteFilePath)) {
                                try (SFTPClient sftpClient = ssh.newSFTPClient()) {
                                    sftpClient.rm(remoteFilePath);
                                }
                            }
                            NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                    .createNotification(DevServerBundle.INSTANCE.message("server.host.delete.file", ""),
                                            "文件删除完成！", NotificationType.INFORMATION)
                                    .notify(project);
                        }
                    }
                    TreePath selectedPath = lastRight.getDirectoryTree().getServerDirectoryTree().getSelectionPath();
                    lastRight.refreshDirectoryList(selectedPath);
                } catch (Exception ignored) {
                } finally {
                    pool.release(ssh);
                }
            }
        };
        consoleActions.add(openAction);
        consoleActions.add(uploadAction);
        consoleActions.add(downloadAction);
        consoleActions.add(renameAction);
        // consoleActions.add(correctAction);
        consoleActions.add(deleteAction);
        ActionToolbar actionToolBar = createActionToolBar(ActionPlaces.TOOLBAR, true, consoleActions);
        this.lastRight.getDirectoryList().getServerFileOperationToolbar().add(actionToolBar.getComponent());
        this.lastRight.getDirectoryList().getServerFileOperationToolbar().setVisible(true);
    }

    /**
     * 刷新 FileTransfer 列表面板
     *
     * @param panel  承载 FileTransfer 的 JPanel，布局必须是 BoxLayout Y_AXIS
     * @param fileTransfers 按插入顺序的 FileTransfer Map
     * @param rowHeight 每行 FileTransfer 组件的高度
     * @param spacing 组件上下间距
     */
    public static void refreshFileTransfersPanel(JPanel panel,
                                                 Map<String, FileTransfer> fileTransfers,
                                                 int rowHeight,
                                                 int spacing) {
        if (panel == null) return;
        panel.removeAll();
        fileTransfers.forEach((key, fileTransfer) -> {
            JComponent comp = fileTransfer.getRoot();
            // 固定高度
            Dimension size = new Dimension(Integer.MAX_VALUE, rowHeight);
            comp.setMinimumSize(new Dimension(0, rowHeight));
            comp.setPreferredSize(new Dimension(0, rowHeight));
            comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
            // 添加组件
            panel.add(comp);
            // 上下间距
            panel.add(Box.createRigidArea(new Dimension(0, spacing)));
            // 分割线
            JSeparator separator = new JSeparator();
            separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            separator.setForeground(JBColor.GRAY); // IntelliJ 风格
            panel.add(separator);

            // 分割线下间距
            panel.add(Box.createRigidArea(new Dimension(0, spacing)));
        });

        panel.revalidate();
        panel.repaint();
    }

    private void initCommandToolbar() {
        RemoteServer command = this;
        // 添加工具栏
        List<AnAction> consoleActions = new ArrayList<>();
        consoleActions.add(new AnAction("新增", "", DevServerIcons.DevServer_TOOLBAR_ADD) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                EditCommandDialog editCommand = new EditCommandDialog(project, command, OperateEnum.ADD);
                editCommand.showAndGet();
            }
        });
        consoleActions.add(new AnAction("修改", "", DevServerIcons.DevServer_TOOLBAR_UPDATE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                EditCommandDialog editCommand = new EditCommandDialog(project, command, OperateEnum.UPDATE);
                editCommand.showAndGet();
            }
        });
        consoleActions.add(new AnAction("删除", "", DevServerIcons.DevServer_TOOLBAR_DELETE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                EditCommandDialog editCommand = new EditCommandDialog(project, command, OperateEnum.DELETE);
                editCommand.showAndGet();
            }
        });
        consoleActions.add(new AnAction("保存", "", DevServerIcons.DevServer_TOOLBAR_SAVE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 获取脚本类型
                ExecuteRightComponent executeRight = lastRight.getExecuteRight();
                boolean selected = executeRight.getScriptType().isSelected();
                String text = executeRight.getIllustrateTextPane().getText();
                String executeCommandText = executeRight.getExecuteCommand().getText();
                String hostName = getSelectedNodeName(command.getLastLeft().getServerTree());
                Optional<ServerHostModel> hostModelOpt = hostModels.stream()
                        .filter(v -> hostName.equals(v.getHost()))
                        .findFirst();
                if (hostModelOpt.isEmpty()) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification("服务器", "没有要保存的命令", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                ServerHostModel serverHostModel = hostModelOpt.get();
                String commandJsonStr = serverHostModel.getCommand();
                JSONArray objects = StringUtils.isNotBlank(commandJsonStr) ? JSON.parseArray(commandJsonStr) : new JSONArray();
                JSONObject selectedCommandJson = command.getSelectedCommandJson(command.getLastLeft().getServerTree(), command.getLastRight().getExecuteLeft().getCommandTree());
                if (selectedCommandJson == null) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification("服务器", "没有要保存的命令", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                String title = selectedCommandJson.getString("title");
                objects.stream()
                        .filter(v -> {
                            JSONObject jsonObject = JSON.parseObject(v.toString());
                            return jsonObject.getString("title").equals(title);
                        }).findFirst()
                        .ifPresent(v -> {
                            JSONObject json = (JSONObject) v;
                            json.put("scriptType", selected);
                            json.put("illustrate", text);
                            json.put("executeCommand", executeCommandText);
                        });
                serverHostModel.setCommand(objects.toJSONString());
                saveServerHost(hostModels);
                NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                        .createNotification("服务器详情", "服务器详情保存成功", NotificationType.INFORMATION)
                        .notify(project);
            }
        });
        consoleActions.add(new AnAction("执行命令", "", DevServerIcons.DevServer_TOOLBAR_EXECUTE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ExecuteRightComponent executeRight = lastRight.getExecuteRight();
                boolean selected = executeRight.getScriptType().isSelected();
                String hostName = getSelectedNodeName(command.getLastLeft().getServerTree());
                Optional<ServerHostModel> hostModelOpt = hostModels.stream()
                        .filter(v -> hostName.equals(v.getHost()))
                        .findFirst();
                if (hostModelOpt.isEmpty()) {
                    NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                            .createNotification("服务器", "没有要执行命令的服务器", NotificationType.WARNING)
                            .notify(project);
                    return;
                }
                ServerHostModel hostModel = hostModelOpt.get();
                String command = executeRight.getExecuteCommand().getText();
                String directory = executeRight.getDirectoryTextField().getText();
                String user = executeRight.getUserTextField().getText();
                List<String> executeCommand = new ArrayList<>();
                if (selected) {
                    // 选中  脚本
                    SshConnectionPool pool = GlobalSshPoolManager.getPool(
                            hostModel.getHost(),
                            hostModel.getPort(),
                            hostModel.getUserName(),
                            hostModel.getPassword()
                    );
                    SSHClient sshClient = null;
                    try {
                        command = "script_path=\"$(realpath \"$0\")\"\n" +
                                "rm -rf $script_path \n" + command;
                        sshClient = pool.borrow();
                        String uuid = UUID.randomUUID().toString().replace("-", "");
                        File tempFile = com.intellij.openapi.util.io.FileUtil.createTempFile(uuid, ".sh", true);
                        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
                            com.intellij.openapi.util.io.FileUtil.writeToFile(tempFile, command);
                            sftpClient.put(tempFile.getAbsolutePath(), directory + "/" + uuid + ".sh");
                        }
                        executeCommand.add("cd " + directory);
                        executeCommand.add("chown " + user + ":" + user + " " + directory + "/" + uuid + ".sh");
                        executeCommand.add("su - " + user);
                        executeCommand.add("cd " + directory);
                        executeCommand.add("chmod +x " + uuid + ".sh");
                        executeCommand.add("./" + uuid + ".sh");
                        if (tempFile.exists()) {
                            tempFile.delete();
                        }
                    } catch (Exception ignored) {
                    } finally {
                        pool.release(sshClient);
                    }
                } else {
                    // 未选中  命令
                    executeCommand.add("su - " + user);
                    executeCommand.add("cd " + directory);
                    executeCommand.add(command);
                }
                JComponent remoteNewTerminalComponent = createRemoteNewTerminalComponent(project, hostModel, executeCommand);
                addNewTerminalTab(hostModel.getHost(), remoteNewTerminalComponent);
                nextComponent.setVisible(true);
                nextComponent.revalidate();  // 重新触发布局
                nextComponent.repaint();     // 重新绘制
            }
        });
        Tree commandTree = lastRight.getExecuteLeft().getCommandTree();
        consoleActions.add(new AnAction("展开", "", DevServerIcons.DevServer_EXPAND_DARK) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreePath sel = commandTree.getSelectionPath();
                boolean selectedFileNode = sel != null && sel.getLastPathComponent() instanceof ServerHostTreeNode;
                if (!selectedFileNode) {
                    expandAll(commandTree);
                } else {
                    if (sel.getPathCount() == 1) {
                        // 选中根节点
                        expandAll(commandTree);
                    } else {
                        expandNode(commandTree, sel);
                    }
                }
            }
        });
        consoleActions.add(new AnAction("折叠", "", DevServerIcons.DevServer_COLLAPSE_DARK) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreePath sel = commandTree.getSelectionPath();
                boolean selectedFileNode = sel != null && sel.getLastPathComponent() instanceof ServerHostTreeNode;
                if (!selectedFileNode) {
                    collapseAllExceptRoot(commandTree);
                } else {
                    if (sel.getPathCount() == 1) {
                        // 选中根节点
                        collapseAllExceptRoot(commandTree);
                    } else {
                        collapseDeep(commandTree, sel);
                    }
                }
            }
        });
        ActionToolbar actionToolBar = createActionToolBar(ActionPlaces.TOOLBAR, true, consoleActions);
        JPanel commandToolbar = lastRight.getExecuteLeft().getCommandToolbar();
        commandToolbar.add(actionToolBar.getComponent());
        commandToolbar.setVisible(true);
    }

    /**
     * 选中的执行命令
     *
     */
    public JSONObject getSelectedCommandJson(JTree serverHostTree,
                                             JTree commandTree) {

        if (hostModels == null || hostModels.isEmpty()) {
            return new JSONObject();
        }
        // 获取选中的 hostName
        String hostName = getSelectedNodeName(serverHostTree);

        if (StringUtils.isBlank(hostName)) {
            return new JSONObject();
        }
        // 根据 hostName 找到对应的 ServerHostModel
        Optional<ServerHostModel> hostModelOpt = hostModels.stream()
                .filter(v -> hostName.equals(v.getHost()))
                .findFirst();
        if (!hostModelOpt.isPresent()) {
            return null;
        }
        ServerHostModel hostModel = hostModelOpt.get();
        // 获取选中的 commandName
        String commandName = getSelectedNodeName(commandTree);
        if (StringUtils.isBlank(commandName)) {
            return new JSONObject();
        }
        String command = hostModel.getCommand();
        if (StringUtils.isBlank(command)) {
            return new JSONObject();
        }
        JSONArray objects = JSON.parseArray(command);
        return objects.stream()
                .map(m -> JSON.parseObject(m.toString()))
                .filter(jsonObject -> commandName.equals(jsonObject.getString("title")))
                .findFirst()
                .orElse(new JSONObject());
    }

    public String getSelectedNodeName(JTree tree) {
        if (tree == null) {
            return "";
        }
        TreePath selectedPath = tree.getSelectionPath();
        if (selectedPath == null) {
            return "";
        }
        Object last = selectedPath.getLastPathComponent();
        if (last instanceof ServerHostTreeNode) {
            return ((ServerHostTreeNode) last).getName();
        }
        return "";
    }


    @Override
    public void dispose() {
        Disposer.dispose(this, false);
    }
}
