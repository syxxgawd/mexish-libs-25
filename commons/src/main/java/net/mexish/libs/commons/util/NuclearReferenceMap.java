package net.mexish.libs.commons.util;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@FieldDefaults(level = AccessLevel.PROTECTED)
@SuppressWarnings("all")
public abstract class NuclearReferenceMap<K, V> extends AbstractMap<K, V> {

    @Contract("_, _ -> new")
    public static <K, V> @NotNull NuclearReferenceMap<K, V> create(
            ModernReferenceMap.RefType keyType,
            ModernReferenceMap.RefType valueType) {
        if (keyType == ModernReferenceMap.RefType.STRONG && valueType == ModernReferenceMap.RefType.STRONG) {
            return new StrongStrongMap<>();
        }

        if (keyType == ModernReferenceMap.RefType.WEAK && valueType == ModernReferenceMap.RefType.STRONG) {
            return new WeakStrongMap<>();
        }

        if (keyType == ModernReferenceMap.RefType.STRONG && valueType == ModernReferenceMap.RefType.WEAK) {
            return new StrongWeakMap<>();
        }

        return new WeakWeakMap<>();
    }


    static final VarHandle TABLE_ACCESS = MethodHandles.arrayElementVarHandle(Object[].class);
    static final Object TOMBSTONE = new Object();

    final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    final AtomicInteger size = new AtomicInteger(0);

    volatile Object[] table;
    int mask;
    int threshold;
    final float loadFactor = 0.45f;

    NuclearReferenceMap() {
        val initialCapacity = 1 << 22;
        this.table = new Object[initialCapacity << 1];
        this.mask = initialCapacity - 1;
        this.threshold = (int) (initialCapacity * loadFactor);
    }

    abstract void putInsert(Object[] tab, int idx, K key, V value);
    abstract Object wrapKey(K key);
    abstract Object unwrapKey(Object raw);

    abstract int hash(Object key);
    abstract boolean keyEquals(Object key, Object foundKey);

    private static final class StrongStrongMap<K, V> extends NuclearReferenceMap<K, V> {

        @Override
        int hash(final @NonNull Object key) {
            return key.hashCode();
        }

        @Contract(value = "_, null -> false", pure = true)
        @Override
        boolean keyEquals(final @NonNull Object key,
                          final Object foundKey) {
            return key == foundKey || (key != null && key.equals(foundKey));
        }

        @Override
        public V get(final Object key) {
            if (key == null) {
                return null;
            }

            val tab = table;
            val m = mask;
            val h = key.hashCode();
            var idx = ((h ^ (h >>> 16)) & 0x7fffffff & m) << 1;

            while (true) {
                val k = TABLE_ACCESS.getVolatile(tab, idx);
                if (k == null) {
                    return null;
                }

                if (k.equals(key)) {
                    return (V) TABLE_ACCESS.getVolatile(tab, idx + 1);
                }

                if (k == TOMBSTONE) {
                    idx = (idx + 2) & (m << 1);
                    continue;
                }

                idx = (idx + 2) & (m << 1);
            }
        }

        @Override
        void putInsert(final Object[] tab,
                       final int idx,
                       final K key,
                       final V value) {
            TABLE_ACCESS.setVolatile(tab, idx + 1, value);
        }

        @Override
        Object wrapKey(final K key) {
            return key;
        }

        @Override
        Object unwrapKey(final Object raw) {
            return raw;
        }

        @Override
        V unwrapValue(final Object raw) {
            return (V) raw;
        }
    }

    private static final class WeakStrongMap<K, V> extends NuclearReferenceMap<K, V> {

        @Override
        int hash(final @NonNull Object key) {
            return System.identityHashCode(key);
        }

        @Contract(value = "_, null -> false", pure = true)
        @Override
        boolean keyEquals(final @NonNull Object key,
                          final Object foundKey) {
            return key == foundKey;
        }

        @Override
        public V get(Object key) {
            if (key == null) return null;
            val tab = table;
            val m = mask;
            val h = System.identityHashCode(key);
            var idx = ((h ^ (h >>> 16)) & 0x7fffffff & m) << 1;

            while (true) {
                val k = TABLE_ACCESS.getVolatile(tab, idx);
                if (k == null) return null;

                if (k != TOMBSTONE) {
                    val deref = ((Reference<?>) k).get();
                    if (deref == key) return (V) TABLE_ACCESS.getVolatile(tab, idx + 1);
                }

                idx = (idx + 2) & (m << 1);
            }
        }

        @Override
        void putInsert(Object[] tab, int idx, K key, V value) {
            TABLE_ACCESS.setVolatile(tab, idx + 1, value);
        }

        @Contract("_ -> new")
        @Override
        @NotNull
        Object wrapKey(K key) {
            return new WeakKey<>(key, queue, System.identityHashCode(key));
        }

