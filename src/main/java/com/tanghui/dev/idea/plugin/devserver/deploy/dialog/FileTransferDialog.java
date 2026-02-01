package com.tanghui.dev.idea.plugin.devserver.deploy.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.data.model.FileTransferModel;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.task.FileTransferCallback;
import com.tanghui.dev.idea.plugin.devserver.transfer.ui.FileTransfer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.deploy.dialog
 * @Author: 唐煇
 * @CreateTime: 2026-01-20-10:28
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class FileTransferDialog extends DialogWrapper {

    private final FileTransfer fileTransfer;

    public FileTransferDialog(Project project, ServerHostModel serverHost, FileTransferCallback transferCallback, FileTransferModel fileTransferModel) {
        super(true);
        setTitle(DevServerBundle.INSTANCE.message("file.transfer"));
        this.fileTransfer = new FileTransfer(project, serverHost, transferCallback, fileTransferModel);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.fileTransfer.getRoot();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
                onOK()
        };
    }

    private Action onOK() {
        AbstractAction ok = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Window window = getPeer().getWindow();
                if (window != null) {
                    window.setVisible(false); // 👈 关键
                }
            }
        };
        ok.putValue(Action.NAME, "OK");
        return ok;
    }

    public void showDialogAgain() {
        Window window = getPeer().getWindow();
        if (window != null) {
            window.setVisible(true);
            window.toFront();
            window.requestFocus();
        }
    }

    @Override
    public void dispose(){
        ApplicationManager.getApplication().invokeLater(super::dispose);
    }
}
