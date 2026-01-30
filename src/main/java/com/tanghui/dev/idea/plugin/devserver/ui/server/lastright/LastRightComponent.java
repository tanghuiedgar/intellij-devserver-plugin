package com.tanghui.dev.idea.plugin.devserver.ui.server.lastright;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostFileModel;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons;
import com.tanghui.dev.idea.plugin.devserver.tree.ServerHostTreeNode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.tanghui.dev.idea.plugin.devserver.ui.server.RemoteServer.hostModels;
import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.getFilePath;
import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.getServerHostFileModels;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.ui.server
 * @Author: 唐煇
 * @CreateTime: 2026-01-22-16:23
 * @Description: 上右窗口。
 * @Version: v1.0
 */
@Getter
public class LastRightComponent {
    private JBPanel<?> root;
    private JBTabbedPane serverInfoTabbedPane;
    private JBPanel<?> infoPanel;
    private JBPanel<?> detailPanel;
    private JBPanel<?> executePanel;
    private JBPanel<?> connectionOperationToolbar;
    private JBTextField serverHostTextField;
    private JBTextField serverUserTextField;
    private JBTextField serverPassWordTextField;
    private JBTextField serverDirectoryTextField;
    private Splitter directorySplitter;
    private JBPanel<?> serverPanel;
    private JBPanel<?> directoryPanel;
    private Splitter executeSplitter;

    private DirectoryTreeComponent directoryTree;
    private DirectoryListComponent directoryList;
    private ExecuteLeftComponent executeLeft;
    private ExecuteRightComponent executeRight;

    private final Project project;

    private final Tree serverTree;

    public static boolean passwordShow = false;

    public LastRightComponent(Project project, Tree serverTree, Disposable parentDisposable) {
        this.project = project;
        this.serverTree = serverTree;
        $$$setupUI$$$();
        setSelectionListener();
    }

