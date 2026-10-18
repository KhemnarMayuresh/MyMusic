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
}
