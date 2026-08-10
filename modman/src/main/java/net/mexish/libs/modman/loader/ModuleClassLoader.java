package net.mexish.libs.modman.loader;

import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
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

    public void link(final @NonNull ModuleClassLoader loader) {
        this.dependencies.add(loader);
    }

    public void unlink(final @NonNull ModuleClassLoader loader) {
        this.dependencies.remove(loader);
    }

    @Override
    protected @NotNull Class<?> loadClass(final @NonNull String name,
                                          final boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            var c = findLoadedClass(name);

            if (c == null && isSystemClass(name)) {
                try {
                    c = getParent().loadClass(name);
                } catch (ClassNotFoundException ignored) {}
            }

            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException ignored) {}
            }

            if (c == null) {
                for (val dependency : dependencies) {
                    try {
                        c = dependency.loadClass(name, false);

                        if (c != null) {
                            break;
                        }
                    } catch (ClassNotFoundException ignored) {}
                }
            }

            if (c == null) {
                try {
                    c = getParent().loadClass(name);
                } catch (ClassNotFoundException ignored) {}
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

    private boolean isSystemClass(final String name) {
        return name.startsWith("java.") ||
                name.startsWith("javax.") ||
                name.startsWith("sun.") ||
                name.startsWith("jdk.") ||
                name.startsWith("org.xml.") ||
                name.startsWith("org.w3c.");
    }

}
