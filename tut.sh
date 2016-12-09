#!/bin/bash

# Docker entrypoint for tut documentation generation
#
# docker run --entrypoint="./tut.sh" -v "$PWD:/communicator/" --rm taig/communicator:latest

cd ~/phoenix_echo/
elixir --detached -S mix do phoenix.server
cd -

sbt tut