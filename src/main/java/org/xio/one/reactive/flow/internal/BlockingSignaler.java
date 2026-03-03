package org.xio.one.reactive.flow.internal;

import java.util.concurrent.atomic.AtomicBoolean;

public class BlockingSignaler {

    private final AtomicBoolean ready = new AtomicBoolean(true);

    public boolean canThreadProceed() {
        return ready.get();
    }

    public void stopThreadProceeding() {
        ready.set(false);
    }

    public void startProceedingThread() {
        ready.set(true);
    }
}