package com.tanghui.dev.idea.plugin.devserver.pool;

import net.schmizz.sshj.SSHClient;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;

public class SshConnectionPool {
    private final GenericObjectPool<SSHClient> pool;

    public SshConnectionPool(String host, int port, String user, String password) {

        GenericObjectPoolConfig<SSHClient> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(10); // 最大连接数
        config.setMaxIdle(5);   // 最大空闲
        config.setMinIdle(2);   // 保底3个连接
        config.setBlockWhenExhausted(true);
        config.setMaxWait(Duration.ofSeconds(5)); // 最长等待5秒
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(30)); // 每30秒检测一次
        config.setMinEvictableIdleDuration(Duration.ofMinutes(5)); // 5分钟没用回收

        this.pool = new GenericObjectPool<>(
                new SshClientPooledObjectFactory(host, port, user, password),
                config
        );
    }

    public SSHClient borrow() throws Exception {
        return pool.borrowObject();
    }

    public void delete(SSHClient client) throws Exception {
        pool.invalidateObject(client);
    }

    public void release(SSHClient client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }

    public void close() {
        pool.close();
    }
}
