package com.bhond.debugger.action;

import com.bhond.debugger.backend.GraphBuilder;
import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.Value;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.Objects;

public class DiagramAction extends XDebuggerTreeActionBase {

    @Override
    protected void perform(XValueNodeImpl node, @NotNull String nodeName, AnActionEvent e) {
        XValue value = node.getValueContainer();
        String actualGraph = null;
        if (value instanceof JavaValue) {
            ValueDescriptorImpl descriptor = ((JavaValue) value).getDescriptor();
            Value nodeValue = descriptor.getValue();
            actualGraph = new GraphBuilder().generateDOT(nodeValue);
        }
        Objects.requireNonNull(actualGraph, "node is " + value);
        setClipboardData(actualGraph);
    }

    private static void setClipboardData(String value) {
        CopyPasteManager.getInstance()
                .setContents(new StringSelection(value));
    }

}

