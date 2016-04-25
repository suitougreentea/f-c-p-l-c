package io.github.suitougreentea.fcplc

open class Block(var y: Long, val color: Int) {
  // -4 -> おじゃま消去中
  // -3 -> おじゃま消去中(最下段)
  // -2 -> 消去中
  // -1 -> swap中
  // 256 -> おじゃま
  var stayTimer = 0
  var velocity = 0
  var chain = false
  var active = false
  var disabled = false

  fun activate(initVelocity: Int = 0) {
    active = true
    stayTimer = 1
    velocity = initVelocity
  }

  fun deactivate() {
    active = false
    stayTimer = 0
    velocity = 0
  }
}

class BlockGarbage(y: Long, val id: Int): Block(y, 256)
class BlockGarbageExtract(y: Long, val extractColor : Int, val id: Int): Block(y, -3)
class BlockGarbageErasing(y: Long, val id: Int): Block(y, -4)
