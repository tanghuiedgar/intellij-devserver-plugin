package com.tanghui.dev.idea.plugin.devserver.deploy.dialog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerInfoModel;
import com.tanghui.dev.idea.plugin.devserver.pool.GlobalSshPoolManager;
import com.tanghui.dev.idea.plugin.devserver.pool.SshConnectionPool;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import com.tanghui.dev.idea.plugin.devserver.utils.file.FileUtil;
import git4idea.ui.ComboBoxWithAutoCompletion;
import lombok.Getter;
import lombok.Setter;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.NumberFormat;

import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.execStdout;
import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.userExists;

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.deploy.dialog
 * @Author: 唐煇
 * @CreateTime: 2026-01-19-14:21
 * @Description: 服务器认证。
 * @Version: v1.0
 */

@Getter
public class ServerAuthentication {
    private JBPanel<?> root;
    private JEditorPane infoTextArea;
    private ComboBoxWithAutoCompletion<ServerInfoModel> serverTextField;
    private JFormattedTextField portTextField;
    private JBTextField userTextField;
    private JBTextField controlsUserTextField;
    private JBPasswordField passwordField;
    private JButton joinButton;

    // 是否连接成功
    private boolean successNot = false;

    private final Project project;

    @Setter
    private DevServerRunConfig parentComponent;


