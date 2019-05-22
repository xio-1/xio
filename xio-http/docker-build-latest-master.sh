#!/bin/bash
docker build -t weeio/app:latest ./
docker build -t weeio/edge:latest ./ -f DockerfileEdge
