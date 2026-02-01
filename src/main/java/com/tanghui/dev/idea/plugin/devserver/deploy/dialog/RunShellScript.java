package com.tanghui.dev.idea.plugin.devserver.deploy.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons;
import com.tanghui.dev.idea.plugin.devserver.pool.GlobalSshPoolManager;
import com.tanghui.dev.idea.plugin.devserver.pool.SshConnectionPool;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import com.tanghui.dev.idea.plugin.devserver.ui.HorizontalScrollBarEditor;
import com.tanghui.dev.idea.plugin.devserver.utils.file.FileUtil;
import lombok.Getter;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.fileExists;


/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.deploy.dialog
 * @Author: 唐煇
 * @CreateTime: 2026-01-20-14:21
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
@Getter
public class RunShellScript {
    private JBPanel<?> root;
    private JBPanel<?> shellScript;
    private JBLabel uploadLabel;
    private ComboBox<String> serverListComboBox;
    private JButton uploadButton;
    private JButton saveButton;
    private JBTextField nameTextField;
    private final Project project;
    private HorizontalScrollBarEditor shellScriptTextField;

    public RunShellScript(Project project, String targetDirectory, String shellName, List<DevServerRunConfig> runConfigList) {
        this.project = project;
        if (CollectionUtils.isNotEmpty(runConfigList)) {
            List<String> list = runConfigList.stream().map(DevServerRunConfig::getServerHost).distinct().toList();
            list.forEach(v -> serverListComboBox.addItem(v));
        }
        uploadLabel.setText("");
        uploadButton.setToolTipText(DevServerBundle.INSTANCE.message("upload.script.server"));
        uploadButton.setIcon(DevServerIcons.DevServer_UPLOAD);
        saveButton.setToolTipText(DevServerBundle.INSTANCE.message("save.script"));
        saveButton.setIcon(DevServerIcons.DevServer_SAVE);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("sh");
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("logeText.sh", fileType, "ll -a");
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            HorizontalScrollBarEditor shellScriptTextField = new HorizontalScrollBarEditor(document, project, fileType, false, false);
            this.shellScriptTextField = shellScriptTextField;
            shellScriptTextField.setFont(EditorUtil.getEditorFont());
            shellScriptTextField.updateUI();
            shellScript.removeAll();
            shellScript.add(shellScriptTextField);
            ApplicationManager.getApplication().runWriteAction(() -> {
                File file = FileUtil.getInstance().getConfigPathExample("template" + File.separator + "shell", shellName);
                if (file == null) {
                    file = FileUtil.getInstance().getResourceFile("/template/shell", shellName);
                }
                String shellScript = FileUtil.getInstance().getFileContents(file).trim();
                shellScriptTextField.setText(shellScript);
            });
            shellScript.updateUI();
        });


        uploadButton.addActionListener(e -> {
            if (CollectionUtils.isNotEmpty(runConfigList)) {
                uploadLabel.setText(DevServerBundle.INSTANCE.message("upload.file.server.info"));
                SwingUtilities.invokeLater(() -> {
                    // 获取文件下拉框选择服务器
                    String server = (String) serverListComboBox.getSelectedItem();
                    Optional<DevServerRunConfig> devServerRunConfig = runConfigList.stream().filter(v -> v.getServerHost().equals(server)).findFirst();
                    if (devServerRunConfig.isPresent()) {
                        SshConnectionPool pool = GlobalSshPoolManager.getPool(
                                devServerRunConfig.get().getServerHost(),
                                Integer.parseInt(devServerRunConfig.get().getServerPort()),
                                devServerRunConfig.get().getServerUser(),
                                devServerRunConfig.get().getServerPassword()
                        );
                        SSHClient ssh = null;
                        Session session = null;
                        try {
                            // 创建会话并连接
                            ssh = pool.borrow();
                            // 创建 SFTP通道并连接
                            session = ssh.startSession();
                            Session.Shell shell = session.startShell();
                            if (fileExists(ssh, targetDirectory + "/" + shellName)) {
                                // 第一次上传文件时删除已经存在的文件
                                try (SFTPClient sftpClient = ssh.newSFTPClient()) {
                                    sftpClient.rm(targetDirectory + "/" + shellName);
                                }
                            }
                            File file = FileUtil.getInstance().getConfigPathExample("template" + File.separator + "shell", shellName);
                            if (file == null) {
                                file = FileUtil.getInstance().getResourceFile("/template/shell", shellName);
                            }
                            // 上传服务器
                            try {
                                try (SFTPClient sftpClient = ssh.newSFTPClient()) {
                                    sftpClient.put(file.getAbsolutePath(), targetDirectory + "/" + shellName);
                                }
                                OutputStream outputStream = shell.getOutputStream();
                                if (StringUtils.isNotBlank(devServerRunConfig.get().getControlsUser()) &&
                                        !devServerRunConfig.get().getControlsUser().equals(devServerRunConfig.get().getServerUser())) {
                                    outputStream.write(("chmod +x " + targetDirectory + "/" + shellName + "\n")
                                            .getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    outputStream.write(("chown " + devServerRunConfig.get().getControlsUser() + ":" + devServerRunConfig.get().getControlsUser() + " " + targetDirectory + "/" + shellName + "\n")
                                            .getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    outputStream.write((("echo \"exit\"\n")).getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    InputStream inputStream = shell.getInputStream();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                                    while (true) {
                                        Future<String> future = executorService.submit(reader::readLine);
                                        try {
                                            String line = future.get(20, TimeUnit.SECONDS);  // 设置超时时间
                                            if (line != null) {
                                                if ("exit".equalsIgnoreCase(line.trim())) {
                                                    break;
                                                }
                                            }
                                        } catch (TimeoutException exception) {
                                            future.cancel(true);  // 超时后取消任务
                                            break;
                                        } catch (InterruptedException | ExecutionException exception) {
                                            break;
                                        }
                                    }
                                    executorService.shutdown();
                                }
                                if (fileExists(ssh, targetDirectory + "/" + shellName)) {
                                    // 第一次上传文件时删除已经存在的文件
                                    Messages.showMessageDialog(
                                            DevServerBundle.INSTANCE.message("upload.file.server.success.message"),
                                            DevServerBundle.INSTANCE.message("upload.file.server.title"),
                                            Messages.getInformationIcon()
                                    );
                                    uploadLabel.setText(DevServerBundle.INSTANCE.message("upload.file.server.success.message"));
                                } else {
                                    Messages.showMessageDialog(
                                            DevServerBundle.INSTANCE.message("upload.file.server.error.message"),
                                            DevServerBundle.INSTANCE.message("upload.file.server.title"),
                                            Messages.getInformationIcon()
                                    );
                                    uploadLabel.setText(DevServerBundle.INSTANCE.message("upload.file.server.error.message"));
                                }
                            } catch (IOException exception) {
                                Messages.showMessageDialog(
                                        DevServerBundle.INSTANCE.message("upload.file.server.error.message"),
                                        DevServerBundle.INSTANCE.message("upload.file.server.title"),
                                        Messages.getErrorIcon()
                                );
                                uploadLabel.setText(DevServerBundle.INSTANCE.message("upload.file.server.error.message"));
                            }

                        } catch (Exception exception) {
                            Messages.showMessageDialog(
                                    DevServerBundle.INSTANCE.message("upload.file.server.error.message"),
                                    DevServerBundle.INSTANCE.message("upload.file.server.title"),
                                    Messages.getErrorIcon()
                            );
                            uploadLabel.setText(DevServerBundle.INSTANCE.message("upload.file.server.error.message"));
                        } finally {
                            // 关闭资源
                            if (session != null) try {
                                session.close();
                            } catch (Exception ignored) {
                            }
                            pool.release(ssh);
                        }
                    }
                });
            }
        });

        saveButton.addActionListener(e -> {
            // 获取文本内容
            if (this.shellScriptTextField != null) {
                String text = this.shellScriptTextField.getDocument().getText();
                File file = FileUtil.getInstance().saveConfigPathExample("template" + File.separator + "shell", shellName, text);
                if (file != null) {
                    Messages.showMessageDialog(
                            DevServerBundle.INSTANCE.message("save.file.success.message"),
                            DevServerBundle.INSTANCE.message("save.file"),
                            Messages.getInformationIcon()
                    );
                } else {
                    Messages.showMessageDialog(
                            DevServerBundle.INSTANCE.message("save.file.error.message"),
                            DevServerBundle.INSTANCE.message("save.file"),
                            Messages.getErrorIcon()
                    );

                }
            }
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /** Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        root = new JBPanel();
        root.setLayout(new GridLayoutManager(1, 1, new Insets(0, 10, 10, 10), -1, -1));
        root.setMaximumSize(JBUI.size(600, 700));
        root.setMinimumSize(JBUI.size(600, 700));
        root.setPreferredSize(JBUI.size(600, 700));
        final JBPanel panel1 = new JBPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        root.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel2 = new JBPanel();
        panel2.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setMaximumSize(JBUI.size(2147483647, 40));
        panel2.setMinimumSize(JBUI.size(24, 40));
        panel2.setPreferredSize(JBUI.size(24, 40));
        panel1.add(panel2, BorderLayout.NORTH);
        final JBPanel panel3 = new JBPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, JBUI.size(160, -1), JBUI.size(160, -1), JBUI.size(160, -1), 0, false));
        serverListComboBox = new ComboBox();
        panel3.add(serverListComboBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JBPanel panel4 = new JBPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        uploadButton = new JButton();
        uploadButton.setBorderPainted(false);
        uploadButton.setText("");
        panel4.add(uploadButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, JBUI.size(30, 30), JBUI.size(30, 30), JBUI.size(30, 30), 0, false));
        saveButton = new JButton();
        saveButton.setBorderPainted(false);
        saveButton.setText("");
        panel4.add(saveButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, JBUI.size(30, 30), JBUI.size(30, 30), JBUI.size(30, 30), 0, false));
        final JBPanel panel5 = new JBPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel5, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        uploadLabel = new JBLabel();
        uploadLabel.setText("");
        panel5.add(uploadLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel6 = new JBPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, JBUI.size(100, -1), JBUI.size(100, -1), JBUI.size(100, -1), 0, false));
        nameTextField = new JBTextField();
        nameTextField.setEditable(false);
        panel6.add(nameTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shellScript = new JBPanel();
        shellScript.setLayout(new BorderLayout(0, 0));
        panel1.add(shellScript, BorderLayout.CENTER);
        shellScript.setBorder(BorderFactory.createTitledBorder(null, DevServerBundle.INSTANCE.message("shell.script"), TitledBorder.LEFT, TitledBorder.ABOVE_TOP, null, null));
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}
