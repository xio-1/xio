#!/bin/bash

RED='\033[0;31m'          # Red
NC='\033[0m'          # No Color

fail=false;

if [ "$fail" = "false" ]; then
    echo -e "${NC}---> starting"
    java -Xmx1g -classpath /artifacts/app.jar org.xio.one.reactive.http.weeio.EdgeServer -wsh host.docker.internal;
else
    echo -e "${RED}exiting!!!!!!";
    echo -e "${NC}";
    exit;
fi

