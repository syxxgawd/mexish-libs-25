package net.mexish.libs.jdiff.sort;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Data
public final class SearchResult {

    /** Number of matched bytes */
    int length;

    /** Position of the result in the suffix array */
    int position;

    @Override
    public String toString() {
        return "length = " + length + ", position = " + position;
    }
}
