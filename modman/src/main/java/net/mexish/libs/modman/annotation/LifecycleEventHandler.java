package net.mexish.libs.modman.annotation;

import net.mexish.libs.modman.ModuleLifecycleEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author mexish
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LifecycleEventHandler {

    ModuleLifecycleEvent value();

}
