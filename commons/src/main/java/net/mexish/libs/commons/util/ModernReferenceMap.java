package net.mexish.libs.commons.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SuppressWarnings("all")
@Getter
public final class ModernReferenceMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

    public enum RefType {
        STRONG,
        WEAK,
        SOFT
    }

    private interface FetchStrategy<V> {
        V fetch(Object val);
    }

    ConcurrentMap<Object, Object> backingMap = new ConcurrentHashMap<>();
    ReferenceQueue<Object> queue = new ReferenceQueue<>();
    RefType keyType;
    RefType valueType;

    Function<Object, V> unwrapper;
    AtomicBoolean purging = new AtomicBoolean(false);

    FetchStrategy<V> fetcher;

    private static final ThreadLocal<Integer> PURGE_TICK = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<LookupKey> RECYCLED_KEY = ThreadLocal.withInitial(() -> new LookupKey(null));

    public ModernReferenceMap(final @NonNull RefType keyType,
                              final @NonNull RefType valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.unwrapper = (valueType == RefType.STRONG) ? (v -> (V)v) : (v -> (V)((Reference<?>)v).get());

        if (valueType == RefType.STRONG) {
            this.fetcher = val -> (V) val;
        } else {
            this.fetcher = val -> (V) ((Reference<?>) val).get();
        }
    }

    private void purge() {
        val tick = PURGE_TICK.get();
        if ((tick & 63) != 0) {
            PURGE_TICK.set(tick + 1);
            return;
        }

        PURGE_TICK.set(tick + 1);

        if (purging.compareAndSet(false, true)) {
            try {
                Reference<?> ref;
                var limit = 0;

                while ((ref = queue.poll()) != null && limit++ < 64) {
                    backingMap.remove(ref);
                }
            } finally {
                purging.set(false);
            }
        }
    }

    private Object toStoreKey(final Object key) {
        if (keyType == RefType.STRONG) return key;

        if (queue.poll() != null) {
            purge();
        }

        return (keyType == RefType.WEAK) ? new WeakKey<>(key, queue) : new SoftKey<>(key, queue);
    }

    private Object toStoreValue(final @NonNull Object value) {
        if (valueType == RefType.STRONG) return value;
        return (valueType == RefType.WEAK) ? new WeakRef<>(value, queue) : new SoftRef<>(value, queue);
    }

    private V fromStoreValue(final Object value) {
        if (valueType == RefType.STRONG) return (V) value;
        return (V) ((Reference<?>) value).get();
    }

    @Override
    public V put(final K key,
                 final V value) {
        val k = toStoreKey(key);
        val v = toStoreValue(value);
        val old = backingMap.put(k, v);
        return fromStoreValue(old);
    }

    @Override
    public V get(final Object key) {
        if (keyType == RefType.STRONG) {
            return (V) backingMap.get(key);
        }

        val lookupKey = RECYCLED_KEY.get();
        lookupKey.k = key;

        val h = System.identityHashCode(key);
        lookupKey.hash = (h ^ (h >>> 16)) & 0x7fffffff;

        return fetcher.fetch(backingMap.get(lookupKey));
    }

    @Override
    public V remove(final Object key) {
        val lookupKey = (keyType == RefType.STRONG) ? key : new LookupKey(key);
        val old = backingMap.remove(lookupKey);
        return fromStoreValue(old);
    }

    @Override
    public void clear() {
        backingMap.clear();
        while (queue.poll() != null) {}
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("EntrySet view not fully optimized for ReferenceMap");
    }

    @Override
    public V putIfAbsent(final @NonNull K key,
                         final V value) {
        val k = toStoreKey(key);
        val v = toStoreValue(value);
        val old = backingMap.putIfAbsent(k, v);

        return fromStoreValue(old);
    }

    @Override
    public boolean remove(final @NonNull Object key,
                          final Object value) {
        val k = (keyType == RefType.STRONG) ? key : new LookupKey(key);
        val current = backingMap.get(k);
        val currentVal = fromStoreValue(current);

        if (Objects.equals(currentVal, value)) {
            return backingMap.remove(k, current);
        }

        return false;
    }

    @Override
    public boolean replace(final @NonNull K key,
                           final @NonNull V oldValue,
                           final @NonNull V newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V replace(final @NonNull K key,
                     final @NonNull V value) {
        throw new UnsupportedOperationException();
    }

    public static class LookupKey {
        Object k;
        int hash;

        public LookupKey(final Object k) {
            reset(k);
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (o instanceof KeyRef) return ((KeyRef) o).matches(k);
            return false;
        }

        void reset(final Object k) {
            this.k = k;
            val h = System.identityHashCode(k);
            this.hash = (h ^ (h >>> 16)) & 0x7fffffff;
        }

        public int hashCode() {
            return hash;
        }
    }

    interface KeyRef {
        boolean matches(final Object realKey);
    }

    static class WeakKey<T> extends WeakReference<T> implements KeyRef {
        int hash;

        WeakKey(T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
            val h = System.identityHashCode(referent);
            this.hash = (h ^ (h >>> 16)) & 0x7fffffff;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (o instanceof LookupKey) return o.equals(this);
            if (!(o instanceof KeyRef)) return false;
            T got = get();
            return got != null && got == ((Reference<?>) o).get();
        }

        public int hashCode() { return hash; }
        public boolean matches(Object real) { return real == get(); }
    }

    static class SoftKey<T> extends SoftReference<T> implements KeyRef {
        int hash;

        SoftKey(T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
            val h = System.identityHashCode(referent);
            this.hash = (h ^ (h >>> 16)) & 0x7fffffff;
        }

        public int hashCode() {
            return hash;
        }

        public boolean matches(final Object real) {
            return real == get();
        }
    }

    static class WeakRef<T> extends WeakReference<T> {
        WeakRef(T referent, ReferenceQueue<? super T> q) { super(referent, q); }
    }
    static class SoftRef<T> extends SoftReference<T> {
        SoftRef(T referent, ReferenceQueue<? super T> q) { super(referent, q); }
    }

}