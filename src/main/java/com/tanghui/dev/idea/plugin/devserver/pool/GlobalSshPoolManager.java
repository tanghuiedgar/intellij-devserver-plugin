package com.tanghui.dev.idea.plugin.devserver.pool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalSshPoolManager {
    private static final Map<String, SshConnectionPool> POOLS = new ConcurrentHashMap<>();

    private static String key(String host, int port, String user) {
        return host + ":" + port + ":" + user;
    }

    public static SshConnectionPool getPool(String host, int port, String user, String password) {
        return POOLS.computeIfAbsent(
                key(host, port, user),
                k -> new SshConnectionPool(host, port, user, password)
        );
    }

    public static void shutdownAll() {
        POOLS.values().forEach(SshConnectionPool::close);
        POOLS.clear();
    }
}
