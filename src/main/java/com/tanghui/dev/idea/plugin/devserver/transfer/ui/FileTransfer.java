package com.tanghui.dev.idea.plugin.devserver.transfer.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.data.model.FileTransferModel;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons;
import com.tanghui.dev.idea.plugin.devserver.pool.GlobalSshPoolManager;
import com.tanghui.dev.idea.plugin.devserver.pool.SshConnectionPool;
import com.tanghui.dev.idea.plugin.devserver.task.FileTransferCallback;
import com.tanghui.dev.idea.plugin.devserver.transfer.listener.FileTransferListener;
import com.tanghui.dev.idea.plugin.devserver.utils.file.FileUtil;
import lombok.Getter;
import lombok.Setter;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.SFTPClient;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.execStdout;
import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.fileExists;
import static com.tanghui.dev.idea.plugin.devserver.utils.SizeFormatUtils.formatBinary;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.connect.transfer.ui
 * @Author: 唐煇
 * @CreateTime: 2025-12-03-14:38
 * @Description: 文件传输ui。
 * @Version: v1.0
 */
@Setter
@Getter
public class FileTransfer {
    // 根容器组件
    private JBPanel<?> root;
    // 启动/停止按钮
    private JButton stateButton;
    // 重新传输按钮
    private JButton startOverButton;
    // 终止传输按钮
    private JButton endButton;
    // 查看本地传输文件路径按钮
    private JButton fileFindButton;
    // 文件大小
    private JBLabel fileSizeLabel;
    // 文件名称
    private JBTextField fileNameTextField;
    // 传输进度条
    private JProgressBar scheduleProgressBar;
    // 传输进度百分比展示
    private JBLabel scheduleLabel;
    // 剩余时间
    private JBLabel timeLeftLabel;
    // 传输时间
    private JBLabel timeTransferLabel;
    // 文件MD5值
    private JBTextField fileMD5TextField;
    // 服务器地址
    private JBTextField serverTextField;
    // 服务器路径
    private JBTextField pathTextField;
    // 网络速度
    private JBLabel transferSpeedLabel;
    // 传输方向
    private JBLabel directionLabel;
    // 操作用户
    private JBTextField operationTextField;

    /**
     * 远程服务器信息
     *
     */
    private final ServerHostModel serverHost;
    /**
     * 传输文件完成回调方法类
     *
     */
    private final FileTransferCallback transferCallback;
    /**
     * 文件传输
     *
     */
    private final FileTransferModel fileTransferModel;
    /**
     * 当前打开项目
     *
     */
    private final Project project;

    /**
     * 是否正在创建连接
     *
     */
    private boolean createConnect = false;

    // 启动
    private boolean start = true;
    // 重启
    private boolean restart = false;

    private static final String START_TEXT = "已启动";
    private static final String STOP_TEXT = "已停止";

    private boolean finish = false;

    // 服务器连接会话
    private SSHClient ssh;
    private SshConnectionPool pool;

    public FileTransfer(Project project,
                        ServerHostModel serverHost,
                        FileTransferCallback transferCallback,
                        FileTransferModel fileTransferModel) {
        this.serverHost = serverHost;
        this.transferCallback = transferCallback;
        this.fileTransferModel = fileTransferModel;
        this.project = project;
        createConnection();
        initComponents();
        actionMonitoring();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // 上传文件
        executorService.submit(() -> transferFiles(false));
        executorService.shutdown();
    }

