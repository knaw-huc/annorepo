import java.net.URI
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import arrow.core.Either
import arrow.core.Either.Right
import arrow.core.raise.either
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.api.WebAnnotation
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.client.ARResult
import nl.knaw.huc.annorepo.client.ARResult.AddIndexResult
import nl.knaw.huc.annorepo.client.ARResult.AddUsersResult
import nl.knaw.huc.annorepo.client.ARResult.AnnotationFieldInfoResult
import nl.knaw.huc.annorepo.client.ARResult.BatchUploadResult
import nl.knaw.huc.annorepo.client.ARResult.ContainerUsersResult
import nl.knaw.huc.annorepo.client.ARResult.CreateAnnotationResult
import nl.knaw.huc.annorepo.client.ARResult.CreateContainerResult
import nl.knaw.huc.annorepo.client.ARResult.CreateSearchResult
import nl.knaw.huc.annorepo.client.ARResult.DeleteAnnotationResult
import nl.knaw.huc.annorepo.client.ARResult.DeleteIndexResult
import nl.knaw.huc.annorepo.client.ARResult.GetAnnotationResult
import nl.knaw.huc.annorepo.client.ARResult.GetGlobalSearchStatusResult
import nl.knaw.huc.annorepo.client.ARResult.GetIndexCreationStatusResult
import nl.knaw.huc.annorepo.client.ARResult.GetIndexResult
import nl.knaw.huc.annorepo.client.ARResult.GetSearchResultPageResult
import nl.knaw.huc.annorepo.client.ARResult.ListIndexesResult
import nl.knaw.huc.annorepo.client.ARResult.MyContainersResult
import nl.knaw.huc.annorepo.client.ARResult.UsersResult
import nl.knaw.huc.annorepo.client.AnnoRepoClient
import nl.knaw.huc.annorepo.client.AnnoRepoClient.Companion.create
import nl.knaw.huc.annorepo.client.FilterContainerAnnotationsResult
import nl.knaw.huc.annorepo.client.RequestError
import nl.knaw.huc.annorepo.client.untangled

class IntegratedClientKotlinTester {
    //    Intentionally not named ClientTest, so mvn test will skip these integration tests

    @Nested
    inner class ConstructorTests {
        @Test
        fun testClientConstructor1() {
            val client = AnnoRepoClient(BASE_URI)
            assertThat(client.serverVersion).isNotBlank
        }

        @Test
        fun testClientConstructor2() {
            val client = AnnoRepoClient(BASE_URI, apiKey)
            assertThat(client.serverVersion).isNotBlank
        }

        @Test
        fun testClientConstructor3() {
            val client = AnnoRepoClient(
                BASE_URI, apiKey, "custom-user-agent"
            )
            assertThat(client.serverVersion).isNotBlank
        }

        @Test
        fun testCreate1() {
            val client = create(BASE_URI)
            assertClient(client)
        }

        @Test
        fun testCreate1a() {
            val client = create(BASE_URL)
            assertClient(client)
        }

        @Test
        fun testCreate2() {
            val client = create(BASE_URI, "root")
            assertClient(client)
        }

        @Test
        fun testCreate2a() {
            val client = create(BASE_URL, "root")
            assertClient(client)
        }

        @Test
        fun testCreate3() {
            val client = create(BASE_URI, "root", "custom-user-agent")
            assertClient(client)
        }

        @Test
        fun testCreate3a() {
            val client = create(BASE_URL, "root", "custom-user-agent")
            assertClient(client)
        }

        private fun assertClient(client: AnnoRepoClient?) {
            assertThat(client).isNotNull
            assertThat(client!!.serverVersion).isNotBlank
        }

        @Test
        fun testFailingCreate() {
            val client = create(URI.create("http://nothingtoseehere"))
            assertThat(client).isNull()
        }
    }

