FROM        taig/scala:1.0.8

MAINTAINER  Niklas Klein "mail@taig.io"

ENV         PHOENIX_ECHO 8c91a3d846e8d3f034d78d34c51decec7ccdf521

# Install Erlang & Elixir
RUN         wget https://packages.erlang-solutions.com/erlang-solutions_1.0_all.deb
RUN         dpkg -i erlang-solutions_1.0_all.deb
RUN         rm erlang-solutions_1.0_all.deb
RUN         apt-get update

RUN         apt-get install -y --no-install-recommends \
                esl-erlang \
                elixir \
                git \
                unzip
RUN         apt-get clean

# Install Phoenix Echo application
RUN         wget https://github.com/PragTob/phoenix_echo/archive/$PHOENIX_ECHO.zip
RUN         unzip ./$PHOENIX_ECHO.zip
RUN         mv ./phoenix_echo-$PHOENIX_ECHO/ ./phoenix_echo/
RUN         rm -r ./$PHOENIX_ECHO.zip

RUN         mix local.hex --force
RUN         mix local.rebar --force
RUN         cd ./phoenix_echo/ && mix deps.get
RUN         cd ./phoenix_echo/ && mix compile

# Install python & pip, necessary to submit test coverage
RUN         apt-get install -y --no-install-recommends \
                python-pip
RUN         apt-get clean
RUN         pip install --upgrade pip
RUN         pip install setuptools
RUN         pip install codecov

# Cache project dependencies
RUN         mkdir -p ./cache/phoenix/src/test/scala/
ADD         ./project/ ./cache/project/
ADD         ./build.sbt ./cache/
RUN         echo "object Foobar" > ./cache/phoenix/src/test/scala/Foobar.scala
RUN         cd ./cache/ && sbt ";coverage;test;tut"
RUN         rm -r ./cache

RUN         cd ~/.sbt/0.13/ && echo "skip in update := true" > offline.sbt

WORKDIR     /communicator/