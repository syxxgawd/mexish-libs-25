package net.mexish.libs.configuration.adapter;

import net.mexish.libs.configuration.type.Configuration;
import org.jetbrains.annotations.NotNull;

// TODO typeadapter precompilation
/**
 * @author mexish
 * @version 11/11/2025
 */
public interface TypeAdapter<T> {

    T read(final @NotNull Configuration reader);

    void write(final @NotNull Configuration writer, final @NotNull T object);

}
