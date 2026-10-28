package my.music.playback

import java.nio.ByteBuffer
import kotlin.math.sqrt

/** One audio-render-thread writer; immutable snapshots safely published to the UI. */
class AudioSignal {
    private var energy=0.0
    private var count=0
    private val history=FloatArray(43)
    @Volatile private var published=FloatArray(43)
    @Volatile private var updated=0L
    fun accept(pcm:ByteBuffer,sampleRate:Int,channels:Int) {
        val window=(sampleRate.coerceAtLeast(1)*channels.coerceAtLeast(1)/30).coerceAtLeast(1)
        var i=pcm.position()
        while(i+1<pcm.limit()) {
            val sample=(((pcm.get(i).toInt() and 255) or (pcm.get(i+1).toInt() shl 8)).toShort().toDouble()/32768.0)
            energy+=sample*sample;count++;i+=2
            if(count>=window) {
                history.copyInto(history,0,1)
                history[history.lastIndex]=(sqrt(energy/count)*2).toFloat().coerceIn(0f,1f)
                published=history.copyOf();updated=System.nanoTime();energy=0.0;count=0
            }
        }
    }
    fun snapshot(now:Long=System.nanoTime()):FloatArray = if(now-updated>250_000_000L) FloatArray(43) else published.copyOf()
    fun reset() {energy=0.0;count=0;history.fill(0f);published=FloatArray(43);updated=0}
}

/** Same-process visual telemetry, separate from MediaSession commands and saved state. */
object PlayerAudioSignal { @Volatile var active:AudioSignal?=null }
