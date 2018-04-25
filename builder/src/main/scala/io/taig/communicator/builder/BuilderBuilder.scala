package io.taig.communicator.builder

trait BuilderBuilder[+T] {
  def build: T
}
