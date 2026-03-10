package net.mexish.libs.impl;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public final class Impls {

    private final Map<Class<?>, Class<?>> IMPLEMENTATIONS = new HashMap<>();

    private Class<?> getImplementation(final @NonNull Class<?> cls) {
        var impl = IMPLEMENTATIONS.get(cls);

        if (impl == null) {
            impl = readImplementation(cls);

            if (impl == null) {
                throw new IllegalStateException("Cannot load impl for " + cls.getName());
            }

            IMPLEMENTATIONS.put(cls, impl);
        }

        return impl;
    }

    private Class<?> readImplementation(final @NonNull Class<?> cls) {
        try (val is = cls.getClassLoader().getResourceAsStream("META-INF/impl/" + cls.getName())) {
            if (is == null) return null;

            try (val br = new BufferedReader(new InputStreamReader(is))) {
                return br.lines()
                        .map(line -> {
                            int separator = line.indexOf(':');
                            val implName = line.substring(0, separator);
                            val priority = line.substring(separator + 1);

                            return new ImplModel(implName, ImplPriority.valueOf(priority));
                        })
                        .min(Comparator.naturalOrder())
                        .flatMap(model -> {
                            try {
                                return Optional.of(Class.forName(model.getImplName()));
                            } catch (Exception e) {
                                return Optional.empty();
                            }
                        })
                        .orElse(null);
            }
        } catch (IOException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final @NonNull Class<T> cls) {
        try {
            val impl = getImplementation(cls);

            try {
                return (T) impl.getDeclaredField("INSTANCE").get(null);
            } catch (NoSuchFieldException e) {
                return (T) impl.getConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
