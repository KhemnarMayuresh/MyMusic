package my.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import my.music.R
import kotlinx.coroutines.delay

@Composable fun AppIntro(content:@Composable ()->Unit) {
 var completed by rememberSaveable {mutableStateOf(false)}
 val visibility=remember {MutableTransitionState(!completed)}
 val showing=visibility.currentState || visibility.targetState
 LaunchedEffect(Unit) {if(!completed) {delay(1400);visibility.targetState=false}}
 LaunchedEffect(showing) {if(!showing) completed=true}
 BackHandler(showing) {visibility.targetState=false}
 Box(Modifier.fillMaxSize()) {
  Box(if(showing) Modifier.fillMaxSize().clearAndSetSemantics {} else Modifier.fillMaxSize()) {content()}
  AnimatedVisibility(visibleState=visibility,exit=fadeOut(tween(300))) {
   val motion=rememberInfiniteTransition(label="Intro glow")
   val glow by motion.animateFloat(0f,1f,infiniteRepeatable(tween(2200),RepeatMode.Reverse),label="Gradient")
   val scale by animateFloatAsState(if(visibility.targetState) 1f else .94f,tween(600),label="Logo")
   Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(lerp(Color(0xFF170728),Color(0xFF341151),glow),Color(0xFF49136D),lerp(Color(0xFF24103D),Color(0xFF6E3248),glow)))).pointerInput(Unit) {awaitPointerEventScope {while(true) awaitPointerEvent().changes.forEach {it.consume()}}}.semantics {contentDescription="Music Player intro"},contentAlignment=Alignment.Center) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
     Image(painterResource(R.drawable.brand_icon),"Music Player logo",Modifier.size(152.dp).graphicsLayer {scaleX=scale;scaleY=scale}.clip(RoundedCornerShape(34.dp)))
     Spacer(Modifier.height(28.dp))
     Text("My Music",color=Color(0xFFFFDB9B),style=MaterialTheme.typography.headlineLarge)
     Text("Offline music, your way",color=Color(0xFFE0CCE9),modifier=Modifier.padding(top=10.dp))
    }
    TextButton(onClick={visibility.targetState=false},modifier=Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=32.dp)) {Text("Start listening",color=Color(0xFFFFD391))}
   }
  }
 }
}
