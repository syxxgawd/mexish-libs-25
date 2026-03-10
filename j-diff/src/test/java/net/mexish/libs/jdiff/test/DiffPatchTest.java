package net.mexish.libs.jdiff.test;

import lombok.val;
import net.mexish.libs.jdiff.Diff;
import net.mexish.libs.jdiff.Patch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DiffPatchTest {

    @TempDir
    Path tempDir;

    @Test
    void testSimpleStringUpdate() throws Exception {
        val oldContent = "The quick brown fox jumps over the lazy dog.".getBytes(StandardCharsets.UTF_8);
        val newContent = "The quick brown fox jumps over the energetic dog.".getBytes(StandardCharsets.UTF_8);

        verifyDiffAndPatch(oldContent, newContent);
    }

    @Test
    void testBinaryData() throws Exception {
        val random = new Random(12345);
        byte[] oldContent = new byte[1024 * 1024];
        random.nextBytes(oldContent);

        byte[] newContent = oldContent.clone();

        for (int i = 500000; i < 510000; i++) {
            newContent[i] = (byte) (newContent[i] ^ 0xFF);
        }

        byte[] resizedContent = new byte[newContent.length + 1024];
        System.arraycopy(newContent, 0, resizedContent, 0, newContent.length);
        random.nextBytes(resizedContent);

        verifyDiffAndPatch(oldContent, resizedContent);
    }

    @Test
    void testEmptyFiles() throws Exception {
        verifyDiffAndPatch(new byte[0], new byte[0]);
        verifyDiffAndPatch(new byte[10], new byte[0]);
        verifyDiffAndPatch(new byte[0], new byte[10]);
    }

    /**
     * Helper to run the full cycle: Diff -> Patch -> Assert Equals
     */
    private void verifyDiffAndPatch(byte[] oldBytes, byte[] newBytes) throws Exception {
        File oldFile = tempDir.resolve("old.bin").toFile();
        File newFile = tempDir.resolve("new.bin").toFile();
        File patchFile = tempDir.resolve("patch.bin").toFile();
        File patchedFile = tempDir.resolve("patched.bin").toFile();

        Files.write(oldFile.toPath(), oldBytes);
        Files.write(newFile.toPath(), newBytes);

        try (val out = new BufferedOutputStream(new FileOutputStream(patchFile))) {
            Diff.diff(oldBytes, newBytes, out);
        }

        assertTrue(patchFile.exists());
        assertTrue(patchFile.length() > 0, "Patch file should not be empty");

        Patch.patch(oldFile, patchedFile, patchFile);

        byte[] patchedBytes = Files.readAllBytes(patchedFile.toPath());
        assertArrayEquals(newBytes, patchedBytes, "Patched file must match the new version exactly");
    }
}
