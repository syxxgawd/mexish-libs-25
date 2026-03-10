package net.mexish.libs.jdiff.test;

import lombok.val;
import net.mexish.libs.jdiff.Offset;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class OffsetTest {

    @Test
    void testPositiveLongs() throws Exception {
        assertEncoding(0L);
        assertEncoding(1L);
        assertEncoding(2147483647L); // Integer.MAX_VALUE
        assertEncoding(2147483648L); // Integer.MAX_VALUE + 1
        assertEncoding(Long.MAX_VALUE / 2); // Large long
    }

    @Test
    void testNegativeLongs() throws Exception {
        assertEncoding(-1L);
        assertEncoding(-2147483648L); // Integer.MIN_VALUE
        assertEncoding(Long.MIN_VALUE + 1); // Very small long
    }

    private void assertEncoding(long value) throws Exception {
        val out = new ByteArrayOutputStream();
        Offset.writeOffset(value, out);

        val bytes = out.toByteArray();
        assertEquals(8, bytes.length, "Offsets must always be 8 bytes");

        val in = new ByteArrayInputStream(bytes);
        val result = Offset.readOffset(in);

        assertEquals(value, result, "Decoded value must match encoded value");
    }
}
