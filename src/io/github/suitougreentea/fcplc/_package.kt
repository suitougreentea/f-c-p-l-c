package io.github.suitougreentea.fcplc

import org.newdawn.slick.AppGameContainer

fun main(args: Array<String>) {
    val app = AppGameContainer(Game("Fully Customizable Puzzle League Clone"))
    app.setDisplayMode(800, 600, false)
    app.setTargetFrameRate(60)
    app.setShowFPS(false)
    app.start()
}