package net.mexish.libs.modman;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.commons.util.IndexMap;
import net.mexish.libs.commons.util.ThreadUtils;
import net.mexish.libs.configuration.factory.ConfigurationProviderFactory;
import net.mexish.libs.modman.annotation.LifecycleEventHandler;
import net.mexish.libs.modman.dependency.DependencyGraph;
import net.mexish.libs.modman.exception.ModuleLoadingException;
import net.mexish.libs.modman.loader.ModuleClassLoader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.FailedException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@NoArgsConstructor
@Getter
public final class ModuleManager {

    AtomicReference<Path> moduleDir = new AtomicReference<>(Paths.get(".", "modules"));

    Map<String, ModuleMeta> gatheredModules = new ConcurrentHashMap<>();
    Map<String, Boolean> moduleStatus = new ConcurrentHashMap<>();

    Set<String> loadedDependencies = ConcurrentHashMap.newKeySet();
    AtomicReference<DependencyGraph> dependencyGraph = new AtomicReference<>(new DependencyGraph());

    Map<String, ModuleClassLoader> loaders = new ConcurrentHashMap<>();

    IndexMap<ModuleWrapper> moduleWrappers = IndexMap.<ModuleWrapper>newMap()
            .registerIndex(String.class, mod -> mod.getMeta().getName().toLowerCase())
            .registerIndex(Class.class, mod -> {
                try {
                    return loaders.get(mod.getMeta().getName().toLowerCase()).loadClass(mod.getMeta().getMain());
                } catch (final Throwable ignored) {
                }

                return null;
            });

    public ModuleManager(final Path moduleDir) {
        this.moduleDir.set(moduleDir);
    }

    public void load() throws Exception {
        try (val scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            for (val meta : gatheredModules.values()) {
                if (loadedDependencies.contains(meta.getName())) {
                    continue;
                }

                scope.fork(() -> {
                    loadModule(meta);
                    return null;
                });
            }

            scope.join();
        } catch (FailedException e) {
            throw new ModuleLoadingException("Failed to load modules", e.getCause());
        }

        loadedDependencies.clear();
    }

    @Contract("_ -> new")
    private @Nullable ModuleWrapper loadModule(final @NonNull String name) {
        val meta = gatheredModules.get(name.toLowerCase());

        if (meta == null) {
            return null;
        }

        return loadModule(meta);
    }

    @Contract("_ -> new")
    private ModuleWrapper loadModule(final @NonNull ModuleMeta meta) {
        val name = meta.getName().toLowerCase();

        synchronized (loaders) {
            if (loadedDependencies.contains(name)) {
                return moduleWrappers.get(String.class, name);
            }

            val depends = meta.getDependencies();

            if (depends != null) {
                for (val depend : depends) {
                    if (dependencyGraph.get().isCircular(name, depend)) {
                        continue;
                    }

                    loadModule(depend.toLowerCase());
                    dependencyGraph.get().registerDependency(name, depend);
                }
            }

            val wrapper = new ModuleWrapper(meta, moduleDir.get().resolve(meta.getName()), this);
            val lookup = MethodHandles.lookup();
            val originalLoader = Thread.currentThread().getContextClassLoader();

            try {
                val loader = new ModuleClassLoader(new URL[]{meta.getPath().toUri().toURL()}, originalLoader);

                if (depends != null) {
                    for (val depend : depends) {
                        val depLoader = loaders.get(depend.toLowerCase());

                        if (depLoader == null) {
                            continue;
                        }

                        loader.link(depLoader);
                    }
                }

                Thread.currentThread().setContextClassLoader(loader);

                val mainClazz = loader.loadClass(meta.getMain());

                for (val method : mainClazz.getDeclaredMethods()) {
                    val handlerAnnotation = method.getDeclaredAnnotation(LifecycleEventHandler.class);

                    if (handlerAnnotation == null) {
                        continue;
                    }

                    wrapper.registerHandler(handlerAnnotation.value(), lookup.unreflect(method));
                }

                loaders.put(name, loader);
                loadedDependencies.add(name);
                moduleWrappers.store(wrapper);

                wrapper.handle(ModuleLifecycleEvent.LOAD);
                return wrapper;
            } catch (final Throwable t) {
                val loader = loaders.get(name);

                if (loader != null) {
                    try {
                        loader.close();
                    } catch (IOException ignored) {}
                }

                moduleStatus.put(name, false);
                throw ModuleLoadingException.withCause("Throwable caught while loading module `" + name + "`", t);
            } finally {
                Thread.currentThread().setContextClassLoader(originalLoader);
            }
        }
    }

