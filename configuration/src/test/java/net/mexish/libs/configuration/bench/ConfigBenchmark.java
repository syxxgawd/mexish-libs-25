package net.mexish.libs.configuration.bench;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;
import net.mexish.libs.configuration.adapter.TypeAdapter;
import net.mexish.libs.configuration.factory.ConfigurationProviderFactory;
import net.mexish.libs.configuration.provider.ConfigurationProvider;
import net.mexish.libs.configuration.type.Configuration;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ConfigBenchmark {

    private static final String JSON_SOURCE = """
            {
            	"name":"benchmark_user",
            	"age":123,
            	"meta":{
            		"status":"active",
            		"server":"node-1"
            	},
            	"entitySection":{
            		"name":"Wrapper",
            		"status":"Processing"
            	}
            }""";

    private ConfigurationProvider provider;
    private Configuration preLoadedConfig;
    private Configuration entitySectionConfig;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BenchmarkEntity {
        String name;
        String status;
    }

    public static class EntityAdapter implements TypeAdapter<BenchmarkEntity> {
        @Override
        public BenchmarkEntity read(Configuration section) {
            return new BenchmarkEntity(
                    section.getString("name"),
                    section.getString("status")
            );
        }

        @Override
        public void write(Configuration section, BenchmarkEntity object) {
            section.setString("name", object.getName());
            section.setString("status", object.getStatus());
        }
    }

    @Setup(Level.Trial)
    public void setup() throws IOException {
        provider = ConfigurationProviderFactory.createJson();
        provider.typeAdapter(BenchmarkEntity.class, new EntityAdapter());

        preLoadedConfig = provider.provide(JSON_SOURCE);
        entitySectionConfig = preLoadedConfig.getConfiguration("entitySection");
    }

    @Benchmark
    public Configuration testParseJson() throws IOException {
        return provider.provide(JSON_SOURCE);
    }

    @Benchmark
    public String testDirectAccess() {
        val root = preLoadedConfig;
        val s1 = root.getString("name");
        val i1 = root.getInt("age");
        val sub = root.getConfiguration("meta");
        assert sub != null;
        val s2 = sub.getString("server");
        return s2;
    }

    @Benchmark
    public BenchmarkEntity testTypeAdapterRead() {
        return (BenchmarkEntity) provider.provide(entitySectionConfig, BenchmarkEntity.class);
    }

    @Benchmark
    public String testSerialize() throws IOException {
        return provider.write();
    }

    @Test
    public void runBenchmarks() throws Exception {
        String javaPath = System.getProperty("bench.java.home");
        if (javaPath == null || javaPath.isEmpty()) {
            javaPath = ProcessHandle.current().info().command().orElse("java");
        }

        Options opt = new OptionsBuilder()
                .include(ConfigBenchmark.class.getSimpleName())
                .forks(1)
                .jvm(javaPath)
                .warmupIterations(2)
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(1))
                .shouldFailOnError(true)
                .jvmArgs("--enable-preview", "-XX:+UseCompactObjectHeaders")
                .build();

        new Runner(opt).run();
    }
}