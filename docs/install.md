# AnnoRepo: Installing, configuring and running the server.

## Using docker image

A docker image is available at [dockerhuh](https://hub.docker.com/r/knawhuc/annorepo)

`docker pull knawhuc/annorepo`

## From source

Clone this repository:

`git clone https://github.com/knaw-huc/annorepo.git`

Build and run the server:

`make run-server`

## Configuring

The config file `config.yml` has some values that can be overridden by setting `AF_` environment variables:

| environment variable   | default value         | purpose                                                 |
|------------------------|-----------------------|---------------------------------------------------------|
| `AR_SERVER_PORT`       | 8080                  | main port for accessing the server locally              |
| `AR_EXTERNAL_BASE_URL` | http://localhost:8080 | the url at which the server can be accessed externally. |
| `AR_PAGE_SIZE`         | 100                   | The number of annotations to show per AnnotationPage    |
| `AR_MONGODB_URL`       | mongodb://localhost/  | The mongodb url                                         |
| `AR_DB_NAME`           | annorepo              | The name of the mongo database to use                   |


