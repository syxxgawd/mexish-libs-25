package net.mexish.libs.commons.bench;

import net.mexish.libs.commons.util.ModernReferenceMap;
import net.mexish.libs.commons.util.NuclearReferenceMap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.mexish.libs.commons.util.ModernReferenceMap.RefType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Random;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@Fork(1)
public class MapRandomizedBenchmark {

    private static final int KEY_COUNT = 1_048_576;
    private final Integer[] keys = new Integer[KEY_COUNT];
    private int index = 0;

    private ConcurrentHashMap<Integer, Integer> chm;
    private NuclearReferenceMap<Integer, Integer> nuclearMap;
    private ModernReferenceMap<Integer, Integer> modernMap;

    @Setup
    public void setup() {
        chm = new ConcurrentHashMap<>(KEY_COUNT);

        nuclearMap = NuclearReferenceMap.create(RefType.STRONG, RefType.STRONG);

        modernMap = new ModernReferenceMap<>(RefType.STRONG, RefType.STRONG);

        Random random = new Random(42);
        for (int i = 0; i < KEY_COUNT; i++) {
            keys[i] = random.nextInt();
            chm.put(keys[i], i);
            nuclearMap.put(keys[i], i);
            modernMap.put(keys[i], i);
        }

        System.out.println("Setup complete. Validating map integrity...");
        validate();
    }

    @Test
    public void runBenchmarks() throws Exception {
        Options opt = new OptionsBuilder()
                .include(MapRandomizedBenchmark.class.getSimpleName())
                .forks(1)
                .jvm("C:\\Users\\mexish\\.jdks\\graalvm-jdk-25\\bin\\java")
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(5))
                .shouldFailOnError(true)
                .jvmArgs("-XX:+UseParallelGC",
                        "--enable-preview",
                        "-XX:+UseCompactObjectHeaders",
                        "-Xms8g",
                        "-Xmx8g",
                        "-XX:+AlwaysPreTouch",
                        "-XX:+UseLargePages",
                        "-XX:+ParallelRefProcEnabled",
                        "-XX:ConcGCThreads=4",
                        "-XX:SoftRefLRUPolicyMSPerMB=0",
                        "-XX:NewRatio=2",
                        "-XX:SurvivorRatio=8",
                        "-XX:MaxTenuringThreshold=15")
                .build();

        new Runner(opt).run();
    }

    @Test
    public void validate() {
        if (chm == null) setup();

        for (int i = 0; i < 1000; i++) {
            Integer testKey = keys[i];
            Integer expectedValue = i;

            assertEquals(expectedValue, chm.get(testKey), "CHM Mismatch at index " + i);
            assertEquals(expectedValue, nuclearMap.get(testKey), "Nuclear Mismatch at index " + i);
        }

        System.out.println("Validation passed: All 1000 sample keys matched perfectly.");
    }

    @Benchmark
    @Threads(16)
    public void testCHM_RandomGet(Blackhole bh) {
        bh.consume(chm.get(keys[index++ & (KEY_COUNT - 1)]));
    }

    @Benchmark
    @Threads(16)
    public void testModernMap_RandomGet(Blackhole bh) {
        bh.consume(modernMap.get(keys[index++ & (KEY_COUNT - 1)]));
    }

    @Benchmark
    @Threads(16)
    public void testNuclearMap_RandomGet(Blackhole bh) {
        bh.consume(nuclearMap.get(keys[index++ & (KEY_COUNT - 1)]));
    }
}