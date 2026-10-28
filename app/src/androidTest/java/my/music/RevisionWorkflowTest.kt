package my.music
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.appwidget.AppWidgetManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import androidx.media3.session.*
import androidx.media3.common.*
import my.music.library.MediaLibrary
import my.music.model.OperationResult
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RevisionWorkflowTest {
 private val instrumentation=InstrumentationRegistry.getInstrumentation()
 private val context get()=instrumentation.targetContext
 private val device get()=UiDevice.getInstance(instrumentation)
 private fun launch() {
  instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
  device.wakeUp();device.executeShellCommand("wm dismiss-keyguard")
  context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
  assertTrue(device.wait(Until.hasObject(By.desc("Open menu")),60000))
 }
 private fun screenshot(name:String) {device.executeShellCommand("mkdir -p /sdcard/Download/MusicPlayerTestOutput");device.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/$name.png")}
 private fun text(label:String) {assertTrue("Visible: $label",device.wait(Until.hasObject(By.text(label)),30000));device.findObject(By.text(label)).click()}
 @Test fun dragMovesAnOccurrenceSeveralPositionsAndBack():Unit=runBlocking {
  launch()
  val source=(MediaLibrary(context).rescan() as OperationResult.Success).value.first()
  val controller=MediaController.Builder(context,SessionToken(context,ComponentName(context,"my.music.playback.PlaybackService"))).buildAsync().get(20,TimeUnit.SECONDS)
  try {
   instrumentation.runOnMainSync {controller.pause();controller.setMediaItems((0..3).map {MediaItem.Builder().setMediaId("revision-$it").setUri(source.id.uri).setMediaMetadata(MediaMetadata.Builder().setTitle("Drag $it").setArtist("Gesture fixture").build()).build()});controller.prepare()}
   assertTrue(device.wait(Until.hasObject(By.desc("Playing queue")),30000));device.findObject(By.desc("Playing queue")).click()
   assertTrue(device.wait(Until.hasObject(By.desc("Drag to reorder Drag 0")),30000))
   fun drag(from:String,to:String) {
    val a=device.findObject(By.desc("Drag to reorder $from")).visibleBounds
    val b=device.findObject(By.desc("Drag to reorder $to")).visibleBounds
    device.swipe(a.centerX(),a.centerY(),b.centerX(),b.centerY()+15,70)
   }
   drag("Drag 0","Drag 3")
   val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);var moved=false
   while(!moved && System.nanoTime()<deadline) {Thread.sleep(100);instrumentation.runOnMainSync {moved=controller.getMediaItemAt(3).mediaId=="revision-0"}}
   assertTrue("Drag must move across more than one row",moved)
   drag("Drag 0","Drag 1")
   var first=false;val backDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
   while(!first && System.nanoTime()<backDeadline) {Thread.sleep(100);instrumentation.runOnMainSync {first=controller.getMediaItemAt(0).mediaId=="revision-0"}}
   assertTrue("Drag must work upwards too",first);screenshot("revision-queue")
  } finally {instrumentation.runOnMainSync {controller.pause();controller.release()}}
 }
 @Test fun groupedTabsExposeMenusSortingAndBulkDeletionConfirmation() {
  launch();text("Folders")
  assertTrue(device.wait(Until.hasObject(By.desc("Sort folders")),30000));device.findObject(By.desc("Sort folders")).click();text("Song count")
  assertTrue(device.wait(Until.hasObject(By.descStartsWith("folder actions:")),30000))
  val menu=device.findObjects(By.descStartsWith("folder actions:")).first();menu.click();assertTrue(device.wait(Until.hasObject(By.text("Hide folder")),30000))
  listOf("Play","Play next","Add to queue","Add to playlist","Hide folder").forEach {assertTrue(device.hasObject(By.text(it)))}
  device.pressBack();assertTrue(device.wait(Until.hasObject(By.desc("Select folders")),30000));device.findObject(By.desc("Select folders")).click();text("Delete");assertTrue(device.hasObject(By.text("Delete selected music?")));text("Cancel")
  if(!device.hasObject(By.text("Albums"))) {val tabY=device.findObject(By.text("Folders")).visibleBounds.centerY();device.swipe(device.displayWidth*9/10,tabY,device.displayWidth/10,tabY,30)}
  text("Albums");assertTrue(device.wait(Until.hasObject(By.desc("Sort albums")),30000));assertTrue(device.wait(Until.hasObject(By.descStartsWith("album actions:")),30000))
 }
 @Test fun themePreviewAppliesAndThreeWidgetProvidersExist():Unit=runBlocking {
  val repository=(context.applicationContext as MusicApplication).collections
  repository.setPreference("player_theme","Artwork")
  launch();device.findObject(By.desc("Themes")).click();text("Player");text("Vinyl");text("Apply")
  val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
  while(repository.state.value.pref("player_theme")!="Vinyl" && System.nanoTime()<deadline) Thread.sleep(100)
  assertEquals("Vinyl",repository.state.value.pref("player_theme"));assertTrue(device.hasObject(By.text("Applied")));screenshot("revision-themes")
  val providers=AppWidgetManager.getInstance(context).installedProviders.filter {it.provider.packageName==context.packageName}
  assertEquals(3,providers.size)
  providers.forEach {provider->assertNotNull(android.widget.RemoteViews(context.packageName,provider.initialLayout).apply(context,null))}
  device.pressBack();assertTrue(device.wait(Until.hasObject(By.desc("Open menu")),30000));device.findObject(By.desc("Open menu")).click();text("Widgets")
  assertTrue(device.wait(Until.hasObject(By.text("Classic • 4 × 1")),30000));screenshot("revision-widgets")
 }
 @Test fun lockScreenPreferenceIsPresentAndPersists():Unit=runBlocking {
  val repository=(context.applicationContext as MusicApplication).collections
  repository.setPreference("lock_screen","true")
  launch();device.findObject(By.desc("Settings")).click()
  UiScrollable(UiSelector().scrollable(true)).scrollTextIntoView("Lock screen playing")
  text("Lock screen playing")
  val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
  while(repository.state.value.flag("lock_screen",true) && System.nanoTime()<deadline) Thread.sleep(100)
  assertFalse(repository.state.value.flag("lock_screen",true))
  repository.setPreference("lock_screen","true")
 } @Test fun dragContinuesAfterAutoScrollRecyclesTheStartingRow():Unit=runBlocking {
  launch()
  val source=(MediaLibrary(context).rescan() as OperationResult.Success).value.first()
  val controller=MediaController.Builder(context,SessionToken(context,ComponentName(context,"my.music.playback.PlaybackService"))).buildAsync().get(20,TimeUnit.SECONDS)
  try {
   instrumentation.runOnMainSync {controller.pause();controller.setMediaItems((0..24).map {MediaItem.Builder().setMediaId("long-$it").setUri(source.id.uri).setMediaMetadata(MediaMetadata.Builder().setTitle("Long $it").build()).build()});controller.prepare()}
   assertTrue(device.wait(Until.hasObject(By.desc("Playing queue")),30000));device.findObject(By.desc("Playing queue")).click()
   assertTrue(device.wait(Until.hasObject(By.desc("Drag to reorder Long 0")),30000))
   val start=device.findObject(By.desc("Drag to reorder Long 0")).visibleBounds
   val footer=device.findObject(By.desc("Playing queue")).visibleBounds
   val edge=footer.top-30
   assertTrue(device.swipe(arrayOf(android.graphics.Point(start.centerX(),start.centerY()),android.graphics.Point(start.centerX(),edge),android.graphics.Point(start.centerX(),edge),android.graphics.Point(start.centerX(),edge)),150))
   var position=0;val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
   while(position<12 && System.nanoTime()<deadline) {Thread.sleep(100);instrumentation.runOnMainSync {position=(0 until controller.mediaItemCount).first {controller.getMediaItemAt(it).mediaId=="long-0"}}}
   assertTrue("Dragged occurrence must pass the initially visible rows; got $position",position>=12)
  } finally {instrumentation.runOnMainSync {controller.pause();controller.release()}}
 } @Test fun widgetControlsFitTheirMinimumHeightWithoutClipping() {
  instrumentation.runOnMainSync {
   val density=context.resources.displayMetrics.density
   listOf(R.layout.player_widget to 64,R.layout.widget_compact to 64,R.layout.widget_artwork to 110).forEach {(layout,heightDp)->
    val root=android.widget.RemoteViews(context.packageName,layout).apply(context,null) as android.view.ViewGroup
    val width=(320*density).toInt();val height=(heightDp*density).toInt()
    root.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(height,android.view.View.MeasureSpec.EXACTLY));root.layout(0,0,width,height)
    listOf(R.id.widget_play,R.id.widget_previous,R.id.widget_next,R.id.widget_favorite,R.id.widget_queue).forEach {id->
     val child=root.findViewById<android.view.View>(id);val bounds=android.graphics.Rect();child.getDrawingRect(bounds);root.offsetDescendantRectToMyCoords(child,bounds)
     assertTrue("Widget control clips at minimum height: $layout / $id / $bounds",bounds.top>=0 && bounds.bottom<=height && bounds.left>=0 && bounds.right<=width)
    }
   }
  }
 }}