    @Nested
    inner class ContainerTests {
        @Test
        fun testCreateContainer() {
            val preferredName = "my-container"
            val label = "A container for all my annotations"
            val success = client.createContainer(preferredName, label).fold({ error: RequestError ->
                handleError(error)
                false
            }) { (_, location, containerName, eTag): CreateContainerResult ->
                doSomethingWith(containerName, location, eTag)
                true
            }
            assertThat(success).isTrue
        }

        @Test
        fun testGetContainer() {
            either {
                val containerName = client.createContainer().bind().containerName
                val (response, entity, eTag1) = client.getContainer(containerName).bind()
                val entityTag = response.entityTag
                doSomethingWith(eTag1, entity, entityTag)
            }.mapLeft<Void> { requestError ->
                log.error("error=$requestError")
                fail(requestError.message)
            }
        }

        @Test
        fun testDeleteContainer() {
            either {
                val (response, location, containerName, eTag) = client.createContainer().bind()
                doSomethingWith(response.status, location)
                client.deleteContainer(containerName, eTag)
            }.mapLeft<Void> { requestError ->
                log.error("error=$requestError")
                fail(requestError.message)
            }
        }

        @Test
        fun testSetReadOlyAccessForAnonymousUser() {
            either {
                val (response, location, containerName, eTag) = client.createContainer().bind()
                val (response2) = client.setAnonymousUserReadAccess(containerName, true).bind()
                doSomethingWith(response2.status)
                client.deleteContainer(containerName, eTag)
            }.mapLeft<Void> { requestError ->
                log.error("error=$requestError")
                fail(requestError.message)
            }
        }
    }

    @Nested
    inner class AnnotationTests {
        @Test
        fun testAnnotationCRUD() {
            val containerName = "my-container"
            val annotation = WebAnnotation.Builder()
                .withBody("http://example.org/annotation1")
                .withTarget("http://example.org/target")
                .build()

            // Create
            client.createAnnotation(containerName, annotation, null).fold(
                { error: RequestError ->
                    handleError(error)
                    false
                },
                { (_, location, _, annotationName, eTag): CreateAnnotationResult ->
                    continueWithRead(annotationName, location, eTag, containerName)
                    true
                })
        }

        private fun continueWithRead(
            annotationName: String,
            location: URI,
            eTag: String,
            containerName: String
        ) {
            doSomethingWith(annotationName, location, eTag)

            // Read
            client.getAnnotation(containerName, annotationName).fold(
                { error: RequestError ->
                    handleError(error)
                    false
                },
                { (_, eTag, annotation): GetAnnotationResult ->
                    continueWithUpdate(annotation, eTag, containerName, annotationName)
                })
        }

        private fun continueWithUpdate(
            annotation: WebAnnotationAsMap,
            eTag: String,
            containerName: String,
            annotationName: String
        ): Boolean {
            doSomethingWith(annotation, eTag)

            // Update
            val updatedAnnotation = WebAnnotation.Builder().withBody("http://example.org/annotation2")
                .withTarget("http://example.org/target").build()
            client.updateAnnotation(containerName, annotationName, eTag, updatedAnnotation)
                .fold(
                    { error: RequestError ->
                        handleError(error)
                        false
                    },
                    { (_, location, _, _, newETag): CreateAnnotationResult ->
                        continueWithDelete(annotationName, location, newETag, containerName, eTag)
                    })
            return true
        }

        private fun continueWithDelete(
            annotationName: String,
            location: URI,
            newETag: String,
            containerName: String,
            eTag: String
        ): Boolean {
            doSomethingWith(annotationName, location, newETag)
            // Delete
            val success = client.deleteAnnotation(containerName, annotationName, eTag)
                .fold(
                    { error: RequestError ->
                        handleError(error)
                        false
                    },
                    { _: DeleteAnnotationResult -> true })
            assertThat(success).isTrue
            return true
        }

        @Test
        fun testBatchUpload() {
            val containerName = "my-container"
            val annotation1 = WebAnnotation.Builder().withBody("http://example.org/annotation1")
                .withTarget("http://example.org/target1").build()
            val annotation2 = WebAnnotation.Builder().withBody("http://example.org/annotation2")
                .withTarget("http://example.org/target2").build()
            val annotations = listOf(annotation1, annotation2)
            val success = client.batchUpload(containerName, annotations).fold({ error: RequestError ->
                handleError(error)
                false
            }, { (_, annotationIdentifiers): BatchUploadResult ->
                doSomethingWith(annotationIdentifiers)
                true
            })
            assertThat(success).isTrue
        }

    }

