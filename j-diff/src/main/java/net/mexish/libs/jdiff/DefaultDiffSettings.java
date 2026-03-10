package net.mexish.libs.jdiff;

import lombok.val;
import net.mexish.libs.jdiff.sort.SuffixSort;
import org.jetbrains.annotations.NotNull;

public final class DefaultDiffSettings implements DiffSettings {

    @Override
    public int @NotNull [] sort(final byte @NotNull [] input) {
        val I = new int[input.length + 1];
        val V = new int[input.length + 1];
        SuffixSort.qsufsort(I, V, input);

        return I;
    }

}
