package my.music.playback

/** A visit is counted once when audible playback starts, including repeats and fade promotion. */
class ListeningHistoryTracker {
    private var current: String?=null
    private var recorded=false
    fun transition(entryId: String?) {current=entryId;recorded=false}
    fun playing(entryId: String?): Boolean {
        if(entryId==null) return false
        if(current!=entryId) transition(entryId)
        if(recorded) return false
        recorded=true
        return true
    }
}
