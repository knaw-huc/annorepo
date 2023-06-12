# AnnoRepo: REST API

## REST API usage

- [General Notes](#general-notes)
- [Annotation Containers](#annotation-containers):
    - [Create](#creating-an-annotation-container-)
    - [Read](#reading-an-annotation-container-)
    - [Delete](#deleting-an-annotation-container-)
- [Annotations](#annotations):
    - [Create](#adding-an-annotation-to-a-given-annotation-container-)
    - [Read](#reading-an-annotation-)
    - [Update](#updating-an-annotation-)
    - [Delete](#deleting-an-annotation-)
    - [Batch upload](#uploading-multiple-annotations-to-a-given-annotation-container--experimental)
- [Querying a container](#querying-a-container):
    - [Create a query](#create-a-query--experimental)
    - [Get a search result page](#get-a-search-result-page--experimental)
- [Querying (globally)](#querying-globally):
    - [Create a global query](#create-a-global-query--experimental)
    - [Get the search status](#get-a-global-search-status--experimental)
    - [Get a global search result page](#get-a-global-search-result-page--experimental)
- [Indexes](#indexes)
    - [Add an index](#add-index-)
    - [Read an index](#read-index-)
    - [List all indexes](#list-all-indexes-for-a-container-)
    - [Delete an index](#delete-index-)
- [Admin](#admin):
    - [Read users](#get-users-)
    - [Add users](#add-users-)
    - [Delete user](#delete-user-)
- [Container Users](#container-users):
    - [Read container users](#get-container-users-)
    - [Add container users](#add-container-users-)
    - [Delete container user](#delete-container-user-)
    - [Show containers for user](#show-containers-for-user-)
- [Miscellaneous](#miscellaneous):
    - [Annotation Field Count](#get-annotation-field-count-)
- [OpenAPI](#openapi)
- [Server info](#server-info)

---

## General notes

For the basic annotation CRUD handling, AnnoRepo has a `/w3c/` endpoint that implements part of
the [W3C Web Annotation Protocol](https://www.w3.org/TR/2017/REC-annotation-protocol-20170223/)  
As the protocol does not specify how to create, update or delete annotation containers, AnnoRep implements endpoints for
that in a way similar to that used by [elucidate](https://github.com/dlcs/elucidate-server)

- The following requests expect the annorepo server to be running locally at `http://localhost:8080/`

- Features marked `(experimental)` are likely to change in an upcoming release.

- Endpoints marked with 🔒 require authentication if the server was started with authentication
  enabled (`withAuthentication: true` in [/about](#server-info) ).

- Authentication is done by adding an `Authorization` header to the request:
  ``` 
  Authorization: Bearer {apiKey}
  ```

- Endpoints marked with 🔒🔒 are only available when the server was started with authentication enabled, and require root
  authentication.

- `{variable}`s in the Request parts need to be substituted with the appropriate value.

- Gzip compression is enabled by default:
    - Add the header `Content-Encoding: gzip` when sending gzip-compressed data.
    - Add the header `Accept-Encoding: gzip` to receive the data gzip-compressed. (Data smaller than 256 bytes will not
      be
      compressed)

---

## Annotation Containers

### Creating an annotation container (🔒)

If the annorepo instance has authorization enabled, the user creating a container will automatically get `ADMIN`
rights to that container.
(see [Container Users](#container-users))

#### Request

```
POST http://localhost:8080/w3c/ HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Content-Type: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"

{
  "@context": [
    "http://www.w3.org/ns/anno.jsonld",
    "http://www.w3.org/ns/ldp.jsonld"
  ],
  "type": [
    "BasicContainer",
    "AnnotationCollection"
  ],
  "label": "A Container for Web Annotations"
}
```

#### Response

```
HTTP/1.1 201 Created

Location: http://localhost:8080/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/
Content-Location: http://localhost:8080/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/
Vary: Accept
Link: <http://www.w3.org/ns/ldp#BasicContainer>; rel="type"
Link: <http://www.w3.org/TR/annotation-protocol>; rel="http://www.w3.org/ns/ldp#constrainedBy"
Allow: HEAD,DELETE,POST,GET,OPTIONS
ETag: W/"-1172057598"
Content-Type: application/ld+json;profile="http://www.w3.org/ns/anno.jsonld"
Content-Length: 548

{
  "@context": [
    "http://www.w3.org/ns/anno.jsonld",
    "http://www.w3.org/ns/ldp.jsonld"
  ],
  "id": "http://localhost:8080/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/",
  "type": [
    "BasicContainer",
    "AnnotationCollection"
  ],
  "total": 0,
  "label": "A Container for Web Annotations",
  "first": {
    "id": "http://localhost:8080/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/?page=0",
    "type": "AnnotationPage",
    "partOf": "http://localhost:8080/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/",
    "startIndex": 0,
    "items": []
  },
  "last": "http://localhost:8080/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/?page=0"
}
```

Use a `slug` header to choose your own container name. If a container with the same name already exists, AnnoRepo will
generate a new one.

#### Request

```
POST http://localhost:8080/w3c/ HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Content-Type: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Slug: "my-container"

{
  "@context": [
    "http://www.w3.org/ns/anno.jsonld",
    "http://www.w3.org/ns/ldp.jsonld"
  ],
  "type": [
    "BasicContainer",
    "AnnotationCollection"
  ],
  "label": "A Container for Web Annotations"
}
```

#### Response

```
HTTP/1.1 201 Created

Location: http://localhost:8080/w3c/my-container/
Content-Location: http://localhost:8080/w3c/my-container/
Vary: Accept
Link: <http://www.w3.org/ns/ldp#BasicContainer>; rel="type"
Link: <http://www.w3.org/TR/annotation-protocol>; rel="http://www.w3.org/ns/ldp#constrainedBy"
Allow: HEAD,DELETE,POST,GET,OPTIONS
ETag: W/"2133202336"
Content-Type: application/ld+json;profile="http://www.w3.org/ns/anno.jsonld"
Content-Length: 452

{
  "@context": [
    "http://www.w3.org/ns/anno.jsonld",
    "http://www.w3.org/ns/ldp.jsonld"
  ],
  "id": "http://localhost:8080/w3c/my-container/",
  "type": [
    "BasicContainer",
    "AnnotationCollection"
  ],
  "total": 0,
  "label": "A Container for Web Annotations",
  "first": {
    "id": "http://localhost:8080/w3c/my-container/?page=0",
    "type": "AnnotationPage",
    "partOf": "http://localhost:8080/w3c/my-container/",
    "startIndex": 0,
    "items": []
  },
  "last": "http://localhost:8080/w3c/my-container/?page=0"
}
```

### Reading an annotation container (🔒)

#### Request

```
GET http://localhost:8080/w3c/{containerName}/ HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
```

#### Response

```
HTTP/1.1 200 OK

Content-Location: http://localhost:8080/w3c/my-container/
Vary: Accept
Link: <http://www.w3.org/ns/ldp#BasicContainer>; rel="type"
Link: <http://www.w3.org/TR/annotation-protocol/>; rel="http://www.w3.org/ns/ldp#constrainedBy"
Allow: HEAD,DELETE,POST,GET,OPTIONS
ETag: W/"2133202336"
Content-Type: application/ld+json;profile="http://www.w3.org/ns/anno.jsonld"
Vary: Accept-Encoding
Content-Length: 452

{
  "@context": [
    "http://www.w3.org/ns/anno.jsonld",
    "http://www.w3.org/ns/ldp.jsonld"
  ],
  "id": "http://localhost:8080/w3c/my-container/",
  "type": [
    "BasicContainer",
    "AnnotationCollection"
  ],
  "total": 0,
  "label": "A Container for Web Annotations",
  "first": {
    "id": "http://localhost:8080/w3c/my-container/?page=0",
    "type": "AnnotationPage",
    "partOf": "http://localhost:8080/w3c/my-container/",
    "startIndex": 0,
    "items": []
  },
  "last": "http://localhost:8080/w3c/my-container/?page=0"
}
```

### Deleting an annotation container (🔒)

Deleting an annotation container is only possible if the container doesn't contain any annotations.

The `If-Match` header must contain the ETag of the container.

#### Request

```
DELETE http://localhost:8080/w3c/{containerName}/ HTTP/1.1

If-Match: "{etag}"
```

#### Response

```
HTTP/1.1 204 No Content
```

---

## Annotations

### Adding an annotation to a given annotation container (🔒)

As with the container creation, you can let annorepo pick the annotation name:

#### Request

```
POST http://localhost:8080/w3c/{containerName}/ HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Content-Type: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "http://example.org/annotations/myannotation"
  "type": "Annotation",
  "body": {
    "type": "TextualBody",
    "value": "I like this page!"
  },
  "target": "http://www.example.com/index.html"
}
```

#### Response

```
HTTP/1.1 201 CREATED

Location: http://localhost:8080/w3c/my-container/0bb16696-245c-4614-955f-78dec7065f60
Vary: Accept
Allow: HEAD,DELETE,POST,GET,OPTIONS,PUT
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#Annotation>; rel="type"
ETag: W/"-1699442532"
Content-Type: application/ld+json;profile="http://www.w3.org/ns/anno.jsonld"
Content-Length: 305

{
   "@context": "http://www.w3.org/ns/anno.jsonld",
   "id": "http://localhost:8080/w3c/my-container/0bb16696-245c-4614-955f-78dec7065f60",
   "type": "Annotation",
   "body": {
      "type": "TextualBody",
      "value": "I like this page!"
   },
   "target": "http://www.example.com/index.html",
   "via": "http://example.org/annotations/myannotation"
}
```

or, you can add a `Slug` header to set the name. When the preferred name is already in use in the container, AnnoRepo
will pick the name:

```
POST http://localhost:8080/w3c/{containerName}/ HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Content-Type: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Slug: hello-world

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "http://example.org/annotations/my-annotation",
  "type": "Annotation",
  "body": {
    "type": "TextualBody",
    "value": "Hello!"
  },
  "target": "http://www.example.com/world.html"
}
```

#### Response

```
HTTP/1.1 201 CREATED

Location: http://localhost:8080/w3c/my-container/hello-world
Vary: Accept
Allow: HEAD,DELETE,POST,GET,OPTIONS,PUT
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#Annotation>; rel="type"
ETag: W/"1303452440"
Content-Type: application/ld+json;profile="http://www.w3.org/ns/anno.jsonld"
Content-Length: 270

{
   "@context": "http://www.w3.org/ns/anno.jsonld",
   "id": "http://localhost:8080/w3c/my-container/hello-world",
   "type": "Annotation",
   "body": {
      "type": "TextualBody",
      "value": "Hello!"
   },
   "target": "http://www.example.com/world.html",
   "via": "http://example.org/annotations/my-annotation"
}
```

### Reading an annotation (🔒)

#### Request

```
GET http://localhost:8080/w3c/{containerName}/{annotationName} HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
```

#### Response

```
HTTP/1.1 200 OK

Allow: HEAD,DELETE,POST,GET,OPTIONS,PUT
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#Annotation>; rel="type"
ETag: W/"-1518598207"
Content-Type: application/ld+json;profile="http://www.w3.org/ns/anno.jsonld"
Vary: Accept-Encoding
Content-Length: 272

{
   "id": "http://localhost:8080/w3c/my-container/my-annotation",
   "type": "Annotation",
   "body": {
      "type": "TextualBody",
      "value": "Hello!"
   },
   "@context": "http://www.w3.org/ns/anno.jsonld",
   "target": "http://www.example.com/world.html",
   "via": "http://example.org/annotations/my-annotation"
}
```

### Updating an annotation (🔒)

When updating an annotation, you need to send its ETag in the `If-Match` header.

#### Request

```
PUT http://localhost:8080/w3c/{containerName}/{annotationName} HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Content-Type: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
If-Match: "{etag}"

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "http://example.org/annotations/my-annotation",
  "type": "Annotation",
  "body": {
    "type": "TextualBody",
    "value": "Goodbye!"
  },
  "target": "http://www.example.com/world.html"
}
```

#### Response

```
HTTP/1.1 200 OK

Vary: Accept
Allow: HEAD,DELETE,POST,GET,OPTIONS,PUT
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#Annotation>; rel="type"
ETag: W/"1303452440"
Content-Type: application/ld+json;profile="http://www.w3.org/ns/anno.jsonld"

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "http://localhost:8080/w3c/my-container/hello-world",
  "type": "Annotation",
  "body": {
    "type": "TextualBody",
    "value": "Goodbye!"
  },
  "target": "http://www.example.com/world.html",
  "via": "http://example.org/annotations/my-annotation"
}
```

### Deleting an annotation (🔒)

When deleting an annotation, you need to send its ETag in the `If-Match` header.

#### Request

```
DELETE http://localhost:8080/w3c/{containerName}/{annotationName} HTTP/1.1

If-Match: "{etag}"
```

#### Response

```
HTTP/1.1 204 No Content
```

### Uploading multiple annotations to a given annotation container (🔒) `(experimental)`

#### Request

```
POST http://localhost:8080/batch/{containerName}/annotations HTTP/1.1
Content-Type: application/json

[
    {
        "@context": "http://www.w3.org/ns/anno.jsonld",
        "type": "Annotation",
        "motivation": "classifying",
        "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "attendant",
            "id": "urn:example:republic:person-1"
        },
        "target": {
            "source": "urn:example:republic:text-1",
            "type": "Text",
            "selector": {
                "type": "urn:example:republic:TextAnchorSelector",
                "start": 100,
                "end": 130
            }
        }
    },
    {
        "@context": "http://www.w3.org/ns/anno.jsonld",
        "type": "Annotation",
        "motivation": "classifying",
        "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "recipient",
            "id": "urn:example:republic:person-2"
        },
        "target": {
            "source": "urn:example:republic:text-1",
            "type": "Text",
            "selector": {
                "type": "urn:example:republic:TextAnchorSelector",
                "start": 190,
                "end": 200
            }
        }
    },
    {
        "@context": "http://www.w3.org/ns/anno.jsonld",
        "type": "Annotation",
        "motivation": "classifying",
        "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "attendant",
            "id": "urn:example:republic:person-3"
        },
        "target": {
            "source": "urn:example:republic:text-1",
            "type": "Text",
            "selector": {
                "type": "urn:example:republic:TextAnchorSelector",
                "start": 200,
                "end": 220
            }
        }
    },
    {
        "@context": "http://www.w3.org/ns/anno.jsonld",
        "type": "Annotation",
        "motivation": "classifying",
        "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "attendant",
            "id": "urn:example:republic:person-4"
        },
        "target": {
            "source": "urn:example:republic:text-1",
            "type": "Text",
            "selector": {
                "type": "urn:example:republic:TextAnchorSelector",
                "start": 300,
                "end": 315
            }
        }
    },
    {
        "@context": "http://www.w3.org/ns/anno.jsonld",
        "type": "Annotation",
        "motivation": "classifying",
        "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "attendant",
            "id": "urn:example:republic:person-5"
        },
        "target": {
            "source": "urn:example:republic:text-2",
            "type": "Text",
            "selector": {
                "type": "urn:example:republic:TextAnchorSelector",
                "start": 90,
                "end": 110
            }
        }
    }
]
```

#### Response

The batch request will return a list of the generated annotation names:

```
HTTP/1.1 200 OK

Content-Type: application/json
Content-Length: 23
    
[
   "9de49b24-97d8-4e7a-8167-c043723ef672",
   "82866a01-dd23-4f4b-896b-3e30cb7bff5c",
   "8f2aa0c5-379b-4705-b1be-9c72627fb153",
   "623fa415-6206-4b0f-9268-ae13be1b4fba",
   "a1f6d1a5-af08-4779-b35a-d36e695a9d4e"
]
```

---

## Querying a container

### Create a query (🔒) `(experimental)`

#### Request

```
POST http://localhost:8080/services/{containerName}/search HTTP/1.1

{
  "purpose": "tagging",
  "body.type": {
    ":isNotIn": [
      "Line",
      "Page"
      ]
  },
  ":overlapsWithTextAnchorRange": {
    "start": 12,
    "end": 134,
    "source": "https://textrepo.republic-caf.diginfra.org/api/rest/versions/42df1275-81cd-489c-b28c-345780c3889b/contents"
  }
}

```

This request body (the query) consists of three parts:

- a _field query_, which in this case describes we want annotations that have the value `tagging` in the root `purpose`
  field.
- an _extended field query_, which in this case has the _query operator_ `:isNotIn`
- a call to the custom _query function_ `:overlapsWithTextAnchorRange` with `source`, `start` and `end` parameters.

This query will return only those annotations in `my-container` that match with all three sub-queries.

In general, a query must consist of at least 1 _field query_, _extended field query_ or _query function call_,
and the returned annotations must match with all the sub-queries.

##### field names

- field names are case-sensitive
- values are case-sensitive, and must match the entire field value.

example: `"purpose": "tag"` will not match with `"purpose": "tagging"` or `"Purpose": "tag"`

- use `.` to indicate the hierarchy depth of the field.

example: given this `body`:

```json
{
  "body": [
    {
      "type": "Object",
      "metadata": {
        "height": 12,
        "width": 10,
        "color": "yellow"
      }
    },
    {
      "type": "Text",
      "value": "Hello, World!"
    }
  ]
}
```

This json contains the queryable fields:

- `body.type`
- `body.metadata.height`
- `body.metadata.width`
- `body.metadata.color`
- `body.value`

Use the [`fields` endpoint](#get-annotation-field-count-) to see the fields available in the container.

##### (simple) field query

This matches the given field with the given value.  
A value can be a:

- string (`"value"`)
- number (`42`, `3.14159265358979323846`, `6e23`)
- boolean (`true`, `false`)

##### extended field query

In an extended field query you can use one of the following _query operators_:

- `:=` to indicate the field value should be the given value:  
  example: `"body.type": { ":=": "Object" }` will return those annotations where `body.type` has the value `Object`.

- `:!=` to indicate the field value should not be the given value:  
  example: `"target.type": { ":!=": "Image" }` will return those annotations where no `body.type` has the value `Line`.

- `:<` to indicate the field value should be less than the given value:  
  example: `"body.metadata.offset": { ":<": 100 }` will return those annotations where `body.metadata.offset` has a
  value less than 100.

- `:<=` to indicate the field value should be less than or equal to the given value:
  example: `"target.selector.beginCharOffset": { ":<=": 10 }` will return those annotations
  where `target.selector.beginCharOffset` has a value less than or equal to 10.

- `:>` to indicate the field value should be greater than the given value:  
  example: `"body.metadata.offset": { ":>": 100 }` will return those annotations where `body.metadata.offset` has a
  value greater than 100.

- `:>=` to indicate the field value should be greater than or equal to the given value:
  example: `"target.selector.beginCharOffset": { ":>=": 10 }` will return those annotations
  where `target.selector.beginCharOffset` has a value greater than or equal to 10.

- `:isIn` to indicate the field value should be one of the values in the given list:  
  example: `"body.type": { ":isIn": [ "Object", "Text"] }` will return those annotations where `body.type` is
  either `Object` or `Text`

- `:isNotIn` to indicate the field value should not match with any of the values in the given list:  
  example: `"target.type": { ":isNotIn": [ "Image", "Text"] }` will return those annotations that have no `target.type`
  with values `Image` or `Text`

##### query function call

A _query function_ in AnnoRepo is a pre-programmed combination of simple and extended field queries, and can be called
with a parameter map. The query function

Currently, the following query functions are available:

- `:isWithinTextAnchorRange` (parameters: `source`, `start`, `end`)  
  This function will return those annotations that fall within the _TextAnchorRange_ defined by the `source` uri and
  the `start` and `end` _TextAnchor_ numbers.

- `:overlapsWithTextAnchorRange` (parameters: `source`, `start`, `end`)  
  This function will return those annotations that overlap with the _TextAnchorRange_ defined by the `source` uri and
  the `start` and `end` _TextAnchor_ numbers.

#### Response

```
HTTP/1.1 200 OK

location: http://localhost:8080/services/my-container/search/f3da8d25-701c-4e25-b1be-39cd6243dac7
```

The Location header contains the link to the first search result page.

---

### Get a search result page (🔒) `(experimental)`

#### Request

```
GET http://localhost:8080/services/{containerName}/search/{searchId} HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK

Content-Type: application/json
Vary: Accept-Encoding

{
  "id": "http://localhost:8080/services/my-container/search/d6883433-de41-43fb-93d2-85c1cd9570ee?page=0",
  "type": "AnnotationPage",
  "partOf": "http://localhost:8080/services/my-container/search/d6883433-de41-43fb-93d2-85c1cd9570ee",
  "startIndex": 0,
  "items": [
    ....
  ]
}
```

The Location header contains the link to the first search result page.

---

## Querying globally

To query all the containers the user has read-access to, use the `/global/search` endpoint in a similar way as querying
a specific container.

### Create a global query (🔒) `(experimental)`

#### Request

```
POST http://localhost:8080/global/search HTTP/1.1

{
  "purpose": "identifying"
}
```

#### Response

```
HTTP/1.1 201 Created
Location: http://localhost:8080/global/search/73f62348-7dcc-4d13-9748-fcb8f5a8a367
Link: <http://localhost:8080/global/search/73f62348-7dcc-4d13-9748-fcb8f5a8a367/status>; rel="status"
Content-Type: application/json

{
  "query" : {
    "purpose": "identifying"
  },
  "startedAt" : "2023-05-02T12:49:32",
  "finishedAt" : null,
  "expiresAt" : null,
  "state" : "RUNNING",
  "containersSearched" : 0,
  "totalContainersToSearch" : 11,
  "hitsFoundSoFar" : 0,
  "processingTimeInMillis" : 2
}
```

Creating the global query returns its location in the `Location` header.
The body returned is a representation of the status of the search, with the fields:

- `query`: the query used
- `startedAt`: the time the search was started
- `finishedAt`: the time the search was finished, or null if the search hasn't finished yet.
- `expiresAt`: the time at which the search results will not be available anymore, or null if the search hasn't finished
  yet.
- `state`: the state of the search; this can be:
    - `CREATED`: the search was created, but not started yet
    - `RUNNING`: the search is applying the query to all relevant containers
    - `DONE`: the search is finished
- `containersSearched`: the number of containers that have been searched so far.
- `totalContainersToSearch`: the number of containers to search in total.
- `hitsFoundSoFar`: the number of annotations found so far.
- `processingTimeInMillis`: The number of milliseconds that the search has run.

---

### Get a global search status (🔒) `(experimental)`

#### Request

```
GET http://localhost:8080/global/search/{searchId}/status HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK
Content-Type: application/json

{
    "query": {
        "type": "Annotation"
    },
    "startedAt": "2023-05-09T11:00:20",
    "finishedAt": null,
    "expiresAt": null,
    "state": "RUNNING",
    "containersSearched": 3,
    "totalContainersToSearch": 11,
    "hitsFoundSoFar": 1114,
    "processingTimeInMillis": 41
}
```

---

### Get a global search result page (🔒) `(experimental)`

#### Request

```
GET http://localhost:8080/global/search/{searchId} HTTP/1.1
```

#### Response

If the search is still ongoing,

```
HTTP/1.1 202 Accepted
```

is returned, with the search status in the body.

If the search has finished:

```
HTTP/1.1 200 OK

Content-Type: application/json
Vary: Accept-Encoding

{
  "id": "http://localhost:8080/global/search/73f62348-7dcc-4d13-9748-fcb8f5a8a367?page=0",
  "type": "AnnotationPage",
  "partOf": "http://localhost:8080/global/search/73f62348-7dcc-4d13-9748-fcb8f5a8a367",
  "startIndex": 0,
  "items": [
    ....
  ]
}
```

The Location header contains the link to the first search result page.

---

## Indexes

When some fields are queried often, creating an index on that field will speed up the querying.

### Add index (🔒)

#### Request

```
PUT http://localhost:8080/services/{containerName}/indexes/{fieldName}/{indexType} HTTP/1.1
```

(no body required)

Available options for `indexType`:

- `hashed` - for fields used in simple field queries or the extended field queries: `:=`, `:!=`, `:isIn`, `:notIn`
- `ascending` - for fields used in extended field queries `:<`, `:<=`, `:>`, `:>=`
- `descending` - for fields used in extended field queries `:<`, `:<=`, `:>`, `:>=`

#### Response

```
HTTP/1.1 200 OK
Location: http://localhost:8080/services/my-container/indexes/body.metadata/hashed
```

---

### Read index (🔒)

#### Request

```
GET http://localhost:8080/services/{containerName}/indexes/{fieldName}/{indexType} HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "field" : "body.metadata",
  "type" : "HASHED",
  "url" : "http://localhost:8080/services/my-container/indexes/body.metadata/hashed"
}
```

---

### List all indexes for a container (🔒)

#### Request

```
GET http://localhost:8080/services/{containerName}/indexes HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "field" : "body.metadata",
    "type" : "HASHED",
    "url" : "http://localhost:8080/services/my-container/indexes/body.metadata/hashed"
  },
  ...
]
```

---

### Delete index (🔒)

#### Request

```
DELETE http://localhost:8080/services/{containerName}/indexes/{fieldName}/{indexType} HTTP/1.1
```

#### Response

```
HTTP/1.1 204 No Content
```

---

## Admin

### Get users (🔒🔒)

#### Request

```
GET http://localhost:8080/admin/users HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "userName" : "user",
    "apiKey" : "1234567890abcdefghijklmnopqrstuvwxy"
  },
  ...
]
```

### Add users (🔒🔒)

#### Request

```
POST http://localhost:8080/admin/users HTTP/1.1
Content-Type: application/json

[
  {
    "userName": "user1",
    "apiKey": "88bbfbe6cdb3965e55a3a2f0d5286d0e"
  },
  {
    "userName": "user2",
    "apiKey": "161cbd4930910a4455a830bfb95ebb6c"
  },
  ...
]
```

#### Response

```
{
  "added": [
    "user1",
    "user2"
  ],
  "rejected": []
}
```

Users will be rejected if:

- A user with the given userName already exists.
- A user with the given apiKey already exists.
- Either of the fields `userName` or `apiKey` is missing in the request.

### Delete user (🔒🔒)

#### Request

```
DELETE http://localhost:8080/admin/{userName} HTTP/1.1
```

#### Response

```
HTTP/1.1 204 No Content
```

---

## Container Users

These endpoints are only functional on annorepo servers that have authentication enabled.

Only users with admin rights to the given container (and the root user) can use these endpoints.

Users that have been previously added via the [add users endpoint](#add-users---) can be added to the given container,
with one of
these roles:

- `GUEST` -> user has read-only access to the container
- `EDITOR` -> user can read and write to the container, but cannot add or delete container users
- `ADMIN` -> user can read and write to the container, and add or delete users for this container

### Get Container users (🔒)

#### Request

```
GET http://localhost:8080/services/{containerName}/users HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK
Content-Type: application/json

[
    {
        "userName": "user1",
        "role": "ADMIN"
    },
    {
        "userName": "user2",
        "role": "GUEST"
    }
]
```

### Add Container users (🔒)

#### Request

```
POST http://localhost:8080/services/{containerName}/users HTTP/1.1

Content-Type: application/json

[
  { "userName": "username1", "role": "EDITOR" },
  { "userName": "username2", "role": "ADMIN" }
]
```

#### Response

```
HTTP/1.1 200 OK
Content-Type: application/json

[
    {
        "userName": "username1",
        "role": "EDITOR"
    },
    {
        "userName": "username2",
        "role": "ADMIN"
    },

    {
        "userName": "user1",
        "role": "ADMIN"
    },
    {
        "userName": "user2",
        "role": "GUEST"
    }
]
```

### Delete Container user (🔒)

#### Request

```
DELETE http://localhost:8080/services/{containerName}/users/{userName} HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK
```

### Show containers for user (🔒)

Calling this endpoint returns the containers that the authenticated user has access to, grouped by role.

#### Request

```
GET http://localhost:8080/my/containers HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK
Content-Type: application/json

{
    "ADMIN": [
        "my-container"
    ],
    "GUEST": [
        "7696f94e-5809-45a2-b575-4872ce693bd2",
        "b4421353-debb-4be7-8891-ccb510bbdff7",
    ]
    "EDITOR": [
        "1ad90412-6247-47d4-b7bf-c9a4ec8bf8a4"
    ]
}
```

---

## Miscellaneous

### Get Annotation Field Count (🔒)

When composing a search query, it helps to know what annotation fields you can search on, and also how many annotations
contain that field.
The services/fields endpoint serves this purpose: it will return a map/dictionary of all the annotation fields available
in the given container, plus the number of annotations that field is used in.

#### Request

```
GET http://localhost:8080/services/{containerName}/fields HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK

Content-Type: application/json
Vary: Accept-Encoding

{
  "body.id" : 1,
  "body.type" : 15,
  "body.value" : 15,
  "target" : 15,
  "type" : 15
}
```

This response, for example, indicates there are 15 annotations in `my-container` with a `target` field, and only 1 with
a `body.id` field.

---

### Get Distinct Annotation Field Values (🔒)

This endpoint will list the distinct values for the given annotation field in the given container.

#### Request

```
GET http://localhost:8080/services/{containerName}/distinct-values/{fieldName} HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK

Content-Type: application/json
Vary: Accept-Encoding

[
  value1,
  value2
]

```

---

## OpenAPI

AnnoRepo provides an [openapi](https://www.openapis.org/) API definition via [swagger](https://swagger.io/)

The basic swagger UI is available via

```
GET http://localhost:8080/swagger HTTP/1.1
```

and the definition file is available as json via

```
GET http://localhost:8080/swagger.json HTTP/1.1
```

or

```
GET http://localhost:8080/openapi.json HTTP/1.1
```

and as yaml via

```
GET http://localhost:8080/swagger.yaml HTTP/1.1
```

or

```
GET http://localhost:8080/openapi.yaml HTTP/1.1
```

---

## Server info

### Get information about the servers

#### Request

```
GET http://localhost:8080/about HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK
...
Content-Type: application/json
...

{
  "appName" : "AnnoRepo",
  "version" : "0.3.0-beta",
  "startedAt" : "2022-09-22T15:30:24.854713Z",
  "baseURI" : "http://localhost:8080",
  "withAuthentication" : false,
  "sourceCode" : "https://github.com/knaw-huc/annorepo"
}
```
