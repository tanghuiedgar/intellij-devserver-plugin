package com.tanghui.dev.idea.plugin.devserver.pool;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;

public class SshClientFactory {

    public static SSHClient create(String host, int port, String user, String password) throws IOException {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier()); // 自行替换为安全的验证
        ssh.connect(host, port);
        ssh.authPassword(user, password);
        ssh.getConnection().getKeepAlive().setKeepAliveInterval(30);
        return ssh;
    }
}
