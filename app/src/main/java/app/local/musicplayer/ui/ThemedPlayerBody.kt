package app.local.musicplayer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.local.musicplayer.playback.PlaybackSnapshot
import kotlinx.coroutines.delay
import kotlin.math.*

class PlayerActions(
 val seek:(Long)->Unit={},val toggle:()->Unit={},val previous:()->Unit={},val next:()->Unit={},
 val back:()->Unit={},val forward:()->Unit={},val shuffle:()->Unit={},val repeat:()->Unit={},
 val favorite:()->Unit={},val playlist:()->Unit={},val equalizer:()->Unit={},val timer:()->Unit={},val queue:()->Unit={},
)

/** Shared by the actual player and its preview; owns gesture/pending-seek state per occurrence. */
@Composable fun ThemeSeek(state:PlaybackSnapshot,ring:Boolean=false,modifier:Modifier=Modifier,onSeek:(Long)->Unit,artwork:@Composable ()->Unit={}) {
 val id=state.current?.mediaId ?: "theme-demo"
 var draft by remember(id) {mutableStateOf<Long?>(null)}
 var pending by remember(id) {mutableStateOf<Long?>(null)}
 val seek by rememberUpdatedState(onSeek)
 val duration=state.duration.coerceAtLeast(0)
 val position=draft ?: pending ?: state.position
 val fraction=SeekMath.fraction(position,duration)
 val accent=MaterialTheme.colorScheme.primary
 val foreground=LocalContentColor.current
 var width by remember {mutableIntStateOf(0)}
 var labelWidth by remember {mutableIntStateOf(0)}
 fun commit(value:Long) {if(duration>0) {pending=value;seek(value)};draft=null}
 fun at(point:Offset,w:Float,h:Float)=SeekMath.position(if(ring) SeekMath.ringFraction(point.x,point.y,w,h) else if(w>0) point.x/w else 0f,duration)
 LaunchedEffect(state.position,pending) {pending?.let {if(abs(state.position-it)<1000) pending=null}}
 LaunchedEffect(pending) {if(pending!=null) {delay(2000);pending=null}}
 val gestures=Modifier.pointerInput(id,duration,ring) {detectTapGestures {if(duration>0) commit(at(it,size.width.toFloat(),size.height.toFloat()))}}
  .pointerInput(id,duration,ring) {detectDragGestures(onDragStart={if(duration>0) draft=at(it,size.width.toFloat(),size.height.toFloat())},onDragEnd={draft?.let(::commit)},onDragCancel={draft=null},onDrag={change,_->if(duration>0) {change.consume();draft=at(change.position,size.width.toFloat(),size.height.toFloat())}})}
 Box(modifier.onSizeChanged {width=it.width}.semantics {
  contentDescription="Song progress"
  stateDescription="${timeLabel(position)} / ${timeLabel(duration)}"
  progressBarRangeInfo=ProgressBarRangeInfo(fraction,0f..1f)
  if(duration>0) setProgress {commit(SeekMath.position(it,duration));true} else disabled()
 }.then(gestures),contentAlignment=Alignment.Center) {
  if(ring) {
   artwork()
   Canvas(Modifier.fillMaxSize().padding(4.dp)) {
    val diameter=size.minDimension-12.dp.toPx();val origin=Offset((size.width-diameter)/2,(size.height-diameter)/2)
    drawArc(foreground.copy(alpha=.3f),-90f,360f,false,origin,Size(diameter,diameter),style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
    drawArc(accent,-90f,360f*fraction,false,origin,Size(diameter,diameter),style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(),cap=StrokeCap.Round))
    val radians=(fraction*360-90)*PI/180
    drawCircle(accent,6.dp.toPx(),Offset(size.width/2+cos(radians).toFloat()*diameter/2,size.height/2+sin(radians).toFloat()*diameter/2))
   }
   Text(timeLabel(position),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold,color=Color.White,modifier=Modifier.background(Color.Black.copy(alpha=.24f),CircleShape).padding(8.dp))
  } else {
   Canvas(Modifier.fillMaxWidth().height(4.dp)) {drawLine(foreground.copy(alpha=.25f),Offset(0f,size.height/2),Offset(size.width,size.height/2),3.dp.toPx(),StrokeCap.Round);drawLine(accent,Offset(0f,size.height/2),Offset(size.width*fraction,size.height/2),3.dp.toPx(),StrokeCap.Round)}
   Text("${timeLabel(position)}/${timeLabel(duration)}",color=Color(0xFF20222B),style=MaterialTheme.typography.labelSmall,modifier=Modifier.align(Alignment.CenterStart).offset {IntOffset((width*fraction-labelWidth/2).toInt().coerceIn(0,(width-labelWidth).coerceAtLeast(0)),0)}.onSizeChanged {labelWidth=it.width}.background(Color.White,CircleShape).padding(horizontal=7.dp,vertical=3.dp))
  }
 }
}
@Composable private fun Timeline(state:PlaybackSnapshot,actions:PlayerActions) {
 Row(verticalAlignment=Alignment.CenterVertically) {
  IconButton(onClick=actions.back) {Icon(Icons.Rounded.Replay10,"Back 10 seconds")}
  ThemeSeek(state,modifier=Modifier.weight(1f).height(48.dp),onSeek=actions.seek)
  IconButton(onClick=actions.forward) {Icon(Icons.Rounded.Forward10,"Forward 10 seconds")}
 }
}
@Composable private fun TitleBlock(title:String,artist:String,center:Boolean=true) {
 Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),horizontalAlignment=if(center) Alignment.CenterHorizontally else Alignment.Start) {
  Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
  Text(artist,color=LocalContentColor.current.copy(alpha=.65f),maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=4.dp))
 }
}
@Composable private fun ActionStrip(favorite:Boolean,actions:PlayerActions,timerActive:Boolean=false) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
  IconButton(onClick=actions.favorite) {Icon(if(favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favorite",tint=if(favorite) MaterialTheme.colorScheme.primary else LocalContentColor.current)}
  IconButton(onClick=actions.playlist) {Icon(Icons.Rounded.PlaylistAdd,"Add to playlist")}
  IconButton(onClick=actions.equalizer) {Icon(Icons.Rounded.Tune,"Equalizer")}
  IconButton(onClick=actions.timer) {Icon(Icons.Rounded.Alarm,"Sleep timer",tint=if(timerActive) MaterialTheme.colorScheme.primary else LocalContentColor.current)}
  IconButton(onClick=actions.queue) {Icon(Icons.Rounded.QueueMusic,"Playing queue")}
 }
}
@Composable private fun Transport(state:PlaybackSnapshot,actions:PlayerActions) {
 Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically) {
  IconButton(onClick=actions.shuffle) {Icon(Icons.Rounded.Shuffle,"Shuffle",tint=if(state.shuffle) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha=.6f))}
  IconButton(onClick=actions.previous) {Icon(Icons.Rounded.SkipPrevious,"Previous",Modifier.size(32.dp))}
  FilledIconButton(onClick=actions.toggle,modifier=Modifier.size(64.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=MaterialTheme.colorScheme.primary,contentColor=Color(0xFF151325))) {Icon(if(state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if(state.playing) "Pause" else "Play",Modifier.size(38.dp))}
  IconButton(onClick=actions.next) {Icon(Icons.Rounded.SkipNext,"Next",Modifier.size(32.dp))}
  IconButton(onClick=actions.repeat) {Icon(if(state.repeat==1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,"Repeat",tint=if(state.repeat!=0) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha=.6f))}
 }
}
@Composable fun ThemedPlayerBody(theme:PlayerTheme,state:PlaybackSnapshot,title:String,artist:String,uri:String?,favorite:Boolean,actions:PlayerActions,modifier:Modifier=Modifier) {
 Column(modifier.fillMaxWidth().semantics {contentDescription="Player layout: ${theme.layout.name}"}) {
  if(theme.layout==PlayerLayout.HEADER_PROGRESS) {TitleBlock(title,artist,false);Timeline(state,actions)}
  BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
   if(theme.layout==PlayerLayout.IMMERSIVE) {
    Artwork(uri,Modifier.fillMaxSize())
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(theme.bottom)))))
    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(8.dp)) {TitleBlock(title,artist,false);ActionStrip(favorite,actions,state.timerRemaining>=0)}
   } else {
    val artworkSize=minOf(maxWidth-24.dp,maxHeight-8.dp,340.dp).coerceAtLeast(1.dp)
    if(theme.layout==PlayerLayout.RING) ThemeSeek(state,true,Modifier.size(artworkSize),actions.seek) {Artwork(uri,Modifier.fillMaxSize(.82f).clip(CircleShape))}
    else PlayerThemeArtwork(theme,uri,Modifier.size(artworkSize),state.playing,state.current?.mediaId)
   }
  }
  val light=theme.layout==PlayerLayout.LIGHT_PANEL
  CompositionLocalProvider(LocalContentColor provides if(light) Color(0xFF23212D) else Color.White) {
   Column(Modifier.fillMaxWidth().then(if(light) Modifier.background(Color.White).padding(horizontal=8.dp) else Modifier)) {
    if(theme.layout!=PlayerLayout.HEADER_PROGRESS && theme.layout!=PlayerLayout.IMMERSIVE) TitleBlock(title,artist,theme.layout!=PlayerLayout.INLINE_ACTIONS)
    if(theme.layout!=PlayerLayout.IMMERSIVE) ActionStrip(favorite,actions,state.timerRemaining>=0)
    TimerStatus(state)
    if(theme.layout!=PlayerLayout.RING && theme.layout!=PlayerLayout.HEADER_PROGRESS) Timeline(state,actions)
    Transport(state,actions)
   }
  }
 }
}

@Composable fun PlayerControlsFooter(state:PlaybackSnapshot,favorite:Boolean,actions:PlayerActions) {
 ActionStrip(favorite,actions,state.timerRemaining>=0);TimerStatus(state);Timeline(state,actions);Transport(state,actions)
}

@Composable private fun TimerStatus(state:PlaybackSnapshot) {
 if(state.timerRemaining>=0) Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {Text(if(state.timerRemaining==0L && state.timerFinish) "Stopping after this song" else "Sleep in ${timeLabel(state.timerRemaining)}",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelSmall)}
}
