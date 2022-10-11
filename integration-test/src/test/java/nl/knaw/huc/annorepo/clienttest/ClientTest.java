package nl.knaw.huc.annorepo.clienttest;

import nl.knaw.huc.annorepo.client.AnnoRepoClient;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientTest {
    
    private Logger log = LoggerFactory.getLogger(this.getClass()); 
    private static final URI BASE_URL = URI.create("http://localhost:8080");
    private static final String apiKey = "";

//    @Test
    public void testClient() {
        AnnoRepoClient client = new AnnoRepoClient(BASE_URL,apiKey,null);
        boolean success = client.getAbout().fold(
                 e -> { System.out.println(e.toString()); return false; },
                 a -> { System.out.println(a.toString()); return true ; }
        );
        assertTrue(success);
    }

    public static void main(String[] args) {
        new ClientTest().testClient();
    }

}
