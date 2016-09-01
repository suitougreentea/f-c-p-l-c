package io.github.suitougreentea.fcplc

import org.newdawn.slick.Input
import java.util.ArrayList
import java.util.HashSet

/*
 * TODO: == の === への書き換え(== baseBlockなど)
 */
class GameLogic(val width: Int, val height: Int, val numColor: Int, val event: EventHandler) {
  val fieldRandomizer = FieldRandomizer(numColor, width)
  
  var field: Array<MutableList<Block>> = Array(width, { ArrayList<Block>() })
  init {
    for (iy in 0..3) {
      val next = fieldRandomizer.next()
      for (ix in 0 .. width - 1) {
        field[ix].add(Block(iy.toLong() * 1000, next[ix]))
      }
    }
  }
  // 0: 通常, 1: ピンチ, 2: 天井
  var columnState: List<Int> = field.map { 0 }
  var pinchTimer = 0
  var pinchTimerMax = 20

  var garbageList: MutableList<Garbage> = ArrayList()
  var lastGarbageId = 0

  var cursorX = 0
  var cursorY = 0

  var swapTimer = 0
  var swapTimerMax = 4

  var swapLeftBlock: Block? = null
  var swapRightBlock: Block? = null
  var swapX: Int? = null
  var swapY: Int? = null

  var floorY = 0L
  var lowestY = 0L

  var stayTimerMax = 15
  //var stayTimerMax = 5

  var landedTimerMax = 6

  var chain = 0

  var horizontalMoveTimer = 0
  var verticalMoveTimer = 0
  var moveTimerMax = 10

  var next: Array<Block>
  init {
    val nextNext = fieldRandomizer.next()
    next = Array(width, { i -> Block(-1000L, nextNext[i]) })
  }

  var eraseState: MutableList<EraseState> = ArrayList()

  var internalSpeed = 1
  var manualRiseSpeed = 1000 / 15

  var initEraseTime = 80
  var eraseTimePerBlock = 15

  //var initEraseTime = 40
  //var eraseTimePerBlock = 5

  var garbageAfterSpeed = 40
  //var garbageAfterSpeed = 0

  //var initVelocity = 0
  //var acceralation = -30
  //var maxVelocity = -300
  var initVelocity = -1000
  var acceralation = 0
  var maxVelocity = -1000

  var nextManualRiseTarget: Long? = null

  var gameOver = false
  var gameOverTimer = 0
  var gameOverTimerMax = 60
  var stopTimer = 0

  fun update(controller: Controller) {
    for(e in garbageList) {
      e.doneCurrentFrameActivation = false
      e.doneCurrentFrameDrop = false
    }

    if(gameOver) {
      if(gameOverTimer == gameOverTimerMax) {

      } else {
        gameOverTimer ++
      }
      return
    }

    if(judgeGameOver()) {
      return
    }

    if (controller.isPressed(Controller.Button.LEFT))  horizontalMovePressed(-1)
    if (controller.isDown   (Controller.Button.LEFT))  horizontalMoveDown(-1)
    if (controller.isPressed(Controller.Button.RIGHT)) horizontalMovePressed(1)
    if (controller.isDown   (Controller.Button.RIGHT)) horizontalMoveDown(1)
    if (controller.isPressed(Controller.Button.DOWN))  verticalMovePressed(-1)
    if (controller.isDown   (Controller.Button.DOWN))  verticalMoveDown(-1)
    if (controller.isPressed(Controller.Button.UP))    verticalMovePressed(1)
    if (controller.isDown   (Controller.Button.UP))    verticalMoveDown(1)

    updateColumnState()
    updatePinchTimer()
    automaticRise()
    if (controller.isDown(Controller.Button.RISE)) manualRise()
    automaticManualRise()

    if (controller.isPressed(Controller.Button.FLIP)) swap()
    countSwap()

    updateLandedTimer()
    activateBlock()
    dropBlock()
    eraseBlock()

    remainChain()

    countErase()
  }

  fun horizontalMovePressed(direction: Int) {
    horizontalMoveTimer = direction
  }

