package com.tanghui.dev.idea.plugin.devserver.tree;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.tree
 * @Author: 唐煇
 * @CreateTime: 2024-11-21-上午11:48
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
@Getter
@Setter
public class ServerHostTreeNode extends DefaultMutableTreeNode {
    private String name;
    private Icon icon;
    private boolean isChildNodes;

    public ServerHostTreeNode(Object userObject, Icon icon) {
        this(userObject, icon, false);
    }

    public ServerHostTreeNode(Object userObject, Icon icon, boolean isChildNodes) {
        super(userObject);
        this.name = userObject.toString();
        this.isChildNodes = isChildNodes;
        this.icon = icon;
    }
}
