# AnnoRepo

## usage

For the basic annotation CRUD handling, AnnoRepo has a `/w3c/` endpoint that implements part of
the [W3C Web Annotation Protocol](https://www.w3.org/TR/2017/REC-annotation-protocol-20170223/)
As the protocol does not specify how to create, update or delete annotation containers, AnnoRep implements endpoints for
that in a way similar to that used by [elucidate](https://github.com/dlcs/elucidate-server)

The following requests expect the annorepo server running locally at `http://localhost:9999/`

## Annotation Containers

### creating an annotation container

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
HTTP/1.1 201 CREATED

Accept-Post: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld", text/turtle
Allow: POST,GET,OPTIONS,HEAD
Content-Location: http://localhost:9999/w3c/my-container/
Content-Type: application/ld+json;charset=UTF-8
Link: <http://www.w3.org/ns/ldp#BasicContainer>; rel="type", <http://www.w3.org/TR/annotation-protocol/>; rel="http://www.w3.org/ns/ldp#constrainedBy"
Location: http://localhost:9999/w3c/my-container/
Vary: Origin, Accept, Prefer

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
  "label": "A Container for Web Annotations",
  "first": {
    "type": "AnnotationPage",
    "as:items": {
      "@list": []
    },
    "partOf": "http://localhost:9999/w3c/my-container/",
    "startIndex": 0
  },
  "last": "http://localhost:9999/w3c/my-container/?page=0&desc=1",
  "total": 0
}
```

### reading an annotation container

#### Request

```
GET http://localhost:9999/w3c/my-container/ HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
```

#### Response

```

HTTP/1.1 200 OK

Accept-Post: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld", text/turtle
Allow: POST,GET,OPTIONS,HEAD
Content-Location: http://localhost:9999/w3c/my-container/
Content-Type: application/ld+json;charset=UTF-8
Link: <http://www.w3.org/ns/ldp#BasicContainer>; rel="type", <http://www.w3.org/TR/annotation-protocol/>; rel="http://www.w3.org/ns/ldp#constrainedBy"
Vary: Accept,Prefer

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
  "label": "A Container for Web Annotations",
  "first": {
    "type": "AnnotationPage",
    "as:items": {
      "@list": []
    },
    "partOf": "http://localhost:9999/w3c/my-container/",
    "startIndex": 0
  },
  "last": "http://localhost:9999/w3c/my-container/?page=0&desc=1",
  "total": 0
}
```

### deleting an annotation container

Deleting an annotation container is only possible if the container doesn't contain any annotations.

#### Request

```
DELETE http://localhost:9999/w3c/my-container/ HTTP/1.1

```

#### Response

```

HTTP/1.1 200 OK

Accept-Post: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld", text/turtle
Allow: POST,GET,OPTIONS,HEAD
Content-Location: http://localhost:9999/w3c/my-container/
Content-Type: application/ld+json;charset=UTF-8
Link: <http://www.w3.org/ns/ldp#BasicContainer>; rel="type", <http://www.w3.org/TR/annotation-protocol/>; rel="http://www.w3.org/ns/ldp#constrainedBy"
Vary: Accept,Prefer

{
```

## Annotations

### adding an annotation to a given annotation container

#### Request

```
POST http://localhost:9999/w3c/mycontainer/ HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Content-Type: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
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

Allow: PUT,GET,OPTIONS,HEAD,DELETE
Content-Type: application/ld+json;charset=UTF-8
ETag: W/"797c2ee5253966de8882f496c25dd823"
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Location: http://localhost:9999/w3c/my-container/my-annotation
Vary: Origin, Accept

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "http://localhost:9999/w3c/my-container/my-annotation",
  "type": "Annotation",
  "body": {
    "type": "TextualBody",
    "value": "I like this page!"
  },
  "target": "http://www.example.com/index.html",
}
```

### reading an annotation

#### Request

```
GET http://localhost:9999/w3c/my-container/my-annotation HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
```

#### Response

```
HTTP/1.1 200 OK

Allow: PUT,GET,OPTIONS,HEAD,DELETE
Content-Type: application/ld+json;charset=UTF-8
ETag: W/"797c2ee5253966de8882f496c25dd823"
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Vary: Accept

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "http://localhost:9999/w3c/my-container/my-annotation",
  "type": "Annotation",
  "body": {
    "type": "TextualBody",
    "value": "I like this page!"
  },
  "target": "http://www.example.com/index.html",
}
```

### updating an annotation

#### Request

```
PUT http://localhost:9999/w3c/my-container/my-annotation HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Content-Type: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
If-Match: 797c2ee5253966de8882f496c25dd823

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "http://localhost:9999/w3c/mycontainer/anno1",
  "type": "Annotation",
  "created": "2015-01-31T12:03:45Z",
  "body": {
    "type": "TextualBody",
    "value": "I don't like this page!"
  },
  "target": "http://www.example.com/index.html"
}
```

#### Response

```
HTTP/1.1 200 OK

Allow: PUT,GET,OPTIONS,HEAD,DELETE
Content-Type: application/ld+json;charset=UTF-8
ETag: W/"24d535a13f2c16e2701bf46b11407cea"
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Vary: Origin, Accept

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "http://localhost:9999/w3c/my-collection/my-annotation",
  "type": "Annotation",
  "body": {
    "type": "TextualBody",
    "value": "I like this page!"
  },
  "target": "http://www.example.com/index.html"
}
```

### deleting an annotation

#### Request

```
DELETE http://localhost:9999/w3c/my-container/my-annotation HTTP/1.1

If-Match: 24d535a13f2c16e2701bf46b11407cea
```

#### Response

```
HTTP/1.1 204 No Content
```

### uploading multiple annotations to a given annotation container

#### Request

```
POST http://localhost:9999/w3c/mycontainer/ HTTP/1.1

Accept: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"
Content-Type: application/ld+json; profile="http://www.w3.org/ns/anno.jsonld"

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
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

Allow: PUT,GET,OPTIONS,HEAD,DELETE
Content-Type: application/ld+json;charset=UTF-8
ETag: W/"797c2ee5253966de8882f496c25dd823"
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Location: http://localhost:9999/w3c/my-container/my-annotation
Vary: Origin, Accept

{
  "@context": "http://www.w3.org/ns/anno.jsonld",
  "id": "http://localhost:9999/w3c/my-container/my-annotation",
  "type": "Annotation",
  "body": {
    "type": "TextualBody",
    "value": "I like this page!"
  },
  "target": "http://www.example.com/index.html",
}
```

## Querying

### find annotations with the given field/value combinations

#### Request

```
POST http://localhost:9999/w3c/mycontainer/ HTTP/1.1

```

#### Response

```
HTTP/1.1 201 CREATED

```

### find annotations that overlap with the given range

#### Request

```
POST http://localhost:9999/w3c/mycontainer/ HTTP/1.1

```

#### Response

```
HTTP/1.1 201 CREATED

```

### find annotations that fall within the given range

#### Request

```
POST http://localhost:9999/w3c/mycontainer/ HTTP/1.1

```

#### Response

```
HTTP/1.1 201 CREATED

```

## Server info

### get information about the servers

#### Request

```
GET http://localhost:9999/about HTTP/1.1
```

#### Response

```
HTTP/1.1 200 OK

Date: Mon, 04 Jul 2022 10:55:27 GMT
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
