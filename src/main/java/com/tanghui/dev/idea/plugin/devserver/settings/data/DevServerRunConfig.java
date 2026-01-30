package com.tanghui.dev.idea.plugin.devserver.settings.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * @BelongsPackage: com.tanghui.settings
 * @Author: 唐煇
 * @CreateTime: 2024-07-19 16:17
 * @Description: 描述类的主要功能和用途。
 * @Version: 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class DevServerRunConfig implements Serializable {
    /**
     * 名称
     *
     */
    private String name;
    /**
     * 上传服务器
     */
    private String serverHost;
    /**
     * 服务器用户
     */
    private String serverUser;
    /**
     * 服务器密码
     */
    private String serverPassword;
    /**
     * 服务器端口
     */
    private String serverPort;
    /**
     * 操作用户
     *
     */
    private String controlsUser;
    /**
     * 上传服务器路径
     *
     */
    private String targetDirectory;
    /**
     * shell 脚本
     *
     */
    private String shellScript;
    /**
     * 回退脚本
     */
    private String rollbackScript;
    /**
     * 查看日志
     */
    private String logeText;
    /**
     * 回退脚本运行开关
     */
    private String rollbackScriptOnOff;
    /**
     * 运行脚本运行开关
     */
    private String shellScriptOnOff;
}
