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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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

@Composable fun CompactPlayer(vm: PlayerViewModel,state: PlaybackSnapshot,onPlayer: ()->Unit,onQueue: ()->Unit) {
    val item=state.current ?: return
    Column(Modifier.background(Color.White.copy(alpha=.06f)).navigationBarsPadding()) {
        LinearProgressIndicator(progress={if(state.duration>0) state.position.toFloat()/state.duration else 0f},Modifier.fillMaxWidth().height(2.dp),color=MaterialTheme.colorScheme.primary,trackColor=Color.White.copy(alpha=.06f))
        Row(Modifier.fillMaxWidth().height(78.dp).semantics {contentDescription="Open player"}.clickable(onClick=onPlayer).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
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
@Composable fun FullPlayer(vm:PlayerViewModel,state:PlaybackSnapshot,current:Track?,favorite:Boolean,lyricsText:String,onBack:()->Unit,onMenu:()->Unit,onQueue:()->Unit,onEqualizer:()->Unit,onTimer:()->Unit,onPlaylist:()->Unit,onLyricsEdit:()->Unit,onTheme:()->Unit) {
 val settings by vm.collections.collectAsStateWithLifecycle()
 val artwork by vm.playingArtwork.collectAsStateWithLifecycle()
 var lyrics by remember {mutableStateOf(false)}
 val controller=vm.connection.controller
 val actions=PlayerActions(
  seek={value->if(controller?.currentMediaItem?.mediaId==state.current?.mediaId) controller?.seekTo(value)},
  toggle={controller?.let {if(it.isPlaying) it.pause() else it.play()}},previous={controller?.seekToPrevious()},next={controller?.seekToNextMediaItem()},back={controller?.seekBack()},forward={controller?.seekForward()},
  shuffle={vm.reshuffle()},repeat={controller?.repeatMode=when(state.repeat) {0->2;2->1;else->0}},favorite={current?.let(vm::favorite)},playlist=onPlaylist,equalizer=onEqualizer,timer=onTimer,queue=onQueue)
 PlayerSurface(PlayerThemes.find(settings.pref("player_theme","Artwork"),settings.pref(CustomThemes.KEY)),artwork ?: current?.artworkUri,Modifier.fillMaxSize()) {theme->
 Column(Modifier.fillMaxSize().padding(horizontal=12.dp)) {
  Row(Modifier.fillMaxWidth().height(60.dp),verticalAlignment=Alignment.CenterVertically) {
   IconButton(onClick=onBack) {Icon(Icons.Rounded.KeyboardArrowDown,"Collapse player")};Spacer(Modifier.weight(1f))
   TextButton(onClick={lyrics=false}) {Text("Song",color=if(!lyrics) Color.White else Muted,fontWeight=FontWeight.Bold)};Text("|",color=Muted);TextButton(onClick={lyrics=true}) {Text("Lyrics",color=if(lyrics) Color.White else Muted)}
   Spacer(Modifier.weight(1f));IconButton(onClick=onTheme) {Icon(Icons.Rounded.Palette,"Player themes")};IconButton(onClick=onMenu) {Icon(Icons.Rounded.MoreVert,"Player actions")}
  }
  if(lyrics) {
   val document=remember(lyricsText) {LrcParser.parse(lyricsText)};val active=document.activeLine(state.position)
   Column(Modifier.weight(1f)) {
    if(document.lines.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {TextButton(onClick=onLyricsEdit) {Text("Add lyrics")}}
    else LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal=24.dp)) {itemsIndexed(document.lines) {_,line->Text(line.text,Modifier.fillMaxWidth().clickable(enabled=line.timeMs!=null) {line.timeMs?.let(actions.seek)}.padding(vertical=14.dp),style=MaterialTheme.typography.headlineSmall,fontWeight=if(line==active) FontWeight.Bold else FontWeight.Normal,color=if(line==active || !document.timed) Color.White else Muted)}}
    TextButton(onClick=onLyricsEdit,modifier=Modifier.align(Alignment.CenterHorizontally)) {Text("Edit / import lyrics")}
   }
   PlayerControlsFooter(state,favorite,actions)
  } else ThemedPlayerBody(theme,state,current?.title ?: state.current?.mediaMetadata?.title?.toString() ?: "Nothing playing",current?.artist ?: state.current?.mediaMetadata?.artist?.toString().orEmpty(),artwork ?: current?.artworkUri,favorite,actions,Modifier.weight(1f))
 }
 }
}
