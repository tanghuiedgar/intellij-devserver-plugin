package com.tanghui.dev.idea.plugin.devserver.listener;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.listener
 * @Author: 唐煇
 * @CreateTime: 2025-03-12-11:08
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public abstract class DevServerDocumentListener implements DocumentListener {

    @Override
    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

}
