package net.mexish.libs.jdiff;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.jpountz.lz4.LZ4FrameInputStream;

import java.io.*;

@UtilityClass
public final class Patch {

    private static final int BUFFER_SIZE = 8192; // 8kb

    public void patch(final @NonNull File oldFile,
                      final @NonNull File newFile,
                      final @NonNull File patchFile) throws IOException, InvalidHeaderException {
        try (val headerIn = new FileInputStream(patchFile)) {
            val header = new Header(headerIn);

            try (
                    val patchFileStream1 = new FileInputStream(patchFile);
                    val patchFileStream2 = new FileInputStream(patchFile);
                    val patchFileStream3 = new FileInputStream(patchFile);
                    val out = new BufferedOutputStream(new FileOutputStream(newFile));
                    val oldFileRaf = new RandomAccessFile(oldFile, "r")
            ) {
                val controlOffset = Header.HEADER_SIZE;
                val diffOffset = controlOffset + header.getControlLength();
                val extraOffset = diffOffset + header.getDiffLength();

                patchFileStream1.skip(controlOffset);
                patchFileStream2.skip(diffOffset);
                patchFileStream3.skip(extraOffset);

                try (
                        val controlIn = new LZ4FrameInputStream(new BufferedInputStream(patchFileStream1));
                        val diffIn = new LZ4FrameInputStream(new BufferedInputStream(patchFileStream2));
                        val extraIn = new LZ4FrameInputStream(new BufferedInputStream(patchFileStream3))
                ) {
                    val oldBytes = new byte[(int) oldFile.length()];
                    oldFileRaf.readFully(oldBytes);

                    var newPointer = 0L;
                    var oldPointer = 0L;
                    var outputLength = header.getOutputLength();

                    byte[] diffBuffer = new byte[BUFFER_SIZE];
                    byte[] oldBuffer  = new byte[BUFFER_SIZE];

                    while (newPointer < outputLength) {
                        val control = new ControlBlock(controlIn);

                        val diffLen = control.getDiffLength();
                        val extraLen = control.getExtraLength();

                        var bytesProcessed = 0L;
                        while (bytesProcessed < diffLen) {
                            val chunkSize = (int) Math.min(BUFFER_SIZE, diffLen - bytesProcessed);

                            readFully(diffIn, diffBuffer, chunkSize);

                            oldFileRaf.seek(oldPointer);
                            oldFileRaf.readFully(oldBuffer, 0, chunkSize);

                            for (var i = 0; i < chunkSize; i++) {
                                diffBuffer[i] += oldBuffer[i];
                            }

                            out.write(diffBuffer, 0, chunkSize);

                            bytesProcessed += chunkSize;
                            oldPointer += chunkSize;
                            newPointer += chunkSize;
                        }

                        var bytesCopied = 0L;
                        while (bytesCopied < extraLen) {
                            val chunkSize = (int) Math.min(BUFFER_SIZE, extraLen - bytesCopied);

                            readFully(extraIn, diffBuffer, chunkSize);
                            out.write(diffBuffer, 0, chunkSize);

                            bytesCopied += chunkSize;
                            newPointer += chunkSize;
                        }

                        oldPointer += control.getSeekLength();
                    }
                }
            }
        }
    }

    private void readFully(final @NonNull InputStream in,
                           final byte[] dest,
                           int len) throws IOException {
        var total = 0;
        while (total < len) {
            val result = in.read(dest, total, len - total);

            if (result == -1) {
                throw new EOFException("Stream ended unexpectedly");
            }

            total += result;
        }
    }
}