FROM azul/zulu-openjdk:11
RUN apt-get -y update
RUN apt-get -y install iputils-ping

RUN mkdir /artifacts
COPY target/app.jar /artifacts/app.jar
COPY entrypoint.sh /artifacts/start.sh

RUN  chmod a+x /artifacts/start.sh
EXPOSE 8000
EXPOSE 7000

ENTRYPOINT /artifacts/start.sh
