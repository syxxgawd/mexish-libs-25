package net.mexish.libs.modman;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.configuration.factory.ConfigurationProviderFactory;
import net.mexish.libs.configuration.provider.ConfigurationProvider;
import net.mexish.libs.configuration.type.impl.JsonConfiguration;
import net.mexish.libs.modman.exception.ModuleLifecycleHandlerException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Data
public final class ModuleWrapper {

    Map<ModuleLifecycleEvent, MethodHandle> moduleHandlers
            = new LinkedHashMap<>();

    ModuleMeta meta;

    Path modulePath;

    ConfigurationProvider<JsonConfiguration> provider = ConfigurationProviderFactory.createJson();
    AtomicReference<JsonConfiguration> config = new AtomicReference<>(provider.createConfiguration());

    ModuleManager core;

    public ModuleWrapper(final @NonNull ModuleMeta meta,
                         final @NonNull Path modulePath,
                         final @NonNull ModuleManager core) {
        this.meta = meta;
        this.modulePath = modulePath;
        this.core = core;

        if (Files.notExists(modulePath) || !Files.isDirectory(modulePath)) {
            return;
        }

        try {
            val configPath = modulePath.resolve("config.json");

            if (Files.notExists(configPath)) {
                //Files.createFile(configPath);
                return;
            }

            this.config.set(provider.provide(configPath));
        } catch (final Throwable ignored) {
        }
    }

    public void handle(final @NonNull ModuleLifecycleEvent event) throws ModuleLifecycleHandlerException {
        val handler = moduleHandlers.get(event);

        if (handler == null) {
            return;
        }

        try {
            handler.invokeExact();
        } catch (final Throwable t) {
            throw ModuleLifecycleHandlerException.withCause(event, t);
        }
    }

    public void registerHandler(final @NonNull ModuleLifecycleEvent event,
                                final @NonNull MethodHandle handle) {
        moduleHandlers.put(event, handle);
    }

    public JsonConfiguration getConfig() {
        return config.get();
    }

    public void saveDefaultConfig() throws IOException {
        if (Files.notExists(modulePath) || !Files.isDirectory(modulePath)) {
            Files.createDirectories(modulePath);
        }

        val configPath = modulePath.resolve("config.json");

        if (Files.exists(configPath)) {
            return;
        }

        val in = getResourceAsStream("config.json");

        if (in == null) {
            throw new IllegalStateException("config.json not found in module jar file");
        }

        provider.write(provider.provide(in), configPath);
        reloadConfig();
    }

    public void saveConfig() throws IOException {
        provider.write(modulePath.resolve("config.json"));
    }

    public void reloadConfig() throws IOException {
        val configPath = modulePath.resolve("config.json");

        if (Files.notExists(configPath)) {
            //Files.createFile(configPath);
            return;
        }

        this.config.set(provider.provide(configPath));
    }

    public InputStream getResourceAsStream(final @NonNull String name) {
        return core.getLoaders().get(meta.getName().toLowerCase()).getResourceAsStream(name);
    }

}
