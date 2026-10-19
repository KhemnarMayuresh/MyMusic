package app.local.musicplayer.widgets
/** Identity of the artwork currently requested by the widget owner, including absence. */
internal class ArtworkRequests {
 private var selected:String?=null
 private var generation=0L
 fun select(uri:String?):Long {if(selected!=uri) {selected=uri;generation++};return generation}
 fun owns(ticket:Long,uri:String?)=ticket==generation && selected==uri
}
