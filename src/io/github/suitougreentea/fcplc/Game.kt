package io.github.suitougreentea.fcplc

import io.github.suitougreentea.fcplc.state.StateDebugMenu
import io.github.suitougreentea.fcplc.state.StateLoading
import io.github.suitougreentea.fcplc.state.StatePlay
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import io.github.suitougreentea.fcplc.SystemResource as Res

class Game(val name: String): StateBasedGame(name) {
    object States {
        val LOADING = -1
        val DEBUG_MENU = 0
        val PLAY = 1
        val TEST_RENDER = 2
    }

    val resource = Res()

    override fun initStatesList(container: GameContainer) {
      container.input.initControllers()
        this.addState(StateLoading(States.LOADING, resource))
        this.addState(StateDebugMenu(States.DEBUG_MENU))
        this.addState(StatePlay(States.PLAY, resource))
    }

    override fun postUpdateState(container: GameContainer, delta: Int) {
      container.input.clearKeyPressedRecord()
      container.input.clearControlPressedRecord()
      container.input.clearMousePressedRecord()
    }
    override fun postRenderState(container: GameContainer, g: Graphics) {
        val fps = container.getFPS()
        resource.getFont(Res.Fnt.jp).drawString("FCPLC dev / $fps FPS", 0, 580, color = Color.white)
    }
}

