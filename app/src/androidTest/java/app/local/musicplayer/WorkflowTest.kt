package app.local.musicplayer

import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import app.local.musicplayer.collections.CollectionRepository
import app.local.musicplayer.library.MediaLibrary
import app.local.musicplayer.model.OperationResult
import app.local.musicplayer.backup.BackupCodec
import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.MediaItem
import java.util.concurrent.TimeUnit
import app.local.musicplayer.transfer.TransferArchive
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import androidx.media3.session.SessionCommand
import android.os.Bundle
import android.content.ContentValues
import android.provider.MediaStore
import android.media.MediaMetadataRetriever
import app.local.musicplayer.files.TagFileWriter
import app.local.musicplayer.files.TagChange
import app.local.musicplayer.model.TrackId
import app.local.musicplayer.playback.TimerStore
import app.local.musicplayer.playback.SleepDeadline
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkflowTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    @Test fun commandsIssuedDuringCrossfadeAreNotLostOnPromotion(): Unit = runBlocking {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val repository=(context.applicationContext as MusicApplication).collections
        repository.setPreference("crossfade","true");repository.setPreference("crossfade_seconds","2")
        val tracks=(MediaLibrary(context).rescan() as OperationResult.Success).value
        val controller=MediaController.Builder(context,SessionToken(context,ComponentName(context,"app.local.musicplayer.playback.PlaybackService"))).buildAsync().get(15,TimeUnit.SECONDS)
        try {
            instrumentation.runOnMainSync {controller.setMediaItems(listOf(MediaItem.fromUri(tracks.first {it.title=="Dawn"}.id.uri),MediaItem.fromUri(tracks.first {it.title=="Night"}.id.uri)),0,9000);controller.prepare();controller.play()}
            var position=0L;var index=0;val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
            while(position<10800 && index==0 && System.nanoTime()<deadline) {Thread.sleep(25);instrumentation.runOnMainSync {position=controller.currentPosition;index=controller.currentMediaItemIndex}}
            assertEquals(0,index);assertTrue(position>=10800)
            instrumentation.runOnMainSync {controller.addMediaItem(MediaItem.fromUri(tracks.first {it.title=="Dawn reprise"}.id.uri));controller.setPlaybackSpeed(1.5f);controller.repeatMode=androidx.media3.common.Player.REPEAT_MODE_ALL}
            while(index==0 && System.nanoTime()<deadline) {Thread.sleep(25);instrumentation.runOnMainSync {index=controller.currentMediaItemIndex}}
            instrumentation.runOnMainSync {assertEquals(3,controller.mediaItemCount);assertEquals(1.5f,controller.playbackParameters.speed);assertEquals(androidx.media3.common.Player.REPEAT_MODE_ALL,controller.repeatMode)}
        } finally {instrumentation.runOnMainSync {controller.pause();controller.release()};repository.setPreference("crossfade","false")}
    }
    @Test fun explicitPauseDuringNonGaplessTransitionCannotBeUndone(): Unit = runBlocking {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val repository=(context.applicationContext as MusicApplication).collections
        repository.setPreference("crossfade","false");repository.setPreference("gapless","false")
        val tracks=(MediaLibrary(context).rescan() as OperationResult.Success).value
        val controller=MediaController.Builder(context,SessionToken(context,ComponentName(context,"app.local.musicplayer.playback.PlaybackService"))).buildAsync().get(15,TimeUnit.SECONDS)
        try {
            instrumentation.runOnMainSync {controller.setMediaItems(listOf(MediaItem.fromUri(tracks.first {it.title=="Dawn"}.id.uri),MediaItem.fromUri(tracks.first {it.title=="Night"}.id.uri)),0,11500);controller.prepare();controller.play()}
            var pauseWindow=false;val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
            while(!pauseWindow && System.nanoTime()<deadline) {Thread.sleep(10);instrumentation.runOnMainSync {pauseWindow=controller.currentMediaItemIndex==1 && !controller.playWhenReady}}
            assertTrue("Must observe the deliberate transition interval",pauseWindow)
            instrumentation.runOnMainSync {controller.pause()}
            Thread.sleep(600)
            instrumentation.runOnMainSync {assertFalse("A controller pause must cancel automatic resume",controller.playWhenReady)}
        } finally {instrumentation.runOnMainSync {controller.pause();controller.release()};repository.setPreference("gapless","true")}
    }
    @Test fun timerDeadlineSurvivesOwnerRecreationWithinTheSameBoot() {
        val store=TimerStore(context,"timer-test")
        store.save(SleepDeadline(android.os.SystemClock.elapsedRealtime(),5000,true),"entry-a")
        val restored=TimerStore(context,"timer-test").restore()!!
        assertEquals("entry-a",restored.entryId)
        assertTrue(restored.deadline.finishCurrentTrack)
        assertTrue(restored.deadline.remaining(android.os.SystemClock.elapsedRealtime()) in 4000..5000)
        store.clear();assertNull(TimerStore(context,"timer-test").restore())
    }
    @Test fun songActionsActuallyAddTheSelectedSongToExistingPlaylist(): Unit = runBlocking {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val repository=(context.applicationContext as MusicApplication).collections
        val id=repository.createPlaylist("UI queue test")
        val device=UiDevice.getInstance(instrumentation)
        try {
            device.wakeUp();device.executeShellCommand("wm dismiss-keyguard")
            context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
            val ready=device.wait(Until.hasObject(By.desc("Song actions")),60000)
            if(!ready) {
                device.executeShellCommand("mkdir -p /sdcard/Download/MusicPlayerTestOutput")
                device.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/playlist-failure.png")
                val hierarchy=java.io.ByteArrayOutputStream();device.dumpWindowHierarchy(hierarchy);println("UI_FAILURE_HIERARCHY="+hierarchy.toString("UTF-8"))
            }
            assertTrue("Song action buttons must be accessible",ready)
            device.findObjects(By.desc("Song actions")).first().click()
            assertTrue(device.wait(Until.hasObject(By.text("Add to playlist")),30000))
            device.findObject(By.text("Add to playlist")).click()
            assertTrue(device.wait(Until.hasObject(By.text("UI queue test")),30000))
            device.findObject(By.text("UI queue test")).click()
            val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
            var count=0
            while(count==0 && System.nanoTime()<deadline) {Thread.sleep(100);count=repository.export().playlists.first {it.id==id}.tracks.size}
            assertEquals("The selected song must survive closing the picker",1,count)
        } finally {repository.deletePlaylist(id)}
    }
    @Test fun mp3TagWriteChangesPhysicalMetadataOnAnOwnedCopyOnly(): Unit = runBlocking {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val source=(MediaLibrary(context).rescan() as OperationResult.Success).value.firstOrNull {it.mimeType=="audio/mpeg"}
        org.junit.Assume.assumeTrue("Requires a local MP3 to copy for the integration fixture",source!=null && Build.VERSION.SDK_INT>=29)
        val uri=context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,ContentValues().apply {put(MediaStore.Audio.Media.DISPLAY_NAME,"Tag writer test.mp3");put(MediaStore.Audio.Media.MIME_TYPE,"audio/mpeg");put(MediaStore.Audio.Media.RELATIVE_PATH,"Music/CloneFixtures/");put(MediaStore.Audio.Media.IS_PENDING,1)})!!
        var backup: File?=null
        try {
            val original=context.contentResolver.openInputStream(Uri.parse(source!!.id.uri))!!.use {it.readBytes()}
            context.contentResolver.openOutputStream(uri)!!.use {it.write(original)}
            context.contentResolver.update(uri,ContentValues().apply {put(MediaStore.Audio.Media.IS_PENDING,0)},null,null)
            backup=TagFileWriter(context).write(source.copy(id=TrackId(uri.toString())),TagChange("Physical tag test","Fixture artist","Fixture album"))
            assertArrayEquals(original,backup.readBytes())
            val reader=MediaMetadataRetriever()
            try {reader.setDataSource(context,uri);assertEquals("Physical tag test",reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));assertEquals("Fixture artist",reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))} finally {reader.release()}
            assertArrayEquals(original,context.contentResolver.openInputStream(Uri.parse(source.id.uri))!!.use {it.readBytes()})
        } finally {
            context.contentResolver.delete(uri,null,null)
            backup?.let {File(it.parentFile,it.nameWithoutExtension+".json").delete();it.delete()}
        }
        Unit
    }
    @Test fun sleepTimerPausesActualDecodedPlayback() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val uri=runBlocking {(MediaLibrary(context).rescan() as OperationResult.Success).value.first {it.title=="Night"}.id.uri}
        val controller=MediaController.Builder(context,SessionToken(context,ComponentName(context,"app.local.musicplayer.playback.PlaybackService"))).buildAsync().get(15,TimeUnit.SECONDS)
        try {
            instrumentation.runOnMainSync {controller.setMediaItem(MediaItem.fromUri(uri));controller.prepare();controller.play()}
            var playing=false;val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
            while(!playing && System.nanoTime()<deadline) {Thread.sleep(100);instrumentation.runOnMainSync {playing=controller.isPlaying}}
            assertTrue("Must be playing before timer test",playing)
            instrumentation.runOnMainSync {controller.sendCustomCommand(SessionCommand("timer.start",Bundle.EMPTY),Bundle().apply {putLong("duration",1000);putBoolean("finish",false)})}
            Thread.sleep(1800)
            var position=0L
            instrumentation.runOnMainSync {playing=controller.isPlaying;position=controller.currentPosition}
            assertFalse("Timer must pause the player",playing)
            assertTrue("Timer must allow approximately a second of playback",position in 700..3000)
        } finally {instrumentation.runOnMainSync {controller.pause();controller.release()}}
    }
    @Test fun crossfadeStartsNextDecoderBeforeCurrentTrackFinishes() = runBlocking<Unit> {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val application=context.applicationContext as MusicApplication
        application.collections.setPreference("crossfade","true")
        application.collections.setPreference("crossfade_seconds","2")
        val tracks=(MediaLibrary(context).rescan() as OperationResult.Success).value
        val first=tracks.first {it.title=="Dawn"};val second=tracks.first {it.title=="Night"}
        val future=MediaController.Builder(context,SessionToken(context,ComponentName(context,"app.local.musicplayer.playback.PlaybackService"))).buildAsync()
        val controller=future.get(15,TimeUnit.SECONDS)
        try {
            instrumentation.runOnMainSync {controller.setMediaItems(listOf(MediaItem.fromUri(first.id.uri),MediaItem.fromUri(second.id.uri)),0,9000);controller.prepare();controller.play()}
            var index=0;var position=0L;val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
            while(index==0 && System.nanoTime()<deadline) {Thread.sleep(50);instrumentation.runOnMainSync {index=controller.currentMediaItemIndex;position=controller.currentPosition}}
            assertEquals(1,index)
            assertTrue("Crossfade must decode overlapping audio, not just advance normally; position=$position",position>=500)
        } finally {instrumentation.runOnMainSync {controller.pause();controller.release()};application.collections.setPreference("crossfade","false")}
    }
    @Test fun transferRoundTripVerifiesBytesAndPreservesSourceMusic(): Unit = runBlocking {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val source=(MediaLibrary(context).rescan() as OperationResult.Success).value.first {it.title=="Dawn"}
        val repository=CollectionRepository(context,"test-transfer.db")
        val playlist=repository.createPlaylist("Transferred tones");repository.addToPlaylist(playlist,listOf(source.id.uri,source.id.uri))
        val archive=TransferArchive(context,repository)
        val file=File(context.cacheDir,"test-transfer.zip")
        var created=emptyList<String>()
        fun digest(uri: String)=context.contentResolver.openInputStream(Uri.parse(uri))!!.use {MessageDigest.getInstance("SHA-256").digest(it.readBytes()).toList()}
        val before=digest(source.id.uri)
        try {
            archive.export(Uri.fromFile(file),listOf(source)) { }
            created=archive.importPackage(Uri.fromFile(file)) { }
            assertEquals(1,created.size);assertEquals(before,digest(created.single()));assertEquals(before,digest(source.id.uri))
            assertEquals(listOf(created.single(),created.single()),repository.export().playlists.last().tracks)
        } finally {created.forEach {context.contentResolver.delete(Uri.parse(it),null,null)};repository.close();context.deleteDatabase("test-transfer.db");file.delete()}
        Unit
    }
    @Test fun serviceDecodesFixtureAndBackgroundSessionAdvances() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val uri=runBlocking { (MediaLibrary(context).rescan() as OperationResult.Success).value.first { it.title=="Dawn" }.id.uri }
        val future=MediaController.Builder(context,SessionToken(context,ComponentName(context,"app.local.musicplayer.playback.PlaybackService"))).buildAsync()
        val controller=future.get(15,TimeUnit.SECONDS)
        try {
            instrumentation.runOnMainSync { controller.setMediaItem(MediaItem.fromUri(uri)); controller.prepare(); controller.play() }
            val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15)
            var position=0L
            while(System.nanoTime()<deadline && position<500) {
                Thread.sleep(250)
                instrumentation.runOnMainSync { position=controller.currentPosition }
            }
            assertTrue("Playback position must advance for real decoded WAV",position>=500)
            instrumentation.runOnMainSync { controller.pause() }
        } finally { instrumentation.runOnMainSync { controller.release() } }
    }
    @Test fun libraryUsesRealMediaStoreAndFindsGeneratedFixtures() = runBlocking {
        val permission=if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,permission)
        val library=MediaLibrary(context)
        val result=library.rescan()
        assertTrue(result is OperationResult.Success)
        val tracks=(result as OperationResult.Success).value
        assertTrue("Dawn WAV fixture must be installed",tracks.any { it.title=="Dawn" && it.durationMs==12000L })
        assertTrue(tracks.none { it.durationMs<=0 })
        assertEquals(tracks.size,tracks.map { it.id }.distinct().size)
    }
    @Test fun collectionRestoreIsTransactionalAndDuplicatesSurvive(): Unit = runBlocking {
        val repository=CollectionRepository(context,"test-collections.db")
        val id=repository.createPlaylist("Integration playlist")
        repository.addToPlaylist(id,listOf("content://one","content://one"))
        val backup=repository.export()
        assertEquals(listOf("content://one","content://one"),backup.playlists.single().tracks)
        try { repository.restore(BackupCodec.decode("{broken")); fail("Must reject malformed backup") } catch(_: IllegalArgumentException) { }
        assertEquals("Integration playlist",repository.export().playlists.single().name)
        repository.restore(backup)
        assertEquals(2,repository.export().playlists.single().tracks.size)
        repository.close()
        context.deleteDatabase("test-collections.db")
        Unit
    }
    @Test fun launcherShowsLibraryAndCanNavigateSettingsWithoutAds() {
        val device=UiDevice.getInstance(instrumentation)
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent=context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        assertTrue(device.wait(Until.hasObject(By.text("Songs")),60000))
        device.findObject(By.desc("Open menu")).click()
        assertTrue(device.wait(Until.hasObject(By.text("Settings")),30000))
        device.findObject(By.text("Settings")).click()
        assertTrue(device.wait(Until.hasObject(By.text("Scan music")),30000))
        assertFalse(device.hasObject(By.text("Go Premium")))
    }
}
