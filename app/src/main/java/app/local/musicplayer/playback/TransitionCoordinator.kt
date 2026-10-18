@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package app.local.musicplayer.playback

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*

/** Service-owned overlap. Only this class can own a second decoder. All calls use the main looper. */
class TransitionCoordinator(
    private val scope: CoroutineScope,private val active: ()->ExoPlayer,
    private val create: ()->ExoPlayer,private val dispose: (ExoPlayer)->Unit,private val promote: (ExoPlayer)->Unit,
) {
    private var pending: ExoPlayer?=null
    private var job: Job?=null
    val overlapping get()=pending!=null
    fun tick(enabled: Boolean,seconds: Float) {
        if(!enabled) {cancel();return}
        if(pending!=null) return
        val primary=active()
        if(!primary.isPlaying || primary.duration<=0 || primary.isCurrentMediaItemLive) return
        val remaining=primary.duration-primary.currentPosition
        val speed=primary.playbackParameters.speed
        if(remaining<=0 || remaining>seconds*1000*speed) return
        val next=if(primary.repeatMode==Player.REPEAT_MODE_ONE) primary.currentMediaItemIndex else primary.nextMediaItemIndex
        if(next<0) return
        val secondary=create();pending=secondary
        val originalIndex=primary.currentMediaItemIndex
        secondary.volume=0f
        secondary.setMediaItems((0 until primary.mediaItemCount).map(primary::getMediaItemAt),next,0)
        secondary.repeatMode=primary.repeatMode;secondary.shuffleModeEnabled=primary.shuffleModeEnabled;secondary.playbackParameters=primary.playbackParameters
        secondary.prepare()
        job=scope.launch {
            try {
                val readyDeadline=SystemClock.elapsedRealtime()+3000
                while(secondary.playbackState!=Player.STATE_READY && SystemClock.elapsedRealtime()<readyDeadline) {
                    if(secondary.playerError!=null || !primary.playWhenReady) {cancel();return@launch}
                    delay(20)
                }
                if(secondary.playbackState!=Player.STATE_READY || primary.currentMediaItemIndex!=originalIndex) {cancel();return@launch}
                val duration=((primary.duration-primary.currentPosition)/speed).toLong().coerceIn(80,(seconds*1000).toLong().coerceAtLeast(80))
                val start=SystemClock.elapsedRealtime()
                secondary.play()
                while(pending===secondary) {
                    if(!primary.playWhenReady) {cancel();return@launch}
                    val progress=((SystemClock.elapsedRealtime()-start).toFloat()/duration).coerceIn(0f,1f)
                    primary.volume=1f-progress;secondary.volume=progress
                    if(progress>=1f) {finish();return@launch}
                    delay(20)
                }
            } catch(e: CancellationException) {throw e}
            catch(_: Exception) {cancel()}
        }
    }
    fun finish() {
        val secondary=pending ?: return
        pending=null;job?.cancel();job=null
        secondary.volume=1f
        promote(secondary)
    }
    fun cancel() {
        job?.cancel();job=null
        val secondary=pending;pending=null
        active().volume=1f
        if(secondary!=null) dispose(secondary)
    }
}
