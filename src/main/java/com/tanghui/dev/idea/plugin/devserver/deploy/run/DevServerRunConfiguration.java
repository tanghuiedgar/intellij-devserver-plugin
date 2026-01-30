package com.tanghui.dev.idea.plugin.devserver.deploy.run;

import com.intellij.configurationStore.XmlSerializer;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsPackage: com.tanghui.run
 * @Author: 唐煇
 * @CreateTime: 2024-07-19 09:00
 * @Description: 描述类的主要功能和用途。
 * @Version: 1.0
 */

public class DevServerRunConfiguration extends RunConfigurationBase<DevServerRunProfileState> {
    private final Project project;

    @Getter
    private DevServerSettingsEditor devServerSettingsEditor;

    /**
     * 升级类型 升级 上传文件 执行命令
     */
    @Getter
    @Setter
    private String upgradeType;

    /**
     * 待上传文件路径
     */
    @Getter
    @Setter
    private String uploadFile;

    /**
     * 服务器配置
     */
    public final DevServerRunOptions options = new DevServerRunOptions();

    protected DevServerRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "DevServerRunConfiguration");
        this.project = project;
        this.devServerSettingsEditor = new DevServerSettingsEditor(project, this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
            throws ExecutionException {
        return new DevServerRunProfileState(this, this.project);
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return this.devServerSettingsEditor;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        List<BeforeRunTask<?>> beforeRunTasks = this.getBeforeRunTasks();
        beforeRunTasks.forEach(v -> {

        });
    }

    @Override
    public @Nullable @NonNls String getId() {
        return "DevServerConfigurationType";
    }

    /**
     * 读取配置
     */
    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);
        // 读取配置并设置
        this.setUploadFile(JDOMExternalizerUtil.readField(element, "uploadFile"));
        this.setUpgradeType(JDOMExternalizerUtil.readField(element, "upgradeType"));

        try {
            XmlSerializer.deserializeInto(element, options);
        } catch (Exception ignored) {
            this.getDevServerSettingsEditor().getDevServerRunUI().setRunConfigList(new ArrayList<>(new ArrayList<>()));
        }
    }

    /**
     * 写入配置
     */
    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        JDOMExternalizerUtil.writeField(element, "uploadFile", uploadFile);// 获取服务器配置
        XmlSerializer.serializeObjectInto(options, element);
        // 升级类型
        JDOMExternalizerUtil.writeField(element, "upgradeType", upgradeType);
    }

    public List<DevServerRunConfig> getRunConfigList() {
        return options.runConfigList;
    }
}
