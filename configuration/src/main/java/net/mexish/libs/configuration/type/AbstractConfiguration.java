package net.mexish.libs.configuration.type;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mexish
 * @version 10/11/2025
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@SuppressWarnings("all")
@Getter
public abstract class AbstractConfiguration implements SerializableConfiguration {

    @NonFinal
    @Getter
    @Setter
    Configuration parent;

    /* Object can be either an underlying Configuration (section) or a param */
    Map<String, Object> map;

    public AbstractConfiguration(final @Nullable Configuration parent,
                                 final @NonNull Map<?, ?> map) {
        this.parent = parent;
        this.map = new LinkedHashMap<>();

        for (val entry : map.entrySet()) {
            this.map.put(String.valueOf(entry.getKey()), entry.getValue());
        }
    }

    /**
     * for the root configuration ONLY
     */
    public AbstractConfiguration() {
        this.parent = null;
        this.map = new LinkedHashMap<>();
    }

    @Override
    public void appendInternal(final @NonNull String name, final @NonNull Configuration other) {
        other.setParent(this);
        map.put(name, other);
    }

    @Override
    public @NotNull Map<?, ?> toSerializableMap() {
        val newMap = new LinkedHashMap<String, Object>();

        for (val entry : map.entrySet()) {
            val key = entry.getKey();
            val value = entry.getValue();

            if (!(value instanceof Configuration)) {
                newMap.put((String) key, value);
                continue;
            }

            newMap.put((String) key, ((SerializableConfiguration) value).toSerializableMap());
        }

        return newMap;
    }

    @Override
    public @Nullable Object get(@NonNull String name) {
        return map.get(name);
    }

    @Override
    public void set(final @NonNull String name,
                    final @Nullable Object value) {
        map.put(name, value);
    }

}
