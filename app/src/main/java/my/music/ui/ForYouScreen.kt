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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.music.model.Track
import kotlinx.coroutines.delay

@Composable internal fun rememberRecommendationDay():String {
 val day by produceState(my.music.library.Recommendations.dayKey()) {
  while(true) {delay(60000);value=my.music.library.Recommendations.dayKey()}
 }
 return day
}
@Composable internal fun ForYouScreen(mixes:Map<String,List<Track>>,day:String,onMix:(String)->Unit,onPlay:(List<Track>,Int)->Unit,modifier:Modifier=Modifier) {
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item {Text("For you",Modifier.padding(horizontal=20.dp,vertical=12.dp),style=MaterialTheme.typography.headlineMedium)}
  for(title in listOf("Top picks","Recently Played")) item {
   val tracks=mixes[title].orEmpty()
   Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);TextButton(onClick={onMix(title)}) {Text("See all")}}
   if(tracks.isEmpty()) Text(if(title=="Recently Played") "Your listening history will appear here as you play music with For you enabled." else "Add songs to your library to get started.",Modifier.padding(horizontal=20.dp),color=Muted)
   else LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    items(tracks.take(8),key={it.id.uri}) {track->Column(Modifier.width(132.dp).clickable {onPlay(tracks,tracks.indexOf(track))}) {
     Artwork(track.artworkUri,Modifier.size(132.dp).clip(RoundedCornerShape(16.dp)))
     Text(track.title,Modifier.padding(top=8.dp),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleSmall)
     Text(track.artist,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
    }}
   }
  }
  item {Text("Explore your library",Modifier.padding(horizontal=20.dp,vertical=12.dp),style=MaterialTheme.typography.titleLarge)}
  for(title in listOf("Top Artist mix","Daily discover","Never Played","Before 1980","Before 2000","High Quality")) item {
   val tracks=mixes[title].orEmpty()
   val description=when(title) {
    "Top Artist mix"->tracks.firstOrNull()?.artist ?: "Play music to find your artist mix"
    "Daily discover"->"A fresh selection each day"
    "Never Played"->"Not played since recommendation tracking began or was cleared"
    "Before 1980"->"Songs tagged with a year before 1980"
    "Before 2000"->"Songs tagged with a year before 2000"
    else->"Lossless files or at least 320 kbps, when metadata is available"
   }
   Card(onClick={onMix(title)},modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp),colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=.07f))) {
    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
     Artwork(tracks.firstOrNull()?.artworkUri,Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
     Column(Modifier.weight(1f).padding(horizontal=14.dp)) {Text(title,style=MaterialTheme.typography.titleMedium);Text(description,color=Muted,style=MaterialTheme.typography.bodySmall);Text("${tracks.size} songs",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)}
     Icon(Icons.Rounded.ChevronRight,null)
    }
   }
  }
  item {
   val tracks=mixes["Month’s Top Chart"].orEmpty()
   Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {Column(Modifier.weight(1f)) {Text("Month’s Top Chart",style=MaterialTheme.typography.titleLarge);Text(day.take(7),color=Muted)};TextButton(onClick={onMix("Month’s Top Chart")}) {Text("See all")}}
   if(tracks.isEmpty()) Text("Play music with For you enabled to build this month’s chart.",Modifier.padding(horizontal=20.dp),color=Muted)
   else tracks.take(5).forEachIndexed {index,track->Row(Modifier.fillMaxWidth().clickable {onPlay(tracks,index)}.padding(horizontal=20.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {Text("${index+1}",Modifier.width(28.dp),color=MaterialTheme.colorScheme.primary);Artwork(track.artworkUri,Modifier.size(48.dp));Column(Modifier.weight(1f).padding(start=12.dp)) {Text(track.title,maxLines=1,overflow=TextOverflow.Ellipsis);Text(track.artist,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)}}}
  }
 }
}
