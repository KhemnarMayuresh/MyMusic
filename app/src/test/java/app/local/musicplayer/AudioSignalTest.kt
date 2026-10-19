package app.local.musicplayer
import app.local.musicplayer.playback.AudioSignal
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
class AudioSignalTest {
 @Test fun silenceAndLoudAudioProduceDifferentLevelsWithoutConsumingPcm() {
  val signal=AudioSignal();val pcm=ByteBuffer.allocate(3200).order(ByteOrder.LITTLE_ENDIAN)
  repeat(1600) {pcm.putShort(0)};pcm.flip();signal.accept(pcm,48000,1)
  assertTrue(signal.snapshot().all {it==0f});assertEquals(0,pcm.position())
  pcm.clear();repeat(1600) {pcm.putShort(if(it%2==0) 16384 else -16384)};pcm.flip()
  val original=pcm.array().copyOf();signal.accept(pcm,48000,1)
  assertEquals(1f,signal.snapshot().last(),.001f);assertEquals(0,pcm.position());assertArrayEquals(original,pcm.array())
  assertTrue(signal.snapshot(Long.MAX_VALUE).all {it==0f})
  signal.reset();assertTrue(signal.snapshot().all {it==0f})
 }
 @Test fun stereoAntiphaseDoesNotCancelEnergy() {
  val signal=AudioSignal();val pcm=ByteBuffer.allocate(6400).order(ByteOrder.LITTLE_ENDIAN)
  repeat(1600) {pcm.putShort(8192);pcm.putShort(-8192)};pcm.flip();signal.accept(pcm,48000,2)
  assertEquals(.5f,signal.snapshot().last(),.001f)
 }
}
