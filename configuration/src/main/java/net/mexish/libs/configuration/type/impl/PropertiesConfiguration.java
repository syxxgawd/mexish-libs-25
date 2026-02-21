package net.mexish.libs.configuration.type.impl;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.mexish.libs.configuration.type.AbstractConfiguration;
import net.mexish.libs.configuration.type.Configuration;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author mexish
 * @version 10/11/2025
 */
@NoArgsConstructor
public final class PropertiesConfiguration extends AbstractConfiguration {

    public PropertiesConfiguration(final @Nullable PropertiesConfiguration parent,
                                   final @NonNull Map<?, ?> map) {
        super(parent, map);
    }

    @Override
    public @Nullable Configuration getConfiguration(final @NonNull String name) {
        throw new UnsupportedOperationException();
    }

}
