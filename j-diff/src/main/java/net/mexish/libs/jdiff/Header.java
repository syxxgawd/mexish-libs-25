package net.mexish.libs.jdiff;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public final class Header {

    public static final int HEADER_SIZE = 36;
    public static final String HEADER_MAGIC = "JDIFF_MEXISH";

    String magic;
    @Getter long controlLength;
    @Getter long diffLength;
    @Getter long outputLength;

    public Header(final @NonNull InputStream in) throws IOException, InvalidHeaderException {
        val headerIn = new DataInputStream(in);

        val buf = new byte[HEADER_MAGIC.length()];

        headerIn.readFully(buf);
        magic = new String(buf, StandardCharsets.US_ASCII);

        if (!HEADER_MAGIC.equals(magic)) {
            throw new InvalidHeaderException("Header missing magic number");
        }

        controlLength = Offset.readOffset(headerIn);
        diffLength = Offset.readOffset(headerIn);
        outputLength = Offset.readOffset(headerIn);

        verify();
    }

    public Header(long controlLength,
                  long diffLength,
                  long outLength) throws InvalidHeaderException {
        this.controlLength = controlLength;
        this.diffLength = diffLength;
        this.outputLength = outLength;

        verify();
    }

    public void write(final @NonNull OutputStream out) throws IOException {
        out.write(HEADER_MAGIC.getBytes());
        Offset.writeOffset(controlLength, out);
        Offset.writeOffset(diffLength, out);
        Offset.writeOffset(outputLength, out);
    }

    private void verify() throws InvalidHeaderException {
        if (controlLength < 0) {
            throw new InvalidHeaderException("control block length",
                    controlLength);
        }

        if (diffLength < 0) {
            throw new InvalidHeaderException("diff block length", diffLength);
        }

        if (outputLength < 0) {
            throw new InvalidHeaderException("output file length", outputLength);
        }
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        var s = "";

        s += magic + "\n";
        s += "control bytes = " + controlLength + "\n";
        s += "diff bytes = " + diffLength + "\n";
        s += "output size = " + outputLength;

        return s;
    }

    public void setControlLength(final long length) throws InvalidHeaderException {
        controlLength = length;
        verify();
    }

    public void setDiffLength(final long length) throws InvalidHeaderException {
        diffLength = length;
        verify();
    }

    public void setOutputLength(final long length) throws InvalidHeaderException {
        outputLength = length;
        verify();
    }
}