  fun verticalMovePressed(direction: Int) {
    verticalMoveTimer = direction
  }

  fun horizontalMoveDown(direction: Int) {
    val newCursorX = cursorX + direction
    val valid = horizontalMoveTimer * direction > 0
    if(Math.abs(horizontalMoveTimer) == 1 || (Math.abs(horizontalMoveTimer) >= moveTimerMax && valid))
      if(0 <= newCursorX && newCursorX <= width - 2) cursorX = newCursorX
    if(valid) horizontalMoveTimer += direction
  }

  fun verticalMoveDown(direction: Int) {
    val newCursorY = cursorY + direction
    val valid = verticalMoveTimer * direction > 0
    if(Math.abs(verticalMoveTimer) == 1 || (Math.abs(verticalMoveTimer) >= moveTimerMax && valid))
      if(floorY / 1000 <= newCursorY && newCursorY <= floorY / 1000 + height) cursorY = newCursorY
    if(valid) verticalMoveTimer += direction
  }


  // 現在のカーソル位置でswapを行う
  fun swap() {
    val leftBlock = field[cursorX].firstOrNull { e -> cursorY * 1000 <= e.y && e.y < cursorY * 1000 + 1000 }
    val rightBlock = field[cursorX + 1].firstOrNull { e -> cursorY * 1000 <= e.y && e.y < cursorY * 1000 + 1000 }

    // swap可能か判定
    fun isSwappable(): Boolean {
      val swapX = this.swapX
      val swapY = this.swapY

      // 空
      if (leftBlock == null && rightBlock == null) return false
      // 消去中のブロックがある
      if ((leftBlock != null && (-4 <= leftBlock.color && leftBlock.color <= -2)) || (rightBlock != null && (-4 <= rightBlock.color && rightBlock.color <= -2))) return false
      // おじゃまがある
      if ((leftBlock != null && leftBlock.color >= 256) || (rightBlock != null && rightBlock.color >= 256)) return false
      // どちらもswap中
      if (cursorX == swapX && cursorY == swapY) return false
      // どちらもactive
      if ((leftBlock != null && leftBlock.active) && (rightBlock != null && rightBlock.active)) return false
      // どちらかdisable
      if ((leftBlock != null && leftBlock.disabled) || (rightBlock != null && rightBlock.disabled)) return false
      // 滞空期間中のブロックはフリップできない
      if (leftBlock != null && 0 < leftBlock.stayTimer && leftBlock.stayTimer < stayTimerMax) return false
      if (rightBlock != null && 0 < rightBlock.stayTimer && rightBlock.stayTimer < stayTimerMax) return false
      // 滞空期間中のブロックの下部はフリップできない(上部のブロックが滞空中だったらfalse)
      if (field[cursorX].firstOrNull { e -> e.y == cursorY * 1000L + 1000 && 0 < e.stayTimer && e.stayTimer < stayTimerMax } != null) return false
      if (field[cursorX + 1].firstOrNull { e -> e.y == cursorY * 1000L + 1000 && 0 < e.stayTimer && e.stayTimer < stayTimerMax } != null) return false
      if(swapX != null && swapY != null) {
        // 片方nullのswapで、nullじゃない側(swap後にnullになる側)の上部に静止したブロックがある場合
        //  　[]        []
        // {  []}] -> -{--[]}  : Left == null, Right != null, カーソル == swap + 1 (カーソルを右に1つ移動したときのみ起こる), 上部ブロックあり
        if (swapLeftBlock == null && swapRightBlock != null && cursorX == swapX + 1 && field[cursorX].firstOrNull { e -> e.y == cursorY * 1000L + 1000 } != null) return false
        //   []           []
        // [{[]  } ->  {[]--}- : Left != null, Right == null, カーソル == swap - 1 (カーソルを左に1つ移動したときのみ起こる), 上部ブロックあり
        if (swapLeftBlock != null && swapRightBlock == null && cursorX == swapX - 1 && field[cursorX + 1].firstOrNull { e -> e.y == cursorY * 1000L + 1000 } != null) return false
        // // swap"後が"どちらもnullになる場合
        // [{  []} ->  []-{--  }
        // else if (swapLeftBlock == null && swapRightBlock != null && cursorX == swapX + 1 && rightBlock == null) return false
        // {[]  }] -> {  --}-[]
        // else if (swapLeftBlock != null && swapRightBlock == null && cursorX == swapX - 1 && leftBlock == null) return false
        // // inactiveかつ下に空洞(1フレームだけある、いいのかな？)
        // else if(left != null && !left.active && field[cursorX].firstOrNull {e -> e.y == left.y - 1000} == null) return false
        // else if(right != null && !right.active && field[cursorX + 1].firstOrNull {e -> e.y == right.y - 1000} == null) return false

        // swap後のブロックの下に空洞がある場合
        // {  []}    {  --}-
        //    []  ->      []
        if (swapLeftBlock == null && swapRightBlock != null && cursorX == swapX - 1 && field[cursorX + 1].firstOrNull { e -> e.y == cursorY * 1000L - 1000 } == null) return false
        // {[]  }    -{--  }
        //  []    -> []
        if (swapLeftBlock != null && swapRightBlock == null && cursorX == swapX + 1 && field[cursorX].firstOrNull { e -> e.y == cursorY * 1000L - 1000 } == null) return false
      }
      return true
    }

    if (isSwappable()) {
      if (swapTimer > 0) endSwap()
      // -1が実際のブロックに置き換わったので再度評価
      val newLeftBlock = field[cursorX].firstOrNull { e -> cursorY * 1000 <= e.y && e.y < cursorY * 1000 + 1000 }
      val newRightBlock = field[cursorX + 1].firstOrNull { e -> cursorY * 1000 <= e.y && e.y < cursorY * 1000 + 1000 }
      if (newLeftBlock == null && newRightBlock != null) {
        //右のみ
        field[cursorX + 1].remove(newRightBlock)
        field[cursorX].add(Block(cursorY.toLong() * 1000, -1))
        swapLeftBlock = null
        swapRightBlock = newRightBlock
      } else if (newLeftBlock != null && newRightBlock == null) {
        //左のみ
        field[cursorX].remove(newLeftBlock)
        field[cursorX + 1].add(Block(cursorY.toLong() * 1000, -1))
        swapLeftBlock = newLeftBlock
        swapRightBlock = null
      } else if (newLeftBlock != null && newRightBlock != null) {
        // 両方フロックがある
        field[cursorX].remove(newLeftBlock)
        field[cursorX + 1].remove(newRightBlock)
        field[cursorX].add(Block(cursorY.toLong() * 1000, -1))
        field[cursorX + 1].add(Block(cursorY.toLong() * 1000, -1))
        swapLeftBlock = newLeftBlock
        swapRightBlock = newRightBlock
      }
      swapX = cursorX
      swapY = cursorY
      swapTimer = 1
      event.swap(this, cursorX, cursorY * 1000L, swapLeftBlock, swapRightBlock)
    }
  }

