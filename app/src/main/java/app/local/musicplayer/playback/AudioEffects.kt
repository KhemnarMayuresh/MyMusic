@file:Suppress("DEPRECATION")
@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package app.local.musicplayer.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.media.audiofx.PresetReverb
import androidx.media3.common.AuxEffectInfo
import androidx.media3.exoplayer.ExoPlayer
import app.local.musicplayer.collections.CollectionState

class AudioEffects {
    private var sessionId=0
    private var bass: BassBoost?=null;private var virtualizer: Virtualizer?=null;private var reverb: PresetReverb?=null
    fun apply(player: ExoPlayer,settings: CollectionState): String? {
        if(player.audioSessionId<=0) return null
        if(sessionId!=player.audioSessionId) {
            release();sessionId=player.audioSessionId
            bass=runCatching {BassBoost(0,sessionId)}.getOrNull()
            virtualizer=runCatching {Virtualizer(0,sessionId)}.getOrNull()
            reverb=runCatching {PresetReverb(0,0)}.getOrNull()
        }
        val bassValue=(settings.pref("bass","0").toIntOrNull() ?: 0).coerceIn(0,100)
        val virtualValue=(settings.pref("virtualizer","0").toIntOrNull() ?: 0).coerceIn(0,100)
        val reverbValue=(settings.pref("reverb","0").toIntOrNull() ?: 0).coerceIn(0,6)
        val failures=mutableListOf<String>()
        runCatching {bass?.apply {if(strengthSupported) setStrength((bassValue*10).toShort());enabled=bassValue>0}}.onFailure {failures+="Bass boost unavailable"}
        runCatching {virtualizer?.apply {if(strengthSupported) setStrength((virtualValue*10).toShort());enabled=virtualValue>0}}.onFailure {failures+="Virtualizer unavailable"}
        runCatching {reverb?.apply {preset=reverbValue.toShort();enabled=reverbValue>0;player.setAuxEffectInfo(AuxEffectInfo(id,if(reverbValue>0) .5f else 0f))}}.onFailure {failures+="Reverb unavailable"}
        if(bassValue>0 && bass==null) failures+="Bass boost unsupported by this device"
        if(virtualValue>0 && virtualizer==null) failures+="Virtualizer unsupported by this device"
        if(reverbValue>0 && reverb==null) failures+="Reverb unsupported by this device"
        return failures.distinct().takeIf {it.isNotEmpty()}?.joinToString(" • ")
    }
    fun release() {bass?.release();virtualizer?.release();reverb?.release();bass=null;virtualizer=null;reverb=null;sessionId=0}
}
