package net.mexish.libs.command.annotation;

import net.mexish.libs.command.executor.CommandExecutor;
import net.mexish.libs.command.executor.SubcommandExecutor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation, that signifies the declaration of a subcommand inside a command,
 * not equal to a subcommand executor as this only applies to {@link ElementType#TYPE}
 * and is efficient when creating multiple subcommands inside each other, see also {@link SubcommandExecutor}.
 * This also uses the default {@link CommandExecutor} annotation for declaring the subcommand logic.
 *
 * @author mexish
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NamedSubcommand {

    String value();

}
