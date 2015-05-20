package io.github.suitougreentea.fcplc

public class Garbage(val id: Int, val width: Int, val height: Int, val blockList: Array<MutableList<BlockGarbage>>) {
  val lowestBlock: Array<BlockGarbage?> = Array(blockList.size(), { i -> blockList[i].minBy { e -> e.y } })
  // 左下隅のブロックをベースとする
  val baseBlock: BlockGarbage
  val baseBlockX: Int
  init {
    var ix = 0
    while(lowestBlock[ix] == null) ix++
    baseBlock = lowestBlock[ix]!!
    baseBlockX = ix
  }

  var doneCurrentFrameActivation = false
  var doneCurrentFrameDrop = false
  var erasing = false
  //var doneErasing = false
}