    // 初始化组件
    private void initComponents() {
        if (this.stateButton != null) {
            this.stateButton.setBorder(null);
            this.stateButton.setIcon(DevServerIcons.DevServer_STOP);
            this.stateButton.setText(START_TEXT);
        }
        if (this.fileFindButton != null) {
            this.fileFindButton.setBorder(null);
            this.fileFindButton.setIcon(DevServerIcons.DevServer_OPEN);
            this.fileFindButton.setToolTipText("本地文件路径");
        }
        if (this.startOverButton != null) {
            this.startOverButton.setBorder(null);
            this.startOverButton.setIcon(DevServerIcons.DevServer_RESTART);
        }
        if (this.endButton != null) {
            this.endButton.setBorder(null);
            this.endButton.setIcon(DevServerIcons.DevServer_END);
        }
        if (this.directionLabel != null) {
            if (this.fileTransferModel.getState()) {
                this.directionLabel.setText("上传");
                File file = new File(this.fileTransferModel.getLocalFilesPath() + File.separator + this.fileTransferModel.getLocalFilesName());
                if (file.exists()) {
                    // 计算文件的 MD5 值
                    try {
                        String checksum = FileUtil.getInstance().getFileChecksum(file);
                        this.fileMD5TextField.setText(checksum);
                    } catch (Exception ignored) {
                    }
                    long length = file.length();
                    this.fileSizeLabel.setText(formatBinary(length) + " / " + length + " bytes");
                    // 设置进度条最小值和最大值
                    this.scheduleProgressBar.setMinimum(0);
                    this.scheduleProgressBar.setMaximum((int) length);
                }
            }
            this.fileNameTextField.setText(this.fileTransferModel.getLocalFilesName());
            this.serverTextField.setText(this.serverHost.getHost());
            this.operationTextField.setText(this.serverHost.getUserName());
            this.pathTextField.setText(this.fileTransferModel.getRemoteFilesPath());
        }
    }

    // 创建连接
    private void createConnection() {
        new Task.Backgroundable(project, "正在连接服务器...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    createConnect = true;
                    pool = GlobalSshPoolManager.getPool(
                            serverHost.getHost(),
                            serverHost.getPort(),
                            serverHost.getUserName(),
                            serverHost.getPassword()
                    );
                    ssh = pool.borrow();
                    createConnect = false;
                    start = true;
                    if (!fileTransferModel.getState()) {
                        directionLabel.setText("下载");
                        String remoteFilePath = fileTransferModel.getRemoteFilesPath() + "/" + fileTransferModel.getRemoteFilesName();
                        try (SFTPClient sftpClient = ssh.newSFTPClient();
                             Session session = ssh.startSession()) {
                            long size = 0L;
                            if (fileExists(ssh, remoteFilePath)) {
                                FileAttributes attrs = sftpClient.stat(remoteFilePath);
                                size = attrs.getSize();
                            }
                            String md5File = execStdout(session, "echo $(md5sum " + remoteFilePath + "|cut -d ' ' -f1)");
                            fileMD5TextField.setText(md5File);
                            fileSizeLabel.setText(formatBinary(size) + " / " + size + " bytes");
                            // 设置进度条最小值和最大值
                            scheduleProgressBar.setMinimum(0);
                            scheduleProgressBar.setMaximum((int) size);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception e) {
                    if (start) {
                        finish = true;
                        ApplicationManager.getApplication().invokeLater(() ->
                                NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                        .createNotification("服务器连接", "服务器连接失败！", NotificationType.ERROR)
                                        .notify(project)
                        );
                    }
                }
            }
        }.queue();
    }

    // 关闭连接
    public void closeConnection() {
        this.start = false;
        closeQuietly(this.ssh);
        try {
            this.pool.delete(this.ssh);
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(SSHClient c) {
        if (c == null) return;
        try {
            c.disconnect();
        } catch (Throwable ignored) {
        }
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }

    // 动作监听
    private void actionMonitoring() {
        // 查看本地文件路径
        this.fileFindButton.addActionListener(e -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // 在后台线程执行文件查找操作
            // 创建文件选择描述器，仅允许选择文件夹
            FileChooserDescriptor folderChooserDescriptor = new FileChooserDescriptor(
                    true, // 允许选择文件
                    true,  // 允许选择文件夹
                    true,
                    false,
                    false,
                    false
            );
            VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(fileTransferModel.getLocalFilesPath() + File.separator + fileTransferModel.getLocalFilesName());
            ApplicationManager.getApplication().invokeLater(() -> FileChooser.chooseFile(folderChooserDescriptor, project, fileByPath));
        }));

        // 启动/关闭按钮监听
        this.stateButton.addActionListener(e -> stateTransfer());

        // 重新传输按钮监听
        this.startOverButton.addActionListener(e -> startOverTransfer());

        // 终止传输按钮监听
        this.endButton.addActionListener(e -> endTransfer());
    }

