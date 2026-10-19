package app.local.musicplayer
import app.local.musicplayer.library.GroupLibrary
import app.local.musicplayer.model.*
import app.local.musicplayer.ui.PlayerThemes
import org.junit.Assert.*
import org.junit.Test
class RevisionTest {
 private fun track(id:String,folder:String,album:String)=Track(TrackId(id),id,"Artist",album,null,folder,12000,"audio/wav",0,null)
 @Test fun groupingAndBulkSelectionPreserveIdentityAndRanking() {
  val tracks=listOf(track("a","Music/A","One"),track("b","Music/B","Two"),track("c","Music/A","One"))
  val groups=GroupLibrary.groups(tracks,"folder","Name Z–A")
  assertEquals(listOf("Music/B","Music/A"),groups.map {it.key})
  assertEquals(listOf("a","c"),GroupLibrary.selected(groups,setOf("Music/A")).map {it.id.uri})
  assertEquals(1,GroupLibrary.groups(tracks,"artist","Name A–Z").size)
  assertEquals(2,GroupLibrary.groups(tracks,"album","Name A–Z").size)
 }
 @Test fun playerThemesHaveDistinctVisualStylesAndStableFallback() {
  assertTrue(PlayerThemes.all.size>=11)
  assertEquals(PlayerThemes.all.size,PlayerThemes.all.map {it.style}.distinct().size)
  assertEquals("Artwork",PlayerThemes.find("missing").name)
 }
 @Test fun staleArtworkCannotOwnNullOrCachedReplacement() {
  val requests=app.local.musicplayer.widgets.ArtworkRequests()
  val a=requests.select("a");assertTrue(requests.owns(a,"a"))
  requests.select(null);assertFalse(requests.owns(a,"a"))
  val b=requests.select("b");requests.select("a");assertFalse(requests.owns(b,"b"))
 }}
