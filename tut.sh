#!/bin/bash

# Docker entrypoint for tut documentation generation
#
# docker run --entrypoint="./tut.sh" -v "$PWD:/communicator/" --rm taig/communicator:latest

set -e

sbt tut