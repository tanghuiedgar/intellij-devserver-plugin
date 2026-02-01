package com.tanghui.dev.idea.plugin.devserver.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.terminal.JBTerminalWidget;
import com.jediterm.core.util.TermSize;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostFileModel;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons;
import com.tanghui.dev.idea.plugin.devserver.pool.GlobalSshPoolManager;
import com.tanghui.dev.idea.plugin.devserver.pool.SshClientFactory;
import com.tanghui.dev.idea.plugin.devserver.pool.SshConnectionPool;
import com.tanghui.dev.idea.plugin.devserver.tree.ServerHostTreeNode;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.PTYMode;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static com.tanghui.dev.idea.plugin.devserver.utils.TerminalUtil.getJbTerminalWidget;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.utils
 * @Author: 唐煇
 * @CreateTime: 2026-01-22-15:51
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */

public class ServerHostTool {
    private static final Logger LOG = LoggerFactory.getLogger(ServerHostTool.class);

    /**
     * 服务器用户名称
     */
    private static final Map<String, Map<Long, String>> hostUidInfo = new HashMap<>();
    /**
     * 服务器用户组名称
     */
    private static final Map<String, Map<Long, String>> hostGidInfo = new HashMap<>();


    public static @Nullable ServerHostTreeNode getServerHostTreeNode(String path, ServerHostTreeNode defaultMutableTreeNode) {
        String[] nodes = path.split("//");
        // 从根节点开始逐层查找
        ServerHostTreeNode currentNode = defaultMutableTreeNode;
        ServerHostTreeNode treeNode = null;
        for (String node : nodes) {
            boolean found = false;
            // 遍历当前节点的所有子节点，寻找匹配的节点
            for (int j = 0; j < currentNode.getChildCount(); j++) {
                ServerHostTreeNode child = (ServerHostTreeNode) currentNode.getChildAt(j);
                if (child.getName().equals(node)) {
                    currentNode = child;  // 如果找到了匹配的节点，进入该节点
                    found = true;
                    break;
                }
            }
            // 如果在某一层没有找到匹配的节点，返回false
            if (!found) {
                treeNode = new ServerHostTreeNode(node, DevServerIcons.DevServer_PACKAGE);
                currentNode.add(treeNode);
                currentNode = treeNode;
            } else {
                treeNode = currentNode;
            }
        }
        return treeNode;
    }

    /**
     * 保存服务器信息
     */
    public static void saveServerHost(List<ServerHostModel> hostModels) {
        JSONArray jsonArray = getServerHostJsonArray(hostModels);
        saveServerHost(jsonArray);
    }

