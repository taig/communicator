#!/bin/bash

# Docker entrypoint for test suite execution

set -e # halt on errors

cd ~/phoenix_echo/
elixir --detached -S mix do phoenix.server

cd /communicator/
sbt ";coverage;test;coverageReport;coverageAggregate;tut"

if [ -z "$CODECOV_TOKEN" ]; then
    codecov
fi
