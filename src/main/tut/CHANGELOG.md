# Changelog

## 3.0.1

_2017-01-09_

 * Fix heartbeat issues not beng killed properly under all circumstances
 * Upgrade to monix 2.1.2
 * Disable logging of mockwebserver
 * Add logging to phoenix module (such as heartbeat events)
 * Don't publish empty jar of root project

## 3.0.0

_2016-12-27_

 * Remove common module (phoenix depends on request now)
 * Remove logging
 * Finalize readme

## 3.0.0-RC12

_2016-12-23_

 * Remove `Result` ADT

## 3.0.0-RC11

_2016-12-22_

 * Revisit phoenix module
 * Delete builder module
 * Delete websocket module
 * Upgrade to monix 2.1.2
 * Upgrade to tut 0.4.8

## 3.0.0-RC10

_2016-11-12_

 * Move tut documentation into root project
 * Upgrade to monix 2.1.1
 * Upgrade to circe 0.6.1
 * Upgrade to tut 0.4.7

## 3.0.0-RC9

_2016-11-10_

 * Flag websocket & phoenix modules as experimental
 * Upgrade to monix 2.1.0
 * Upgrade to circe 0.6.0
 * Upgrade to cats 0.8.1
 * Upgrade to scalatest 3.0.1

## 3.0.0-RC8

_2016-11-08_

 * Upgrade to okhttp 3.4.2
 * Upgrade to circe 0.5.4
 * Upgrade to monix 2.0.6
 * Upgrade to tut 0.4.6
 * Upgrade to sbt-scalariform 1.7.1
 * Upgrade to sbt 0.13.13

## 3.0.0-RC7

_2016-10-18_

 * Introduce builder module
 * Upgrade to scala-logging 3.5.0
 * Upgrade to sbt-scoverage 1.4.0
 * Upgrade to tut 0.4.4
 * Upgrade to monix 2.0.3
 * Upgrade to circe 0.5.3
 * Upgrade to monix 2.0.4

## 3.0.0-RC6

_2016-09-19_

 * Add reconnect parameter to phoenix constructor

## 3.0.0-RC5

_2016-09-12_

 * Upgrade to cats 0.7.2
 * Upgrade to monix 2.0.1
 * Upgrade to circe 0.5.1

## 3.0.0-RC4

_2016-09-06_

 * Improved `phoenix` module stability with blackbox tests
 * Minor API changes in `websocket` and `phoenix` modules

## 3.0.0-RC3

_2016-08-22_

 * Introduce Phoenix Channels module
 * Revisit WebSocket module
 * Upgrade to monix 2.0-RC11

## 3.0.0-RC2

_2016-08-01_

 * Upgrade to monix 2.0-RC9

## 3.0.0-RC1

_2016-07-25_

 * Migrated from `scala.concurrent.Future` to `monix.Task`
 * Introduced WebSocket module
 * Introduced `Parser[Array[Byte]]` and `Parser[InputStream]`
 * Upgraded to okhttp 3.4.1