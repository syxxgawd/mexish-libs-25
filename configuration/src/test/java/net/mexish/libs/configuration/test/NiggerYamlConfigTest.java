package net.mexish.libs.configuration.test;

import lombok.val;
import net.mexish.libs.configuration.factory.ConfigurationProviderFactory;
import net.mexish.libs.configuration.provider.ConfigurationProvider;
import net.mexish.libs.configuration.test.adapter.NiggerAdapter;
import net.mexish.libs.configuration.test.type.Nigger;
import net.mexish.libs.configuration.type.Configuration;
import net.mexish.libs.configuration.type.impl.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author mexish
 */
public final class NiggerYamlConfigTest {

    private static final String TEST_CONFIG =
            """
                    fuckknowswhat: Hello
                    niggersection:
                        penis: 4
                        hello: 1
                    nigger:
                        name: Retard
                        status: Retardation
                    """;

    private static final String MODIFIED_TEST_CONFIG =
            """
                    fuckknowswhat: Hello
                    niggersection:
                        penis: 4
                        hello: 123
                    nigger:
                        name: Retard
                        status: Retardation
                    """;

    private static final String NIGGER_TEST_CONFIG =
            """
                    nigger:
                        name: Retard
                        status: Retardation
                    """;

    private static final String MODIFIED_NIGGER_TEST_CONFIG =
            """
                    nigger:
                        name: Retard
                        status: Cotton Picking
                    """;

    @Test
    public void checkLoadAndValidateTestParam() throws IOException {
        ConfigurationProvider provider = ConfigurationProviderFactory.createYaml();
        Configuration root = provider.provide(TEST_CONFIG);

        assertEquals("Hello", root.getString("fuckknowswhat"));
    }

    @Test
    public void checkSections() throws IOException {
        ConfigurationProvider provider = ConfigurationProviderFactory.createYaml();
        Configuration root = provider.provide(TEST_CONFIG);
        Configuration sexion = root.getConfiguration("niggersection");

        assertNotNull(sexion);
        assertEquals(4, sexion.getInt("penis"));
        assertEquals(1, sexion.getInt("hello"));
    }

    @Test
    public void loadModifyAndSaveConfig() throws IOException {
        ConfigurationProvider provider = ConfigurationProviderFactory.createYaml();
        Configuration root = provider.provide(TEST_CONFIG);
        Configuration sexion = root.getConfiguration("niggersection");

        assertNotNull(sexion);

        sexion.setInt("hello", 123);

        assertEquals(MODIFIED_TEST_CONFIG, provider.write());
    }

    @Test
    public void loadConfigWithTypeAdapter() throws IOException {
        val provider = ConfigurationProviderFactory.createYaml();
        provider.typeAdapter(Nigger.class, new NiggerAdapter());

        val niggerSection = provider.provide(NIGGER_TEST_CONFIG).getConfiguration("nigger");
        assertNotNull(niggerSection);

        Nigger nigger = provider.provide(niggerSection, Nigger.class);

        assertNotNull(nigger);
        assertEquals("Retard", nigger.getName());
        assertEquals("Retardation", nigger.getStatus());
    }

    @Test
    public void loadConfigWithAdapterAndModify() throws IOException {
        val provider = ConfigurationProviderFactory.createYaml();
        provider.typeAdapter(Nigger.class, new NiggerAdapter());

        val root = provider.provide(NIGGER_TEST_CONFIG);
        val niggerSection = root.<YamlConfiguration>getConfiguration("nigger");

        assertNotNull(niggerSection);

        Nigger nigger = provider.provide(niggerSection, Nigger.class);
        nigger.setStatus("Cotton Picking");

        assertEquals(MODIFIED_NIGGER_TEST_CONFIG, provider.write(niggerSection, nigger, Nigger.class));
    }

}
