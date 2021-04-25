package com.bhond.debugger.backend;

@FunctionalInterface
public interface ArrayElementAttributeProvider {
    String getAttribute(Object array, int index);
}
