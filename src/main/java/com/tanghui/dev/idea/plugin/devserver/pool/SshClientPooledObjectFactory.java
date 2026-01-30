package com.tanghui.dev.idea.plugin.devserver.pool;

import net.schmizz.sshj.SSHClient;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public record SshClientPooledObjectFactory(String host, int port, String user,
                                           String password) implements PooledObjectFactory<SSHClient> {

    @Override
    public PooledObject<SSHClient> makeObject() throws Exception {
        SSHClient ssh = SshClientFactory.create(host, port, user, password);
        return new DefaultPooledObject<>(ssh);
    }

    @Override
    public void destroyObject(PooledObject<SSHClient> p) throws Exception {
        SSHClient ssh = p.getObject();
        try {
            ssh.disconnect();
        } catch (Exception ignored) {
        }
        try {
            ssh.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean validateObject(PooledObject<SSHClient> p) {
        SSHClient ssh = p.getObject();
        return ssh.isConnected() && ssh.isAuthenticated();
    }

    @Override
    public void activateObject(PooledObject<SSHClient> p) throws Exception {
        SSHClient ssh = p.getObject();
        if (!ssh.isConnected()) {
            destroyObject(p);
            p = makeObject();
        }
    }

    @Override
    public void passivateObject(PooledObject<SSHClient> pooledObject) {
    }
}
