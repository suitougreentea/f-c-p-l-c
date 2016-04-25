package io.github.suitougreentea.fcplc

import io.github.suitougreentea.util.AngelCodeFontXML
import org.newdawn.slick.Image

class SystemResource {
  enum class Img(val path: String) {
    background("background.png"),
    block40("block-40.png"),
    garbage40("garbage-40.png"),
    eraseBlur40("eraseblur-40.png"),
    eraseEffect40("eraseeffect-40.png"),
    chain("chain.png"),
  }
  private val _img: Array<Image?> = Array(Img.values().size, {null})
  fun getImage(img: Img): Image {
    val result = _img[img.ordinal]
    if(result != null) return result
    else throw IllegalStateException()
  }

  enum class Fnt(val path: String) {
    jp("font/jpfont16.fnt"),
  }
  private val _fnt: Array<AngelCodeFontXML?> = Array(Fnt.values().size, {null})
  fun getFont(fnt: Fnt): AngelCodeFontXML {
    val result = _fnt[fnt.ordinal]
    if(result != null) return result
    else throw IllegalStateException()
  }

  fun loadImages() {
    for(e in Img.values()) {
      _img[e.ordinal] = loadImage(e.path)
    }
  }

  fun loadFonts() {
    for(e in Fnt.values()) {
      _fnt[e.ordinal] = loadFont(e.path)
    }
  }

  fun loadImage(path: String): Image {
    return Image("res/" + path)
  }

  fun loadFont(path: String): AngelCodeFontXML {
    return AngelCodeFontXML("res/" + path)
  }
}
