package io.github.suitougreentea.fcplc

import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import io.github.suitougreentea.fcplc.SystemResource as Res

interface Sprite {
  var timer: Int
  fun increaseTimer(){
    timer++
  }

  fun render(r: Renderer, g: Graphics, logic: GameLogic): Unit

  val disposable: Boolean
}

// 描画はフィールド左上隅基準
class SpriteEraseBlock(val order: List<EraseList>, val initEraseSpeed: Int, val eraseSpeedPerBlock: Int, val size: Int): Sprite {
  override var timer = 1
  val timerMax = initEraseSpeed + order.size * eraseSpeedPerBlock

  override fun render(r: Renderer, g: Graphics, logic: GameLogic) {
    // ブラーと点滅
    for(i in order.indices) {
      val e = order.get(i)
      val dy = r.getFieldY(e.y, logic.height, logic.lowestY, size)
      r.drawEraseBlur(g, e.x * size, dy, size, e.color, getBlurAlpha(timer, initEraseSpeed))
      val state = 5 + timer % 4 / 2

      if(timer < initEraseSpeed) {
        if (0 < e.color && e.color < 256) {
          r.drawBlock(g, e.x * size, dy, size, e.color, state)
        } else {
          r.drawSpriteGarbage(g, e.x * size, dy, size, e.color, state)
        }
      }
    }

    // 消えるやつ
    for(i in order.indices) {
      val e = order.get(i)
      val dy = r.getFieldY(e.y, logic.height, logic.lowestY, size)
      if(timer >= initEraseSpeed) {
        val eff = getEraseEffect(timer, i, initEraseSpeed, eraseSpeedPerBlock)
        if (eff == -1) {
          if (0 < e.color && e.color < 256) {
            r.drawBlock(g, e.x * size, dy, size, e.color, 6)
          } else {
            r.drawSpriteGarbage(g, e.x * size, dy, size, e.color, 5)
          }
        } else if (eff < 5) {
          r.drawEraseEffect(g, e.x * size, dy, size, eff)
        }
      }
    }
  }

  // 4次関数
  fun getBlurAlpha(timer: Int, initEraseSpeed: Int) = when (timer) {
    in 0..initEraseSpeed -> (-Math.pow((((timer * 2) / initEraseSpeed.toFloat()) - 1).toDouble(), 4.toDouble()) + 1).toFloat()
    else -> 0f
  }

  fun getEraseEffect(timer: Int, order: Int, initEraseSpeed: Int, eraseSpeedPerBlock: Int): Int {
    val offset = timer - initEraseSpeed
    if(offset / eraseSpeedPerBlock == order) {
      return (((offset % eraseSpeedPerBlock) / eraseSpeedPerBlock.toFloat()) * 5).toInt()
    } else if(offset / eraseSpeedPerBlock > order) {
      return 5
    } else {
      return -1
    }
  }

  override val disposable: Boolean
  get() { return timer >= timerMax }
}

class SpriteSwap(val x: Int, val y: Long, val left: Block?, val right: Block?, val size: Int, val timerMax: Int): Sprite {
  override var timer = 1

  override fun render(r: Renderer, g: Graphics, logic: GameLogic) {
    if(left != null && right != null) {
      val ldx = ((x + (timer / timerMax.toFloat())) * size).toInt()
      val ldy = r.getFieldY(y, logic.height, logic.lowestY, size)
      val rdx = ((x + 1 - (timer / timerMax.toFloat())) * size).toInt()
      val rdy = r.getFieldY(y, logic.height, logic.lowestY, size)
      r.drawBlock(g, ldx, ldy, size, left.color, 0)
      r.drawBlock(g, rdx, rdy, size, right.color, 0)
    }else if(left != null && right == null) {
      val dx = ((x + (timer / timerMax.toFloat())) * size).toInt()
      val dy = r.getFieldY(y, logic.height, logic.lowestY, size)
      r.drawBlock(g, dx, dy, size, left.color, 0)
    }else if(left == null && right != null) {
      val dx = ((x + 1 - (timer / timerMax.toFloat())) * size).toInt()
      val dy = r.getFieldY(y, logic.height, logic.lowestY, size)
      r.drawBlock(g, dx, dy, size, right.color, 0)
    } else throw IllegalStateException()
  }

  override val disposable: Boolean
    get() { return timer >= timerMax }
}

class SpriteChainComboSmall(val renderer: Renderer, val x: Int, val y: Long, val chain: Int?, val combo: Int?, val size: Int, val timerMax: Int): Sprite {
  override var timer = 1
  val chainColor = arrayOf(Color(0f, 0.75f, 1f, 0.6f), Color(0f, 0.75f, 1f, 0.2f))
  val comboColor = arrayOf(Color(1f, 0.7f, 0f, 0.6f), Color(1f, 0.7f, 0f, 0.2f))
  val foregroundColor = arrayOf(Color(1f, 1f, 1f, 1f), Color(1f, 1f, 1f, 0.7f))

  override fun render(r: Renderer, g: Graphics, logic: GameLogic) {
    val ic = renderer.res.getImage(Res.Img.chain)
    val dx = x * size
    val chaindy = r.getFieldY(y, logic.height, logic.lowestY, size)
    val combody = r.getFieldY(y, logic.height, logic.lowestY, size) + if(chain != null) 24 else 0
    if(chain != null) {
      ic.draw(dx.toFloat(), chaindy.toFloat(), dx + 14f, chaindy + 24f, 985f, 920f, 985f + 14f, 920f + 24f, chainColor[timer % 2])
      ic.draw(dx.toFloat(), chaindy.toFloat(), dx + 14f, chaindy + 24f, 985f, 408f, 985f + 14f, 408f + 24f, foregroundColor[timer % 2])
      val chainStr = chain.toString()
      for (i in chainStr.indices) {
        val c = chainStr[i].toInt()
        drawNum(dx + (i + 1) * 14, chaindy, c - 48, true, chainColor[timer % 2])
        drawNum(dx + (i + 1) * 14, chaindy, c - 48, false, foregroundColor[timer % 2])
      }
    }
    if(combo != null) {
      ic.draw(dx.toFloat(), combody.toFloat(), dx + 14f, combody + 24f, 969f, 920f, 969f + 14f, 920f + 24f, comboColor[timer % 2])
      ic.draw(dx.toFloat(), combody.toFloat(), dx + 14f, combody + 24f, 969f, 408f, 969f + 14f, 408f + 24f, foregroundColor[timer % 2])
      val comboStr = combo.toString()
      for (i in comboStr.indices) {
        val c = comboStr[i].toInt()
        drawNum(dx + (i + 1) * 14, combody, 48, true, comboColor[timer % 2])
        drawNum(dx + (i + 1) * 14, combody, c - 48, false, foregroundColor[timer % 2])
      }
    }
  }

  fun drawNum(dx: Int, dy: Int, num: Int, underlay: Boolean, color: Color) {
    val sx = num * 16 + 809f
    val sy = 408f + if(underlay) 512f else 0f
    renderer.res.getImage(Res.Img.chain).draw(dx.toFloat(), dy.toFloat(), dx + 14f, dy + 24f, sx, sy, sx + 14f, sy + 24f, color)
  }

  override val disposable: Boolean
    get() { return timer >= timerMax }
}
