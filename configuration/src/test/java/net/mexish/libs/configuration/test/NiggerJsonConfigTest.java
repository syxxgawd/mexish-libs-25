package net.mexish.libs.configuration.test;

import lombok.val;
import net.mexish.libs.configuration.factory.ConfigurationProviderFactory;
import net.mexish.libs.configuration.provider.ConfigurationProvider;
import net.mexish.libs.configuration.test.adapter.NiggerAdapter;
import net.mexish.libs.configuration.test.type.Nigger;
import net.mexish.libs.configuration.type.Configuration;
import net.mexish.libs.configuration.type.impl.JsonConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public final class NiggerJsonConfigTest {

    private static final String TEST_CONFIG =
                    """
                    {
                    	"name":"nigger",
                    	"age":123,
                    	"penis":{
                    		"hello":"Loser",
                    		"technoblade":"Fake Death"
                    	}
                    }""";

    private static final String MODIFIED_TEST_CONFIG =
                    """
                    {
                    	"name":"nigger",
                    	"age":123,
                    	"penis":{
                    		"hello":"Bro",
                    		"technoblade":"Fake Death"
                    	}
                    }""";

    private static final String NIGGER_TEST_CONFIG =
            """
            {
            	"name":"nigger",
            	"age":123,
            	"nigger":{
            		"name":"Retard",
            		"status":"Retardation"
            	}
            }""";

    private static final String MODIFIED_NIGGER_TEST_CONFIG =
            """
            {
            	"name":"nigger",
            	"age":123,
            	"nigger":{
            		"name":"Retard",
            		"status":"Cotton Picking"
            	}
            }""";

    @Test
    public void checkLoadAndValidateTestParam() throws IOException {
        ConfigurationProvider provider = ConfigurationProviderFactory.createJson();
        Configuration config = provider.provide(TEST_CONFIG);

        assertEquals("nigger", config.getString("name"));
        assertEquals(123, config.getInt("age"));
    }

    @Test
    public void checkSections() throws IOException {
        ConfigurationProvider provider = ConfigurationProviderFactory.createJson();
        Configuration root = provider.provide(TEST_CONFIG);
        Configuration sexion = root.getConfiguration("penis");

        assertNotNull(sexion);
        assertEquals("Loser", sexion.getString("hello"));
        assertEquals("Fake Death", sexion.getString("technoblade"));
    }

    @Test
    public void loadModifyAndSaveConfig() throws IOException {
        ConfigurationProvider provider = ConfigurationProviderFactory.createJson();
        Configuration root = provider.provide(TEST_CONFIG);
        Configuration sexion = root.getConfiguration("penis");

        assertNotNull(sexion);

        sexion.setString("hello", "Bro");

        assertEquals(MODIFIED_TEST_CONFIG, provider.write());
    }

    @Test
    public void loadConfigWithTypeAdapter() throws IOException {
        val provider = ConfigurationProviderFactory.createJson();
        provider.typeAdapter(Nigger.class, new NiggerAdapter());

        Configuration niggerSection = provider.provide(NIGGER_TEST_CONFIG).getConfiguration("nigger");
        assertNotNull(niggerSection);

        Nigger nigger = provider.provide(niggerSection, Nigger.class);

        assertNotNull(nigger);
        assertEquals("Retard", nigger.getName());
        assertEquals("Retardation", nigger.getStatus());
    }

    @Test
    public void loadConfigWithAdapterAndModify() throws IOException {
        val provider = ConfigurationProviderFactory.createJson();
        provider.typeAdapter(Nigger.class, new NiggerAdapter());

        val root = provider.provide(NIGGER_TEST_CONFIG);
        val niggerSection = root.<JsonConfiguration>getConfiguration("nigger");

        assertNotNull(niggerSection);

        Nigger nigger = provider.provide(niggerSection, Nigger.class);
        nigger.setStatus("Cotton Picking");

        assertEquals(MODIFIED_NIGGER_TEST_CONFIG, provider.write(niggerSection, nigger, Nigger.class));
    }

}
