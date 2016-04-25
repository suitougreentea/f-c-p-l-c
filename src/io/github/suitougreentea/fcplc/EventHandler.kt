package io.github.suitougreentea.fcplc

public interface EventHandler {
  fun erase(logic: GameLogic, chain: Boolean, eraseList: Set<EraseList>, eraseListGarbage: Set<EraseList>)
  fun swap(logic: GameLogic, x: Int, y: Long, left: Block?, right: Block?)
}
