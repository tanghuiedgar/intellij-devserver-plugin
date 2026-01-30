package com.tanghui.dev.idea.plugin.devserver.utils;

import com.tanghui.dev.idea.plugin.devserver.tree.ServerHostTreeNode;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.jetbrains.annotations.NotNull;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.utils
 * @Author: 唐煇
 * @CreateTime: 2026-01-23-08:55
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class JTreeUtil {
    /**
     * 折叠全部节点
     * */
    public static void collapseAllExceptRoot(JTree tree) {
        Object rootObj = tree.getModel().getRoot();
        if (!(rootObj instanceof TreeNode root)) return;
        TreePath rootPath = new TreePath(((ServerHostTreeNode) root).getPath());

        // 从根的每个子节点开始，递归折叠
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode child = root.getChildAt(i);
            collapseDeep(tree, new TreePath(((ServerHostTreeNode) child).getPath()));
        }
        // 保证根处于展开状态（可选）
        tree.expandPath(rootPath);
    }

    /**
     * 递归折叠某个节点
     * */
    public static void collapseDeep(JTree tree, TreePath path) {
        if (path == null) return;
        Object node = path.getLastPathComponent();
        TreeModel model = tree.getModel();
        int count = model.getChildCount(node);
        for (int i = 0; i < count; i++) {
            Object child = model.getChild(node, i);
            collapseDeep(tree, path.pathByAddingChild(child));
        }
        tree.collapsePath(path);
    }

    /**
     * 展开全部节点
     * */
    public static void expandAll(JTree tree) {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row++);
        }
    }

    /**
     * 递归展开某个节点
     * */
    public static void expandNode(JTree tree, TreePath path) {
        if (path == null) return;
        Object node = path.getLastPathComponent();
        var model = tree.getModel();
        int childCount = model.getChildCount(node);
        for (int i = 0; i < childCount; i++) {
            Object child = model.getChild(node, i);
            expandNode(tree, path.pathByAddingChild(child));
        }
        tree.expandPath(path);
    }

    public static boolean hasChildren(@NotNull JTree tree, @NotNull TreePath path) {
        Object node = path.getLastPathComponent();
        TreeModel model = tree.getModel();
        return !model.isLeaf(node);
    }
}