        @Override
        Object unwrapKey(Object raw) {
            return raw instanceof Reference ? ((Reference<?>) raw).get() : raw;
        }

        @Override
        V unwrapValue(final Object raw) {
            return (V) raw;
        }
    }

    private static final class StrongWeakMap<K, V> extends NuclearReferenceMap<K, V> {

        @Override
        int hash(final @NonNull Object key) {
            return key.hashCode();
        }

        @Contract(value = "_, null -> false", pure = true)
        @Override
        boolean keyEquals(final @NonNull Object key,
                          final Object foundKey) {
            return key.equals(foundKey);
        }

        @Override
        public V get(final Object key) {
            if (key == null) {
                return null;
            }

            val tab = table;
            val m = mask;
            val h = key.hashCode();
            var idx = ((h ^ (h >>> 16)) & 0x7fffffff & m) << 1;

            while (true) {
                val k = TABLE_ACCESS.getVolatile(tab, idx);

                if (k == null) {
                    return null;
                }

                if (k.equals(key)) {
                    val v = TABLE_ACCESS.getVolatile(tab, idx + 1);
                    return v == null ? null : (V) ((Reference<?>) v).get();
                }

                idx = (idx + 2) & (m << 1);
            }
        }

        @Override
        void putInsert(final Object[] tab,
                       final int idx,
                       final K key,
                       final V value) {
            TABLE_ACCESS.setVolatile(tab, idx + 1, new WeakReference<>(value, queue));
        }

        @Override
        Object wrapKey(final K key) {
            return key;
        }

        @Override
        Object unwrapKey(final Object raw) {
            return raw;
        }

        @Override
        V unwrapValue(final Object raw) {
            return (V) raw;
        }
    }

    private static final class WeakWeakMap<K, V> extends NuclearReferenceMap<K, V> {

        @Override
        int hash(final @NonNull Object key) {
            return System.identityHashCode(key);
        }

        @Contract(value = "_, null -> false", pure = true)
        @Override
        boolean keyEquals(final @NonNull Object key,
                          final Object foundKey) {
            return key == foundKey;
        }

        @Override
        public V get(Object key) {
            if (key == null) return null;
            val tab = table;
            val m = mask;
            val h = System.identityHashCode(key);
            var idx = ((h ^ (h >>> 16)) & 0x7fffffff & m) << 1;

            while (true) {
                val k = TABLE_ACCESS.getVolatile(tab, idx);
                if (k == null) return null;

                if (k != TOMBSTONE) {
                    val deref = ((Reference<?>) k).get();
                    if (deref == key) {
                        val v = TABLE_ACCESS.getVolatile(tab, idx + 1);
                        return v == null ? null : (V) ((Reference<?>) v).get();
                    }
                }

                idx = (idx + 2) & (m << 1);
            }
        }

        @Override
        void putInsert(final Object[] tab,
                       final int idx,
                       final K key,
                       final V value) {
            TABLE_ACCESS.setVolatile(tab, idx + 1, new WeakReference<>(value, queue));
        }

        @Override
        Object wrapKey(final K key) {
            return new WeakKey<>(key, queue, System.identityHashCode(key));
        }

        @Override
        Object unwrapKey(final Object raw) {
            return raw instanceof Reference ? ((Reference<?>) raw).get() : raw;
        }

        @Override
        V unwrapValue(final Object raw) {
            return (V) raw;
        }
    }

    @Override
    public V put(final K key,
                 final V value) {
        if (size.get() > threshold) rehash();

        val h = hash(key);
        val hash = (h ^ (h >>> 16)) & 0x7fffffff;
        val keyObj = wrapKey(key);

        while (true) {
            val tab = table;
            val m = mask;
            var idx = (hash & m) << 1;

            while (true) {
                val existingK = TABLE_ACCESS.getVolatile(tab, idx);

                if (existingK == null || existingK == TOMBSTONE) {
                    if (TABLE_ACCESS.compareAndSet(tab, idx, existingK, keyObj)) {
                        putInsert(tab, idx, key, value);

                        if (existingK == null) {
                            size.incrementAndGet();
                        }

                        return null;
                    }

                    continue;
                }

                val deref = unwrapKey(existingK);

                if (keyEquals(key, deref)) {
                    putInsert(tab, idx, key, value);
                    return null;
                }

                idx = (idx + 2) & (m << 1);
            }
        }
    }

