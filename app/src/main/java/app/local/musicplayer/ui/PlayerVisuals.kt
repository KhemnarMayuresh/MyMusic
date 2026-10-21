package app.local.musicplayer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.local.musicplayer.playback.PlayerAudioSignal

@Composable internal fun PlayerSurface(base:PlayerTheme,uri:String?,modifier:Modifier=Modifier,content:@Composable (PlayerTheme)->Unit) {
    val art=rememberCover(if(base.coverColors) uri else null)
    val theme=art?.let {base.copy(top=it.colors[0],bottom=it.colors[1],accent=it.colors[2])} ?: base
    Box(modifier.background(Brush.verticalGradient(listOf(Color(theme.top),Color(theme.bottom))))) {
        art?.let {
            Image(it.blur.asImageBitmap(),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(theme.top).copy(alpha=.72f),Color(theme.bottom).copy(alpha=.82f)))))
        }
        MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(primary=Color(theme.accent))) {content(theme)}
    }
}
internal data class PlayerMotion(val degrees:Float=0f,val levels:FloatArray=FloatArray(43))
@Composable internal fun rememberPlayerMotion(playing:Boolean,entry:String?):PlayerMotion {
    var motion by remember(entry) {mutableStateOf(PlayerMotion())}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(playing,entry,lifecycle) {
        if(!playing) {motion=motion.copy(levels=FloatArray(43));return@LaunchedEffect}
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last=0L
            while(true) withFrameNanos {now->
                val delta=if(last==0L) 0f else ((now-last)/1_000_000_000f).coerceAtMost(.1f)
                last=now
                motion=PlayerMotion((motion.degrees+delta*30f)%360f,PlayerAudioSignal.active?.snapshot() ?: FloatArray(43))
            }
        }
    }
    return motion
}
