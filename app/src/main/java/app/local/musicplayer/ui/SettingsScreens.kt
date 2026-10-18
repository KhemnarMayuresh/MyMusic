package app.local.musicplayer.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.collections.CollectionState
import app.local.musicplayer.model.Track
import app.local.musicplayer.playback.PlaybackSnapshot

@Composable private fun Section(title: String) {Text(title,Modifier.padding(start=20.dp,top=24.dp,bottom=8.dp),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
@Composable fun PreferenceToggle(title: String,subtitle: String,checked: Boolean,onChecked: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().clickable {onChecked(!checked)}.padding(horizontal=20.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end=16.dp)) {Text(title,style=MaterialTheme.typography.titleMedium);if(subtitle.isNotBlank()) Text(subtitle,color=Muted,style=MaterialTheme.typography.bodyMedium)}
        Switch(checked,onChecked)
    }
}
@Composable fun SettingsScreen(vm: PlayerViewModel,data: CollectionState,onBack: ()->Unit,onNavigate: (String)->Unit) {
    val context=LocalContext.current
    Column {
        Header("Settings",onBack)
        LazyColumn {
            item {Section("General")}
            item {ActionRow(Icons.Rounded.Radar,"Scan music","Refresh your library and choose scan filters") {onNavigate("scan")}}
            item {ActionRow(Icons.Rounded.VisibilityOff,"Hidden music","View and restore hidden songs") {onNavigate("hidden")}}
            item {ActionRow(Icons.Rounded.Backup,"Backup & restore","Save playlists, favorites, lyrics and settings") {onNavigate("backup")}}
            item {ActionRow(Icons.Rounded.PhonelinkRing,"Music transfer","Export or import a local music package") {onNavigate("transfer")}}
            item {Section("Playback")}
            item {PreferenceToggle("Simultaneous playback","Allow music to play with other apps",data.flag("simultaneous")) {vm.pref("simultaneous",it.toString())}}
            item {PreferenceToggle("Crossfade","Blend the end of one song into the next",data.flag("crossfade")) {vm.pref("crossfade",it.toString())}}
            if(data.flag("crossfade")) item {
                val seconds=data.pref("crossfade_seconds","5").toFloatOrNull()?.coerceIn(1f,12f) ?: 5f
                Column(Modifier.padding(horizontal=24.dp)) {Text("Crossfade: ${seconds.toInt()} seconds",color=Muted);Slider(seconds,{vm.pref("crossfade_seconds",it.toInt().toString())},valueRange=1f..12f,steps=10)}
            }
            item {PreferenceToggle("Gapless playback","Play consecutive files seamlessly; crossfade takes priority",data.flag("gapless",true)) {vm.pref("gapless",it.toString())}}
            item {PreferenceToggle("Pause on detach","Pause when headphones are disconnected",data.flag("pause_detach",true)) {vm.pref("pause_detach",it.toString())}}
            item {ActionRow(Icons.Rounded.BatteryChargingFull,"Background playback","Open Android app settings for battery and notification controls") {context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}}
            item {Section("Preference")}
            item {PreferenceToggle("Keep screen on","Stay awake while the full player is open",data.flag("keep_screen")) {vm.pref("keep_screen",it.toString())}}
            item {ActionRow(Icons.Rounded.Notifications,"Notifications","Android manages media and lock-screen controls") {val intent=if(android.os.Build.VERSION.SDK_INT>=26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName) else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}"));context.startActivity(intent)}}
            item {ActionRow(Icons.Rounded.Palette,"Skin theme",data.pref("theme","Midnight")) {onNavigate("themes")}}
            item {ActionRow(Icons.Rounded.Widgets,"Widgets","Add playback controls to your home screen") {onNavigate("widgets")}}
            item {Section("About")}
            item {Text("Music Player 0.1.0\nLocal music. No ads. No purchases.\n\nAudio formats depend on the device decoder. Unavailable media and unsupported effects are reported explicitly.",Modifier.padding(20.dp),color=Muted)}
        }
    }
}
@Composable fun ScanScreen(vm: PlayerViewModel,data: CollectionState,onBack: ()->Unit,onPermission: ()->Unit) {
    var seconds by remember(data.pref("min_duration")) {mutableStateOf(data.pref("min_duration","0"))}
    var excluded by remember(data.pref("excluded_folders")) {mutableStateOf(data.pref("excluded_folders"))}
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header("Scan music",onBack)
        Text("Find audio stored on your device",Modifier.padding(20.dp),style=MaterialTheme.typography.titleLarge)
        OutlinedTextField(seconds,{seconds=it.filter(Char::isDigit).take(4)},Modifier.fillMaxWidth().padding(horizontal=20.dp),label={Text("Minimum duration (seconds)")},singleLine=true)
        OutlinedTextField(excluded,{excluded=it},Modifier.fillMaxWidth().padding(20.dp),label={Text("Excluded folders — one path per line")},minLines=3)
        Button(onClick={vm.pref("min_duration",(seconds.toIntOrNull()?.coerceIn(0,3600) ?: 0).toString());vm.pref("excluded_folders",excluded);if(vm.app.library.hasPermission()) vm.rescan() else onPermission()},modifier=Modifier.padding(horizontal=20.dp)) {Icon(Icons.Rounded.Radar,null);Spacer(Modifier.width(8.dp));Text("Scan music")}
        Text("Scan filters only affect this player's library. Hiding or filtering a song preserves the source file.",Modifier.padding(20.dp),color=Muted)
    }
}
@Composable fun HiddenScreen(vm: PlayerViewModel,data: CollectionState,all: List<Track>,onBack: ()->Unit) {
    Column {
        Header("Hidden music",onBack)
        LazyColumn {
            if(data.hidden.isEmpty()) item {Text("No hidden songs",Modifier.padding(24.dp),color=Muted)}
            items(data.hidden.toList(),key={it}) { uri ->
                val track=all.find {it.id.uri==uri}
                ActionRow(Icons.Rounded.Visibility,track?.title ?: "Unavailable song","Tap to unhide") {vm.work {vm.repository.setHidden(uri,false)}}
            }
        }
    }
}
@Composable fun ThemeScreen(vm: PlayerViewModel,data: CollectionState,onBack: ()->Unit) {
    Column {
        Header("Themes",onBack)
        Section("Library theme")
        listOf("Midnight","Ocean","Sunset","Obsidian").forEach {theme-> ActionRow(if(data.pref("theme","Midnight")==theme) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,theme) {vm.pref("theme",theme)}}
        Section("Player theme")
        listOf("Artwork","Vinyl").forEach {theme-> ActionRow(if(data.pref("player_theme","Artwork")==theme) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,theme) {vm.pref("player_theme",theme)}}
    }
}
@Composable fun TimerDialog(vm: PlayerViewModel,state: PlaybackSnapshot,onDismiss: ()->Unit) {
    var hours by remember {mutableStateOf("0")};var minutes by remember {mutableStateOf("30")};var seconds by remember {mutableStateOf("0")};var finish by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Stop music after")},text={Column {
        if(state.timerRemaining>=0) Text("Active: ${timeLabel(state.timerRemaining)}",color=MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(hours,{hours=it.filter(Char::isDigit).take(2)},Modifier.weight(1f),label={Text("Hours")},singleLine=true)
            OutlinedTextField(minutes,{minutes=it.filter(Char::isDigit).take(2)},Modifier.weight(1f),label={Text("Min")},singleLine=true)
            OutlinedTextField(seconds,{seconds=it.filter(Char::isDigit).take(2)},Modifier.weight(1f),label={Text("Sec")},singleLine=true)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {listOf(15,30,45,60).forEach {preset->TextButton(onClick={hours="0";minutes=preset.toString();seconds="0"}) {Text("${preset}m")}}}
        Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(finish,{finish=it});Text("Stop when current song ends")}
    }},confirmButton={TextButton(onClick={val ms=((hours.toLongOrNull() ?: 0)*3600+(minutes.toLongOrNull() ?: 0)*60+(seconds.toLongOrNull() ?: 0))*1000;if(ms in 1000..86_400_000) {vm.connection.timer(ms,finish);onDismiss()} else vm.message.value="Choose a duration between 1 second and 24 hours"}) {Text("Start")}},dismissButton={Row {if(state.timerRemaining>=0) TextButton(onClick={vm.connection.cancelTimer();onDismiss()}) {Text("Stop timer")};TextButton(onClick=onDismiss) {Text("Cancel")}}})
}
@Composable fun SpeedDialog(vm: PlayerViewModel,state: PlaybackSnapshot,onDismiss: ()->Unit) {
    var speed by remember {mutableFloatStateOf(state.speed)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Playback speed")},text={Column {
        Text("%.2f×".format(speed),style=MaterialTheme.typography.headlineMedium)
        Slider(speed,{speed=it;vm.connection.controller?.setPlaybackSpeed(it)},valueRange=.5f..2f)
        Row {listOf(.5f,1f,1.5f,2f).forEach {value->TextButton(onClick={speed=value;vm.connection.controller?.setPlaybackSpeed(value)}) {Text("${value}×")}}}
    }},confirmButton={TextButton(onClick=onDismiss) {Text("Done")}})
}
