@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package my.music.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import android.content.Context
import androidx.media3.session.*
import my.music.MusicApplication
import my.music.widgets.PlayerWidgetProvider
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*

class PlaybackService: MediaSessionService() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var session: MediaSession?=null
    private lateinit var queueStore: QueueStore
    private var timer: SleepDeadline?=null
    private var timerTrack: String?=null
    private lateinit var timerStore: TimerStore
    private var effectsStatus: String?=null
    private val processors=java.util.IdentityHashMap<ExoPlayer,EqProcessor>()
    private val effects=java.util.IdentityHashMap<ExoPlayer,AudioEffects>()
    private lateinit var transition: TransitionCoordinator
    private lateinit var playerListener: Player.Listener
    private val history=ListeningHistoryTracker()
    private var gapResume: Job?=null
    private val noisyReceiver=object: android.content.BroadcastReceiver() {
        override fun onReceive(context: Context,intent: Intent) {
            if(app.collections.state.value.flag("pause_detach",true)) {cancelGapResume();player.pause()}
        }
    }
    private val app get()=application as MusicApplication
    override fun onCreate() {
        super.onCreate()
        queueStore=QueueStore(this)
        timerStore=TimerStore(this)
        player=createPlayer(false)
        PlayerAudioSignal.active=processors[player]?.signal
        androidx.core.content.ContextCompat.registerReceiver(this,noisyReceiver,android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        val launch=packageManager.getLaunchIntentForPackage(packageName)!!
        val activity=PendingIntent.getActivity(this,0,launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        session=MediaSession.Builder(this,player).setSessionActivity(activity).setCallback(object: MediaSession.Callback {
            override fun onPlayerInteractionFinished(session: MediaSession,controllerInfo: MediaSession.ControllerInfo,playerCommands: Player.Commands) {
                // An idempotent pause produces no Player event, but still revokes delayed resume.
                if(playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) cancelGapResume()
            }
            override fun onConnect(session: MediaSession,controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                if(controller.packageName!=packageName && !controller.isTrusted) return MediaSession.ConnectionResult.reject()
                val commands=MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                if(controller.packageName==packageName) listOf("timer.start","timer.cancel","queue.reshuffle").forEach { commands.add(SessionCommand(it,Bundle.EMPTY)) }
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(commands.build()).build()
            }
            override fun onCustomCommand(session: MediaSession,controller: MediaSession.ControllerInfo,command: SessionCommand,args: Bundle): ListenableFuture<SessionResult> {
                if(controller.packageName!=packageName) return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
                when(command.customAction) {
                    "queue.reshuffle" -> {
                        if(player.mediaItemCount>1) {
                            val resumeAfterShuffle=gapResume?.isActive==true
                            transition.cancel();cancelGapResume()
                            val current=player.currentMediaItemIndex
                            val rest=(0 until player.mediaItemCount).filter {it!=current}.map(player::getMediaItemAt)
                            var shuffled=rest.shuffled()
                            if(shuffled==rest && rest.size>1) shuffled=rest.drop(1)+rest.first()
                            // Move the existing playing source; replacing only other sources keeps its decoder/position.
                            player.shuffleModeEnabled=false
                            if(current>0) player.moveMediaItem(current,0)
                            player.replaceMediaItems(1,player.mediaItemCount,shuffled)
                            if(resumeAfterShuffle && player.playbackSuppressionReason==Player.PLAYBACK_SUPPRESSION_REASON_NONE) player.play()
                            queueStore.save(player)
                        }
                    }
                    "timer.start" -> {
                        val duration=args.getLong("duration")
                        if(duration !in 1000..86_400_000) return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                        timer=SleepDeadline(SystemClock.elapsedRealtime(),duration,args.getBoolean("finish")); timerTrack=player.currentMediaItem?.mediaId
                        timerStore.save(timer!!,timerTrack)
                    }
                    "timer.cancel" -> { timer=null; timerTrack=null;timerStore.clear() }
                    else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                }
                publishTimer()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }).build()
        queueStore.restore()?.let { saved ->
            player.setMediaItems(saved.items,saved.index,saved.position)
            player.repeatMode=saved.repeat; player.shuffleModeEnabled=saved.shuffle; player.setPlaybackSpeed(saved.speed)
            // Preparation restores metadata and position, never automatically resumes sound.
            player.prepare()
        }
        timerStore.restore()?.let {timer=it.deadline;timerTrack=it.entryId;publishTimer()}
        transition=TransitionCoordinator(scope,{player},{createPlayer(true)},::disposePlayer) {next->
            val old=player
            old.removeListener(playerListener)
            player=next
            PlayerAudioSignal.active=processors[next]?.signal
            next.addListener(playerListener)
            session?.setPlayer(next)
            disposePlayer(old)
            applySettings()
            history.transition(next.currentMediaItem?.mediaId)
            recordPlayback()
            queueStore.save(next)
        }
        playerListener=object: Player.Listener {
            override fun onEvents(player: Player,events: Player.Events) {
                if(transition.overlapping && events.containsAny(Player.EVENT_TIMELINE_CHANGED,Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,Player.EVENT_REPEAT_MODE_CHANGED,Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) transition.cancel()
                if(events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION,Player.EVENT_TIMELINE_CHANGED,Player.EVENT_PLAY_WHEN_READY_CHANGED,Player.EVENT_REPEAT_MODE_CHANGED,Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) queueStore.save(player)
                PlayerWidgetProvider.refresh(this@PlaybackService,player)
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?,reason: Int) {
                cancelGapResume()
                if(transition.overlapping) {
                    if(reason==Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason==Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {transition.finish();return} else transition.cancel()
                } else if(reason==Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && !app.collections.state.value.flag("gapless",true) && !app.collections.state.value.flag("crossfade") && player.playWhenReady) {
                    val item=player.currentMediaItem
                    player.pause()
                    val owner=player
                    gapResume=scope.launch {delay(200);gapResume=null;if(player===owner && owner.currentMediaItem===item && owner.playbackSuppressionReason==Player.PLAYBACK_SUPPRESSION_REASON_NONE) owner.play()}
                }
                history.transition(mediaItem?.mediaId)
                recordPlayback()
                val deadline=timer
                if(deadline!=null && deadline.finishCurrentTrack && deadline.remaining(SystemClock.elapsedRealtime())==0L && (timerTrack!=mediaItem?.mediaId || reason==Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)) stopForTimer()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if(playbackState==Player.STATE_READY) applySettings()
                if(playbackState==Player.STATE_ENDED && transition.overlapping) transition.finish()
                if(playbackState==Player.STATE_ENDED && timer?.shouldStop(SystemClock.elapsedRealtime(),true)==true) stopForTimer()
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean,reason: Int) {
                if(!playWhenReady) {transition.cancel();cancelGapResume()}
            }
            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {if(playbackSuppressionReason!=Player.PLAYBACK_SUPPRESSION_REASON_NONE) cancelGapResume()}
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo,newPosition: Player.PositionInfo,reason: Int) {
                if(reason==Player.DISCONTINUITY_REASON_SEEK || reason==Player.DISCONTINUITY_REASON_REMOVE || reason==Player.DISCONTINUITY_REASON_SKIP) {transition.cancel();cancelGapResume()}
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if(isPlaying) recordPlayback()
            }
        }
        player.addListener(playerListener)
        scope.launch {
            app.collections.state.collect {
                applySettings()
            }
        }
        scope.launch {
            var ticks=0
            while(isActive) {
                delay(500)
                val deadline=timer
                if(deadline!=null) {
                    if(deadline.shouldStop(SystemClock.elapsedRealtime(),player.playbackState==Player.STATE_ENDED)) stopForTimer() else publishTimer()
                }
                val settings=app.collections.state.value
                transition.tick(settings.flag("crossfade") && timer?.finishCurrentTrack!=true,(settings.pref("crossfade_seconds","5").toFloatOrNull() ?: 5f).coerceIn(1f,12f))
                if(++ticks%10==0 && player.isPlaying) {queueStore.save(player);PlayerWidgetProvider.refreshProgress(this@PlaybackService,player)}
            }
        }
    }
    private fun cancelGapResume() {gapResume?.cancel();gapResume=null}
    private fun recordPlayback() {
        val item=player.currentMediaItem ?: return
        if(player.isPlaying && history.playing(item.mediaId)) {
            val uri=item.mediaMetadata.extras?.getString("source_uri") ?: item.localConfiguration?.uri?.toString() ?: return
            scope.launch {app.collections.recordPlay(uri)}
        }
    }
    private fun createPlayer(secondary: Boolean): ExoPlayer {
        val processor=EqProcessor()
        val factory=object: DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context,enableFloatOutput: Boolean,enableAudioTrackPlaybackParams: Boolean): AudioSink = DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(processor)).build()
        }
        val result=ExoPlayer.Builder(this,factory).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).build()
        processors[result]=processor;effects[result]=AudioEffects()
        result.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),!secondary && !app.collections.state.value.flag("simultaneous"))
        result.setHandleAudioBecomingNoisy(app.collections.state.value.flag("pause_detach",true))
        applyGains(processor)
        return result
    }
    private fun applyGains(processor: EqProcessor) {
        val settings=app.collections.state.value
        val raw=settings.pref("eq_gains").split(',').map {it.toFloatOrNull()?.takeIf(Float::isFinite)?.coerceIn(-12f,12f) ?: 0f}
        processor.setGains(List(10) {index->if(!settings.flag("eq_enabled") || (settings.pref("eq_bands")=="5" && index%2!=0)) 0f else raw.getOrElse(index) {0f}})
    }
    private fun applySettings() {
        val settings=app.collections.state.value
        player.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),!settings.flag("simultaneous"))
        player.setHandleAudioBecomingNoisy(settings.flag("pause_detach",true))
        processors.values.forEach(::applyGains)
        effectsStatus=effects[player]?.apply(player,settings)
        publishTimer()
    }
    private fun disposePlayer(value: ExoPlayer) {effects.remove(value)?.release();processors.remove(value);value.release()}
    private fun publishTimer() { session?.setSessionExtras(Bundle().apply { putLong("timer_remaining",timer?.remaining(SystemClock.elapsedRealtime()) ?: -1); putBoolean("timer_finish",timer?.finishCurrentTrack ?: false);putString("effects_status",effectsStatus) }) }
    private fun stopForTimer() { player.pause(); timer=null; timerTrack=null;timerStore.clear();publishTimer() }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo)=session
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        val result=super.onStartCommand(intent,flags,startId)
        if(intent?.action?.startsWith("widget.")==true) cancelGapResume()
        when(intent?.action) {
            "widget.toggle" -> if(player.isPlaying) player.pause() else { if(player.playbackState==Player.STATE_IDLE) player.prepare(); player.play() }
            "widget.favorite" -> player.currentMediaItem?.let {item->val uri=item.mediaMetadata.extras?.getString("source_uri") ?: item.localConfiguration?.uri?.toString();if(uri!=null) scope.launch {app.collections.setFavorite(uri,uri !in app.collections.state.value.favorites);PlayerWidgetProvider.refresh(this@PlaybackService,player)}}
            "widget.next" -> player.seekToNextMediaItem()
            "widget.previous" -> player.seekToPrevious()
        }
        return result
    }
    override fun onDestroy() {
        PlayerAudioSignal.active=null
        cancelGapResume();unregisterReceiver(noisyReceiver)
        queueStore.save(player)
        transition.cancel();scope.cancel();session?.release();session=null;disposePlayer(player)
        super.onDestroy()
    }
}
