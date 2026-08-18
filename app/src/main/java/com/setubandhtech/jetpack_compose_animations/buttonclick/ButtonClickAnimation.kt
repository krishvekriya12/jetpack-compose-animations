package com.setubandhtech.jetpack_compose_animations.buttonclick

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ButtonClickAnimation() {
    var isClicked by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isClicked) 0.85f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "button_scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isClicked) Color(0xFF6200EE) else Color(0xFF03DACE),
        animationSpec = tween(durationMillis = 150),
        label = "button_color"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Button(
            onClick = {
                isClicked = !isClicked
            },
            modifier = Modifier
                .scale(scale)
                .width(200.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor
            )
        ) {
            Text(
                text = if (isClicked)"Clicked! 🎉" else "Click Me!",
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}