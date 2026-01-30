package com.tanghui.dev.idea.plugin.devserver.transfer.listener;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.tanghui.dev.idea.plugin.devserver.transfer.ui.FileTransfer;
import lombok.Getter;
import lombok.Setter;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.xfer.TransferListener;

import java.awt.*;

import static com.tanghui.dev.idea.plugin.devserver.utils.DurationFormatUtils.autoFormat;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.connect.transfer.listener
 * @Author: 唐煇
 * @CreateTime: 2025-12-03-16:10
 * @Description: 文件传输监听。
 * @Version: v1.0
 */
@Setter
@Getter
public class FileTransferListener implements TransferListener {
    // 传输进度条
    private final FileTransfer fileTransfer;
    // 开始时间戳
    private long startTime;

    private long transferredTemp = 0;
    private long startTimeTemp;

    public FileTransferListener(FileTransfer fileTransfer) {
        this.fileTransfer = fileTransfer;
        this.startTime = System.currentTimeMillis();
        this.startTimeTemp = System.currentTimeMillis();
    }

    @Override
    public TransferListener directory(String name) {
        return this;
    }

    @Override
    public StreamCopier.Listener file(String name, long size) {
        return transferred -> {
            long endTime = System.currentTimeMillis();
            // 传输耗时
            long time = endTime - startTimeTemp;
            if (time >= 1000 || ((fileTransfer.getFileTransferModel().getOffset() + transferred) >= size)) {
                long remaining = transferred - transferredTemp;
                double transferRate = (remaining) / (double) time;  // 每毫秒字节数
                // 毫秒
                double lastTime = (size - fileTransfer.getFileTransferModel().getOffset() - transferred) / transferRate;
                double percent = (((double) (fileTransfer.getFileTransferModel().getOffset() + transferred)) / size) * 100;
                String format = String.format("%.4f", percent);
                this.fileTransfer.getScheduleLabel().setText(format + "%");
                this.fileTransfer.getScheduleProgressBar().setValue((int) (fileTransfer.getFileTransferModel().getOffset() + transferred));
                this.fileTransfer.getTimeTransferLabel().setText(autoFormat(Math.round(endTime - startTime)));
                this.fileTransfer.getTimeLeftLabel().setText(autoFormat(Math.round(lastTime)));
                this.fileTransfer.getTransferSpeedLabel().setText(String.format("%.4f", ((transferRate * 1000) / 1024.0)) + " KB/sec");
                this.startTimeTemp = System.currentTimeMillis();
                this.transferredTemp = transferred;
            }
            if ((fileTransfer.getFileTransferModel().getOffset() + transferred) >= size) {
                // 禁用按钮
                this.fileTransfer.getStateButton().setEnabled(false);
                this.fileTransfer.getStartOverButton().setEnabled(false);
                this.fileTransfer.getEndButton().setEnabled(false);
                this.fileTransfer.setFinish(true);
                this.fileTransfer.getTimeLeftLabel().setText("已完成");
                this.fileTransfer.getTimeLeftLabel().setFont(JBUI.Fonts.label());
                this.fileTransfer.getTimeLeftLabel().setForeground(new JBColor(new Color(7, 177, 5), new Color(7, 177, 5)));

                // 传输完成执行代码
                this.fileTransfer.getTransferCallback().callback();
            }
        };
    }
}
