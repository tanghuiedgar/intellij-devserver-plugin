package com.tanghui.dev.idea.plugin.devserver.deploy.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.deploy.dialog
 * @Author: 唐煇
 * @CreateTime: 2026-01-20-14:20
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class RunShellScriptDialog extends DialogWrapper {

    @Getter
    private final RunShellScript runShellScript;

    public RunShellScriptDialog(@Nullable Project project, String targetDirectory, String shellName, List<DevServerRunConfig> runConfigList) {
        super(project, true);
        this.runShellScript = new RunShellScript(project, targetDirectory, shellName, runConfigList);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.runShellScript.$$$getRootComponent$$$();
    }
}
