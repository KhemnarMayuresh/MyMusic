@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.local.musicplayer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.model.Track
import app.local.musicplayer.lyrics.LrcParser
import app.local.musicplayer.playback.PlaybackSnapshot
import androidx.media3.common.Player

@Composable fun CompactPlayer(vm: PlayerViewModel,state: PlaybackSnapshot,onPlayer: ()->Unit,onQueue: ()->Unit) {
    val item=state.current ?: return
    Column(Modifier.background(Color.White.copy(alpha=.06f)).navigationBarsPadding()) {
        LinearProgressIndicator(progress={if(state.duration>0) state.position.toFloat()/state.duration else 0f},Modifier.fillMaxWidth().height(2.dp),color=MaterialTheme.colorScheme.primary,trackColor=Color.White.copy(alpha=.06f))
        Row(Modifier.fillMaxWidth().height(78.dp).clickable(onClick=onPlayer).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
            Artwork(item.mediaMetadata.artworkUri?.toString(),Modifier.size(52.dp).clip(CircleShape))
            Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                Text(item.mediaMetadata.title?.toString() ?: "Unknown song",maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Bold)
                Text(item.mediaMetadata.artist?.toString().orEmpty(),maxLines=1,overflow=TextOverflow.Ellipsis,color=Muted,style=MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick={vm.connection.controller?.let { if(it.isPlaying) it.pause() else it.play() }}) {Icon(if(state.playing) Icons.Rounded.PauseCircleOutline else Icons.Rounded.PlayCircleOutline,if(state.playing) "Pause" else "Play",Modifier.size(38.dp))}
            IconButton(onClick=onQueue) {Icon(Icons.Rounded.QueueMusic,"Playing queue")}
        }
    }
}
@Composable fun FullPlayer(vm: PlayerViewModel,state: PlaybackSnapshot,current: Track?,favorite: Boolean,lyricsText: String,onBack: ()->Unit,onMenu: ()->Unit,onQueue: ()->Unit,onEqualizer: ()->Unit,onTimer: ()->Unit,onPlaylist: ()->Unit,onLyricsEdit: ()->Unit,onTheme: ()->Unit) {
    val settings by vm.collections.collectAsStateWithLifecycle()
    var lyrics by remember { mutableStateOf(false) }
    var seek by remember { mutableStateOf<Float?>(null) }
    val controller=vm.connection.controller
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp)) {
        Row(Modifier.fillMaxWidth().height(60.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack) {Icon(Icons.Rounded.KeyboardArrowDown,"Collapse player")}
            Spacer(Modifier.weight(1f))
            TextButton(onClick={lyrics=false}) {Text("Song",color=if(!lyrics) Color.White else Muted,fontWeight=FontWeight.Bold)}
            Text("|",color=Muted)
            TextButton(onClick={lyrics=true}) {Text("Lyrics",color=if(lyrics) Color.White else Muted)}
            Spacer(Modifier.weight(1f));IconButton(onClick=onTheme) {Icon(Icons.Rounded.Palette,"Player themes")};IconButton(onClick=onMenu) {Icon(Icons.Rounded.MoreVert,"Player actions")}
        }
        if(lyrics) {
            val document=remember(lyricsText) { LrcParser.parse(lyricsText) }
            val active=document.activeLine(state.position)
            Column(Modifier.weight(1f)) {
                if(document.lines.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {TextButton(onClick=onLyricsEdit) {Text("Add lyrics")}}
                else LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal=24.dp)) {itemsIndexed(document.lines) {_,line ->
                    Text(line.text,Modifier.fillMaxWidth().clickable(enabled=line.timeMs!=null) {line.timeMs?.let {controller?.seekTo(it)}}.padding(vertical=14.dp),style=MaterialTheme.typography.headlineSmall,fontWeight=if(line==active) FontWeight.Bold else FontWeight.Normal,color=if(line==active || !document.timed) Color.White else Muted)
                } }
                TextButton(onClick=onLyricsEdit,modifier=Modifier.align(Alignment.CenterHorizontally)) {Text("Edit / import lyrics")}
            }
        } else Column(Modifier.weight(1f).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            BoxWithConstraints(Modifier.fillMaxWidth(.84f).weight(1f,false),contentAlignment=Alignment.Center) {
                val size=minOf(maxWidth,maxHeight,340.dp)
                PlayerThemeArtwork(PlayerThemes.find(settings.pref("player_theme","Artwork")),current?.artworkUri ?: state.current?.mediaMetadata?.artworkUri?.toString(),Modifier.size(size))
            }
            Spacer(Modifier.height(24.dp))
            Text(current?.title ?: state.current?.mediaMetadata?.title?.toString() ?: "Nothing playing",Modifier.padding(horizontal=8.dp),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(current?.artist ?: state.current?.mediaMetadata?.artist?.toString().orEmpty(),color=Muted,modifier=Modifier.padding(top=8.dp,bottom=24.dp))
        }
        Row(Modifier.fillMaxWidth().padding(vertical=14.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            IconButton(onClick={current?.let(vm::favorite)},enabled=current!=null) {Icon(if(favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favorite",tint=if(favorite) MaterialTheme.colorScheme.primary else Color.White)}
            IconButton(onClick=onPlaylist) {Icon(Icons.Rounded.PlaylistAdd,"Add to playlist")}
            IconButton(onClick=onEqualizer) {Icon(Icons.Rounded.Tune,"Equalizer")}
            IconButton(onClick=onTimer) {Icon(Icons.Rounded.Alarm,"Sleep timer",tint=if(state.timerRemaining>=0) MaterialTheme.colorScheme.primary else Color.White)}
            IconButton(onClick=onQueue) {Icon(Icons.Rounded.QueueMusic,"Playing queue")}
        }
        if(state.timerRemaining>=0) Text(if(state.timerRemaining==0L && state.timerFinish) "Stopping after this song" else "Sleep in ${timeLabel(state.timerRemaining)}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,modifier=Modifier.align(Alignment.CenterHorizontally))
        Row(verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={controller?.seekBack()}) {Icon(Icons.Rounded.Replay10,"Back 10 seconds")}
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(timeLabel(seek?.toLong() ?: state.position),style=MaterialTheme.typography.labelSmall);Text(timeLabel(state.duration),style=MaterialTheme.typography.labelSmall,color=Muted)}
                Slider(value=seek ?: state.position.toFloat().coerceAtMost(state.duration.toFloat()),onValueChange={seek=it},onValueChangeFinished={seek?.let {controller?.seekTo(it.toLong())};seek=null},valueRange=0f..state.duration.toFloat().coerceAtLeast(1f))
            }
            IconButton(onClick={controller?.seekForward()}) {Icon(Icons.Rounded.Forward10,"Forward 10 seconds")}
        }
        Row(Modifier.fillMaxWidth().padding(top=6.dp,bottom=24.dp),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={controller?.shuffleModeEnabled=!state.shuffle}) {Icon(Icons.Rounded.Shuffle,"Shuffle",tint=if(state.shuffle) MaterialTheme.colorScheme.primary else Muted)}
            IconButton(onClick={controller?.seekToPrevious()}) {Icon(Icons.Rounded.SkipPrevious,"Previous",Modifier.size(36.dp))}
            FilledIconButton(onClick={controller?.let {if(it.isPlaying) it.pause() else it.play()}},modifier=Modifier.size(76.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color.White,contentColor=Ink)) {Icon(if(state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if(state.playing) "Pause" else "Play",Modifier.size(44.dp))}
            IconButton(onClick={controller?.seekToNextMediaItem()}) {Icon(Icons.Rounded.SkipNext,"Next",Modifier.size(36.dp))}
            IconButton(onClick={controller?.repeatMode=when(state.repeat) {Player.REPEAT_MODE_OFF->Player.REPEAT_MODE_ALL;Player.REPEAT_MODE_ALL->Player.REPEAT_MODE_ONE;else->Player.REPEAT_MODE_OFF}}) {Icon(if(state.repeat==Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,"Repeat",tint=if(state.repeat!=0) MaterialTheme.colorScheme.primary else Muted)}
        }
    }
}
