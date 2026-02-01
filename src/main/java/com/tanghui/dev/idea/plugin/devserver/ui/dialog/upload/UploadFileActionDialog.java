package com.tanghui.dev.idea.plugin.devserver.ui.dialog.upload;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.data.model.FileTransferModel;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.task.FileTransferCallback;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.ui.dialog.upload
 * @Author: 唐煇
 * @CreateTime: 2026-01-20-13:42
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class UploadFileActionDialog extends DialogWrapper {

    private final UploadFileAction uploadFileAction;

    private final Project project;

    private final FileTransferModel fileTransferModel;

    @Getter
    private boolean start = false;

    public UploadFileActionDialog(@Nullable Project project,
                                  ServerHostModel serverHostModel,
                                  FileTransferModel fileTransferModel,
                                  FileTransferCallback fileTransferCallback) {
        super(project, true);
        setTitle(DevServerBundle.INSTANCE.message("select.upload.file"));
        this.project = project;
        this.fileTransferModel = fileTransferModel;
        this.uploadFileAction = new UploadFileAction(project, serverHostModel, fileTransferModel, fileTransferCallback);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.uploadFileAction.$$$getRootComponent$$$();
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
                boolean selected = uploadFileAction.getCurrentFile().isSelected();
                if (selected) {
                    // 获取当前编辑器打开的文件信息
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                    // 获取当前打开的第一个编辑器
                    FileEditor editor = fileEditorManager.getSelectedEditor();
                    if (editor != null) {
                        VirtualFile file = editor.getFile();
                        String name = file.getName();
                        // 当前打开文件
                        fileTransferModel.setLocalFilesPath(file.getParent().getPath());
                        fileTransferModel.setLocalFilesName(name);
                        fileTransferModel.setRemoteFilesName(name);
                    }
                } else {
                    // 其他选择的文件
                    String uploadFilePath = uploadFileAction.getFileTextField().getText();
                    if (StringUtils.isNotBlank(uploadFilePath)) {
                        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(uploadFilePath);
                        if (file != null) {
                            // 选择上传文件
                            fileTransferModel.setLocalFilesPath(file.getParent().getPath());
                            fileTransferModel.setLocalFilesName(file.getName());
                            fileTransferModel.setRemoteFilesName(file.getName());
                        }
                    }
                }
                start = true;
                dispose();
            }
        };
        ok.putValue(Action.NAME, "OK");
        return ok;
    }
}
