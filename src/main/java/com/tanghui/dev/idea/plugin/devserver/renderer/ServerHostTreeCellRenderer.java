package com.tanghui.dev.idea.plugin.devserver.renderer;

import com.intellij.ui.JBColor;
import com.tanghui.dev.idea.plugin.devserver.tree.ServerHostTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * @BelongsPackage: com.tanghui.ui.serverHost
 * @Author: 唐煇
 * @CreateTime: 2024-11-27-上午9:03
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class ServerHostTreeCellRenderer extends DefaultTreeCellRenderer {

    public ServerHostTreeCellRenderer() {
        super();
        setBorderSelectionColor(null);
        setOpaque(false); // 让 renderer 自身不绘制背景
        setBackgroundNonSelectionColor(null);
        setBackgroundSelectionColor(null);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        // Let the parent class handle the basic rendering
        //super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        String  stringValue = tree.convertValueToText(value, selected,
                expanded, leaf, row, hasFocus);
        setText(stringValue);

        if (value instanceof ServerHostTreeNode hostTreeNode) {
            setIcon(hostTreeNode.getIcon());
        }
        super.selected = selected;
        super.hasFocus = hasFocus;
        this.setBackground(new JBColor(new Color(0x1d2021), new Color(0x1d2021)));
        return this;
    }

}