  // swapのタイマーを加算させ、一定時間経ったらswapを完了させる
  fun countSwap() {
    if (swapTimer > 0) {
      if (swapTimer == swapTimerMax) {
        endSwap()
      } else {
        swapTimer++
      }
    }
  }

  // swapを完了させる
  fun endSwap() {
    val swapLeftBlock = this.swapLeftBlock
    val swapRightBlock = this.swapRightBlock
    val swapX = this.swapX
    val swapY = this.swapY

    if (swapX == null || swapY == null) throw IllegalStateException()

    if (swapRightBlock != null) {
      swapRightBlock.y = swapY * 1000L
      if(swapRightBlock.active) {
        val below = field[swapX].firstOrNull { e -> e.y == (swapY - 1) * 1000L}
        swapRightBlock.stayTimer = if(below != null) stayTimerMax else 1
        swapRightBlock.velocity = initVelocity
      }
      field[swapX].add(swapRightBlock)
      field[swapX].remove(field[swapX].first { e -> e.color == -1 })
    }
    if (swapLeftBlock != null) {
      swapLeftBlock.y = swapY * 1000L
      if(swapLeftBlock.active) {
        val below = field[swapX + 1].firstOrNull { e -> e.y == (swapY - 1) * 1000L}
        swapLeftBlock.stayTimer = if(below != null) stayTimerMax else 1
        swapLeftBlock.velocity = initVelocity
      }
      field[swapX + 1].add(swapLeftBlock)
      field[swapX + 1].remove(field[swapX + 1].first { e -> e.color == -1 })
    }

    this.swapLeftBlock = null
    this.swapRightBlock = null
    this.swapX = null
    this.swapY = null
    swapTimer = 0
  }

