package my.music.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.music.model.Track

internal object ForYouPalette {
 val gold=listOf(Color(0xFFFFA600),Color(0xFFFF7300))
 val blue=listOf(Color(0xFF1746C7),Color(0xFF2698FA))
 val violet=listOf(Color(0xFF7813EB),Color(0xFFD315EA))
 val teal=listOf(Color(0xFF006F91),Color(0xFF39BEB2))
 fun mix(title:String)=when(title) {"High Quality"->gold;"Top Artist mix"->blue;"Daily discover"->violet;"Never Played"->teal;"Before 1980"->listOf(Color(0xFF83462D),Color(0xFFD89F67));else->listOf(Color(0xFFAE594B),Color(0xFFCD9674))}
}
@Composable internal fun ForYouSectionTitle(title:String,onOpen:(()->Unit)?=null) {
 Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
  Text(title,Modifier.weight(1f),fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.ExtraBold)
  if(onOpen!=null) IconButton(onClick=onOpen,modifier=Modifier.size(32.dp)) {Icon(Icons.Rounded.ChevronRight,"See all $title",tint=Muted)}
 }
}
@Composable internal fun ForYouPlayBadge(label:String,enabled:Boolean,onPlay:()->Unit,modifier:Modifier=Modifier) {
 IconButton(onClick=onPlay,enabled=enabled,modifier=modifier.size(34.dp).background(Color.White.copy(alpha=if(enabled) .2f else .08f),CircleShape).border(1.dp,Color.White.copy(alpha=.24f),CircleShape)) {Icon(Icons.Rounded.PlayArrow,label,tint=Color.White.copy(alpha=if(enabled) 1f else .35f),modifier=Modifier.size(24.dp))}
}
@Composable internal fun ForYouMixCard(key:String,title:String,detail:String,tracks:List<Track>,colors:List<Color>,onOpen:()->Unit,onPlay:()->Unit,eyebrow:String?=null) {
 Box(Modifier.width(154.dp).height(210.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(colors)).clickable(onClick=onOpen).semantics {contentDescription="Open $key"}) {
  Canvas(Modifier.matchParentSize()) {drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha=.3f),Color.Transparent),Offset(size.width*.35f,size.height*.45f),size.width*.8f))}
  if(eyebrow!=null) {
   tracks.firstOrNull()?.artworkUri?.let {Artwork(it,Modifier.fillMaxSize())}
   Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.65f)))))
   ForYouPlayBadge("Play $key",tracks.isNotEmpty(),onPlay,Modifier.align(Alignment.BottomEnd).padding(end=10.dp,bottom=70.dp))
   Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
    Text(eyebrow,color=Color.White.copy(alpha=.76f),fontSize=12.sp,maxLines=1)
    Text(title,fontSize=19.sp,lineHeight=23.sp,fontWeight=FontWeight.ExtraBold,maxLines=2,overflow=TextOverflow.Ellipsis,color=Color.White)
   }
  } else {
   Box(Modifier.fillMaxWidth().height(156.dp),contentAlignment=Alignment.Center) {
    Text(title,Modifier.fillMaxWidth().padding(horizontal=12.dp),fontSize=26.sp,lineHeight=31.sp,fontWeight=FontWeight.ExtraBold,textAlign=TextAlign.Center,color=Color.White,maxLines=3)
    ForYouPlayBadge("Play $key",tracks.isNotEmpty(),onPlay,Modifier.align(Alignment.BottomEnd).padding(end=8.dp,bottom=8.dp))
   }
   Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(54.dp).background(Color.Black.copy(alpha=.17f)).padding(horizontal=12.dp,vertical=8.dp)) {
    Text(detail,color=Color.White.copy(alpha=.8f),fontSize=12.sp,lineHeight=17.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
   }
  }
 }
}
