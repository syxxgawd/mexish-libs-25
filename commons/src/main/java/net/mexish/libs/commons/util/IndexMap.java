package net.mexish.libs.commons.util;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import net.mexish.libs.commons.util.ModernReferenceMap.RefType;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class IndexMap<Instance> {

    final Map<Class<?>, Index> indexes = new ConcurrentHashMap<>();

    public static <Instance> IndexMap<Instance> newMap() {
        return new BasicIndexMap<>();
    }

    public abstract Set<Instance> values();
    public abstract int size();

    public boolean isEmpty() {
        return size() == 0;
    }

    public <T> IndexMap<Instance> registerIndex(final @NonNull Class<T> cls,
                                                final @NonNull Function<Instance, ?> fun) {
        indexes.put(cls, new HashIndex<>(fun));
        return this;
    }

    public <T> IndexMap<Instance> registerMultiIndex(final @NonNull Class<T> cls,
                                                     final @NonNull Function<Instance, Collection<?>> fun) {
        indexes.put(cls, new MultiIndex<>(fun));
        return this;
    }

    public IndexMap<Instance> registerIntIndex(final @NonNull ToIntFunction<Instance> fun) {
        indexes.put(int.class, new IntIndex<>(fun));
        return this;
    }

    public void unregisterIndex(final @NonNull Class<?> cls) {
        indexes.remove(cls);
    }

    public <T> Instance get(final @NonNull Class<T> cls, final @NonNull T object) {
        val index = indexes.get(cls);

        if (index instanceof ObjectIndex) {
            return ((ObjectIndex<Instance>) index).get(object);
        }

        throw new IllegalStateException("Index not found or type mismatch: " + cls.getSimpleName());
    }

    public Instance get(final int i) {
        val index = indexes.get(int.class);

        if (index instanceof IntIndex) {
            return ((IntIndex<Instance>) index).get(i);
        }

        throw new IllegalStateException("Int index not registered");
    }

    public <T> Set<Instance> getAll(final @NonNull Class<T> cls, final @NonNull T object) {
        val index = indexes.get(cls);

        if (index instanceof MultiIndex) {
            return ((MultiIndex<Instance>) index).get(object);
        }

        throw new IllegalStateException("MultiIndex not found for: " + object.getClass().getSimpleName());
    }

    public boolean contains(final int i) {
        return get(i) != null;
    }

    public <T> boolean contains(final @NonNull Class<T> cls, final @NonNull T object) {
        val index = indexes.get(cls);

        if (index instanceof ObjectIndex) {
            return ((ObjectIndex<Instance>) index).get(object) != null;
        } else if (index instanceof MultiIndex) {
            return !((MultiIndex<Instance>) index).get(object).isEmpty();
        }

        throw new IllegalStateException("Index not found: " + object.getClass().getSimpleName());
    }

    public void store(final @NonNull Instance instance) {
        indexes.values().forEach(index -> index.store(instance));
    }

    public void remove(final @NonNull Instance instance) {
        indexes.values().forEach(index -> index.remove(instance));
    }

    public void clear() {
        indexes.clear();
    }

    @Override
    public String toString() {
        return indexes.values().toString();
    }

    abstract static class Index<Instance> {
        abstract void store(Instance instance);
        abstract void remove(Instance instance);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    static class BasicIndexMap<Instance> extends IndexMap<Instance> {
        Map<Instance, Boolean> masterStore = NuclearReferenceMap.create(RefType.STRONG, RefType.STRONG);

        @Override
        public void store(@NonNull Instance instance) {
            if (masterStore.put(instance, Boolean.TRUE) == null) {
                super.store(instance);
            }
        }

        @Override
        public void remove(@NonNull Instance instance) {
            if (masterStore.remove(instance) != null) {
                super.remove(instance);
            }
        }

        @Override
        public Set<Instance> values() {
            return masterStore.keySet();
        }

        @Override
        public int size() {
            return masterStore.size();
        }

        @Override
        public void clear() {
            masterStore.clear();
            super.clear();
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    static class IntIndex<Instance> extends Index<Instance> {
        ToIntFunction<Instance> fun;
        SentinelNuclearIntMap<Instance> storage = new SentinelNuclearIntMap<>(1 << 20);

        @Override
        void store(final @NonNull Instance instance) {
            storage.put(fun.applyAsInt(instance), instance);
        }

        @Override
        void remove(final @NonNull Instance instance) {
            storage.remove(fun.applyAsInt(instance));
        }

        Instance get(final int i) {
            return storage.get(i);
        }
    }

    @FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
    @RequiredArgsConstructor
    abstract static class ObjectIndex<Instance> extends Index<Instance> {
        Function<Instance, ?> fun;
        abstract Instance get(Object object);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    static class HashIndex<Instance> extends ObjectIndex<Instance> {
        Map<Object, Instance> storage = NuclearReferenceMap.create(RefType.STRONG, RefType.STRONG);

        HashIndex(final @NonNull Function<Instance, ?> transfer) {
            super(transfer);
        }

        @Override
        void store(Instance instance) {
            Object key = fun.apply(instance);
            if (key != null) storage.put(key, instance);
        }

        @Override
        void remove(Instance instance) {
            Object key = fun.apply(instance);
            if (key != null) storage.remove(key, instance);
        }

        @Override
        Instance get(Object object) {
            return storage.get(object);
        }
    }

    @FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
    @RequiredArgsConstructor
    static class MultiIndex<Instance> extends Index<Instance> {
        Function<Instance, Collection<?>> fun;
        Map<Object, Set<Instance>> storage = NuclearReferenceMap.create(RefType.STRONG, RefType.STRONG);

        @Override
        void store(Instance instance) {
            val keys = fun.apply(instance);
            if (keys == null || keys.isEmpty()) return;

            for (val key : keys) {
                var set = storage.get(key);

                if (set == null) {
                    storage.put(key, set = ConcurrentHashMap.newKeySet());
                }

                set.add(instance);
            }
        }

        @Override
        void remove(final @NonNull Instance instance) {
            val keys = fun.apply(instance);
            if (keys == null) return;

            for (val key : keys) {
                val set = storage.get(key);
                if (set != null) {
                    set.remove(instance);
                    //if (set.isEmpty()) storage.remove(key, set);
                }
            }
        }

        Set<Instance> get(final @NonNull Object object) {
            Set<Instance> set = storage.get(object);
            return set != null ? set : Collections.emptySet();
        }
    }
}