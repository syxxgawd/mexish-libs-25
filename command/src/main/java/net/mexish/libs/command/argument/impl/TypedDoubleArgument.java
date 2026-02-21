package net.mexish.libs.command.argument.impl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.mexish.libs.command.argument.TypedArgument;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public final class TypedDoubleArgument implements TypedArgument<Double> {
    double value;

    @Override
    public Double get() {
        return value;
    }

    @Override
    public void set(final @NonNull Double value) {
        this.value = value;
    }

    @Override
    public @NotNull Double parse(final @NonNull String value) {
        val d = Double.parseDouble(value);
        set(d);
        return d;
    }
}