    // 启动传输
    private void stateTransfer() {
        if (this.createConnect) {
            // 连接正在创建中
            Messages.showMessageDialog(
                    "连接正在创建中请稍后重试！",
                    "Host",
                    Messages.getInformationIcon()
            );
            return;
        }
        if (start) {
            // 已启动的传输点击则代表关闭
            closeConnection();
            this.transferSpeedLabel.setText("-- KB/sec");
            this.timeLeftLabel.setText("-- --");
            this.stateButton.setIcon(DevServerIcons.DevServer_START);
            this.stateButton.setText(STOP_TEXT);
        } else {
            // 已关闭的传输点击则代表启动
            createConnection();
            stateButton.setIcon(DevServerIcons.DevServer_STOP);
            stateButton.setText(START_TEXT);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            // 上传文件
            executorService.submit(() -> transferFiles(true));
            executorService.shutdown();
        }
    }

    // 重新传输
    private void startOverTransfer() {
        if (this.createConnect) {
            // 连接正在创建中
            Messages.showMessageDialog(
                    "连接正在创建中请稍后重试！",
                    "Host",
                    Messages.getInformationIcon()
            );
            return;
        }
        int result = Messages.showYesNoDialog(
                "是否重新传输",
                "文件传输",
                DevServerBundle.INSTANCE.message("define", ""),
                DevServerBundle.INSTANCE.message("cancel", ""),
                Messages.getQuestionIcon()
        );
        if (result == Messages.YES) {
            restart = true;
            closeConnection();
            createConnection();
            restart = false;
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            // 上传文件
            executorService.submit(() -> transferFiles(false));
            executorService.shutdown();
        }
    }

    // 终止传输
    private void endTransfer() {
        if (this.createConnect) {
            // 连接正在创建中
            Messages.showMessageDialog(
                    "连接正在创建中请稍后重试！",
                    "Host",
                    Messages.getInformationIcon()
            );
            return;
        }
        int result = Messages.showYesNoDialog(
                "是否终止传输",
                "文件传输",
                DevServerBundle.INSTANCE.message("define", ""),
                DevServerBundle.INSTANCE.message("cancel", ""),
                Messages.getQuestionIcon()
        );
        if (result == Messages.YES) {
            closeConnection();
            // 禁用按钮
            this.stateButton.setEnabled(false);
            this.startOverButton.setEnabled(false);
            this.endButton.setEnabled(false);
            this.finish = true;
            this.timeLeftLabel.setText("已终止");
            this.timeLeftLabel.setFont(new Font("黑体", Font.BOLD, 16));
            this.timeLeftLabel.setForeground(new JBColor(new Color(205, 5, 5), new Color(205, 5, 5)));
            this.transferCallback.stopTransfer();
        }
    }

