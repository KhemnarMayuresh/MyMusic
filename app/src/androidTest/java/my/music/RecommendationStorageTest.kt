package my.music
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import my.music.collections.CollectionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class RecommendationStorageTest {
 @Test fun disabledTrackingRetainsDataUntilExplicitCleanupAndSurvivesReload():Unit=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="recommendation-${System.nanoTime()}.db";var repo=CollectionRepository(context,name)
  try {
   repo.load();repo.recordPlay("one",1000);repo.recordPlay("one",2000)
   assertEquals(2,repo.state.value.recommendationPlays.single().count)
   assertEquals(2,repo.state.value.monthPlays.single().count)
   repo.setPreference("for_you_enabled","false")
   val before=repo.state.value.recommendationPlays;val months=repo.state.value.monthPlays
   repo.recordPlay("one",3000);repo.recordPlay("two",4000)
   assertEquals(before,repo.state.value.recommendationPlays);assertEquals(months,repo.state.value.monthPlays)
   repo.close();repo=CollectionRepository(context,name);repo.load()
   assertEquals(before,repo.state.value.recommendationPlays);assertFalse(repo.state.value.flag("for_you_enabled",true))
   repo.setPreference("for_you_enabled","true");repo.recordPlay("two",5000)
   assertEquals(2,repo.state.value.recommendationPlays.size)
   val history=repo.state.value.history
   repo.clearRecommendations();assertTrue(repo.state.value.recommendationPlays.isEmpty());assertTrue(repo.state.value.monthPlays.isEmpty())
   assertEquals(history,repo.state.value.history)
  } finally {repo.close();context.deleteDatabase(name)}
 }
 @Test fun upgradingVersionOnePreservesExistingCollections():Unit=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext;val name="migration-${System.nanoTime()}.db"
  val file=context.getDatabasePath(name);file.parentFile!!.mkdirs()
  SQLiteDatabase.openOrCreateDatabase(file,null).use {db->
   db.execSQL("CREATE TABLE playlists(id TEXT PRIMARY KEY,name TEXT NOT NULL,created INTEGER NOT NULL)")
   db.execSQL("CREATE TABLE entries(id TEXT PRIMARY KEY,playlist TEXT NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,uri TEXT NOT NULL,position INTEGER NOT NULL)")
   listOf("favorites","hidden").forEach {db.execSQL("CREATE TABLE $it(uri TEXT PRIMARY KEY)")}
   listOf("lyrics","preferences","overrides").forEach {db.execSQL("CREATE TABLE $it(key TEXT PRIMARY KEY,value TEXT NOT NULL)")}
   db.execSQL("CREATE TABLE history(uri TEXT PRIMARY KEY,last_played INTEGER NOT NULL,count INTEGER NOT NULL)")
   db.execSQL("INSERT INTO playlists VALUES ('p','Saved playlist',0)");db.execSQL("INSERT INTO entries VALUES ('e','p','one',0)")
   db.execSQL("INSERT INTO favorites VALUES ('one')");db.execSQL("INSERT INTO preferences VALUES ('theme','Ocean')")
   db.execSQL("INSERT INTO history VALUES ('one',123,4)");db.version=1
  }
  val repo=CollectionRepository(context,name)
  try {repo.load();assertEquals("Saved playlist",repo.state.value.playlists.single().name);assertEquals("one",repo.state.value.playlists.single().entries.single().trackUri);assertEquals(setOf("one"),repo.state.value.favorites);assertEquals("Ocean",repo.state.value.pref("theme"));assertEquals(4,repo.state.value.history.single().count);assertTrue(repo.state.value.recommendationPlays.isEmpty());repo.recordPlay("one");assertEquals(1,repo.state.value.recommendationPlays.single().count)} finally {repo.close();context.deleteDatabase(name)}
 }
 @Test fun monthlyBucketsRespectLocalCalendarAndDisabledConcurrentWrites():Unit=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext;val name="months-${System.nanoTime()}.db";val repo=CollectionRepository(context,name)
  try {
   val format=java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",java.util.Locale.US)
   repo.load();repo.recordPlay("one",format.parse("2026-09-30 23:59")!!.time)
   repo.recordPlay("one",format.parse("2026-10-01 00:01")!!.time);repo.recordPlay("one",format.parse("2026-10-02 12:00")!!.time)
   assertEquals(3,repo.state.value.recommendationPlays.single().count)
   assertEquals(mapOf("2026-09" to 1,"2026-10" to 2),repo.state.value.monthPlays.associate {it.month to it.count})
   repo.setPreference("for_you_enabled","false");repo.clearRecommendations()
   kotlinx.coroutines.coroutineScope {repeat(10) {launch {repo.recordPlay("late-$it")}}}
   assertTrue(repo.state.value.recommendationPlays.isEmpty());assertTrue(repo.state.value.monthPlays.isEmpty())
  } finally {repo.close();context.deleteDatabase(name)}
 }

}
