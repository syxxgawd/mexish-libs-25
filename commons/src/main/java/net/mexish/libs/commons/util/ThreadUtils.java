package net.mexish.libs.commons.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.function.Consumer;

/**
 * @author mexish
 */
@UtilityClass
public final class ThreadUtils {

    public void doWithOtherLoader(final @NonNull Thread thread,
                                  final @NonNull ClassLoader loader,
                                  final @NonNull Runnable task,
                                  final Consumer<Throwable> onException) {
        val originalLoader = thread.getContextClassLoader();

        try {
            thread.setContextClassLoader(loader);
            task.run();
        } catch (final Throwable t) {
            if (onException == null) {
                return;
            }

            onException.accept(t);
        } finally {
            thread.setContextClassLoader(originalLoader);
        }
    }

    public void doWithOtherLoader(final @NonNull ClassLoader loader,
                                  final @NonNull Runnable task,
                                  final Consumer<Throwable> onException) {
        doWithOtherLoader(Thread.currentThread(), loader, task, onException);
    }

    public void doWithOtherLoader(final @NonNull ClassLoader loader, final @NonNull Runnable task) {
        doWithOtherLoader(loader, task, null);
    }

}
