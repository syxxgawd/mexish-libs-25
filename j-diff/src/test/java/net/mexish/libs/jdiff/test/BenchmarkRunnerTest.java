package net.mexish.libs.jdiff.test;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunnerTest {

    @Test
    public void launchBenchmark() throws Exception {
        val opt = new OptionsBuilder()
                .include(JDiffBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(2)
                .measurementIterations(5)
                .build();

        new Runner(opt).run();
    }
}
