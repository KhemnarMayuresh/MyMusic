package my.music
import android.Manifest
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import my.music.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class LibraryThemeRevisionTest {
 @Test fun customLibraryThemeCreateReloadEditDeleteAndTabRestoration():Unit=runBlocking {
  val ins=InstrumentationRegistry.getInstrumentation();val context=ins.targetContext;val d=UiDevice.getInstance(ins)
  val repo=(context.applicationContext as MusicApplication).collections
  val old=repo.state.value.pref(CustomLibraryThemes.KEY);val selected=repo.state.value.pref("theme","Midnight")
  ins.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.READ_MEDIA_AUDIO)
  ins.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
  fun launch() {d.pressHome();context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));assertTrue(d.wait(Until.hasObject(By.desc("Open menu")),60000))}
  fun click(s:String) {assertTrue("Visible: $s",d.wait(Until.hasObject(By.text(s)),30000));d.findObject(By.text(s)).click()}
  fun await(check:()->Boolean) {val end=System.currentTimeMillis()+10000;while(!check() && System.currentTimeMillis()<end) Thread.sleep(100);assertTrue(check())}
  try {
   d.wakeUp();d.executeShellCommand("wm dismiss-keyguard");launch()
   click("Playlists");assertTrue(d.wait(Until.hasObject(By.text("New playlist")),10000))
   d.findObject(By.desc("Themes")).click();assertTrue(d.wait(Until.hasObject(By.text("Create library theme")),10000))
   click("Create library theme");assertTrue(d.wait(Until.hasObject(By.clazz("android.widget.EditText")),10000));d.findObject(By.clazz("android.widget.EditText")).setText("Library test")
   d.findObject(By.desc("Choose top color")).click();assertTrue(d.wait(Until.hasObject(By.desc("Blue")),10000));d.findObject(By.desc("Blue")).click();click("Use color");click("Save and apply")
   await {CustomLibraryThemes.decode(repo.state.value.pref(CustomLibraryThemes.KEY)).any {it.name=="Library test"}}
   val v=CustomLibraryThemes.decode(repo.state.value.pref(CustomLibraryThemes.KEY)).first {it.name=="Library test"};assertEquals(v.id,repo.state.value.pref("theme"));assertEquals(0xFF102B52L,v.colors.first())
   assertTrue(d.wait(Until.hasObject(By.text("Create library theme")),10000));d.findObject(By.desc("Back")).click();assertTrue("Return to Playlists",d.wait(Until.hasObject(By.text("New playlist")),10000))
   launch();d.findObject(By.desc("Themes")).click();assertTrue(d.wait(Until.hasObject(By.text("Create library theme")),10000));d.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/library-reload-debug.png");click("Edit");assertTrue(d.wait(Until.hasObject(By.clazz("android.widget.EditText")),10000));d.findObject(By.clazz("android.widget.EditText")).setText("Edited library");click("Save and apply")
   await {CustomLibraryThemes.decode(repo.state.value.pref(CustomLibraryThemes.KEY)).any {it.id==v.id && it.name=="Edited library"}}
   d.executeShellCommand("mkdir -p /sdcard/Download/MusicPlayerTestOutput");d.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/library-custom-revision.png")
   click("Delete");assertTrue(d.wait(Until.hasObject(By.text("Delete library theme?")),10000));d.findObjects(By.text("Delete")).last().click()
   await {CustomLibraryThemes.decode(repo.state.value.pref(CustomLibraryThemes.KEY)).none {it.id==v.id}}
   assertEquals("Midnight",repo.state.value.pref("theme"))
  } finally {repo.setPreferences(mapOf(CustomLibraryThemes.KEY to old,"theme" to selected))}
 }
 @Test fun playerReturnPreservesEachLibraryTabAndFolder():Unit=runBlocking {
  val ins=InstrumentationRegistry.getInstrumentation();val context=ins.targetContext;val d=UiDevice.getInstance(ins)
  ins.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.READ_MEDIA_AUDIO)
  ins.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
  context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
  assertTrue(d.wait(Until.hasObject(By.desc("Open menu")),60000))
  val tracks=(my.music.library.MediaLibrary(context).rescan() as my.music.model.OperationResult.Success).value
  assertTrue(tracks.isNotEmpty())
  val controller=androidx.media3.session.MediaController.Builder(context,androidx.media3.session.SessionToken(context,android.content.ComponentName(context,"my.music.playback.PlaybackService"))).buildAsync().get(20,java.util.concurrent.TimeUnit.SECONDS)
  fun click(s:String) {assertTrue(d.wait(Until.hasObject(By.text(s)),20000));d.findObject(By.text(s)).click()}
  fun playerBack() {
   assertTrue(d.wait(Until.hasObject(By.desc("Open player")),20000));d.findObject(By.desc("Open player")).click()
   assertTrue(d.wait(Until.hasObject(By.desc("Collapse player")),20000));d.findObject(By.desc("Collapse player")).click()
  }
  try {
   ins.runOnMainSync {controller.pause();controller.setMediaItem(androidx.media3.common.MediaItem.Builder().setMediaId("navigation-fixture").setUri(tracks.first().id.uri).setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle("Navigation fixture").build()).build());controller.prepare()}
   for((tab,marker) in listOf("Playlists" to "New playlist","Folders" to "Sort folders","Albums" to "Sort albums","Artists" to "Sort artists")) {
    if(!d.hasObject(By.text(tab))) d.swipe(d.displayWidth*9/10,200,d.displayWidth/10,200,20)
    click(tab);playerBack()
    assertTrue("Restore $tab",d.wait(Until.hasObject(if(tab=="Playlists") By.text(marker) else By.desc(marker)),10000))
   }
   click("Folders")
   val folder=my.music.library.GroupLibrary.groups(tracks,"folder","Name A–Z").first()
   click(folder.title);assertTrue(d.wait(Until.hasObject(By.desc("Sort songs")),10000))
   playerBack();assertTrue("Folder remains open",d.wait(Until.hasObject(By.desc("Sort songs")),10000));assertTrue(d.hasObject(By.text(folder.title)))
   d.pressBack();assertTrue("Folder tab remains selected",d.wait(Until.hasObject(By.desc("Sort folders")),10000))
  } finally {ins.runOnMainSync {controller.release()}}
 }

}
