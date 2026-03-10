package net.mexish.libs.jdiff;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
public final class ControlBlock {

    /**
     * Length of the patch diff block
     */
    long diffLength;

    /**
     * Length of the patch extra block
     */
    long extraLength;

    /**
     * Bytes to seek forward after completing the control block directives.
     */
    long seekLength;

    /**
     * Read a jdiff control block from an input stream.
     * @throws IOException if I/O errors occur while reading a jdiff offset.
     */
    public ControlBlock(final InputStream in) throws IOException {
        diffLength = Offset.readOffset(in);
        extraLength = Offset.readOffset(in);
        seekLength = Offset.readOffset(in);
        //TODO: validate lengths (should be >= 0)
    }

    public ControlBlock(final int diffLength,
                        final int extraLength,
                        final int seekLength) {
        this.diffLength = diffLength;
        this.extraLength = extraLength;
        this.seekLength = seekLength;
        //TODO: validate lengths (should be >= 0)
    }

    /**
     * Writes a ControlBlock to an OutputStream.
     *
     * @throws IOException if I/O errors occur while writing a bsdiff offset.
     */
    public void write(final @NonNull OutputStream out) throws IOException {
        Offset.writeOffset(diffLength, out);
        Offset.writeOffset(extraLength, out);
        Offset.writeOffset(seekLength, out);
    }

    @Override
    public String toString() {
        return diffLength + ", " + extraLength + ", " + seekLength;
    }

}
