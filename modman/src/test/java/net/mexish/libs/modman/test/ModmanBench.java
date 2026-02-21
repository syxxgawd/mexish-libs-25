package net.mexish.libs.modman.test;

import net.mexish.libs.modman.ModuleManager;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ModmanBench {

    private Path tempDir;
    private Path modulesDir;

    @Setup(Level.Trial)
    public void setupEnvironment() throws IOException {
        tempDir = Files.createTempDirectory("mexish-bench-");
        modulesDir = tempDir.resolve("modules");
        Files.createDirectories(modulesDir);

        Path compileDir = tempDir.resolve("classes");
        Files.createDirectories(compileDir);
        compileDummyClass(compileDir);

        for (int i = 0; i < 100; i++) {
            String name = "Module-" + i;
            String dependency = (i > 0 && i % 5 != 0) ? "Module-" + (i - 1) : null;
            createModuleJar(name, dependency, compileDir);
        }
    }

    @TearDown(Level.Trial)
    public void cleanup() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Benchmark
    public void testFullLifecycle() throws Exception {
        ModuleManager manager = new ModuleManager(modulesDir);
        manager.gather();
        manager.load();
        manager.init();
        manager.shutdown();
    }

    private void createModuleJar(String name, String dependency, Path classesDir) throws IOException {
        Path jarPath = modulesDir.resolve(name + ".jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            JarEntry metaEntry = new JarEntry("meta.json");
            jos.putNextEntry(metaEntry);
            String json = "{\n" +
                    "  \"name\": \"" + name + "\",\n" +
                    "  \"main\": \"TestModule\",\n" +
                    "  \"version\": \"1.0.0\",\n" +
                    "  \"author\": \"Benchmark\",\n" +
                    (dependency != null ? "  \"depend\": [\"" + dependency + "\"]\n" : "") +
                    "}";
            jos.write(json.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            JarEntry classEntry = new JarEntry("TestModule.class");
            jos.putNextEntry(classEntry);
            Files.copy(classesDir.resolve("TestModule.class"), jos);
            jos.closeEntry();
        }
    }

    private void compileDummyClass(Path outputDir) {
        String source = "import net.mexish.libs.modman.annotation.LifecycleEventHandler;\n" +
                "import net.mexish.libs.modman.ModuleLifecycleEvent;\n" +
                "public class TestModule {\n" +
                "    @LifecycleEventHandler(ModuleLifecycleEvent.LOAD)\n" +
                "    public static void onLoad() { }\n" +
                "    @LifecycleEventHandler(ModuleLifecycleEvent.INIT)\n" +
                "    public static void onInit() { }\n" +
                "    @LifecycleEventHandler(ModuleLifecycleEvent.SHUTDOWN)\n" +
                "    public static void onShutdown() { }\n" +
                "}";

        Path sourceFile = outputDir.resolve("TestModule.java");
        try {
            Files.writeString(sourceFile, source);
            ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "-d", outputDir.toString(),
                    sourceFile.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to compile dummy module", e);
        }
    }

    @Test
    public void runBenchmarks() throws Exception {
        String javaPath = System.getProperty("bench.java.home");
        if (javaPath == null || javaPath.isEmpty()) {
            javaPath = ProcessHandle.current().info().command().orElse("java");
        }

        Options opt = new OptionsBuilder()
                .include(ModmanBench.class.getSimpleName())
                .forks(1)
                .jvm(javaPath)
                .warmupIterations(2)
                .measurementIterations(5)
                .shouldFailOnError(true)
                .jvmArgs("--enable-preview", "-XX:+UseCompactObjectHeaders")
                .build();

        new Runner(opt).run();
    }
}