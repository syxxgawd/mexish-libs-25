package net.mexish.libs.jdiff;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.jpountz.lz4.LZ4FrameOutputStream;
import net.mexish.libs.jdiff.sort.SearchResult;
import net.mexish.libs.jdiff.sort.SuffixSort;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@UtilityClass
public final class Diff {

    public void diff(final byte[] oldBytes,
                     final byte[] newBytes,
                     final OutputStream out)
            throws IOException, InvalidHeaderException {
        diff(oldBytes, newBytes, out, new DefaultDiffSettings());
    }

    public void diff(final byte[] oldBytes,
                     final byte @NotNull [] newBytes,
                     final @NonNull OutputStream out,
                     final @NonNull DiffSettings settings)
            throws IOException, InvalidHeaderException {

        val I = settings.sort(oldBytes);

        val controlBo = new ByteArrayOutputStream();
        val diffBo = new ByteArrayOutputStream();
        val extraBo = new ByteArrayOutputStream();

        try (val controlOut = new LZ4FrameOutputStream(controlBo);
             val diffOut = new LZ4FrameOutputStream(diffBo);
             val extraOut = new LZ4FrameOutputStream(extraBo)) {

            SearchResult result;
            int scan = 0, len = 0, position = 0;
            int lastScan = 0, lastPos = 0, lastOffset = 0;
            int oldScore, scsc;
            int s, Sf, lenf, Sb, lenb;
            int overlap, Ss, lens;

            while (scan < newBytes.length) {
                oldScore = 0;

                for (scsc = scan += len; scan < newBytes.length; scan++) {
                    result = SuffixSort.search(I, oldBytes, 0, newBytes, scan, 0, oldBytes.length);
                    len = result.getLength();
                    position = result.getPosition();

                    for (; scsc < scan + len; scsc++) {
                        if ((scsc + lastOffset < oldBytes.length) &&
                                (oldBytes[scsc + lastOffset] == newBytes[scsc]))
                            oldScore++;
                    }

                    if (((len == oldScore) && (len != 0)) || (len > oldScore + 8)) {
                        break;
                    }

                    if ((scan + lastOffset < oldBytes.length) &&
                            (oldBytes[scan + lastOffset] == newBytes[scan]))
                        oldScore--;
                }

                if ((len != oldScore) || (scan == newBytes.length)) {
                    s = 0;
                    Sf = 0;
                    lenf = 0;
                    for (int i = 0; (lastScan + i < scan) && (lastPos + i < oldBytes.length); ) {
                        if (oldBytes[lastPos + i] == newBytes[lastScan + i]) {
                            s++;
                        }
                        i++;
                        if (s * 2 - i > Sf * 2 - lenf) {
                            Sf = s;
                            lenf = i;
                        }
                    }

                    lenb = 0;
                    if (scan < newBytes.length) {
                        s = 0;
                        Sb = 0;
                        for (int i = 1; (scan >= lastScan + i) && (position >= i); i++) {
                            if (oldBytes[position - i] == newBytes[scan - i]) {
                                s++;
                            }
                            if (s * 2 - i > Sb * 2 - lenb) {
                                Sb = s;
                                lenb = i;
                            }
                        }
                    }

                    if (lastScan + lenf > scan - lenb) {
                        overlap = (lastScan + lenf) - (scan - lenb);
                        s = 0;
                        Ss = 0;
                        lens = 0;
                        for (int i = 0; i < overlap; i++) {
                            if (newBytes[lastScan + lenf - overlap + i] ==
                                    oldBytes[lastPos + lenf - overlap + i]) {
                                s++;
                            }
                            if (newBytes[scan - lenb + i] ==
                                    oldBytes[position - lenb + i]) {
                                s--;
                            }
                            if (s > Ss) {
                                Ss = s;
                                lens = i + 1;
                            }
                        }
                        lenf += lens - overlap;
                        lenb -= lens;
                    }

                    for (int i = 0; i < lenf; i++) {
                        diffOut.write(newBytes[lastScan + i] - oldBytes[lastPos + i]);
                    }

                    for (int i = 0; i < (scan - lenb) - (lastScan + lenf); i++) {
                        extraOut.write(newBytes[lastScan + lenf + i]);
                    }

                    val control = new ControlBlock();
                    control.setDiffLength(lenf);
                    control.setExtraLength((scan - lenb) - (lastScan + lenf));
                    control.setSeekLength((position - lenb) - (lastPos + lenf));
                    control.write(controlOut);

                    lastScan = scan - lenb;
                    lastPos = position - lenb;
                    lastOffset = position - scan;
                }
            }
        }

        val header = new Header();
        header.setControlLength(controlBo.size());
        header.setDiffLength(diffBo.size());
        header.setOutputLength(newBytes.length);

        header.write(out);
        controlBo.writeTo(out);
        diffBo.writeTo(out);
        extraBo.writeTo(out);
    }
}