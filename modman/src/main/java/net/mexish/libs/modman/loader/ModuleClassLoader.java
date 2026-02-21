package net.mexish.libs.modman.loader;

import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class ModuleClassLoader extends URLClassLoader {

    private final Set<ModuleClassLoader> dependencies = new CopyOnWriteArraySet<>();

    public ModuleClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Registers a neighbor module as a dependency.
     * This module will be searched when a class is not found locally.
     */
    public void link(final @NonNull ModuleClassLoader loader) {
        this.dependencies.add(loader);
    }

    @Override
    protected Class<?> loadClass(final @NonNull String name,
                                 final boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            var c = findLoadedClass(name);

            if (c == null) {
                try {
                    c = super.loadClass(name, false);
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (c != null) {
                return c;
            }

            for (val dependency : dependencies) {
                try {
                    c = dependency.loadClass(name, false);

                    if (c == null) {
                        continue;
                    }

                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (c == null) {
                throw new ClassNotFoundException(name);
            }

            if (resolve) {
                resolveClass(c);
            }

            return c;
        }
    }

    @Override
    public void close() throws IOException {
        dependencies.clear();
        super.close();
    }

}
