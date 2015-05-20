package io.github.suitougreentea.fcplc

class FieldRandomizer(val color: Int, val width: Int) {
  init {
    if(color <= 2) throw UnsupportedOperationException()
  }

  var firstCache: Array<Int>
  var secondCache: Array<Int>
  init {
    firstCache = Array(width, {0})
    secondCache = Array(width, {0})
    for (i in 0..width - 1) {
      if(i >= 2) {
        do {
          firstCache[i] = (Math.random() * color).toInt() + 1
        } while (firstCache[i] == firstCache[i - 1] && firstCache[i] == firstCache[i - 2])
        do {
          secondCache[i] = (Math.random() * color).toInt() + 1
        } while (secondCache[i] == secondCache[i - 1] && secondCache[i] == secondCache[i - 2])
      } else {
        firstCache[i] = (Math.random() * color).toInt() + 1
        secondCache[i] = (Math.random() * color).toInt() + 1
      }
    }
  }

  fun next(): Array<Int> {
    var nextSecondCache = Array(width, {0})
    for (i in 0..width - 1) {
      if(i >= 2) {
        do {
          nextSecondCache[i] = (Math.random() * color).toInt() + 1
        } while ((nextSecondCache[i] == nextSecondCache[i - 1] && nextSecondCache[i] == nextSecondCache[i - 2]) || (nextSecondCache[i] == firstCache[i] && nextSecondCache[i] == secondCache[i]))
      } else {
        do {
          nextSecondCache[i] = (Math.random() * color).toInt() + 1
        } while (nextSecondCache[i] == firstCache[i] && nextSecondCache[i] == secondCache[i])
      }
    }

    val result = firstCache
    firstCache = secondCache
    secondCache = nextSecondCache
    return result
  }
}
