package nl.knaw.huc.annorepo.clienttest;

import nl.knaw.huc.annorepo.api.AboutInfo;
import nl.knaw.huc.annorepo.client.AnnoRepoClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static nl.knaw.huc.annorepo.clienttest.Util.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TEST)
public class IntegratedClientTester {
//    Intentionally not named ClientTest, so mvn test will skip these integration tests

    private static Logger log = LoggerFactory.getLogger(IntegratedClientTester.class);
    public static final String BASE_URL = "http://localhost:8080";
    private static final URI BASE_URI = URI.create(BASE_URL);
    private static final String apiKey = "";
    static AnnoRepoClient client;

    @BeforeAll
    public static void beforeAll() {
        client = new AnnoRepoClient(BASE_URI, apiKey, "integrated-client-tester");
    }

    @Nested
    class ConstructorTests {
        @Test
        public void testClientConstructor1() {
            AnnoRepoClient client = new AnnoRepoClient(BASE_URI);
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testClientConstructor2() {
            AnnoRepoClient client = new AnnoRepoClient(BASE_URI, apiKey);
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testClientConstructor3() {
            AnnoRepoClient client = new AnnoRepoClient(BASE_URI, apiKey, "custom-user-agent");
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testCreate1() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URI);
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testCreate1a() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URL);
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testCreate2() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URI, "root");
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testCreate2a() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URL, "root");
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testCreate3() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URI, "root", "custom-user-agent");
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testCreate3a() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URL, "root", "custom-user-agent");
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testFailingCreate() {
            AnnoRepoClient client = AnnoRepoClient.create(URI.create("http://nothingtoseehere"));
            assertThat(client).isNull();
        }
    }

    @Test
    public void testAbout() {
        var getAboutResult = client.getAbout().orNull();
        AboutInfo aboutInfo = getAboutResult.getAboutInfo();
        log.info("aboutInfo={}", aboutInfo);
        assertThat(aboutInfo).isNotNull();
        assertThat(aboutInfo.getAppName()).isEqualTo("AnnoRepo");
    }

}