package net.mexish.libs.commons.util;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SuppressWarnings("all")
public final class SentinelNuclearIntMap<V> {

    static final Unsafe UNSAFE;
    static final long INT_BASE;
    static final int INT_SCALE;
    static final long OBJ_BASE;
    static final int OBJ_SCALE;
    static final Object TOMBSTONE = new Object();

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
            INT_BASE = UNSAFE.arrayBaseOffset(int[].class);
            INT_SCALE = UNSAFE.arrayIndexScale(int[].class);
            OBJ_BASE = UNSAFE.arrayBaseOffset(Object[].class);
            OBJ_SCALE = UNSAFE.arrayIndexScale(Object[].class);
        } catch (Exception e) { throw new Error(e); }
    }

    int[] keys;
    Object[] values;
    int mask;

    @NonFinal V zeroValue = null;
    @NonFinal boolean hasZero = false;

    public SentinelNuclearIntMap(final int initialCapacity) {
        var cap = 1;
        while (cap < initialCapacity) cap <<= 1;
        this.keys = new int[cap << 1];
        this.values = new Object[cap << 1];
        this.mask = (cap << 1) - 1;
    }

    public @Nullable V get(final int key) {
        if (key == 0) return hasZero ? zeroValue : null;

        val kTab = keys;
        val m = mask;
        var idx = (key ^ (key >>> 16)) & m;

        while (true) {
            val k = UNSAFE.getInt(kTab, INT_BASE + ((long)idx * INT_SCALE));

            if (k == 0) {
                return null;
            }

            if (k == key) {
                val val = getObj(idx);

                if (val == TOMBSTONE) {
                    idx = (idx + 1) & m;
                    continue;
                }

                return (V) val;
            }

            idx = (idx + 1) & m;
        }
    }

    public void put(final int key,
                    final V value) {
        if (value == null) throw new IllegalArgumentException("Null values not allowed");

        if (key == 0) {
            zeroValue = value;
            hasZero = true;
            return;
        }

        val kTab = keys;
        val m = mask;
        var idx = (key ^ (key >>> 16)) & m;

        while (true) {
            val k = UNSAFE.getInt(kTab, INT_BASE + ((long)idx * INT_SCALE));

            if (k == 0) {
                UNSAFE.putInt(kTab, INT_BASE + ((long)idx * INT_SCALE), key);
                putObj(idx, value);
                return;
            }

            if (k == key) {
                putObj(idx, value);
                return;
            }

            idx = (idx + 1) & m;
        }
    }

    public void remove(final int key) {
        if (key == 0) {
            hasZero = false;
            zeroValue = null;
            return;
        }

        val kTab = keys;
        val m = mask;
        var idx = (key ^ (key >>> 16)) & m;

        while (true) {
            val k = UNSAFE.getInt(kTab, INT_BASE + ((long)idx * INT_SCALE));
            if (k == 0) return;

            if (k == key) {
                putObj(idx, TOMBSTONE);
                return;
            }

            idx = (idx + 1) & m;
        }
    }

    private Object getObj(int idx) {
        return UNSAFE.getObjectVolatile(values, OBJ_BASE + ((long)idx * OBJ_SCALE));
    }
    private void putObj(int idx, Object val) {
        UNSAFE.putObjectVolatile(values, OBJ_BASE + ((long)idx * OBJ_SCALE), val);
    }
}