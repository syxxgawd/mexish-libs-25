package net.mexish.libs.configuration.provider.impl;

import lombok.NonNull;
import lombok.val;
import net.mexish.libs.configuration.factory.ConfigurationFactory;
import net.mexish.libs.configuration.provider.AbstractConfigurationProvider;
import net.mexish.libs.configuration.type.AbstractConfiguration;
import net.mexish.libs.configuration.type.impl.PropertiesConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * @author mexish
 * @version 10/11/2025
 */
public final class PropertiesConfigurationProvider extends AbstractConfigurationProvider<PropertiesConfiguration> {

    @Override
    public PropertiesConfiguration provide(@NonNull Path path) throws IOException {
        val properties = new Properties();
        properties.load(Files.newBufferedReader(path));
        return process(properties);
    }

    @Override
    public PropertiesConfiguration provide(final @NonNull String contents) throws IOException {
        val properties = new Properties();
        properties.load(new StringReader(contents));
        return process(properties);
    }

    @Override
    public PropertiesConfiguration provide(final @NonNull InputStream in) throws IOException {
        val properties = new Properties();
        properties.load(in);
        return process(properties);
    }

    @Override
    public void write(final @NonNull Writer writer) throws IOException {
        val properties = new Properties();
        properties.putAll(root.serialize());
        properties.store(writer, "");
    }

    @Contract(" -> new")
    @Override
    public @NotNull PropertiesConfiguration createConfiguration() {
        return ConfigurationFactory.createProps();
    }

    @Contract("_, _ -> new")
    @Override
    public @NotNull PropertiesConfiguration createConfiguration(final @NonNull PropertiesConfiguration parent,
                                                                final @NonNull Map<?, ?> preLoadedMap) {
        return ConfigurationFactory.createProps(parent, preLoadedMap);
    }
}
