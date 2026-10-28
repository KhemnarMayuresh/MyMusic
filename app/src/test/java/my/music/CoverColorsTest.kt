package my.music
import my.music.ui.coverColors
import org.junit.Assert.*
import org.junit.Test
class CoverColorsTest {
 @Test fun backgroundsAndAccentRetainCoverHue() {
  val red=coverColors(intArrayOf(0xFFFF0000.toInt()));val blue=coverColors(intArrayOf(0xFF0000FF.toInt()))
  for(i in 0..2) {assertTrue((red[i] shr 16 and 255)>(red[i] and 255));assertTrue((blue[i] and 255)>(blue[i] shr 16 and 255))}
  assertTrue((red[0] shr 16 and 255)<(red[1] shr 16 and 255))
 }
 @Test fun transparentPixelsDoNotPollutePaletteAndEmptyUsesDefault() {
  assertArrayEquals(coverColors(intArrayOf(0xFFFF0000.toInt())),coverColors(intArrayOf(0xFFFF0000.toInt(),0x000000FF)))
  assertArrayEquals(longArrayOf(0xFF090B22,0xFF35134D,0xFFFFBF00),coverColors(intArrayOf(0)))
 }
}
