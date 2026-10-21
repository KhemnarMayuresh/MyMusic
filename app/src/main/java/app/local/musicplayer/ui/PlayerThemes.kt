package app.local.musicplayer.ui

data class PlayerTheme(val name:String,val style:Int,val top:Long,val bottom:Long,val accent:Long,val id:String=name,val coverColors:Boolean=true) { val layout:PlayerLayout get()=when(style) {0,9,17->PlayerLayout.INLINE_ACTIONS;3->PlayerLayout.RING;6->PlayerLayout.LIGHT_PANEL;7,16->PlayerLayout.HEADER_PROGRESS;8->PlayerLayout.IMMERSIVE;else->PlayerLayout.STANDARD} }
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
  PlayerTheme("Retro",11,0xFF352542,0xFF705268,0xFFFFBDA7),
  PlayerTheme("Duotone",14,0xFF281039,0xFF803A61,0xFFFFB6C9),
  PlayerTheme("Pulse",15,0xFF071E20,0xFF114D46,0xFF73F5D0),
  PlayerTheme("Film",16,0xFF21170E,0xFF5B4838,0xFFFFD399),
  PlayerTheme("Halo",17,0xFF121032,0xFF393672,0xFFC8BBFF))
 fun options(raw:String="")=all+CustomThemes.decode(raw)
 fun find(name:String,raw:String="")=options(raw).find {it.id==name} ?: all.first()
}
