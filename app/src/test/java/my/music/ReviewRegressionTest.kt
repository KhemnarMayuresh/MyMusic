package my.music

import my.music.collections.*
import my.music.files.WriteAccess
import my.music.library.LibraryQuery
import my.music.model.*
import my.music.playback.ListeningHistoryTracker
import org.junit.Assert.*
import org.junit.Test

class ReviewRegressionTest {
    private fun track(id: String,title: String)=Track(TrackId(id),title,"Artist","Album",null,"Music",12000,"audio/wav",0,null)
    @Test fun duplicatePlaylistSelectionUsesEntryIdentityEvenWithMissingMedia() {
        val a=track("a","Alpha");val b=track("b","Beta");val c=track("c","Charlie")
        val entries=listOf(PlaylistEntry("first-a","a"),PlaylistEntry("missing","gone"),PlaylistEntry("b","b"),PlaylistEntry("second-a","a"),PlaylistEntry("c","c"))
        val plan=PlaylistPlayback.plan(entries,listOf(a,b,c),"second-a")!!
        assertEquals(2,plan.startIndex);assertEquals(listOf("a","b","a","c"),plan.tracks.map {it.id.uri})
        assertNull(PlaylistPlayback.plan(entries,listOf(a,b,c),"missing"))
    }
    @Test fun playCountsAdvanceOnTransitionsAndNeverOnPauseResume() {
        val history=ListeningHistoryTracker()
        history.transition("a");assertTrue(history.playing("a"));assertFalse(history.playing("a"))
        history.transition("b");assertTrue(history.playing("b"));assertFalse(history.playing("b"))
        history.transition("b");assertTrue(history.playing("b")) // repeat of the same entry
    }
    @Test fun legacyDeletionRequestsWriteAccessIndependentlyOfReadAccess() {
        assertTrue(WriteAccess.needsPermission(24,false));assertTrue(WriteAccess.needsPermission(28,false))
        assertFalse(WriteAccess.needsPermission(29,false));assertFalse(WriteAccess.needsPermission(24,true))
    }
    @Test fun historyBrowsingPreservesRankingWhileNormalLibraryCanSort() {
        val items=listOf(track("b","Beta"),track("a","Alpha"))
        assertEquals(listOf("b","a"),LibraryQuery.browse(items,"",null).map {it.id.uri})
        assertEquals(listOf("a","b"),LibraryQuery.browse(items,"","Title A–Z").map {it.id.uri})
    }
}
