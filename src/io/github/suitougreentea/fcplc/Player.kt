package io.github.suitougreentea.fcplc

import io.github.suitougreentea.fcplc.state.PlayerWrapper
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import java.util.*

class Player(val wrapper: PlayerWrapper, val resource: SystemResource, mode: Int, player: Int, maxPlayer: Int): EventHandler {
  val logic: GameLogic = GameLogic(6, 12, 5, this)
  val renderer = Renderer(resource, mode, player, maxPlayer)

  fun update(controller: Controller) {
    renderer.increaseTimer()
    logic.update(controller)
  }

  fun render(g: Graphics) {
    renderer.render(g, logic)
  }

  fun attackChain(chain: Int) {
    val last = logic.garbageQueue.lastOrNull()
    if(last != null && !last.done) last.chainSize = chain - 1
    else logic.garbageQueue.add(GarbageQueue(1, chain - 1, ArrayList()))
  }

  fun attackCombo(combo: Int) {
    val last = logic.garbageQueue.lastOrNull()
    val size = when(combo) {
      4 -> arrayOf(3)
      5 -> arrayOf(4)
      6 -> arrayOf(5)
      7 -> arrayOf(6)
      8 -> arrayOf(4, 3)
      9 -> arrayOf(4, 4)
      10 -> arrayOf(5, 5)
      11 -> arrayOf(6, 5)
      12 -> arrayOf(6, 6)
      in 13..Int.MAX_VALUE -> Array(combo - 10, { 6 })
      else -> throw IllegalStateException()
    }
    if(last != null && !last.done) last.other.addAll(size)
    else {
      val e = GarbageQueue(1, 0, size.toMutableList())
      e.done = true
      logic.garbageQueue.add(e)
    }
  }

  fun breakAttack() {
    logic.garbageQueue.forEach { it.done = true }
  }

  override fun erase(logic: GameLogic, chain: Boolean, eraseList: Set<EraseList>, eraseListGarbage: Set<EraseList>) {
    renderer.erase(logic, chain, eraseList, eraseListGarbage)
    wrapper.erase(logic, chain, eraseList, eraseListGarbage)
  }

  override fun swap(logic: GameLogic, x: Int, y: Long, left: Block?, right: Block?) {
    renderer.swap(logic, x, y, left, right)
    wrapper.swap(logic, x, y, left, right)
  }

  override fun gameOver(logic: GameLogic) {
    wrapper.gameOver(logic)
  }

  override fun endChain(chain: Int) {
    wrapper.endChain(chain)
  }

  override fun attack(chain: Int, combo: Int) {
    wrapper.attack(chain, combo)
  }
}

