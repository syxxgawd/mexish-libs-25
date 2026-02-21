package net.mexish.libs.configuration.provider.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import lombok.NonNull;
import net.mexish.libs.configuration.factory.ConfigurationFactory;
import net.mexish.libs.configuration.provider.AbstractConfigurationProvider;
import net.mexish.libs.configuration.type.impl.JsonConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
public final class JsonConfigurationProvider extends AbstractConfigurationProvider<JsonConfiguration> {

    private static final JSONWriter.Feature[] DEFAULT_FEATURES = new JSONWriter.Feature[] {
            JSONWriter.Feature.PrettyFormat
    };

    @Override
    public @NotNull JsonConfiguration provide(final @NonNull Path path) throws IOException {
        return process(JSON.parseObject(Files.newBufferedReader(path)));
    }

    @Override
    public @NotNull JsonConfiguration provide(@NonNull String contents) {
        return process(JSON.parseObject(contents));
    }

    @Override
    public JsonConfiguration provide(final @NonNull InputStream in) throws IOException {
        return process(JSON.parseObject(in));
    }

    @Override
    public void write(final @NonNull Writer writer) throws IOException {
        writer.write(JSON.toJSONString(root.serialize(), DEFAULT_FEATURES));
        writer.flush();
        writer.close();
    }

    @Contract(" -> new")
    @Override
    public @NotNull JsonConfiguration createConfiguration() {
        return ConfigurationFactory.createJson();
    }

    @Contract("_, _ -> new")
    @Override
    public @NotNull JsonConfiguration createConfiguration(final @NonNull JsonConfiguration parent,
                                                          final @NonNull Map<?, ?> preLoadedMap) {
        return new JsonConfiguration(parent, preLoadedMap);
    }

}
