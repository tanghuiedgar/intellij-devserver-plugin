package com.tanghui.dev.idea.plugin.devserver.deploy.run;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.pool.GlobalSshPoolManager;
import com.tanghui.dev.idea.plugin.devserver.pool.SshConnectionPool;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import com.tanghui.dev.idea.plugin.devserver.utils.TerminalUtil;
import lombok.Getter;
import lombok.Setter;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.fileExists;


/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.deploy.run
 * @Author: 唐煇
 * @CreateTime: 2024-07-19 10:34
 * @Description: 描述类的主要功能和用途。
 * @Version: 1.0
 */
public class DevServerRunConsoleView {
    @Getter
    private final ConsoleView consoleView;

    @Getter
    private final DevServerRunConfig configuration;

    private final AnsiEscapeDecoder ansiEscapeDecoder = new AnsiEscapeDecoder();

    @Getter
    private final Project project;

    // 查看日志
    @Getter
    @Setter
    private JButton logButton;

    // 版本回退
    @Getter
    @Setter
    private JButton rollbackButton;

    // 版本是否已经回退，默认没有，防止多次回退
    @Getter
    private Boolean rollback = false;

    public DevServerRunConsoleView(Project project, DevServerRunConfig configuration) {
        consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        this.configuration = configuration;
        this.project = project;
    }

    public void print(@NotNull String message, @NotNull ConsoleViewContentType contentType) {
        // ANSI 转义打印文本信息
        ansiEscapeDecoder.escapeText(message + "\n", ProcessOutputTypes.STDOUT, (text, processOutputType) -> {
            // printToConsole(text, ConsoleViewContentType.getConsoleViewType(processOutputType));
            consoleView.print(text, ConsoleViewContentType.getConsoleViewType(processOutputType));
        });
    }

    public void clear() {
        consoleView.clear();
    }

    public void dispose() {
        Disposer.dispose(consoleView);
    }

    public void printLog(String host, String port, String user, String password) {
        List<String> executeCommand = new ArrayList<>();
        executeCommand.add(configuration.getLogeText());
        ServerHostModel hostModel = new ServerHostModel();
        hostModel.setHost(host);
        hostModel.setPort(Integer.parseInt(port));
        hostModel.setUserName(user);
        hostModel.setPassword(password);
        TerminalUtil.createRemoteConnectionNewTerminal(project, hostModel, executeCommand);
    }

    public void versionRollback(String host, String port, String user, String controlsUser, String password) {
        // 版本已经回退
        this.rollback = true;
        if (rollbackButton != null) rollbackButton.setEnabled(false);
        ApplicationManager.getApplication().invokeLater(() -> {
            Task.Backgroundable task = new Task.Backgroundable(project, DevServerBundle.INSTANCE.message("version.rollback.title")) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    progressIndicator.setIndeterminate(true);
                    SshConnectionPool pool = GlobalSshPoolManager.getPool(
                            host,
                            Integer.parseInt(port),
                            user,
                            password
                    );
                    SSHClient ssh = null;
                    print("", ConsoleViewContentType.NORMAL_OUTPUT);
                    print("\033[1;34m" + DevServerBundle.INSTANCE.message("version.rollback.title.start") + "\033[0m", ConsoleViewContentType.NORMAL_OUTPUT);
                    try {
                        // 创建会话并连接
                        ssh = pool.borrow();
                        try (Session session = ssh.startSession();
                             Session.Shell shell = session.startShell()) {
                            OutputStream outputStream = shell.getOutputStream();
                            int count;
                            if (StringUtils.isNotBlank(controlsUser) && !controlsUser.equals(user)) {
                                outputStream.write(("su - " + controlsUser + "\n").getBytes(StandardCharsets.UTF_8));
                                outputStream.flush();
                                count = 2;
                            } else {
                                count = 1;
                            }
                            String uuid = UUID.randomUUID().toString().replace("-", "");
                            File tempFile = FileUtil.createTempFile(uuid, ".sh", true);
                            if ("true".equals(configuration.getRollbackScriptOnOff())) {
                                // 执行脚本
                                try {
                                    try (SFTPClient sftpClient = ssh.newSFTPClient()) {
                                        // 创建临时文件
                                        String command = configuration.getRollbackScript();
                                        FileUtil.writeToFile(tempFile, command);
                                        sftpClient.put(tempFile.getAbsolutePath(), configuration.getTargetDirectory() + "/" + uuid + ".sh");
                                    }
                                    if (StringUtils.isNotBlank(controlsUser) && !controlsUser.equals(user)) {
                                        outputStream.write((("exit" + "\n")).getBytes(StandardCharsets.UTF_8));
                                        outputStream.flush();
                                    }
                                    outputStream.write((("cd " + configuration.getTargetDirectory() + "\n")).getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    outputStream.write((("chown " + controlsUser + ":" + controlsUser + " " + configuration.getTargetDirectory() + "/" + uuid + ".sh" + "\n")).getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    outputStream.write((("su - " + controlsUser + "\n")).getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    outputStream.write((("cd " + configuration.getTargetDirectory() + "\n")).getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    outputStream.write((("chmod +x " + uuid + ".sh" + "\n")).getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                    outputStream.write((("./" + uuid + ".sh" + "\n")).getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                } catch (IOException ignored) {
                                }
                            } else {
                                if (StringUtils.isNotBlank(configuration.getRollbackScript())) {
                                    String[] lines = configuration.getRollbackScript().split("\\n");
                                    for (String line : lines) {
                                        if (StringUtils.isNotBlank(line)) {
                                            outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                                            outputStream.flush();
                                        }
                                    }
                                }
                            }
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
                                        if (count <= 0) {
                                            // ANSI 转义打印文本信息
                                            print(line, ConsoleViewContentType.NORMAL_OUTPUT);
                                            if ("exit".equalsIgnoreCase(line.trim())) {
                                                break;
                                            }
                                        }
                                    }
                                    count--;
                                } catch (TimeoutException e) {
                                    future.cancel(true);  // 超时后取消任务
                                    break;
                                } catch (InterruptedException | ExecutionException e) {
                                    break;
                                }
                            }
                            executorService.shutdown();
                            if ("true".equals(configuration.getRollbackScriptOnOff())) {
                                // 删除临时脚本
                                try (SFTPClient sftpClient = ssh.newSFTPClient()) {
                                    if (fileExists(ssh, configuration.getTargetDirectory() + "/" + uuid + ".sh")) {
                                        // 第一次上传文件时删除已经存在的文件
                                        sftpClient.rm(configuration.getTargetDirectory() + "/" + uuid + ".sh");
                                    }
                                }
                                if (tempFile.exists()) {
                                    tempFile.delete();
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        pool.release(ssh);
                    }
                    print("\033[1;34m" + DevServerBundle.INSTANCE.message("version.rollback.title.finish") + "\033[0m", ConsoleViewContentType.NORMAL_OUTPUT);
                    progressIndicator.setIndeterminate(false);
                }
            };
            ProgressManager.getInstance().run(task);
        });

    }
}
