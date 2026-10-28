package my.music.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.session.*
import my.music.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PlaybackSnapshot(
    val connected: Boolean=false, val items: List<MediaItem> = emptyList(), val index: Int=0,
    val playing: Boolean=false, val position: Long=0, val duration: Long=0, val shuffle: Boolean=false,
    val repeat: Int=0, val speed: Float=1f, val timerRemaining: Long = -1, val timerFinish: Boolean=false, val error: String?=null, val effectsMessage: String?=null,
) { val current get()=items.getOrNull(index) }

class PlaybackConnection(context: Context,private val scope: CoroutineScope) {
    private val mutable=MutableStateFlow(PlaybackSnapshot())
    val state=mutable.asStateFlow()
    var controller: MediaController?=null; private set
    private val future=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()
    private val listener=object: Player.Listener {
        override fun onEvents(player: Player,events: Player.Events)=refresh(events.containsAny(Player.EVENT_TIMELINE_CHANGED,Player.EVENT_MEDIA_METADATA_CHANGED,Player.EVENT_MEDIA_ITEM_TRANSITION))
        override fun onPlayerError(error: PlaybackException) { mutable.value=mutable.value.copy(error="Unable to play this file: ${error.errorCodeName}") }
    }
    init {
        future.addListener({
            try { controller=future.get().also { it.addListener(listener) }; refresh(true) }
            catch(e: Exception) { mutable.value=mutable.value.copy(error=e.cause?.message ?: "Unable to connect to playback") }
        },ContextCompat.getMainExecutor(context))
        scope.launch { while(isActive) { delay(400); refresh() } }
    }
    private fun refresh(rebuild: Boolean=false) {
        val p=controller ?: return
        val items=if(rebuild || mutable.value.items.size!=p.mediaItemCount) (0 until p.mediaItemCount).map(p::getMediaItemAt) else mutable.value.items
        mutable.value=PlaybackSnapshot(true,items,p.currentMediaItemIndex.coerceAtLeast(0),p.isPlaying,p.currentPosition.coerceAtLeast(0),p.duration.takeIf { it>0 } ?: 0,p.shuffleModeEnabled,p.repeatMode,p.playbackParameters.speed,p.sessionExtras.getLong("timer_remaining",-1),p.sessionExtras.getBoolean("timer_finish"),p.playerError?.let { "Unable to play this file: ${it.errorCodeName}" },p.sessionExtras.getString("effects_status"))
    }
    fun play(tracks: List<Track>,start: Int=0,shuffle: Boolean?=null) {
        if(tracks.isEmpty()) return
        check(controller!=null) {"Playback is still connecting. Try again in a moment."}
        require(tracks.size<=5000) { "A queue can contain up to 5,000 songs" }
        controller?.apply {
            shuffle?.let {shuffleModeEnabled=it}
            if(shuffle==true) setMediaItems(tracks.map(::item),true)
            else setMediaItems(tracks.map(::item),start.coerceIn(tracks.indices),0)
            prepare();play()
        }
    }
    fun add(tracks: List<Track>,next: Boolean) {
        controller?.let { p ->
            require(p.mediaItemCount+tracks.size<=5000) { "A queue can contain up to 5,000 songs" }
            p.addMediaItems(if(next) (p.currentMediaItemIndex+1).coerceIn(0,p.mediaItemCount) else p.mediaItemCount,tracks.map(::item))
            if(p.playbackState==Player.STATE_IDLE) p.prepare()
        }
    }
    private fun item(track: Track)=MediaItem.Builder().setMediaId(UUID.randomUUID().toString()).setUri(track.id.uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).setAlbumTitle(track.album)
            .setArtworkUri(track.artworkUri?.let(Uri::parse)).setExtras(Bundle().apply { putString("source_uri",track.id.uri); putLong("duration",track.durationMs) }).build()).build()
    fun timer(durationMs: Long,finish: Boolean) { controller?.sendCustomCommand(SessionCommand("timer.start",Bundle.EMPTY),Bundle().apply { putLong("duration",durationMs); putBoolean("finish",finish) }) }
    fun reshuffle() {controller?.sendCustomCommand(SessionCommand("queue.reshuffle",Bundle.EMPTY),Bundle.EMPTY)}
    fun cancelTimer() { controller?.sendCustomCommand(SessionCommand("timer.cancel",Bundle.EMPTY),Bundle.EMPTY) }
    fun release() { controller?.removeListener(listener); MediaController.releaseFuture(future); controller=null }
}
