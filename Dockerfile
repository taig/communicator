FROM        taig/scala:1.0.4

MAINTAINER  Niklas Klein "mail@taig.io"

ENV         PHOENIX_ECHO 5055696fce8eed8780175bed64033480f19076e5

# Please Elixir (which complains when not using UTF-8)
RUN         locale-gen en_US.UTF-8  
ENV         LANG en_US.UTF-8  
ENV         LANGUAGE en_US:en  
ENV         LC_ALL en_US.UTF-8 

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

RUN         wget https://github.com/PragTob/phoenix_echo/archive/$PHOENIX_ECHO.zip
RUN         unzip ./$PHOENIX_ECHO.zip
RUN         mv ./phoenix_echo-$PHOENIX_ECHO/ ./phoenix_echo/
RUN         rm -r ./$PHOENIX_ECHO.zip

RUN         mix local.hex --force
RUN         mix local.rebar --force
RUN         cd ./phoenix_echo/ && mix deps.get
RUN         cd ./phoenix_echo/ && mix compile

WORKDIR     /communicator/
ADD         . .

ENTRYPOINT  [ "/communicator/entrypoint.sh" ]