package io.github.suitougreentea.fcplc

import java.util.*

open class GarbageQueue(val color: Int, var chainSize: Int, var other: MutableList<Int>) {
  var done = false
  var timer = 0
}
