package net.mexish.libs.configuration.processor;

import lombok.NonNull;
import lombok.val;
import net.mexish.libs.configuration.type.Configuration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author mexish
 * @version 09/11/2025
 */
public interface ConfigurationProcessor<T extends Configuration> {

    // processing
    default T process(final @NonNull Map<?, ?> loadedMap) {
        val config = processMap(loadedMap);
        postProcess(config);
        return config;
    }

    default void postProcess(final @NotNull T configuration) {
        // no-op
    }

    T processMap(final @NotNull Map<?, ?> loadedMap);

}
