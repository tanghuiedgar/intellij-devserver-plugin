package com.tanghui.dev.idea.plugin.devserver.deploy.cluster;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.OnOffButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.tanghui.dev.idea.plugin.devserver.deploy.dialog.EditScriptDialog;
import com.tanghui.dev.idea.plugin.devserver.deploy.dialog.RunShellScriptDialog;
import com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons;
import com.tanghui.dev.idea.plugin.devserver.listener.DevServerDocumentListener;
import com.tanghui.dev.idea.plugin.devserver.settings.data.DevServerRunConfig;
import com.tanghui.dev.idea.plugin.devserver.ui.HorizontalScrollBarEditor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.List;


/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.deploy.cluster
 * @Author: 唐煇
 * @CreateTime: 2025-12-12-13:50
 * @Description: 执行脚本。
 * @Version: v1.0
 */
@Setter
@Getter
public class ExecuteScript implements Disposable {
    private JBPanel<?> root;
    private JBTextField targetDirectoryTextField;
    private JBPanel<?> logeText;
    private JBPanel<?> shellScript;
    private JBPanel<?> rollbackScript;
    private OnOffButton shellScriptOnOff;
    private OnOffButton rollbackScriptOnOff;
    private JButton shellScriptButton;
    private JButton rollbackScriptButton;
    private JBPanel<?> rollbackPanel;
    private JBPanel<?> logePanel;
    private JBPanel<?> workDirectory;
    private JButton shellEditScriptButton;
    private JButton rollbackEditScriptButton;

    private HorizontalScrollBarEditor shellScriptEditor;
    private HorizontalScrollBarEditor logeTextEditor;
    private HorizontalScrollBarEditor rollbackScriptTextEditor;

    private final Project project;

    private final DevServerRunConfig runConfig;

    private List<DevServerRunConfig> runConfigList;

    public ExecuteScript(Project project, List<DevServerRunConfig> runConfigList, DevServerRunConfig runConfig) {
        this.project = project;
        this.runConfig = runConfig;
        this.runConfigList = runConfigList;
        createUIComponents();
        this.shellScriptButton.setIcon(DevServerIcons.DevServer_QUERY);
        this.shellScriptButton.setBorder(null);
        this.shellEditScriptButton.setIcon(DevServerIcons.DevServer_TOOLBAR_UPDATE);
        this.shellEditScriptButton.setBorder(null);
        this.rollbackScriptButton.setIcon(DevServerIcons.DevServer_QUERY);
        this.rollbackScriptButton.setBorder(null);
        this.rollbackEditScriptButton.setIcon(DevServerIcons.DevServer_TOOLBAR_UPDATE);
        this.rollbackEditScriptButton.setBorder(null);
        // 回退脚本
        this.rollbackScriptOnOff.setOnText("脚本");
        this.rollbackScriptOnOff.setOffText("命令");
        // 执行脚本
        this.shellScriptOnOff.setOnText("脚本");
        this.shellScriptOnOff.setOffText("命令");
        initTextField();
        actionListener();
    }

    private void initTextField() {
        if (StringUtils.isNotBlank(this.runConfig.getTargetDirectory())) {
            this.targetDirectoryTextField.setText(this.runConfig.getTargetDirectory());
        }

        if (StringUtils.isNotBlank(this.runConfig.getShellScript())) {
            this.shellScriptEditor.setText(this.runConfig.getShellScript());
        }

        if (StringUtils.isNotBlank(this.runConfig.getRollbackScript())) {
            this.rollbackScriptTextEditor.setText(this.runConfig.getRollbackScript());
        }

        if (StringUtils.isNotBlank(this.runConfig.getLogeText())) {
            this.logeTextEditor.setText(this.runConfig.getLogeText());
        }
        this.shellScriptOnOff.setSelected("true".equals(this.runConfig.getShellScriptOnOff()));
        this.rollbackScriptOnOff.setSelected("true".equals(this.runConfig.getRollbackScriptOnOff()));
    }

