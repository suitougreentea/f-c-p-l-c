package io.github.suitougreentea.fcplc

import org.lwjgl.input.Controllers
import org.newdawn.slick.ControllerListener
import org.newdawn.slick.Input
import java.util.*
import java.util.function.Function

class Controller(val mapping: Map<Button, Array<InputInfo>>) {
  enum class Button {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    FLIP,
    RISE,
  }

  val state: MutableMap<Button, Int> = HashMap()
  init {
    Button.values().forEach { state[it] = 0 }
  }

  fun update(input: Input) {
    mapping.forEach {
      val down = it.value.any { it.isDown(input) }
      state[it.key] = when(state[it.key]) {
        0 -> if(down) 1 else 0
        1 -> if(down) 2 else 3
        2 -> if(down) 2 else 3
        3 -> if(down) 1 else 0
        else -> throw IllegalStateException()
      }
    }
  }

  fun isPressed(button: Button) = state[button] == 1
  fun isDown(button: Button) = state[button] == 1 || state[button] == 2
  fun isReleased(button: Button) = state[button] == 3
}

interface InputInfo {
  fun isDown(input: Input): Boolean
}

class InputInfoKeyboard(val keyCode: Int): InputInfo {
  override fun isDown(input: Input): Boolean {
    return input.isKeyDown(keyCode)
  }
}

class InputInfoPadButton(val controllerId: Int, val code: Int): InputInfo {
  override fun isDown(input: Input): Boolean {
    return input.isButtonPressed(code, controllerId)
  }
}

class InputInfoPadAxis(val controllerId: Int, val axisId: Int, val biasFunction: (Float) -> Boolean): InputInfo {
  override fun isDown(input: Input): Boolean {
    return biasFunction(input.getAxisValue(controllerId, axisId))
  }

}

