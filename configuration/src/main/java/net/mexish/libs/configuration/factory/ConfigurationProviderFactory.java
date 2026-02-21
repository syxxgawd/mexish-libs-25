package net.mexish.libs.configuration.factory;

import net.mexish.libs.configuration.provider.impl.JsonConfigurationProvider;
import net.mexish.libs.configuration.provider.impl.PropertiesConfigurationProvider;
import net.mexish.libs.configuration.provider.impl.YamlConfigurationProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 * @version 10/11/2025
 */
public interface ConfigurationProviderFactory {

    @Contract(value = " -> new", pure = true)
    static @NotNull YamlConfigurationProvider createYaml() {
        return new YamlConfigurationProvider();
    }

    @Contract(value = " -> new", pure = true)
    static @NotNull JsonConfigurationProvider createJson() {
        return new JsonConfigurationProvider();
    }

    @Contract(value = " -> new", pure = true)
    static @NotNull PropertiesConfigurationProvider createProps() {
        return new PropertiesConfigurationProvider();
    }

}
