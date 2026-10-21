package app.local.musicplayer.ui

import org.json.JSONArray
import org.json.JSONObject

/** Library presets keep stable identity separately from their editable display name. */
object CustomLibraryThemes {
 const val KEY="custom_library_themes"
 private fun valid(t:LibraryTheme)=t.id.startsWith("library:") && t.id.length in 9..80 && t.name.isNotBlank() && t.name.length<=40 && t.colors.size==2 && (t.colors+t.accent).all {it in 0xFF000000L..0xFFFFFFFFL}
 fun decode(raw:String):List<LibraryTheme> {
  if(raw.length>200000) return emptyList()
  return runCatching {
   val a=JSONArray(raw);if(a.length()>50) return emptyList()
   (0 until a.length()).mapNotNull {i->runCatching {
    val o=a.getJSONObject(i)
    LibraryTheme(o.getString("name"),listOf(o.getLong("top"),o.getLong("bottom")),o.getLong("accent"),o.getString("id")).takeIf(::valid)
   }.getOrNull()}.distinctBy {it.id}
  }.getOrDefault(emptyList())
 }
 fun encode(values:List<LibraryTheme>):String {
  require(values.size<=50 && values.all(::valid) && values.map {it.id}.distinct().size==values.size)
  return JSONArray().apply {values.forEach {put(JSONObject().put("id",it.id).put("name",it.name).put("top",it.colors.first()).put("bottom",it.colors.last()).put("accent",it.accent))}}.toString()
 }
}
