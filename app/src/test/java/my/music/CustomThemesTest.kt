package my.music
import my.music.ui.CustomThemes
import my.music.ui.PlayerTheme
import org.junit.Assert.*
import org.junit.Test
class CustomThemesTest {
 @Test fun retiredThemesFallBackWithoutRenumberingLayouts() {
  assertEquals(15,my.music.ui.PlayerThemes.all.size)
  listOf("Aurora","Prism","Frost").forEach {assertEquals("Artwork",my.music.ui.PlayerThemes.find(it).name)}
  assertEquals(16,my.music.ui.PlayerThemes.find("Film").style)
 }

 @Test fun customThemeRoundTripPreservesIdentityLayoutAndColors() {
  val theme=PlayerTheme("My violet",2,0xFF140828,0xFF41145E,0xFFFFC875,id="custom:test",coverColors=false)
  assertEquals(listOf(theme),CustomThemes.decode(CustomThemes.encode(listOf(theme))))
 }
 @Test fun malformedPresetsDoNotOverrideBuiltins() {
  assertTrue(CustomThemes.decode("bad json").isEmpty())
  assertTrue(CustomThemes.decode("[{\"id\":\"Vinyl\"}]").isEmpty())
  assertEquals(0xFF1256ABL,CustomThemes.color("1256ab"));assertNull(CustomThemes.color("no color"))
 }
}