  fun updateColumnState() {
    columnState = field.map {
      if((it.filter { !it.active }.map { it.y }.max() ?: Long.MIN_VALUE) >= lowestY + height * 1000L - 1000L) 2
      else if((it.filter { !it.active }.map { it.y }.max() ?: Long.MIN_VALUE) >= lowestY + height * 1000L - 2000L) 1
      else 0
    }
  }

  fun updatePinchTimer() {
    if(columnState.any { it >= 1 } && stopTimer == 0) {
      pinchTimer ++
      if(pinchTimer == pinchTimerMax) {
        pinchTimer = 0
      }
    } else pinchTimer = 0
  }

  fun isAnyActiveBlock() = field.any { it.any { it.active } }
  fun getTopMostY() = (field.map { it.filter { !it.active }.map { it.y }.max() ?: Long.MIN_VALUE }.max() ?: Long.MIN_VALUE)
  fun isDead() = lowestY + height * 1000L <= getTopMostY() + 1000L

  fun judgeGameOver(): Boolean {
    if(swapTimer == 0 && chain == 0 && !isAnyActiveBlock() && stopTimer == 0 && isDead()) {
      gameOver = true
      event.gameOver(this)
      return true
    }
    return false
  }

  fun rise(delta: Int) {
    if(delta >= 1000) throw UnsupportedOperationException()
    lowestY -= delta
    if (floorY - lowestY >= 1000) {
      floorY -= 1000
      val nextNext = fieldRandomizer.next()
      for (ix in field.indices) {
        field[ix].add(next[ix])
        next[ix] = Block(floorY - 1000, nextNext[ix])
      }
    }
  }

  // 自動せりあがり
  fun automaticRise() {
    //if (swapTimer == 0 && chain == 0) rise(internalSpeed)
    if (swapTimer == 0 && chain == 0 && !isAnyActiveBlock()) {
      if (stopTimer == 0) rise(internalSpeed)
      else stopTimer --
    }
  }

  // 手動せりあがり
  fun manualRise() {
    stopTimer = 0
    nextManualRiseTarget = Math.max(((lowestY - manualRiseSpeed) / 1000L) * 1000L - 1000L, getTopMostY() - height * 1000L + 1000L)
  }

  fun automaticManualRise() {
    nextManualRiseTarget?.let { if(lowestY - it > 0) rise(Math.min(manualRiseSpeed, (lowestY - it).toInt())) }
    if(lowestY == nextManualRiseTarget) nextManualRiseTarget = null
  }

  fun updateLandedTimer() {
    field.forEach {
      it.forEach {
        if(it.landedTimer > 0) {
          if(it.landedTimer == landedTimerMax) it.landedTimer = 0
          else it.landedTimer ++
        }
      }
    }
  }

