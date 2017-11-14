FROM        taig/scala:1.0.9

MAINTAINER  Niklas Klein "mail@taig.io"

# Install python & pip, necessary to submit test coverage
RUN         apt-get install -y --no-install-recommends python-pip
RUN         apt-get clean
RUN         pip install --upgrade pip
RUN         pip install setuptools
RUN         pip install codecov

# Cache project dependencies
RUN         mkdir -p ./cache/phoenix/src/test/scala/
ADD         ./project/ ./cache/project/
ADD         ./build.sbt ./cache/
RUN         echo "object Foobar" > ./cache/phoenix/src/test/scala/Foobar.scala
RUN         cd ./cache/ && sbt ";coverage;+test;+tut"
RUN         rm -r ./cache

WORKDIR     /communicator/