package com.tanghui.dev.idea.plugin.devserver.ui.dialog.connectserver;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.tanghui.dev.idea.plugin.devserver.data.model.OperateEnum;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.pool.GlobalSshPoolManager;
import com.tanghui.dev.idea.plugin.devserver.pool.SshConnectionPool;
import lombok.Getter;
import net.schmizz.sshj.SSHClient;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

import static com.tanghui.dev.idea.plugin.devserver.ui.server.RemoteServer.hostModels;
import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.saveServerHost;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.connect.ui.dialog
 * @Author: 唐煇
 * @CreateTime: 2025-12-22-08:54
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class ConnectServerDialog extends DialogWrapper {

    private final Project project;
    private final OperateEnum operateType;

    @Getter
    private final ServerHostModel serverHostModel;

    @Getter
    private JPanel root;
    @Getter
    private ConnectServer connectServer;
    @Getter
    private boolean isConnect = false;
    @Getter
    private boolean operate = false;


    public ConnectServerDialog(Project project,
                               OperateEnum operateType,
                               ServerHostModel serverHostModel) {
        super(true);
        setTitle("连接服务器");
        this.project = project;
        this.operateType = operateType;
        this.serverHostModel = serverHostModel;
        this.connectServer = new ConnectServer(project, operateType, serverHostModel);
        this.root = connectServer.getRoot();
        init();
    }


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return root;
    }

    @Override
    protected Action @NotNull [] createActions() {
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (operateType.equals(OperateEnum.DELETE)) {
                    // 删除
                    onDeleteButton();
                } else {
                    // 连接
                    onConnectButton();
                }
            }
        };
        if (operateType.equals(OperateEnum.DELETE)) {
            action.putValue(Action.NAME, "删除");
        } else {
            action.putValue(Action.NAME, "连接");
        }
        return new Action[]{
                action,
                onOK(),
                getCancelAction()
        };
    }

    private Action onOK() {
        AbstractAction ok = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Optional<ServerHostModel> first = hostModels.stream().filter(host -> {
                    String text = connectServer.getServerHost().getText();
                    return host.getHost().equals(text);
                }).findFirst();
                if (first.isPresent() && OperateEnum.ADD.equals(operateType)) {
                    Messages.showMessageDialog(project, "当前主机已经添加，请勿重复添加！现有分组: " + first.get().getServerGroupBy(), "添加主机", Messages.getWarningIcon());
                } else {
                    if (!isConnect) {
                        onConnectButton();
                    }
                }
                dispose();
            }
        };
        ok.putValue(Action.NAME, "OK");
        return ok;
    }

    /**
     * 连接服务器
     */
    private void onConnectButton() {
        String host = this.connectServer.getServerHost().getText();
        if (StringUtils.isBlank(host)) {
            Messages.showMessageDialog(project, "主机不能为空", "服务器连接校验", Messages.getErrorIcon());
            isConnect = false;
            return;
        }
        String port = this.connectServer.getServerPort().getText();
        if (StringUtils.isBlank(port)) {
            Messages.showMessageDialog(project, "端口不能为空", "服务器连接校验", Messages.getErrorIcon());
            isConnect = false;
            return;
        }
        String user = this.connectServer.getServerUser().getText();
        if (StringUtils.isBlank(user)) {
            Messages.showMessageDialog(project, "用户不能为空", "服务器连接校验", Messages.getErrorIcon());
            isConnect = false;
            return;
        }
        String password = new String(this.connectServer.getServerPassword().getPassword());
        if (StringUtils.isBlank(password)) {
            Messages.showMessageDialog(project, "密码不能为空", "服务器连接校验", Messages.getErrorIcon());
            isConnect = false;
            return;
        }
        this.connectServer.getFeedback().setText("正在连接服务器...");
        new Task.Backgroundable(project, "正在连接服务器...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                SshConnectionPool pool = GlobalSshPoolManager.getPool(
                        host,
                        Integer.parseInt(port),
                        user,
                        password
                );
                SSHClient ssh = null;
                try {
                    ssh = pool.borrow();
                    if (ssh.isConnected() && ssh.isAuthenticated()) {
                        connectServer.getFeedback().setText("连接成功！");
                        connectServer.getFeedback().updateUI();
                        serverHostModel.setHost(connectServer.getServerHost().getText());
                        serverHostModel.setPort(Integer.parseInt(port));
                        serverHostModel.setUserName(user);
                        serverHostModel.setPassword(password);
                        serverHostModel.setServerGroupBy(connectServer.getServerGroupBy().getText());
                        serverHostModel.setEnvironment(connectServer.getDescribe().getText());
                        isConnect = true;
                    } else {
                        connectServer.getFeedback().setText("连接失败！");
                        isConnect = false;
                    }
                } catch (Exception e) {
                    connectServer.getFeedback().setText("连接失败！");
                    isConnect = false;
                } finally {
                    pool.release(ssh);
                }
            }
        }.queue();
    }

    private void onDeleteButton(){
        String host = this.connectServer.getServerHost().getText();
        if (StringUtils.isBlank(host)) {
            Messages.showMessageDialog(project, "主机不能为空", "服务器连接校验", Messages.getErrorIcon());
            this.operate = false;
            return;
        }
        Optional<ServerHostModel> serverHostModel = hostModels.stream().filter(v -> host.equals(v.getHost())).findFirst();
        if (serverHostModel.isPresent()) {
            hostModels.remove(serverHostModel.get());
            saveServerHost(hostModels);
            this.operate = true;
            dispose();
        }
    }
}
