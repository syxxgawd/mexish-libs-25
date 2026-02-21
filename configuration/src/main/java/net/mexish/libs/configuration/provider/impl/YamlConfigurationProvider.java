package net.mexish.libs.configuration.provider.impl;

import lombok.NonNull;
import lombok.val;
import net.mexish.libs.configuration.exception.LoaderException;
import net.mexish.libs.configuration.factory.ConfigurationFactory;
import net.mexish.libs.configuration.provider.AbstractConfigurationProvider;
import net.mexish.libs.configuration.type.AbstractConfiguration;
import net.mexish.libs.configuration.type.Configuration;
import net.mexish.libs.configuration.type.impl.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author mexish
 * @version 10/11/2025
 */
public final class YamlConfigurationProvider extends AbstractConfigurationProvider<YamlConfiguration> {

    private static final DumperOptions DEFAULT_DUMPER_OPT = new DumperOptions();

    static {
        DEFAULT_DUMPER_OPT.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        DEFAULT_DUMPER_OPT.setIndent(4);
    }

    @Override
    public @NotNull YamlConfiguration provide(final @NonNull Path path) throws IOException {
        try {
            return process(new Yaml().load(Files.newBufferedReader(path)));
        } catch (final ClassCastException ex) {
            throw LoaderException.withReason("Top level of YAML configuration is not a map.");
        }
    }

    @Override
    public @NotNull YamlConfiguration provide(@NonNull String contents) {
        return process(new Yaml().load(contents));
    }

    @Override
    public YamlConfiguration provide(final @NonNull InputStream in) throws IOException {
        return process(new Yaml().load(in));
    }

    @Override
    public void write(final @NonNull Writer writer) {
        new Yaml(DEFAULT_DUMPER_OPT).dump(root.serialize(), writer);
    }

    @Contract(" -> new")
    @Override
    public @NotNull YamlConfiguration createConfiguration() {
        return ConfigurationFactory.createYaml();
    }

    @Contract("_, _ -> new")
    @Override
    public @NotNull YamlConfiguration createConfiguration(final @NonNull YamlConfiguration parent,
                                                          final @NonNull Map<?, ?> preLoadedMap) {
        return new YamlConfiguration(parent, preLoadedMap);
    }

}
