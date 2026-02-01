package com.tanghui.dev.idea.plugin.devserver.data.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.data.model
 * @Author: 唐煇
 * @CreateTime: 2024-11-20-下午2:02
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
@Data
public class ServerHostModel implements Serializable {
    /**
     * 环境
     */
    private String environment = "";
    /**
     * 服务器 Ip
     */
    private String host = "";
    /**
     * 端口
     *
     */
    private Integer port = 22;
    /**
     * 连接用户
     */
    private String userName = "";
    /**
     * 连接密码
     */
    private String password = "";
    /**
     * 路径
     *
     */
    private String path = "";

    /**
     * 执行命令集合
     *
     */
    private String command;

    /**
     * 服务器说明
     *
     */
    private String serverInfo;

    /**
     * 所属分组
     *
     */
    private String serverGroupBy;

    /**
     * 服务器类型
     *
     */
    private String osType;

    public ServerHostModel() {
    }

    public ServerHostModel(String host, Integer port, String userName, String password, String path) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.path = path;
    }

    public String chekString() {
        return this.host + this.port + this.userName + this.password + this.path;
    }
}
