package net.mexish.libs.commons.bench;

import lombok.val;
import net.mexish.libs.commons.util.ModernReferenceMap;
import net.mexish.libs.commons.util.NuclearReferenceMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MapBenchmarkTest {

    private Map<Integer, Integer> concurrentMap;
    private ModernReferenceMap<Integer, Integer> modernRefMap;
    private NuclearReferenceMap<Integer, Integer> nuclearRefMap;
    private Map<Integer, Integer> legacySyncMap;

    @Setup(Level.Trial)
    public void setup() {
        concurrentMap = new ConcurrentHashMap<>();
        modernRefMap = new ModernReferenceMap<>(ModernReferenceMap.RefType.WEAK, ModernReferenceMap.RefType.STRONG);
        nuclearRefMap = NuclearReferenceMap.create(ModernReferenceMap.RefType.STRONG, ModernReferenceMap.RefType.STRONG);
        legacySyncMap = Collections.synchronizedMap(new HashMap<>());

        for (int i = 0; i < 1000; i++) {
            concurrentMap.put(i, i);
            modernRefMap.put(i, i);
            legacySyncMap.put(i, i);
        }
    }

    @State(Scope.Thread)
    public static class BenchmarkState {
        public ModernReferenceMap.LookupKey recycledLookupKey = new ModernReferenceMap.LookupKey(500);
    }

    @Benchmark
    @Threads(8)
    public void testConcurrentHashMap_Get(Blackhole bh, BenchmarkState state) {
        bh.consume(concurrentMap.get(state.recycledLookupKey));
    }

    @Benchmark
    @Threads(8)
    public void testLegacySyncMap_Get(Blackhole bh, BenchmarkState state) {
        bh.consume(legacySyncMap.get(state.recycledLookupKey));
    }

    @Benchmark
    @Threads(8)
    public void testNuclearReferenceMap_Get(Blackhole bh, BenchmarkState state) {
        bh.consume(nuclearRefMap.get(state.recycledLookupKey));
    }

    @Benchmark
    @Threads(8)
    public void testModernReferenceMap_Get(Blackhole bh, BenchmarkState state) {
        bh.consume(modernRefMap.getBackingMap().get(state.recycledLookupKey));
    }

    @Test
    public void runBenchmarks() throws Exception {
        Options opt = new OptionsBuilder()
                .include(MapBenchmarkTest.class.getSimpleName())
                .forks(1)
                .jvm("C:\\Users\\mexish\\.jdks\\graalvm-jdk-25\\bin\\java")
                .warmupIterations(2)
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(3))
                .shouldFailOnError(true)
                .jvmArgs("--enable-preview", "-XX:+UseCompactObjectHeaders")
                .build();

        Collection<RunResult> results = new Runner(opt).run();

        double modernRead = getScore(results, "testModernReferenceMap_Get");
        double nuclearRead = getScore(results, "testNuclearReferenceMap_Get");
        double legacyRead = getScore(results, "testLegacySyncMap_Get");

        System.out.println("Modern Read Score: " + modernRead + " ops/ms");
        System.out.println("Nuclear Read Score: " + nuclearRead + " ops/ms");
        System.out.println("Legacy Read Score: " + legacyRead + " ops/ms");

        Assertions.assertTrue(modernRead > legacyRead, "Modern Map Reads should be faster!");
    }

    private double getScore(Collection<RunResult> results, String benchmarkName) {
        return results.stream()
                .filter(r -> r.getParams().getBenchmark().endsWith(benchmarkName))
                .findFirst()
                .map(r -> r.getPrimaryResult().getScore())
                .orElse(0.0);
    }
}