package net.mexish.libs.configuration.provider;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.configuration.adapter.TypeAdapter;
import net.mexish.libs.configuration.exception.LoaderException;
import net.mexish.libs.configuration.type.AbstractConfiguration;
import net.mexish.libs.configuration.type.Configuration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mexish
 * @version 10/11/2025
 */
@FieldDefaults(level = AccessLevel.PROTECTED)
@Getter
@Setter
public abstract class AbstractConfigurationProvider<K extends AbstractConfiguration> implements ConfigurationProvider<K> {

    final Map<Class<?>, TypeAdapter<?>> typeAdapters = new ConcurrentHashMap<>();

    K root;

    @Override
    public void postProcess(final @NonNull K configuration) {
        this.root = configuration;
    }

    @Override
    public <T> T provide(final @NonNull Configuration configuration,
                         final @NonNull Class<?> typeClazz) {
        val adapter = getTypeAdapter(typeClazz);

        if (adapter == null) {
            throw new IllegalStateException("Adapter for class `" + typeClazz.getSimpleName() + "` isn't registered!");
        }

        return (T) adapter.read(configuration);
    }

    @Override
    public <T> void write(final @NonNull T object,
                          final @NonNull Writer writer,
                          final @NonNull Class<?> typeClazz) throws IOException {
        val adapter = (TypeAdapter<T>) getTypeAdapter(typeClazz);

        if (adapter == null) {
            throw new IllegalStateException("Adapter for class `" + typeClazz.getSimpleName() + "` isn't registered!");
        }

        adapter.write(root, object);
        write(writer);
    }

    @Override
    public <T> void write(final @NonNull Configuration section,
                          final @NonNull T object,
                          final @NonNull Writer writer,
                          final @NonNull Class<?> typeClazz) throws IOException {
        val adapter = (TypeAdapter<T>) getTypeAdapter(typeClazz);

        if (adapter == null) {
            throw new IllegalStateException("Adapter for class `" + typeClazz.getSimpleName() + "` isn't registered!");
        }

        adapter.write(section, object);
        write(writer);
    }

    @Override
    public void write(final @NonNull K section,
                      final @NonNull Writer writer) throws IOException {
        root = section;
        write(writer);
    }

    @Override
    public void typeAdapter(final @NonNull Class<?> typeClazz,
                            final @NonNull TypeAdapter<?> adapter) {
        typeAdapters.put(typeClazz, adapter);
    }

    @Override
    public @NotNull K processMap(final @NonNull Map<?, ?> loadedMap) {
        val root = createConfiguration();

        try {
            for (val entry : loadedMap.entrySet()) {
                val key = String.valueOf(entry.getKey());
                val value = entry.getValue();

                if (value instanceof Map<?, ?> castMap) {
                    root.append(key, createConfiguration(root, castMap));
                } else {
                    root.set(key, value);
                }
            }
        } catch (ClassCastException ex) {
            throw LoaderException.with(ex);
        }

        return root;
    }

    @Override
    public void unregisterTypeAdapter(final @NonNull Class<?> typeClazz) {
        typeAdapters.remove(typeClazz);
    }

    @Override
    public TypeAdapter<?> getTypeAdapter(final @NonNull Class<?> typeClazz) {
        return typeAdapters.get(typeClazz);
    }

}
