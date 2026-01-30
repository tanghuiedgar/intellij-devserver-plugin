package com.tanghui.dev.idea.plugin.devserver.ui.server;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostModel;
import com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons;
import com.tanghui.dev.idea.plugin.devserver.ui.HorizontalScrollBarEditor;
import com.tanghui.dev.idea.plugin.devserver.utils.DevServerUtil;
import com.tanghui.dev.idea.plugin.devserver.utils.MarkdownToHtmlTool;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.intellij.plugins.markdown.lang.MarkdownFileType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static com.tanghui.dev.idea.plugin.devserver.ui.server.RemoteServer.hostModels;
import static com.tanghui.dev.idea.plugin.devserver.utils.MarkdownToHtmlTool.LOCAL_RENDER_BASE;
import static com.tanghui.dev.idea.plugin.devserver.utils.MarkdownToHtmlTool.getJBCefBrowser;
import static com.tanghui.dev.idea.plugin.devserver.utils.ServerHostTool.saveServerHost;

/**
 * @Author: 唐煇
 * @CreateTime: 2025-12-30-09:17
 * @Description: Browser 面板
 * @Version: v1.0
 */
public class BrowserPanel {

    private final Project project;
    private final JBCefBrowser browser;
    private String markdownContent;

    public static boolean serverInfoShow = false;

    private final JPanel root = new JPanel(new BorderLayout());
    private JBTextField urlField;

    private final JPanel serverInfoPanel;

    private final ServerHostModel hostModel;

    private HorizontalScrollBarEditor serverInfo;

    private final Disposable parentDisposable;

    public BrowserPanel(Project project, JBCefBrowser browser, Disposable parentDisposable, ServerHostModel hostModel) {
        this.project = project;
        this.browser = browser;
        this.hostModel = hostModel;
        this.markdownContent = hostModel.getServerInfo();
        this.parentDisposable = parentDisposable;
        this.serverInfoPanel = new JPanel(new BorderLayout());
        create();
        root.add(createToolbar(), BorderLayout.NORTH);
        if (serverInfoShow) {
            this.serverInfoPanel.add(serverInfo, BorderLayout.CENTER);
        } else {
            this.serverInfoPanel.add(browser.getComponent(), BorderLayout.CENTER);
        }
        root.add(this.serverInfoPanel, BorderLayout.CENTER);
    }

