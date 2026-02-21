package net.mexish.libs.commons.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SuppressWarnings("all")
public final class NuclearIntMap<V> {

    static final VarHandle VAL_ACCESS = MethodHandles.arrayElementVarHandle(Object[].class);
    static final Object TOMBSTONE = new Object();

    int[] keys;
    Object[] values;
    int mask;

    public NuclearIntMap(final int initialCapacity) {
        var cap = 1;
        while (cap < initialCapacity) cap <<= 1;
        this.keys = new int[cap << 1];
        this.values = new Object[cap << 1];
        this.mask = (cap << 1) - 1;
    }

    public @Nullable V get(final int key) {
        val kTab = keys;
        val vTab = values;
        val m = mask;

        var idx = (key ^ (key >>> 16)) & m;

        while (true) {
            val val = (Object) VAL_ACCESS.getVolatile(vTab, idx);

            if (val == null) return null;

            if (kTab[idx] == key) {
                if (val == TOMBSTONE) {
                    idx = (idx + 1) & m;
                    continue;
                }

                return (V) val;
            }

            idx = (idx + 1) & m;
        }
    }

    public void put(int key, V value) {
        if (value == null) throw new IllegalArgumentException("Value cannot be null");

        final int[] kTab = keys;
        final Object[] vTab = values;
        final int m = mask;
        int idx = (key ^ (key >>> 16)) & m;

        while (true) {
            val val = (Object) VAL_ACCESS.getVolatile(vTab, idx);

            if (val == null || val == TOMBSTONE) {
                kTab[idx] = key;

                if (VAL_ACCESS.compareAndSet(vTab, idx, val, value)) {
                    return;
                }

                continue;
            }

            if (kTab[idx] == key) {
                VAL_ACCESS.setVolatile(vTab, idx, value);
                return;
            }

            idx = (idx + 1) & m;
        }
    }

    public void remove(final int key) {
        val kTab = keys;
        val vTab = values;
        val m = mask;
        var idx = (key ^ (key >>> 16)) & m;

        while (true) {
            val val = (Object) VAL_ACCESS.getVolatile(vTab, idx);
            if (val == null) return;

            if (kTab[idx] == key) {
                if (val != TOMBSTONE) {
                    VAL_ACCESS.setVolatile(vTab, idx, TOMBSTONE);
                }

                return;
            }

            idx = (idx + 1) & m;
        }
    }
}