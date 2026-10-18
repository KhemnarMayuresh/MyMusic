@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package app.local.musicplayer.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EqProcessor: BaseAudioProcessor() {
    @Volatile private var requested: List<Float> = List(10) {0f}
    private var applied: List<Float>?=null
    private var filter: EqFilter?=null
    fun setGains(gains: List<Float>) {require(gains.size==10);requested=gains.toList()}
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if(inputAudioFormat.encoding!=C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        return inputAudioFormat
    }
    override fun queueInput(inputBuffer: ByteBuffer) {
        // Media3 submits its shared EMPTY_BUFFER while draining the pipeline.
        if(!inputBuffer.hasRemaining()) return
        val remaining=inputBuffer.remaining();val output=replaceOutputBuffer(remaining).order(ByteOrder.LITTLE_ENDIAN)
        val desired=requested
        if(desired.all {it==0f}) {output.put(inputBuffer);output.flip();return}
        if(filter==null) {filter=EqFilter(inputAudioFormat.sampleRate,inputAudioFormat.channelCount);applied=null}
        if(applied!=desired) {filter!!.configure(desired);applied=desired}
        val samples=ShortArray(remaining/2)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for(i in samples.indices) samples[i]=inputBuffer.short
        filter!!.process(samples)
        samples.forEach(output::putShort);output.flip()
    }
    override fun onFlush() {filter=null;applied=null}
    override fun onReset() {filter=null;applied=null}
}
