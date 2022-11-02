package nl.knaw.huc.annorepo.clienttest;

import arrow.core.Either;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huc.annorepo.api.*;
import nl.knaw.huc.annorepo.client.ARResult;
import nl.knaw.huc.annorepo.client.AnnoRepoClient;
import nl.knaw.huc.annorepo.client.RequestError;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.EntityTag;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public class ContainerTests {
        @Test
        public void testCreateContainer() {
            String preferredName = "my-container";
            String label = "A container for all my annotations";
            Boolean success = client.createContainer(preferredName, label).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.CreateContainerResult result) -> {
                        String containerName = result.getContainerName();
                        URI location = result.getLocation();
                        String eTag = result.getETag();
                        doSomethingWith(containerName, location, eTag);
                        return true;
                    }
            );
            assertThat(success).isTrue();
        }

        @Test
        public void testGetContainer() {
            Either<RequestError, Boolean> either = client.createContainer()
                    .map(
                            (ARResult.CreateContainerResult result) -> {
                                String containerName = result.getContainerName();
                                client.getContainer(containerName).map(
                                        (ARResult.GetContainerResult result2) -> {
                                            String eTag1 = result2.getETag();
                                            String entity = result2.getEntity();
                                            EntityTag entityTag = result2.getResponse().getEntityTag();
                                            doSomethingWith(eTag1, entity, entityTag);
                                            return true;
                                        }
                                );
                                return true;
                            }
                    );
            assertThat(either).isInstanceOf(Either.Right.class);
        }
    }

    @Nested
    public class SearchTests {

        @Test
        public void testCreateSearch() {
            String containerName = "volume-1728";
            Map<String, Object> query = Map.of("body.type", "Page");
            Boolean success = client.createSearch(containerName, query).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.CreateSearchResult result) -> {
                        URI location = result.getLocation();
                        String queryId = result.getQueryId();
                        doSomethingWith(location, queryId);
                        return true;
                    }
            );
            assertThat(success).isTrue();
        }

        @Test
        public void testGetSearchResultPage() {
            String containerName = "volume-1728";
            Map<String, Object> query = Map.of("body.type", "Page");
            Optional<String> optionalQueryId = client.createSearch(containerName, query).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return Optional.empty();
                    },
                    (ARResult.CreateSearchResult result) -> Optional.of(result.getQueryId())
            );
            optionalQueryId.ifPresent(queryId -> {
                client.getSearchResultPage(containerName, queryId, 0).fold(
                        (RequestError error) -> {
                            handleError(error);
                            return false;
                        },
                        result -> {
                            AnnotationPage annotationPage = result.getAnnotationPage();
                            doSomethingWith(annotationPage);
                            return true;
                        }
                );
            });
        }

        @Test
        public void testGetSearchInfo() {
            String containerName = "volume-1728";
            Map<String, Object> query = Map.of("body.type", "Page");
            Optional<String> optionalQueryId = client.createSearch(containerName, query).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return Optional.empty();
                    },
                    (ARResult.CreateSearchResult result) -> Optional.of(result.getQueryId())
            );
            optionalQueryId.ifPresent(queryId -> {
                Boolean success = client.getSearchInfo(containerName, queryId).fold(
                        (RequestError error) -> {
                            handleError(error);
                            return false;
                        },
                        result -> {
                            SearchInfo searchInfo = result.getSearchInfo();
                            doSomethingWith(searchInfo);
                            return true;
                        }
                );
                assertThat(success).isTrue();
            });
        }

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
    public class IndexTests {
        @Test
        public void testIndexCreation() {
            String containerName = "volume-1728";
            String fieldName = "body.type";
            IndexType indexType = IndexType.HASHED;
            Boolean success = client.addIndex(containerName, fieldName, indexType).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    result -> true
            );
            assertThat(success).isTrue();
        }

        @Test
        public void testGetIndex() {
            String containerName = "volume-1728";
            String fieldName = "body.type";
            IndexType indexType = IndexType.HASHED;
            Boolean success = client.getIndex(containerName, fieldName, indexType).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.GetIndexResult result) -> {
                        IndexConfig indexConfig = result.getIndexConfig();
                        doSomethingWith(indexConfig);
                        return true;
                    }
            );
            assertThat(success).isTrue();
        }

        @Test
        public void testListIndexes() {
            String containerName = "volume-1728";
            Boolean success = client.listIndexes(containerName).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.ListIndexesResult result) -> {
                        List<IndexConfig> indexes = result.getIndexes();
                        doSomethingWith(indexes);
                        return true;
                    }
            );
            assertThat(success).isTrue();
        }

        @Test
        public void testDeleteIndex() {
            String containerName = "volume-1728";
            String fieldName = "body.type";
            IndexType indexType = IndexType.HASHED;
            Boolean success = client.deleteIndex(containerName, fieldName, indexType).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.DeleteIndexResult result) -> true
            );
            assertThat(success).isTrue();
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