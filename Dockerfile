FROM        taig/scala:1.0.6

MAINTAINER  Niklas Klein "mail@taig.io"

ENV         PHOENIX_ECHO d4e76204b96dbaa64c6800e8f0ca8fb41d1d1eac

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
ADD         ./build.sbt ./cache/
ADD         ./project/build.properties ./cache/project/
ADD         ./project/plugins.sbt ./cache/project/
ADD         ./project/Settings.scala ./cache/project/
RUN         cd ./cache/ && sbt ";test;tut"
RUN         rm -r ./cache

WORKDIR     /communicator/