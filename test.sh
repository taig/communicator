#!/bin/bash

# Docker entrypoint for test suite execution
#
# docker run -e CODECOV_TOKEN=$CODECOV_TOKEN --entrypoint="./test.sh" -v "$PWD:/communicator/" --rm taig/communicator:latest

set -e

sbt ";scalafmt::test;test:scalafmt::test;sbt:scalafmt::test"
sbt ";coverage;+test;+tut;coverageReport;coverageAggregate"

if [ -n "$CODECOV_TOKEN" ]; then
    codecov
fi