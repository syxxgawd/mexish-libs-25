package net.mexish.libs.command.executor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubcommandExecutor {

    /**
     * The subcommand's name
     */
    String value();

    boolean async() default false;

}
