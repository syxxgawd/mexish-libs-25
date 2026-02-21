package net.mexish.libs.command.argument.impl;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.command.argument.TypedArgument;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public final class TypedStringArgument implements TypedArgument<String> {
    String value;

    @Override
    public String get() {
        return value;
    }

    @Override
    public void set(final @NonNull String value) {
        this.value = value;
    }

    @Override
    public String parse(final @NonNull String value) {
        set(value);
        return value;
    }
}
