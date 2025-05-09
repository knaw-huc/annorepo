package nl.knaw.huc.annorepo.clienttest;

import arrow.core.Either;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.EntityTag;
import nl.knaw.huc.annorepo.api.*;
import nl.knaw.huc.annorepo.client.ARResult;
import nl.knaw.huc.annorepo.client.ARResult.DeleteResult;
import nl.knaw.huc.annorepo.client.ARResult.GetSearchResultPageResult;
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
        void testClientConstructor1() {
            AnnoRepoClient client = new AnnoRepoClient(BASE_URI);
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        void testClientConstructor2() {
            AnnoRepoClient client = new AnnoRepoClient(BASE_URI, apiKey);
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        void testClientConstructor3() {
            AnnoRepoClient client = new AnnoRepoClient(BASE_URI, apiKey, "custom-user-agent");
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        void testCreate1() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URI);
            assertClient(client);
        }

        @Test
        void testCreate1a() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URL);
            assertClient(client);
        }

        @Test
        void testCreate2() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URI, "root");
            assertClient(client);
        }

        @Test
        void testCreate2a() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URL, "root");
            assertClient(client);
        }

        @Test
        void testCreate3() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URI, "root", "custom-user-agent");
            assertClient(client);
        }

        @Test
        void testCreate3a() {
            AnnoRepoClient client = AnnoRepoClient.create(BASE_URL, "root", "custom-user-agent");
            assertClient(client);
        }

        private void assertClient(AnnoRepoClient client) {
            assertThat(client).isNotNull();
            assertThat(client.getServerVersion()).isNotBlank();
        }

        @Test
        void testFailingCreate() {
            AnnoRepoClient client = AnnoRepoClient.create(URI.create("http://nothingtoseehere"));
            assertThat(client).isNull();
        }
    }

    @Nested
    class ContainerTests {
        @Test
        void testCreateContainer() {
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
        void testGetContainer() {
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

        @Test
        void testDeleteContainer() {
            Either<RequestError, Boolean> either = client.createContainer()
                    .map(
                            (ARResult.CreateContainerResult result) -> {
                                String containerName = result.getContainerName();
                                String eTag = result.getETag();
                                client.deleteContainer(containerName, eTag, false).map(
                                        (DeleteResult result2) -> true
                                );
                                return true;
                            }
                    );
            assertThat(either).isInstanceOf(Either.Right.class);
        }
    }

    @Nested
    class AnnotationTests {
        @Test
        void testCreateAnnotation() {
            String containerName = "my-container";
            Map<String, Object> annotation = new WebAnnotation.Builder()
                    .withBody("http://example.org/annotation1")
                    .withTarget("http://example.org/target")
                    .build();
            Boolean success = client.createAnnotation(containerName, annotation, null).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.CreateAnnotationResult result) -> {
                        URI location = result.getLocation();
                        String eTag = result.getETag();
                        String annotationName = result.getAnnotationName();
                        doSomethingWith(annotationName, location, eTag);
                        return true;
                    }
            );
            assertThat(success).isTrue();
        }

        @Test
        void testReadAnnotation() {
            String containerName = "my-container";
            String annotationName = "my-annotation";
            Boolean success = client.getAnnotation(containerName, annotationName).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.GetAnnotationResult result) -> {
                        String eTag = result.getETag();
                        Map<String, Object> annotation = result.getAnnotation();
                        doSomethingWith(annotation, eTag);
                        return true;
                    }
            );
            assertThat(success).isTrue();
        }

        @Test
        void testUpdateAnnotation() {
            String containerName = "my-container";
            String annotationName = "my-annotation";
            String eTag = "abcde";
            Map<String, Object> updatedAnnotation = new WebAnnotation.Builder()
                    .withBody("http://example.org/annotation2")
                    .withTarget("http://example.org/target")
                    .build();
            Boolean success = client.updateAnnotation(containerName, annotationName, eTag, updatedAnnotation).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.CreateAnnotationResult result) -> {
                        URI location = result.getLocation();
                        String newETag = result.getETag();
                        doSomethingWith(annotationName, location, newETag);
                        return true;
                    }
            );
            assertThat(success).isTrue();

        }

        @Test
        void testDeleteAnnotation() {
            String containerName = "my-container";
            String annotationName = "my-annotation";
            String eTag = "abcdefg";
            Boolean success = client.deleteAnnotation(containerName, annotationName, eTag).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (DeleteResult result) -> true
            );
            assertThat(success).isTrue();

        }

        @Test
        void testBatchUpload() {
            String containerName = "my-container";
            Map<String, Object> annotation1 = new WebAnnotation.Builder()
                    .withBody("http://example.org/annotation1")
                    .withTarget("http://example.org/target1")
                    .build();
            Map<String, Object> annotation2 = new WebAnnotation.Builder()
                    .withBody("http://example.org/annotation2")
                    .withTarget("http://example.org/target2")
                    .build();

            List<Map<String, Object>> annotations = List.of(annotation1, annotation2);
            Boolean success = client.batchUpload(containerName, annotations).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.BatchUploadResult result) -> {
                        List<AnnotationIdentifier> annotationIdentifiers = result.getAnnotationData();
                        doSomethingWith(annotationIdentifiers);
                        return true;
                    }
            );
            assertThat(success).isTrue();

        }
    }

    @Nested
    class ContainerSearchTests {

        @Test
        void testCreateSearch() {
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
        void testGetSearchResultPage() {
            String containerName = "volume-1728";
            Map<String, Object> query = Map.of("body.type", "Page");
            Optional<String> optionalQueryId = client.createSearch(containerName, query).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return Optional.empty();
                    },
                    (ARResult.CreateSearchResult result) -> Optional.of(result.getQueryId())
            );
            assertThat(optionalQueryId).isPresent();
            optionalQueryId.ifPresent(queryId -> client.getSearchResultPage(containerName, queryId, 0).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (GetSearchResultPageResult result) -> {
                        AnnotationPage annotationPage = result.getAnnotationPage();
                        doSomethingWith(annotationPage);
                        return true;
                    }
            ));
        }

        @Test
        void testGetSearchInfo() {
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
        void testFilterContainerAnnotations() {
            String containerName = "volume-1728";
            Map<String, Object> query = Map.of("body.type", "Page");
            Boolean success = client.filterContainerAnnotations(containerName, query).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    result -> {
                        Stream<Either<RequestError, String>> annotations = result.getAnnotations();
                        annotations.forEach((a) -> a.fold(
                                e -> {
                                    System.out.println(e);
                                    return false;
                                },
                                r -> {
                                    System.out.println(r);
                                    return true;
                                }
                        ));
                        return true;
                    }

            );
            assertThat(success).isTrue();

        }
    }

    @Nested
    class GlobalSearchTests {

        @Test
        void testCreateGlobalSearch() {
            Map<String, Object> query = Map.of("body.type", "Page");
            Boolean success = client.createGlobalSearch(query).fold(
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
        void testGetGlobalSearchStatus() {
            Map<String, Object> query = Map.of("body.type", "Page");
            Optional<String> optionalQueryId = client.createGlobalSearch(query).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return Optional.empty();
                    },
                    (ARResult.CreateSearchResult result) -> Optional.of(result.getQueryId())
            );
            optionalQueryId.ifPresent(queryId -> {
                Boolean success = client.getGlobalSearchStatus(queryId).fold(
                        (RequestError error) -> {
                            handleError(error);
                            return false;
                        },
                        result -> {
                            SearchStatusSummary searchStatus = result.getSearchStatus();
                            doSomethingWith(searchStatus);
                            return true;
                        }
                );
                assertThat(success).isTrue();
            });
        }

        @Test
        void testGetGlobalSearchResultPage() {
            Map<String, Object> query = Map.of("type", "Annotation");
            Optional<String> optionalQueryId = client.createGlobalSearch(query).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return Optional.empty();
                    },
                    (ARResult.CreateSearchResult result) -> Optional.of(result.getQueryId())
            );
            assertThat(optionalQueryId).isPresent();
            optionalQueryId.ifPresent(queryId -> client.getGlobalSearchResultPage(queryId, 0, true).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (GetSearchResultPageResult result) -> {
                        AnnotationPage annotationPage = result.getAnnotationPage();
                        doSomethingWith(annotationPage);
                        return true;
                    }
            ));
        }

    }

    @Nested
    class IndexTests {
        @Test
        void testIndexCreation() {
            String containerName = "volume-1728";
            String fieldName = "body.type";
            IndexType indexType = IndexType.HASHED;
            Map<String, IndexType> indexDefinition = Map.of(fieldName, indexType);
            Boolean success = client.addIndex(containerName, indexDefinition).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    result -> true
            );
            assertThat(success).isTrue();
        }

        @Test
        void testGetIndex() {
            String containerName = "volume-1728";
            String indexId = "";
            Boolean success = client.getIndex(containerName, indexId).fold(
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
        void testListIndexes() {
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
        void testDeleteIndex() {
            String containerName = "volume-1728";
            String indexId = "index-id";
            Boolean success = client.deleteIndex(containerName, indexId).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (DeleteResult result) -> true
            );
            assertThat(success).isTrue();
        }
    }

    @Nested
    class FieldTests {
        @Test
        void testFieldInfo() {
            String containerName = "volume-1728";
            Boolean success = client.getFieldInfo(containerName).fold(
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
            assertThat(success).isTrue();
        }

        @Test
        void testDistinctFieldValues() {
            String containerName = "volume-1728";
            String fieldName = "body.type";
            Boolean success = client.getDistinctFieldValues(containerName, fieldName).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    result -> {
                        List<Object> distinctValues = result.getDistinctValues();
                        doSomethingWith(distinctValues);
                        return true;
                    }
            );
            assertThat(success).isTrue();
        }
    }

    @Nested
    class AdminTests {
        @Test
        void testUserCreateAndDelete() {
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
//            boolean deletionSuccess = client.deleteUser(userName).isRight();
            boolean deletionSuccess = client.deleteUser(userName).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (DeleteResult result) -> true
            );
            assertThat(deletionSuccess).isTrue();
        }

        @Test
        void testGetUsers() {
            Boolean success = client.getUsers().fold(
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
            assertThat(success).isTrue();
        }

    }

    @Nested
    class ContainerUsersTests {
        @Test
        void testUserCreateAndDelete() {
            String containerName = "my-container";
            String userName = "userName";
            List<ContainerUserEntry> containerUserEntries = List.of(new ContainerUserEntry(userName, Role.EDITOR));
            Boolean success = client.addContainerUsers(containerName, containerUserEntries).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.ContainerUsersResult result) -> {
                        List<ContainerUserEntry> newContainerUsersList = result.getContainerUserEntries();
                        doSomethingWith(newContainerUsersList);
                        return true;
                    }
            );
            assertThat(success).isTrue();
        }

        @Test
        void testGetContainerUsers() {
            String containerName = "my-container";
            Boolean success = client.getContainerUsers(containerName).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.ContainerUsersResult result) -> {
                        List<ContainerUserEntry> containerUserEntries = result.getContainerUserEntries();
                        doSomethingWith(containerUserEntries);
                        return true;
                    }
            );
            assertThat(success).isTrue();
        }

        @Test
        void testDeleteContainerUser() {
            String containerName = "my-container";
            String userName = "userName";
//            boolean deletionSuccess = client.deleteContainerUser(containerName, userName).isRight();
            boolean deletionSuccess = client.deleteContainerUser(containerName, userName).fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (DeleteResult result) -> true

            );
            assertThat(deletionSuccess).isTrue();
        }
    }

    @Nested
    class MyTests {
        @Test
        void testGetMyContainers() {
            Boolean success = client.getMyContainers().fold(
                    (RequestError error) -> {
                        handleError(error);
                        return false;
                    },
                    (ARResult.MyContainersResult result) -> {
                        Map<String, List<String>> containerMap = result.getContainers();
                        doSomethingWith(containerMap);
                        return true;
                    }
            );
            assertThat(success).isTrue();
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
    void testAbout() {
        var getAboutResult = client.getAbout().getOrNull();
        assertThat(getAboutResult).isNotNull();
        Map<String, Object> aboutInfo = getAboutResult.getAboutInfo();
        doSomethingWith(aboutInfo);
        assertThat(aboutInfo).isNotNull();
        assertThat(aboutInfo.get("appName")).isEqualTo("AnnoRepo");
    }

}