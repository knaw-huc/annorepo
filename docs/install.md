# AnnoRepo: Installing, configuring and running the server.

For running AnnoRepo, you will need to have access to a [mongodb server](https://www.mongodb.com/docs/manual/)

## From source

- Clone this repository:

  `git clone https://github.com/knaw-huc/annorepo.git`

- Set [`AR_` environment variables](#Configuring) as needed.

- Build and run the server:

  `make run-server`

- In the console, you should see something like this:

```
INFO  [2022-07-08 22:26:49,303] nl.knaw.huc.annorepo.AnnoRepoApplication: AR_ environment variables:

  AR_SERVER_PORT:	(not set, using default)
  AR_EXTERNAL_BASE_URL:	(not set, using default)
  AR_MONGODB_URL:	(not set, using default)
  AR_DB_NAME:	(not set, using default)
  AR_PAGE_SIZE:	(not set, using default)
  AR_ROOT_API_KEY:	(not set, using default)
  AR_WITH_AUTHENTICATION:	(not set, using default)
  AR_PRETTY_PRINT:	(not set, using default)

INFO  [2022-07-08 22:26:49,303] nl.knaw.huc.annorepo.AnnoRepoApplication: connecting to mongodb at mongodb://localhost/ ...
INFO  [2022-07-08 22:26:49,385] nl.knaw.huc.annorepo.AnnoRepoApplication: connected!
INFO  [2022-07-08 22:26:49,478] nl.knaw.huc.annorepo.AnnoRepoApplication: Health checks:
INFO  [2022-07-08 22:26:49,478] nl.knaw.huc.annorepo.AnnoRepoApplication:   deadlocks: healthy, message=''
INFO  [2022-07-08 22:26:49,478] nl.knaw.huc.annorepo.AnnoRepoApplication:   mongodb: healthy, message=''
INFO  [2022-07-08 22:26:49,478] nl.knaw.huc.annorepo.AnnoRepoApplication:   server: healthy, message=''
INFO  [2022-07-08 22:26:49,480] nl.knaw.huc.annorepo.AnnoRepoApplication:

  Starting AnnoRepo (v0.1.0)
    locally accessible at    http://localhost:8080
    externally accessible at http://localhost:8080

INFO  [2022-07-08 22:26:49,481] io.dropwizard.server.ServerFactory: Starting AnnoRepo
================================================================================

 █████╗ ███╗   ██╗███╗   ██╗ ██████╗ ██████╗ ███████╗██████╗  ██████╗
██╔══██╗████╗  ██║████╗  ██║██╔═══██╗██╔══██╗██╔════╝██╔══██╗██╔═══██╗
███████║██╔██╗ ██║██╔██╗ ██║██║   ██║██████╔╝█████╗  ██████╔╝██║   ██║
██╔══██║██║╚██╗██║██║╚██╗██║██║   ██║██╔══██╗██╔══╝  ██╔═══╝ ██║   ██║
██║  ██║██║ ╚████║██║ ╚████║╚██████╔╝██║  ██║███████╗██║     ╚██████╔╝
╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═══╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝      ╚═════╝

================================================================================

```

The "accessible at" lines should tell you at what url to access the annorepo server.

## Configuring

The config file `config.yml` has some values that can be overridden by setting `AR_` environment variables:

| environment variable     | default value                             | purpose                                                                                            |
|--------------------------|-------------------------------------------|----------------------------------------------------------------------------------------------------|
| `AR_SERVER_PORT`         | 8080                                      | The main port for accessing the server locally.                                                    |
| `AR_EXTERNAL_BASE_URL`   | http://localhost:8080                     | The URL at which the server can be accessed externally. (in case of proxying)                      |
| `AR_PAGE_SIZE`           | 100                                       | The number of annotations to show per AnnotationPage.                                              |
| `AR_MONGODB_URL`         | mongodb://localhost/                      | The mongodb URL                                                                                    |
| `AR_WITH_AUTHENTICATION` | false                                     | Whether this server should require authentication for certain endpoints.                           |
| `AR_ROOT_API_KEY`        | thisdefaultapikeyisunsafesochangeitplease | The api-key for the root user. (only used when `AR_WITH_AUTHENTICATION` = true)                    |
| `AR_PRETTY_PRINT`        | true                                      | Whether the json output should be formatted for easier human-readability (true) or compact (false) |

## Using docker image

Docker images are available from registry.ghcr.io:

- Run the docker image, with the required [`AR_` environment variables](#Configuring):

  `docker run -p 8080:8080 registry.ghcr.io/knaw-huc/annorepo-server:v0.6.3`

- Alternatively, check the [docker-compose.yml example](../k8s/local/docker-compose.yml) to run mongodb + annorepo using
  docker-compose.

## [API Usage](api-usage.md)