    private static @NotNull JSONArray getServerHostJsonArray(List<ServerHostModel> hostModels) {
        JSONArray jsonArray = new JSONArray();
        for (ServerHostModel hostModel : hostModels) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("environment", hostModel.getEnvironment().trim());
            jsonObject.put("host", hostModel.getHost().trim());
            jsonObject.put("userName", hostModel.getUserName().trim());
            jsonObject.put("password", hostModel.getPassword().trim());
            jsonObject.put("path", StringUtils.isBlank(hostModel.getPath()) ? "" : hostModel.getPath().trim());
            jsonObject.put("serverGroupBy", StringUtils.isBlank(hostModel.getServerGroupBy()) ? "" : hostModel.getServerGroupBy().trim());
            jsonObject.put("command", StringUtils.isBlank(hostModel.getCommand()) ? "" : hostModel.getCommand().trim());
            jsonObject.put("serverInfo", StringUtils.isBlank(hostModel.getServerInfo()) ? "" : hostModel.getServerInfo());
            jsonObject.put("osType", StringUtils.isBlank(hostModel.getOsType()) ? "" : hostModel.getOsType());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    /**
     * 保存服务器信息
     */
    public static void saveServerHost(JSONArray jsonArray) {
        String mapperPath = PathManager.getConfigPath();
        String mapperFileName = "ServerHost.json";
        // 将 JSON 对象写入文件
        File file = new File(mapperPath + File.separator + mapperFileName);
        if (file.exists()) {
            boolean delete = file.delete();
            if (!delete) {
                LOG.info("删除服务器信息失败！");
            }
        }
        try (FileWriter fileWriter = new FileWriter(mapperPath + File.separator + mapperFileName)) {
            fileWriter.write(JSON.toJSONString(jsonArray, true));
        } catch (IOException e) {
            LOG.error("保存服务器信息失败：{}", e.getMessage(), e);
        }
    }

    public static List<ServerHostFileModel> getServerHostFileModels(Project project, ServerHostModel serverHost) {
        List<ServerHostFileModel> hostFileModels = new ArrayList<>();

        SshConnectionPool pool = GlobalSshPoolManager.getPool(
                serverHost.getHost(),
                serverHost.getPort(),
                serverHost.getUserName(),
                serverHost.getPassword()
        );
        SSHClient sshClient = null;
        SFTPClient sftp = null;
        try {
            sshClient = pool.borrow();
            // ③ 只创建一次 sftp
            sftp = sshClient.newSFTPClient();

            // 优化 1: 移除 fileExists 调用(内部会创建新SFTP连接)，直接用当前的 sftp 实例获取列表
            List<RemoteResourceInfo> list = null;
            try {
                list = sftp.ls(serverHost.getPath());
            } catch (SFTPException e) {
                // 忽略文件不存在的情况，其他异常正常抛出
                if (e.getStatusCode() != Response.StatusCode.NO_SUCH_FILE) {
                    throw e;
                }
            }
            if (CollectionUtils.isNotEmpty(list)) {
                for (RemoteResourceInfo info : list) {
                    String fileName = info.getName();                        // 文件名
                    FileMode fileMode = info.getAttributes().getMode();      // 权限
                    long fileSize = info.getAttributes().getSize();          // 文件大小
                    long mtime = info.getAttributes().getMtime();            // 最后修改时间
                    int uid = info.getAttributes().getUID();                 // 所有者用户ID
                    int gid = info.getAttributes().getGID();                 // 所有者组ID
                    int mask = fileMode.getPermissionsMask();                // 权限掩码(Octal)
                    String owner = getUidGidInfo(serverHost.getHost(), uid, true);
                    if (owner == null) {
                        try (Session session = sshClient.startSession()) {
                            owner = getUserNameByUID(serverHost.getHost(), session, uid);
                        } catch (Exception e) {
                            // 获取失败时降级显示 UID
                            owner = String.valueOf(uid);
                        }
                    }
                    String group = getUidGidInfo(serverHost.getHost(), gid, false);
                    if (group == null) {
                        try (Session session = sshClient.startSession()) {
                            group = getGroupNameByGID(serverHost.getHost(), session, gid);
                        } catch (Exception e) {
                            // 获取失败时降级显示 GID
                            group = String.valueOf(gid);
                        }
                    }
                    Date date = new Date(mtime * 1000L);
                    ServerHostFileModel fileModel = new ServerHostFileModel();
                    fileModel.setFileName(fileName);
                    fileModel.setFileSize(fileSize);
                    fileModel.setPermissions(maskToRwx(mask));
                    fileModel.setOwner(owner);
                    fileModel.setMTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
                    fileModel.setTimestamp(mtime * 1000L);
                    fileModel.setGroup(group);
                    fileModel.setIsDir(FileMode.Type.DIRECTORY.equals(fileMode.getType()));
                    hostFileModels.add(fileModel);
                }
            }
        } catch (Exception e) {
            LOG.error("服务器连接失败：{}", e.getMessage(), e);
        } finally {
            // 关闭资源
            if (sftp != null) try {
                sftp.close();
            } catch (Exception ignored) {
            }
            pool.release(sshClient);
        }
        return hostFileModels;
    }

    /**
     * 判断远程服务器文件是否存在
     */
    public static boolean fileExists(SSHClient sshClient, String filePath) {
        try (SFTPClient sftp = sshClient.newSFTPClient()) {
            sftp.stat(filePath);
            return true;    // 文件存在
        } catch (SFTPException e) {
            if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE) {
                return false; // 文件不存在
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    public static String getGroupNameByGID(String serverHost, Session session, long gid) {
        String groupName = getUidGidInfo(serverHost, gid, false);
        if (groupName != null) {
            return groupName;
        }
        // 执行命令获取组名
        String command = "getent group " + gid;
        String result = execStdout(session, command);
        groupName = result.split(":")[0]; // 提取组名部分
        Map<Long, String> hostUser = new HashMap<>();
        hostUser.put(gid, groupName);
        hostGidInfo.put(serverHost, hostUser);
        return groupName;
    }


    public static String getUserNameByUID(String serverHost, Session session, long uid) {
        String userName = getUidGidInfo(serverHost, uid, true);
        if (userName != null) {
            return userName;
        }
        // 执行命令获取用户名
        String command = "id -nu " + uid;
        userName = execStdout(session, command);
        Map<Long, String> hostUser = new HashMap<>();
        hostUser.put(uid, userName);
        hostUidInfo.put(serverHost, hostUser);
        return userName;
    }

    private static String getUidGidInfo(@NotNull String serverHost, long id, boolean isUid) {
        Map<Long, String> userInfo;
        if (isUid) {
            userInfo = hostUidInfo.get(serverHost);
        } else {
            userInfo = hostGidInfo.get(serverHost);
        }
        if (userInfo != null) {
            return userInfo.get(id);
        }
        return null;
    }

    public static String execStdout(Session session, String command) {
        try {
            Session.Command cmd = session.exec(command);
            String out = new String(cmd.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // 等待命令结束再返回，确保输出完整
            cmd.join();
            return out;
        } catch (Exception e) {
            throw new RuntimeException(DevServerBundle.INSTANCE.message("deploy.server.type.execute.command.error") + ": " + command + ", err=" + e.getMessage(), e);
        }
    }

    public static String maskToRwx(int mask) {
        StringBuilder sb = new StringBuilder(9);
        int[] perms = new int[]{
                (mask >> 6) & 7, // owner
                (mask >> 3) & 7, // group
                mask & 7         // other
        };
        for (int p : perms) {
            sb.append((p & 4) != 0 ? 'r' : '-');
            sb.append((p & 2) != 0 ? 'w' : '-');
            sb.append((p & 1) != 0 ? 'x' : '-');
        }
        return sb.toString();
    }


    /**
     * 获取终端工具窗口
     *
     * @param project        项目
     * @param hostModel      远程服务器信息
     * @param executeCommand 执行命令列表
     *
     */
    public static JComponent createRemoteNewTerminalComponent(Project project, ServerHostModel hostModel, List<String> executeCommand) {
        Disposable tempDisposable = Disposer.newDisposable();
        // 创建新的终端实例
        JBTerminalWidget terminalWidget = getJbTerminalWidget(project, tempDisposable);
        // 设置字体为终端字体
        terminalWidget.setFont(EditorUtil.getEditorFont());  // 设置固定宽度字体
        ApplicationManager.getApplication().invokeAndWait(() -> {
            Task.Backgroundable task = new Task.Backgroundable(project, DevServerBundle.INSTANCE.message("open.terminal.title")) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    progressIndicator.setIndeterminate(true);
                    String host = hostModel.getHost();
                    int port = hostModel.getPort();
                    String userName = hostModel.getUserName();
                    String password = hostModel.getPassword();
                    try {
                        SSHClient ssh = SshClientFactory.create( host,
                                port,
                                userName,
                                password);
                        Session session = ssh.startSession();
                        TermSize size = terminalWidget.getTerminal().getSize();
                        session.allocatePTY(
                                "xterm",
                                size.getColumns(), size.getRows(), 0, 0,
                                new EnumMap<>(PTYMode.class)
                        );
                        Session.Shell shell = session.startShell();
                        if (executeCommand != null && !executeCommand.isEmpty()) {
                            OutputStream outputStream = shell.getOutputStream();
                            for (String executeCommandElement : executeCommand) {
                                if (StringUtils.isNotBlank(executeCommandElement)) {
                                    String[] lines = executeCommandElement.split("\\n");
                                    for (String line : lines) {
                                        if (StringUtils.isNotBlank(line)) {
                                            outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                                            outputStream.flush();
                                        }
                                    }
                                }
                            }
                        }
                        SSHJTtyConnector connector = new SSHJTtyConnector(ssh, session, shell);
                        terminalWidget.createTerminalSession(connector);
                        terminalWidget.start();
                        connector.resize(size);
                    } catch (Exception e) {
                        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                                .createNotification(DevServerBundle.INSTANCE.message("server.connect.title"),
                                        DevServerBundle.INSTANCE.message("server.connect.error"), NotificationType.INFORMATION)
                                .notify(project));
                    }
                }
            };
            ProgressManager.getInstance().run(task);
        });
        return terminalWidget.getComponent();
    }

    public static String getFilePath(Object[] path) {
        List<ServerHostTreeNode> list = Arrays.stream(Arrays.copyOfRange(path, 2, path.length)).map(v -> (ServerHostTreeNode) v).toList();
        return "/" + String.join("/", list.stream().map(ServerHostTreeNode::getName).toList());
    }

    public static String getPreviousFilePath(Object[] path) {
        List<ServerHostTreeNode> list = Arrays.stream(Arrays.copyOfRange(path, 2, path.length - 1)).map(v -> (ServerHostTreeNode) v).toList();
        return "/" + String.join("/", list.stream().map(ServerHostTreeNode::getName).toList());
    }

    /**
     * 查询服务器用户是否存在
     * */
    public static boolean userExists(SSHClient ssh, String username) throws IOException {
        if (StringUtils.isBlank(username)) {
            return true;
        }
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec("getent passwd " + username);
            String output = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join();
            return cmd.getExitStatus() == 0 && StringUtils.isNotBlank(output);
        }
    }
}
