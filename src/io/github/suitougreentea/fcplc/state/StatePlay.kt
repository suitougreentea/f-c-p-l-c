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

class StatePlay(val id: Int, val resource: Res) : BasicGameState() {
  var player1: Player? = null
  var player2: Player? = null

  val controller1 = Controller(mapOf(
          Controller.Button.LEFT  to arrayOf<InputInfo>(InputInfoKeyboard(Input.KEY_LEFT)),
          Controller.Button.RIGHT to arrayOf<InputInfo>(InputInfoKeyboard(Input.KEY_RIGHT)),
          Controller.Button.UP    to arrayOf<InputInfo>(InputInfoKeyboard(Input.KEY_UP)),
          Controller.Button.DOWN  to arrayOf<InputInfo>(InputInfoKeyboard(Input.KEY_DOWN)),
          Controller.Button.FLIP  to arrayOf<InputInfo>(InputInfoKeyboard(Input.KEY_SPACE)),
          Controller.Button.RISE  to arrayOf<InputInfo>(InputInfoKeyboard(Input.KEY_ENTER))
  ))
  val controller2 = Controller(mapOf(
          Controller.Button.LEFT  to arrayOf<InputInfo>(InputInfoPadAxis(0, 0, { it < -0.9f })),
          Controller.Button.RIGHT to arrayOf<InputInfo>(InputInfoPadAxis(0, 0, { it > 0.9f })),
          Controller.Button.UP    to arrayOf<InputInfo>(InputInfoPadAxis(0, 1, { it < -0.9f })),
          Controller.Button.DOWN  to arrayOf<InputInfo>(InputInfoPadAxis(0, 1, { it > 0.9f })),
          Controller.Button.FLIP  to arrayOf<InputInfo>(InputInfoPadButton(0, 1)),
          Controller.Button.RISE  to arrayOf<InputInfo>(InputInfoPadButton(0, 7))
  ))

  override fun init(container: GameContainer, game: StateBasedGame) {
  }

  override fun enter(container: GameContainer, game: StateBasedGame) {
    player1 = Player(resource, 0, 0, 2)
    player2 = Player(resource, 0, 1, 2)
  }

  override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
    val input = container.getInput()
    if (input.isKeyPressed(Input.KEY_ESCAPE)) game.enterState(Game.States.DEBUG_MENU)
    controller1.update(input)
    controller2.update(input)
    player1?.update(controller1)
    player2?.update(controller2)
  }


  override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
    resource.getImage(Res.Img.background).draw()
    player1?.render(g)
    player2?.render(g)
  }

  override fun getID() = id
}