package com.tanghui.dev.idea.plugin.devserver.task;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.common.task
 * @Author: 唐煇
 * @CreateTime: 2025-03-12-11:49
 * @Description: 文件传输回调接口。
 * @Version: v1.0
 */
public interface FileTransferCallback {

    /**
     * 文件上传或下载结束之后调用此方法
     * */
    void callback();

    /**
     * 终止传输回调方法
     * */
    void stopTransfer();

}
