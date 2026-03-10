package net.mexish.libs.jdiff.test;

import lombok.val;
import net.mexish.libs.jdiff.sort.SuffixSort;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class JDiffTest {

    @Test
    void testQsufsortAndSearchBasic() {
        // "banana" is the classic suffix sort test case
        val oldData = "banana".getBytes(StandardCharsets.US_ASCII);
        val newData = "ana".getBytes(StandardCharsets.US_ASCII);

        // 1. Setup Arrays (Size + 1 is required by qsufsort)
        val I = new int[oldData.length + 1];
        val V = new int[oldData.length + 1];

        // 2. Sort
        SuffixSort.qsufsort(I, V, oldData);

        // 3. Search for "ana" inside "banana"
        // We expect a full match (length 3) at position 1 (b-ana-na) or 3 (ban-ana)
        // The suffix sort should prioritize the lexicographical order.
        // Suffixes of banana:
        // a (5)
        // ana (3)  <-- Should match this
        // anana (1)
        // banana (0)
        // na (4)
        // nana (2)

        val result = SuffixSort.search(I, oldData, 0, newData, 0, 0, oldData.length);

        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.getLength(), "Should match all 3 bytes 'ana'");

        // Verify the position points to valid data
        val matchedString = new String(oldData, result.getPosition(), result.getLength(), StandardCharsets.US_ASCII);
        assertEquals("ana", matchedString, "The matched content should be 'ana'");
    }

    @Test
    void testExactMatch() {
        val oldData = "Hello World".getBytes(StandardCharsets.UTF_8);
        val newData = "Hello World".getBytes(StandardCharsets.UTF_8);

        val I = new int[oldData.length + 1];
        val V = new int[oldData.length + 1];
        SuffixSort.qsufsort(I, V, oldData);

        val result = SuffixSort.search(I, oldData, 0, newData, 0, 0, oldData.length);

        assertEquals(oldData.length, result.getLength(), "Should match the entire string");
    }

    @Test
    void testNoMatch() {
        val oldData = "ABCDEF".getBytes(StandardCharsets.UTF_8);
        val newData = "XYZ".getBytes(StandardCharsets.UTF_8);

        val I = new int[oldData.length + 1];
        val V = new int[oldData.length + 1];
        SuffixSort.qsufsort(I, V, oldData);

        val result = SuffixSort.search(I, oldData, 0, newData, 0, 0, oldData.length);

        assertEquals(0, result.getLength(), "Should match 0 bytes");
    }

    @Test
    void testPartialMatch() {
        // old: "The quick brown fox"
        // new: "The quick red fox"
        val oldData = "The quick brown fox".getBytes(StandardCharsets.UTF_8);
        val newData = "The quick red fox".getBytes(StandardCharsets.UTF_8);

        val I = new int[oldData.length + 1];
        val V = new int[oldData.length + 1];
        SuffixSort.qsufsort(I, V, oldData);

        // Search for the beginning "The quick "
        val result = SuffixSort.search(I, oldData, 0, newData, 0, 0, oldData.length);

        assertEquals(10, result.getLength(), "Should match 'The quick ' (10 chars)");
    }

    @Test
    void testBinarySafety() {
        // Test with non-text binary data to ensure signed/unsigned byte handling works
        byte[] oldData = { (byte)0xFF, (byte)0x00, (byte)0xAA, 0x11, 0x22 };
        byte[] newData = { (byte)0xAA, 0x11, 0x33 }; // 0xAA, 0x11 matches

        val I = new int[oldData.length + 1];
        val V = new int[oldData.length + 1];
        SuffixSort.qsufsort(I, V, oldData);

        val result = SuffixSort.search(I, oldData, 0, newData, 0, 0, oldData.length);

        assertEquals(2, result.getLength(), "Should match 0xAA 0x11");
        // 0xAA is at index 2 in oldData
        assertEquals(2, result.getPosition(), "Position should be index 2");
    }
}
