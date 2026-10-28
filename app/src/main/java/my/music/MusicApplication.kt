package my.music

import android.app.Application
import my.music.collections.CollectionRepository
import my.music.library.MediaLibrary
import kotlinx.coroutines.*

class MusicApplication: Application() {
    val applicationScope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    val collections by lazy { CollectionRepository(this) }
    val library by lazy { MediaLibrary(this) }
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { collections.load() }
    }
}
