package io.github.suitougreentea.fcplc

import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

class Player(val resource: SystemResource, mode: Int, player: Int, maxPlayer: Int): EventHandler {
  val logic: GameLogic = GameLogic(6, 12, 5, this)
  val renderer = Renderer(resource, mode, player, maxPlayer)

  fun update(controller: Controller) {
    renderer.increaseTimer()
    logic.update(controller)
  }

  fun render(g: Graphics) {
    renderer.render(g, logic)

  }

  override fun erase(logic: GameLogic, chain: Boolean, eraseList: Set<EraseList>, eraseListGarbage: Set<EraseList>) {
    renderer.erase(logic, chain, eraseList, eraseListGarbage)
  }

  override fun swap(logic: GameLogic, x: Int, y: Long, left: Block?, right: Block?) {
    renderer.swap(logic, x, y, left, right)
  }

  override fun gameOver(logic: GameLogic) {
    println("gameover")
  }
}

