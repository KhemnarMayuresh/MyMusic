package app.local.musicplayer.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import app.local.musicplayer.playback.PlaybackSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.collections.CollectionState
import kotlin.math.*

/** The preview and live player deliberately use the same visual renderer. */
@Composable internal fun PlayerThemeArtwork(theme:PlayerTheme,uri:String?,modifier:Modifier=Modifier,playing:Boolean=false,entry:String?=null) {
 val motion=rememberPlayerMotion(playing && theme.style in listOf(1,2,4),entry)
 val accent=Color(theme.accent)
 Box(modifier.then(if(theme.style==1) Modifier.rotate(motion.degrees) else Modifier),contentAlignment=Alignment.Center) {
  when(theme.style) {
   1 -> {Canvas(Modifier.fillMaxSize()) {val r=size.minDimension/2;drawCircle(Color(0xFF151518),r);for(i in 1..20) drawCircle(Color(0xFF333338),r*i/21,style=androidx.compose.ui.graphics.drawscope.Stroke(1.5f));drawCircle(accent,r*.36f)};Artwork(uri,Modifier.fillMaxSize(.3f).clip(CircleShape));Box(Modifier.size(10.dp).background(Color.Black,CircleShape))}
   2 -> {Box(Modifier.fillMaxWidth().aspectRatio(1.5f).background(Color(0xFFB4C1B5),RoundedCornerShape(12.dp)).padding(16.dp)) {Column {Text("STEREO   •   SIDE A",color=Color(0xFF263B38),style=MaterialTheme.typography.labelSmall);Spacer(Modifier.weight(1f));Row(Modifier.fillMaxWidth().height(58.dp).background(Color(0xFF283331),RoundedCornerShape(30.dp)),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically) {repeat(2) {Canvas(Modifier.size(40.dp).rotate(motion.degrees*2)) {drawCircle(accent);drawCircle(Color(0xFF66746D),style=androidx.compose.ui.graphics.drawscope.Stroke(8.dp.toPx()));for(spoke in 0..5) {val a=spoke*PI/3;drawLine(Color(0xFF283331),center,Offset(center.x+cos(a).toFloat()*size.width*.35f,center.y+sin(a).toFloat()*size.height*.35f),4.dp.toPx())};drawCircle(Color(0xFF283331),size.width*.12f)}}};Spacer(Modifier.weight(1f));Text("MUSIC / HIGH FIDELITY",color=Color(0xFF263B38),style=MaterialTheme.typography.labelSmall)}}}
   3 -> {Canvas(Modifier.fillMaxSize()) {val r=size.minDimension*.45f;for(i in 0..3) drawOval(accent.copy(alpha=.5f),Offset(size.width/2-r,size.height/2-r*(.4f+i*.12f)),Size(r*2,r*(.8f+i*.24f)),style=androidx.compose.ui.graphics.drawscope.Stroke(2f));drawCircle(accent,6f,Offset(size.width*.9f,size.height*.5f))};Artwork(uri,Modifier.fillMaxSize(.58f).clip(CircleShape))}
   4 -> {Column(horizontalAlignment=Alignment.CenterHorizontally) {Artwork(uri,Modifier.fillMaxSize(.55f).weight(1f));Canvas(Modifier.fillMaxWidth().height(64.dp)) {for(i in 0..42) {val h=(.035f+motion.levels[i]*.965f)*size.height;val x=i*size.width/43;drawLine(accent,Offset(x,(size.height-h)/2),Offset(x,(size.height+h)/2),size.width/65)}}}}
   5 -> Box(Modifier.fillMaxSize(.88f).rotate(-5f).border(3.dp,accent,RoundedCornerShape(20.dp)).padding(10.dp)) {Artwork(uri,Modifier.fillMaxSize())}
   6 -> Column(Modifier.fillMaxSize(.85f).rotate(5f).background(Color(0xFFFFF7E8)).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally) {Artwork(uri,Modifier.weight(1f).fillMaxWidth());Text("a little music, a little magic",color=Color.DarkGray,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(vertical=16.dp))}
   7 -> {Canvas(Modifier.fillMaxSize()) {drawLine(Color.White,Offset(size.width*.1f,size.height*.8f),Offset(size.width*.9f,size.height*.8f),2f)};Icon(Icons.Rounded.GraphicEq,null,Modifier.fillMaxSize(.42f),tint=Color.White)}
   8 -> {Canvas(Modifier.fillMaxSize()) {drawCircle(Color(0xFFFFCC88),size.minDimension*.38f);for(i in 1..7) drawRect(Color(theme.top),Offset(0f,size.height*(.54f+i*.045f)),Size(size.width,i*1.8f))};Artwork(uri,Modifier.fillMaxSize(.45f).clip(CircleShape))}
   9 -> Box(Modifier.fillMaxSize(.86f).background(Color.White.copy(alpha=.14f),RoundedCornerShape(26.dp)).border(1.dp,Color.White.copy(alpha=.45f),RoundedCornerShape(26.dp)).padding(18.dp)) {Artwork(uri,Modifier.fillMaxSize())}
   10 -> {Canvas(Modifier.fillMaxSize()) {for(i in 1..5) drawCircle(accent.copy(alpha=.055f*i),size.minDimension*(.54f-i*.05f),Offset(size.width*(.3f+i*.07f),size.height*.5f))};Artwork(uri,Modifier.fillMaxSize(.68f).clip(CircleShape))}
   11 -> Column(Modifier.fillMaxSize(.9f).background(Color(0xFFE9CCB3),RoundedCornerShape(16.dp)).padding(16.dp)) {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("FM  88 • 108",color=Color(0xFF44354A),style=MaterialTheme.typography.labelSmall);Icon(Icons.Rounded.Radio,null,tint=Color(0xFF44354A))};Artwork(uri,Modifier.weight(1f).fillMaxWidth().padding(vertical=12.dp));Canvas(Modifier.fillMaxWidth().height(30.dp)) {for(i in 0..14) drawLine(Color(0xFF44354A),Offset(i*size.width/15,0f),Offset(i*size.width/15,size.height),3f)}}
   else -> Artwork(uri,Modifier.fillMaxSize())
  }
 }
}
@Composable fun ThemeScreen(vm:PlayerViewModel,data:CollectionState,onBack:()->Unit,playerOnly:Boolean=false) {
 var player by rememberSaveable {mutableStateOf(playerOnly)}
 var selected by rememberSaveable {mutableStateOf(data.pref("player_theme","Artwork"))}
 val theme=PlayerThemes.find(selected)
 val state by vm.playback.collectAsState()
 val artwork by vm.playingArtwork.collectAsState()
 var demoPosition by rememberSaveable {mutableLongStateOf(76000)}
 var demoPlaying by rememberSaveable {mutableStateOf(false)}
 LaunchedEffect(demoPlaying,state.current) {while(demoPlaying && state.current==null && isActive) {delay(400);demoPosition=(demoPosition+400)%234000}}
 val preview=if(state.current==null) PlaybackSnapshot(duration=234000,position=demoPosition,playing=demoPlaying) else state
 val controller=vm.connection.controller
 val actions=PlayerActions(seek={value->if(state.current==null) demoPosition=value else if(controller?.currentMediaItem?.mediaId==state.current?.mediaId) controller?.seekTo(value)},toggle={if(state.current==null) demoPlaying=!demoPlaying else controller?.let {if(it.isPlaying) it.pause() else it.play()}},previous={controller?.seekToPrevious()},next={controller?.seekToNextMediaItem()},back={if(state.current==null) demoPosition=(demoPosition-10000).coerceAtLeast(0) else controller?.seekBack()},forward={if(state.current==null) demoPosition=(demoPosition+10000).coerceAtMost(234000) else controller?.seekForward()},shuffle={controller?.shuffleModeEnabled=!state.shuffle},repeat={controller?.repeatMode=when(state.repeat) {0->2;2->1;else->0}})
 Column(Modifier.fillMaxSize()) {
  Header("Themes",onBack)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {TextButton(onClick={player=false}) {Text("Library",color=if(!player) Color.White else Muted)};TextButton(onClick={player=true}) {Text("Player",color=if(player) Color.White else Muted)}}
  if(!player) Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {listOf("Midnight","Ocean","Sunset","Obsidian").forEach {name->val colors=when(name) {"Ocean"->listOf(Color(0xFF061824),Color(0xFF123B45));"Sunset"->listOf(Color(0xFF261125),Color(0xFF472735));"Obsidian"->listOf(Color.Black,Color.DarkGray);else->listOf(Ink,Color(0xFF35134D))};Row(Modifier.fillMaxWidth().padding(12.dp).height(85.dp).background(Brush.horizontalGradient(colors),RoundedCornerShape(14.dp)).clickable {vm.pref("theme",name)}.padding(20.dp),verticalAlignment=Alignment.CenterVertically) {Text(name,Modifier.weight(1f));if(data.pref("theme","Midnight")==name) Icon(Icons.Rounded.CheckCircle,null)}}}
  else {
   Text(theme.name,Modifier.align(Alignment.CenterHorizontally).padding(8.dp),style=MaterialTheme.typography.titleLarge)
   Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal=52.dp,vertical=8.dp),contentAlignment=Alignment.Center) {
    BoxWithConstraints(Modifier.fillMaxHeight().aspectRatio(.5625f,false).clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(theme.top),Color(theme.bottom)))).border(1.dp,Color.White.copy(alpha=.25f),RoundedCornerShape(24.dp)),contentAlignment=Alignment.Center) {
     val scale=minOf(maxWidth/360.dp,maxHeight/640.dp)
     PlayerSurface(theme,artwork,Modifier.requiredSize(360.dp,640.dp).graphicsLayer {scaleX=scale;scaleY=scale}) {resolvedTheme->
      Column(Modifier.fillMaxSize().padding(12.dp)) {
       Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {Icon(Icons.Rounded.KeyboardArrowDown,null);Text("Song  |  Lyrics");Icon(Icons.Rounded.MoreVert,null)}
       ThemedPlayerBody(resolvedTheme,preview,state.current?.mediaMetadata?.title?.toString() ?: "Your favorite song",state.current?.mediaMetadata?.artist?.toString() ?: "Music, your way",artwork,false,actions,Modifier.weight(1f))
      }
     }
    }
   }
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {PlayerThemes.all.forEach {option->FilterChip(selected==option.name,{selected=option.name},label={Text(option.name)})}}
   Button(onClick={vm.pref("player_theme",selected)},Modifier.fillMaxWidth().padding(20.dp),enabled=selected!=data.pref("player_theme","Artwork")) {Text(if(selected==data.pref("player_theme","Artwork")) "Applied" else "Apply")}
  }
 }
}
