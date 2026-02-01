package com.tanghui.dev.idea.plugin.devserver.deploy.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.deploy.dialog
 * @Author: 唐煇
 * @CreateTime: 2026-01-19-14:02
 * @Description: 服务器认证弹框。
 * @Version: v1.0
 */
public class ServerAuthenticationDialog extends DialogWrapper {
    @Getter
    private final Project project;
    @Getter
    private final JComponent root;
    @Getter
    private final ServerAuthentication serverAuthentication;
    @Getter
    @Setter
    private DevServerRunConfig parentComponent;

    public ServerAuthenticationDialog(@Nullable Project project, DevServerRunConfig parent) {
        super(true);
        setTitle(DevServerBundle.INSTANCE.message("server.config"));
        this.project = project;
        this.serverAuthentication = new ServerAuthentication(this.project, parent);
        this.root = this.serverAuthentication.$$$getRootComponent$$$();
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.root;
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
                if (serverAuthentication.isSuccessNot()) {
                    parentComponent = new DevServerRunConfig();
                    parentComponent.setServerHost(serverAuthentication.getServerTextField().getText());
                    parentComponent.setServerPort(serverAuthentication.getPortTextField().getText());
                    parentComponent.setServerUser(serverAuthentication.getUserTextField().getText());
                    if (StringUtils.isBlank(serverAuthentication.getControlsUserTextField().getText())) {
                        parentComponent.setControlsUser(serverAuthentication.getUserTextField().getText());
                    } else {
                        parentComponent.setControlsUser(serverAuthentication.getControlsUserTextField().getText());
                    }
                    parentComponent.setServerPassword(new String(serverAuthentication.getPasswordField().getPassword()).trim());
                    // 在此处添加您的代码
                    dispose();
                } else {
                    serverAuthentication.getInfoTextArea().setForeground(new JBColor(new Color(175, 0, 0), new Color(175, 0, 0)));
                    serverAuthentication.getInfoTextArea().setText(DevServerBundle.INSTANCE.message("server.config.message"));
                }
            }
        };
        ok.putValue(Action.NAME, "OK");
        return ok;
    }

}
