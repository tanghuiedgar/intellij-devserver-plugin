package com.tanghui.dev.idea.plugin.devserver.utils;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

import java.util.List;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.utils
 * @Author: 唐煇
 * @CreateTime: 2026-01-22-16:10
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class DevServerUtil {

    public static ActionToolbar createActionToolBar(String places, boolean horizontal, List<AnAction> anActions) {
        // AnAction[] consoleActions = createConsoleActionArr();
        ActionManager actionManager = ActionManager.getInstance();
        //只保留console自身的事件
        // anActions.addAll(Arrays.asList(consoleActions));
        DefaultActionGroup actions = new DefaultActionGroup();
        ActionToolbar actionToolbar = actionManager.createActionToolbar(places, actions, horizontal);
        for (AnAction action : anActions) {
            actions.add(action);
        }
        return actionToolbar;
    }

}
