# AnnoRepoClient

A Java/Kotlin client for connecting to an AnnoRepo server and wrapping the communication with the
endpoints.

## Installation

### Maven

Add the following to your `pom.xml`

```xml

<dependency>
    <groupId>nl.knaw.huc</groupId>
    <artifactId>annorepo-client</artifactId>
    <version>${annorepo.version}</version>
</dependency>
```

## Initialization

**Kotlin:**

```kotlin
val client = AnnoRepoClient(
    serverURI = URI.create("http://localhost:8080"),
    apiKey = apiKey,
    userAgent = "name to identity this client in the User-Agent header"
)
```

`apiKey` and `userAgent` are optional, so in Java there are three constructors:

**Java**

```java
AnnoRepoClient1 client=new AnnoRepoClient(URI.create("http://localhost:8080"));
        AnnoRepoClient1 client2=new AnnoRepoClient(URI.create("http://localhost:8080",apiKey));
        AnnoRepoClient1 client3=new AnnoRepoClient(URI.create("http://localhost:8080",apiKey,userAgent));
```

The client will try to connect to the AnnoRepo server at the given URI, and throw a RuntimeException if this is not
possible.
After connecting, you will be able to check the version of annorepo that the server is running:

**Kotlin:**

```kotlin
val serverVersion = client.serverVersion
```

**Java**

```java
String serverVersion=client.getServerVersion();
```

as well as whether this server requires authentication:

**Kotlin:**

```kotlin
val serverNeedsAuthentication = client.serverNeedsAuthentication
```

**Java**

```java
Boolean serverNeedsAuthentication=client.getServerNeedsAuthentication();
```

## General information about the endpoint calls

The calls to the annorepo server endpoints will return an
[Either](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-either/) of
a RequestError (in case of an unexpected server response) and
an endpoint-specific result (in case of a successful response).

The `Either` has several methods to handle the left or right hand side;
for example: `fold`, where you provide functions to deal with the left and right sides:

**Kotlin:**

```kotlin
client.getAbout().fold(
    { error -> println(error) },
    { result -> println(result) }
)
```

**Java**

```java
Boolean success=client.getAbout().fold(
    error->{
        System.out.println(error.toString());
        return false;
    },
    result->{
        System.out.println(result.toString());
        return true;
    }
)
```

## Get information about the server

**Kotlin:**

```kotlin
val result = client.getAbout()
```

**Java**

```java
Either<RequestError, GetAboutResult> aboutResult = client.getAbout();
```

## Annotation containers

### Creating a container

Parameters:

- `preferredName`: optional String, indicating the preferred name for the container. May be overridden by the server.
- `label`: optional String, a human-readable label for the container.

**Kotlin:**

```kotlin
val result = client.createContainer(preferredName, label)
```

**Java**

```java
Either<RequestError, CreateContainerResult> result = client.createContainer(preferredName,label);
```

On a succeeding call, the CreateContainerResult contains:

- `response` : the raw javax.ws.rs.core.Response
- `location` : the contents of the `location` header
- `containerName` : the name of the container.
- `eTag` : the eTag for the container

### Retrieving a container

**Kotlin:**

```kotlin
```

**Java**

```java
```

### Deleting a container

**Kotlin:**

```kotlin
```

**Java**

```java
```

## Annotations

### Adding an annotation to a container

**Kotlin:**

```kotlin
```

**Java**

```java
```

### Retrieving an annotation

**Kotlin:**

```kotlin
```

**Java**

```java
```

### Updating an annotation

**Kotlin:**

```kotlin
```

**Java**

```java
```

### Deleting an annotation

**Kotlin:**

```kotlin
```

**Java**

```java
```

### Batch uploading of annotations

**Kotlin:**

```kotlin
```

**Java**

```java
```

## Querying a container

### Creating the search

**Kotlin:**

Construct the query as a (nested) map.
When successful, the call returns a createSearchResult, which contains the queryId to be used in getting the result pages.

```kotlin
val query = mapOf("body" to "urn:example:body42")
val createSearchResult = this.createSearch(containerName = containerName, query = query)
```

**Java**

```java
```

### Retrieving a result page

**Kotlin:**

```kotlin
val resultPageResult = this.getSearchResultPage(
    containerName = containerName,
    queryId = queryId,
    page = 0
)
```

**Java**

```java
```

### Filtering Container Annotations

This function combines creating the search and iterating over the search result pages and extracting the annotations from those pages into a stream.
Since there could be unexpected response from the server, the stream returned is one of `Either<RequestError, FilterContainerAnnotationsResult>`


**Kotlin:**

```kotlin
val query = mapOf("body.type" to "Page")
val filterContainerAnnotationsResult: FilterContainerAnnotationsResult? =
    this.filterContainerAnnotations(containerName, query2).orNull()
filterContainerAnnotationsResult?.let {
    it.annotations.forEach { item ->
        item.fold(
            { error: RequestError -> handleError(error) },
            { annotation: Map<String, Any> -> handleSuccess(annotation) }
        )
    }
}
```

**Java**

```java
Map<String, ?> query = Map.of("body.type", "Resolution");
client.filterContainerAnnotations("my-container", query).fold(
        error -> {
            System.out.println(error.toString());
            return false;
        },
        result -> {
            result.getAnnotations().limit(5).forEach(item -> {
                System.out.println(item.orNull());
                System.out.println();
            });
            return true;
        }
)
```

### Retrieving search information

**Kotlin:**

```kotlin
val getSearchInfoResult = this.getSearchInfo(
    containerName = containerName,
    queryId = queryId
)
```

**Java**

```java
```

## Indexes

### Adding an index to a container

**Kotlin:**

```kotlin
val addIndexResult = this.addIndex(containerName, fieldName, indexType)
```

**Java**

```java
```

### Retrieving index information

**Kotlin:**

```kotlin
```

**Java**

```java
```

### Listing all indexes for a container

**Kotlin:**

```kotlin
val listIndexResult = this.listIndexes(containerName)
```

**Java**

```java
```

### Deleting an index

**Kotlin:**

```kotlin
val deleteIndexResult = this.deleteIndex(containerName, fieldName, indexType)
```

**Java**

```java
```

## Retrieving information about the fields used in container annotations

**Kotlin:**

```kotlin
```

**Java**

```java
```

## User administration

These admin functionalities are only available on annorepo servers that have authentication enabled.
The root api-key is required for these calls.

### Adding users

**Kotlin:**

```kotlin
```

**Java**

```java
```

### Retrieving users

**Kotlin:**

```kotlin
```

**Java**

```java
```

### Deleting a user

**Kotlin:**

```kotlin
```

**Java**

```java
```
