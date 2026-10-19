package app.local.musicplayer
import app.local.musicplayer.ui.*
import org.junit.Assert.*
import org.junit.Test
class ThemeProgressTest {
 @Test fun progressUsesRealPositionAndRejectsInvalidInput() {
  assertEquals(.25f,SeekMath.fraction(2500,10000),.001f)
  assertEquals(7500L,SeekMath.position(.75f,10000))
  assertEquals(0L,SeekMath.position(Float.NaN,10000))
  assertEquals(10000L,SeekMath.position(2f,10000))
  assertEquals(0f,SeekMath.fraction(20,0),0f)
 }
 @Test fun circularProgressStartsAtTwelveAndMovesClockwise() {
  assertEquals(0f,SeekMath.ringFraction(50f,0f,100f,100f),.001f)
  assertEquals(.25f,SeekMath.ringFraction(100f,50f,100f,100f),.001f)
  assertEquals(.5f,SeekMath.ringFraction(50f,100f,100f,100f),.001f)
  assertEquals(.75f,SeekMath.ringFraction(0f,50f,100f,100f),.001f)
 }
 @Test fun themesOwnDistinctProgressLayouts() {
  assertEquals(PlayerLayout.RING,PlayerThemes.find("Orbit").layout)
  assertEquals(PlayerLayout.LIGHT_PANEL,PlayerThemes.find("Polaroid").layout)
  assertEquals(PlayerLayout.IMMERSIVE,PlayerThemes.find("Sunset").layout)
  assertEquals(PlayerLayout.HEADER_PROGRESS,PlayerThemes.find("Minimal").layout)
  assertTrue(PlayerThemes.all.map {it.layout}.distinct().size>=5)
 }
}
