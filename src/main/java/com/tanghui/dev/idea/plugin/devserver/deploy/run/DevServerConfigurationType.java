package com.tanghui.dev.idea.plugin.devserver.deploy.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons.DevServer_LOGO;


/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.deploy.run
 * @Author: 唐煇
 * @CreateTime: 2024-07-19 08:59
 * @Description: 描述类的主要功能和用途。
 * @Version: 1.0
 */
public class DevServerConfigurationType implements ConfigurationType {
    private final ConfigurationFactory myConfigurationFactory;

    public DevServerConfigurationType() {
        myConfigurationFactory = new ConfigurationFactory(this) {
            @NotNull
            @Override
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new DevServerRunConfiguration(project, this);
            }

            @Override
            public @NotNull @NonNls String getId() {
                return "DevServerConfigurationType";
            }
        };
    }

    @Override
    public @NotNull String getDisplayName() {
        return "DevServer";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return DevServerBundle.INSTANCE.message("deploy.config.type.description");
    }

    @Override
    public Icon getIcon() {
        return DevServer_LOGO;
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{myConfigurationFactory};
    }

    @NotNull
    @Override
    public String getId() {
        return "DevServerConfigurationType";
    }


}
