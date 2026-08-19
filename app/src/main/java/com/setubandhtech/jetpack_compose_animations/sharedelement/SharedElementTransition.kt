package com.setubandhtech.jetpack_compose_animations.sharedelement

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CardItem(
    val id: Int,
    val title: String,
    val description: String,
    val color: Color
)

@Composable
fun SharedElementTransition() {
    val cards = listOf(
        CardItem(1, "Jetpack Compose 🚀", "Modern UI toolkit for Android", Color(0xFF6200EE)),
        CardItem(2, "Animations 🎨", "Bring your app to life", Color(0xFF03DAC5)),
        CardItem(3, "Material 3 ✨", "Beautiful design system", Color(0xFFE53935)),
        CardItem(4, "Kotlin 💙", "Concise and expressive", Color(0xFF1565C0)),
    )

    var selectedCard by remember { mutableStateOf<CardItem?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedVisibility(
            visible = selectedCard == null,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Tap a card 👆",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(cards) { card ->
                    SmallCard(
                        card = card,
                        onClick = { selectedCard = card }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedCard != null,
            enter = fadeIn(tween(300)) + slideInVertically(
                animationSpec = tween(400),
                initialOffsetY = { it }
            ),
            exit = fadeOut(tween(300)) + slideOutVertically(
                animationSpec = tween(400),
                targetOffsetY = { it }
            )
        ) {
            selectedCard?.let { card ->
                DetailScreen(
                    card = card,
                    onBack = { selectedCard = null }
                )
            }
        }
    }
}

@Composable
fun SmallCard(
    card: CardItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(card.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = card.title.takeLast(2),
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = card.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )
            Text(
                text = card.description,
                fontSize = 13.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun DetailScreen(
    card: CardItem,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(card.color)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = card.title.takeLast(2),
                fontSize = 64.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = card.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = card.description,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(60.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { onBack() }
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = "← Go Back",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}