    @Nested
    inner class SearchTests {
        @Test
        fun testFilterContainerAnnotations() {
            val containerName = "republic"
            val query = mapOf("body.type" to "Page")
            client.filterContainerAnnotations(containerName, query).fold({ error: RequestError -> handleError(error) },
                { (searchId, annotations): FilterContainerAnnotationsResult ->
                    annotations.forEach { a: Either<RequestError, String> ->
                        a.fold({ e: RequestError -> handleError(e) },
                            { jsonString: String -> doSomethingWith(searchId, jsonString) })
                    }
                })
        }

        @Test
        fun testFilterContainerAnnotations2() {
            val containerName = "republic"
            val query = mapOf("body.type" to "Page")

            val e: Either<RequestError, List<String>> =
                client.filterContainerAnnotations2(containerName, query).untangled()
            when (e) {
                is Either.Left -> println(e.value)
                is Right -> println(e.value.size)
            }
        }
    }

    @Nested
    inner class GlobalSearchTests {
        @Test
        fun testCreateGlobalSearch() {
            val query = mapOf("body.type" to "Page")
            val success = client.createGlobalSearch(query = query).fold(
                { error: RequestError ->
                    handleError(error)
                    false
                },
                { (_, location, queryId): CreateSearchResult ->
                    doSomethingWith(location, queryId)
                    true
                })
            assertThat(success).isTrue()
        }

        @Test
        fun testGetGlobalSearchStatus() {
            val query = mapOf("body.type" to "Page")
            val optionalQueryId = client.createGlobalSearch(query = query).fold(
                { error: RequestError ->
                    handleError(error)
                    null
                },
                { (_, _, queryId): CreateSearchResult -> queryId })
            optionalQueryId?.apply {
                val success = client.getGlobalSearchStatus(queryId = this).fold(
                    { error: RequestError ->
                        handleError(error)
                        false
                    },
                    { (_, searchStatus): GetGlobalSearchStatusResult ->
                        doSomethingWith(searchStatus)
                        true
                    })
                assertThat(success).isTrue()
            }
        }

        @Test
        fun testGetGlobalSearchResultPage() {
            val query = mapOf("type" to "Annotation")
            val optionalQueryId = client.createGlobalSearch(query = query).fold({ error: RequestError ->
                handleError(error)
                null
            }, { (_, _, queryId): CreateSearchResult -> queryId })
            assertThat(optionalQueryId).isNotNull()
            optionalQueryId?.apply {
                client.getGlobalSearchResultPage(queryId = this, page = 0, retryUntilDone = true)
                    .fold({ error: RequestError ->
                        handleError(error)
                    }, { (_, annotationPage): GetSearchResultPageResult ->
                        doSomethingWith(annotationPage)
                    })
            }
        }

    }

    @Nested
    inner class IndexTests {
        @Test
        fun testIndexCRUD() {
            val containerName = "republic"
            val fieldName = "body.type"
            val indexType = IndexType.HASHED

            // create
            val create_success = client.addIndex(containerName, fieldName, indexType).fold(
                { error: RequestError ->
                    handleError(error)
                    false
                },
                { (_, statusSummary): AddIndexResult ->
                    doSomethingWith(statusSummary)
                    true
                }
            )
            assertThat(create_success).isTrue

            // read status
            val read_status_success =
                client.getIndexCreationStatus(containerName, fieldName, indexType).fold(
                    { error: RequestError ->
                        handleError(error)
                        false
                    },
                    { (_, statusSummary): GetIndexCreationStatusResult ->
                        doSomethingWith(statusSummary)
                        true
                    }
                )
            assertThat(read_status_success).isTrue

            // read
            val read_success = client.getIndex(containerName, fieldName, indexType).fold(
                { error: RequestError ->
                    handleError(error)
                    false
                },
                { (_, indexConfig): GetIndexResult ->
                    doSomethingWith(indexConfig)
                    true
                }
            )
            assertThat(read_success).isTrue

            // delete
            val delete_success = client.deleteIndex(containerName, fieldName, indexType).fold(
                { error: RequestError ->
                    handleError(error)
                    false
                },
                { _: DeleteIndexResult -> true }
            )
            assertThat(delete_success).isTrue
        }

        @Test
        fun testListIndexes() {
            val containerName = "republic"
            val success = client.listIndexes(containerName).fold(
                { error: RequestError ->
                    handleError(error)
                    false
                },
                { (_, indexes): ListIndexesResult ->
                    doSomethingWith(indexes)
                    true
                })
            assertThat(success).isTrue
        }

    }

