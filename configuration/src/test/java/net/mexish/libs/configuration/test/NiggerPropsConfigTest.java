package net.mexish.libs.configuration.test;

import lombok.val;
import net.mexish.libs.configuration.factory.ConfigurationProviderFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public final class NiggerPropsConfigTest {

    private static final String TEST_CONFIG = """
            name=Nigger
            age=36
            mortgage=Not Paid (50 years 50000000% interest)
            kids=7
            location=Section 8
            occupation=Cotton Picking
            """;

    @Test
    public void loadTestConfigAndCheck() throws IOException {
        val provider = ConfigurationProviderFactory.createProps();
        val configuration = provider.provide(TEST_CONFIG);

        assertEquals("Nigger", configuration.getString("name"));
        assertEquals("Section 8", configuration.getString("location"));
        assertEquals(7, configuration.getInt("kids"));
    }

    @Test
    public void loadTestConfigModifyAndSave() throws IOException {
        val provider = ConfigurationProviderFactory.createProps();
        var configuration = provider.provide(TEST_CONFIG);

        configuration.setString("location", "Murino");
        configuration.setString("occupation", "Manyak");

        val str = provider.write();
        configuration = provider.provide(str);

        assertEquals("Murino", configuration.getString("location"));
        assertEquals("Manyak", configuration.getString("occupation"));
    }

}
