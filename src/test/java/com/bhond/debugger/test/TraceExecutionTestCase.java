package com.bhond.debugger.test;

import com.intellij.debugger.DebuggerTestCase;
import com.intellij.debugger.impl.OutputChecker;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import org.jetbrains.annotations.NotNull;


public class TraceExecutionTestCase extends DebuggerTestCase {

    private final Logger LOG = Logger.getInstance(getClass());


    @Override
    protected OutputChecker initOutputChecker() {
        return null;
    }

    // this does not work also
    /*@Override
    protected String getTestAppPath() {
        return new File("testData").getAbsolutePath();
    }*/

    // this does not work
    @Override
    protected String getTestAppPath() {
        return "testData";
    }

    protected void doTest(boolean isResultNull) {
        try {
            final String className = getTestName(false);
            LOG.info("test name is + " + className);
            doTestImpl(isResultNull, className);
        } catch (Exception e) {
            throw new AssertionError("exception thrown", e);
        }
    }

    private void doTestImpl(boolean isResultNull, @NotNull String className)
            throws ExecutionException {
        LOG.info("Test started: " + getTestName(false));
        createLocalProcess(className);
        final XDebugSession session = getDebuggerSession().getXDebugSession();
        assertNotNull(session);

        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
                try {
                    sessionPausedImpl();
                } catch (Throwable t) {
                    println("Exception caught: " + t + ", " + t.getMessage(), ProcessOutputTypes.SYSTEM);

                    //noinspection CallToPrintStackTrace
                    t.printStackTrace();

                    resume();
                }
            }

            private void sessionPausedImpl() {
                printContext(getDebugProcess().getDebuggerContext());
            }

            private void resume() {
                ApplicationManager.getApplication()
                        .invokeLater(session::resume);
            }
        }, getTestRootDisposable());
    }

}
