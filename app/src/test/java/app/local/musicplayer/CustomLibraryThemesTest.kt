package app.local.musicplayer
import app.local.musicplayer.ui.*
import org.junit.Assert.*
import org.junit.Test
class CustomLibraryThemesTest {
 @Test fun roundTripAndResolutionPreserveStableIdentity() {
  val value=LibraryTheme("My ocean",listOf(0xFF102B52,0xFF10443F),0xFFFFC875,"library:test")
  val raw=CustomLibraryThemes.encode(listOf(value))
  assertEquals(listOf(value),CustomLibraryThemes.decode(raw))
  assertEquals(value,LibraryThemes.find(value.id,raw))
  assertEquals("Midnight",LibraryThemes.find("library:missing",raw).name)
 }
 @Test fun malformedDataAndBuiltinImpersonationAreIgnored() {
  assertTrue(CustomLibraryThemes.decode("bad json").isEmpty())
  assertTrue(CustomLibraryThemes.decode("[{\"id\":\"Midnight\",\"name\":\"Wrong\",\"top\":4278190080,\"bottom\":4278190080,\"accent\":4294967295}]").isEmpty())
  assertTrue(CustomLibraryThemes.decode("[{}]").isEmpty())
 }
}
