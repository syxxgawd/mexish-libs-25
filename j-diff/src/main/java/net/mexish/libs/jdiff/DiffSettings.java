package net.mexish.libs.jdiff;

public interface DiffSettings {

    /**
     * Defines the suffix sorting algorithm to be used during Diff creation.
     *
     * @param input input array
     * @return Sorted array of indices
     */
    int[] sort(byte[] input);

}
