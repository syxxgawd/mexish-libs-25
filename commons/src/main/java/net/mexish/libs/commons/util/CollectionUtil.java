package net.mexish.libs.commons.util;

import lombok.experimental.UtilityClass;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class CollectionUtil {

    public <K, V> Map<K, V> createConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    public <E> Set<E> createConcurrentSet() {
        return ConcurrentHashMap.newKeySet();
    }

    public <K, V> Map<K, Set<V>> createConcurrentSetMultimap() {
        return new ConcurrentHashMap<>();
    }

    public <K, V> Map<K, V> createConcurrentWeakMap() {
        return new ModernReferenceMap<>(ModernReferenceMap.RefType.WEAK, ModernReferenceMap.RefType.STRONG);
    }

    // REPLACES: ConcurrentReferenceHashMap (Soft/Strong)
    public <K, V> Map<K, V> createConcurrentSoftMap() {
        return new ModernReferenceMap<>(ModernReferenceMap.RefType.SOFT, ModernReferenceMap.RefType.STRONG);
    }

    // REPLACES: ConcurrentReferenceHashSet
    public <E> Set<E> createConcurrentWeakSet() {
        return Collections.newSetFromMap(createConcurrentWeakMap());
    }
}