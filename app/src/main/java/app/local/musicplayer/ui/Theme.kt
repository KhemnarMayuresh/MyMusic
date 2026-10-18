package app.local.musicplayer.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold=Color(0xFFFFB900)
val Muted=Color(0xFF9C98B2)
val Ink=Color(0xFF080B24)
val Panel=Color(0xFF252044)
@Composable fun MusicTheme(theme: String,content: @Composable ()->Unit) {
    val accent=when(theme) { "Ocean"->Color(0xFF67DDE8); "Sunset"->Color(0xFFFFA487); else->Gold }
    MaterialTheme(colorScheme=darkColorScheme(primary=accent,onPrimary=Ink,secondary=accent,background=Ink,surface=Color(0xFF17152E),surfaceVariant=Panel,onSurface=Color.White,onSurfaceVariant=Muted),content=content)
}
fun timeLabel(ms: Long): String {
    val seconds=(ms/1000).coerceAtLeast(0)
    return if(seconds>=3600) "%d:%02d:%02d".format(seconds/3600,seconds%3600/60,seconds%60) else "%d:%02d".format(seconds/60,seconds%60)
}