    private void actionListener() {
        this.targetDirectoryTextField.getDocument().addDocumentListener(new DevServerDocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                javax.swing.text.Document doc = e.getDocument();
                try {
                    String text = doc.getText(0, doc.getLength());
                    if (StringUtils.isNotBlank(text)) {
                        runConfig.setTargetDirectory(text);
                    }
                } catch (BadLocationException ignored) {
                }
            }
        });
        this.shellScriptEditor.getDocument().addDocumentListener(new DocumentListener() {
            /**
             * 文档修改
             */
            @Override
            public void documentChanged(@NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
                Document document = event.getDocument();
                runConfig.setShellScript(document.getText());
            }
        }, this);
        this.rollbackScriptTextEditor.getDocument().addDocumentListener(new DocumentListener() {
            /**
             * 文档修改
             */
            @Override
            public void documentChanged(@NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
                Document document = event.getDocument();
                runConfig.setRollbackScript(document.getText());
            }
        }, this);
        this.logeTextEditor.getDocument().addDocumentListener(new DocumentListener() {
            /**
             * 文档修改
             */
            @Override
            public void documentChanged(@NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
                Document document = event.getDocument();
                runConfig.setLogeText(document.getText());
            }
        }, this);
        this.shellScriptOnOff.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            if (selected) {
                runConfig.setShellScriptOnOff("true");
            } else {
                runConfig.setShellScriptOnOff("false");
            }
        });
        this.rollbackScriptOnOff.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            if (selected) {
                runConfig.setRollbackScriptOnOff("true");
            } else {
                runConfig.setRollbackScriptOnOff("false");
            }
        });

        this.shellScriptButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RunShellScriptDialog runShellScriptDialog = new RunShellScriptDialog(project, targetDirectoryTextField.getText().trim(), "upgrade.sh", runConfigList);
                runShellScriptDialog.setTitle("通用运行脚本");
                runShellScriptDialog.getRunShellScript().getNameTextField().setText("upgrade.sh");
                runShellScriptDialog.showAndGet();
            }
        });
        this.rollbackScriptButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RunShellScriptDialog runShellScriptDialog = new RunShellScriptDialog(project, targetDirectoryTextField.getText().trim(), "backspace.sh", runConfigList);
                runShellScriptDialog.setTitle("通用回退脚本");
                runShellScriptDialog.getRunShellScript().getNameTextField().setText("backspace.sh");
                runShellScriptDialog.showAndGet();
            }
        });
        this.shellEditScriptButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditScriptDialog runShellScriptDialog = new EditScriptDialog(project, "运行脚本", StringUtils.isNotBlank(shellScriptEditor.getText().trim()) ? shellScriptEditor.getText().trim() : "");
                runShellScriptDialog.setTitle("编辑运行脚本");
                updateShellScript(runShellScriptDialog, shellScriptEditor);
            }
        });
        this.rollbackEditScriptButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditScriptDialog runShellScriptDialog = new EditScriptDialog(project, "回退脚本", StringUtils.isNotBlank(rollbackScriptTextEditor.getText().trim()) ? rollbackScriptTextEditor.getText().trim() : "");
                runShellScriptDialog.setTitle("编辑回退脚本");
                updateShellScript(runShellScriptDialog, rollbackScriptTextEditor);
            }
        });
    }

    private void updateShellScript(EditScriptDialog runShellScriptDialog, HorizontalScrollBarEditor shellScriptEditor) {
        runShellScriptDialog.showAndGet();
        if (runShellScriptDialog.getEditScript().isEdit()) {
            String extension = "sh";
            FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(extension);
            // 使用 virtualFile 创建 Document（IDE 自动处理）
            Document document = FileDocumentManager.getInstance().getDocument(new LightVirtualFile("script.sh", fileType, runShellScriptDialog.getContent()));
            shellScriptEditor.setDocument(document);
            shellScriptEditor.updateUI();
        }
    }

    public void createUIComponents() {
        String extension = "sh";
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(extension);
        // 使用 virtualFile 创建 Document（IDE 自动处理）
        Document document1 = FileDocumentManager.getInstance().getDocument(new LightVirtualFile("script.sh", fileType, ""));
        Document document2 = FileDocumentManager.getInstance().getDocument(new LightVirtualFile("script.sh", fileType, ""));
        Document document3 = FileDocumentManager.getInstance().getDocument(new LightVirtualFile("script.sh", fileType, ""));
        this.shellScriptEditor = new HorizontalScrollBarEditor(document1, project, fileType, false, false);
        this.logeTextEditor = new HorizontalScrollBarEditor(document2, project, fileType, false, false);
        this.rollbackScriptTextEditor = new HorizontalScrollBarEditor(document3, project, fileType, false, false);
        this.shellScript.add(this.shellScriptEditor);
        this.logeText.add(this.logeTextEditor);
        this.rollbackScript.add(this.rollbackScriptTextEditor);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /** Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        root = new JBPanel();
        root.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        workDirectory = new JBPanel();
        workDirectory.setLayout(new BorderLayout(0, 0));
        workDirectory.setMaximumSize(JBUI.size(2147483647, 40));
        workDirectory.setMinimumSize(JBUI.size(0, 40));
        workDirectory.setPreferredSize(JBUI.size(0, 40));
        CellConstraints cc = new CellConstraints();
        root.add(workDirectory, cc.xy(1, 1));
        final JBPanel panel1 = new JBPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        workDirectory.add(panel1, BorderLayout.CENTER);
        targetDirectoryTextField = new JBTextField();
        panel1.add(targetDirectoryTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, JBUI.size(150, -1), null, 0, false));
        final JBPanel panel2 = new JBPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel2.setMaximumSize(JBUI.size(2147483647, 200));
        panel2.setMinimumSize(JBUI.size(24, 200));
        panel2.setPreferredSize(JBUI.size(24, 200));
        root.add(panel2, cc.xy(1, 3));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JBPanel panel3 = new JBPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel2.add(panel3, BorderLayout.CENTER);
        final JBPanel panel4 = new JBPanel();
        panel4.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        panel4.setMaximumSize(JBUI.size(2147483647, 30));
        panel4.setMinimumSize(JBUI.size(24, 30));
        panel4.setPreferredSize(JBUI.size(24, 30));
        panel3.add(panel4, BorderLayout.NORTH);
        final JBPanel panel5 = new JBPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 40, 0, 0), -1, -1));
        panel4.add(panel5, cc.xy(1, 1));
        shellScriptOnOff = new OnOffButton();
        panel5.add(shellScriptOnOff, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        shellScriptButton = new JButton();
        shellScriptButton.setMaximumSize(JBUI.size(30, 30));
        shellScriptButton.setMinimumSize(JBUI.size(30, 30));
        shellScriptButton.setPreferredSize(JBUI.size(30, 30));
        shellScriptButton.setText("");
        shellScriptButton.setToolTipText("查看通用脚本");
        panel4.add(shellScriptButton, cc.xy(3, 1));
        shellEditScriptButton = new JButton();
        shellEditScriptButton.setMaximumSize(JBUI.size(30, 30));
        shellEditScriptButton.setMinimumSize(JBUI.size(30, 30));
        shellEditScriptButton.setPreferredSize(JBUI.size(30, 30));
        shellEditScriptButton.setText("");
        shellEditScriptButton.setToolTipText("编辑");
        panel4.add(shellEditScriptButton, cc.xy(5, 1));
        shellScript = new JBPanel();
        shellScript.setLayout(new BorderLayout(0, 0));
        panel3.add(shellScript, BorderLayout.CENTER);
        rollbackPanel = new JBPanel();
        rollbackPanel.setLayout(new BorderLayout(0, 0));
        rollbackPanel.setMaximumSize(JBUI.size(2147483647, 150));
        rollbackPanel.setMinimumSize(JBUI.size(24, 150));
        rollbackPanel.setPreferredSize(JBUI.size(24, 150));
        root.add(rollbackPanel, cc.xy(1, 5));
        rollbackPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JBPanel panel6 = new JBPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        rollbackPanel.add(panel6, BorderLayout.CENTER);
        final JBPanel panel7 = new JBPanel();
        panel7.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        panel7.setMaximumSize(JBUI.size(2147483647, 30));
        panel7.setMinimumSize(JBUI.size(24, 30));
        panel7.setPreferredSize(JBUI.size(24, 30));
        panel6.add(panel7, BorderLayout.NORTH);
        final JBPanel panel8 = new JBPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 40, 0, 0), -1, -1));
        panel7.add(panel8, cc.xy(1, 1));
        rollbackScriptOnOff = new OnOffButton();
        panel8.add(rollbackScriptOnOff, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        rollbackScriptButton = new JButton();
        rollbackScriptButton.setMaximumSize(JBUI.size(30, 30));
        rollbackScriptButton.setMinimumSize(JBUI.size(30, 30));
        rollbackScriptButton.setPreferredSize(JBUI.size(30, 30));
        rollbackScriptButton.setText("");
        rollbackScriptButton.setToolTipText("查看通用脚本");
        panel7.add(rollbackScriptButton, cc.xy(3, 1));
        rollbackEditScriptButton = new JButton();
        rollbackEditScriptButton.setMaximumSize(JBUI.size(30, 30));
        rollbackEditScriptButton.setMinimumSize(JBUI.size(30, 30));
        rollbackEditScriptButton.setPreferredSize(JBUI.size(30, 30));
        rollbackEditScriptButton.setText("");
        rollbackEditScriptButton.setToolTipText("编辑");
        panel7.add(rollbackEditScriptButton, cc.xy(5, 1));
        rollbackScript = new JBPanel();
        rollbackScript.setLayout(new BorderLayout(0, 0));
        panel6.add(rollbackScript, BorderLayout.CENTER);
        logePanel = new JBPanel();
        logePanel.setLayout(new BorderLayout(0, 0));
        logePanel.setMaximumSize(JBUI.size(2147483647, 70));
        logePanel.setMinimumSize(JBUI.size(24, 70));
        logePanel.setPreferredSize(JBUI.size(24, 70));
        root.add(logePanel, cc.xy(1, 7));
        logePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        logeText = new JBPanel();
        logeText.setLayout(new BorderLayout(0, 0));
        logePanel.add(logeText, BorderLayout.CENTER);
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

    @Override
    public void dispose() {

    }
}
