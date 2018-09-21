FROM gcr.io/google-appengine/openjdk
RUN mkdir -p /srv/app

COPY target/universal/ot-genetics-api-latest.zip /srv/app/ot-genetics-api-latest.zip
COPY production.conf /srv/app/production.conf

WORKDIR /srv/app
RUN unzip ot-genetics-api-latest.zip

RUN echo "${CH_URL} ${PLAY_SECRET}"

RUN chmod +x ot-genetics-api-latest/bin/ot-genetics-api
ENTRYPOINT ot-genetics-api-latest/bin/ot-genetics-api -Dconfig.file=/srv/app/production.conf
#    -Dplay.http.secret.key=${PLAY_SECRET}
#    -Dslick.dbs.default.db.url=${CH_URL}
