package com.tanghui.dev.idea.plugin.devserver.deploy.cluster;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.deploy.dialog.ServerAuthenticationDialog;
import com.tanghui.dev.idea.plugin.devserver.deploy.run.DevServerRunConfiguration;
import com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons;
import com.tanghui.dev.idea.plugin.devserver.listener.DevServerDocumentListener;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import com.tanghui.dev.idea.plugin.devserver.ui.DevServerTable;
import com.tanghui.dev.idea.plugin.devserver.ui.model.DevServerTableModel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.ButtonGroup;
import javax.swing.event.DocumentEvent;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.deploy.cluster
 * @Author: 唐煇
 * @CreateTime: 2025-12-12-13:37
 * @Description: 集群部署。
 * @Version: v1.0
 */
@Setter
@Getter
public class ClusterDeployment {
    private JBPanel<?> root;
    private JBTabbedPane scriptTabbedPane;
    private JBRadioButton upgradeRadioButton;
    private JBRadioButton uploadRadioButton;
    private JBRadioButton executeRadioButton;
    private JBPanel<?> filePanel;
    private JBPanel<?> serverPanel;

    private JBPanel<?> scriptPanel;
    private JBPanel<?> uploadFilePanel;

    private JBPanel<?> panel12;

    private JBPanel<?> workDirectory;
    private JBPanel<?> rollbackPanel;
    private JBPanel<?> logePanel;

    private ExtendableTextField textField;
    private final Project project;
    private final DevServerTable devServerTable;

    /**
     * 服务器配置
     */
    private List<DevServerRunConfig> runConfigList = new ArrayList<>();

    private DevServerRunConfiguration runConfigName;

    public ClusterDeployment(Project project, DevServerRunConfiguration runConfigName) {
        this.project = project;
        this.devServerTable = new DevServerTable(project);
        // this.runConfigList = runConfigName.getRunConfigList();
        this.runConfigName = runConfigName;
        createUIComponents();
        initComponent();
        initServerTable();
        initScriptTabbedPane();
        ApplicationManager.getApplication().invokeLater(() -> {
            if (scriptTabbedPane.getTabCount() > 0 && scriptTabbedPane.getUI() != null) {
                Rectangle r = scriptTabbedPane.getUI()
                        .getTabBounds(scriptTabbedPane, 0);
                int tabHeaderHeight = r.height;
                panel12.setPreferredSize(JBUI.size(24, tabHeaderHeight));
            }
        });
        actionListener();
    }