    public ServerAuthentication(Project project, DevServerRunConfig parent) {
        this.parentComponent = parent;
        this.project = project;

        $$$setupUI$$$();
        NumberFormat format = NumberFormat.getIntegerInstance();
        format.setGroupingUsed(false); // 不允许 1,000 这种
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(false); // ❗关键：禁止非法字符
        formatter.setMinimum(0);           // 可选：最小值
        formatter.setMaximum(99999);       // 可选：最大值
        DefaultFormatterFactory defaultFormatterFactory = new DefaultFormatterFactory(formatter);
        // 创建一个 JFormattedTextField，并设置其格式
        this.portTextField.setFormatterFactory(defaultFormatterFactory);
        this.portTextField.setColumns(5); // 设置显示的列数
        if (parent != null && StringUtils.isNotBlank(parent.getServerHost())) {
            // 初始化
            parentComponent = parent;
            passwordField.setText(parent.getServerPassword());
            controlsUserTextField.setText(parent.getControlsUser());
            userTextField.setText(parent.getServerUser());
            portTextField.setText(parent.getServerPort());
            String ip = parent.getServerHost();
            ComboBoxModel<ServerInfoModel> fieldModel = serverTextField.getModel();
            for (int i = 0; i < fieldModel.getSize(); i++) {
                ServerInfoModel elementAt = fieldModel.getElementAt(i);
                if (elementAt.ip().equals(ip)) {
                    serverTextField.setItem(elementAt);
                }
            }
        }


        joinButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                infoTextArea.setForeground(new JBColor(new Color(0, 214, 255), new Color(0, 214, 255)));
                infoTextArea.setText("正在连接服务器...");
                Task.Backgroundable task = new Task.Backgroundable(project, "执行后续命令") {
                    private String infoText = "";

                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        progressIndicator.setIndeterminate(true);
                        // 获取服务器地址
                        String serverHost = serverTextField.getText();
                        // 获取端口
                        String port = portTextField.getText();
                        // 获取用户名
                        String user = userTextField.getText();
                        // 获取密码
                        String password = new String(passwordField.getPassword());
                        if (checkNotBlank(serverHost, "服务器地址不能为空")) return;
                        if (checkNotBlank(port, "端口不能为空！")) return;
                        if (checkNotBlank(user, "用户名不能为空！")) return;
                        if (checkNotBlank(password, "密码不能为空！")) return;
                        ApplicationManager.getApplication().invokeAndWait(() -> {
                            // 创建会话并连接
                            SshConnectionPool pool = GlobalSshPoolManager.getPool(
                                    serverHost,
                                    Integer.parseInt(port),
                                    user,
                                    password
                            );
                            SSHClient ssh = null;
                            Session session = null;
                            try {
                                ssh = pool.borrow();
                                // 检查连接是否成功
                                session = ssh.startSession();
                                if (userExists(ssh, controlsUserTextField.getText()) && session.isOpen()) {
                                    String result = execStdout(session, "uname -a");
                                    successNot = true;
                                    if (StringUtils.isNotBlank(result)) {
                                        infoText = "服务器信息：" + result;
                                    } else {
                                        infoText = "服务器连接成功！";
                                    }
                                } else {
                                    successNot = false;
                                    infoText = "服务器连接失败！";
                                }
                            } catch (Exception exception) {
                                successNot = false;
                                infoText = "服务器连接失败！";
                            } finally {
                                // 关闭资源
                                if (session != null) try {
                                    session.close();
                                } catch (Exception ignored) {
                                }
                                pool.release(ssh);
                            }
                        });
                    }

                    @Override
                    public void onSuccess() {
                        if (successNot) {
                            infoTextArea.setForeground(new JBColor(new Color(36, 175, 0), new Color(36, 175, 0)));
                        } else {
                            infoTextArea.setForeground(new JBColor(new Color(175, 0, 0), new Color(175, 0, 0)));
                        }
                        infoTextArea.setText(infoText);
                    }

                    @Override
                    public void onThrowable(@NotNull Throwable error) {
                        if (!successNot) {
                            infoTextArea.setForeground(new JBColor(new Color(175, 0, 0), new Color(175, 0, 0)));
                            infoTextArea.setText("服务器连接失败！");
                        }
                        ;
                    }
                };
                ProgressManager.getInstance().run(task);
            }
        });

    }

    /** Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        root = new JBPanel();
        root.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        root.setMaximumSize(JBUI.size(500, 300));
        root.setMinimumSize(JBUI.size(500, 300));
        root.setPreferredSize(JBUI.size(500, 300));
        final JBPanel panel1 = new JBPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        root.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel2 = new JBPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel2.setMaximumSize(JBUI.size(2147483647, 170));
        panel2.setMinimumSize(JBUI.size(24, 170));
        panel2.setPreferredSize(JBUI.size(24, 170));
        panel1.add(panel2, BorderLayout.NORTH);
        final JBPanel panel3 = new JBPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel3.setMaximumSize(JBUI.size(470, 50));
        panel3.setMinimumSize(JBUI.size(470, 50));
        panel3.setPreferredSize(JBUI.size(470, 50));
        panel2.add(panel3);
        final JBPanel panel4 = new JBPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel4.setMaximumSize(JBUI.size(330, 2147483647));
        panel4.setMinimumSize(JBUI.size(330, 24));
        panel4.setPreferredSize(JBUI.size(330, 24));
        panel3.add(panel4, BorderLayout.WEST);
        final JBPanel panel5 = new JBPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.setMaximumSize(JBUI.size(80, 2147483647));
        panel5.setMinimumSize(JBUI.size(80, 24));
        panel5.setPreferredSize(JBUI.size(80, 24));
        panel4.add(panel5, BorderLayout.WEST);
        final JBLabel label1 = new JBLabel();
        label1.setText("服务器");
        panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel6 = new JBPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel6, BorderLayout.CENTER);
        panel6.add(serverTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel7 = new JBPanel();
        panel7.setLayout(new BorderLayout(0, 0));
        panel3.add(panel7, BorderLayout.CENTER);
        final JBPanel panel8 = new JBPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.setMaximumSize(JBUI.size(70, 2147483647));
        panel8.setMinimumSize(JBUI.size(70, 24));
        panel8.setPreferredSize(JBUI.size(70, 24));
        panel7.add(panel8, BorderLayout.WEST);
        final JBLabel label2 = new JBLabel();
        label2.setText("端口");
        panel8.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel9 = new JBPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel9, BorderLayout.CENTER);
        portTextField = new JFormattedTextField();
        panel9.add(portTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel10 = new JBPanel();
        panel10.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel10.setMaximumSize(JBUI.size(470, 50));
        panel10.setMinimumSize(JBUI.size(470, 50));
        panel10.setPreferredSize(JBUI.size(470, 50));
        panel2.add(panel10);
        final JBPanel panel11 = new JBPanel();
        panel11.setLayout(new BorderLayout(0, 0));
        panel10.add(panel11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel12 = new JBPanel();
        panel12.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel12.setMaximumSize(JBUI.size(80, 2147483647));
        panel12.setMinimumSize(JBUI.size(80, 24));
        panel12.setPreferredSize(JBUI.size(80, 24));
        panel11.add(panel12, BorderLayout.WEST);
        final JBLabel label3 = new JBLabel();
        label3.setText("连接用户");
        panel12.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel13 = new JBPanel();
        panel13.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel11.add(panel13, BorderLayout.CENTER);
        userTextField = new JBTextField();
        panel13.add(userTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel14 = new JBPanel();
        panel14.setLayout(new BorderLayout(0, 0));
        panel10.add(panel14, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JBPanel panel15 = new JBPanel();
        panel15.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel15.setMaximumSize(JBUI.size(100, 2147483647));
        panel15.setMinimumSize(JBUI.size(100, 24));
        panel15.setPreferredSize(JBUI.size(100, 24));
        panel14.add(panel15, BorderLayout.WEST);
        final JBLabel label4 = new JBLabel();
        label4.setText("操作用户");
        panel15.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel16 = new JBPanel();
        panel16.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel14.add(panel16, BorderLayout.CENTER);
        controlsUserTextField = new JBTextField();
        panel16.add(controlsUserTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel17 = new JBPanel();
        panel17.setLayout(new BorderLayout(0, 0));
        panel17.setMaximumSize(JBUI.size(470, 50));
        panel17.setMinimumSize(JBUI.size(470, 50));
        panel17.setPreferredSize(JBUI.size(470, 50));
        panel2.add(panel17);
        final JBPanel panel18 = new JBPanel();
        panel18.setLayout(new BorderLayout(0, 0));
        panel18.setMaximumSize(JBUI.size(250, 2147483647));
        panel18.setMinimumSize(JBUI.size(250, 24));
        panel18.setPreferredSize(JBUI.size(250, 24));
        panel17.add(panel18, BorderLayout.WEST);
        final JBPanel panel19 = new JBPanel();
        panel19.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel19.setMaximumSize(JBUI.size(80, 2147483647));
        panel19.setMinimumSize(JBUI.size(80, 24));
        panel19.setPreferredSize(JBUI.size(80, 24));
        panel18.add(panel19, BorderLayout.WEST);
        final JBLabel label5 = new JBLabel();
        label5.setText("用户密码");
        panel19.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel20 = new JBPanel();
        panel20.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel18.add(panel20, BorderLayout.CENTER);
        passwordField = new JBPasswordField();
        panel20.add(passwordField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JBPanel panel21 = new JBPanel();
        panel21.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel21.setMaximumSize(JBUI.size(80, 2147483647));
        panel21.setMinimumSize(JBUI.size(80, 24));
        panel21.setPreferredSize(JBUI.size(80, 24));
        panel17.add(panel21, BorderLayout.EAST);
        joinButton = new JButton();
        joinButton.setText("连接");
        panel21.add(joinButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, JBUI.size(80, -1), JBUI.size(80, -1), JBUI.size(80, -1), 0, false));
        final JBPanel panel22 = new JBPanel();
        panel22.setLayout(new GridLayoutManager(1, 1, new Insets(10, 20, 10, 20), -1, -1));
        panel1.add(panel22, BorderLayout.CENTER);
        final JBScrollPane scrollPane1 = new JBScrollPane();
        panel22.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        infoTextArea = new JEditorPane();
        scrollPane1.setViewportView(infoTextArea);
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }


    private void createUIComponents() {
        DefaultComboBoxModel<ServerInfoModel> comboBoxModel = new DefaultComboBoxModel<>();
        String mapperPath = PathManager.getConfigPath();
        String mapperFileName = "ServerHost.json";
        File file = new File(mapperPath + File.separator + mapperFileName);
        JSONArray objects = new JSONArray();
        if (file.exists()) {
            String fieldMapperJson = FileUtil.getInstance().getFileContents(file);
            objects = JSON.parseArray(fieldMapperJson);
        }
        if (!objects.isEmpty()) {
            objects.forEach(v -> {
                JSONObject serverHost = (JSONObject) v;
                String host = serverHost.getString("host");
                String serverGroupBy = serverHost.getString("serverGroupBy").replaceAll("//", "-");
                comboBoxModel.addElement(new ServerInfoModel(host, serverGroupBy));
            });
        }
        serverTextField = new ComboBoxWithAutoCompletion<>(comboBoxModel, ProjectManager.getInstance().getOpenProjects()[0]);
        // 自定义渲染器：控制下拉框每一项的显示（例如 显示"IP + 描述信息"）
        serverTextField.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                //Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                JBLabel ipLabel = new JBLabel();
                JBLabel infoLabel = new JBLabel();
                JPanel textRoot = new JPanel();
                textRoot.setLayout(new GridLayoutManager(2, 1));
                if (value instanceof ServerInfoModel(String ip, String info)) {
                    ipLabel.setText(ip); // 下拉框显示 IP
                    infoLabel.setText(info); // 下拉框显示详细信息
                    textRoot.add(ipLabel,
                            new GridConstraints(
                                    0, 0,  // row, column
                                    1, 1,  // rowSpan, colSpan
                                    GridConstraints.ANCHOR_CENTER,
                                    GridConstraints.FILL_HORIZONTAL,
                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                    GridConstraints.SIZEPOLICY_FIXED,
                                    null, null, null
                            ));
                    textRoot.add(infoLabel,
                            new GridConstraints(
                                    1, 0,  // 第二行
                                    1, 1,
                                    GridConstraints.ANCHOR_CENTER,
                                    GridConstraints.FILL_HORIZONTAL,
                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                    GridConstraints.SIZEPOLICY_FIXED,
                                    null, null, null
                            ));
                }
                if (isSelected) {
                    textRoot.setBackground(list.getSelectionBackground());
                    ipLabel.setForeground(list.getSelectionForeground());
                    infoLabel.setForeground(list.getSelectionForeground());
                } else {
                    textRoot.setBackground(list.getBackground());
                    ipLabel.setForeground(list.getForeground());
                    infoLabel.setForeground(list.getForeground());
                }
                // 保证 JPanel 可显示背景
                textRoot.setOpaque(true);
                return textRoot;
            }
        });
        serverTextField.getEditor().getEditorComponent().setFont(EditorUtil.getEditorFont());
        serverTextField.setItem(new ServerInfoModel("", ""));
        serverTextField.setVisible(true);
        JSONArray finalObjects = objects;
        serverTextField.addItemListener(e -> {
            String item = e.getItem().toString();
            JSONObject jsonObject = finalObjects.stream().filter(v -> {
                JSONObject serverHost = (JSONObject) v;
                String temp = serverHost.getString("host");
                return temp.equals(item);
            }).findFirst().map(v -> (JSONObject) v).orElse(new JSONObject());
            userTextField.setText(jsonObject.getString("userName"));
            passwordField.setText(jsonObject.getString("password"));
            portTextField.setText("22");
            userTextField.updateUI();
            passwordField.updateUI();
            portTextField.updateUI();
        });
        serverTextField.setPreferredSize(JBUI.size(-1, 30));
        serverTextField.updateUI();
    }

    private boolean checkNotBlank(String value, String message) {
        if (StringUtils.isBlank(value)) {
            Messages.showMessageDialog(
                    message,
                    "Host",
                    Messages.getErrorIcon()
            );
            return true;
        }
        return false;
    }
}