  // 1000下のブロックがない、あるいはactiveな場合自身のブロックをactiveにする
  fun activateBlock() {
    // 下のブロックから順次処理
    val blockListMixed: MutableList<BlockListMixed> = ArrayList()
    for( ix in field.indices)  {
      val col = field[ix]
      for(b in col.filter {e -> e.color > 0 && !e.active && !e.disabled }) {
        blockListMixed.add(BlockListMixed(ix, b))
      }
    }
    // これでyの小さい順 -> xの小さい順に処理する
    for (bl in blockListMixed.sortedBy { e -> e.block.y }) {
      val b = bl.block
      val col = field[bl.x]
      if(b.color < 256) {
        // 通常ブロックの処理
        val below = col.filter { e -> e.y < b.y }.maxBy { e -> e.y }
        val belowY = if (below == null) floorY else below.y + 1000
        if (b.y - belowY > 0) b.activate(initVelocity)
        else if (below != null && below.active) {
          b.activate(initVelocity)
          if (below.chain) b.chain = below.chain // これであってるかな
        }
      } else if (b.color == 256) {
        // おじゃまの処理 (めんどい)
        // 最下段が全て浮いていればactive化
        val blockGarbage = b as BlockGarbage
        val garbage = garbageList.first { e -> e.id == blockGarbage.id }

        if(!garbage.doneCurrentFrameActivation) {
          val bb = garbage.baseBlock
          var active = true

          for (gix in garbage.lowestBlock.indices) {
            val gcol = field[gix]
            val min = garbage.lowestBlock[gix]
            if (min != null) {
              val below = gcol.filter { e -> e.y < min.y }.maxBy { e -> e.y }
              val belowY = if (below == null) floorY else below.y + 1000
              if (min.y - belowY <= 0) {
                if (below != null && below.active) {
                  if (below.chain) bb.chain = true
                } else {
                  active = false
                }
              }
            }
          }

          if (active) {
            for (gcol in garbage.blockList) {
              for (gb in gcol) {
                gb.activate(initVelocity)
                if (bb.chain) gb.chain = true
              }
            }
          }
          garbage.doneCurrentFrameActivation = true
        }
      }
    }
  }

  // 落下/deactivate処理
  fun dropBlock() {
    val blockListMixed: MutableList<BlockListMixed> = ArrayList()
    for( ix in field.indices)  {
      val col = field[ix]
      for(b in col.filter {e -> e.color > 0 && e.active}) {
        blockListMixed.add(BlockListMixed(ix, b))
      }
    }
    for (bl in blockListMixed.sortedBy { e -> e.block.y }) {
      val b = bl.block
      val col = field[bl.x]
      if(b.color < 256) {
        // 通常ブロックの処理
        if (b.stayTimer < stayTimerMax) b.stayTimer++
        if (b.stayTimer == stayTimerMax) {
          b.velocity += acceralation
          if (b.velocity < maxVelocity) b.velocity = maxVelocity

          val below = col.filter { e -> e.y < b.y }.maxBy { e -> e.y }
          val belowY = if (below == null) floorY else below.y + 1000
          if (b.y + b.velocity </*=*/ belowY) {
            if (below != null && below.active) {
              //activeなブロックとの接触
              b.y = belowY
              if (0 < below.stayTimer && below.stayTimer < stayTimerMax) b.stayTimer = below.stayTimer
              b.velocity = below.velocity
            } else {
              //床または静止ブロックとの接触
              b.y = belowY
              b.deactivate()
            }
          } else {
            b.y += b.velocity
          }
        }
      } else if (b.color == 256) {
        // おじゃまの処理
        // まず、全てのブロックのstayTimerを加算
        // baseBlockのループの時にdeactivateされるので次の自身のループでstayTimerが加算されるのを防ぐ
        if (b.active && b.stayTimer < stayTimerMax) b.stayTimer++

        val blockGarbage = b as BlockGarbage
        val garbage = garbageList.first { e -> e.id == blockGarbage.id }
        if(!garbage.doneCurrentFrameDrop) {
          val bb = garbage.baseBlock
          if (bb.stayTimer == stayTimerMax) {
            var deactivate = false

            bb.velocity += acceralation
            if (bb.velocity < maxVelocity) bb.velocity = maxVelocity
            // まず
            var lowestY = bb.y + bb.velocity

            for (ix in garbage.lowestBlock.indices) {
              val min = garbage.lowestBlock[ix]
              val gcol = field[ix]

              if (min != null) {
                // bbが一番最初に処理される
                min.velocity = bb.velocity

                val below = gcol.filter { t -> t.y < min.y }.maxBy { t -> t.y }
                val belowY = if (below == null) floorY else below.y + 1000
                if (min.y + min.velocity </*=*/ belowY) {
                  if (below != null && below.active) {
                    //activeなブロックとの接触
                    if (lowestY < belowY) lowestY = belowY
                    if (0 < below.stayTimer && below.stayTimer < stayTimerMax && bb.stayTimer > below.stayTimer) bb.stayTimer = below.stayTimer
                    if (bb.velocity < below.velocity) bb.velocity = below.velocity
                  } else {
                    //床または静止ブロックとの接触
                    if (lowestY < belowY) lowestY = belowY
                    deactivate = true
                  }
                }
              }
            }

            // baseを落とす
            bb.y = lowestY
            if (deactivate) {
              bb.deactivate()
            }
            // baseではない最下段のブロックをbaseに合わせる
            for (ix in garbage.lowestBlock.indices) {
              val lb = garbage.lowestBlock[ix]
              if (lb != null && lb !== garbage.baseBlock) {
                lb.y = bb.y
                if (deactivate) {
                  lb.deactivate()
                } else {
                  lb.velocity = bb.velocity
                  lb.stayTimer = bb.stayTimer
                }
              }
            }

            // 最下段以外のブロックを積み上げていく
            for (ix in garbage.blockList.indices) {
              val gcol = garbage.blockList[ix]
              for (gb in gcol.filter { t -> t != garbage.lowestBlock[ix] }.sortedBy { t -> t.y }) {
                gb.y = garbage.blockList[ix].filter { t -> t.y < gb.y }.maxBy { t -> t.y }?.let{ it.y + 1000 } ?: throw IllegalStateException()
                if (deactivate) {
                  gb.deactivate()
                } else {
                  gb.velocity = bb.velocity
                  gb.stayTimer = bb.stayTimer
                }
              }
            }
          }
          garbage.doneCurrentFrameDrop = true
        }
      }
    }
  }

