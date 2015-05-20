package io.github.suitougreentea.util

import java.io.File
import org.newdawn.slick.Image
import org.newdawn.slick.Color
import java.util.HashMap
import kotlin.dom.get
import kotlin.dom.parseXml

class AngelCodeFontXML (fntPath: String){
  val fntFile = File(fntPath)
  val fnt = parseXml(fntFile)

  val pages: Array<Image> = Array((fnt.get("common").get(0).getAttribute("pages").toInt()), { Image(0, 0) })
  val chars: HashMap<Int, Glyph> = HashMap()

  val lineHeight = fnt.get("common").get(0).getAttribute("lineHeight").toInt()
  val base = fnt.get("common").get(0).getAttribute("base").toInt()

  init {
    for (e in fnt.get("pages").get(0).get("page")) {
      val file = File(fntFile.getParent(), (e.getAttribute("file")))
      pages[e.getAttribute("id").toInt()] = Image(file.getPath())
    }

    for (e in fnt.get("chars").get(0).get("char")) {
      var id = e.getAttribute("id").toInt()

      var x = e.getAttribute("x").toInt()
      var y = e.getAttribute("y").toInt()
      var width = e.getAttribute("width").toInt()
      var height = e.getAttribute("height").toInt()
      var xoffset = e.getAttribute("xoffset").toInt()
      var yoffset = e.getAttribute("yoffset").toInt()
      var xadvance = e.getAttribute("xadvance").toInt()
      var page = pages[e.getAttribute("page").toInt()]

      chars.put(id, Glyph(x, y, width, height, xoffset, yoffset, xadvance, page))
    }
  }

  fun measureString(str: String): Int {
    var cx = 0
    for(c in str){
      cx += measureChar(c)
    }
    return cx
  }

  fun measureChar(c: Char): Int {
    val glyph = chars.get(c.toInt())
    if(glyph != null){
      return glyph.xadvance
    } else return 0
  }

  fun drawChar(c: Char, x: Int, y: Int, color: Color = Color(1f, 1f, 1f)): Int {
    val glyph = chars.get(c.toInt())
    if(glyph != null){
      glyph.page.draw(
        (x + glyph.xoffset).toFloat(),
        (y + glyph.yoffset + lineHeight - base).toFloat(),
        (x + glyph.xoffset + glyph.width).toFloat(),
        (y + glyph.yoffset + lineHeight - base + glyph.height).toFloat(),
        glyph.x.toFloat(),
        glyph.y.toFloat(),
        (glyph.x + glyph.width).toFloat(),
        (glyph.y + glyph.height).toFloat(),
        color
      )
      return glyph.xadvance
    } else {
      return 0
    }
  }

  fun drawStringEmbedded(str: String, x: Int, y: Int, color: Color = Color(1f, 1f, 1f)) {
    var cx = 0
    for(c in str){
      cx += drawChar(c, x + cx, y, color)
    }
  }

  fun drawString(str: String, x: Int, y: Int, align: TextAlign = TextAlign.LEFT, color: Color = Color(1f, 1f, 1f)) {
    var cy = 0
    for(s in str.split("\n")){
      when(align) {
        TextAlign.LEFT -> drawStringEmbedded(s, x, y + cy, color)
        TextAlign.CENTER -> drawStringEmbedded(s, x - (measureString(s) / 2), y + cy, color)
        TextAlign.RIGHT -> drawStringEmbedded(s, x - measureString(s), y + cy, color)
      }
      cy += lineHeight
    }
  }

  fun measureStringFormatted(str: String): Int {
    var cx = 0
    var i = 0
    while(i < str.length()){
      var c = str.charAt(i)
      if(c == '@'){
        when(str.charAt(i + 1)) {
          '#' -> {
            if(str.charAt(i + 8) == '#') {
              i += 9
            } else {
              i += 11
            }
          }
          '@' -> {
            cx += measureChar('@')
            i += 2
          }
        }
      } else {
        cx += measureChar(c)
        i += 1
      }
    }
    return cx
  }

  fun drawStringFormattedEmbedded(str: String, x: Int, y: Int, startColor: Color) : Color {
    var cx = 0
    var cc = startColor
    var i = 0
    while(i < str.length()){
      var c = str.charAt(i)
      if(c == '@'){
        when(str.charAt(i + 1)) {
          '#' -> {
            if(str.charAt(i + 8) == '#') {
              cc = Color(
                Integer.parseInt(str.substring(i + 2, i + 4), 16) / 255f,
                Integer.parseInt(str.substring(i + 4, i + 6), 16) / 255f,
                Integer.parseInt(str.substring(i + 6, i + 8), 16) / 255f)
              i += 9
            } else {
              cc = Color(
                Integer.parseInt(str.substring(i + 2, i + 4), 16) / 255f,
                Integer.parseInt(str.substring(i + 4, i + 6), 16) / 255f,
                Integer.parseInt(str.substring(i + 6, i + 8), 16) / 255f,
                Integer.parseInt(str.substring(i + 8, i + 10), 16) / 255f)
              i += 11
            }
          }
          '@' -> {
            cx += drawChar('@', x + cx, y, cc)
            i += 2
          }
        }
      } else {
        cx += drawChar(c, x + cx, y, cc)
        i += 1
      }
    }
    return cc
  }

  fun drawStringFormatted(str: String, x: Int, y: Int, align: TextAlign = TextAlign.LEFT) {
    var cy = 0
    var lastColor = Color(1f, 1f, 1f)
    for(s in str.split("\n")){
      when(align) {
        TextAlign.LEFT -> lastColor = drawStringFormattedEmbedded(s, x, y + cy, lastColor)
        TextAlign.CENTER -> lastColor = drawStringFormattedEmbedded(s, x - (measureStringFormatted(s) / 2), y + cy, lastColor)
        TextAlign.RIGHT -> lastColor = drawStringFormattedEmbedded(s, x - measureStringFormatted(s), y + cy, lastColor)
      }
      cy += lineHeight
    }
  }
}

class Glyph(val x: Int, val y: Int, val width: Int, val height: Int, val xoffset: Int, val yoffset: Int, val xadvance: Int, val page: Image){

}

enum class TextAlign {
  LEFT
  CENTER
  RIGHT
}
