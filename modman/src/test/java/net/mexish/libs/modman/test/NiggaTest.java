package net.mexish.libs.modman.test;

import lombok.val;
import net.mexish.libs.modman.dependency.DependencyGraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class NiggaTest {

    @Test
    public void testDependencyGraph() {
        val graph = new DependencyGraph();

        // moduleA -> moduleB -> moduleC
        // ^             |            |
        // |          moduleD -> moduleF
        // |             |            |
        // |          moduleE         |
        // |             |            |
        // |--------------------------|
        // CIRCULAR

        graph.registerDependency("moduleA", "moduleB");
        graph.registerDependency("moduleB", "moduleC");
        graph.registerDependency("moduleB", "moduleD");
        graph.registerDependency("moduleD", "moduleE");
        graph.registerDependency("moduleD", "moduleF");

        assertTrue(graph.isCircular("moduleC", "moduleA"));
        assertFalse(graph.isCircular("moduleB", "moduleC"));
        assertTrue(graph.isCircular("moduleD", "moduleA"));
        assertTrue(graph.isCircular("moduleE", "moduleA"));
        assertTrue(graph.isCircular("moduleF", "moduleB"));
        assertTrue(graph.isCircular("moduleF", "moduleA"));
    }

}
