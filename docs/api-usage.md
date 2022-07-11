# AnnoRepo: REST API

## REST API usage

For the basic annotation CRUD handling, AnnoRepo has a `/w3c/` endpoint that implements part of
the [W3C Web Annotation Protocol](https://www.w3.org/TR/2017/REC-annotation-protocol-20170223/)
As the protocol does not specify how to create, update or delete annotation containers, AnnoRep implements endpoints for
that in a way similar to that used by [elucidate](https://github.com/dlcs/elucidate-server)

The following requests expect the annorepo server to be running locally at `http://localhost:9999/`

Features marked `(experimental)` are likely to change in the next release.

- [Annotation Containers](#annotation-containers):
  - [Create](#creating-an-annotation-container)
  - [Read](#reading-an-annotation-container)
  - [Delete](#deleting-an-annotation-container)
- [Annotations](#annotations):
  - [Create](#adding-an-annotation-to-a-given-annotation-container)
  - [Read](#reading-an-annotation)
  - [Update](#updating-an-annotation)
  - [Delete](#deleting-an-annotation)
  - [Batch upload](#uploading-multiple-annotations-to-a-given-annotation-container-experimental)
- [Querying](#querying):
  - [On field/value combinations](#find-annotations-with-the-given-fieldvalue-combinations-experimental)
  - [Within a range](#find-annotations-that-fall-within-the-given-range-experimental)
  - [Overlapping a range](#find-annotations-that-overlap-with-the-given-range-experimental)
- [OpenAPI](#openapi)
- [Server info](#server-info)

---

## Annotation Containers

### Creating an annotation container

#### Request

```
POST http://localhost:9999/w3c/ HTTP/1.1

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

Location: http://localhost:9999/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/
Content-Location: http://localhost:9999/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/
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
  "id": "http://localhost:9999/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/",
  "type": [
    "BasicContainer",
    "AnnotationCollection"
  ],
  "total": 0,
  "label": "A Container for Web Annotations",
  "first": {
    "id": "http://localhost:9999/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/?page=0",
    "type": "AnnotationPage",
    "partOf": "http://localhost:9999/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/",
    "startIndex": 0,
    "items": []
  },
  "last": "http://localhost:9999/w3c/137d2619-7bcd-41c9-abc3-7ec78733993c/?page=0"
}
```

Use a `slug` header to choose your own container name. If a container with the same name already exists, AnnoRepo will
generate a new one.

#### Request

```
POST http://localhost:9999/w3c/ HTTP/1.1

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

Location: http://localhost:9999/w3c/my-container/
Content-Location: http://localhost:9999/w3c/my-container/
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
  "id": "http://localhost:9999/w3c/my-container/",
  "type": [
    "BasicContainer",
    "AnnotationCollection"
  ],
  "total": 0,
  "label": "A Container for Web Annotations",
  "first": {
    "id": "http://localhost:9999/w3c/my-container/?page=0",
    "type": "AnnotationPage",
    "partOf": "http://localhost:9999/w3c/my-container/",
    "startIndex": 0,
    "items": []
  },
  "last": "http://localhost:9999/w3c/my-container/?page=0"
}
```

### Reading an annotation container

#### Request

```
GET http://localhost:9999/w3c/my-container/ HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
```

#### Response

```
HTTP/1.1 200 OK

Content-Location: http://localhost:9999/w3c/my-container/
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
  "id": "http://localhost:9999/w3c/my-container/",
  "type": [
    "BasicContainer",
    "AnnotationCollection"
  ],
  "total": 0,
  "label": "A Container for Web Annotations",
  "first": {
    "id": "http://localhost:9999/w3c/my-container/?page=0",
    "type": "AnnotationPage",
    "partOf": "http://localhost:9999/w3c/my-container/",
    "startIndex": 0,
    "items": []
  },
  "last": "http://localhost:9999/w3c/my-container/?page=0"
}
```

### Deleting an annotation container

Deleting an annotation container is only possible if the container doesn't contain any annotations.

The `If-Match` header must contain the ETag of the container.

#### Request

```
DELETE http://localhost:9999/w3c/my-container/ HTTP/1.1

If-Match: "2133202336"
```

#### Response

```
HTTP/1.1 204 No Content
```

---

## Annotations

### Adding an annotation to a given annotation container

As with the container creation, you can let annorepo pick the annotation name:

#### Request

```
POST http://localhost:9999/w3c/my-container/ HTTP/1.1

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

Location: http://localhost:9999/w3c/my-container/0bb16696-245c-4614-955f-78dec7065f60
Vary: Accept
Allow: HEAD,DELETE,POST,GET,OPTIONS,PUT
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#Annotation>; rel="type"
ETag: W/"-1699442532"
Content-Type: application/ld+json;profile="http://www.w3.org/ns/anno.jsonld"
Content-Length: 305

{
   "@context": "http://www.w3.org/ns/anno.jsonld",
   "id": "http://localhost:9999/w3c/my-container/0bb16696-245c-4614-955f-78dec7065f60",
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
POST http://localhost:9999/w3c/my-container/ HTTP/1.1

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

Location: http://localhost:9999/w3c/my-container/hello-world
Vary: Accept
Allow: HEAD,DELETE,POST,GET,OPTIONS,PUT
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#Annotation>; rel="type"
ETag: W/"1303452440"
Content-Type: application/ld+json;profile="http://www.w3.org/ns/anno.jsonld"
Content-Length: 270

{
   "@context": "http://www.w3.org/ns/anno.jsonld",
   "id": "http://localhost:9999/w3c/my-container/hello-world",
   "type": "Annotation",
   "body": {
      "type": "TextualBody",
      "value": "Hello!"
   },
   "target": "http://www.example.com/world.html",
   "via": "http://example.org/annotations/my-annotation"
}
```

### Reading an annotation

#### Request

```
GET http://localhost:9999/w3c/my-container/my-annotation HTTP/1.1

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
   "id": "http://localhost:9999/w3c/my-container/my-annotation",
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

### Updating an annotation

When updating an annotation, you need to send its ETag in the `If-Match` header.

#### Request

```
PUT http://localhost:9999/w3c/my-container/hello-world HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Content-Type: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
If-Match: "1303452440"

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
  "id": "http://localhost:9999/w3c/my-container/hello-world",
  "type": "Annotation",
  "body": {
    "type": "TextualBody",
    "value": "Goodbye!"
  },
  "target": "http://www.example.com/world.html",
  "via": "http://example.org/annotations/my-annotation"
}
```

### Deleting an annotation

When deleting an annotation, you need to send its ETag in the `If-Match` header.

#### Request

```
DELETE http://localhost:9999/w3c/my-container/hello-world HTTP/1.1

If-Match: "1303452440"
```

#### Response

```
HTTP/1.1 204 No Content
```

### Uploading multiple annotations to a given annotation container `(experimental)`

#### Request

```
POST http://localhost:9999/batch/my-container/annotations HTTP/1.1
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

## Querying

### Find annotations with the given field/value combinations `(experimental)`

#### Request

```
POST http://localhost:9999/search/my-container/annotation HTTP/1.1

{
  "body.value": "I like this page!",
  "target": "http://www.example.com/index.html"
}
```

#### Response

```
HTTP/1.1 200 OK

Content-Type: application/json
Content-Length: 935

{
   "id": "http://localhost:9999/search/my-container/annotations?page=0",
   "type": "AnnotationPage",
   "partOf": "http://localhost:9999/search/my-container/annotations",
   "startIndex": 0,
   "items": [
      {
         "@context": "http://www.w3.org/ns/anno.jsonld",
         "id": "http://localhost:9999/w3c/my-container/anno1.jsonld",
         "type": "Annotation",
         "body": {
            "type": "TextualBody",
            "value": "I like this page!"
         },
         "target": "http://www.example.com/index.html"
      },
      {
         "@context": "http://www.w3.org/ns/anno.jsonld",
         "id": "http://localhost:9999/w3c/my-container/eadda8ec-3708-4af6-9283-d6a428c1d1e6",
         "type": "Annotation",
         "body": {
            "type": "TextualBody",
            "value": "I like this page!"
         },
         "target": "http://www.example.com/index.html"
      },
      {
         "@context": "http://www.w3.org/ns/anno.jsonld",
         "id": "http://localhost:9999/w3c/my-container/0bb16696-245c-4614-955f-78dec7065f60",
         "type": "Annotation",
         "body": {
            "type": "TextualBody",
            "value": "I like this page!"
         },
         "target": "http://www.example.com/index.html"
      }
   ]
}
```

### Find annotations that fall within the given range `(experimental)`

#### Request

```
GET http://localhost:9999/search/my-container/within_range?target.source=urn:example:republic:text-1&range.start=50&range.end=210 HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK

Content-Type: application/json
Vary: Accept-Encoding

{
   "id": "http://localhost:9999/search/my-container/within_range?target.source=urn%3Aexample%3Arepublic%3Atext-1&range.start=50.0&range.end=210.0&page=0",
   "type": "AnnotationPage",
   "partOf": "http://localhost:9999/search/my-container/within_range?target.source=urn%3Aexample%3Arepublic%3Atext-1&range.start=50.0&range.end=210.0",
   "startIndex": 0,
   "items": [
      {
         "motivation": "classifying",
         "type": "Annotation",
         "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "attendant",
            "id": "urn:example:republic:person-1"
         },
         "@context": "http://www.w3.org/ns/anno.jsonld",
         "target": {
            "source": "urn:example:republic:text-1",
            "type": "Text",
            "selector": {
               "type": "urn:example:republic:TextAnchorSelector",
               "start": 100,
               "end": 130
            }
         },
         "id": "http://localhost:9999/w3c/my-container/b58a5b5e-ada9-4a13-8995-9588f41d1153"
      },
      {
         "motivation": "classifying",
         "type": "Annotation",
         "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "recipient",
            "id": "urn:example:republic:person-2"
         },
         "@context": "http://www.w3.org/ns/anno.jsonld",
         "target": {
            "source": "urn:example:republic:text-1",
            "type": "Text",
            "selector": {
               "type": "urn:example:republic:TextAnchorSelector",
               "start": 190,
               "end": 200
            }
         },
         "id": "http://localhost:9999/w3c/my-container/62ccf74d-27aa-4f7d-a2ed-edece9bba087"
      }
   ]
}
```

### Find annotations that overlap with the given range `(experimental)`

#### Request

```
GET http://localhost:9999/search/my-container/overlapping_with_range?target.source=urn:example:republic:text-1&range.start=50&range.end=210 HTTP/1.1

```

#### Response

```
HTTP/1.1 200 OK

Content-Type: application/json
Vary: Accept-Encoding

{
   "id": "http://localhost:9999/search/my-container/overlapping_with_range?target.source=urn%3Aexample%3Arepublic%3Atext-1&range.start=50.0&range.end=210.0&page=0",
   "type": "AnnotationPage",
   "partOf": "http://localhost:9999/search/my-container/overlapping_with_range?target.source=urn%3Aexample%3Arepublic%3Atext-1&range.start=50.0&range.end=210.0",
   "startIndex": 0,
   "items": [
      {
         "motivation": "classifying",
         "type": "Annotation",
         "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "attendant",
            "id": "urn:example:republic:person-1"
         },
         "@context": "http://www.w3.org/ns/anno.jsonld",
         "target": {
            "source": "urn:example:republic:text-1",
            "type": "Text",
            "selector": {
               "type": "urn:example:republic:TextAnchorSelector",
               "start": 100,
               "end": 130
            }
         },
         "id": "http://localhost:9999/w3c/my-container/b58a5b5e-ada9-4a13-8995-9588f41d1153"
      },
      {
         "motivation": "classifying",
         "type": "Annotation",
         "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "recipient",
            "id": "urn:example:republic:person-2"
         },
         "@context": "http://www.w3.org/ns/anno.jsonld",
         "target": {
            "source": "urn:example:republic:text-1",
            "type": "Text",
            "selector": {
               "type": "urn:example:republic:TextAnchorSelector",
               "start": 190,
               "end": 200
            }
         },
         "id": "http://localhost:9999/w3c/my-container/62ccf74d-27aa-4f7d-a2ed-edece9bba087"
      },
      {
         "motivation": "classifying",
         "type": "Annotation",
         "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "attendant",
            "id": "urn:example:republic:person-3"
         },
         "@context": "http://www.w3.org/ns/anno.jsonld",
         "target": {
            "source": "urn:example:republic:text-1",
            "type": "Text",
            "selector": {
               "type": "urn:example:republic:TextAnchorSelector",
               "start": 200,
               "end": 220
            }
         },
         "id": "http://localhost:9999/w3c/my-container/470e0052-c3e8-4dce-9dff-512d68f4deab"
      }
   ]
}
```

---

## OpenAPI

AnnoRepo provides an [openapi](https://www.openapis.org/) API definition via [swagger](https://swagger.io/)

The basic swagger UI is available via

```
GET http://localhost:9999/swagger HTTP/1.1
```

and the definition file is available as json via

```
GET http://localhost:9999/swagger.json HTTP/1.1
```

or

```
GET http://localhost:9999/openapi.json HTTP/1.1
```

and as yaml via

```
GET http://localhost:9999/swagger.yaml HTTP/1.1
```

or

```
GET http://localhost:9999/openapi.yaml HTTP/1.1
```

---

## Server info

### Get information about the servers

#### Request

```
GET http://localhost:9999/about HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK

Content-Type: application/json
Vary: Accept-Encoding
Content-Length: 164

{
   "appName": "AnnoRepo",
   "version": "0.1.0",
   "startedAt": "2022-07-04T10:55:12.399444Z",
   "baseURI": "http://localhost:9999",
   "source": "https://github.com/knaw-huc/annorepo"
}
```
