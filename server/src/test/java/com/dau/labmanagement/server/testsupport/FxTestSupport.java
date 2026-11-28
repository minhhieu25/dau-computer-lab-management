package com.dau.labmanagement.server.testsupport;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared JavaFX toolkit bootstrap for Server UI tests.
 *
 * <p>The toolkit can be started only once per JVM, so every test class calls
 * {@link #ensureStarted()} and the toolkit keeps running with
 * {@code implicitExit=false} until the Gradle test worker exits.</p>
 */
public final class FxTestSupport {

    private FxTestSupport() {
    }

    /** Starts the JavaFX toolkit on first use; safe to call from every test class. */
    public static void ensureStarted() {
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyStarted) {
            Platform.setImplicitExit(false);
        } catch (UnsupportedOperationException toolkitAlreadyInitialized) {
            Platform.setImplicitExit(false);
        }
    }

    /** Runs an action on the JavaFX thread and waits for its result. */
    public static <T> T call(Callable<T> action) {
        ensureStarted();
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.call());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(15, TimeUnit.SECONDS)) {
                throw new AssertionError("JavaFX action did not finish within 15 s");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for JavaFX", exception);
        }
        if (failure.get() != null) {
            throw new AssertionError("JavaFX action failed", failure.get());
        }
        return result.get();
    }

    /** Runs an action on the JavaFX thread and waits for it. */
    public static void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }
}
