FROM azul/zulu-openjdk:11
RUN apt-get -y update
RUN apt-get -y install iputils-ping

RUN mkdir /artifacts
COPY target/app.jar /artifacts/app.jar
COPY entrypoint_edge.sh /artifacts/start.sh

RUN  chmod a+x /artifacts/start.sh
EXPOSE 8080

ENTRYPOINT /artifacts/start.sh
