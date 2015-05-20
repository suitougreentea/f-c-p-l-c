package io.github.suitougreentea.fcplc

data class EraseList(val x: Int, val y: Long, val color: Int)

// timerMax: おじゃま以外が消えるまでの時間
// timerMaxGarbage: おじゃまが消えるまでの時間
data class EraseState(val eraseList: MutableSet<EraseList>, val eraseListGarbage: MutableSet<EraseList>, val chain: Boolean, val timerMax: Int, val timerMaxGarbage: Int) {
  var timer = 0
}

