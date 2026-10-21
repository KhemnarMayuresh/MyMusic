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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import app.local.musicplayer.playback.PlaybackSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.collections.CollectionState
import kotlin.math.*

/** The preview and live player deliberately use the same visual renderer. */
@Composable internal fun PlayerThemeArtwork(theme:PlayerTheme,uri:String?,modifier:Modifier=Modifier,playing:Boolean=false,entry:String?=null) {
 val motion=rememberPlayerMotion(playing && theme.style in listOf(1,2,4,5,15,17),entry)
 val accent=Color(theme.accent)
 Box(modifier.then(if(theme.style==1) Modifier.rotate(motion.degrees) else Modifier),contentAlignment=Alignment.Center) {
  when(theme.style) {
   1 -> {Canvas(Modifier.fillMaxSize()) {val r=size.minDimension/2;drawCircle(Color(0xFF151518),r);for(i in 1..20) drawCircle(Color(0xFF333338),r*i/21,style=androidx.compose.ui.graphics.drawscope.Stroke(1.5f));drawCircle(accent,r*.36f)};Artwork(uri,Modifier.fillMaxSize(.3f).clip(CircleShape));Box(Modifier.size(10.dp).background(Color.Black,CircleShape))}
   2 -> {Box(Modifier.fillMaxWidth().aspectRatio(1.5f).background(Color(0xFFB4C1B5),RoundedCornerShape(12.dp)).padding(16.dp)) {Artwork(uri,Modifier.matchParentSize().clip(RoundedCornerShape(8.dp)));Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=.2f)));Column {Text("STEREO   •   SIDE A",color=Color.White,style=MaterialTheme.typography.labelSmall);Spacer(Modifier.weight(1f));Row(Modifier.fillMaxWidth().height(58.dp).background(Color(0xFF283331),RoundedCornerShape(30.dp)),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically) {repeat(2) {Canvas(Modifier.size(40.dp).rotate(motion.degrees*2)) {drawCircle(accent);drawCircle(Color(0xFF66746D),style=androidx.compose.ui.graphics.drawscope.Stroke(8.dp.toPx()));for(spoke in 0..5) {val a=spoke*PI/3;drawLine(Color(0xFF283331),center,Offset(center.x+cos(a).toFloat()*size.width*.35f,center.y+sin(a).toFloat()*size.height*.35f),4.dp.toPx())};drawCircle(Color(0xFF283331),size.width*.12f)}}};Spacer(Modifier.weight(1f));Text("MUSIC / HIGH FIDELITY",color=Color.White,style=MaterialTheme.typography.labelSmall)}}}
   3 -> {Canvas(Modifier.fillMaxSize()) {val r=size.minDimension*.45f;for(i in 0..3) drawOval(accent.copy(alpha=.5f),Offset(size.width/2-r,size.height/2-r*(.4f+i*.12f)),Size(r*2,r*(.8f+i*.24f)),style=androidx.compose.ui.graphics.drawscope.Stroke(2f));drawCircle(accent,6f,Offset(size.width*.9f,size.height*.5f))};Artwork(uri,Modifier.fillMaxSize(.58f).clip(CircleShape))}
   4 -> Column(horizontalAlignment=Alignment.CenterHorizontally) {
    Artwork(uri,Modifier.fillMaxSize(.72f).weight(1f).clip(RoundedCornerShape(18.dp)))
    Canvas(Modifier.fillMaxWidth().height(72.dp)) {
     val path=Path();val lower=Path();val step=size.width/42
     fun y(i:Int,sign:Float)=center.y+sign*(2.dp.toPx()+motion.levels[i]*size.height*.44f)
     for(sign in listOf(-1f,1f)) {
      val p=if(sign<0) path else lower;p.moveTo(0f,y(0,sign))
      for(i in 1..42) {val x=i*step;val mid=x-step/2;p.cubicTo(mid,y(i-1,sign),mid,y(i,sign),x,y(i,sign))}
      drawPath(p,accent,style=androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx(),cap=StrokeCap.Round))
     }
    }
   }
   5 -> Box(Modifier.fillMaxSize(.88f).padding(12.dp)) {
    Artwork(uri,Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)))
    Canvas(Modifier.matchParentSize()) {
     val shader=android.graphics.SweepGradient(center.x,center.y,intArrayOf(accent.toArgb(),android.graphics.Color.CYAN,0xFFFF4FE1.toInt(),accent.toArgb()),null)
     shader.setLocalMatrix(android.graphics.Matrix().apply {setRotate(motion.degrees,center.x,center.y)})
     val brush=ShaderBrush(shader)
     for(width in listOf(16f,9f,3f)) drawRoundRect(brush,cornerRadius=androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),alpha=if(width==3f) 1f else .16f,style=androidx.compose.ui.graphics.drawscope.Stroke(width.dp.toPx()))
    }
   }
   6 -> Column(Modifier.fillMaxSize(.85f).rotate(5f).background(Color(0xFFFFF7E8)).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally) {Artwork(uri,Modifier.weight(1f).fillMaxWidth());Text("a little music, a little magic",color=Color.DarkGray,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(vertical=16.dp))}
   7 -> Artwork(uri,Modifier.fillMaxSize(.76f).clip(RoundedCornerShape(12.dp)))
   8 -> {Canvas(Modifier.fillMaxSize()) {drawCircle(Color(0xFFFFCC88),size.minDimension*.38f);for(i in 1..7) drawRect(Color(theme.top),Offset(0f,size.height*(.54f+i*.045f)),Size(size.width,i*1.8f))};Artwork(uri,Modifier.fillMaxSize(.45f).clip(CircleShape))}
   9 -> Box(Modifier.fillMaxSize(.86f).background(Color.White.copy(alpha=.14f),RoundedCornerShape(26.dp)).border(1.dp,Color.White.copy(alpha=.45f),RoundedCornerShape(26.dp)).padding(18.dp)) {Artwork(uri,Modifier.fillMaxSize())}
   11 -> Column(Modifier.fillMaxSize(.9f).background(Color(0xFFE9CCB3),RoundedCornerShape(16.dp)).padding(16.dp)) {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("FM  88 • 108",color=Color(0xFF44354A),style=MaterialTheme.typography.labelSmall);Icon(Icons.Rounded.Radio,null,tint=Color(0xFF44354A))};Artwork(uri,Modifier.weight(1f).fillMaxWidth().padding(vertical=12.dp));Canvas(Modifier.fillMaxWidth().height(30.dp)) {for(i in 0..14) drawLine(Color(0xFF44354A),Offset(i*size.width/15,0f),Offset(i*size.width/15,size.height),3f)}}
   14 -> {Canvas(Modifier.fillMaxSize()) {drawRoundRect(accent.copy(alpha=.16f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(40f));val p=Path().apply {moveTo(size.width,0f);lineTo(size.width,size.height);lineTo(0f,size.height);close()};drawPath(p,accent.copy(alpha=.4f))};Artwork(uri,Modifier.fillMaxSize(.7f).clip(CircleShape))}
   15 -> {Canvas(Modifier.fillMaxSize()) {val energy=motion.levels.last();for(i in 1..5) drawCircle(accent.copy(alpha=.06f+i*.025f),size.minDimension*(.17f+i*.055f+energy*.025f),style=androidx.compose.ui.graphics.drawscope.Stroke(2f+i))};Artwork(uri,Modifier.fillMaxSize(.58f).clip(CircleShape))}
   16 -> {Box(Modifier.fillMaxSize(.9f).background(Color(0xFF111111),RoundedCornerShape(8.dp)).padding(horizontal=24.dp,vertical=12.dp)) {Artwork(uri,Modifier.fillMaxSize())};Canvas(Modifier.fillMaxSize(.9f)) {for(i in 0..10) for(x in listOf(size.width*.025f,size.width*.945f)) drawRoundRect(Color.White.copy(alpha=.7f),Offset(x,size.height*(i+.2f)/11),Size(size.width*.03f,size.height*.04f),androidx.compose.ui.geometry.CornerRadius(3f))}}
   17 -> {Canvas(Modifier.fillMaxSize()) {for(i in 0..4) drawCircle(accent.copy(alpha=.15f+i*.12f),size.minDimension*(.36f+i*.025f),style=androidx.compose.ui.graphics.drawscope.Stroke(2f));val a=motion.degrees*PI/180;val radius=size.minDimension*.46f;drawCircle(accent,7.dp.toPx(),Offset(center.x+cos(a).toFloat()*radius,center.y+sin(a).toFloat()*radius))};Artwork(uri,Modifier.fillMaxSize(.64f).clip(CircleShape))}
   else -> Artwork(uri,Modifier.fillMaxSize())
  }
 }
}
@Composable fun ThemeScreen(vm:PlayerViewModel,data:CollectionState,onBack:()->Unit,playerOnly:Boolean=false) {
 var player by rememberSaveable {mutableStateOf(playerOnly)}
 var selected by rememberSaveable {mutableStateOf(data.pref("player_theme","Artwork"))}
 var editing by rememberSaveable {mutableStateOf(false)}
 var draftId by rememberSaveable {mutableStateOf("")}
 var draftName by rememberSaveable {mutableStateOf("")}
 var draftStyle by rememberSaveable {mutableIntStateOf(0)}
 var top by rememberSaveable {mutableStateOf("170C37")}
 var bottom by rememberSaveable {mutableStateOf("593271")}
 var accent by rememberSaveable {mutableStateOf("FFC875")}
 var coverColors by rememberSaveable {mutableStateOf(false)}
 var saving by remember {mutableStateOf(false)}
 var deleting by remember {mutableStateOf(false)}
 val custom=CustomThemes.decode(data.pref(CustomThemes.KEY))
 val options=PlayerThemes.all+custom
 val savedTheme=PlayerThemes.find(selected,data.pref(CustomThemes.KEY))
 val theme=if(editing) PlayerTheme(draftName.ifBlank {"Your theme"},draftStyle,CustomThemes.color(top) ?: savedTheme.top,CustomThemes.color(bottom) ?: savedTheme.bottom,CustomThemes.color(accent) ?: savedTheme.accent,draftId,coverColors) else savedTheme
 fun edit(existing:PlayerTheme?) {
  val source=existing ?: savedTheme
  draftId=existing?.id ?: "custom:${java.util.UUID.randomUUID()}"
  draftName=existing?.name.orEmpty();draftStyle=source.style
  top=CustomThemes.hex(source.top);bottom=CustomThemes.hex(source.bottom);accent=CustomThemes.hex(source.accent)
  coverColors=existing?.coverColors ?: false;player=true;editing=true
 }
 androidx.activity.compose.BackHandler(editing) {editing=false}
 val state by vm.playback.collectAsState()
 val artwork by vm.playingArtwork.collectAsState()
 var demoPosition by rememberSaveable {mutableLongStateOf(76000)}
 var demoPlaying by rememberSaveable {mutableStateOf(false)}
 LaunchedEffect(demoPlaying,state.current) {while(demoPlaying && state.current==null && isActive) {delay(400);demoPosition=(demoPosition+400)%234000}}
 val preview=if(state.current==null) PlaybackSnapshot(duration=234000,position=demoPosition,playing=demoPlaying) else state
 val controller=vm.connection.controller
 val actions=PlayerActions(seek={value->if(state.current==null) demoPosition=value else if(controller?.currentMediaItem?.mediaId==state.current?.mediaId) controller?.seekTo(value)},toggle={if(state.current==null) demoPlaying=!demoPlaying else controller?.let {if(it.isPlaying) it.pause() else it.play()}},previous={controller?.seekToPrevious()},next={controller?.seekToNextMediaItem()},back={if(state.current==null) demoPosition=(demoPosition-10000).coerceAtLeast(0) else controller?.seekBack()},forward={if(state.current==null) demoPosition=(demoPosition+10000).coerceAtMost(234000) else controller?.seekForward()},shuffle={vm.reshuffle()},repeat={controller?.repeatMode=when(state.repeat) {0->2;2->1;else->0}})
 Column(Modifier.fillMaxSize()) {
  Header(if(editing) "Create your theme" else "Themes",{if(editing) editing=false else onBack()})
  if(!editing) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {TextButton(onClick={player=false}) {Text("Library",color=if(!player) Color.White else Muted)};TextButton(onClick={player=true}) {Text("Player",color=if(player) Color.White else Muted)}}
  if(!player) LibraryThemeEditor(vm,data,Modifier.weight(1f))
  else {
   Text(theme.name,Modifier.align(Alignment.CenterHorizontally).padding(8.dp),style=MaterialTheme.typography.titleLarge)
   ThemePhonePreview(theme,preview,artwork,actions,Modifier.weight(1f))
   if(editing) {
    val error=when {
     draftName.isBlank()->"Enter a theme name"
     options.any {it.id!=draftId && it.name.equals(draftName.trim(),true)}->"Choose a different name"
     listOf(top,bottom,accent).any {CustomThemes.color(it)==null}->"Use six hex digits for each color"
     custom.size>=50 && custom.none {it.id==draftId}->"You can save up to 50 themes"
     else->null
    }
    Column(Modifier.fillMaxWidth().imePadding()) {
     Column(Modifier.heightIn(max=230.dp).verticalScroll(rememberScrollState()).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(draftName,{draftName=it.take(40)},label={Text("Theme name")},singleLine=true,modifier=Modifier.fillMaxWidth())
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {PlayerThemes.all.forEach {option->FilterChip(draftStyle==option.style,{draftStyle=option.style},label={Text(option.name)})}}
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
       ThemeColorField("Top color",top,{top=it},Modifier.weight(1f));ThemeColorField("Bottom color",bottom,{bottom=it},Modifier.weight(1f));ThemeColorField("Accent",accent,{accent=it},Modifier.weight(1f))
      }
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text("Use cover colors",Modifier.weight(1f));Switch(coverColors,{coverColors=it})}
      if(error!=null) Text(error,color=Muted,style=MaterialTheme.typography.labelSmall)
     }
     Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick={editing=false},Modifier.weight(1f),enabled=!saving) {Text("Cancel")}
      Button(onClick={
       val value=theme.copy(name=draftName.trim());val next=custom.filterNot {it.id==draftId}+value;saving=true
       vm.work {try {vm.repository.setPreferences(mapOf(CustomThemes.KEY to CustomThemes.encode(next),"player_theme" to value.id));selected=value.id;editing=false} finally {saving=false}}
      },Modifier.weight(1f),enabled=error==null && !saving) {Text("Save and apply")}
     }
    }
   } else {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {options.forEach {option->FilterChip(selected==option.id,{selected=option.id},label={Text(option.name)})}}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
     TextButton(onClick={edit(null)}) {Icon(Icons.Rounded.Add,null);Text("Create theme")}
     if(savedTheme.id.startsWith("custom:")) {TextButton(onClick={edit(savedTheme)}) {Text("Edit")};TextButton(onClick={deleting=true}) {Text("Delete")}}
    }
    Button(onClick={vm.pref("player_theme",selected)},Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,bottom=16.dp),enabled=selected!=data.pref("player_theme","Artwork")) {Text(if(selected==data.pref("player_theme","Artwork")) "Applied" else "Apply")}
   }
  }
 }
 if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("Delete theme?")},text={Text(savedTheme.name)},confirmButton={TextButton(onClick={val id=savedTheme.id;vm.work {vm.repository.setPreferences(buildMap {put(CustomThemes.KEY,CustomThemes.encode(custom.filterNot {it.id==id}));if(data.pref("player_theme")==id) put("player_theme","Artwork")});selected="Artwork"};deleting=false}) {Text("Delete")}},dismissButton={TextButton(onClick={deleting=false}) {Text("Cancel")}})
}

