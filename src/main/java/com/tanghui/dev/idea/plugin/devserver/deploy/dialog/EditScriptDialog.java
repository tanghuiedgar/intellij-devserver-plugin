package com.tanghui.dev.idea.plugin.devserver.deploy.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.deploy.dialog
 * @Author: 唐煇
 * @CreateTime: 2026-01-20-15:50
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class EditScriptDialog extends DialogWrapper {
    @Getter
    private final EditScript editScript;
    @Getter
    private String content;

    public EditScriptDialog(@Nullable Project project, String name, String content) {
        super(project, true);
        this.editScript = new EditScript(project, name, content);
        this.content = content;
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.editScript.$$$getRootComponent$$$();
    }


    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
                onOK(),
                getCancelAction()
        };
    }

    private Action onOK() {
        AbstractAction ok = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!editScript.getContent().equals(editScript.getShellScriptEditor().getText())) {
                    int result = Messages.showYesNoDialog(
                            "确认修改脚本",
                            "修改脚本",
                            DevServerBundle.INSTANCE.message("define", ""),
                            DevServerBundle.INSTANCE.message("cancel", ""),
                            Messages.getQuestionIcon()
                    );
                    if (result == Messages.YES) {
                        editScript.setEdit(true);
                        content = editScript.getShellScriptEditor().getText();
                    }
                }
                dispose();
            }
        };
        ok.putValue(Action.NAME, "OK");
        return ok;
    }

}
