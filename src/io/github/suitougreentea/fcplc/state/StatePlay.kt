package io.github.suitougreentea.fcplc.state

import io.github.suitougreentea.fcplc.*
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.util.ArrayList
import java.util.HashSet
import io.github.suitougreentea.fcplc.SystemResource as Res

class StatePlay(val id: Int, val resource: SystemResource) : BasicGameState() {
  var player1: Player? = null
  override fun init(container: GameContainer, game: StateBasedGame) {
  }

  override fun enter(container: GameContainer, game: StateBasedGame) {
    player1 = Player(resource)
  }

  override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
    val input = container.getInput()
    if (input.isKeyPressed(Input.KEY_ESCAPE)) game.enterState(Game.States.DEBUG_MENU)
    player1?.update(input)
  }


  override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
    player1?.render(g)
  }

  override fun getID() = id
}