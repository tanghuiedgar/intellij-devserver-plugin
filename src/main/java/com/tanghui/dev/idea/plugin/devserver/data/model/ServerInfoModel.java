package com.tanghui.dev.idea.plugin.devserver.data.model;

import org.jetbrains.annotations.NotNull;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.common.model
 * @Author: 唐煇
 * @CreateTime: 2025-05-22-11:23
 * @Description: 服务器下拉输入框数据模型。
 * @Version: v1.0
 */
public record ServerInfoModel(String ip, String info) {

    @NotNull
    @Override
    public String toString() {
        // 输入框仅显示 IP
        return ip;
    }
}
