# AnnoRepo Java Client

## Installation

### Maven

Add the following to your `pom.xml`

```xml

<dependency>
    <groupId>nl.knaw.huc</groupId>
    <artifactId>annorepo-client</artifactId>
    <version>${project.version}</version>
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
AnnoRepoClient1 client1=new AnnoRepoClient(URI.create("http://localhost:8080"));
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
String serverVersion=client1.getServerVersion();
```

as well a whether this server requires authentication:

**Kotlin:**

```kotlin
val serverNeedsAuthentication = client.serverNeedsAuthentication
```

**Java**

```java
Boolean serverNeedsAuthentication=client1.getServerNeedsAuthentication();
```

## General information about the endpoint calls

The calls to the annorepo server endpoints will return an
[Either](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-either/) of
a RequestError (in case of an unexpected server response) and
an endpoint-specific result.

## About the server

**Kotlin:**

```kotlin
val result = client.getAbout()
```

**Java**

```java
Either<RequestError, ARResult.GetAboutResult>aboutResult=client1.getAbout();
```

------

**Kotlin:**

```kotlin
```

**Java**

```java
```
