## To deploy in production

You have to define `production.conf` file at the root of the project and it must
contain at least these lines

```
include "application.conf"
# https://www.playframework.com/documentation/latest/Configuration
http.port = 8080

# sbt command playGenerateSecret
play.http.secret.key = "changeme"
slick.dbs.default.db.url = "jdbc:clickhouse://clickhouseinternalnodename:8123/ot"
```