package my.music.backup

import org.json.JSONArray
import org.json.JSONObject

data class BackupPlaylist(val id: String, val name: String, val tracks: List<String>)
data class BackupData(
    val playlists: List<BackupPlaylist>, val favorites: Set<String>, val hidden: Set<String>,
    val lyrics: Map<String,String>, val preferences: Map<String,String>, val overrides: Map<String,String> = emptyMap(),
)
object BackupCodec {
    const val MAX_BYTES = 8 * 1024 * 1024
    private fun strings(array: JSONArray): List<String> {
        require(array.length() <= 50_000) { "Too many entries" }
        return (0 until array.length()).map { val value=array.get(it); require(value is String && value.length in 1..8192); value }
    }
    private fun stringMap(json: JSONObject): Map<String,String> {
        require(json.length() <= 50_000)
        return json.keys().asSequence().associateWith { key -> val value=json.get(key); require(value is String && value.length <= 1_000_000); value }
    }
    fun decode(text: String): BackupData = try {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup is too large" }
        val json=JSONObject(text)
        require(json.getInt("version") == 1) { "Unsupported backup version" }
        val list=json.getJSONArray("playlists"); require(list.length() <= 5000)
        val playlists=(0 until list.length()).map { index ->
            val p=list.getJSONObject(index); val id=p.getString("id"); val name=p.getString("name")
            require(id.length in 1..200 && name.isNotBlank() && name.length <= 200)
            BackupPlaylist(id,name,strings(p.getJSONArray("tracks")))
        }
        require(playlists.map { it.id }.distinct().size == playlists.size) { "Duplicate playlist IDs" }
        BackupData(playlists,strings(json.getJSONArray("favorites")).toSet(),strings(json.getJSONArray("hidden")).toSet(),stringMap(json.getJSONObject("lyrics")),stringMap(json.getJSONObject("preferences")),stringMap(json.optJSONObject("overrides") ?: JSONObject()))
    } catch(e: Exception) { throw IllegalArgumentException(e.message ?: "Invalid backup",e) }
    fun encode(data: BackupData): String = JSONObject().put("version",1)
        .put("playlists",JSONArray().apply { data.playlists.forEach { put(JSONObject().put("id",it.id).put("name",it.name).put("tracks",JSONArray(it.tracks))) } })
        .put("favorites",JSONArray(data.favorites.toList())).put("hidden",JSONArray(data.hidden.toList()))
        .put("lyrics",JSONObject(data.lyrics)).put("preferences",JSONObject(data.preferences)).put("overrides",JSONObject(data.overrides)).toString()
}
