package app.local.musicplayer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.collections.CollectionState

val EqualizerPresets=linkedMapOf(
    "Custom" to List(10) {0f},"Normal" to List(10) {0f},"Rock" to listOf(5f,4f,3f,1f,-2f,-2f,1f,3f,4f,5f),
    "Dance" to listOf(5f,4f,2f,0f,0f,2f,3f,4f,3f,2f),"Bass Boost" to listOf(8f,7f,5f,3f,1f,0f,0f,0f,0f,0f),
    "Electronic" to listOf(5f,4f,1f,0f,-2f,1f,3f,4f,5f,5f),"Flat" to List(10) {0f},"Classical" to listOf(3f,2f,1f,0f,-2f,-2f,0f,2f,3f,4f),
    "Pop" to listOf(-1f,1f,3f,4f,3f,1f,-1f,-1f,1f,2f),"Jazz" to listOf(4f,3f,1f,2f,-2f,-2f,0f,2f,3f,4f),
)
@Composable fun EqualizerScreen(vm: PlayerViewModel,data: CollectionState,onBack: ()->Unit) {
    val playback by vm.playback.collectAsStateWithLifecycle()
    val bands=if(data.pref("eq_bands","10")=="5") 5 else 10
    val gains=data.pref("eq_gains",List(10) {"0"}.joinToString(",")).split(',').map {it.toFloatOrNull()?.coerceIn(-12f,12f) ?: 0f}.let {if(it.size==10) it else List(10) {0f}}
    Column {
        Header("Equalizer",onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {
            PreferenceToggle("Equalizer","Apply real ten-band PCM processing",data.flag("eq_enabled")) {vm.pref("eq_enabled",it.toString())}
            Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                EqualizerPresets.forEach { (name,values)->FilterChip(selected=data.pref("eq_preset","Normal")==name,onClick={vm.pref("eq_preset",name);vm.pref("eq_gains",values.joinToString(","));vm.pref("eq_enabled","true")},label={Text(name)}) }
            }
            Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FilterChip(selected=bands==5,onClick={vm.pref("eq_bands","5")},label={Text("5 Bands")})
                FilterChip(selected=bands==10,onClick={vm.pref("eq_bands","10")},label={Text("10 Bands")})
                TextButton(onClick={vm.pref("eq_gains",List(10) {0}.joinToString(","));vm.pref("eq_preset","Normal")}) {Text("Reset")}
            }
            val indices=if(bands==5) listOf(0,2,4,6,8) else (0..9).toList()
            val labels=listOf("31 Hz","62 Hz","125 Hz","250 Hz","500 Hz","1 kHz","2 kHz","4 kHz","8 kHz","16 kHz")
            indices.forEach {index->Column(Modifier.padding(horizontal=24.dp,vertical=4.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(labels[index]);Text("%+.1f dB".format(gains[index]),color=Muted)}
                Slider(gains[index],{value->val updated=gains.toMutableList();updated[index]=value;vm.pref("eq_gains",updated.joinToString(","));vm.pref("eq_preset","Custom");vm.pref("eq_enabled","true")},valueRange=-12f..12f)
            }}
            Text("Bass boost",Modifier.padding(horizontal=24.dp,vertical=8.dp))
            Slider(data.pref("bass","0").toFloatOrNull()?.coerceIn(0f,100f) ?: 0f,{vm.pref("bass",it.toInt().toString())},Modifier.padding(horizontal=24.dp),valueRange=0f..100f)
            Text("Virtualizer",Modifier.padding(horizontal=24.dp,vertical=8.dp))
            Slider(data.pref("virtualizer","0").toFloatOrNull()?.coerceIn(0f,100f) ?: 0f,{vm.pref("virtualizer",it.toInt().toString())},Modifier.padding(horizontal=24.dp),valueRange=0f..100f)
            Text("Reverb",Modifier.padding(horizontal=24.dp,vertical=8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("None","Small room","Medium room","Large room","Medium hall","Large hall","Plate").forEachIndexed {index,name->FilterChip(selected=data.pref("reverb","0")==index.toString(),onClick={vm.pref("reverb",index.toString())},label={Text(name)})}}
            Text("Bass, virtualizer and reverb depend on Android audio-effect support. The ten-band equalizer works on decoded PCM audio.",Modifier.padding(24.dp),color=Muted)
            playback.effectsMessage?.let {Text(it,Modifier.padding(horizontal=24.dp),color=MaterialTheme.colorScheme.primary)}
        }
    }
}
