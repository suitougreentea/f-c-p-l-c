package io.github.suitougreentea.fcplc.state

import io.github.suitougreentea.fcplc.SystemResource
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

class StateLoading(val id: Int, val res: SystemResource): BasicGameState() {
    override fun init(container: GameContainer, game: StateBasedGame) {
        res.loadImages()
        res.loadFonts()
    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
    }

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
    }

    override fun getID() = id
}
