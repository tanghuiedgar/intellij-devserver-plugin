package com.tanghui.dev.idea.plugin.devserver.deploy.run;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.keyFMap.KeyFMap;
import com.intellij.util.ui.JBUI;
import com.tanghui.dev.idea.plugin.devserver.data.model.FileTransferModel;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.deploy.console.CustomExecutionConsole;
import com.tanghui.dev.idea.plugin.devserver.pool.GlobalSshPoolManager;
import com.tanghui.dev.idea.plugin.devserver.pool.SshConnectionPool;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import com.tanghui.dev.idea.plugin.devserver.task.FileTransferCallback;
import com.tanghui.dev.idea.plugin.devserver.transfer.ui.FileTransfer;
import com.tanghui.dev.idea.plugin.devserver.transfer.ui.MultipleFileTransfer;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.execStdout;

/**
 * @BelongsPackage: com.tanghui.run
 * @Author: 唐煇
 * @CreateTime: 2024-07-19 09:02
 * @Description: 描述类的主要功能和用途。
 * @Version: 1.0
 */
public class DevServerRunProfileState implements RunProfileState {

    private final DevServerRunConfiguration configuration;
    private final Project project;

    private DevServerRunProcessHandler myProcessHandler;

    public DevServerRunProfileState(DevServerRunConfiguration configuration, Project project) {
        this.configuration = configuration;
        this.project = project;
    }

