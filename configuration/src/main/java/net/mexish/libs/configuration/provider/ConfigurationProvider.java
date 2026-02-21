package net.mexish.libs.configuration.provider;

import lombok.NonNull;
import lombok.val;
import net.mexish.libs.configuration.adapter.TypeAdapterHolder;
import net.mexish.libs.configuration.processor.ConfigurationProcessor;
import net.mexish.libs.configuration.provider.impl.YamlConfigurationProvider;
import net.mexish.libs.configuration.type.Configuration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author mexish
 * @version 10/11/2025
 */
public interface ConfigurationProvider<T extends Configuration> extends TypeAdapterHolder, ConfigurationProcessor<T> {

    default T provide(final @NotNull File file) throws IOException {
        return provide(file.toPath());
    }

    T provide(final @NotNull Path path) throws IOException;

    T provide(final @NotNull String contents) throws IOException;

    T provide(final @NotNull InputStream in) throws IOException;

    default <R> R provide(final @NotNull File file,
                          final @NotNull Class<?> typeClazz) throws IOException {
        return provide(file.toPath(), typeClazz);
    }

    default <R> R provide(final @NotNull Path path,
                          final @NotNull Class<?> typeClazz) throws IOException {
        return provide(provide(path), typeClazz);
    }

    default <R> R provide(final @NotNull String contents,
                          final @NotNull Class<?> typeClazz) throws IOException {
        return provide(provide(contents), typeClazz);
    }

    <R> R provide(final @NotNull Configuration configuration,
                  final @NotNull Class<?> typeClazz);

    /**
     * Writes the root {@link Configuration} using the specified {@link Writer}.
     * @param writer The writer to use
     */
    void write(final @NotNull Writer writer) throws IOException;

    <R> void write(final @NotNull R object,
                   final @NotNull Writer writer,
                   final @NotNull Class<?> typeClazz) throws IOException;

    <R> void write(final @NotNull Configuration section,
                   final @NotNull R object,
                   final @NotNull Writer writer,
                   final @NotNull Class<?> typeClazz) throws IOException;

    default String write() throws IOException {
        val writer = new StringWriter();
        write(writer);
        return writer.toString();
    }

    default String write(final @NonNull Object o,
                         final @NonNull Class<?> typeClazz) throws IOException {
        val writer = new StringWriter();
        write(o, writer, typeClazz);
        return writer.toString();
    }

    default String write(final @NonNull T section,
                         final @NonNull Object o,
                         final @NonNull Class<?> typeClazz) throws IOException {
        val writer = new StringWriter();
        write(section, o, writer, typeClazz);
        return writer.toString();
    }

    default void write(final @NonNull T section, final @NonNull Path path) throws IOException {
        write(section, Files.newBufferedWriter(path));
    }

    default void write(final @NonNull T section, final @NonNull File file) throws IOException {
        write(section, file.toPath());
    }

    void write(final @NotNull T section, final @NotNull Writer writer) throws IOException;

    default void write(final @NonNull Path path) throws IOException {
        write(Files.newBufferedWriter(path));
    }

    /**
     * The use of this is discouraged, use {@link #write(Path)}
     */
    default void write(final @NonNull File file) throws IOException {
        write(file.toPath());
    }

    T getRoot();

    void setRoot(final @NotNull T configuration);

    T createConfiguration();

    T createConfiguration(final @NotNull T parent, final @NotNull Map<?, ?> preLoadedMap);

}