    public void actionListener() {
        // 升级
        upgradeRadioButton.addActionListener(e -> {
            uploadFilePanel.setVisible(true);
            rollbackPanel.setVisible(true);
            logePanel.setVisible(true);
            workDirectory.setVisible(true);

            runConfigName.setUpgradeType("upgrade");

            if ((runConfigList != null && !runConfigList.isEmpty()) && (scriptTabbedPane != null)) {
                for (int i = 0; i < runConfigList.size(); i++) {
                    DevServerRunConfig runConfig = runConfigList.get(i);
                    String serverHost = runConfig.getServerHost();
                    ExecuteScript executeScript = new ExecuteScript(project, runConfigList, runConfig);
                    executeScript.getRollbackPanel().setVisible(true);
                    executeScript.getLogePanel().setVisible(true);
                    executeScript.getWorkDirectory().setVisible(true);
                    scriptTabbedPane.setTitleAt(i, serverHost);
                    scriptTabbedPane.setComponentAt(i, executeScript.getRoot());
                }
                scriptTabbedPane.updateUI();
            }

        });
        // 上传文件
        uploadRadioButton.addActionListener(e -> {
            runConfigName.setUpgradeType("upload");
            uploadFilePanel.setVisible(true);
            rollbackPanel.setVisible(false);
            logePanel.setVisible(false);
            workDirectory.setVisible(true);
            if ((runConfigList != null && !runConfigList.isEmpty()) && (scriptTabbedPane != null)) {
                for (int i = 0; i < runConfigList.size(); i++) {
                    DevServerRunConfig runConfig = runConfigList.get(i);
                    String serverHost = runConfig.getServerHost();
                    ExecuteScript executeScript = new ExecuteScript(project, runConfigList, runConfig);
                    executeScript.getRollbackPanel().setVisible(false);
                    executeScript.getLogePanel().setVisible(false);
                    executeScript.getWorkDirectory().setVisible(true);
                    scriptTabbedPane.setTitleAt(i, serverHost);
                    scriptTabbedPane.setComponentAt(i, executeScript.getRoot());
                }
                scriptTabbedPane.updateUI();
            }
        });
        // 执行命令
        executeRadioButton.addActionListener(e -> {
            runConfigName.setUpgradeType("execute");
            uploadFilePanel.setVisible(false);
            rollbackPanel.setVisible(false);
            logePanel.setVisible(false);
            workDirectory.setVisible(false);
            if ((runConfigList != null && !runConfigList.isEmpty()) && (scriptTabbedPane != null)) {
                for (int i = 0; i < runConfigList.size(); i++) {
                    DevServerRunConfig runConfig = runConfigList.get(i);
                    String serverHost = runConfig.getServerHost();
                    ExecuteScript executeScript = new ExecuteScript(project, runConfigList, runConfig);
                    executeScript.getRollbackPanel().setVisible(false);
                    executeScript.getLogePanel().setVisible(false);
                    executeScript.getWorkDirectory().setVisible(false);
                    scriptTabbedPane.setTitleAt(i, serverHost);
                    scriptTabbedPane.setComponentAt(i, executeScript.getRoot());
                }
                scriptTabbedPane.updateUI();
            }
        });

        textField.getDocument().addDocumentListener(new DevServerDocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                Document doc = e.getDocument();
                try {
                    String text = doc.getText(0, doc.getLength());
                    if (StringUtils.isNotBlank(text)) {
                        runConfigName.setUploadFile(text);
                    }
                } catch (BadLocationException ignored) {
                }
            }
        });

    }

    public void initComponent() {
        // 创建按钮组
        ButtonGroup group = new ButtonGroup();
        // 添加到同一组 → 自动单选
        group.add(upgradeRadioButton);
        group.add(uploadRadioButton);
        group.add(executeRadioButton);
        // 默认选中
        switch (this.runConfigName.getUpgradeType()) {
            case "upload" -> uploadRadioButton.setSelected(true);
            case "execute" -> executeRadioButton.setSelected(true);
            case null, default -> upgradeRadioButton.setSelected(true);
        }
        if (textField != null && this.filePanel != null) {
            this.filePanel.add(textField, BorderLayout.CENTER);
        }

        if (StringUtils.isNotBlank(this.runConfigName.getUploadFile())) {
            textField.setText(this.runConfigName.getUploadFile());
        }
        this.scriptPanel.setVisible(false);
    }

    // 初始化表格
    private void initServerTable() {
        String[] columns = {"服务器IP", "服务器端口", "连接用户", "操作用户"};
        DevServerTableModel model = new DevServerTableModel(columns, 0);
        this.devServerTable.getDevServerTableComponent().setModel(model);
        this.serverPanel.add(this.devServerTable.getRoot(), BorderLayout.CENTER);
        initParamsActions();
    }

    public void initParamsActions() {
        // 添加工具栏
        List<AnAction> consoleActions = new ArrayList<>();
        // 创建自定义的 Action
        AnAction addAction = new AnAction(DevServerBundle.INSTANCE.message("sql.field.type.mapper.add"),
                DevServerBundle.INSTANCE.message("sql.field.type.mapper.add.text"), DevServerIcons.DevServer_TOOLBAR_ADD) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TableModel model = devServerTable.getDevServerTableComponent().getModel();
                int selectedRow = devServerTable.getDevServerTableComponent().getSelectedRow();
                if (model instanceof DevServerTableModel pojoTableModel) {
                    int rowCount = pojoTableModel.getRowCount();
                    if (selectedRow >= 0) {
                        rowCount = selectedRow + 1;
                    }
                    /*
                    pojoTableModel.setEditable(true);*/
                    ServerAuthenticationDialog serverAuthentication = new ServerAuthenticationDialog(project, new DevServerRunConfig());
                    serverAuthentication.showAndGet();
                    if (serverAuthentication.getServerAuthentication().isSuccessNot()) {
                        DevServerRunConfig parentComponent = serverAuthentication.getParentComponent();
                        String serverHost = parentComponent.getServerHost();
                        Optional<DevServerRunConfig> first = runConfigList.stream().filter(v -> serverHost.equals(v.getServerHost()))
                                .findFirst();
                        if (!first.isPresent()) {
                            String serverPort = parentComponent.getServerPort();
                            String serverUser = parentComponent.getServerUser();
                            String controlsUser = parentComponent.getControlsUser();
                            pojoTableModel.insertRow(rowCount, new Object[]{serverHost, serverPort, serverUser, controlsUser});
                            runConfigList.add(rowCount, parentComponent);
                            addScriptTabbedPane(runConfigList, parentComponent);
                            scriptPanel.setVisible(true);
                        }
                    }
                }
            }
        };
        AnAction updateAction = new AnAction(DevServerBundle.INSTANCE.message("sql.field.type.mapper.update"),
                DevServerBundle.INSTANCE.message("sql.field.type.mapper.update.text"), DevServerIcons.DevServer_TOOLBAR_UPDATE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TableModel model = devServerTable.getDevServerTableComponent().getModel();
                if (model instanceof DevServerTableModel pojoTableModel) {
                    // pojoTableModel.setEditable(true);
                    int rowCount = pojoTableModel.getRowCount();
                    // 单选
                    int viewRow = devServerTable.getDevServerTableComponent().getSelectedRow();
                    if (rowCount >= 1 && viewRow >= 0) {
                        DevServerRunConfig devServerRunConfig = runConfigList.get(viewRow);
                        ServerAuthenticationDialog serverAuthentication = new ServerAuthenticationDialog(project, devServerRunConfig);
                        serverAuthentication.showAndGet();
                        if (serverAuthentication.getServerAuthentication().isSuccessNot()) {
                            DevServerRunConfig parentComponent = serverAuthentication.getParentComponent();
                            pojoTableModel.removeRow(viewRow);
                            String serverHost = parentComponent.getServerHost();
                            String serverPort = parentComponent.getServerPort();
                            String serverUser = parentComponent.getServerUser();
                            String controlsUser = parentComponent.getControlsUser();
                            devServerRunConfig.setServerHost(serverHost);
                            devServerRunConfig.setServerPort(serverPort);
                            devServerRunConfig.setServerUser(serverUser);
                            devServerRunConfig.setControlsUser(controlsUser);
                            pojoTableModel.insertRow(viewRow, new Object[]{serverHost, serverPort, serverUser, controlsUser});
                            scriptPanel.setVisible(true);
                            scriptTabbedPane.setTitleAt(viewRow, serverHost);
                        }
                    }
                }
            }
        };
        AnAction deleteAction = new AnAction(DevServerBundle.INSTANCE.message("sql.field.type.mapper.delete"),
                DevServerBundle.INSTANCE.message("sql.field.type.mapper.delete.text"), DevServerIcons.DevServer_TOOLBAR_DELETE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                int result = Messages.showYesNoDialog(
                        "是否删除服务器配置",
                        "服务器配置",
                        DevServerBundle.INSTANCE.message("define", ""),
                        DevServerBundle.INSTANCE.message("cancel", ""),
                        Messages.getWarningIcon()
                );
                if (result == Messages.YES) {
                    TableModel model = devServerTable.getDevServerTableComponent().getModel();
                    int selectedRow = devServerTable.getDevServerTableComponent().getSelectedRow();
                    if (model instanceof DevServerTableModel pojoTableModel) {
                        if (selectedRow >= 0) {
                            pojoTableModel.removeRow(selectedRow);
                            scriptTabbedPane.removeTabAt(selectedRow);
                            runConfigList.remove(selectedRow);
                        }
                    }
                }
            }
        };
        consoleActions.add(addAction);
        consoleActions.add(updateAction);
        consoleActions.add(deleteAction);
        this.devServerTable.initToolbar(consoleActions);
    }

    private void initScriptTabbedPane() {
        if (runConfigList == null || runConfigList.isEmpty()) {
            return;
        }
        for (DevServerRunConfig runConfig : runConfigList) {
            String serverHost = runConfig.getServerHost();
            ExecuteScript executeScript = new ExecuteScript(project, runConfigList, runConfig);

            if (upgradeRadioButton.isSelected()) {
                executeScript.getRollbackPanel().setVisible(true);
                executeScript.getLogePanel().setVisible(true);
                executeScript.getWorkDirectory().setVisible(true);
            }

            if (uploadRadioButton.isSelected()) {
                executeScript.getRollbackPanel().setVisible(false);
                executeScript.getLogePanel().setVisible(false);
                executeScript.getWorkDirectory().setVisible(true);
            }

            if (executeRadioButton.isSelected()) {
                executeScript.getRollbackPanel().setVisible(false);
                executeScript.getLogePanel().setVisible(false);
                executeScript.getWorkDirectory().setVisible(false);
            }

            scriptTabbedPane.addTab(serverHost, executeScript.getRoot());
        }
        scriptTabbedPane.updateUI();
    }

    public void addScriptTabbedPane(List<DevServerRunConfig> runConfigList, DevServerRunConfig devServerRunConfig) {

        ExecuteScript executeScript = new ExecuteScript(project, runConfigList, devServerRunConfig);
        if (upgradeRadioButton.isSelected()) {
            executeScript.getRollbackPanel().setVisible(true);
            executeScript.getLogePanel().setVisible(true);
            executeScript.getWorkDirectory().setVisible(true);
        }
        if (uploadRadioButton.isSelected()) {
            executeScript.getRollbackPanel().setVisible(false);
            executeScript.getLogePanel().setVisible(false);
            executeScript.getWorkDirectory().setVisible(true);
        }
        if (executeRadioButton.isSelected()) {
            executeScript.getRollbackPanel().setVisible(false);
            executeScript.getLogePanel().setVisible(false);
            executeScript.getWorkDirectory().setVisible(false);
        }
        scriptTabbedPane.addTab(devServerRunConfig.getServerHost(), executeScript.getRoot());
        scriptTabbedPane.updateUI();
        ApplicationManager.getApplication().invokeLater(() -> {
            if (scriptTabbedPane.getTabCount() > 0 && scriptTabbedPane.getUI() != null) {
                Rectangle r = scriptTabbedPane.getUI()
                        .getTabBounds(scriptTabbedPane, 0);
                int tabHeaderHeight = r.height;
                panel12.setPreferredSize(JBUI.size(24, tabHeaderHeight));
            }
        });
    }

    private void createUIComponents() {
        this.textField = new ExtendableTextField();
        // 选择文件按钮
        ExtendableTextComponent.Extension chooseFileButton = getChooseFileButton(textField);
        textField.addExtension(chooseFileButton);
    }

    public static ExtendableTextComponent.Extension getChooseFileButton(ExtendableTextField textField) {
        return ExtendableTextComponent.Extension.create(
                AllIcons.Nodes.Folder,          // 图标
                AllIcons.Nodes.Folder,          // hover 图标
                "选择文件",
                () -> {
                    FileChooserDescriptor descriptor =
                            new FileChooserDescriptor(true, false, false, false, false, false);
                    VirtualFile file;
                    if (StringUtils.isNotBlank(textField.getText())) {
                        VirtualFile defaultDir =
                                LocalFileSystem.getInstance()
                                        .findFileByPath(textField.getText());
                        file = FileChooser.chooseFile(descriptor, null, defaultDir);
                    } else {
                        file = FileChooser.chooseFile(descriptor, null, null);
                    }
                    if (file != null) {
                        textField.setText(file.getPath());
                    }
                }
        );
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        root = new JBPanel();
        root.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        final JBPanel panel1 = new JBPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        panel1.setMaximumSize(JBUI.size(2147483647, 40));
        panel1.setMinimumSize(JBUI.size(24, 40));
        panel1.setPreferredSize(JBUI.size(24, 40));
        CellConstraints cc = new CellConstraints();
        root.add(panel1, cc.xy(1, 1));
        final JBPanel panel2 = new JBPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setMaximumSize(JBUI.size(100, 2147483647));
        panel2.setMinimumSize(JBUI.size(100, 24));
        panel2.setPreferredSize(JBUI.size(100, 24));
        panel1.add(panel2, BorderLayout.WEST);
        final JBLabel label1 = new JBLabel();
        label1.setText("类型");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel3 = new JBPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, BorderLayout.CENTER);
        final JBPanel panel4 = new JBPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        upgradeRadioButton = new JBRadioButton();
        upgradeRadioButton.setText("升级");
        panel4.add(upgradeRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel5 = new JBPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        uploadRadioButton = new JBRadioButton();
        uploadRadioButton.setText("上传文件");
        panel5.add(uploadRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel6 = new JBPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel6, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        executeRadioButton = new JBRadioButton();
        executeRadioButton.setText("执行命令");
        panel6.add(executeRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        uploadFilePanel = new JBPanel();
        uploadFilePanel.setLayout(new BorderLayout(0, 0));
        uploadFilePanel.setMaximumSize(JBUI.size(2147483647, 40));
        uploadFilePanel.setMinimumSize(JBUI.size(24, 40));
        uploadFilePanel.setPreferredSize(JBUI.size(24, 40));
        root.add(uploadFilePanel, cc.xy(1, 3));
        final JBPanel panel7 = new JBPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.setMaximumSize(JBUI.size(100, 2147483647));
        panel7.setMinimumSize(JBUI.size(100, 24));
        panel7.setPreferredSize(JBUI.size(100, 24));
        uploadFilePanel.add(panel7, BorderLayout.WEST);
        final JBLabel label2 = new JBLabel();
        label2.setText("文件");
        panel7.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        filePanel = new JBPanel();
        filePanel.setLayout(new BorderLayout(0, 0));
        uploadFilePanel.add(filePanel, BorderLayout.CENTER);
        final JBPanel panel8 = new JBPanel();
        panel8.setLayout(new BorderLayout(0, 0));
        panel8.setMaximumSize(JBUI.size(2147483647, 200));
        panel8.setMinimumSize(JBUI.size(24, 200));
        panel8.setPreferredSize(JBUI.size(24, 200));
        root.add(panel8, cc.xy(1, 5));
        final JBPanel panel9 = new JBPanel();
        panel9.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow"));
        panel9.setMaximumSize(JBUI.size(100, 2147483647));
        panel9.setMinimumSize(JBUI.size(100, 24));
        panel9.setPreferredSize(JBUI.size(100, 24));
        panel8.add(panel9, BorderLayout.WEST);
        final JBPanel panel10 = new JBPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel10.setMaximumSize(JBUI.size(2147483647, 40));
        panel10.setMinimumSize(JBUI.size(24, 40));
        panel10.setPreferredSize(JBUI.size(24, 40));
        panel9.add(panel10, cc.xy(1, 1));
        final JBLabel label3 = new JBLabel();
        label3.setText("服务器");
        panel10.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverPanel = new JBPanel();
        serverPanel.setLayout(new BorderLayout(0, 0));
        panel8.add(serverPanel, BorderLayout.CENTER);
        scriptPanel = new JBPanel();
        scriptPanel.setLayout(new BorderLayout(0, 0));
        scriptPanel.setMaximumSize(JBUI.size(2147483647, 530));
        scriptPanel.setMinimumSize(JBUI.size(24, 530));
        scriptPanel.setPreferredSize(JBUI.size(24, 530));
        root.add(scriptPanel, cc.xy(1, 7));
        final JBPanel panel11 = new JBPanel();
        panel11.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel11.setMaximumSize(JBUI.size(100, 2147483647));
        panel11.setMinimumSize(JBUI.size(100, 24));
        panel11.setPreferredSize(JBUI.size(100, 24));
        scriptPanel.add(panel11, BorderLayout.WEST);
        panel12 = new JBPanel();
        panel12.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel12.setMaximumSize(JBUI.size(2147483647, 24));
        panel12.setMinimumSize(JBUI.size(24, 24));
        panel12.setPreferredSize(JBUI.size(24, 24));
        panel11.add(panel12, cc.xy(1, 1));
        workDirectory = new JBPanel();
        workDirectory.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        workDirectory.setMaximumSize(JBUI.size(2147483647, 40));
        workDirectory.setMinimumSize(JBUI.size(24, 40));
        workDirectory.setPreferredSize(JBUI.size(24, 40));
        panel11.add(workDirectory, cc.xy(1, 3));
        final JBLabel label4 = new JBLabel();
        label4.setText("工作目录");
        workDirectory.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel13 = new JBPanel();
        panel13.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel13.setMaximumSize(JBUI.size(2147483647, 200));
        panel13.setMinimumSize(JBUI.size(24, 200));
        panel13.setPreferredSize(JBUI.size(24, 200));
        panel11.add(panel13, cc.xy(1, 5));
        final JBLabel label5 = new JBLabel();
        label5.setText("运行脚本");
        panel13.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rollbackPanel = new JBPanel();
        rollbackPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rollbackPanel.setMaximumSize(JBUI.size(2147483647, 150));
        rollbackPanel.setMinimumSize(JBUI.size(24, 150));
        rollbackPanel.setPreferredSize(JBUI.size(24, 150));
        panel11.add(rollbackPanel, cc.xy(1, 7));
        final JBLabel label6 = new JBLabel();
        label6.setText("回退脚本");
        rollbackPanel.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        logePanel = new JBPanel();
        logePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        logePanel.setMaximumSize(JBUI.size(2147483647, 70));
        logePanel.setMinimumSize(JBUI.size(24, 70));
        logePanel.setPreferredSize(JBUI.size(24, 70));
        panel11.add(logePanel, cc.xy(1, 9));
        final JBLabel label7 = new JBLabel();
        label7.setText("查看日志");
        logePanel.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel14 = new JBPanel();
        panel14.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scriptPanel.add(panel14, BorderLayout.CENTER);
        scriptTabbedPane = new JBTabbedPane();
        panel14.add(scriptTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}