    private void setSelectionListener() {
        this.directoryTree.getServerDirectoryTree().addTreeSelectionListener(e -> {
            new Task.Backgroundable(project, "连接服务器") {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    progressIndicator.setIndeterminate(true);
                    TreePath newLeadSelectionPath = e.getNewLeadSelectionPath();
                    refreshDirectoryList(newLeadSelectionPath);
                }
            }.queue();
        });
    }

    public void refreshDirectoryList(TreePath newLeadSelectionPath) {
        if (newLeadSelectionPath != null && StringUtils.isNotBlank(newLeadSelectionPath.getLastPathComponent().toString())) {
            Object[] path = newLeadSelectionPath.getPath();
            if (path.length > 1) {
                ServerHostTreeNode lastPathComponent = (ServerHostTreeNode) newLeadSelectionPath.getLastPathComponent();
                String filePath = getFilePath(path);
                String name = ((ServerHostTreeNode) path[0]).getName();
                Optional<ServerHostModel> serverHostModel = hostModels.stream()
                        .filter(v -> v.getHost().equals(name))
                        .findFirst();
                if (serverHostModel.isPresent()) {
                    ServerHostModel hostModel = serverHostModel.get();
                    hostModel.setPath(filePath);
                    List<ServerHostFileModel> hostFileModels = getServerHostFileModels(project, hostModel);
                    hostFileModels.stream().filter(ServerHostFileModel::getIsDir)
                            .forEach(v -> {
                                Icon icon = DevServerIcons.DevServer_PACKAGE;
                                ServerHostTreeNode mutableTreeNode = new ServerHostTreeNode(v.getFileName(), icon);
                                lastPathComponent.add(mutableTreeNode);
                            });
                    // 默认展开 / 节点
                    ApplicationManager.getApplication().invokeAndWait(() -> {
                        TreePath treePath = new TreePath(lastPathComponent.getPath());
                        directoryTree.getServerDirectoryTree().expandPath(treePath);
                    });

                    List<ServerHostFileModel> fileModels = hostFileModels.stream()
                            .filter(v -> !v.getIsDir())
                            .toList();
                    DefaultListModel<ServerHostFileModel> model = (DefaultListModel<ServerHostFileModel>) directoryList.getDirectoryList().getModel();
                    model.clear();
                    if (!fileModels.isEmpty()) {
                        model.addAll(fileModels);
                    }
                    ApplicationManager.getApplication().invokeAndWait(() -> serverDirectoryTextField.setText(filePath));
                }
            }
        }
    }

    public boolean hasTabByTitle(String title) {
        for (int i = 0; i < serverInfoTabbedPane.getTabCount(); i++) {
            if (title.equals(serverInfoTabbedPane.getTitleAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.directorySplitter = new Splitter(false, 0.3f);
        this.directoryTree = new DirectoryTreeComponent(project);
        this.directoryList = new DirectoryListComponent(project);
        this.directorySplitter.setFirstComponent(this.directoryTree.getRoot());
        this.directorySplitter.setSecondComponent(this.directoryList.getRoot());
        this.executeSplitter = new Splitter(false, 0.3f);
        this.executeLeft = new ExecuteLeftComponent(project);
        this.executeRight = new ExecuteRightComponent(project);
        this.executeSplitter.setFirstComponent(this.executeLeft.getRoot());
        this.executeSplitter.setSecondComponent(this.executeRight.getRoot());
    }

    /** Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        root = new JBPanel();
        root.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        serverInfoTabbedPane = new JBTabbedPane();
        serverInfoTabbedPane.setTabComponentInsets(new Insets(0, 0, 0, 0));
        serverInfoTabbedPane.setTabLayoutPolicy(0);
        serverInfoTabbedPane.setTabPlacement(4);
        serverInfoTabbedPane.setVerifyInputWhenFocusTarget(true);
        root.add(serverInfoTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        infoPanel = new JBPanel();
        infoPanel.setLayout(new BorderLayout(0, 0));
        serverInfoTabbedPane.addTab("信息", infoPanel);
        connectionOperationToolbar = new JBPanel();
        connectionOperationToolbar.setLayout(new BorderLayout(0, 0));
        connectionOperationToolbar.setMaximumSize(JBUI.size(2147483647, 30));
        connectionOperationToolbar.setMinimumSize(JBUI.size(24, 30));
        connectionOperationToolbar.setPreferredSize(JBUI.size(24, 30));
        infoPanel.add(connectionOperationToolbar, BorderLayout.NORTH);
        final JBPanel panel1 = new JBPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        infoPanel.add(panel1, BorderLayout.CENTER);
        final JBPanel panel2 = new JBPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setMaximumSize(JBUI.size(2147483647, 70));
        panel2.setMinimumSize(JBUI.size(24, 70));
        panel2.setPreferredSize(JBUI.size(24, 70));
        panel1.add(panel2, BorderLayout.NORTH);
        final JBPanel panel3 = new JBPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel4 = new JBPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 20, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBLabel label1 = new JBLabel();
        label1.setText("服务器IP");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel5 = new JBPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        serverHostTextField = new JBTextField();
        serverHostTextField.setEditable(false);
        panel5.add(serverHostTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel6 = new JBPanel();
        panel6.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel7 = new JBPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 20, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBLabel label2 = new JBLabel();
        label2.setText("连接用户");
        panel7.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel8 = new JBPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel8, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        serverUserTextField = new JBTextField();
        serverUserTextField.setEditable(false);
        panel8.add(serverUserTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel9 = new JBPanel();
        panel9.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel9, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel10 = new JBPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 20, 0, 0), -1, -1));
        panel9.add(panel10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBLabel label3 = new JBLabel();
        label3.setText("用户密码");
        panel10.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel11 = new JBPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel11, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        serverPassWordTextField = new JBTextField();
        serverPassWordTextField.setEditable(false);
        panel11.add(serverPassWordTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverPanel = new JBPanel();
        serverPanel.setLayout(new BorderLayout(0, 0));
        panel1.add(serverPanel, BorderLayout.CENTER);
        final JBPanel panel12 = new JBPanel();
        panel12.setLayout(new BorderLayout(0, 0));
        panel12.setMaximumSize(JBUI.size(2147483647, 30));
        panel12.setMinimumSize(JBUI.size(24, 30));
        panel12.setPreferredSize(JBUI.size(24, 30));
        serverPanel.add(panel12, BorderLayout.NORTH);
        serverDirectoryTextField = new JBTextField();
        panel12.add(serverDirectoryTextField, BorderLayout.CENTER);
        directoryPanel = new JBPanel();
        directoryPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        serverPanel.add(directoryPanel, BorderLayout.CENTER);
        directoryPanel.add(directorySplitter, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        detailPanel = new JBPanel();
        detailPanel.setLayout(new BorderLayout(0, 0));
        serverInfoTabbedPane.addTab("详情", detailPanel);
        executePanel = new JBPanel();
        executePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        serverInfoTabbedPane.addTab("执行", executePanel);
        executePanel.add(executeSplitter, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}
