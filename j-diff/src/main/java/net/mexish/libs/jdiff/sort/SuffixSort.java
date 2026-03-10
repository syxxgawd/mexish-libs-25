package net.mexish.libs.jdiff.sort;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class SuffixSort {

    public static void qsufsort(final int[] I,
                                final int[] V,
                                final byte @NotNull [] data) {
        val buckets = new int[256];
        int i, h, len;

        for (i = 0; i < data.length; i++) buckets[data[i] & 0xFF]++;
        for (i = 1; i < 256; i++) buckets[i] += buckets[i - 1];
        for (i = 255; i > 0; i--) buckets[i] = buckets[i - 1];
        buckets[0] = 0;

        for (i = 0; i < data.length; i++) I[++buckets[data[i] & 0xFF]] = i;
        I[0] = data.length;
        for (i = 0; i < data.length; i++) V[i] = buckets[data[i] & 0xFF];
        V[data.length] = 0;

        for (i = 1; i < 256; i++) {
            if (buckets[i] == buckets[i - 1] + 1) I[buckets[i]] = -1;
        }
        I[0] = -1;

        for (h = 1; I[0] != -(data.length + 1); h += h) {
            len = 0;
            for (i = 0; i < data.length + 1; ) {
                if (I[i] < 0) {
                    len -= I[i];
                    i -= I[i];
                } else {
                    if (len != 0) I[i - len] = -len;
                    len = V[I[i]] + 1 - i;
                    split(I, V, i, len, h);
                    i += len;
                    len = 0;
                }
            }
            if (len != 0) I[i - len] = -len;
        }

        for (i = 0; i < data.length + 1; i++) I[V[i]] = i;
    }

    private void split(final int[] I,
                       final int[] V,
                       final int start,
                       final int len,
                       final int h) {
        int i, j, k, x, tmp, jj, kk;

        if (len < 16) {
            for (k = start; k < start + len; k += j) {
                j = 1;
                x = V[I[k] + h];
                for (i = 1; k + i < start + len; i++) {
                    if (V[I[k + i] + h] < x) {
                        x = V[I[k + i] + h];
                        j = 0;
                    }
                    if (V[I[k + i] + h] == x) {
                        tmp = I[k + j]; I[k + j] = I[k + i]; I[k + i] = tmp;
                        j++;
                    }
                }
                for (i = 0; i < j; i++) V[I[k + i]] = k + j - 1;
                if (j == 1) I[k] = -1;
            }
            return;
        }

        x = V[I[start + len / 2] + h];
        jj = 0; kk = 0;
        for (i = start; i < start + len; i++) {
            if (V[I[i] + h] < x) jj++;
            if (V[I[i] + h] == x) kk++;
        }
        jj += start; kk += jj;

        i = start; j = 0; k = 0;
        while (i < jj) {
            if (V[I[i] + h] < x) i++;
            else if (V[I[i] + h] == x) {
                tmp = I[i]; I[i] = I[jj + j]; I[jj + j] = tmp; j++;
            } else {
                tmp = I[i]; I[i] = I[kk + k]; I[kk + k] = tmp; k++;
            }
        }

        while (jj + j < kk) {
            if (V[I[jj + j] + h] == x) j++;
            else {
                tmp = I[jj + j]; I[jj + j] = I[kk + k]; I[kk + k] = tmp; k++;
            }
        }

        if (jj > start) split(I, V, start, jj - start, h);
        for (i = 0; i < kk - jj; i++) V[I[jj + i]] = kk - 1;
        if (jj == kk - 1) I[jj] = -1;
        if (start + len > kk) split(I, V, kk, start + len - kk, h);
    }

    public @NotNull SearchResult search(final int[] I,
                                        final byte[] oldBytes,
                                        final int oldOffset,
                                        final byte[] newBytes,
                                        final int newOffset,
                                        final int start,
                                        final int end) {
        if (end - start < 2) {
            val x = matchLength(oldBytes, I[start], newBytes, newOffset);
            val y = matchLength(oldBytes, I[end], newBytes, newOffset);
            return (x > y) ? new SearchResult(x, I[start]) : new SearchResult(y, I[end]);
        }

        val center = start + (end - start) / 2;
        if (compareBytes(oldBytes, I[center], newBytes, newOffset) < 0) {
            return search(I, oldBytes, 0, newBytes, newOffset, center, end);
        } else {
            return search(I, oldBytes, 0, newBytes, newOffset, start, center);
        }
    }

    @Contract(pure = true)
    private int matchLength(final byte @NotNull [] bytesA,
                            final int offsetA,
                            final byte @NotNull [] bytesB,
                            final int offsetB) {
        val limit = Math.min(bytesA.length - offsetA, bytesB.length - offsetB);

        int i;
        for (i = 0; i < limit; ++i) {
            if (bytesA[i + offsetA] != bytesB[i + offsetB]) break;
        }

        return i;
    }

    @Contract(pure = true)
    private int compareBytes(final byte @NotNull [] bytesA,
                             final int offsetA,
                             final byte @NotNull [] bytesB,
                             final int offsetB) {
        val lenA = bytesA.length - offsetA;
        val lenB = bytesB.length - offsetB;
        val limit = Math.min(lenA, lenB);

        for (int i = 0; i < limit; ++i) {
            val valA = bytesA[i + offsetA] & 0xFF;
            val valB = bytesB[i + offsetB] & 0xFF;
            if (valA != valB) {
                return valA - valB;
            }
        }

        return lenA - lenB;
    }


}