package com.cmcc.newcalllib.tool.thread;

/**
 * Callback of the task's result
 */
public interface TaskCallback<T, P> {
    /**
     * process result of task
     *
     * @param type task result type
     * @return process return type
     */
    P process(T type);
}