    public void init(final @NonNull ModuleWrapper wrapper,
                     final @NonNull ModuleClassLoader loader,
                     final @NonNull ModuleMeta meta) {
        val name = meta.getName().toLowerCase();

        ThreadUtils.doWithOtherLoader(loader, () -> {
                    wrapper.handle(ModuleLifecycleEvent.INIT);
                    moduleStatus.put(name, true);
                },
                t -> {
                    moduleStatus.put(name, false);
                    throw ModuleLoadingException.withCause("Couldn't initialize module", t);
                });
    }

    public void init(final @NonNull ModuleWrapper wrapper) {
        val meta = wrapper.getMeta();
        val name = meta.getName().toLowerCase();
        val loader = loaders.get(name);

        if (loader == null) {
            return;
        }

        init(wrapper, loader, meta);
    }

    public void init(final @NonNull String name) {
        val wrapper = moduleWrappers.get(String.class, name.toLowerCase());

        if (wrapper == null) {
            return;
        }

        init(wrapper);
    }

    public void init() throws Exception {
        try (val scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            for (val name : gatheredModules.keySet()) {
                scope.fork(() -> {
                    init(name);
                    return null;
                });
            }

            scope.join();
        } catch (FailedException e) {
            throw new ModuleLoadingException("Failed to init modules", e.getCause());
        }
    }

    public void shutdown() throws Exception {
        try (val scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            for (val name : gatheredModules.keySet()) {
                scope.fork(() -> {
                    shutdown(name);
                    return null;
                });
            }

            scope.join();
        } catch (FailedException e) {
            throw new ModuleLoadingException("Failed to shutdown modules", e.getCause());
        }
    }

    public void shutdown(final @NonNull ModuleWrapper wrapper,
                         final @NonNull ModuleClassLoader loader,
                         final @NonNull ModuleMeta meta) {
        val name = meta.getName().toLowerCase();

        ThreadUtils.doWithOtherLoader(loader, () -> wrapper.handle(ModuleLifecycleEvent.SHUTDOWN),
                t -> {
                    throw ModuleLoadingException.withCause("Couldn't shutdown module", t);
                });

        moduleStatus.put(name, false);
    }

    public void shutdown(final @NonNull ModuleWrapper wrapper) {
        val meta = wrapper.getMeta();
        val name = meta.getName().toLowerCase();
        val loader = loaders.get(name);

        if (loader == null) {
            return;
        }

        shutdown(wrapper, loader, meta);
    }

    public void shutdown(final @NonNull String name) {
        val wrapper = moduleWrappers.get(String.class, name.toLowerCase());

        if (wrapper == null) {
            return;
        }

        shutdown(wrapper);
    }

    public void unload(final @NonNull ModuleWrapper wrapper,
                       final @NonNull ModuleClassLoader loader,
                       final @NonNull ModuleMeta meta) {
        shutdown(wrapper, loader, meta);
        cleanModule(meta, wrapper);
    }

    public void unload(final @NonNull ModuleWrapper wrapper) {
        val meta = wrapper.getMeta();
        val name = meta.getName().toLowerCase();
        val loader = loaders.get(name);

        if (loader == null) {
            return;
        }

        unload(wrapper, loader, meta);
    }

