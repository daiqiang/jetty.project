//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.util.thread;

import java.util.concurrent.Callable;

import org.eclipse.jetty.util.Callback;

/**
 * <p>A task (typically either a {@link Runnable} or {@link Callable}
 * that declares how it will behave when invoked:</p>
 * <ul>
 * <li>blocking, the invocation will certainly block (e.g. performs blocking I/O)</li>
 * <li>non-blocking, the invocation will certainly <strong>not</strong> block</li>
 * <li>either, the invocation <em>may</em> block</li>
 * </ul>
 *
 * <p>
 * Static methods and are provided that allow the current thread to be tagged
 * with a {@link ThreadLocal} to indicate if it has a blocking invocation type.
 * </p>
 */
public interface Invocable
{
    static ThreadLocal<Boolean> __nonBlocking = new ThreadLocal<>();

    /**
     * <p>The behavior of an {@link Invocable} when it is invoked.</p>
     * <p>Typically, {@link Runnable}s or {@link Callback}s declare their
     * invocation type; this information is then used by the code that should
     * invoke the {@code Runnable} or {@code Callback} to decide whether to
     * invoke it directly, or submit it to a thread pool to be invoked by
     * a different thread.</p>
     */
    enum InvocationType
    {
        /**
         * <p>Invoking the {@link Invocable} may block the invoker thread,
         * and the invocation may be performed immediately (possibly blocking
         * the invoker thread) or deferred to a later time, for example
         * by submitting the {@code Invocable} to a thread pool.</p>
         * <p>This invocation type is suitable for {@code Invocable}s that
         * call application code, for example to process an HTTP request.</p>
         */
        BLOCKING,
        /**
         * <p>Invoking the {@link Invocable} does not block the invoker thread,
         * and the invocation may be performed immediately in the invoker thread.</p>
         * <p>This invocation type is suitable for {@code Invocable}s that
         * call implementation code that is guaranteed to never block the
         * invoker thread.</p>
         */
        NON_BLOCKING,
        /**
         * <p>Invoking the {@link Invocable} may block the invoker thread,
         * but the invocation cannot be deferred to a later time, differently
         * from {@link #BLOCKING}.</p>
         * <p>This invocation type is suitable for {@code Invocable}s that
         * themselves perform the non-deferrable action in a non-blocking way,
         * thus advancing a possibly stalled system.</p>
         */
        EITHER
    }

    /**
     * <p>A task with an {@link InvocationType}.</p>
     */
    interface Task extends Invocable, Runnable
    {
    }

    /**
     * <p>A {@link Runnable} decorated with an {@link InvocationType}.</p>
     */
    class ReadyTask implements Task
    {
        private final InvocationType type;
        private final Runnable task;

        public ReadyTask(InvocationType type, Runnable task)
        {
            this.type = type;
            this.task = task;
        }

        @Override
        public void run()
        {
            task.run();
        }

        @Override
        public InvocationType getInvocationType()
        {
            return type;
        }

        @Override
        public String toString()
        {
            return String.format("%s@%x[%s|%s]", getClass().getSimpleName(), hashCode(), type, task);
        }
    }

    /**
     * <p>Creates a {@link Task} from the given InvocationType and Runnable.</p>
     *
     * @param type the InvocationType
     * @param task the Runnable
     * @return a new Task
     */
    public static Task from(InvocationType type, Runnable task)
    {
        return new ReadyTask(type, task);
    }

    /**
     * Test if the current thread has been tagged as non blocking
     *
     * @return True if the task the current thread is running has
     * indicated that it will not block.
     */
    public static boolean isNonBlockingInvocation()
    {
        return Boolean.TRUE.equals(__nonBlocking.get());
    }

    /**
     * Invoke a task with the calling thread, tagged to indicate
     * that it will not block.
     *
     * @param task The task to invoke.
     */
    public static void invokeNonBlocking(Runnable task)
    {
        Boolean wasNonBlocking = __nonBlocking.get();
        try
        {
            __nonBlocking.set(Boolean.TRUE);
            task.run();
        }
        finally
        {
            __nonBlocking.set(wasNonBlocking);
        }
    }

    static InvocationType combine(InvocationType it1, InvocationType it2)
    {
        if (it1 != null && it2 != null)
        {
            if (it1 == it2)
                return it1;
            if (it1 == InvocationType.EITHER)
                return it2;
            if (it2 == InvocationType.EITHER)
                return it1;
        }
        return InvocationType.BLOCKING;
    }

    /**
     * Get the invocation type of an Object.
     *
     * @param o The object to check the invocation type of.
     * @return If the object is an Invocable, it is coerced and the {@link #getInvocationType()}
     * used, otherwise {@link InvocationType#BLOCKING} is returned.
     */
    public static InvocationType getInvocationType(Object o)
    {
        if (o instanceof Invocable)
            return ((Invocable)o).getInvocationType();
        return InvocationType.BLOCKING;
    }

    /**
     * @return The InvocationType of this object
     */
    default InvocationType getInvocationType()
    {
        return InvocationType.BLOCKING;
    }
}
