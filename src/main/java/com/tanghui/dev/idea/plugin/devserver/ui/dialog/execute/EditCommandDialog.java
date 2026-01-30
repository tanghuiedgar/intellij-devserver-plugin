package com.tanghui.dev.idea.plugin.devserver.ui.dialog.execute;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.tanghui.dev.idea.plugin.devserver.data.model.OperateEnum;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.tree.ServerHostTreeNode;
import com.tanghui.dev.idea.plugin.devserver.ui.server.RemoteServer;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.util.Optional;

import static com.tanghui.dev.idea.plugin.devserver.ui.server.RemoteServer.hostModels;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.connect.ui.dialog
 * @Author: 唐煇
 * @CreateTime: 2026-01-20-16:51
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class EditCommandDialog extends DialogWrapper {

    @Getter
    private final EditCommand editCommand;

    private final RemoteServer command;
    private final Project project;
    private final OperateEnum type;

    public EditCommandDialog(@Nullable Project project, RemoteServer command, OperateEnum type) {
        super(project, true);
        this.command = command;
        this.project = project;
        this.type = type;
        this.editCommand = new EditCommand(project, command, type);
        if (OperateEnum.ADD.equals(type)) {
            setTitle("新增命令");
        } else if (OperateEnum.UPDATE.equals(type)) {
            setTitle("修改命令");
        } else if (OperateEnum.DELETE.equals(type)) {
            setTitle("删除命令");
        }
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.editCommand.$$$getRootComponent$$$();
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
                if (StringUtils.isEmpty(editCommand.getTitleTextField().getText().trim())) {
                    Messages.showMessageDialog(project, "命令标题不能为空！", "编辑命令", Messages.getErrorIcon());
                    return;
                }
                if (StringUtils.isEmpty(editCommand.getUserTextField().getText().trim())) {
                    Messages.showMessageDialog(project, "执行用户不能为空！", "编辑命令", Messages.getErrorIcon());
                    return;
                }
                if (StringUtils.isEmpty(editCommand.getDirectoryTextField().getText().trim())) {
                    Messages.showMessageDialog(project, "执行目录不能为空！", "编辑命令", Messages.getErrorIcon());
                    return;
                }
                String hostName = command.getSelectedNodeName(command.getLastLeft().getServerTree());
                Optional<ServerHostModel> hostModelOpt = hostModels.stream()
                        .filter(v -> hostName.equals(v.getHost()))
                        .findFirst();
                if (hostModelOpt.isEmpty()) {
                    return;
                }
                ServerHostModel serverHostModel = hostModelOpt.get();
                String commandJsonStr = serverHostModel.getCommand();
                JSONArray objects = StringUtils.isNotBlank(commandJsonStr) ? JSON.parseArray(commandJsonStr) : new JSONArray();

                if (OperateEnum.ADD.equals(type)) {
                    JSONObject json = new JSONObject();
                    json.put("title", editCommand.getTitleTextField().getText().trim());
                    json.put("directory", editCommand.getDirectoryTextField().getText().trim());
                    json.put("user", editCommand.getUserTextField().getText().trim());
                    json.put("path", editCommand.getServerGroupBy().getText().trim());
                    objects.add(json);
                    serverHostModel.setCommand(objects.toJSONString());
                    command.initCommandTree(command.getLastLeft().getServerTree(), json);
                } else {
                    JSONObject selectedCommandJson = command.getSelectedCommandJson(command.getLastLeft().getServerTree(), command.getLastRight().getExecuteLeft().getCommandTree());
                    if (selectedCommandJson == null) {
                        return;
                    }
                    String title = selectedCommandJson.getString("title");
                    if (OperateEnum.UPDATE.equals(type)) {
                        objects.stream()
                                .filter(v -> {
                                    JSONObject jsonObject = JSON.parseObject(v.toString());
                                    return jsonObject.getString("title").equals(title);
                                }).findFirst()
                                .ifPresent(v -> {
                                    JSONObject json = (JSONObject) v;
                                    json.put("title", editCommand.getTitleTextField().getText().trim());
                                    json.put("directory", editCommand.getDirectoryTextField().getText().trim());
                                    json.put("user", editCommand.getUserTextField().getText().trim());
                                    json.put("path", editCommand.getServerGroupBy().getText().trim());
                                    serverHostModel.setCommand(objects.toJSONString());
                                    command.initCommandTree(command.getLastLeft().getServerTree(), json);
                                });
                    } else if (OperateEnum.DELETE.equals(type)) {
                        objects.stream().filter(v -> {
                            JSONObject jsonObject = (JSONObject) v;
                            return jsonObject.getString("title").equals(title);
                        }).findFirst().ifPresent(objects::remove);
                        serverHostModel.setCommand(objects.toJSONString());
                        command.initCommandTree(command.getLastLeft().getServerTree(), null);
                    }
                }
                command.initCommandInfo(command.getLastLeft().getServerTree(), command.getLastRight().getExecuteLeft().getCommandTree());
                dispose();
            }
        };
        ok.putValue(Action.NAME, "OK");
        return ok;
    }
}
