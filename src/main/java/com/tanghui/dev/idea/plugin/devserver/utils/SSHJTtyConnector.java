package com.tanghui.dev.idea.plugin.devserver.utils;

import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.TtyConnector;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.deploy.run
 * @Author: 唐煇
 * @CreateTime: 2025-11-14-10:43
 * @Description: sshj实现终端远程交互。
 * @Version: v1.0
 */
public class SSHJTtyConnector implements TtyConnector {
    private final SSHClient sshClient;
    private final Session session;
    private final Session.Shell shell;
    private final InputStreamReader myReader;
    private volatile boolean connected = true;

    public SSHJTtyConnector(SSHClient sshClient, Session session, Session.Shell shell) throws IOException {
        this.session = session;
        this.sshClient = sshClient;
        this.shell = shell;
        this.myReader = new InputStreamReader(shell.getInputStream(), StandardCharsets.UTF_8);
    }

    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        return this.myReader.read(buffer, offset, length);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        OutputStream out = shell.getOutputStream();
        out.write(bytes);
        out.flush();
    }

    @Override
    public void write(String s) throws IOException {
        this.write(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isConnected() {
        return connected && sshClient.isConnected() && shell.isOpen();
    }

    @Override
    public void resize(@NotNull TermSize termSize) {
        try {
            // 按控件实际列/行更新，不加任何偏移，避免视觉“间距变大”
            shell.changeWindowDimensions(
                    termSize.getColumns(),
                    termSize.getRows(),
                    0, 0
            );
        } catch (IOException ignored) {
        }
    }

    @Override
    public int waitFor() throws InterruptedException {
        // 非阻塞，按需要实现
        return 0;
    }

    @Override
    public boolean ready() throws IOException {
        return this.myReader.ready();
    }

    @Override
    public String getName() {
        return "SSHJ Terminal";
    }

    @Override
    public void close() {
        connected = false;
        try {
            shell.close();
        } catch (IOException ignored) {
        }
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }
}
