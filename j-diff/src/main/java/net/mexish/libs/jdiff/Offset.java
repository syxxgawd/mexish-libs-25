package net.mexish.libs.jdiff;

import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@FieldDefaults(makeFinal = true)
@UtilityClass
public final class Offset {
    int OFFSET_SIZE = 8;

    public long readOffset(final @NonNull InputStream in) throws IOException {
        val buf = new byte[OFFSET_SIZE];
        val bytesRead = in.readNBytes(buf, 0, OFFSET_SIZE);

        if (bytesRead < OFFSET_SIZE) {
            throw new IOException("Unexpected end of stream while reading offset");
        }

        long value = buf[7] & 0x7F;

        for (var i = 6; i >= 0; i--) {
            value = (value << 8) | (buf[i] & 0xFF);
        }

        if ((buf[7] & 0x80) != 0) {
            value = -value;
        }

        return value;
    }

    public void writeOffset(final long value,
                            final @NonNull OutputStream out) throws IOException {
        val buf = new byte[OFFSET_SIZE];
        var y = value;

        if (value < 0) {
            y = -value;
            buf[7] |= 0x80;
        }

        for (var i = 0; i < 7; i++) {
            buf[i] = (byte) (y & 0xFF);
            y >>>= 8;
        }

        buf[7] |= (byte) (y & 0x7F);

        out.write(buf);
    }
}