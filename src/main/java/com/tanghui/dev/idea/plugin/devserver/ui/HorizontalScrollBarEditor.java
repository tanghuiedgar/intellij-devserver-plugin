package com.tanghui.dev.idea.plugin.devserver.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.HorizontalScrollBarEditorCustomization;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.ui
 * @Author: 唐煇
 * @CreateTime: 2024-05-27 14:03
 * @Description: 描述类的主要功能和用途。
 * @Version: 1.0
 */
public class HorizontalScrollBarEditor extends EditorTextField {

    public HorizontalScrollBarEditor(Document document, Project project, FileType fileType, boolean isViewer, boolean oneLineMode) {
        super(document, project, fileType, isViewer, oneLineMode);
        setNewDocumentAndFileType(fileType, document);
        setFont(EditorUtil.getEditorFont());
    }

    @Override
    protected @NotNull EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        HorizontalScrollBarEditorCustomization.ENABLED.customize(editor);
        EditorSettings settings = editor.getSettings();
        settings.setFullLineHeightCursor(true);
        settings.setFoldingOutlineShown(true);
        settings.setAllowSingleLogicalLineFolding(true);
        settings.setLineNumbersShown(true);
        settings.setWrapWhenTypingReachesRightMargin(true);
        JScrollPane scrollPane = editor.getScrollPane();
        JScrollBar yBar = scrollPane.getVerticalScrollBar();
        yBar.setValue(0);
        JScrollBar xBar = scrollPane.getHorizontalScrollBar();
        xBar.setValue(0);
        editor.setVerticalScrollbarVisible(true);
        return editor;
    }
}
