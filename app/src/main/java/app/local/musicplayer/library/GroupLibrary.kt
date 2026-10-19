package app.local.musicplayer.library
import app.local.musicplayer.model.Track

data class MediaGroup(val key:String,val title:String,val tracks:List<Track>)
object GroupLibrary {
 fun groups(tracks:List<Track>,kind:String,order:String):List<MediaGroup> {
  val groups=tracks.groupBy {when(kind) {"folder"->it.folder;"album"->it.albumId?.toString() ?: it.album;else->it.artist}}.map {(key,songs)->MediaGroup(key,when(kind) {"folder"->key.substringAfterLast('/').ifEmpty {"Music"};"album"->songs.first().album;else->key},songs)}
  return when(order) {"Name Z–A"->groups.sortedByDescending {it.title.lowercase()};"Song count"->groups.sortedByDescending {it.tracks.size};"Newest"->groups.sortedByDescending {it.tracks.maxOf {t->t.dateAddedSeconds}};else->groups.sortedBy {it.title.lowercase()}}
 }
 fun selected(groups:List<MediaGroup>,keys:Set<String>)=groups.filter {it.key in keys}.flatMap {it.tracks}.distinctBy {it.id}
}