    @Nested
    inner class FieldTests {
        @Test
        fun testFieldInfo() {
            val containerName = "republic"
            client.getFieldInfo(containerName).fold(
                { error: RequestError -> handleError(error) },
                { (_, fieldInfo): AnnotationFieldInfoResult -> doSomethingWith(fieldInfo) }
            )
        }

        @Test
        fun testDistinctFieldValues() {
            val containerName = "republic"
            client.getDistinctFieldValues(containerName, "body.type")
                .fold(
                    { error: RequestError -> handleError(error) },
                    { (_, distinctValues): ARResult.DistinctAnnotationFieldValuesResult ->
                        doSomethingWith(distinctValues)
                    }
                )
        }
    }

    @Nested
    inner class AdminTests {
        @Test
        fun testUserCreateAndDelete() {
            val userName = "userName"
            val userEntries = listOf(UserEntry(userName, "apiKey"))
            client.addUsers(userEntries).fold({ error: RequestError ->
                handleError(error)
                false
            }, { (_, accepted, rejected): AddUsersResult ->
                doSomethingWith(accepted, rejected)
                true
            })
            val deletionSuccess: Boolean = client.deleteUser(userName).isRight()
            assertThat(deletionSuccess).isTrue
        }

        @Test
        fun testGetUsers() {
            client.getUsers().fold({ error: RequestError ->
                handleError(error)
                false
            }, { (_, userEntries): UsersResult ->
                doSomethingWith(userEntries)
                true
            })
        }
    }

    @Test
    fun testAbout() {
        val getAboutResult = client.getAbout().getOrNull()
        val aboutInfo = getAboutResult!!.aboutInfo
        doSomethingWith(aboutInfo)
        assertThat(aboutInfo).isNotNull
        assertThat(aboutInfo["appName"]).isEqualTo("AnnoRepo")
    }

    @Nested
    inner class ContainerUsersTests {
        @Test
        fun testAddingContainerUsers() {
            val containerName = "republic"
            val newUserName = "user1"
            client.getContainerUsers(containerName).fold({ error: RequestError ->
                handleError(error)
                false
            }, { (_, containerUserEntries): ContainerUsersResult ->
                doSomethingWith(containerUserEntries)
                assertThat(containerUserEntries).doesNotContain(ContainerUserEntry(newUserName, Role.GUEST))
                true
            })
            val containerUserEntries = listOf(ContainerUserEntry(newUserName, Role.GUEST))
            client.addContainerUsers(containerName, containerUserEntries).fold({ error: RequestError ->
                handleError(error)
                false
            }, { (_, containerUserEntries): ContainerUsersResult ->
                doSomethingWith(containerUserEntries)
                assertThat(containerUserEntries).contains(ContainerUserEntry(newUserName, Role.GUEST))
                true
            })
            client.getContainerUsers(containerName).fold({ error: RequestError ->
                handleError(error)
                false
            }, { (_, containerUserEntries): ContainerUsersResult ->
                doSomethingWith(containerUserEntries)
                assertThat(containerUserEntries).contains(ContainerUserEntry(newUserName, Role.GUEST))
                true
            })
            client.deleteContainerUser(containerName, newUserName).fold({ error: RequestError ->
                handleError(error)
                false
            }, { result: ARResult.DeleteContainerUserResult ->
                doSomethingWith(result.response.status)
                true
            })
            client.getContainerUsers(containerName).fold({ error: RequestError ->
                handleError(error)
                false
            }, { (_, containerUserEntries): ContainerUsersResult ->
                doSomethingWith(containerUserEntries)
                assertThat(containerUserEntries).doesNotContain(ContainerUserEntry(newUserName, Role.GUEST))
                true
            })
        }

        @Test
        fun testAddContainerUsers() {
            val containerName = "my-container"
            val userName = "userName"
            val containerUserEntries = listOf(ContainerUserEntry(userName, Role.EDITOR))
            client.addContainerUsers(containerName, containerUserEntries)
                .fold({ error: RequestError -> handleError(error) },
                    { (_, newContainerUsersList): ContainerUsersResult -> doSomethingWith(newContainerUsersList) })
        }

        @Test
        fun testGetContainerUsers() {
            val containerName = "my-container"
            client.getContainerUsers(containerName).fold({ error: RequestError -> handleError(error) },
                { (_, containerUserEntries): ContainerUsersResult -> doSomethingWith(containerUserEntries) })
        }

        @Test
        fun testDeleteContainerUser() {
            val containerName = "my-container"
            val userName = "userName"
            val deletionSuccess = client.deleteContainerUser(containerName, userName).isRight()
            assertThat(deletionSuccess).isTrue
        }
    }

