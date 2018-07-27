FROM gcr.io/google-appengine/openjdk
RUN mkdir -p /srv/app

COPY target/universal/ot-geckoapi-latest.zip /srv/app/ot-geckoapi-latest.zip
COPY production.conf /srv/app/production.conf

WORKDIR /srv/app

RUN cd /srv/app && unzip ot-geckoapi-latest.zip

RUN echo ${CH_URL} ${PLAY_SECRET}

RUN chmod +x ot-geckoapi-latest/bin/ot-geckoapi
ENTRYPOINT ot-geckoapi-latest/bin/ot-geckoapi -Dconfig.file=/srv/app/production.conf
#    -Dplay.http.secret.key=${PLAY_SECRET}
#    -Dslick.dbs.default.db.url=${CH_URL}
