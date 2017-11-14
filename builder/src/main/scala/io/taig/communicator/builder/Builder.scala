package io.taig.communicator.builder

trait Builder[+T] {
  def build: T
}
