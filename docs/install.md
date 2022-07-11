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

  AR_SERVER_PORT:	9999
  AR_EXTERNAL_BASE_URL:	http://localhost:9999
  AR_MONGODB_URL:	(not set, using default)
  AR_DB_NAME:	(not set, using default)
  AR_PAGE_SIZE:	10

INFO  [2022-07-08 22:26:49,303] nl.knaw.huc.annorepo.AnnoRepoApplication: connecting to mongodb at mongodb://localhost/ ...
INFO  [2022-07-08 22:26:49,385] nl.knaw.huc.annorepo.AnnoRepoApplication: connected!
INFO  [2022-07-08 22:26:49,478] nl.knaw.huc.annorepo.AnnoRepoApplication: Health checks:
INFO  [2022-07-08 22:26:49,478] nl.knaw.huc.annorepo.AnnoRepoApplication:   deadlocks: healthy, message=''
INFO  [2022-07-08 22:26:49,478] nl.knaw.huc.annorepo.AnnoRepoApplication:   mongodb: healthy, message=''
INFO  [2022-07-08 22:26:49,478] nl.knaw.huc.annorepo.AnnoRepoApplication:   server: healthy, message=''
INFO  [2022-07-08 22:26:49,480] nl.knaw.huc.annorepo.AnnoRepoApplication:

  Starting AnnoRepo (v0.1.0)
    locally accessible at    http://localhost:9999
    externally accessible at http://localhost:9999

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

The config file `config.yml` has some values that can be overridden by setting `AF_` environment variables:

| environment variable   | default value         | purpose                                                 |
|------------------------|-----------------------|---------------------------------------------------------|
| `AR_SERVER_PORT`       | 8080                  | main port for accessing the server locally              |
| `AR_EXTERNAL_BASE_URL` | http://localhost:8080 | the url at which the server can be accessed externally. |
| `AR_PAGE_SIZE`         | 100                   | The number of annotations to show per AnnotationPage    |
| `AR_MONGODB_URL`       | mongodb://localhost/  | The mongodb url                                         |
| `AR_DB_NAME`           | annorepo              | The name of the mongo database to use                   |

## Using docker image

Currently, the docker image is only available from the firewalled registry.diginfra.net repo:

- Run the docker image, with the required [`AR_` environment variables](#Configuring):

  `docker run -p 8080:8080 registry.diginfra.net/tt/annorepo`

- Alternatively, check the [docker-compose.yml example](k8s/local/docker-compose.yml) to run mongodb + annorepo using
  docker-compose.

## [API Usage](api-usage.md)