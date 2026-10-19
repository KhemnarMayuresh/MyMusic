package app.local.musicplayer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import app.local.musicplayer.playback.EqProcessor
import org.junit.Assert.*
import org.junit.Test

class EqProcessorTest {
    @Test fun pipelineEmptyBufferDoesNotAttemptSelfCopy() {
        val processor=EqProcessor()
        processor.configure(AudioProcessor.AudioFormat(44100,1,C.ENCODING_PCM_16BIT))
        processor.flush()
        processor.queueInput(AudioProcessor.EMPTY_BUFFER)
        assertEquals(0,processor.output.remaining())
    }
    @Test fun flatEqualizerFeedsVisualsWithoutChangingPcm() {
        val processor=EqProcessor()
        processor.configure(AudioProcessor.AudioFormat(48000,1,C.ENCODING_PCM_16BIT));processor.flush()
        val input=java.nio.ByteBuffer.allocateDirect(3200).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        repeat(1600) {input.putShort(8192)};input.flip();processor.queueInput(input)
        val output=processor.output.order(java.nio.ByteOrder.LITTLE_ENDIAN)
        assertEquals(.5f,processor.signal.snapshot().last(),.001f)
        repeat(1600) {assertEquals(8192,output.short.toInt())}
        processor.flush();assertTrue(processor.signal.snapshot().all {it==0f})
    }
}
