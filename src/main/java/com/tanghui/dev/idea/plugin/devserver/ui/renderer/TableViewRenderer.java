package com.tanghui.dev.idea.plugin.devserver.ui.renderer;

import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;


public class TableViewRenderer extends JTextArea implements TableCellRenderer {
    public TableViewRenderer() {
        setLineWrap(true);
        setWrapStyleWord(true);
        setBorder(JBUI.Borders.empty(10));
    }
    // DefaultTableCellRenderer
    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object obj,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        if(isSelected){
            //设置表格选中的那一行的颜色
            setBackground(UIManager.getColor("Tree.selectionBackground"));
            setForeground(UIUtil.getTableForeground());
        }else{
            setBackground(UIUtil.getTableBackground());
            setForeground(UIUtil.getTableForeground());
        }

        // 计算当下行的最佳高度
        int maxPreferredHeight = 35;
        for (int i = 0; i < jtable.getColumnCount(); i++) {
            setText("" + jtable.getValueAt(row, i));
            setSize(jtable.getColumnModel().getColumn(column).getWidth(), 0);
            maxPreferredHeight = Math.max(maxPreferredHeight, getPreferredSize().height);
        }

        if (jtable.getRowHeight(row) != maxPreferredHeight) {
            jtable.setRowHeight(row, maxPreferredHeight);
        }
        // 少了这行则处理器瞎忙
        setFont(jtable.getFont());
        setText(obj == null ? "" : obj.toString());
        return this;
    }
}
