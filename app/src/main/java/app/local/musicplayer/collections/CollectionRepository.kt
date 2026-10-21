package app.local.musicplayer.collections

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.local.musicplayer.backup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

data class PlaylistEntry(val id: String, val trackUri: String)
data class Playlist(val id: String, val name: String, val entries: List<PlaylistEntry>)
data class PlayRecord(val trackUri: String, val lastPlayed: Long, val count: Int)
data class CollectionState(
    val playlists: List<Playlist> = emptyList(), val favorites: Set<String> = emptySet(), val hidden: Set<String> = emptySet(),
    val lyrics: Map<String,String> = emptyMap(), val preferences: Map<String,String> = emptyMap(),
    val overrides: Map<String,String> = emptyMap(), val history: List<PlayRecord> = emptyList(),
) {
    fun pref(key: String, default: String="") = preferences[key] ?: default
    fun flag(key: String, default: Boolean=false) = preferences[key]?.toBooleanStrictOrNull() ?: default
}

class CollectionRepository(context: Context, databaseName: String="music.db") {
    private val helper=object: SQLiteOpenHelper(context.applicationContext,databaseName,null,1) {
        override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE playlists(id TEXT PRIMARY KEY,name TEXT NOT NULL,created INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE entries(id TEXT PRIMARY KEY,playlist TEXT NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,uri TEXT NOT NULL,position INTEGER NOT NULL)")
            listOf("favorites","hidden").forEach { db.execSQL("CREATE TABLE $it(uri TEXT PRIMARY KEY)") }
            listOf("lyrics","preferences","overrides").forEach { db.execSQL("CREATE TABLE $it(key TEXT PRIMARY KEY,value TEXT NOT NULL)") }
            db.execSQL("CREATE TABLE history(uri TEXT PRIMARY KEY,last_played INTEGER NOT NULL,count INTEGER NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase,oldVersion: Int,newVersion: Int) { error("Unsupported database migration") }
    }
    private val lock=Mutex()
    private val mutable=MutableStateFlow(CollectionState())
    val state=mutable.asStateFlow()
    suspend fun load()=withContext(Dispatchers.IO) { lock.withLock { mutable.value=read(helper.readableDatabase) } }
    private fun read(db: SQLiteDatabase): CollectionState {
        fun values(table: String)=db.rawQuery("SELECT uri FROM $table",null).use { c -> buildSet { while(c.moveToNext()) add(c.getString(0)) } }
        fun map(table: String)=db.rawQuery("SELECT key,value FROM $table",null).use { c -> buildMap { while(c.moveToNext()) put(c.getString(0),c.getString(1)) } }
        val entries=db.rawQuery("SELECT id,playlist,uri FROM entries ORDER BY position",null).use { c -> buildList { while(c.moveToNext()) add(c.getString(1) to PlaylistEntry(c.getString(0),c.getString(2))) } }.groupBy({it.first},{it.second})
        val lists=db.rawQuery("SELECT id,name FROM playlists ORDER BY created,id",null).use { c -> buildList { while(c.moveToNext()) add(Playlist(c.getString(0),c.getString(1),entries[c.getString(0)] ?: emptyList())) } }
        val history=db.rawQuery("SELECT uri,last_played,count FROM history ORDER BY last_played DESC",null).use { c -> buildList { while(c.moveToNext()) add(PlayRecord(c.getString(0),c.getLong(1),c.getInt(2))) } }
        return CollectionState(lists,values("favorites"),values("hidden"),map("lyrics"),map("preferences"),map("overrides"),history)
    }
    private suspend fun <T> transaction(block: (SQLiteDatabase)->T): T = withContext(Dispatchers.IO) {
        lock.withLock {
            val db=helper.writableDatabase
            db.beginTransaction()
            val result: T
            try { result=block(db); db.setTransactionSuccessful() } finally { db.endTransaction() }
            mutable.value=read(db)
            result
        }
    }
    suspend fun createPlaylist(name: String): String {
        require(name.isNotBlank() && name.length<=200)
        val id=UUID.randomUUID().toString()
        transaction { db -> db.insertOrThrow("playlists",null,ContentValues().apply { put("id",id); put("name",name.trim()); put("created",System.currentTimeMillis()) }) }
        return id
    }
    suspend fun renamePlaylist(id: String,name: String) { require(name.isNotBlank() && name.length<=200); transaction { it.update("playlists",ContentValues().apply { put("name",name.trim()) },"id=?",arrayOf(id)) } }
    suspend fun deletePlaylist(id: String) { transaction { it.delete("playlists","id=?",arrayOf(id)) } }
    suspend fun addToPlaylist(id: String,uris: List<String>) { require(uris.size<=50_000); transaction { db ->
        var position=db.rawQuery("SELECT COALESCE(MAX(position),-1)+1 FROM entries WHERE playlist=?",arrayOf(id)).use { it.moveToFirst(); it.getInt(0) }
        uris.forEach { uri -> db.insertOrThrow("entries",null,ContentValues().apply { put("id",UUID.randomUUID().toString()); put("playlist",id); put("uri",uri); put("position",position++) }) }
    } }
    suspend fun removePlaylistEntry(entryId: String) { transaction { it.delete("entries","id=?",arrayOf(entryId)) } }
    suspend fun movePlaylistEntry(playlistId: String,from: Int,to: Int) { transaction { db ->
        val ids=db.rawQuery("SELECT id FROM entries WHERE playlist=? ORDER BY position",arrayOf(playlistId)).use { c -> buildList { while(c.moveToNext()) add(c.getString(0)) } }.toMutableList()
        require(from in ids.indices && to in ids.indices); ids.add(to,ids.removeAt(from))
        ids.forEachIndexed { index,id -> db.update("entries",ContentValues().apply { put("position",index) },"id=?",arrayOf(id)) }
    } }
    private suspend fun setMembership(table: String,uri: String,value: Boolean) { transaction { db ->
        if(value) db.insertWithOnConflict(table,null,ContentValues().apply { put("uri",uri) },SQLiteDatabase.CONFLICT_IGNORE) else db.delete(table,"uri=?",arrayOf(uri))
    } }
    suspend fun setFavorite(uri: String,value: Boolean)=setMembership("favorites",uri,value)
    suspend fun setHidden(uri: String,value: Boolean)=setMembership("hidden",uri,value)
    private suspend fun setValue(table: String,key: String,value: String) { transaction { db -> db.insertWithOnConflict(table,null,ContentValues().apply { put("key",key); put("value",value) },SQLiteDatabase.CONFLICT_REPLACE) } }
    suspend fun setPreferences(values:Map<String,String>) {transaction {db->
        values.forEach {(key,value)->check(db.insertWithOnConflict("preferences",null,ContentValues().apply {put("key",key);put("value",value)},SQLiteDatabase.CONFLICT_REPLACE)>=0) {"Unable to save preferences"}}
    }}
    suspend fun setPreference(key: String,value: String)=setPreferences(mapOf(key to value))
    suspend fun setLyrics(uri: String,text: String) { require(text.length<=1_000_000); setValue("lyrics",uri,text) }
    suspend fun setOverride(uri: String,json: String)=setValue("overrides",uri,json)
    suspend fun recordPlay(uri: String) { transaction { db ->
        val count=db.rawQuery("SELECT count FROM history WHERE uri=?",arrayOf(uri)).use {if(it.moveToFirst()) it.getInt(0) else 0}
        db.insertWithOnConflict("history",null,ContentValues().apply {put("uri",uri);put("last_played",System.currentTimeMillis());put("count",count+1)},SQLiteDatabase.CONFLICT_REPLACE)
    } }
    suspend fun export(): BackupData=withContext(Dispatchers.IO) { lock.withLock {
        val s=read(helper.readableDatabase)
        BackupData(s.playlists.map { BackupPlaylist(it.id,it.name,it.entries.map { e->e.trackUri }) },s.favorites,s.hidden,s.lyrics,s.preferences,s.overrides)
    } }
    suspend fun restore(data: BackupData) { transaction { db ->
        listOf("entries","playlists","favorites","hidden","lyrics","preferences","overrides").forEach { db.delete(it,null,null) }
        data.playlists.forEachIndexed { index,p ->
            db.insertOrThrow("playlists",null,ContentValues().apply { put("id",p.id); put("name",p.name); put("created",index) })
            p.tracks.forEachIndexed { position,uri -> db.insertOrThrow("entries",null,ContentValues().apply { put("id",UUID.randomUUID().toString()); put("playlist",p.id); put("uri",uri); put("position",position) }) }
        }
        listOf("favorites" to data.favorites,"hidden" to data.hidden).forEach { (table,uris) -> uris.forEach { uri -> db.insertOrThrow(table,null,ContentValues().apply { put("uri",uri) }) } }
        listOf("lyrics" to data.lyrics,"preferences" to data.preferences,"overrides" to data.overrides).forEach { (table,map) -> map.forEach { (key,value) -> db.insertOrThrow(table,null,ContentValues().apply { put("key",key); put("value",value) }) } }
    } }
    fun close()=helper.close()
}
