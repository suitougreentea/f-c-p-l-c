package io.github.suitougreentea.fcplc

import io.github.suitougreentea.util.AngelCodeFontXML
import org.newdawn.slick.AngelCodeFont
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import java.util.ArrayList
import java.util.Comparator
import io.github.suitougreentea.fcplc.SystemResource as Res

class Renderer(val res: Res, val mode: Int, val player: Int, val maxPlayer: Int): EventHandler {
  val debug = false
  val pinchAnimation = arrayOf(4, 0, 0, 2, 2, 3, 3, 2, 2, 0, 0, 4)
  val landedAnimation = arrayOf(4, 4, 0, 2, 3, 2)
  val jpFont = res.getFont(Res.Fnt.jp)
  val eraseStateColor = arrayOf(Color.red, Color.green, Color.blue, Color.yellow, Color.cyan, Color.magenta)

  var timer = 0
  fun increaseTimer(){
    timer++
    val spriteEraseListIter = spriteEraseList.iterator()
    while(spriteEraseListIter.hasNext()) {
      val e = spriteEraseListIter.next()
      e.increaseTimer()
      if(e.disposable) spriteEraseListIter.remove()
    }

    val spriteChainComboSmallListIter = spriteChainComboSmallList.iterator()
    while(spriteChainComboSmallListIter.hasNext()) {
      val e = spriteChainComboSmallListIter.next()
      e.increaseTimer()
      if(e.disposable) spriteChainComboSmallListIter.remove()
    }

    if(spriteSwap != null) {
      spriteSwap!!.increaseTimer()
      if (spriteSwap!!.disposable) spriteSwap = null
    }
  }

  val spriteEraseList: MutableList<Sprite> = ArrayList()
  val spriteChainComboSmallList: MutableList<Sprite> = ArrayList()
  var spriteSwap: SpriteSwap? = null

