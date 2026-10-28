package my.music.ui

import org.json.JSONArray
import org.json.JSONObject

object CustomThemes {
 const val KEY="custom_player_themes"
 private fun valid(theme:PlayerTheme)=theme.id.startsWith("custom:") && theme.id.length in 8..80 && theme.name.isNotBlank() && theme.name.length<=40 && theme.style in 0..17 && listOf(theme.top,theme.bottom,theme.accent).all {it in 0xFF000000L..0xFFFFFFFFL}
 fun decode(raw:String):List<PlayerTheme> {
  if(raw.length>200000) return emptyList()
  return runCatching {
   val array=JSONArray(raw)
   if(array.length()>50) return emptyList()
   (0 until array.length()).mapNotNull {index->runCatching {
    val item=array.getJSONObject(index)
    PlayerTheme(item.getString("name"),item.getInt("style"),item.getLong("top"),item.getLong("bottom"),item.getLong("accent"),item.getString("id"),item.optBoolean("coverColors",false)).takeIf(::valid)
   }.getOrNull()}.distinctBy {it.id}
  }.getOrDefault(emptyList())
 }
 fun encode(themes:List<PlayerTheme>):String {
  require(themes.size<=50 && themes.all(::valid) && themes.map {it.id}.distinct().size==themes.size)
  return JSONArray().apply {themes.forEach {theme->put(JSONObject().put("id",theme.id).put("name",theme.name).put("style",theme.style).put("top",theme.top).put("bottom",theme.bottom).put("accent",theme.accent).put("coverColors",theme.coverColors))}}.toString()
 }
 fun color(value:String):Long?=value.trim().removePrefix("#").takeIf {it.matches(Regex("[0-9a-fA-F]{6}"))}?.toLongOrNull(16)?.or(0xFF000000L)
 fun hex(value:Long)=String.format("%06X",value and 0xFFFFFF)
}
