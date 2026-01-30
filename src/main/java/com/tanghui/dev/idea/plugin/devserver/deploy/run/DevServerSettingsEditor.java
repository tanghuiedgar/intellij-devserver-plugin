package com.tanghui.dev.idea.plugin.devserver.deploy.run;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.tanghui.dev.idea.plugin.devserver.deploy.cluster.ClusterDeployment;
import com.tanghui.dev.idea.plugin.devserver.deploy.cluster.ExecuteScript;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import com.tanghui.dev.idea.plugin.devserver.ui.DevServerTable;
import com.tanghui.dev.idea.plugin.devserver.ui.model.DevServerTableModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @BelongsPackage: com.tanghui.run
 * @Author: 唐煇
 * @CreateTime: 2024-07-19 09:37
 * @Description: 描述类的主要功能和用途。
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DevServerSettingsEditor extends SettingsEditor<DevServerRunConfiguration> {

    private ClusterDeployment devServerRunUI;

    private Project project;

    private DevServerRunConfiguration runConfigName;

    public DevServerSettingsEditor(Project project, DevServerRunConfiguration runConfigName) {
        Disposer.register(project, () -> {
            disposeEditor();
            uninstallWatcher();
        });
        this.project = project;
        this.runConfigName = runConfigName;
        this.devServerRunUI = new ClusterDeployment(project, runConfigName);
    }

    @Override
    protected void resetEditorFrom(@NotNull DevServerRunConfiguration s) {
        //从RunConfiguration对象获取值并设置到文本框中
        // 设置待上传文件路径
        String uploadFile = s.getUploadFile();
        this.devServerRunUI.getTextField().setText(uploadFile);
        // 设置升级类型
        String upgradeType = s.getUpgradeType();
        applyUpgradeType(upgradeType);
        List<DevServerRunConfig> runConfigList = s.getRunConfigList();
        if (runConfigList == null || runConfigList.isEmpty()) {
            this.devServerRunUI.getScriptPanel().setVisible(false);
            return;
        }
        this.devServerRunUI.getScriptPanel().setVisible(true);
        DevServerTable devServerTable = this.devServerRunUI.getDevServerTable();
        DevServerTableModel tableModel = devServerTable.getTableModel();
        // 清空数据
        tableModel.setRowCount(0);
        this.devServerRunUI.getScriptTabbedPane().removeAll();
        this.devServerRunUI.setRunConfigList(runConfigList);
        for (int i = 0; i < runConfigList.size(); i++) {
            DevServerRunConfig runConfig = runConfigList.get(i);
            String serverHost = runConfig.getServerHost();
            String serverPort = runConfig.getServerPort();
            String serverUser = runConfig.getServerUser();
            String controlsUser = runConfig.getControlsUser();
            tableModel.insertRow(i, new Object[]{serverHost, serverPort, serverUser, controlsUser});
            this.devServerRunUI.addScriptTabbedPane(runConfigList, runConfig);
        }

        switch (upgradeType) {
            case "upgrade":
                setJTabbedPane(true, true, true);
                break;
            case "upload":
                setJTabbedPane(false, false, true);
                break;
            case "execute":
                setJTabbedPane(false, false, false);
                break;
            default:
                // 默认 upgrade
                setJTabbedPane(true, true, true);
                break;
        }
        this.devServerRunUI.getScriptPanel().updateUI();
        this.devServerRunUI.getScriptTabbedPane().updateUI();
    }

    @Override
    protected void applyEditorTo(@NotNull DevServerRunConfiguration s) throws ConfigurationException {
        //设置RunConfiguration对象的值从文本框中获取
        String uploadFile = this.devServerRunUI.getTextField().getText().trim();
        s.setUploadFile(uploadFile);

        // 设置升级类型
        boolean upgradeSelected = this.devServerRunUI.getUpgradeRadioButton().isSelected();
        boolean uploadSelected = this.devServerRunUI.getUploadRadioButton().isSelected();
        boolean executeSelected = this.devServerRunUI.getExecuteRadioButton().isSelected();
        if (upgradeSelected) {
            s.setUpgradeType("upgrade");
        } else if (uploadSelected) {
            s.setUpgradeType("upload");
        } else if (executeSelected) {
            s.setUpgradeType("execute");
        } else {
            s.setUpgradeType("upgrade");
        }
        s.options.runConfigList = (this.devServerRunUI.getRunConfigList());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        AtomicReference<JComponent> jComponent = new AtomicReference<>(new JPanel());
        ApplicationManager.getApplication().invokeAndWait(() ->
                jComponent.set(this.devServerRunUI.getRoot())
        );
        return jComponent.get();
    }

    @Override
    protected void disposeEditor() {
        super.dispose();
        Disposer.dispose(this);
    }

    private void applyUpgradeType(String upgradeType) {
        // 先统一清空状态
        this.devServerRunUI.getUpgradeRadioButton().setSelected(false);
        this.devServerRunUI.getUploadRadioButton().setSelected(false);
        this.devServerRunUI.getExecuteRadioButton().setSelected(false);
        if (upgradeType == null) {
            this.devServerRunUI.getUpgradeRadioButton().setSelected(true);
            setPanelVisible(true, true, true, true);
            return;
        }
        switch (upgradeType) {
            case "upgrade":
                this.devServerRunUI.getUpgradeRadioButton().setSelected(true);
                setPanelVisible(true, true, true, true);
                break;
            case "upload":
                this.devServerRunUI.getUploadRadioButton().setSelected(true);
                setPanelVisible(true, false, false, true);
                break;
            case "execute":
                this.devServerRunUI.getExecuteRadioButton().setSelected(true);
                setPanelVisible(false, false, false, false);
                break;
            default:
                // 默认 upgrade
                this.devServerRunUI.getUpgradeRadioButton().setSelected(true);
                setPanelVisible(true, true, true, true);
                break;
        }
    }

    private void setPanelVisible(
            boolean uploadFile,
            boolean rollback,
            boolean log,
            boolean workDir) {
        this.devServerRunUI.getUploadFilePanel().setVisible(uploadFile);
        this.devServerRunUI.getRollbackPanel().setVisible(rollback);
        this.devServerRunUI.getLogePanel().setVisible(log);
        this.devServerRunUI.getWorkDirectory().setVisible(workDir);
    }

    private void setJTabbedPane(boolean rollback,
                                boolean log,
                                boolean workDir) {
        List<DevServerRunConfig> runConfigList = this.devServerRunUI.getRunConfigList();
        JTabbedPane scriptTabbedPane = this.devServerRunUI.getScriptTabbedPane();
        if ((runConfigList != null && !runConfigList.isEmpty()) && (scriptTabbedPane != null)) {
            for (int i = 0; i < runConfigList.size(); i++) {
                DevServerRunConfig runConfig = runConfigList.get(i);
                String serverHost = runConfig.getServerHost();
                ExecuteScript executeScript = new ExecuteScript(project, runConfigList, runConfig);
                executeScript.getRollbackPanel().setVisible(rollback);
                executeScript.getLogePanel().setVisible(log);
                executeScript.getWorkDirectory().setVisible(workDir);
                scriptTabbedPane.setTitleAt(i, serverHost);
                scriptTabbedPane.setComponentAt(i, executeScript.getRoot());
            }
        }
    }
}