  // 消去判定
  fun eraseBlock() {
    val eraseList: MutableSet<EraseList> = HashSet()
    val eraseListGarbage: MutableSet<EraseList> = HashSet()

    for (ix in field.indices) {
      val col = field[ix]
      for (b in col.filter { e -> e.color > 0 && e.color < 256 && !e.active && !e.disabled }) {
        val verticalCombineList: MutableSet<EraseList> = HashSet()
        val horizontalCombineList: MutableSet<EraseList> = HashSet()
        verticalCombineList.add(EraseList(ix, b.y, b.color))
        horizontalCombineList.add(EraseList(ix, b.y, b.color))

        getVerticalCombineUp(verticalCombineList, ix, b)
        getVerticalCombineDown(verticalCombineList, ix, b)
        getHorizontalCombineLeft(horizontalCombineList, ix, b)
        getHorizontalCombineRight(horizontalCombineList, ix, b)

        if (verticalCombineList.size >= 3) verticalCombineList.forEach { e -> eraseList.add(e) }
        if (horizontalCombineList.size >= 3) horizontalCombineList.forEach { e -> eraseList.add(e) }
      }
    }

    if (eraseList.size > 0) {
      // 1つでもchain状態のブロックがあったら連鎖になる
      for (e in eraseList) {
        getGarbageCombine(eraseListGarbage, e.x, e.y)
      }
      var chain = false
      for (e in eraseList) {
        val block = field[e.x].first { t -> !t.active && t.y == e.y }
        if (block.chain) chain = true

        // -2のブロックに変化
        field[e.x].remove(block)
        field[e.x].add(Block(e.y, -2))
      }

      for (e in eraseListGarbage) {
        val block = (field[e.x].first { t -> !t.active && t.y == e.y }) as BlockGarbage
        val garbage = garbageList.first { t -> t.id == block.id }
        if (block.chain) chain = true

        field[e.x].remove(block)
        if(block == garbage.lowestBlock[e.x]) {
          // 最下段のブロックは解凍候補(-3)に
          field[e.x].add(BlockGarbageExtract(block.y, (Math.random() * numColor).toInt() + 1, block.id))
        } else {
          // それ以外は-4
          field[e.x].add(BlockGarbageErasing(block.y, block.id))
        }
        garbage.erasing = true
      }

      // 本家は10個までしか保持できないらしいよ
      eraseState.add(EraseState(eraseList, eraseListGarbage, chain,
              initEraseTime + eraseTimePerBlock * eraseList.size,
              initEraseTime + eraseTimePerBlock * (eraseList.size + eraseListGarbage.filter {e -> GameUtil.isInField(e.y, height, lowestY)}.size) + garbageAfterSpeed))
      // 連鎖継続、あるいは1連鎖目の場合カウンターを上げる
      if (chain || this.chain == 0) this.chain++

      if(this.chain >= 2 || eraseList.size >= 4) {
        if(columnState.any { it >= 1 }) stopTimer += 600
        else stopTimer += 120
      }
      event.erase(this, chain, eraseList, eraseListGarbage)
    }
  }

