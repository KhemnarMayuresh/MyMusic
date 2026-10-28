package my.music
import my.music.model.LibraryDetails
import org.junit.Assert.*
import org.junit.Test
class LibraryDetailsTest {
 @Test fun coverOverridesAreIndependentOfTextFieldsAndRejectMalformedInput() {
  val details=LibraryDetails.parse("{\"cover\":\"content://test/cover\",\"title\":null}")!!
  assertEquals("content://test/cover",details.cover);assertNull(details.title);assertEquals("null",LibraryDetails.parse("{\"title\":\"null\"}")!!.title)
  assertNull(LibraryDetails.parse("not json"));assertNull(LibraryDetails.parse(null));assertNull(LibraryDetails.parse("{}")!!.cover)
 }
}