  fun render(g: Graphics, logic: GameLogic) {
    val pinchTimer = logic.pinchTimer / logic.pinchTimerMax.toFloat()

    g.pushTransform()

    if(maxPlayer == 1) g.translate(280f, 92f)
    if(maxPlayer == 2) g.translate(60f + player * 400f, 92f)

    g.setColor(Color(0f, 0f, 0f, 0.6f))
    g.fillRect(0f, 0f, logic.width * 40f, logic.height * 40f)
    g.setWorldClip(0f, 0f, logic.width * 40f, logic.height * 40f)
    for(ix in logic.field.indices) {
      val bounce = if(logic.columnState[ix] == 1) ((pinchTimer * 2 - 1) * (pinchTimer * 2 - 1) - 1f) * 2f else 0f
      g.pushTransform()
      g.translate(0f, bounce)
      val col = logic.field[ix]
      for(b in col) {
        if(b.color > 0) {
          val dy = getFieldY(b.y, logic.height, logic.lowestY, 40)
          val state = if(logic.gameOver) 7 else when(logic.columnState[ix]) {
            0 -> if(b.landedTimer > 0) {
              landedAnimation[(((b.landedTimer - 1) / logic.landedTimerMax.toFloat()) * landedAnimation.size).toInt()]
            } else 0
            1 -> pinchAnimation[(pinchTimer * pinchAnimation.size).toInt()]
            2 -> 4
            else -> throw IllegalStateException()
          }
          val brightness = if(logic.gameOver) 1f - logic.gameOverTimer / logic.gameOverTimerMax.toFloat() else 1f
          drawBlock(g, ix * 40, dy, 40, b.color, state, brightness)
        }
      }
      g.popTransform()
      val dy = getFieldY(logic.next[ix].y, logic.height, logic.lowestY, 40)
      drawBlock(g, ix * 40, dy, 40, logic.next[ix].color, 1)
    }
    for (e in logic.garbageList) {
      val dy = getFieldY(e.baseBlock.y + (e.height - 1) * 1000L, logic.height, logic.lowestY, 40)
      drawGarbage(g, e.baseBlockX * 40, dy, e.width, e.height, 40)
    }
    spriteSwap?.render(this, g, logic)
    for(e in spriteEraseList) {
      e.render(this, g, logic)
    }

    g.clearWorldClip()

    if(debug) {
      logic.field.forEachIndexed { ix, col ->
        col.forEachIndexed { iy, b ->
          val dy = getFieldY(b.y, logic.height, logic.lowestY, 40)
          jpFont.drawString(b.stayTimer.toString(), ix * 40, dy + 20)
          jpFont.drawString(b.landedTimer.toString(), ix * 40 + 20, dy + 20)
          if (b.active) jpFont.drawString("A", ix * 40, dy, color = Color(1f, 0f, 0f))
          if (b.chain) jpFont.drawString("C", ix * 40 + 20, dy, color = Color(1f, 1f, 0f))
          if (b.disabled) jpFont.drawString("D", ix * 40 + 30, dy, color = Color(0.5f, 0.5f, 0.5f))
        }
      }
      logic.columnState.forEachIndexed { i, p ->
        jpFont.drawString(p.toString(), i * 40, -20)
      }
      jpFont.drawString("""
      |floorY: ${logic.floorY}
      |lowestY: ${logic.lowestY}
      |nextManualRiseTarget: ${logic.nextManualRiseTarget}
      |chain: ${logic.chain}
      |lastGarbageId: ${logic.lastGarbageId}
      |gameOver: ${logic.gameOver}
      |horizontalMoveTimer: ${logic.horizontalMoveTimer}
      |verticalMoveTimer: ${logic.verticalMoveTimer}
      |swapTimer: ${logic.swapTimer}
      |pinchTimer: ${logic.pinchTimer}
      |stopTimer: ${logic.stopTimer.toString()}
      |garbageStopTimer: ${logic.garbageStopTimer.toString()}
      """.trimMargin(), 240, 0)
      g.lineWidth = 2f
      logic.eraseState.forEachIndexed { i, e ->
        val color = eraseStateColor[i % eraseStateColor.size]
        g.color = color
        (e.eraseList + e.eraseListGarbage).forEach {
          val dy = getFieldY(it.y, logic.height, logic.lowestY, 40)
          g.drawRect(it.x * 40f, dy.toFloat(), 40f, 40f)
        }
        jpFont.drawString("c: ${e.chain}, t: ${e.timer}, max: ${e.timerMax}, gmax: ${e.timerMaxGarbage}", 240, 220 + i * 16, color = color)
      }
      logic.garbageList.forEachIndexed { i, e ->

      }
      g.lineWidth = 1f
      jpFont.drawString(logic.garbageQueue.map {
        "${it.chainSize}: " + it.other.joinToString(", ") + if(it.done) "!" else ""
      }.joinToString("\n"), 240, 300)
    }

    for(e in spriteChainComboSmallList) {
      e.render(this, g, logic)
    }
    if(!logic.gameOver) drawCursor(g, logic.cursorX * 40, getFieldY(logic.cursorY * 1000L, logic.height, logic.lowestY, 40), 40)
    g.popTransform()
  }

  override fun erase(logic: GameLogic, chain: Boolean, eraseList: Set<EraseList>, eraseListGarbage: Set<EraseList>) {
    val order = eraseList.sortedWith(Comparator { a, b -> if(a.y < b.y) 1 else if (a.y == b.y && a.x > b.x) 1 else -1 })
            .plus(eraseListGarbage.sortedWith(Comparator { a, b -> if(a.y > b.y) 1 else if (a.y == b.y && a.x > b.x) 1 else -1 }))
    spriteEraseList.add(SpriteEraseBlock(order, logic.initEraseTime, logic.eraseTimePerBlock, 40))

    val viewChain = if(chain && logic.chain >= 2) logic.chain else null
    val viewCombo = if(eraseList.size >= 4) eraseList.size else null
    if(viewChain != null || viewCombo != null) {
      val topLeft = order.get(0)
      val stop = logic.columnState.any { it >= 1 }
      spriteChainComboSmallList.add(SpriteChainComboSmall(this, topLeft.x, topLeft.y, viewChain, viewCombo, stop, 40, 60))
    }
  }

  override fun swap(logic: GameLogic, x: Int, y: Long, left: Block?, right: Block?) {
    spriteSwap = SpriteSwap(x, y, left, right, 40, logic.swapTimerMax)
  }