    @Nested
    inner class MyTests {
        @Test
        fun testMyContainers() {
            client.getMyContainers().fold({ error: RequestError -> handleError(error) },
                { (_, containers): MyContainersResult -> doSomethingWith(containers) })
        }
    }

    @Nested
    inner class Tests {
        @Test
        fun `after deleting a container, it shouldn't show up in my-containers anymore`() {
            either {
                val createResult = client.createContainer("test-container").bind()
                val containerName = createResult.containerName
                val eTag = createResult.eTag
                val myContainers = client.getMyContainers().bind().containers
                assertThat(myContainers["ROOT"]).contains(containerName)
                client.deleteContainer(containerName, eTag).bind()
                val myContainersAfterDelete = client.getMyContainers().bind().containers
                assertThat(myContainersAfterDelete["ROOT"]).doesNotContain(containerName)
            }.mapLeft<Void> {
                log.error("error=$it")
                fail(it.message)
            }
        }

        @Test
        fun `you should be able to reuse an annotation name after deleting the existing annotation with that name`() {
            either {
                val createResult = client.createContainer("test-container").bind()
                val containerName = createResult.containerName
                val containerETag = createResult.eTag

                val annotation1 = WebAnnotation.Builder().withBody("http://example.org/annotation1")
                    .withTarget("http://example.org/target").build()
                val preferredAnnotationName = "my-annotation"
                val createAnnotationResult =
                    client.createAnnotation(containerName, annotation1, preferredAnnotationName).bind()
                val annotationETag = createAnnotationResult.eTag
                val annotationName = createAnnotationResult.annotationName
                assertThat(annotationName).isEqualTo(preferredAnnotationName)

                client.deleteAnnotation(containerName, annotationName, annotationETag).bind()

                val annotation2 = WebAnnotation.Builder().withBody("http://example.org/annotation2")
                    .withTarget("http://example.org/target2").build()
                val createAnnotationResult2 =
                    client.createAnnotation(containerName, annotation2, preferredAnnotationName).bind()
                val annotationName2 = createAnnotationResult2.annotationName
                assertThat(annotationName2).isEqualTo(preferredAnnotationName)
                log.info("this link will be valid for 10 seconds:")
                println(createAnnotationResult2.location)
                Thread.sleep(10_000)

                client.deleteContainer(containerName, containerETag, force = true).bind()
            }.mapLeft<Void> {
                log.error("error=$it")
                fail(it.message)
            }

        }

    }

    companion object {
        const val BASE_URL = "http://localhost:2023"
        val BASE_URI: URI = URI.create(BASE_URL)
        private const val apiKey = "root"
        val client = AnnoRepoClient(BASE_URI, apiKey, "integrated-client-tester")
        private val log = LoggerFactory.getLogger(IntegratedClientKotlinTester::class.java)

        private fun handleError(error: RequestError) {
            println(error)
            throw RuntimeException(error.message)
        }

        private fun doSomethingWith(vararg objects: Any) {
            for (o in objects) {
                try {
                    log.info("{}", ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o))
                } catch (e: JsonProcessingException) {
                    e.printStackTrace()
                }
            }
        }
    }

}