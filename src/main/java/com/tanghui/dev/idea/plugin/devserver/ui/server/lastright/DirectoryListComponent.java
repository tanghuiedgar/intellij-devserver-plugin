package com.tanghui.dev.idea.plugin.devserver.ui.server.lastright;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.tanghui.dev.idea.plugin.devserver.DevServerBundle;
import com.tanghui.dev.idea.plugin.devserver.data.model.ServerHostFileModel;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import static com.tanghui.dev.idea.plugin.devserver.utils.SizeFormatUtils.formatBinary;

@Getter
public class DirectoryListComponent {
    private JBPanel<?> root;
    private JBList<ServerHostFileModel> directoryList;
    private JBPanel<?> serverFileOperationToolbar;

    private final Project project;

    public DirectoryListComponent(Project project) {
        this.project = project;
        DefaultListModel<ServerHostFileModel> model = new DefaultListModel<>();
        directoryList.setModel(model);
        directoryList.setFixedCellHeight(JBUI.scale(60));
        directoryList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JBPanel<?> fileRoot = new JBPanel<>();
            // 设置一列两行
            fileRoot.setLayout(new GridLayoutManager(
                    2,   // rows：2 行
                    1,   // columns：1 列
                    JBUI.emptyInsets(),
                    -1,  // hGap
                    -1   // vGap
            ));
            JBPanel<?> comp1 = new JBPanel<>(new BorderLayout());
            // 第一个组件：紧贴左边
            JBLabel firstComp = new JBLabel();
            firstComp.setText(value.getPermissions());
            firstComp.setForeground(new JBColor(new Color(7, 177, 5), new Color(7, 177, 5)));
            comp1.add(firstComp);
            comp1.add(firstComp, BorderLayout.WEST);

            // 右侧容器：统一加左边距
            JBPanel<?> restPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 25, 0));

            // 后续组件加到 restPanel
            JBLabel secondComp = new JBLabel();
            secondComp.setText(value.getGroup() + " : " + value.getOwner());
            secondComp.setForeground(new JBColor(new Color(-16722177), new Color(-16722177)));
            comp1.add(secondComp);
            JBLabel thirdComp = new JBLabel();
            thirdComp.setText(value.getMTime());
            thirdComp.setForeground(new JBColor(new Color(-16722177), new Color(-16722177)));
            comp1.add(thirdComp);
            JBLabel label = new JBLabel();
            label.setText(formatBinary(value.getFileSize()));
            label.setForeground(new JBColor(new Color(-16722177), new Color(-16722177)));
            comp1.add(label);
            restPanel.add(secondComp);
            restPanel.add(thirdComp);
            restPanel.add(label);

            comp1.setOpaque(false);
            restPanel.setOpaque(false);

            comp1.add(restPanel, BorderLayout.CENTER);
            fileRoot.add(comp1, new GridConstraints(
                    0, 0,  // row, column
                    1, 1,  // rowSpan, colSpan
                    GridConstraints.ANCHOR_CENTER,
                    GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_FIXED,
                    null, null, null, 0, false
            ));
            JBPanel<?> comp2 = new JBPanel<>();
            comp2.setOpaque(false);
            comp2.setLayout(new BorderLayout());
            comp2.setPreferredSize(JBUI.size(-1, 30));
            JBLabel fileNameLabel = new JBLabel();
            fileNameLabel.setText(DevServerBundle.INSTANCE.message("file") + "：");
            fileNameLabel.setPreferredSize(JBUI.size(40, -1));
            JBLabel fileLabel = new JBLabel();
            fileLabel.setText(value.getFileName());
            fileLabel.setForeground(new JBColor(new Color(0xF9, 0x72, 0x00), new Color(0xF9, 0x72, 0x00)));
            fileLabel.setHorizontalAlignment(SwingConstants.LEFT);
            comp2.add(fileNameLabel, BorderLayout.WEST);
            comp2.add(fileLabel, BorderLayout.CENTER);
            JSeparator separator = new JSeparator();
            separator.setOpaque(false);
            comp2.add(separator, BorderLayout.SOUTH);
            fileRoot.add(comp2, new GridConstraints(
                    1, 0,  // row, column
                    1, 1,  // rowSpan, colSpan
                    GridConstraints.ANCHOR_CENTER,
                    GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                    GridConstraints.SIZEPOLICY_FIXED,
                    null, null, null, 0, false
            ));

            if (isSelected) {
                fileRoot.setBackground(list.getSelectionBackground());
                fileRoot.setForeground(list.getSelectionForeground());
            } else {
                fileRoot.setBackground(list.getBackground());
                fileRoot.setForeground(list.getForeground());
            }
            //  关键：否则背景不生效
            fileRoot.setOpaque(true);
            return fileRoot;
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        root = new JBPanel();
        root.setLayout(new BorderLayout(0, 0));
        serverFileOperationToolbar = new JBPanel();
        serverFileOperationToolbar.setLayout(new BorderLayout(0, 0));
        serverFileOperationToolbar.setMinimumSize(JBUI.size(0, 30));
        serverFileOperationToolbar.setPreferredSize(JBUI.size(0, 30));
        root.add(serverFileOperationToolbar, BorderLayout.NORTH);
        final JBScrollPane jBScrollPane1 = new JBScrollPane();
        root.add(jBScrollPane1, BorderLayout.CENTER);
        directoryList = new JBList();
        directoryList.setEnabled(true);
        jBScrollPane1.setViewportView(directoryList);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}
