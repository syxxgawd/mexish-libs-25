package net.mexish.libs.configuration.factory;

import lombok.NonNull;
import net.mexish.libs.configuration.type.Configuration;
import net.mexish.libs.configuration.type.impl.JsonConfiguration;
import net.mexish.libs.configuration.type.impl.PropertiesConfiguration;
import net.mexish.libs.configuration.type.impl.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author mexish
 * @version 10/11/2025
 */
public interface ConfigurationFactory {

    static @NotNull YamlConfiguration createYaml() {
        return new YamlConfiguration();
    }

    static @NotNull YamlConfiguration createYaml(final @NonNull YamlConfiguration parent,
                                                 final @NonNull Map<?, ?> preLoadedMap) {
        return new YamlConfiguration(parent, preLoadedMap);
    }

    static @NotNull JsonConfiguration createJson() {
        return new JsonConfiguration();
    }

    static @NotNull JsonConfiguration createJson(final @NonNull JsonConfiguration parent,
                                                 final @NonNull Map<?, ?> preLoadedMap) {
        return new JsonConfiguration(parent, preLoadedMap);
    }

    static @NotNull PropertiesConfiguration createProps() {
        return new PropertiesConfiguration();
    }

    static @NotNull PropertiesConfiguration createProps(final @NonNull PropertiesConfiguration parent,
                                                       final @NonNull Map<?, ?> preLoadedMap) {
        return new PropertiesConfiguration(parent, preLoadedMap);
    }

}
