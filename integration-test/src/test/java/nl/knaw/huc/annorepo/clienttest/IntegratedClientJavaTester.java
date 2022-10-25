package nl.knaw.huc.annorepo.clienttest;

import arrow.core.Either;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.annorepo.api.AboutInfo;
import nl.knaw.huc.annorepo.api.RejectedUserEntry;
import nl.knaw.huc.annorepo.api.UserEntry;
import nl.knaw.huc.annorepo.client.ARResult;
import nl.knaw.huc.annorepo.client.AnnoRepoClient;
import nl.knaw.huc.annorepo.client.RequestError;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegratedClientJavaTester {
//    Intentionally not named ClientTest, so mvn test will skip these integration tests

    private static final Logger log = LoggerFactory.getLogger(IntegratedClientJavaTester.class);
    public static final String BASE_URL = "http://localhost:8080";
    private static final URI BASE_URI = URI.create(BASE_URL);
    private static final String apiKey = "root";
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
            assertClient(client);
        }

        @Test
        public void testCreate1a() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URL);
            assertClient(client);
        }

        @Test
        public void testCreate2() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URI, "root");
            assertClient(client);
        }

        @Test
        public void testCreate2a() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URL, "root");
            assertClient(client);
        }

        @Test
        public void testCreate3() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URI, "root", "custom-user-agent");
            assertClient(client);
        }

        @Test
        public void testCreate3a() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URL, "root", "custom-user-agent");
            assertClient(client);
        }

        private void assertClient(AnnoRepoClient client) {
            assertThat(client).isNotNull();
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        public void testFailingCreate() {
            AnnoRepoClient client = AnnoRepoClient.create(URI.create("http://nothingtoseehere"));
            assertThat(client).isNull();
        }
    }

    @Nested
    public class SearchTests {
        @Test
        public void testFilterContainerAnnotations() {
            String containerName = "volume-1728";
            Map<String, Object> query = Map.of("body.type", "Page");
            client.filterContainerAnnotations(containerName, query).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    result -> {
                        Stream<Either<RequestError, String>> annotations = result.getAnnotations();
                        annotations.forEach((a) -> {
                            a.fold(
                                    e -> {
                                        System.out.println(e);
                                        return false;
                                    },
                                    r -> {
                                        System.out.println(r);
                                        return true;
                                    }
                            );
                        });
                        return true;
                    }

            );
        }
    }

    @Nested
    public class FieldInfoTests {
        @Test
        public void testFieldInfo() {
            String containerName = "volume-1728";
            client.getFieldInfo(containerName).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    result -> {
                        Map<String, Integer> fieldInfo = result.getFieldInfo();
                        doSomethingWith(fieldInfo);
                        return true;
                    }
            );
        }
    }

    @Nested
    public class AdminTests {
        @Test
        public void testUserCreateAndDelete() {
            String userName = "userName";
            List<UserEntry> userEntries = List.of(new UserEntry(userName, "apiKey"));
            client.addUsers(userEntries).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.AddUsersResult result) -> {
                        List<String> accepted = result.getAccepted();
                        List<RejectedUserEntry> rejected = result.getRejected();
                        doSomethingWith(accepted, rejected);
                        return true;
                    }
            );
            boolean deletionSuccess = client.deleteUser(userName).isRight();
            assertThat(deletionSuccess).isTrue();
        }

        @Test
        public void testGetUsers() {
            client.getUsers().fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.UsersResult result) -> {
                        List<UserEntry> userEntries = result.getUserEntries();
                        doSomethingWith(userEntries);
                        return true;
                    }
            );
        }

    }

    private static void handleError(RequestError error) {
        System.out.println(error.getMessage());
    }

    private void doSomethingWith(Object... objects) {
        for (Object o : objects) {
            try {
                log.info("{}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testAbout() {
        var getAboutResult = client.getAbout().orNull();
        AboutInfo aboutInfo = getAboutResult.getAboutInfo();
        doSomethingWith(aboutInfo);
        assertThat(aboutInfo).isNotNull();
        assertThat(aboutInfo.getAppName()).isEqualTo("AnnoRepo");
    }

}