  override fun gameOver(logic: GameLogic) {}
  override fun endChain(chain: Int) {}
  override fun attack(chain: Int, combo: Int) {}

  // フィールド左上隅からのY座標を取得
  fun getFieldY(y: Long, fieldHeight: Int, lowestY: Long, size: Int): Int {
    return ((fieldHeight - ((y - lowestY) / 1000f) - 1) * size).toInt()
  }

  // 0 .. 通常
  // 1 .. せり上がり予備
  // 2-4 .. ピンチ
  // 5-6 .. 消去
  // 7-8 .. ゲームオーバー
  fun drawBlock(g: Graphics, dx: Int, dy: Int, size: Int, color: Int, state: Int = 0, brightness: Float = 1f) {
    if(size != 40) throw UnsupportedOperationException()
    val iBlock = res.getImage(Res.Img.block40)
    if(0 < color && color < 256) {
      val srcX = (color - 1) * 42 + 1f
      val srcY = state * 42f + 1f
      iBlock.draw(dx.toFloat(), dy.toFloat(), dx + 40f, dy + 40f, srcX, srcY, srcX + 40f, srcY + 40f, Color(brightness, brightness, brightness))
    }
  }

  fun drawSpriteGarbage(g: Graphics, dx: Int, dy: Int, size: Int, color: Int, state: Int = 0) {
    val iBlock = res.getImage(Res.Img.garbage40)
    val srcX = if(state == 5) 43f else if (state == 6) 85f else throw UnsupportedOperationException()
    val srcY = 169f
    iBlock.draw(dx.toFloat(), dy.toFloat(), dx + 40f, dy + 40f, srcX, srcY, srcX + 40f, srcY + 40f)
  }

  // 左上隅をdx, dyとする
  fun drawGarbage(g: Graphics, dx: Int, dy: Int, bWidth: Int, bHeight: Int, size: Int) {
    if(size != 40) throw UnsupportedOperationException()
    val iBlock = res.getImage(Res.Img.garbage40)
    for(iy in 0 .. bHeight - 1) {
      for (ix in 0..bWidth - 1) {
        val srcXTile = when(ix) {
          0 -> 0
          bWidth - 1 -> 2
          else -> 1
        }
        val srcYTile = if(bHeight == 1) 0 else when (iy) {
          0 -> 1
          bHeight - 1 -> 3
          else -> 2
        }
        val srcX = srcXTile * 42 + 1f
        val srcY = srcYTile * 42 + 1f
        iBlock.draw((dx + ix * 40).toFloat(), (dy + iy * 40).toFloat(), (dx + ix * 40) + 40f, (dy + iy * 40) + 40f, srcX.toFloat(), srcY, srcX + 40f, srcY + 40f)
      }
    }
  }

  fun drawEraseBlur(g: Graphics, dx: Int, dy: Int, size: Int, color: Int, alpha: Float) {
    val srcX: Float
    val srcY: Float
    val iBlur = res.getImage(Res.Img.eraseBlur40)
    if(0 < color && color < 256) {
      srcX = (color - 1) * 80 + 26f
      srcY = 26f
    } else if (color == 256) {
      srcX = 26f
      srcY = 106f
    } else {
      throw UnsupportedOperationException()
    }
    iBlur.draw(dx - 14f, dy - 14f, dx + 54f, dy + 54f, srcX, srcY, srcX + 68f, srcY + 68f, Color(1f, 1f, 1f, alpha))
  }

  fun drawEraseEffect(g: Graphics, dx: Int, dy: Int, size: Int, phase: Int) {
    val srcX = phase * 120 + 1f
    val srcY = 1f
    res.getImage(Res.Img.eraseEffect40).draw(dx - 39f, dy - 39f, dx + 79f, dy + 79f, srcX, srcY, srcX + 118f, srcY + 118f)
  }

  fun drawCursor(g: Graphics, dx: Int, dy: Int, size: Int) {
    val a = (timer % 60) / 30
    res.getImage(Res.Img.cursor).draw(dx - 8f, dy - 8f, dx - 8f + 96f, dy - 8f + 56f, a * 97f, 0f, a * 97f + 96f, 56f)
  }
}
