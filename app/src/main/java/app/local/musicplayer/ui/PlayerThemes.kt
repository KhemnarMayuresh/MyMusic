package app.local.musicplayer.ui

data class PlayerTheme(val name:String,val style:Int,val top:Long,val bottom:Long,val accent:Long)
object PlayerThemes {
 val all=listOf(
  PlayerTheme("Artwork",0,0xFF090B22,0xFF35134D,0xFFFFBF00),
  PlayerTheme("Vinyl",1,0xFF17130F,0xFF53362C,0xFFFFC179),
  PlayerTheme("Cassette",2,0xFF10242A,0xFF276465,0xFF86E4CC),
  PlayerTheme("Orbit",3,0xFF090F2D,0xFF202D66,0xFF96B8FF),
  PlayerTheme("Waveform",4,0xFF071C1B,0xFF123F39,0xFF59E3BA),
  PlayerTheme("Neon",5,0xFF160920,0xFF411357,0xFFFF7BDC),
  PlayerTheme("Polaroid",6,0xFF342E26,0xFF776553,0xFFFFE0A3),
  PlayerTheme("Minimal",7,0xFF080808,0xFF252525,0xFFFFFFFF),
  PlayerTheme("Sunset",8,0xFF471836,0xFFCF695B,0xFFFFDB9A),
  PlayerTheme("Glass",9,0xFF103046,0xFF3B7C94,0xFFB2E6FF),
  PlayerTheme("Aurora",10,0xFF081D33,0xFF286A58,0xFF9AFFCD),
  PlayerTheme("Retro",11,0xFF352542,0xFF705268,0xFFFFBDA7))
 fun find(name:String)=all.find {it.name==name} ?: all.first()
}
