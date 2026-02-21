package net.mexish.libs.commons.bench;

import net.mexish.libs.commons.util.IndexMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class IndexMapBenchmarkTest {

    private IndexMap<TestEntity> indexMap;

    // A dummy entity to store
    static class TestEntity {
        final int id;
        final String name;

        TestEntity(int id) {
            this.id = id;
            this.name = "Entity-" + id;
        }

        public int getId() {
            return id;
        }
        public String getName() {
            return name;
        }
    }

    @Setup(Level.Trial)
    public void setup() {
        indexMap = IndexMap.newMap();

        indexMap.registerIntIndex(TestEntity::getId);
        indexMap.registerIndex(String.class, TestEntity::getName);

        for (int i = 0; i < 10000; i++) {
            indexMap.store(new TestEntity(i));
        }
    }

    @Benchmark
    @Threads(4)
    public TestEntity testIntLookup() {
        return indexMap.get(5000);
    }

    @Benchmark
    @Threads(4)
    public TestEntity testObjectLookup() {
        return indexMap.get(String.class, "Entity-5000");
    }

    @Benchmark
    @Threads(4)
    public void testWriteCycle() {
        TestEntity newEntity = new TestEntity(99999);
        indexMap.store(newEntity);
        indexMap.remove(newEntity);
    }

    @Test
    public void runBenchmarks() throws Exception {
        Options opt = new OptionsBuilder()
                .include(IndexMapBenchmarkTest.class.getSimpleName())
                .forks(1)
                .jvm("C:\\Users\\mexish\\.jdks\\graalvm-jdk-25\\bin\\java")
                .warmupIterations(2)
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(1))
                .shouldFailOnError(true)
                .jvmArgs("--enable-preview", "-XX:+UseCompactObjectHeaders")
                .build();

        Collection<RunResult> results = new Runner(opt).run();

        double intLookupScore = getScore(results, "testIntLookup");
        double objLookupScore = getScore(results, "testObjectLookup");

        System.out.println("Int Lookup Throughput: " + intLookupScore + " ops/ms");
        System.out.println("Obj Lookup Throughput: " + objLookupScore + " ops/ms");

        // Sanity Check: It should be incredibly fast (> 1M ops/ms on modern hardware)
        Assertions.assertTrue(intLookupScore > 500, "IndexMap is too slow!");
    }

    private double getScore(Collection<RunResult> results, String benchmarkName) {
        return results.stream()
                .filter(r -> r.getParams().getBenchmark().endsWith(benchmarkName))
                .findFirst()
                .map(r -> r.getPrimaryResult().getScore())
                .orElse(0.0);
    }
}