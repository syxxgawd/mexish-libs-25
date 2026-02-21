package net.mexish.libs.configuration.type.impl;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.mexish.libs.configuration.type.AbstractConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author mexish
 * @version 10/11/2025
 */
@NoArgsConstructor
public final class YamlConfiguration extends AbstractConfiguration {

    public YamlConfiguration(final @Nullable YamlConfiguration parent,
                             final @NonNull Map<?, ?> map) {
        super(parent, map);
    }

}
