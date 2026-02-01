package com.tanghui.dev.idea.plugin.devserver.ui.model;

import com.tanghui.dev.idea.plugin.devserver.utils.CollectionUtil;
import lombok.Getter;
import lombok.Setter;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.ui.model
 * @Author: 唐煇
 * @CreateTime: 2025-09-16-14:30
 * @Description: DevServer自定义表格模型。
 * @Version: v1.0
 */
public class DevServerTableModel extends DefaultTableModel {
    // 默认不可编辑
    private boolean isEditable = false;

    // 不可编辑列
    @Setter
    @Getter
    private List<Integer> columns;

    // 不可编辑行
    @Setter
    @Getter
    private List<Integer> rows;


    public DevServerTableModel(Object[] columnNames, int rowCount) {
        this(columnNames, rowCount, new ArrayList<>(), new ArrayList<>());
    }

    public DevServerTableModel(Object[] columnNames, int rowCount, List<Integer> columns, List<Integer> rows) {
        super(columnNames, rowCount);
        this.columns = columns;
        this.rows = rows;
    }

    public DevServerTableModel(Object[] columnNames, int rowCount, List<Integer> columns) {
        this(columnNames, rowCount, columns, new ArrayList<>());
    }

    // 控制哪些单元格可以编辑
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (CollectionUtil.isNotEmpty(this.columns)) {
            for (Integer column : this.columns) {
                if (column == columnIndex) {
                    return false;
                }
            }
        }
        if (CollectionUtil.isNotEmpty(this.rows)) {
            for (Integer row : this.rows) {
                if (row == rowIndex) {
                    return false;
                }
            }
        }
        return isEditable; // 根据 isEditable 标志返回可编辑性
    }

    // 设置是否可编辑
    public void setEditable(boolean editable) {
        this.isEditable = editable;
        fireTableDataChanged(); // 触发表格更新
    }

}
