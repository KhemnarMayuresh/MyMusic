package my.music.library

import my.music.collections.RecommendationPlay
import my.music.collections.MonthPlay
import my.music.model.Track
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** Recommendations use only the visible library and feature-specific aggregates. */
object Recommendations {
 fun dayKey(now:Long=System.currentTimeMillis())=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date(now))
 fun build(tracks:List<Track>,plays:List<RecommendationPlay>,months:List<MonthPlay>,day:String=dayKey(),favorites:Set<String> = emptySet()):Map<String,List<Track>> {
  val library=tracks.distinctBy {it.id.uri};val byUri=library.associateBy {it.id.uri};val history=plays.filter {it.trackUri in byUri}.associateBy {it.trackUri}
  val stable=library.sortedBy {it.id.uri}
  val daily=stable.shuffled(Random(day.hashCode()))
  val ranked=library.filter {it.id.uri in history}.sortedWith(compareByDescending<Track> {history[it.id.uri]?.count ?: 0}.thenByDescending {history[it.id.uri]?.lastPlayed ?: 0}.thenBy {it.id.uri})
  val artist=library.filter {it.artist.isNotBlank() && it.artist!="Unknown artist"}.groupBy {it.artist}.entries.filter {e->e.value.any {it.id.uri in history}}.sortedWith(compareByDescending<Map.Entry<String,List<Track>>> {e->e.value.sumOf {history[it.id.uri]?.count?.toLong() ?: 0L}}.thenByDescending {it.value.size}.thenBy {it.key}).firstOrNull()
  val chart=months.filter {it.month==day.take(7) && it.trackUri in byUri}.sortedWith(compareByDescending<MonthPlay> {it.count}.thenBy {it.trackUri}).mapNotNull {byUri[it.trackUri]}
  return linkedMapOf(
   "Top picks" to (ranked+daily.filter {it.id.uri in favorites}+daily).distinctBy {it.id.uri}.take(50),
   "Recently Played" to library.filter {it.id.uri in history}.sortedWith(compareByDescending<Track> {history[it.id.uri]?.lastPlayed ?: 0}.thenBy {it.id.uri}).take(50),
   "Top Artist mix" to (artist?.value?.sortedWith(compareByDescending<Track> {history[it.id.uri]?.count ?: 0}.thenBy {it.title}) ?: emptyList()),
   "Daily discover" to daily.sortedBy {history[it.id.uri]?.count ?: 0}.take(30),
   "Never Played" to daily.filter {it.id.uri !in history},
   "Before 1980" to library.filter {it.year!=null && it.year in 1..1979}.sortedBy {it.year},
   "Before 2000" to library.filter {it.year!=null && it.year in 1..1999}.sortedBy {it.year},
   "High Quality" to library.filter {it.bitrate?.let {rate->rate>=320000}==true || it.mimeType?.lowercase(Locale.ROOT) in lossless},
   "Month’s Top Chart" to chart
  )
 }
 fun emptyReason(title:String)=when(title) {
  "Recently Played","Top Artist mix"->"Play music with For you enabled to build this mix."
  "Month’s Top Chart"->"Play music with For you enabled this month to build your chart."
  "Before 1980","Before 2000"->"No visible songs have a matching release year. Missing year tags are not guessed."
  "High Quality"->"No visible songs have known lossless formats or a bitrate of at least 320 kbps."
  "Never Played"->"All songs with tracked playback have been played, or your visible library is empty."
  else->"Add songs to your visible library to build this mix."
 }
 private val lossless=setOf("audio/flac","audio/x-flac","audio/alac","audio/x-alac","audio/wav","audio/x-wav","audio/wave","audio/vnd.wave","audio/aiff","audio/x-aiff","audio/ape","audio/x-ape","audio/pcm","audio/x-pcm")
}
