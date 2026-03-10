package net.mexish.libs.jdiff.test;

import lombok.val;
import net.mexish.libs.jdiff.Diff;
import net.mexish.libs.jdiff.Patch;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
public class JDiffBenchmark {

    private byte[] oldBytes;
    private byte[] newBytes;
    private File oldFile;
    private File patchFile;
    private File patchedFile;

    @Param({"102400", "1048576", "10485760"}) // 100KB, 1MB, 10MB
    private int fileSize;

    @Setup
    public void setup() throws Exception {
        Random r = new Random(1337);
        oldBytes = new byte[fileSize];
        r.nextBytes(oldBytes);

        // Create "New" version by modifying 5% of the file
        newBytes = oldBytes.clone();
        for (int i = 0; i < fileSize / 20; i++) {
            int pos = r.nextInt(fileSize);
            newBytes[pos] = (byte) (newBytes[pos] ^ 0xAA);
        }

        // Setup Files for Patching Benchmark
        oldFile = File.createTempFile("jmh_old", ".bin");
        patchFile = File.createTempFile("jmh_patch", ".bin");
        patchedFile = File.createTempFile("jmh_patched", ".bin");

        Files.write(oldFile.toPath(), oldBytes);

        // Pre-generate a patch for the Patch benchmark
        try (val out = new BufferedOutputStream(new FileOutputStream(patchFile))) {
            Diff.diff(oldBytes, newBytes, out);
        }
    }

    @TearDown
    public void tearDown() {
        oldFile.delete();
        patchFile.delete();
        patchedFile.delete();
    }

    @Benchmark
    public void benchmarkDiff(Blackhole bh) throws Exception {
        val nullOut = new OutputStream() {
            @Override public void write(int b) {}
            @Override public void write(byte[] b, int off, int len) {}
        };

        Diff.diff(oldBytes, newBytes, nullOut);
    }

    @Benchmark
    public void benchmarkPatch() throws Exception {
        Patch.patch(oldFile, patchedFile, patchFile);
    }
}
