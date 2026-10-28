package my.music.playback

data class SleepDeadline(val startedAtMs: Long, val durationMs: Long, val finishCurrentTrack: Boolean) {
    init { require(durationMs > 0 && durationMs <= 24 * 60 * 60 * 1000L) }
    fun remaining(nowMs: Long) = (durationMs - (nowMs-startedAtMs).coerceAtLeast(0)).coerceAtLeast(0)
    fun shouldStop(nowMs: Long, trackEnded: Boolean) = remaining(nowMs) == 0L && (!finishCurrentTrack || trackEnded)
}
