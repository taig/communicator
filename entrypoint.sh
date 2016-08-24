#!/bin/bash

set -e # halt on errors

cd ~/phoenix_echo/ && elixir --detached -S mix do phoenix.server
cd /communicator/ && sbt test