package net.mexish.libs.command.argument;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.mexish.libs.command.annotation.ArgumentSignature;
import net.mexish.libs.command.argument.impl.TypedBooleanArgument;
import net.mexish.libs.command.argument.impl.TypedDoubleArgument;
import net.mexish.libs.command.argument.impl.TypedIntArgument;
import net.mexish.libs.command.argument.impl.TypedStringArgument;
import net.mexish.libs.command.context.CommandContext;
import net.mexish.libs.command.exception.CommandArgumentException;
import net.mexish.libs.command.exception.CommandArgumentParseException;
import net.mexish.libs.commons.logging.Logging;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SuppressWarnings("all")
public final class ArgumentManager {

    Map<String, Supplier<TypedArgument<?>>> argumentFactories = new LinkedHashMap<>();
    Map<String, Supplier<TypedArgument<?>>[]> bakedMethodSignatures = new LinkedHashMap<>();

    public ArgumentManager() {
        registerType("int", TypedIntArgument::new);
        registerType("string", TypedStringArgument::new);
        registerType("bool", TypedBooleanArgument::new);
        registerType("double", TypedDoubleArgument::new);
    }

    public void registerType(final @NonNull String typename,
                             final @NonNull Supplier<TypedArgument<?>> supplier) {
        argumentFactories.put(typename, supplier);
    }

    public boolean bakeArgumentSignature(final @NonNull String name,
                                         final @NonNull Method method) {
        val signature = method.getDeclaredAnnotation(ArgumentSignature.class);

        // TODO move to an exception
        if (signature == null) {
            return false;
        }

        val split = signature.value().split(" ");
        Supplier<TypedArgument<?>>[] bakedSignature = new Supplier[0];

        for (val type : split) {
            val factory = argumentFactories.get(type);

            if (factory == null) {
                Logging.IMP.warn("[ArgumentManager] | Invalid argument signature on method: {} | Type `{}` isn't valid",
                        method.getName(),
                        type);
                //System.out.println("[Commands/ArgumentManager] | Invalid argument signature on method: " + method.getName() + " | Type `" + type + "` isn't registered");
                continue;
            }

            bakedSignature = ArrayUtils.add(bakedSignature, factory);
        }

        bakedMethodSignatures.put(name, bakedSignature);
        return true;
    }

    public void checkArguments(final @NonNull CommandContext ctx) throws CommandArgumentException {
        val name = ctx.getWrapper().getFullName();
        val args = ctx.getArguments();
        val signature = bakedMethodSignatures.get(name);

        if (signature == null) {
            return;
        }

        if (args.length < signature.length) {
            throw CommandArgumentException.of(name, "Not enough arguments! Expected: {}", signature.length);
        }

        val argumentInstances = new TypedArgument<?>[signature.length];

        for (var i = 0; i < signature.length; i++) {
            val typedArg = signature[i].get();
            val rawArg = args[i];

            try {
                typedArg.parse(rawArg);
                argumentInstances[i] = typedArg;
            } catch (final Exception e) {
                throw CommandArgumentParseException.of(name, "Invalid argument `{}` at pos {}: {}", rawArg, i, e.getMessage());
            }
        }

        ctx.setTypedArgumentCache(argumentInstances);
    }

    public @NotNull Set<String> tabComplete(final @NonNull String commandFullName,
                                            final @NonNull String[] args) {
        val signature = bakedMethodSignatures.get(commandFullName);

        if (signature == null) {
            return Collections.emptySet();
        }

        var currentIndex = args.length - 1;

        if (currentIndex >= signature.length) {
            return Collections.emptySet();
        }

        val typedArg = signature[currentIndex].get();

        return new LinkedHashSet<>(typedArg.tabComplete(args[currentIndex]));
    }

//    public void checkArguments(final @NonNull CommandContext ctx) throws CommandArgumentException {
//        val name = ctx.getWrapper().getFullName();
//        val args = ctx.getArguments();
//        val signature = bakedMethodSignatures.get(name);
//
//        if (signature == null) {
//            return;
//        }
//
//        if (args.length < signature.length) {
//            throw CommandArgumentException.of(name, "Argument length is shorter than the command's signature length! [{} < {}]", args.length, signature.length);
//        }
//
//        // sig length not args since we drop all the following args
//        for (var i = 0; i < signature.length; i++) {
//            val typedArg = signature[i];
//            val arg = args[i];
//
//            if (typedArg == null || arg == null) {
//                continue;
//            }
//
//            try {
//                typedArg.parse(arg);
//            } catch (final NumberFormatException e) {
//                throw CommandArgumentParseException.of(name, "Invalid argument type!", i, "", typedArg);
//            }
//        }
//    }

    // argument caching with execution id
//    private void cacheArgument(final String commandName, final TypedArgument<?> arg) {
//        val list = argumentCache.getOrDefault()
//    }

}
