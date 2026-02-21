package net.mexish.libs.configuration.adapter;

import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 * @version 09/11/2025
 */
public interface TypeAdapterHolder {

    void typeAdapter(final @NotNull Class<?> typeClazz,
                     final @NotNull TypeAdapter<?> adapter);

    void unregisterTypeAdapter(final @NotNull Class<?> typeClazz);

    TypeAdapter<?> getTypeAdapter(final @NotNull Class<?> typeClazz);

}
