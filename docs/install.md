# AnnoRepo: Installing, configuring and running the server.

For running AnnoRepo, you will need to have access to a [mongodb server](https://www.mongodb.com/docs/manual/)

## From source

- Clone this repository:

  `git clone https://github.com/knaw-huc/annorepo.git`

- Set [`AR_` environment variables](#Configuring) as needed.

- Build and run the server:

  `make run-server-without-auth`

- In the console, you should see something like this:

```
INFO  [2022-07-08 22:26:49,303] nl.knaw.huc.annorepo.AnnoRepoApplication: AR_ environment variables:

  AR_SERVER_PORT:	(not set, using default)
  AR_EXTERNAL_BASE_URL:	(not set, using default)
  AR_MONGODB_URL:	(not set, using default)
  AR_DB_NAME:	(not set, using default)
  AR_PAGE_SIZE:	(not set, using default)
  AR_ROOT_API_KEY:	(not set, using default)
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

## Using docker image

Docker images are available from ghcr.io:

- Run the docker image, with the required [`AR_` environment variables](#Configuring) and using [the latest release](https://github.com/knaw-huc/annorepo/pkgs/container/annorepo-server):

  `docker run -p 8080:8080 ghcr.io/knaw-huc/annorepo-server:latest`

- Alternatively, check the [docker-compose.yml example](../k8s/local/docker-compose.yml) to run mongodb + annorepo using
  docker-compose.

## Configuring

From version `0.8.0`, enabling authentication/authorization is done by using either `config-with-auth.yml` or `config-without-auth.yml` 
These config files have some values that can be overridden by setting `AR_` environment variables:

| environment variable   | default value                             | purpose                                                                                            |
|------------------------|-------------------------------------------|----------------------------------------------------------------------------------------------------|
| `AR_SERVER_PORT`       | 8080                                      | The main port for accessing the server locally.                                                    |
| `AR_EXTERNAL_BASE_URL` | http://localhost:8080                     | The URL at which the server can be accessed externally. (in case of proxying)                      |
| `AR_PAGE_SIZE`         | 100                                       | The number of annotations to show per AnnotationPage.                                              |
| `AR_MONGODB_URL`       | mongodb://localhost/                      | The mongodb URL                                                                                    |
| `AR_DB_NAME`           | annorepo                                  | The mongodb database to use (will be created if needed)                                            |
                    |
| `AR_PRETTY_PRINT`      | true                                      | Whether the json output should be formatted for easier human-readability (true) or compact (false) |

The config file `config-with-auth.yml` has these additional `AR_` variables:

| environment variable   | default value                             | purpose                                                                                            |
|------------------------|-------------------------------------------|----------------------------------------------------------------------------------------------------|
| `AR_ROOT_API_KEY`      | thisdefaultapikeyisunsafesochangeitplease | The api-key for the root user.
| `AR_APPLICATION_TOKEN` | default-value                             | The SRAM Application Token for this instance of the annorepo server


## Authentication

From version `0.8.0`, there are 3 ways for a api-users to identify themselves:

### using annorepo-internal user/api-key combinations

In the `authentication:` setting in the config, this requires the `rootApiKey:` to be set, since it's the root user that will initially have to add users and their api-keys 

### using OpenId ID tokens from registered OIDC servers

In the `authentication:` setting in the config, add the `oidc:` setting, with a list of the oidc servers you want to use for authentication.
Each oidc-server has the following config settings:

- `name` (optional) - the name of this oidc config. For display purposes only
- `serverUrl` (required) - the URL of the OIDC server to use. This server should have [a `/.well-known/openid-configuration` endpoint](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig)
- `requiredIssuer` (optional) - the `iss` value that should be encoded in [the ID token](https://openid.net/specs/openid-connect-core-1_0.html#IDToken).
- `requiredAudiences` (optional) - a list of `aud` values, at least one of which should be encoded in [the ID token](https://openid.net/specs/openid-connect-core-1_0.html#IDToken).


### using access tokens from [SURF Research Access Management (SRAM)](https://servicedesk.surf.nl/wiki/spaces/IAM/pages/74226073/SURF+Research+Access+Management)

In the `authentication:` setting in the config, add the `sram:` setting,
with the following config settings:

- `introspectUrl` - the `introspect` endpoint URL for the SRAM server, typically https://sram.surf.nl/api/tokens/introspect
- `applicationToken` - the [User introspection token](https://servicedesk.surf.nl/wiki/spaces/IAM/pages/74226123/Connect+a+token-based+application) assigned to this annorepo server instance.

## [API Usage](api-usage.md)
