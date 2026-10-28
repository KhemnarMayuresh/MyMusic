package my.music.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.music.model.Track
import my.music.playback.PlaybackSnapshot
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@Composable internal fun rememberRecommendationDay():String {
 val day by produceState(my.music.library.Recommendations.dayKey()) {while(true) {delay(60000);value=my.music.library.Recommendations.dayKey()}}
 return day
}
@Composable internal fun ForYouScreen(mixes:Map<String,List<Track>>,day:String,onMix:(String)->Unit,onPlay:(List<Track>,Int)->Unit,library:List<Track>,playback:PlaybackSnapshot,onContinue:()->Unit,onToggle:()->Unit,onShuffle:()->Unit,onPlayLibrary:()->Unit,modifier:Modifier=Modifier) {
 fun play(key:String) {mixes[key]?.takeIf {it.isNotEmpty()}?.let {onPlay(it,0)}}
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(top=16.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(32.dp)) {
  item {
   Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    playback.current?.let {current->
     Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha=.06f)).clickable(onClick=onContinue).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
      Artwork(current.mediaMetadata.artworkUri?.toString(),Modifier.size(40.dp))
      Column(Modifier.weight(1f).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
       Row(verticalAlignment=Alignment.CenterVertically) {Text("Continue",Modifier.padding(end=8.dp),fontSize=15.sp);Text(current.mediaMetadata.title?.toString() ?: "Your music",color=Muted,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
       LinearProgressIndicator(progress={if(playback.duration>0) (playback.position.toFloat()/playback.duration).coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth().height(2.dp),color=Muted,trackColor=Color.White.copy(alpha=.1f))
      }
      IconButton(onClick=onToggle,modifier=Modifier.size(32.dp)) {Icon(if(playback.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,if(playback.playing) "Pause current song" else "Resume current song",tint=Muted)}
     }
    }
    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
     Button(onClick=onShuffle,enabled=library.isNotEmpty(),modifier=Modifier.weight(1f).height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White.copy(alpha=.06f),contentColor=Color.White)) {Icon(Icons.Rounded.Shuffle,null,tint=Gold);Spacer(Modifier.width(10.dp));Text("Shuffle",fontSize=18.sp)}
     Button(onClick=onPlayLibrary,enabled=library.isNotEmpty(),modifier=Modifier.weight(1f).height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White.copy(alpha=.06f),contentColor=Color.White)) {Icon(Icons.Rounded.PlayArrow,null,tint=Gold);Spacer(Modifier.width(10.dp));Text("Play",fontSize=18.sp)}
    }
   }
  }
  item {
   Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {ForYouSectionTitle("Top picks",{onMix("Top picks")});Text("Music you keep coming back to",Modifier.padding(horizontal=16.dp),color=Muted,fontSize=14.sp)}
    LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     item {val list=mixes["Your Hits"].orEmpty();ForYouMixCard("Your Hits","Your Hits","",list,ForYouPalette.blue,{onMix("Your Hits")},{play("Your Hits")},"Most Played")}
     item {val list=mixes["Top Album"].orEmpty();ForYouMixCard("Top Album",list.firstOrNull()?.album ?: "Your albums","",list,ForYouPalette.violet,{onMix("Top Album")},{play("Top Album")},"Top Album")}
     item {val list=mixes["Top Artist mix"].orEmpty();ForYouMixCard("Top Artist mix",list.firstOrNull()?.artist ?: "Your artists","",list,ForYouPalette.teal,{onMix("Top Artist mix")},{play("Top Artist mix")},"Top Artist")}
    }
   }
  }
  item {
   val list=mixes["Recently Played"].orEmpty()
   Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
    ForYouSectionTitle("Recently Played",{onMix("Recently Played")})
    if(list.isEmpty()) Text("Play music with For you enabled to build your listening history.",Modifier.padding(horizontal=16.dp),color=Muted)
    else LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     items(list.take(10),key={it.id.uri}) {track->Column(Modifier.width(112.dp).clickable {onPlay(list,list.indexOf(track))}) {
      Box {Artwork(track.artworkUri,Modifier.size(112.dp));ForYouPlayBadge("Play ${track.title}",true,{onPlay(list,list.indexOf(track))},Modifier.align(Alignment.BottomEnd).padding(6.dp))}
      Text(track.title,Modifier.padding(top=8.dp),fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
      Text(track.artist,color=Muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
     }}
    }
   }
  }
  item {
   Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {ForYouSectionTitle("Explore your library");Text("Fresh mixes, updated daily",Modifier.padding(horizontal=16.dp),fontSize=14.sp,color=Muted)}
    LazyRow(Modifier.semantics {contentDescription="Explore library mixes"},contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     items(listOf("High Quality","Top Artist mix","Daily discover","Never Played","Before 1980","Before 2000"),key={it}) {title->
      val list=mixes[title].orEmpty()
      val detail=if(title=="High Quality") "Lossless & 320K songs" else list.take(2).joinToString("\n") {it.title}.ifBlank {if(title=="Top Artist mix") "Your favorite artists" else "No matching songs yet"}
      ForYouMixCard(title,title,detail,list,ForYouPalette.mix(title),{onMix(title)},{play(title)})
     }
    }
   }
  }
  item {
   val list=mixes["Month’s Top Chart"].orEmpty()
   val month=remember(day) {runCatching {SimpleDateFormat("MMMM",Locale.getDefault()).format(SimpleDateFormat("yyyy-MM",Locale.US).parse(day.take(7))!!).uppercase(Locale.getDefault())}.getOrDefault(day.take(7))}
   Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
    ForYouSectionTitle("Month’s Top Chart",{onMix("Month’s Top Chart")})
    Column(Modifier.padding(horizontal=16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF6623AF),Color(0xFF502B84))))) {
     Box(Modifier.fillMaxWidth().height(96.dp).background(Brush.linearGradient(ForYouPalette.violet)).clickable {onMix("Month’s Top Chart")} ) {
      Text(day.take(4),Modifier.padding(start=16.dp),fontSize=56.sp,fontWeight=FontWeight.Bold,color=Color.White.copy(alpha=.14f))
      Text(month,Modifier.align(Alignment.BottomStart).padding(16.dp),fontSize=26.sp,fontWeight=FontWeight.ExtraBold,color=Color.White,maxLines=1)
      ForYouPlayBadge("Play monthly chart",list.isNotEmpty(),{play("Month’s Top Chart")},Modifier.align(Alignment.CenterEnd).padding(end=16.dp))
     }
     if(list.isEmpty()) Text("Play music this month to build your chart.",Modifier.padding(20.dp),color=Color.White.copy(alpha=.7f))
     else list.take(3).forEachIndexed {index,track->Row(Modifier.fillMaxWidth().clickable {onPlay(list,index)}.padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
      Text("${index+1}",Modifier.width(32.dp),fontSize=20.sp,fontWeight=FontWeight.Bold,color=if(index==0) Gold else Color.White.copy(alpha=.7f))
      Artwork(track.artworkUri,Modifier.size(48.dp))
      Column(Modifier.weight(1f).padding(start=12.dp)) {Text(track.title,color=Color.White,fontSize=14.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Text(track.artist,color=Color.White.copy(alpha=.55f),fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
     }}
    }
   }
  }
 }
}
