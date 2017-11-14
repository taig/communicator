FROM        openjdk:8-jdk-alpine

# Install wget and unzip; add SSL certs to wget
RUN         apk update
RUN         apk add --no-cache bash git wget
RUN         update-ca-certificates

# Install python & pip, necessary to submit test coverage
RUN         apk add py-pip
RUN         pip install --upgrade pip
RUN         pip install setuptools
RUN         pip install codecov

# Install Sbt
RUN         wget -q https://raw.githubusercontent.com/paulp/sbt-extras/master/sbt -O /bin/sbt
RUN         chmod +x /bin/sbt

# Cache project dependencies
RUN         mkdir -p ./cache/request/src/test/scala/
ADD         ./project/ ./cache/project/
ADD         ./build.sbt ./cache/
RUN         echo "object Foobar" > ./cache/request/src/test/scala/Foobar.scala
RUN         cd ./cache/ && sbt ";coverage;+test;+tut"
RUN         rm -r ./cache

WORKDIR     /app/