    @Override
    public @Nullable ExecutionResult execute(Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        CustomExecutionConsole customExecutionConsole = new CustomExecutionConsole();

        DevServerRunConsoleView myConsoleViewHead = new DevServerRunConsoleView(project, new DevServerRunConfig());
        this.myProcessHandler = new DevServerRunProcessHandler();

        myConsoleViewHead.getConsoleView().attachToProcess(myProcessHandler);
        String upgradeType = StringUtils.isNotBlank(configuration.getUpgradeType()) ? configuration.getUpgradeType() : "upgrade";
        if (!"execute".equals(upgradeType)) {
            // 升级或者上传文件
            String uploadFilePath = configuration.getUploadFile();
            myConsoleViewHead.print("上传文件路径: \033[1;34m" + uploadFilePath + "\033[0m", ConsoleViewContentType.NORMAL_OUTPUT);
            /*Path filePath = Paths.get(uploadFilePath);
            long fileSizeInBytes;
            try {
                fileSizeInBytes = Files.size(filePath);
                myConsoleViewHead.print("文件大小: \033[1;33m" + fileSizeInBytes + "\033[0m bytes", ConsoleViewContentType.NORMAL_OUTPUT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }*/
            // 计算文件md5值
            try {
                // 创建一个文件对象
                File file = new File(uploadFilePath);
                // 计算文件的 MD5 值
                // String checksum = FileUtil.getInstance().getFileChecksum(file);
                // 打印结果
                // myConsoleViewHead.print("文件md5值: \033[1;31m" + checksum + "\033[0m", ConsoleViewContentType.NORMAL_OUTPUT);
                // 获取创建时间
                // 根据文件的绝对路径获取Path
                Path path = Paths.get(file.getAbsolutePath());
                // 根据path获取文件的基本属性类
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                // 从基本属性类中获取文件创建时间
                FileTime fileTime = attrs.creationTime();
                // 将文件创建时间转成毫秒
                long millis = fileTime.toMillis();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date();
                date.setTime(millis);
                // 毫秒转成时间字符串
                String time = dateFormat.format(date);
                myConsoleViewHead.print("文件创建时间: \033[1;31m" + time + "\033[0m", ConsoleViewContentType.NORMAL_OUTPUT);
            } catch (IOException ignored) {
            }
            SwingUtilities.invokeLater(() -> {
                List<DevServerRunConfig> devServerRunConfigList = this.configuration.getRunConfigList();
                if (CollectionUtils.isEmpty(devServerRunConfigList)) {
                    Messages.showMessageDialog(
                            "服务器信息不能为空！",
                            "Host",
                            Messages.getErrorIcon()
                    );
                    return;
                }
                List<FileTransfer> fileTransferList = new ArrayList<>();
                JTabbedPane uploadTabbedPane = new JBTabbedPane();
                for (DevServerRunConfig devServerRunConfig : devServerRunConfigList) {
                    // 校验运行配置信息
                    if (!validateRunConfig(devServerRunConfig, configuration)) {
                        return;
                    }
                    DevServerRunConsoleView myConsoleView = new DevServerRunConsoleView(project, devServerRunConfig);
                    myConsoleView.getConsoleView().attachToProcess(myProcessHandler);
                    JButton logButton = new JButton();
                    logButton.setText("查看日志");
                    logButton.setPreferredSize(JBUI.size(80, 30));
                    logButton.setForeground(new JBColor(new Color(19, 218, 5), new Color(19, 218, 5)));
                    logButton.setBorder(null);
                    JButton rollbackButton = new JButton();
                    rollbackButton.setText("版本回退");
                    rollbackButton.setPreferredSize(JBUI.size(80, 30));
                    rollbackButton.setForeground(new JBColor(new Color(255, 181, 30), new Color(255, 181, 30)));
                    rollbackButton.setBorder(null);
                    myConsoleView.setLogButton(logButton);
                    myConsoleView.setRollbackButton(rollbackButton);
                    logButton.setEnabled(false);
                    rollbackButton.setEnabled(false);
                    ServerHostModel serverHost = new ServerHostModel();
                    serverHost.setHost(devServerRunConfig.getServerHost());
                    serverHost.setPort(Integer.parseInt(devServerRunConfig.getServerPort()));
                    serverHost.setUserName(devServerRunConfig.getServerUser());
                    serverHost.setPassword(devServerRunConfig.getServerPassword());
                    serverHost.setPath(devServerRunConfig.getTargetDirectory());
                    FileTransferModel fileTransferModel = new FileTransferModel();
                    File file = new File(configuration.getUploadFile());
                    String fileName = file.getName();
                    fileTransferModel.setLocalFilesPath(file.getParent());
                    fileTransferModel.setLocalFilesName(fileName);
                    fileTransferModel.setRemoteFilesPath(devServerRunConfig.getTargetDirectory());
                    fileTransferModel.setRemoteFilesName(fileName);
                    fileTransferModel.setState(Boolean.TRUE);
                    fileTransferModel.setOffset(0L);
                    FileTransferCallback fileTransferCallback = new FileTransferCallback() {
                        /**
                         * 文件上传或下载结束之后调用此方法
                         * */
                        @Override
                        public void callback() {
                            // 执行命令
                            ApplicationManager.getApplication().invokeAndWait(() -> {
                                Task.Backgroundable task = new Task.Backgroundable(project, "执行后续命令") {
                                    @Override
                                    public void run(@NotNull ProgressIndicator progressIndicator) {
                                        progressIndicator.setIndeterminate(true);
                                        printToConsole(myConsoleView, "\033[1;34m后续操作(开始)\033[0m", ConsoleViewContentType.NORMAL_OUTPUT);
                                        // 执行后续命令
                                        // 创建连接
                                        SSHClient ssh = null;
                                        Session session = null;
                                        Session.Shell shell = null;
                                        Session shellSession = null;
                                        try {
                                            ssh = new SSHClient();
                                            ssh.addHostKeyVerifier(new PromiscuousVerifier());
                                            ssh.connect(devServerRunConfig.getServerHost(), Integer.valueOf(devServerRunConfig.getServerPort()));
                                            ssh.authPassword(devServerRunConfig.getServerUser(), devServerRunConfig.getServerPassword());
                                            session = ssh.startSession();
                                            // 工作目录
                                            String targetDirectory = devServerRunConfig.getTargetDirectory();
                                            // 获取服务器文件md5值
                                            String md5File = execStdout(session, "echo -e \"服务器文件md5值: \\033[1;31m$(md5sum " + targetDirectory + "/" + fileName + "|cut -d ' ' -f1)\\033[0m\"");
                                            myConsoleView.print(md5File, ConsoleViewContentType.NORMAL_OUTPUT);
                                            shellSession = ssh.startSession();
                                            shell = shellSession.startShell();
                                            OutputStream outputStream = shell.getOutputStream();
                                            // 获取操作用户
                                            String controlsUser = devServerRunConfig.getControlsUser().trim();
                                            // 连接用户
                                            String user = devServerRunConfig.getServerUser().trim();
                                            if (StringUtils.isNotBlank(controlsUser) && !controlsUser.equals(user)) {
                                                outputStream.write(("chown " + controlsUser + ":" + controlsUser + " " + targetDirectory + "/" + fileName + "\n").getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                                outputStream.write(("su - " + controlsUser + "\n").getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                            }
                                            outputStream.write(("cd " + targetDirectory + "\n").getBytes(StandardCharsets.UTF_8));
                                            outputStream.flush();
                                            outputStream.write(("ll ./\n").getBytes(StandardCharsets.UTF_8));
                                            outputStream.flush();
                                            /*String md5Sum = "echo -e \"服务器文件md5值: \\033[1;31m$(md5sum " + fileName + "|cut -d ' ' -f1)\\033[0m\"\n";
                                            outputStream.write((md5Sum).getBytes(StandardCharsets.UTF_8));
                                            outputStream.flush();*/
                                            String uuid = UUID.randomUUID().toString().replace("-", "");
                                            if ("true".equals(devServerRunConfig.getShellScriptOnOff())) {
                                                // 执行脚本，上传脚本文件
                                                String command = devServerRunConfig.getShellScript();
                                                InputStream inputStream = new ByteArrayInputStream(command.getBytes(StandardCharsets.UTF_8));
                                                // Java
                                                Path tmp = Files.createTempFile("upload-", ".sh");
                                                try (OutputStream os = Files.newOutputStream(tmp)) {
                                                    inputStream.transferTo(os);
                                                }
                                                try (SFTPClient sftp = ssh.newSFTPClient()) {
                                                    String remoteDir = targetDirectory.substring(0, targetDirectory.lastIndexOf('/'));
                                                    sftp.mkdirs(remoteDir);                // 确保目录存在
                                                    sftp.put(tmp.toString(), targetDirectory + "/" + uuid + ".sh");  // 上传
                                                }
                                                // finally 删除临时文
                                                Files.deleteIfExists(tmp);
                                                if (StringUtils.isNotBlank(controlsUser) && !controlsUser.equals(user)) {
                                                    outputStream.write((("exit" + "\n")).getBytes(StandardCharsets.UTF_8));
                                                    outputStream.flush();
                                                }
                                                outputStream.write((("cd " + targetDirectory + "\n")).getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                                outputStream.write((("chown " + controlsUser + ":" + controlsUser + " " + targetDirectory + "/" + uuid + ".sh" + "\n")).getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                                outputStream.write((("su - " + controlsUser + "\n")).getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                                outputStream.write((("cd " + targetDirectory + "\n")).getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                                outputStream.write((("chmod +x " + uuid + ".sh" + "\n")).getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                                outputStream.write((("./" + uuid + ".sh" + "\n")).getBytes(StandardCharsets.UTF_8));
                                                outputStream.flush();
                                            } else {
                                                // 执行命令
                                                if (StringUtils.isNotBlank(devServerRunConfig.getShellScript())) {
                                                    String[] lines = devServerRunConfig.getShellScript().split("\\n");
                                                    for (String line : lines) {
                                                        if (StringUtils.isNotBlank(line)) {
                                                            outputStream.write(((line + "\n")).getBytes(StandardCharsets.UTF_8));
                                                            outputStream.flush();
                                                        }
                                                    }
                                                }
                                            }
                                            outputStream.write((("echo \"exit\"\n")).getBytes(StandardCharsets.UTF_8));
                                            outputStream.flush();
                                            InputStream inputStream = shell.getInputStream();
                                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), 81920);
                                            ExecutorService executorService = Executors.newSingleThreadExecutor();
                                            while (true) {
                                                Future<String> future = executorService.submit(reader::readLine);
                                                try {
                                                    String line = future.get(20, TimeUnit.SECONDS);  // 设置超时时间
                                                    if (line != null) {
                                                        printToConsole(myConsoleView, line, ConsoleViewContentType.NORMAL_OUTPUT);
                                                        if ("exit".equalsIgnoreCase(line.trim())) {
                                                            break;
                                                        }
                                                        if (!shell.isOpen() || !shellSession.isOpen()) {
                                                            break;
                                                        }
                                                    }
                                                } catch (TimeoutException e) {
                                                    future.cancel(true);  // 超时后取消任务
                                                    break;
                                                } catch (InterruptedException |
                                                         java.util.concurrent.ExecutionException e) {
                                                    break;
                                                }
                                            }
                                            executorService.shutdown();
                                            if ("true".equals(devServerRunConfig.getShellScriptOnOff())) {
                                                // 删除远程临时脚本
                                                SFTPClient sftpClient = ssh.newSFTPClient();
                                                sftpClient.rm(devServerRunConfig.getTargetDirectory() + "/" + uuid + ".sh");
                                                sftpClient.close();
                                            }
                                        } catch (IOException ignored) {
                                        } finally {
                                            if (shell != null) try {
                                                shell.close();
                                            } catch (Exception ignored) {
                                            }
                                            if (session != null) try {
                                                session.close();
                                            } catch (Exception ignored) {
                                            }
                                            if (shellSession != null) try {
                                                shellSession.close();
                                            } catch (Exception ignored) {
                                            }
                                            if (ssh != null) try {
                                                ssh.close();
                                            } catch (Exception ignored) {
                                            }
                                        }

                                        printToConsole(myConsoleView, "", ConsoleViewContentType.NORMAL_OUTPUT);
                                        printToConsole(myConsoleView, "\033[1;34m后续操作(结束)\033[0m", ConsoleViewContentType.NORMAL_OUTPUT);

                                        // 后续命令执行完成之后才能进行后续操作
                                        logButton.setEnabled(true);
                                        rollbackButton.setEnabled(true);

                                        // 结束运行状态
                                        long counted = fileTransferList.stream()
                                                .filter(v -> !v.isFinish())
                                                .count();
                                        if (counted <= 0) {
                                            myProcessHandler.endExecution();
                                            myProcessHandler.destroyProcess();
                                        }

                                        progressIndicator.setIndeterminate(true);
                                    }
                                };
                                ProgressManager.getInstance().run(task);
                            });
                        }

                        /**
                         * 终止传输回调方法
                         * */
                        @Override
                        public void stopTransfer() {
                            printToConsole(myConsoleView, "", ConsoleViewContentType.NORMAL_OUTPUT);
                            printToConsole(myConsoleView, devServerRunConfig.getServerHost() + "  " + "\033[1;31m本次升级已经终止\033[0m", ConsoleViewContentType.NORMAL_OUTPUT);
                            printToConsole(myConsoleView, "", ConsoleViewContentType.NORMAL_OUTPUT);
                            // 结束运行状态
                            long counted = fileTransferList.stream()
                                    .filter(v -> !v.isFinish())
                                    .count();
                            if (counted <= 0) {
                                myProcessHandler.endExecution();
                                myProcessHandler.destroyProcess();
                            }
                        }
                    };
                    FileTransfer devServerUp = new FileTransfer(project, serverHost, fileTransferCallback, fileTransferModel);
                    fileTransferList.add(devServerUp);
                    customExecutionConsole.print(myConsoleView);
                }
                customExecutionConsole.addComponent(new MultipleFileTransfer(project, fileTransferList));
                this.myProcessHandler.addProcessListener(new ProcessListener() {
                    @Override
                    public void startNotified(@NotNull ProcessEvent event) {
                        // 进程开始
                        // System.out.println("Run开始");
                    }

                    @Override
                    public void processTerminated(@NotNull ProcessEvent event) {
                        // 进程结束（被停止或者自然完成）
                        // System.out.println("Run结束，退出码：" + event.getExitCode());
                        int exitCode = event.getExitCode();
                        if (exitCode == 130) {
                            for (FileTransfer fileTransfer : fileTransferList) {
                                fileTransfer.closeConnection();
                                // 禁用按钮
                                fileTransfer.getStateButton().setEnabled(false);
                                fileTransfer.getStartOverButton().setEnabled(false);
                                fileTransfer.getEndButton().setEnabled(false);
                                fileTransfer.getTimeLeftLabel().setText("已终止");
                                fileTransfer.getTimeLeftLabel().setFont(JBUI.Fonts.label());
                                fileTransfer.getTimeLeftLabel().setForeground(new JBColor(new Color(205, 5, 5), new Color(205, 5, 5)));
                            }
                        }
                    }

                    @Override
                    public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                        // 进程即将终止
                    }

                    @Override
                    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                        // 输出日志，可选
                        // System.out.println(event.getText());
                    }
                });
            });
        } else {
            // 执行命令，不上传文件
            SwingUtilities.invokeLater(() -> {
                List<DevServerRunConfig> devServerRunConfigList = this.configuration.getRunConfigList();
                if (CollectionUtils.isEmpty(devServerRunConfigList)) {
                    Messages.showMessageDialog(
                            "服务器信息不能为空！",
                            "Host",
                            Messages.getErrorIcon()
                    );
                    return;
                }

                for (DevServerRunConfig devServerRunConfig : devServerRunConfigList) {
                    // 校验运行配置信息
                    if (StringUtils.isBlank(devServerRunConfig.getServerHost().trim())) {
                        Messages.showMessageDialog(
                                "服务器ip不能为空！",
                                "Host",
                                Messages.getErrorIcon()
                        );
                        return;
                    }
                    if (StringUtils.isBlank(devServerRunConfig.getServerPort().trim())) {
                        Messages.showMessageDialog(
                                "服务器端口不能为空！",
                                "Host",
                                Messages.getErrorIcon()
                        );
                        return;
                    }
                    if (StringUtils.isBlank(devServerRunConfig.getServerUser().trim())) {
                        Messages.showMessageDialog(
                                "服务器用户名不能为空！",
                                "Host",
                                Messages.getErrorIcon()
                        );
                        return;
                    }
                    if (StringUtils.isBlank(devServerRunConfig.getServerPassword().trim())) {
                        Messages.showMessageDialog(
                                "服务器密码不能为空！",
                                "Host",
                                Messages.getErrorIcon()
                        );
                        return;
                    }
                    if (StringUtils.isBlank(devServerRunConfig.getShellScript())) {
                        Messages.showMessageDialog(
                                "运行脚本不能为空！",
                                "Host",
                                Messages.getErrorIcon()
                        );
                        return;
                    }

                    DevServerRunConsoleView myConsoleView = new DevServerRunConsoleView(project, devServerRunConfig);
                    myConsoleView.getConsoleView().attachToProcess(myProcessHandler);

                    JButton logButton = new JButton();
                    logButton.setText("查看日志");
                    logButton.setPreferredSize(JBUI.size(80, 30));
                    JButton rollbackButton = new JButton();
                    rollbackButton.setText("版本回退");
                    rollbackButton.setPreferredSize(JBUI.size(80, 30));
                    myConsoleView.setLogButton(logButton);
                    myConsoleView.setRollbackButton(rollbackButton);
                    logButton.setEnabled(false);
                    rollbackButton.setEnabled(false);

                    SshConnectionPool pool = GlobalSshPoolManager.getPool(
                            devServerRunConfig.getServerHost().trim(),
                            Integer.parseInt(devServerRunConfig.getServerPort().trim()),
                            devServerRunConfig.getServerUser().trim(),
                            devServerRunConfig.getServerPassword().trim()
                    );
                    SSHClient ssh = null;
                    try {
                        ssh = pool.borrow();
                        printToConsole(myConsoleView, "执行命令开始", ConsoleViewContentType.NORMAL_OUTPUT);
                        try (Session session = ssh.startSession();
                             Session.Shell shell = session.startShell()) {
                            OutputStream outputStream = shell.getOutputStream();
                            // 执行初始化命令
                            // 获取操作用户
                            String controlsUser = devServerRunConfig.getControlsUser().trim();
                            String user = devServerRunConfig.getServerUser().trim();
                            if (StringUtils.isNotBlank(controlsUser) && !controlsUser.equals(user)) {
                                outputStream.write(("su - " + controlsUser + "\n").getBytes(StandardCharsets.UTF_8));
                                outputStream.flush();
                            }
                            outputStream.write(("ll ./\n").getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                            if (StringUtils.isNotBlank(devServerRunConfig.getShellScript())) {
                                String[] lines = devServerRunConfig.getShellScript().split("\\n");
                                for (String line : lines) {
                                    if (StringUtils.isNotBlank(line)) {
                                        outputStream.write(((line + "\n")).getBytes(StandardCharsets.UTF_8));
                                        outputStream.flush();
                                    }
                                }
                            }
                            outputStream.write((("echo \"exit\"\n")).getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                            InputStream inputStream = shell.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), 81920);
                            ExecutorService executorService = Executors.newSingleThreadExecutor();
                            while (true) {
                                Future<String> future = executorService.submit(reader::readLine);
                                try {
                                    String line = future.get(20, TimeUnit.SECONDS);  // 设置超时时间
                                    if (line != null) {
                                        printToConsole(myConsoleView, line, ConsoleViewContentType.NORMAL_OUTPUT);
                                        if ("exit".equalsIgnoreCase(line.trim())) {
                                            break;
                                        }
                                    }
                                } catch (TimeoutException e) {
                                    future.cancel(true);  // 超时后取消任务
                                    break;
                                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                                    break;
                                }
                            }
                            executorService.shutdown();
                        }
                        printToConsole(myConsoleView, "执行命令结束", ConsoleViewContentType.NORMAL_OUTPUT);
                        // 后续命令执行完成之后才能点击操作按钮
                        logButton.setEnabled(true);
                        rollbackButton.setEnabled(true);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        pool.release(ssh);
                    }
                    customExecutionConsole.print(myConsoleView);
                }
                this.myProcessHandler.endExecution();
                this.myProcessHandler.destroyProcess();
            });
        }
        customExecutionConsole.printHead(myConsoleViewHead.getConsoleView());
        return new DefaultExecutionResult(customExecutionConsole, myProcessHandler);
    }

    private void printToConsole(DevServerRunConsoleView myConsoleView, String message, @NotNull ConsoleViewContentType contentType) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            myConsoleView.print(message, contentType);
        });
    }

    private boolean validateRunConfig(DevServerRunConfig devServerRunConfig, DevServerRunConfiguration configuration) {
        if (isBlankAndAlert(devServerRunConfig.getServerHost(), "服务器ip不能为空！")) return false;
        if (isBlankAndAlert(devServerRunConfig.getServerPort(), "服务器端口不能为空！")) return false;
        if (isBlankAndAlert(devServerRunConfig.getServerUser(), "服务器用户名不能为空！")) return false;
        if (isBlankAndAlert(devServerRunConfig.getServerPassword(), "服务器密码不能为空！")) return false;
        if (isBlankAndAlert(devServerRunConfig.getTargetDirectory(), "上传服务器路径不能为空！")) return false;
        return !isBlankAndAlert(configuration.getUploadFile(), "本地待上传文件路径不能为空！");
    }

    /**
     * 通用字符串判空并弹出提示
     */
    private boolean isBlankAndAlert(String value, String message) {
        if (StringUtils.isBlank(value) || StringUtils.isBlank(value.trim())) {
            Messages.showMessageDialog(
                    message,
                    "Host",
                    Messages.getErrorIcon()
            );
            return true;
        }
        return false;
    }

}
