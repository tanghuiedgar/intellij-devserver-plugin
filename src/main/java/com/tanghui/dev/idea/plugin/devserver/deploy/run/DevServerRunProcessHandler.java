package com.tanghui.dev.idea.plugin.devserver.deploy.run;

import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.deploy.run
 * @Author: 唐煇
 * @CreateTime: 2024-07-19 10:35
 * @Description: 描述类的主要功能和用途。
 * @Version: 1.0
 */
public class DevServerRunProcessHandler extends ProcessHandler {
    @Override
    protected void destroyProcessImpl() {
        notifyProcessTerminated(130);
    }

    @Override
    protected void detachProcessImpl() {
        notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
        return false;
    }

    @Override
    public @Nullable OutputStream getProcessInput() {
        return null;
    }

    public void endExecution() {
        notifyProcessTerminated(0);
    }
}
