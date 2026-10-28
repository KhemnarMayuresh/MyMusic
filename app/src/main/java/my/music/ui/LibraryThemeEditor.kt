package my.music.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import my.music.PlayerViewModel
import my.music.collections.CollectionState

@Composable internal fun LibraryThemeEditor(vm:PlayerViewModel,data:CollectionState,modifier:Modifier=Modifier) {
 val custom=CustomLibraryThemes.decode(data.pref(CustomLibraryThemes.KEY))
 var selected by rememberSaveable {mutableStateOf(data.pref("theme","Midnight"))}
 var editing by rememberSaveable {mutableStateOf(false)}
 var id by rememberSaveable {mutableStateOf("")}
 var name by rememberSaveable {mutableStateOf("")}
 var top by rememberSaveable {mutableStateOf("")}
 var bottom by rememberSaveable {mutableStateOf("")}
 var accent by rememberSaveable {mutableStateOf("")}
 var deleting by remember {mutableStateOf(false)}
 var saving by remember {mutableStateOf(false)}
 val saved=LibraryThemes.find(selected,data.pref(CustomLibraryThemes.KEY))
 val draft=LibraryTheme(name,listOf(CustomThemes.color(top) ?: saved.colors.first(),CustomThemes.color(bottom) ?: saved.colors.last()),CustomThemes.color(accent) ?: saved.accent,id)
 val shown=if(editing) draft else saved
 fun edit(existing:Boolean) {
  id=if(existing) saved.id else "library:${java.util.UUID.randomUUID()}"
  name=if(existing) saved.name else ""
  top=CustomThemes.hex(saved.colors.first());bottom=CustomThemes.hex(saved.colors.last());accent=CustomThemes.hex(saved.accent);editing=true
 }
 androidx.activity.compose.BackHandler(editing) {editing=false}
 Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Box(Modifier.fillMaxWidth().height(100.dp).background(Brush.verticalGradient(shown.colors.map {Color(it)}),RoundedCornerShape(18.dp)))
  if(editing) {
   OutlinedTextField(name,{name=it.take(40)},label={Text("Library theme name")},singleLine=true,modifier=Modifier.fillMaxWidth())
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    ThemeColorField("Top color",top,{top=it},Modifier.weight(1f));ThemeColorField("Bottom color",bottom,{bottom=it},Modifier.weight(1f));ThemeColorField("Accent",accent,{accent=it},Modifier.weight(1f))
   }
   val error=when {
    name.isBlank()->"Enter a theme name"
    (LibraryThemes.all+custom).any {it.id!=id && it.name.equals(name.trim(),true)}->"Choose a different name"
    custom.size>=50 && custom.none {it.id==id}->"You can save up to 50 themes"
    listOf(top,bottom,accent).any {CustomThemes.color(it)==null}->"Choose valid colors"
    else->null
   }
   error?.let {Text(it,color=Muted)}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick={editing=false},enabled=!saving) {Text("Cancel")}
    Button(onClick={saving=true;val value=draft.copy(name=name.trim());vm.work {try {vm.repository.setPreferences(mapOf(CustomLibraryThemes.KEY to CustomLibraryThemes.encode(custom.filterNot {it.id==id}+value),"theme" to id));selected=id;editing=false} finally {saving=false}}},enabled=error==null && !saving) {Text("Save and apply")}
   }
  } else {
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    TextButton(onClick={edit(false)}) {Text("Create library theme")}
    if(saved.id.startsWith("library:")) {TextButton(onClick={edit(true)}) {Text("Edit")};TextButton(onClick={deleting=true}) {Text("Delete")}}
   }
   (LibraryThemes.all+custom).forEach {option->
    OutlinedButton(onClick={selected=option.id;vm.pref("theme",option.id)},Modifier.fillMaxWidth().background(Brush.horizontalGradient(option.colors.map {Color(it)}),RoundedCornerShape(24.dp))) {Text(option.name+(if(data.pref("theme","Midnight")==option.id) " ✓" else ""),color=Color(option.accent))}
   }
  }
 }
 if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("Delete library theme?")},text={Text(saved.name)},confirmButton={TextButton(onClick={val removed=saved.id;vm.work {vm.repository.setPreferences(buildMap {put(CustomLibraryThemes.KEY,CustomLibraryThemes.encode(custom.filterNot {it.id==removed}));if(data.pref("theme")==removed) put("theme","Midnight")});selected="Midnight"};deleting=false}) {Text("Delete")}},dismissButton={TextButton(onClick={deleting=false}) {Text("Cancel")}})
}
