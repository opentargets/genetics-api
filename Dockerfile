FROM gcr.io/google-appengine/openjdk:8
RUN mkdir -p /srv/app

COPY target/universal/ot-genetics-api-latest.zip /srv/app/ot-genetics-api-latest.zip
COPY production.conf /srv/app/production.conf

WORKDIR /srv/app
RUN unzip ot-genetics-api-latest.zip

RUN chmod +x ot-genetics-api-latest/bin/ot-genetics-api
ENTRYPOINT ot-genetics-api-latest/bin/ot-genetics-api -J-Xms2g -J-Xmx7g -J-server -Dconfig.file=/srv/app/production.conf
