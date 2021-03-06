[![Project Status: WIP – Initial development is in progress, but there has not yet been a stable, usable release suitable for the public.](https://www.repostatus.org/badges/latest/wip.svg)](https://www.repostatus.org/#wip)
[![Stability: Experimental](https://masterminds.github.io/stability/experimental.svg)](https://masterminds.github.io/stability/experimental.html)
[![Build Status](https://github.com/HuygensING/hyper-collate/workflows/tests/badge.svg)](https://github.com/knaw-huc/annorepo/actions)
![GitHub language count](https://img.shields.io/github/languages/count/knaw-huc/annorepo)
![GitHub](https://img.shields.io/github/license/knaw-huc/annorepo)
[![Known Vulnerabilities](https://snyk.io/test/github/knaw-huc/annorepo/badge.svg)](https://snyk.io/test/github/knaw-huc/annorepo)

[//]: # (![Swagger Validator]&#40;https://img.shields.io/swagger/valid/3.0?specUrl=https%3A%2F%2Fraw.githubusercontent.com%2Fknaw-huc%2Fannorepo%2Fmain%2Fdocs%2Fswagger.json&#41;)

[//]: # (![GitHub tag &#40;latest by date&#41;]&#40;https://img.shields.io/github/v/tag/knaw-huc/annorepo&#41;)

# AnnoRepo

A repository for [W3C Web Annotations](https://www.w3.org/TR/annotation-model/), implementing
the [W3C Web Annotation Protocol](https://www.w3.org/TR/annotation-protocol/).

Inspired by [elucidate](https://github.com/dlcs/elucidate-server).

How to start the AnnoRepo application
---

1. Run `mvn clean install` to build your application
1. Start application with `java -jar target/annorepo-0.1-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:8080`

Health Check
---

To see your application's health enter url `http://localhost:8081/healthcheck`

----

[USAGE](docs/usage.md) |
[LICENSE](LICENSE)