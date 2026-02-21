package net.mexish.libs.modman;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Data
public final class ModuleMeta {
    String name;
    String main;
    String version;
    String author;
    @NonFinal String[] dependencies;
    Path path;

    public boolean anyRequiredNull() {
        return name == null
                || main == null
                || version == null
                || author == null;
    }

    @Contract("_, _, _, _, _ -> new")
    public static @NotNull ModuleMeta of(final String name,
                                         final String main,
                                         final String version,
                                         final String author,
                                         final @NonNull Path path) {
        return new ModuleMeta(name, main, version, author, path);
    }
}
