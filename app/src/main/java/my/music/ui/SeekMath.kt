package my.music.ui
import kotlin.math.atan2
object SeekMath {
 fun fraction(position:Long,duration:Long)=if(duration<=0) 0f else (position.toDouble()/duration).toFloat().coerceIn(0f,1f)
 fun position(fraction:Float,duration:Long)=if(duration<=0 || !fraction.isFinite()) 0L else (fraction.coerceIn(0f,1f).toDouble()*duration).toLong()
 fun ringFraction(x:Float,y:Float,width:Float,height:Float):Float {
  val angle=atan2((y-height/2).toDouble(),(x-width/2).toDouble())+Math.PI/2
  return (((angle+Math.PI*2)%(Math.PI*2))/(Math.PI*2)).toFloat()
 }
}
enum class PlayerLayout { STANDARD, INLINE_ACTIONS, RING, IMMERSIVE, LIGHT_PANEL, HEADER_PROGRESS }
