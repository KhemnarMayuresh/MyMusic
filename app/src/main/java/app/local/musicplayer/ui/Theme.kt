package app.local.musicplayer.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold=Color(0xFFFFB900)
val Muted=Color(0xFF9C98B2)
val Ink=Color(0xFF080B24)
val Panel=Color(0xFF252044)
data class LibraryTheme(val name:String,val colors:List<Long>,val accent:Long)
object LibraryThemes {
 val all=listOf(
  LibraryTheme("Midnight",listOf(0xFF080B24,0xFF17103F,0xFF35134D),0xFFFFB900),
  LibraryTheme("Ocean",listOf(0xFF061824,0xFF123B45),0xFF67DDE8),
  LibraryTheme("Sunset",listOf(0xFF261125,0xFF472735),0xFFFFA487),
  LibraryTheme("Obsidian",listOf(0xFF080808,0xFF19191D),0xFFFFB900),
  LibraryTheme("Violet",listOf(0xFF1C0A34,0xFF4D2266),0xFFFFCC85),
  LibraryTheme("Forest",listOf(0xFF071E19,0xFF1E493B),0xFF9AE1AF),
  LibraryTheme("Ruby",listOf(0xFF220B17,0xFF591D38),0xFFFFA3BF),
  LibraryTheme("Graphite",listOf(0xFF11151B,0xFF2E3944),0xFFC2D7E9))
 fun find(name:String)=all.find {it.name==name} ?: all.first()
}
@Composable fun MusicTheme(theme:String,content:@Composable ()->Unit) {
 val accent=Color(LibraryThemes.find(theme).accent)
 MaterialTheme(colorScheme=darkColorScheme(primary=accent,onPrimary=Ink,secondary=accent,background=Ink,surface=Color(0xFF17152E),surfaceVariant=Panel,onSurface=Color.White,onSurfaceVariant=Muted),content=content)
}
fun timeLabel(ms: Long): String {
    val seconds=(ms/1000).coerceAtLeast(0)
    return if(seconds>=3600) "%d:%02d:%02d".format(seconds/3600,seconds%3600/60,seconds%60) else "%d:%02d".format(seconds/60,seconds%60)
}
