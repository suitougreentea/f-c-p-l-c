package io.github.suitougreentea.fcplc.state

import io.github.suitougreentea.fcplc.Game
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

class StateDebugMenu(val id: Int): BasicGameState() {
  var cursor = 0
  val items = arrayOf("Play", "Render")

  override fun init(container: GameContainer, game: StateBasedGame) {
  }

  override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
    val input = container.getInput()
    if(input.isKeyPressed(Input.KEY_UP)) cursor = (cursor + items.size - 1) % items.size
    if(input.isKeyPressed(Input.KEY_DOWN)) cursor = (cursor + 1) % items.size
    if(input.isKeyPressed(Input.KEY_ENTER)) {
      game.enterState(when(cursor){
        0 -> Game.States.PLAY
        1 -> Game.States.TEST_RENDER
        else -> throw IllegalStateException()
      })
    }
  }

  override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
    g.setColor(Color.white)
    g.drawString("*** DEBUG MENU ***", 0f, 0f)
    for(i in items.indices) {
      var s = items[i]
      g.setColor(if(i == cursor) Color.red else Color.white)
      g.drawString(s, 16f, 32f + i * 16f)
    }
  }

  override fun getID() = id
}