  // chain状態のinactiveブロック(設置したばかりで連鎖しなかったもの)のchainを解除
  // chain状態のブロック、またはchain状態のeraseStateがあった場合は連鎖継続
  fun remainChain() {
    var remainChain = false
    for (col in field) {
      for (e in col) {
        if (!e.active && e.chain) e.chain = false
        if (e.chain) remainChain = true
      }
    }
    for (e in eraseState) {
      if (e.chain) remainChain = true
    }

    val swapLeftBlock = this.swapLeftBlock
    val swapRightBlock = this.swapRightBlock

    if ((swapLeftBlock != null && swapLeftBlock.chain) || (swapRightBlock != null) && swapRightBlock.chain) remainChain = true

    if (!remainChain) {
      if (eraseState.size > 0) {
        chain = 1
      } else {
        chain = 0
      }
    }
  }

  // 消去状態のブロックは一定時間経ったら消える
  fun countErase() {
    val iter = eraseState.iterator()
    while (iter.hasNext()) {
      val e = iter.next()
      if (e.timer == e.timerMax) {
        for (eb in e.eraseList) {
          field[eb.x].remove(field[eb.x].firstOrNull { t -> t.color == -2 && t.y == eb.y })
          val up = field[eb.x].firstOrNull { t -> t.color >= 0 && !t.active && t.y == eb.y + 1000 }
          if (up != null) {
            up.chain = true
          }
        }

        // 各idにつき1回だけ処理が行われるようにすべし
        for (eb in e.eraseListGarbage) {
          val block = field[eb.x].first { t -> (t.color == -3 || t.color == -4) && t.y == eb.y }
          val garbageId = if(block.color == -3) (block as BlockGarbageExtract).id else (block as BlockGarbageErasing).id
          val garbage = garbageList.firstOrNull { t -> t.id == garbageId }
          field[eb.x].remove(block)
          if(block.color == -3) {
            // ブロックが解凍される
            val blockGarbageExtract = block as BlockGarbageExtract
            val newBlock = Block(blockGarbageExtract.y, blockGarbageExtract.extractColor)
            newBlock.disabled = true
            //newBlock.chain = true
            field[eb.x].add(newBlock)
          }
          if(garbage != null) {
            // 各idにつき1回だけの処理...厚いやつを薄くする
            if (garbage.height >= 2){
              generateGarbage(garbage.baseBlockX, garbage.baseBlock.y + 1000L, garbage.width, garbage.height - 1, true)
            }
            garbageList.remove(garbage)
          }
        }
      }
      if (e.timer == e.timerMaxGarbage) {
        for(eb in e.eraseListGarbage) {
          val block = field[eb.x].first { t -> t.disabled && t.y == eb.y }
          block.disabled = false
          block.activate(initVelocity)
          block.chain = true
        }
        iter.remove()
      } else {
        e.timer++
      }
    }
  }

  // おじゃまを降らす
  fun dropGarbage(baseX: Int, width: Int, height: Int, disabled: Boolean = false) {
    val baseY = Math.max(lowestY + this.height * 1000L, getTopMostY()) + 1000L
    generateGarbage(baseX, baseY, width, height, disabled)
  }

