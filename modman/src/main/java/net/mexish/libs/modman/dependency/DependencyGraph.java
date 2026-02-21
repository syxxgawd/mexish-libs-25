package net.mexish.libs.modman.dependency;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class DependencyGraph {

    Map<String, Set<String>> dependencies = new ConcurrentHashMap<>();

    public void registerDependency(final @NonNull String module,
                                   final @NonNull String dependency) {
        dependencies.computeIfAbsent(module, _ -> ConcurrentHashMap.newKeySet()).add(dependency);
    }

    public boolean dependsOn(final @NonNull String module,
                             final @NonNull String dependency) {
        Set<String> deps = dependencies.get(module);
        return deps != null && deps.contains(dependency);
    }

    public boolean isCircular(final @NonNull String module,
                              final @NonNull String dependency) {
        return checkCycle(module, dependency, new HashSet<>());
    }

    private boolean checkCycle(final @NonNull String target,
                               final @NonNull String current,
                               final @NonNull Set<String> visited) {
        if (!visited.add(current)) return false;

        val neighbors = dependencies.get(current);

        if (neighbors == null || neighbors.isEmpty()) {
            return false;
        }

        if (neighbors.contains(target)) {
            return true;
        }

        for (val neighbor : neighbors) {
            if (checkCycle(target, neighbor, visited)) return true;
        }

        return false;
    }
}