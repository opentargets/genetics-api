## To deploy in production

You have to define `production.conf` file at the root of the project and it must
contain at least these lines

```
include "application.conf"
# https://www.playframework.com/documentation/latest/Configuration
http.port = 8080

# sbt command playGenerateSecret
play.http.secret.key = "changeme"
slick.dbs {
  default {
    profile = "clickhouse.ClickHouseProfile$"
    db {
      driver = "ru.yandex.clickhouse.ClickHouseDriver"
      url = "jdbc:clickhouse://clickhouseinternalnodename1:8123/ot"
    }
  }
  sumstats {
    profile = "clickhouse.ClickHouseProfile$"
    db {
      driver = "ru.yandex.clickhouse.ClickHouseDriver"
      url = "jdbc:clickhouse://clickhouseinternalnodename2:8123/sumstats"
    }
  }
}

ot.elasticsearch {
  host = "clickhouseinternalnodename3.c.open-targets-genetics.internal"
  port = 9200
}


# slick.dbs.default.db.url=${?SLICK_CLICKHOUSE_URL}
# ot.elasticsearch.host=${?ELASTICSEARCH_HOST}

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