@Composable internal fun ThemeColorField(label:String,value:String,onValue:(String)->Unit,modifier:Modifier) {
 var picking by remember {mutableStateOf(false)}
 Column(modifier) {
  Text(label,style=MaterialTheme.typography.labelSmall,color=Muted)
  OutlinedButton(onClick={picking=true},modifier=Modifier.fillMaxWidth().semantics {contentDescription="Choose ${label.lowercase()}"},contentPadding=PaddingValues(horizontal=6.dp,vertical=8.dp)) {
   Box(Modifier.size(14.dp).background(Color(CustomThemes.color(value) ?: 0xFF170C37),CircleShape));Spacer(Modifier.width(5.dp));Text("#$value",style=MaterialTheme.typography.labelSmall)
  }
 }
 if(picking) ThemeColorPicker(label,value,{picking=false}) {onValue(it);picking=false}
}
@Composable private fun ThemeColorPicker(label:String,value:String,onDismiss:()->Unit,onColor:(String)->Unit) {
 val initial=remember(value) {FloatArray(3).also {android.graphics.Color.colorToHSV((CustomThemes.color(value) ?: 0xFF170C37).toInt(),it)}}
 var hue by remember {mutableFloatStateOf(initial[0])};var saturation by remember {mutableFloatStateOf(initial[1])};var brightness by remember {mutableFloatStateOf(initial[2])}
 var hex by remember {mutableStateOf(value)}
 fun updateHex() {hex=CustomThemes.hex(android.graphics.Color.HSVToColor(floatArrayOf(hue,saturation,brightness)).toLong() and 0xFFFFFFFFL)}
 fun choose(color:Long) {val hsv=FloatArray(3);android.graphics.Color.colorToHSV(color.toInt(),hsv);hue=hsv[0];saturation=hsv[1];brightness=hsv[2];hex=CustomThemes.hex(color)}
 val swatches=listOf("Purple" to 0xFF341151L,"Blue" to 0xFF102B52L,"Teal" to 0xFF10443FL,"Rose" to 0xFF6B2847L,"Gold" to 0xFFFFC875L,"White" to 0xFFFFFFFFL)
 AlertDialog(onDismissRequest=onDismiss,title={Text(label)},text={Column(Modifier.verticalScroll(rememberScrollState())) {
  Box(Modifier.fillMaxWidth().height(48.dp).background(Color(CustomThemes.color(hex) ?: 0xFF170C37),RoundedCornerShape(12.dp)))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {swatches.forEach {(name,color)->IconButton(onClick={choose(color)},modifier=Modifier.size(36.dp).semantics {contentDescription=name}) {Box(Modifier.size(25.dp).background(Color(color),CircleShape).border(1.dp,Color.White.copy(alpha=.5f),CircleShape))}}}
  Text("Hue",style=MaterialTheme.typography.labelSmall);Slider(hue,{hue=it;updateHex()},valueRange=0f..360f)
  Text("Saturation",style=MaterialTheme.typography.labelSmall);Slider(saturation,{saturation=it;updateHex()})
  Text("Brightness",style=MaterialTheme.typography.labelSmall);Slider(brightness,{brightness=it;updateHex()})
  OutlinedTextField(hex,{candidate->hex=candidate.removePrefix("#").filter {it in "0123456789abcdefABCDEF"}.take(6);CustomThemes.color(hex)?.let {color->val hsv=FloatArray(3);android.graphics.Color.colorToHSV(color.toInt(),hsv);hue=hsv[0];saturation=hsv[1];brightness=hsv[2]}},label={Text("Hex color")},prefix={Text("#")},singleLine=true)
 }},confirmButton={TextButton(onClick={onColor(hex.uppercase())},enabled=CustomThemes.color(hex)!=null) {Text("Use color")}},dismissButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
@Composable private fun ThemePhonePreview(theme:PlayerTheme,state:PlaybackSnapshot,artwork:String?,actions:PlayerActions,modifier:Modifier) {
 Box(modifier.fillMaxWidth().padding(horizontal=52.dp,vertical=8.dp),contentAlignment=Alignment.Center) {
  BoxWithConstraints(Modifier.fillMaxHeight().aspectRatio(.5625f,false).clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(theme.top),Color(theme.bottom)))).border(1.dp,Color.White.copy(alpha=.25f),RoundedCornerShape(24.dp)),contentAlignment=Alignment.Center) {
   val scale=minOf(maxWidth/360.dp,maxHeight/640.dp)
   PlayerSurface(theme,artwork,Modifier.requiredSize(360.dp,640.dp).graphicsLayer {scaleX=scale;scaleY=scale}) {resolvedTheme->
    Column(Modifier.fillMaxSize().padding(12.dp)) {
     Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {Icon(Icons.Rounded.KeyboardArrowDown,null);Text("Song  |  Lyrics");Icon(Icons.Rounded.MoreVert,null)}
     ThemedPlayerBody(resolvedTheme,state,state.current?.mediaMetadata?.title?.toString() ?: "Your favorite song",state.current?.mediaMetadata?.artist?.toString() ?: "Music, your way",artwork,false,actions,Modifier.weight(1f))
    }
   }
  }
 }
}
