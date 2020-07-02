[![codecov](https://codecov.io/gh/opentargets/genetics-api/branch/master/graph/badge.svg)](https://codecov.io/gh/opentargets/genetics-api)
[![Build Status](https://travis-ci.com/opentargets/genetics-api.svg)](https://travis-ci.com/opentargets/genetics-api)
[![Docker Repository on Quay](https://quay.io/repository/opentargets/genetics-api/status "Docker Repository on Quay")](https://quay.io/repository/opentargets/genetics-api)

# Open Targets Genetics GraphQL API
This repo contains the application code for the GraphQL of [Open Targets Genetics](https://genetics.opentargets.org/).

It is one component of several and the overarching project is described [here](https://github.com/opentargets/genetics), which is also where issues can be raised.

## User guide

The API is a GraphQL implementation and can be queried at the following [endpoint](http://genetics-api.opentargets.io/graphql/browser)

The data used by the service is updated regularly; to see which version is currently in use you can use the following query in the browser linked to above:

```graqhql
query metadataQ {
  meta{
    dataVersion {
      major
      minor
      patch
    }
  }
}
``` 
The `dataVersion` can be typically interpreted as the major version being the year, the minor version the month, and the patch number indicating iterative releases. Older data can be found on the [Genetics Portal's data mirror](ftp://ftp.ebi.ac.uk/pub/databases/opentargets/genetics/).

## To set up development
You can use an IDE such as IntelliJ IDEA, which is recommended, but it's also sufficient to have `sbt` installed.

### Connect to development databases
The current development database configuration contains the following nodes:
* **default**: runs clickhouse and elasticsearch instances covering most queries
* **sumstats**: runs clickhouse instance for summary statistics, which drives the phewas plot

You can forward the needed ports to the remote databases defined in conf/application.conf.
```
gcloud --project=<gcloud-project> compute ssh <default> -- -L 8123:localhost:8123 -L 9200:localhost:9200
gcloud --project=<gcloud-project> compute ssh <sumstats> -- -L 8124:localhost:8123
```

### Run the API
Hit build within IntelliJ. Alternatively, start `sbt` in a terminal and use the `run` and `test` commands. If the build was successful, you should be able to view the running API at `localhost:9000`.

## To deploy in production

You have to define `production.conf` file at the root of the project and it must contain at least these lines

```
include "application.conf"
# https://www.playframework.com/documentation/latest/Configuration
http.port = 8080
http.port = ${?PLAY_PORT}
play.http.secret.key = "changeme"
play.http.secret.key = ${?PLAY_SECRET}

# Root logger:
logger=ERROR
# Logger used by the framework:
logger.play=ERROR
# Logger provided to your application:
logger.application=INFO

slick.dbs {
  default {
    profile = "clickhouse.ClickHouseProfile$"
    db {
      driver = "ru.yandex.clickhouse.ClickHouseDriver"
      url = "jdbc:clickhouse://machine.internal:8123/ot"
      url = ${?SLICK_CLICKHOUSE_URL}
      numThreads = 4
      queueSize = 128
    }
  }
  sumstats {
    profile = "clickhouse.ClickHouseProfile$"
    db {
      driver = "ru.yandex.clickhouse.ClickHouseDriver"
      url = "jdbc:clickhouse://machine2.internal:8123/sumstats"
      url = ${?SLICK_CLICKHOUSE_URL_SS}
      numThreads = 4
      queueSize = 128
    }
  }
}

ot.elasticsearch {
  host = "machine.internal:8123"
  host = ${?ELASTICSEARCH_HOST}
  port = 9200
}

# env vars to pass to docker image or `-D`
# http.port=${?PLAY_PORT}
# play.http.secret.key=${?PLAY_SECRET}
# slick.dbs.default.db.url=${?SLICK_CLICKHOUSE_URL}
# ot.elasticsearch.host=${?ELASTICSEARCH_HOST}
# slick.dbs.default.db.url=${?SLICK_CLICKHOUSE_URL_SS}

```

# Copyright
Copyright 2014-2018 Biogen, Celgene Corporation, EMBL - European Bioinformatics Institute, GlaxoSmithKline, Takeda Pharmaceutical Company and Wellcome Sanger Institute

This software was developed as part of the Open Targets project. For more information please see: http://www.opentargets.org

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