    private void create() {
        FileType fileType = MarkdownFileType.INSTANCE;
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("logeText.md", fileType, "");
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        this.serverInfo = new HorizontalScrollBarEditor(document, project, fileType, false, false);
        this.serverInfo.setFont(EditorUtil.getEditorFont());
        this.serverInfo.setText(markdownContent);
        serverInfo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                markdownContent = event.getDocument().getText();
            }
        }, this.parentDisposable); // 👈 Disposable，避免内存泄漏

        this.serverInfo.updateUI();
    }

    private JComponent createToolbar() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        List<AnAction> actions = new ArrayList<>();
        AnAction back = new AnAction("后退", "", DevServerIcons.DevServer_TOOLBAR_TOAPI) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                browser.getCefBrowser().goBack();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                if (!serverInfoShow) {
                    presentation.setEnabled(true);
                    presentation.setVisible(true);
                } else {
                    presentation.setVisible(false); // 👈 直接隐藏
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        AnAction forward = new AnAction("前进", "", DevServerIcons.DevServer_TOOLBAR_TOREQUEST) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                browser.getCefBrowser().goForward();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                if (!serverInfoShow) {
                    presentation.setEnabled(true);
                    presentation.setVisible(true);
                } else {
                    presentation.setVisible(false); // 👈 直接隐藏
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };

        AnAction refresh = new AnAction("刷新", "", DevServerIcons.DevServer_RESTART) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                browser.getCefBrowser().reload();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                if (!serverInfoShow) {
                    presentation.setEnabled(true);
                    presentation.setVisible(true);
                } else {
                    presentation.setVisible(false); // 👈 直接隐藏
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };

        AnAction home = new AnAction("首页", "", DevServerIcons.DevServer_TOOLBAR_HOME) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 设置启用的扩展
                String htmlContent = browserLoadHTML(markdownContent);
                browser.loadHTML(
                        MarkdownToHtmlTool.renderMarkdownToHtml(htmlContent), LOCAL_RENDER_BASE
                );
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                if (!serverInfoShow) {
                    presentation.setEnabled(true);
                    presentation.setVisible(true);
                } else {
                    presentation.setVisible(false); // 👈 直接隐藏
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        actions.add(new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                serverInfoShow = !serverInfoShow;
                if (serverInfoShow) {
                    // 编辑
                    serverInfoPanel.removeAll();
                    serverInfoPanel.add(serverInfo, BorderLayout.CENTER);
                    if (urlField != null) {
                        urlField.setVisible(false);
                    }
                    serverInfoPanel.revalidate(); // 刷新布局
                    serverInfoPanel.repaint();    // 重绘
                } else {
                    if (JBCefApp.isSupported()) {
                        // use JCEF
                        ApplicationManager.getApplication().invokeAndWait(() -> {
                            if (urlField != null) {
                                urlField.setVisible(true);
                            }
                            // 预览
                            serverInfoPanel.removeAll();
                            String markdownContent = serverInfo.getText();
                            JBCefBrowser browser = getJBCefBrowser(markdownContent);
                            serverInfoPanel.add(browser.getComponent(), BorderLayout.CENTER); // 添加到面板
                            serverInfoPanel.revalidate(); // 刷新布局
                            serverInfoPanel.repaint();    // 重绘
                        });
                    }
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                if (serverInfoShow) {
                    presentation.setText("预览");
                    presentation.setIcon(DevServerIcons.DevServer_TOOLBAR_SHOW);
                } else {
                    presentation.setText("编辑");
                    presentation.setIcon(DevServerIcons.DevServer_TOOLBAR_UPDATE);
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });
        actions.add(new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                for (ServerHostModel model : hostModels) {
                    if (hostModel.chekString().equals(model.chekString())) {
                        model.setServerInfo(markdownContent);
                        break; // 找到就停
                    }
                }
                saveServerHost(hostModels);
                NotificationGroupManager.getInstance().getNotificationGroup("DevServer.Plugin.Notification")
                        .createNotification("服务器详情", "服务器详情保存成功", NotificationType.INFORMATION)
                        .notify(project);
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                presentation.setText("保存");
                presentation.setIcon(DevServerIcons.DevServer_SAVE);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });
        actions.add(back);
        actions.add(forward);
        actions.add(refresh);
        actions.add(home);

        var actionToolBar = DevServerUtil.createActionToolBar(
                ActionPlaces.TOOLBAR, true, actions);

        buttons.add(actionToolBar.getComponent());

        urlField = new JBTextField();
        urlField.addActionListener((ActionEvent e) ->
                browser.loadURL(urlField.getText())
        );

        browser.getJBCefClient().addDisplayHandler(
                new CefDisplayHandlerAdapter() {
                    @Override
                    public void onAddressChange(
                            CefBrowser cefBrowser,
                            CefFrame frame,
                            String url) {
                        if (!frame.isMain()) return;
                        // 排除本地 render 的 HTML
                        if (url != null && url.endsWith(LOCAL_RENDER_BASE)) {
                            SwingUtilities.invokeLater(() ->
                                    urlField.setText("")
                            );
                            return;
                        }
                        SwingUtilities.invokeLater(() ->
                                urlField.setText(url)
                        );
                    }
                },
                browser.getCefBrowser()
        );

        panel.add(buttons, BorderLayout.WEST);

        urlField.setPreferredSize(JBUI.size(-1, 30));

        JPanel jPanel = new JPanel(new BorderLayout());
        JPanel centerWrapper = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        centerWrapper.add(urlField, gbc);
        jPanel.add(centerWrapper, BorderLayout.CENTER);
        panel.add(jPanel, BorderLayout.CENTER);

        return panel;
    }

    public static String browserLoadHTML(String markdownContent) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS,
                List.of(TablesExtension.create()));
        options.set(HtmlRenderer.SOFT_BREAK, "<br/>");

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(markdownContent);
        return renderer.render(document);
    }

    public JComponent getComponent() {
        return root;
    }
}
