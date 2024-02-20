package nl.knaw.huc.annorepo.client

const val ANNOREPO_BASE_URL = "http://localhost:9999"

class AnnoRepoClientTest {

//    private val client = AnnoRepoClient(URI.create(ANNOREPO_BASE_URL), javaClass.canonicalName)
//
//    @Test
//    fun `client should connect, and GET about should return a map with at least a version field`() {
//        assertThat(client).isNotNull
//        client.getAbout().bimap(
//            { error -> fail<String>("Unexpected error: $error") },
//            { aboutInfo ->
//                assertThat(aboutInfo.version).isNotBlank
//                logger.info{"{}", aboutInfo)
//            }
//        )
//    }
//
//    @Test
//    fun `createContainer without a preferred name should create a container with a generated name`() {
//        client.createContainer().bimap(
//            { error -> fail<String>("Unexpected error: $error") },
//            { response ->
//                assertThat(response.created).isTrue
//                assertThat(response.location).startsWith(ANNOREPO_BASE_URL)
//                assertThat(response.containerId).isNotNull
//                assertThat(response.eTag).isNotNull
//                client.deleteContainer(response.containerId, response.eTag)
//            }
//
//        )
//    }
//
////    @Test
//    fun `createContainer with a preferred name should create a container with the preferred name`() {
//        val preferredName = "container-name" + Random.nextInt()
//        client.createContainer(preferredName).bimap(
//            { error -> fail<String>("Unexpected error: $error") },
//            { response ->
//                assertThat(response.created).isTrue
//                assertThat(response.location).endsWith("/$preferredName/")
//                assertThat(response.containerId).isEqualTo(preferredName)
//                assertThat(response.eTag).isNotNull
//                val ok = client.deleteContainer(preferredName, response.eTag).getOrElse { throw Exception() }
//                assertThat(ok).isTrue
//            }
//        )
//    }

}