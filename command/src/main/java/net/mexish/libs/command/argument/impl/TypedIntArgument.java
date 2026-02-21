package net.mexish.libs.command.argument.impl;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.command.argument.TypedArgument;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public final class TypedIntArgument implements TypedArgument<Integer> {
    int value;

    @Override
    public Integer get() {
        return value;
    }

    @Override
    public void set(final @NonNull Integer value) {
        this.value = value;
    }

    @Contract(pure = true)
    @SneakyThrows
    @Override
    public @NotNull Integer parse(final @NonNull String value) {
        val parsed = Integer.parseInt(value);
        set(parsed);
        return parsed;
    }

}
