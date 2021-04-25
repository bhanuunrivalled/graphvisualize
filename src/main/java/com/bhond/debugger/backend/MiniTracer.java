package com.bhond.debugger.backend;

import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.Value;
import com.sun.jdi.VoidType;

public class MiniTracer {

    /**
     * TODO check replace with DebugUtils?
     * @param v
     * @return
     */
    public static boolean isPrimitive(Value v) {
        return v instanceof PrimitiveValue || v instanceof VoidType;
    }


}
