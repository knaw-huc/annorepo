package nl.knaw.huc.annorepo.clienttest;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.net.URI;

/**
 * The type Client test.
 */
public class ClientTest {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private static final URI BASE_URI = URI.create("http://localhost:8080");
    private static final String apiKey = "";

    /**
     * Test client.
     */
    @Test
    public void testClient() {
//        AnnoRepoClient client = new AnnoRepoClient(BASE_URI);
//        AnnoRepoClient client2 = new AnnoRepoClient(BASE_URI, apiKey);
//        AnnoRepoClient client3 = new AnnoRepoClient(BASE_URI, apiKey, "test-client");
//        String serverVersion = client.getServerVersion();
//        Boolean serverNeedsAuthentication = client.getServerNeedsAuthentication();
//        Either<RequestError, ARResult.GetAboutResult> aboutResult = client.getAbout();
//        boolean success = client.getAbout().fold(
//                e -> {
//                    System.out.println(e.toString());
//                    return false;
//                },
//                a -> {
//                    System.out.println(a.toString());
//                    return true;
//                }
//        );
//        assertTrue(success);
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        new ClientTest().testClient();
    }

}