    @Override
    public V remove(final Object key) {
        if (key == null) {
            return null;
        }

        val h = hash(key);
        val hash = (h ^ (h >>> 16)) & 0x7fffffff;

        while (true) {
            val tab = table;
            val m = mask;
            var idx = (hash & m) << 1;

            while (true) {
                val kRef = TABLE_ACCESS.getVolatile(tab, idx);

                if (kRef == null) return null;

                if (kRef == TOMBSTONE) {
                    idx = (idx + 2) & (m << 1);
                    continue;
                }

                val derefKey = unwrapKey(kRef);

                if (keyEquals(key, derefKey)) {
                    val vRef = TABLE_ACCESS.getVolatile(tab, idx + 1);
                    val oldValue = unwrapValue(vRef);

                    if (TABLE_ACCESS.compareAndSet(tab, idx, kRef, TOMBSTONE)) {
                        TABLE_ACCESS.setVolatile(tab, idx + 1, null);
                        size.decrementAndGet();
                        return oldValue;
                    }

                    continue;
                }

                idx = (idx + 2) & (m << 1);
            }
        }
    }

    @Override
    public boolean remove(final @NonNull Object key,
                          final @NonNull Object value) {
        if (key == null || value == null) return false;

        val h = System.identityHashCode(key);
        val hash = (h ^ (h >>> 16)) & 0x7fffffff;

        while (true) {
            val tab = table;
            val m = mask;
            var idx = (hash & m) << 1;

            while (true) {
                val kRef = TABLE_ACCESS.getVolatile(tab, idx);

                if (kRef == null) return false;
                if (kRef == TOMBSTONE) {
                    idx = (idx + 2) & (m << 1);
                    continue;
                }

                val derefKey = unwrapKey(kRef);
                if (derefKey == key) {
                    val vRef = TABLE_ACCESS.getVolatile(tab, idx + 1);
                    val existingVal = unwrapValue(vRef);

                    if (existingVal != null && existingVal.equals(value)) {
                        if (TABLE_ACCESS.compareAndSet(tab, idx, kRef, TOMBSTONE)) {
                            TABLE_ACCESS.setVolatile(tab, idx + 1, null);
                            size.decrementAndGet();
                            return true;
                        }
                        continue;
                    }
                    return false;
                }

                idx = (idx + 2) & (m << 1);
            }
        }
    }

    abstract V unwrapValue(Object raw);

    private synchronized void rehash() {
        if (size.get() <= threshold) return;

        val oldTab = table;
        val oldLen = oldTab.length;
        val newCap = oldLen;
        val newTab = new Object[newCap << 1];
        val newMask = (newCap - 1);
        var newSize = 0;

        for (var i = 0; i < oldLen; i += 2) {
            val kRef = oldTab[i];

            if (kRef == null || kRef == TOMBSTONE) {
                continue;
            }

            val k = unwrapKey(kRef);

            if (k == null) {
                continue;
            }

            val h = hash(k);
            val hash = (h ^ (h >>> 16)) & 0x7fffffff;
            var idx = (hash & newMask) << 1;

            while (newTab[idx] != null) {
                idx = (idx + 2) & (newMask << 1);
            }

            newTab[idx] = kRef;
            newTab[idx + 1] = oldTab[i + 1];
            newSize++;
        }

        this.table = newTab;
        this.mask = newMask;
        this.threshold = (int) (newCap * loadFactor);
        this.size.set(newSize);
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    final class EntrySet extends AbstractSet<Entry<K, V>> {
        @Override
        public @NotNull Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return size.get();
        }

        @Override
        public void clear() {
            NuclearReferenceMap.this.clear();
        }
    }

    final class EntryIterator implements Iterator<Entry<K, V>> {
        private final Object[] t;
        private int index = 0;
        private int lastRet = -1;
        private Entry<K, V> nextEntry;

        EntryIterator() {
            this.t = table;
            advance();
        }

        private void advance() {
            nextEntry = null;
            while (index < t.length) {
                val kRef = TABLE_ACCESS.getAcquire(t, index);

                if (kRef == null || kRef == TOMBSTONE) {
                    index += 2;
                    continue;
                }

                val key = unwrapKey(kRef);
                if (key == null) {
                    index += 2;
                    continue;
                }

                val vRef = TABLE_ACCESS.getAcquire(t, index + 1);
                V value;

                if (vRef instanceof Reference) {
                    value = (V) ((Reference<?>) vRef).get();
                    if (value == null) {
                        index += 2;
                        continue;
                    }
                } else {
                    value = (V) vRef;
                }

                nextEntry = new SimpleImmutableEntry<>((K) key, value);
                index += 2;
                return;
            }
        }

        @Override
        public boolean hasNext() {
            return nextEntry != null;
        }

        @Override
        public Entry<K, V> next() {
            if (nextEntry == null) throw new NoSuchElementException();

            val e = nextEntry;
            lastRet = index - 2;
            advance();
            return e;
        }

        @Override
        public void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            throw new UnsupportedOperationException("Remove via iterator not supported in High-Speed Map");
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    static class WeakKey<T> extends WeakReference<T> {
        int hash;

        WeakKey(T r, ReferenceQueue<? super T> q, int h) {
            super(r, q);
            hash = h;
        }
    }
}