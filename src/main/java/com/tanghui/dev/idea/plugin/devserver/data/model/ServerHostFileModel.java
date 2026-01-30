package com.tanghui.dev.idea.plugin.devserver.data.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @BelongsPackage: com.tanghui.ui.model
 * @Author: 唐煇
 * @CreateTime: 2024-11-20-下午2:02
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
@Setter
@Getter
public class ServerHostFileModel implements Serializable {
    /** 文件名称 */
    private String fileName;
    /** 文件大小 */
    private Long fileSize;
    /** 文件权限 */
    private String permissions;
    /** 文件所属用户 */
    private String owner;
    /** 文件所属用户组 */
    private String group;
    /** 最近修改日期 */
    private String mTime;
    /** 日期时间戳 */
    private Long timestamp;
    /** 是否是目录 */
    private Boolean isDir;

    @Override
    public String toString() {
        return permissions + "  " +
                group + "、" + owner + "  " +
                fileName + "  " +
                mTime + "  " +
                ", 文件大小：" + fileSize;
    }
}
