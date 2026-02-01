package com.tanghui.dev.idea.plugin.devserver.data.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.data.model
 * @Author: 唐煇
 * @CreateTime: 2025-03-12-14:03
 * @Description: 文件传输model。
 * @Version: v1.0
 */
@Setter
@Getter
public class FileTransferModel implements Serializable {
    /**
     * 本地文件路径
     * */
    private String localFilesPath;

    /**
     * 本地文件名
     * */
    private String localFilesName;

    /**
     * 远程文件路径
     * */
    private String remoteFilesPath;

    /**
     * 远程文件名
     * */
    private String remoteFilesName;

    /**
     * 上传/下载  上传服务器 true，服务器下载文件 false，默认文件是上传
     * */
    private Boolean state = Boolean.TRUE;

    /**
     * 文件传输偏移度
     * */
    private Long offset = 0L;
}