  fun generateGarbage(baseX: Int, baseY: Long, width: Int, height: Int, disabled: Boolean = false) {
    if(height > 1 && width != 6) throw UnsupportedOperationException()
    val blockList: Array<MutableList<BlockGarbage>> = Array(this.width, { ArrayList<BlockGarbage>() })

    for (iy in 0..height - 1) {
      for (ix in baseX..baseX + width - 1) {
        val block = BlockGarbage(baseY + iy * 1000L, lastGarbageId + 1)
        if(disabled) block.disabled = true
        blockList[ix].add(block)
        field[ix].add(block)
      }
    }

    garbageList.add(Garbage(lastGarbageId + 1, width, height, blockList))
    lastGarbageId += 1
  }

  fun dropTestBlock() {
    for (iy in 0..2) {
      for (ix in field.indices) {
        val n = (Math.random() * (numColor + 1)).toInt()
        if(n != 0) field[ix].add(Block(10000 + iy * 1000L, n))
      }
    }
  }

  // 結合リストを得る
  fun getVerticalCombineUp(list: MutableSet<EraseList>, col: Int, currentBlock: Block) {
    val up = field[col].firstOrNull { e -> e.color == currentBlock.color && !e.active && !e.disabled && e.y == currentBlock.y + 1000L }
    if (up != null) {
      list.add(EraseList(col, up.y, up.color))
      getVerticalCombineUp(list, col, up)
    }
  }

  fun getVerticalCombineDown(list: MutableSet<EraseList>, col: Int, currentBlock: Block) {
    val down = field[col].firstOrNull { e -> e.color == currentBlock.color && !e.active && !e.disabled && e.y == currentBlock.y - 1000L }
    if (down != null) {
      list.add(EraseList(col, down.y, down.color))
      getVerticalCombineDown(list, col, down)
    }
  }

  fun getHorizontalCombineLeft(list: MutableSet<EraseList>, col: Int, currentBlock: Block) {
    val left = if (col == 0) null else field[col - 1].firstOrNull { e -> e.color == currentBlock.color && !e.active && !e.disabled && e.y == currentBlock.y }
    if (left != null) {
      list.add(EraseList(col - 1, left.y, left.color))
      getHorizontalCombineLeft(list, col - 1, left)
    }
  }

  fun getHorizontalCombineRight(list: MutableSet<EraseList>, col: Int, currentBlock: Block) {
    val right = if (col == width - 1) null else field[col + 1].firstOrNull { e -> e.color == currentBlock.color && !e.active && !e.disabled && e.y == currentBlock.y }
    if (right != null) {
      list.add(EraseList(col + 1, right.y, right.color))
      getHorizontalCombineRight(list, col + 1, right)
    }
  }

  fun getGarbageCombine(list: MutableSet<EraseList>, x: Int, y: Long) {
    val up = field[x].firstOrNull { e -> e.color == 256 && !e.active && !e.disabled && e.y == y + 1000L }
    val down = field[x].firstOrNull { e -> e.color == 256 && !e.active && !e.disabled && e.y == y - 1000L }
    val left = if (x == 0) null else field[x - 1].firstOrNull { e -> e.color == 256 && !e.active && !e.disabled && e.y == y }
    val right = if (x == width - 1) null else field[x + 1].firstOrNull { e -> e.color == 256 && !e.active && !e.disabled && e.y == y }
    if(up != null && list.firstOrNull{ e -> e.x == x && e.y == y + 1000L } == null) {
      list.add(EraseList(x, y + 1000L, 256))
      getGarbageCombine(list, x, y + 1000L)
    }
    if(down != null && list.firstOrNull{ e -> e.x == x && e.y == y - 1000L } == null) {
      list.add(EraseList(x, y - 1000L, 256))
      getGarbageCombine(list, x, y - 1000L)
    }
    if(left != null && list.firstOrNull{ e -> e.x == x - 1 && e.y == y } == null) {
      list.add(EraseList(x - 1, y, 256))
      getGarbageCombine(list, x - 1, y)
    }
    if(right != null && list.firstOrNull{ e -> e.x == x + 1 && e.y == y } == null) {
      list.add(EraseList(x + 1, y, 256))
      getGarbageCombine(list, x + 1, y)
    }
  }
}

class GameUtil {
  companion object {
    fun isInField(y: Long, fieldHeight: Int, lowestY: Long): Boolean {
      return lowestY <= y && y < lowestY + fieldHeight * 1000
    }
  }
}