    // 传输文件
    // restart 是否重启
    private void transferFiles(boolean restart) {
        try (SFTPClient sftp = this.ssh.newSFTPClient()) {
            Boolean state = this.fileTransferModel.getState();
            // 上传文件
            String localFilePath = fileTransferModel.getLocalFilesPath() + File.separator + fileTransferModel.getLocalFilesName();
            String remoteFilePath = fileTransferModel.getRemoteFilesPath() + "/" + fileTransferModel.getRemoteFilesName();
            // 设置文件监听
            sftp.getFileTransfer().setTransferListener(
                    new FileTransferListener(this)
            );
            if (state) {
                // 上传文件
                try (SFTPClient sftpClient = this.ssh.newSFTPClient()) {
                    long size = 0L;
                    if (fileExists(this.ssh, remoteFilePath)) {
                        FileAttributes attrs = sftpClient.stat(remoteFilePath);
                        size = attrs.getSize();
                    }
                    if (restart) {
                        // 继续传输，读取服务器文件大小
                        fileTransferModel.setOffset(size);
                    } else {
                        // 删除服务器文件
                        if (size > 0) {
                            sftpClient.rm(remoteFilePath);
                        }
                        fileTransferModel.setOffset(0L);
                    }
                }
                sftp.put(localFilePath, remoteFilePath, fileTransferModel.getOffset());
            } else {
                // 下载文件
                File file = new File(localFilePath);
                long size = 0L;
                if (file.exists()) {
                    size = file.length();
                } else {
                    file.createNewFile();   // 创建一个空文件
                }
                if (restart) {
                    // 继续传输，读取本地文件大小
                    fileTransferModel.setOffset(size);
                } else {
                    // 删除服务器文件
                    if (file.exists()) {
                        FileOutputStream fos = new FileOutputStream(file, false); //
                        fos.close(); // 立即关闭即可，文件内容被清空
                    } else {
                        file.createNewFile();
                    }
                    fileTransferModel.setOffset(0L);
                }
                sftp.get(remoteFilePath, localFilePath, fileTransferModel.getOffset());
            }
        } catch (IOException e) {
            if (start && !"Disconnected".equals(e.getMessage())) {
                closeConnection();
                this.transferSpeedLabel.setText("-- KB/sec");
                this.timeLeftLabel.setText("-- --");
                this.stateButton.setIcon(DevServerIcons.DevServer_START);
                this.stateButton.setText(STOP_TEXT);
                finish = true;
                ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                        .createNotification("服务器连接", "传输文件失败！", NotificationType.INFORMATION)
                        .addAction(new NotificationAction("") {
                            @Override
                            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            }
                        }).notify(project));
            }
        }
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
        root.setLayout(new GridLayoutManager(9, 1, new Insets(5, 5, 5, 5), -1, -1));
        root.setMinimumSize(new Dimension(500, 400));
        root.setPreferredSize(new Dimension(500, 400));
        final JBPanel panel1 = new JBPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        root.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel2 = new JBPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setMaximumSize(new Dimension(100, 2147483647));
        panel2.setMinimumSize(new Dimension(100, 24));
        panel2.setPreferredSize(new Dimension(100, 24));
        panel1.add(panel2, BorderLayout.WEST);
        final JBLabel label1 = new JBLabel();
        Font label1Font = this.$$$getFont$$$(null, Font.BOLD, -1, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setForeground(new Color(-16722177));
        label1.setText("文件大小");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel3 = new JBPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, BorderLayout.CENTER);
        fileSizeLabel = new JBLabel();
        fileSizeLabel.setText("");
        panel3.add(fileSizeLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel4 = new JBPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        root.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel5 = new JBPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.setMaximumSize(new Dimension(100, 2147483647));
        panel5.setMinimumSize(new Dimension(100, 24));
        panel5.setPreferredSize(new Dimension(100, 24));
        panel4.add(panel5, BorderLayout.WEST);
        final JBLabel label2 = new JBLabel();
        Font label2Font = this.$$$getFont$$$(null, Font.BOLD, -1, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setForeground(new Color(-16722177));
        label2.setText("文件名称");
        panel5.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel6 = new JBPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.setMaximumSize(new Dimension(40, 2147483647));
        panel6.setMinimumSize(new Dimension(40, 24));
        panel6.setPreferredSize(new Dimension(40, 24));
        panel4.add(panel6, BorderLayout.EAST);
        fileFindButton = new JButton();
        fileFindButton.setText("");
        panel6.add(fileFindButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(35, 35), new Dimension(35, 35), new Dimension(35, 35), 0, false));
        final JBPanel panel7 = new JBPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel7, BorderLayout.CENTER);
        fileNameTextField = new JBTextField();
        fileNameTextField.setEditable(false);
        panel7.add(fileNameTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JBPanel panel8 = new JBPanel();
        panel8.setLayout(new BorderLayout(0, 0));
        root.add(panel8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel9 = new JBPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel9.setMaximumSize(new Dimension(100, 2147483647));
        panel9.setMinimumSize(new Dimension(100, 24));
        panel9.setPreferredSize(new Dimension(100, 24));
        panel8.add(panel9, BorderLayout.WEST);
        final JBLabel label3 = new JBLabel();
        Font label3Font = this.$$$getFont$$$(null, Font.BOLD, -1, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setForeground(new Color(-16722177));
        label3.setText("传输进度");
        panel9.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel10 = new JBPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel10.setMaximumSize(new Dimension(80, 2147483647));
        panel10.setMinimumSize(new Dimension(80, 24));
        panel10.setPreferredSize(new Dimension(80, 24));
        panel8.add(panel10, BorderLayout.EAST);
        scheduleLabel = new JBLabel();
        scheduleLabel.setText("");
        panel10.add(scheduleLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel11 = new JBPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel11, BorderLayout.CENTER);
        scheduleProgressBar = new JProgressBar();
        panel11.add(scheduleProgressBar, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        final JBPanel panel12 = new JBPanel();
        panel12.setLayout(new BorderLayout(0, 0));
        root.add(panel12, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel13 = new JBPanel();
        panel13.setLayout(new BorderLayout(0, 0));
        panel13.setMaximumSize(new Dimension(2147483647, 2147483647));
        panel13.setMinimumSize(new Dimension(230, 24));
        panel13.setPreferredSize(new Dimension(230, 24));
        panel12.add(panel13, BorderLayout.WEST);
        final JBPanel panel14 = new JBPanel();
        panel14.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel14.setMaximumSize(new Dimension(100, 2147483647));
        panel14.setMinimumSize(new Dimension(100, 24));
        panel14.setPreferredSize(new Dimension(100, 24));
        panel13.add(panel14, BorderLayout.WEST);
        final JBLabel label4 = new JBLabel();
        Font label4Font = this.$$$getFont$$$(null, Font.BOLD, -1, label4.getFont());
        if (label4Font != null) label4.setFont(label4Font);
        label4.setForeground(new Color(-16722177));
        label4.setText("网络速度");
        panel14.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel15 = new JBPanel();
        panel15.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel13.add(panel15, BorderLayout.CENTER);
        transferSpeedLabel = new JBLabel();
        transferSpeedLabel.setText("");
        panel15.add(transferSpeedLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel16 = new JBPanel();
        panel16.setLayout(new BorderLayout(0, 0));
        panel12.add(panel16, BorderLayout.CENTER);
        final JBPanel panel17 = new JBPanel();
        panel17.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel17.setMaximumSize(new Dimension(100, 2147483647));
        panel17.setMinimumSize(new Dimension(100, 24));
        panel17.setPreferredSize(new Dimension(100, 24));
        panel16.add(panel17, BorderLayout.WEST);
        final JBLabel label5 = new JBLabel();
        Font label5Font = this.$$$getFont$$$(null, Font.BOLD, -1, label5.getFont());
        if (label5Font != null) label5.setFont(label5Font);
        label5.setForeground(new Color(-16722177));
        label5.setText("传输方向");
        panel17.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel18 = new JBPanel();
        panel18.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel16.add(panel18, BorderLayout.CENTER);
        directionLabel = new JBLabel();
        directionLabel.setText("");
        panel18.add(directionLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel19 = new JBPanel();
        panel19.setLayout(new BorderLayout(0, 0));
        root.add(panel19, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel20 = new JBPanel();
        panel20.setLayout(new BorderLayout(0, 0));
        panel20.setMinimumSize(new Dimension(230, 24));
        panel20.setPreferredSize(new Dimension(230, 24));
        panel19.add(panel20, BorderLayout.WEST);
        final JBPanel panel21 = new JBPanel();
        panel21.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel21.setMaximumSize(new Dimension(100, 2147483647));
        panel21.setMinimumSize(new Dimension(100, 24));
        panel21.setPreferredSize(new Dimension(100, 24));
        panel20.add(panel21, BorderLayout.WEST);
        final JBLabel label6 = new JBLabel();
        Font label6Font = this.$$$getFont$$$(null, Font.BOLD, -1, label6.getFont());
        if (label6Font != null) label6.setFont(label6Font);
        label6.setForeground(new Color(-16722177));
        label6.setText("剩余时间");
        panel21.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel22 = new JBPanel();
        panel22.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel20.add(panel22, BorderLayout.CENTER);
        timeLeftLabel = new JBLabel();
        timeLeftLabel.setText("");
        panel22.add(timeLeftLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel23 = new JBPanel();
        panel23.setLayout(new BorderLayout(0, 0));
        panel19.add(panel23, BorderLayout.CENTER);
        final JBPanel panel24 = new JBPanel();
        panel24.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel24.setMaximumSize(new Dimension(100, 2147483647));
        panel24.setMinimumSize(new Dimension(100, 24));
        panel24.setPreferredSize(new Dimension(100, 24));
        panel23.add(panel24, BorderLayout.WEST);
        final JBLabel label7 = new JBLabel();
        Font label7Font = this.$$$getFont$$$(null, Font.BOLD, -1, label7.getFont());
        if (label7Font != null) label7.setFont(label7Font);
        label7.setForeground(new Color(-16722177));
        label7.setText("传输时间");
        panel24.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel25 = new JBPanel();
        panel25.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel23.add(panel25, BorderLayout.CENTER);
        timeTransferLabel = new JBLabel();
        timeTransferLabel.setText("");
        panel25.add(timeTransferLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel26 = new JBPanel();
        panel26.setLayout(new BorderLayout(0, 0));
        root.add(panel26, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel27 = new JBPanel();
        panel27.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel27.setMaximumSize(new Dimension(100, 2147483647));
        panel27.setMinimumSize(new Dimension(100, 24));
        panel27.setPreferredSize(new Dimension(100, 24));
        panel26.add(panel27, BorderLayout.WEST);
        final JBLabel label8 = new JBLabel();
        Font label8Font = this.$$$getFont$$$(null, Font.BOLD, -1, label8.getFont());
        if (label8Font != null) label8.setFont(label8Font);
        label8.setForeground(new Color(-16722177));
        label8.setText("文件MD5");
        panel27.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel28 = new JBPanel();
        panel28.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel26.add(panel28, BorderLayout.CENTER);
        fileMD5TextField = new JBTextField();
        fileMD5TextField.setEditable(false);
        panel28.add(fileMD5TextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JBPanel panel29 = new JBPanel();
        panel29.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        root.add(panel29, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel30 = new JBPanel();
        panel30.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel29.add(panel30, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        stateButton = new JButton();
        Font stateButtonFont = this.$$$getFont$$$(null, Font.BOLD, -1, stateButton.getFont());
        if (stateButtonFont != null) stateButton.setFont(stateButtonFont);
        stateButton.setForeground(new Color(-16722177));
        stateButton.setText("");
        panel30.add(stateButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel31 = new JBPanel();
        panel31.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel29.add(panel31, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        startOverButton = new JButton();
        Font startOverButtonFont = this.$$$getFont$$$(null, Font.BOLD, -1, startOverButton.getFont());
        if (startOverButtonFont != null) startOverButton.setFont(startOverButtonFont);
        startOverButton.setForeground(new Color(-19170));
        startOverButton.setText("重新传输");
        panel31.add(startOverButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel32 = new JBPanel();
        panel32.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel29.add(panel32, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        endButton = new JButton();
        Font endButtonFont = this.$$$getFont$$$(null, Font.BOLD, -1, endButton.getFont());
        if (endButtonFont != null) endButton.setFont(endButtonFont);
        endButton.setForeground(new Color(-65536));
        endButton.setText("终止传输");
        panel32.add(endButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel33 = new JBPanel();
        panel33.setLayout(new BorderLayout(0, 0));
        root.add(panel33, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel34 = new JBPanel();
        panel34.setLayout(new BorderLayout(0, 0));
        panel34.setMaximumSize(new Dimension(280, 2147483647));
        panel34.setMinimumSize(new Dimension(280, 24));
        panel34.setPreferredSize(new Dimension(280, 24));
        panel33.add(panel34, BorderLayout.WEST);
        final JBPanel panel35 = new JBPanel();
        panel35.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel35.setMaximumSize(new Dimension(100, 2147483647));
        panel35.setMinimumSize(new Dimension(100, 24));
        panel35.setPreferredSize(new Dimension(100, 24));
        panel34.add(panel35, BorderLayout.WEST);
        final JBLabel label9 = new JBLabel();
        Font label9Font = this.$$$getFont$$$(null, Font.BOLD, -1, label9.getFont());
        if (label9Font != null) label9.setFont(label9Font);
        label9.setForeground(new Color(-16722177));
        label9.setText("服务器");
        panel35.add(label9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel36 = new JBPanel();
        panel36.setLayout(new BorderLayout(0, 0));
        panel34.add(panel36, BorderLayout.CENTER);
        serverTextField = new JBTextField();
        serverTextField.setEditable(false);
        panel36.add(serverTextField, BorderLayout.CENTER);
        final JBPanel panel37 = new JBPanel();
        panel37.setLayout(new BorderLayout(0, 0));
        panel33.add(panel37, BorderLayout.CENTER);
        final JBPanel panel38 = new JBPanel();
        panel38.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel38.setMaximumSize(new Dimension(100, 2147483647));
        panel38.setMinimumSize(new Dimension(100, 24));
        panel38.setPreferredSize(new Dimension(100, 24));
        panel37.add(panel38, BorderLayout.WEST);
        final JBLabel label10 = new JBLabel();
        Font label10Font = this.$$$getFont$$$(null, Font.BOLD, -1, label10.getFont());
        if (label10Font != null) label10.setFont(label10Font);
        label10.setForeground(new Color(-16722177));
        label10.setText("操作用户");
        panel38.add(label10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel39 = new JBPanel();
        panel39.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel37.add(panel39, BorderLayout.CENTER);
        operationTextField = new JBTextField();
        operationTextField.setEditable(false);
        panel39.add(operationTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel40 = new JBPanel();
        panel40.setLayout(new BorderLayout(0, 0));
        root.add(panel40, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel41 = new JBPanel();
        panel41.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel41.setMaximumSize(new Dimension(100, 2147483647));
        panel41.setMinimumSize(new Dimension(100, 24));
        panel41.setPreferredSize(new Dimension(100, 24));
        panel40.add(panel41, BorderLayout.WEST);
        final JBLabel label11 = new JBLabel();
        Font label11Font = this.$$$getFont$$$(null, Font.BOLD, -1, label11.getFont());
        if (label11Font != null) label11.setFont(label11Font);
        label11.setForeground(new Color(-16722177));
        label11.setText("远程目录");
        panel41.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel42 = new JBPanel();
        panel42.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel40.add(panel42, BorderLayout.CENTER);
        pathTextField = new JBTextField();
        pathTextField.setEditable(false);
        panel42.add(pathTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /** @noinspection ALL */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}
