package app.local.musicplayer

import app.local.musicplayer.model.*
import app.local.musicplayer.library.LibraryQuery
import app.local.musicplayer.lyrics.LrcParser
import app.local.musicplayer.playback.QueueState
import app.local.musicplayer.playback.SleepDeadline
import app.local.musicplayer.backup.BackupCodec
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    private fun track(id: String, title: String, artist: String = "Artist") = Track(TrackId(id),title,artist,"Album",null,"Music",12000,"audio/wav",0,null)
    @Test fun queueReorderPreservesActiveDuplicateInstance() {
        val queue = QueueState(listOf(QueueEntry("a",TrackId("same")),QueueEntry("b",TrackId("same")),QueueEntry("c",TrackId("other"))),"b")
        val moved = queue.move(1,0)
        assertEquals(listOf("b","a","c"),moved.entries.map { it.entryId })
        assertEquals("b",moved.activeEntryId)
        assertEquals("b",moved.remove("a").activeEntryId)
    }
    @Test fun removingCurrentChoosesNextAndClearingIsEmpty() {
        val queue = QueueState(listOf(QueueEntry("a",TrackId("1")),QueueEntry("b",TrackId("2"))),"a")
        assertEquals("b",queue.remove("a").activeEntryId)
        assertNull(queue.remove("a").remove("b").activeEntryId)
    }
    @Test fun searchPreservesUriIdentityAndFindsArtist() {
        val tracks = listOf(track("content://1","Dawn","Blue"),track("content://2","Dawn","Red"))
        assertEquals(listOf("content://1","content://2"),LibraryQuery.filter(tracks,"dawn").map { it.id.uri })
        assertEquals("content://2",LibraryQuery.filter(tracks,"RED").single().id.uri)
        assertTrue(LibraryQuery.filter(tracks,"missing").isEmpty())
    }
    @Test fun lyricsSupportMultipleStampsOffsetsAndPlainText() {
        val doc = LrcParser.parse("[offset:500]\n[00:01.20][00:03.00]Hello\n[00:04.00]World")
        assertEquals(listOf(1700L,3500L,4500L),doc.lines.map { it.timeMs })
        assertEquals("Hello",doc.activeLine(3600)?.text)
        assertEquals("plain words",LrcParser.parse("plain words").lines.single().text)
        assertFalse(LrcParser.parse("[xx:bad]bad timestamp").timed)
    }
    @Test fun sleepDeadlineUsesMonotonicTimeAndFinishesCurrentTrack() {
        val sleep = SleepDeadline(1000,5000,true)
        assertFalse(sleep.shouldStop(5999,false))
        assertFalse(sleep.shouldStop(6000,false))
        assertTrue(sleep.shouldStop(6000,true))
        assertTrue(SleepDeadline(1000,5000,false).shouldStop(6000,false))
    }
    @Test fun backupRejectsUnsupportedAndMalformedBeforeRestore() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("{\"version\":99}") }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("[]") }
        val decoded = BackupCodec.decode("{\"version\":1,\"playlists\":[{\"id\":\"p\",\"name\":\"Evening\",\"tracks\":[\"content://1\",\"content://1\"]}],\"favorites\":[\"content://1\"],\"hidden\":[],\"lyrics\":{},\"preferences\":{}}")
        assertEquals(listOf("content://1","content://1"),decoded.playlists.single().tracks)
        assertEquals("Evening",BackupCodec.decode(BackupCodec.encode(decoded)).playlists.single().name)
    }
}