    public void unload(final @NonNull String name) {
        val wrapper = moduleWrappers.get(String.class, name.toLowerCase());

        if (wrapper == null) {
            return;
        }

        unload(wrapper);
    }

    public void reload(final @NonNull ModuleWrapper wrapper,
                       final @NonNull ModuleClassLoader loader,
                       final @NonNull ModuleMeta meta) {
        unload(wrapper, loader, meta);
        init(loadModule(meta));
    }

    public void reload(final @NonNull ModuleWrapper wrapper) {
        val meta = wrapper.getMeta();
        val name = meta.getName().toLowerCase();
        val loader = loaders.get(name);

        if (loader == null) {
            return;
        }

        reload(wrapper, loader, meta);
    }

    public void reload(final @NonNull String name) {
        val wrapper = moduleWrappers.get(String.class, name.toLowerCase());

        if (wrapper == null) {
            return;
        }

        reload(wrapper);
    }

    public void gather() {
        gather(moduleDir.get());
    }

    public void gather(final @NonNull Path folder) {
        gatheredModules.clear();

        if (!Files.isDirectory(folder)) {
            throw ModuleLoadingException.withReason("The path provided for gathering modules must be a directory!");
        }

        try (val listed = Files.list(folder)) {
            listed.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .forEach(path -> {
                        try (val jar = new JarFile(path.toFile())) {
                            val metaEntry = jar.getJarEntry("meta.json");

                            if (metaEntry == null) {
                                throw ModuleLoadingException.withReason("Module `" + path.getFileName() + "` does not have a meta.json!");
                            }

                            try (val in = new BufferedInputStream(jar.getInputStream(metaEntry))) {
                                val provider = ConfigurationProviderFactory.createJson();
                                val metaRoot = provider.provide(in);
                                val name = metaRoot.getString("name");

                                if (name == null) {
                                    throw ModuleLoadingException.withReason("Module `" + path.getFileName() + "`'s meta.json doesn't contain a `name` field!");
                                }

                                val meta = ModuleMeta.of(
                                        name,
                                        metaRoot.getString("main"),
                                        metaRoot.getString("version"),
                                        metaRoot.getString("author"),
                                        path
                                );

                                val depends = metaRoot.getStringList("depend");
                                if (depends != null) {
                                    meta.setDependencies(depends.toArray(new String[0]));
                                }

                                if (meta.anyRequiredNull()) {
                                    throw ModuleLoadingException.withReason("Module `" + name + "` is missing fields in it's meta.json!");
                                }

                                gatheredModules.put(name.toLowerCase(), meta);
                            }
                        } catch (final IOException ex) {
                            throw ModuleLoadingException.withCause("Failed to load module.", ex);
                        }
                    });
        } catch (final IOException ex) {
            throw ModuleLoadingException.withCause("Failed to list files.", ex);
        }
    }

    public void cleanModule(final @NonNull ModuleMeta meta, final @NonNull ModuleWrapper wrapper) {
        val name = meta.getName().toLowerCase();
        loadedDependencies.remove(name);
        moduleWrappers.remove(wrapper);

        val loader = loaders.remove(name);

        if (loader != null) {
            try {
                loader.close();
            } catch (final Throwable ignored) {}
        }

        moduleStatus.remove(name);
    }

    @Contract(pure = true)
    public @NotNull Set<Map.Entry<String, Boolean>> getModuleStatuses() {
        return moduleStatus.entrySet();
    }

    public @Nullable ModuleWrapper getWrapper(final @NonNull Class<?> clazz) {
        val mapped = moduleWrappers.get(Class.class, clazz);

        if (mapped != null) {
            return mapped;
        }

        for (val wrapper : moduleWrappers.values()) {
            if (!wrapper.getMeta().getMain().equalsIgnoreCase(clazz.getCanonicalName())) {
                continue;
            }

            return wrapper;
        }

        return null;
    }

    public @Nullable ModuleWrapper getWrapperForCaller() {
        return getWrapper(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
    }

}