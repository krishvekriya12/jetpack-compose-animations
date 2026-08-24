package com.setubandhtech.jetpack_compose_animations.bankingdashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Transaction(
    val title: String,
    val subtitle: String,
    val amount: String,
    val isCredit: Boolean,
    val icon: String,
    val time: String
)

data class QuickAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun BankingDashboard() {
    var isBalanceVisible by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    val transactions = listOf(
        Transaction("Netflix", "Entertainment", "- ₹649", false, "🎬", "Today, 2:30 PM"),
        Transaction("Salary", "HDFC Bank", "+ ₹85,000", true, "💼", "Today, 10:00 AM"),
        Transaction("Swiggy", "Food & Dining", "- ₹340", false, "🍔", "Yesterday"),
        Transaction("Freelance", "Client Payment", "+ ₹12,500", true, "💻", "Yesterday"),
        Transaction("Amazon", "Shopping", "- ₹2,399", false, "📦", "2 days ago"),
        Transaction("Uber", "Transport", "- ₹180", false, "🚗", "2 days ago"),
    )

    val quickActions = listOf(
        QuickAction("Send", Icons.Filled.Send, Color(0xFF6200EE)),
        QuickAction("Receive", Icons.Filled.AccountBalanceWallet, Color(0xFF00897B)),
        QuickAction("Pay", Icons.Filled.Payment, Color(0xFFE53935)),
        QuickAction("More", Icons.Filled.MoreHoriz, Color(0xFF1565C0))
    )

    val balanceAlpha by animateFloatAsState(
        targetValue = if (isBalanceVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "balance_alpha"
    )

    val cardRotation by animateFloatAsState(
        targetValue = if (isBalanceVisible) 0f else 180f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "card_rotation"
    )

    val greetingScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "greeting_scale"
    )

    val pulseScale by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good Morning 👋",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Setu Bandhtech",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(26.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(pulseScale)
                                .background(Color(0xFFE53935), CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6200EE),
                                        Color(0xFF03DAC5)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SB",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(200.dp)
                    .graphicsLayer {
                        rotationY = cardRotation
                        cameraDistance = 14f * density
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A0050),
                                Color(0xFF6200EE),
                                Color(0xFF9C27B0)
                            )
                        )
                    )
                    .clickable { isBalanceVisible = !isBalanceVisible }
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 60.dp, y = (-60).dp)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-40).dp, y = 40.dp)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                )

                if (cardRotation <= 90f) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SETUBANDHTECH BANK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            Icon(
                                imageVector = Icons.Filled.CreditCard,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Column {
                            Text(
                                text = "Total Balance",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isBalanceVisible) {
                                Text(
                                    text = "₹ 1,24,839.50",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = balanceAlpha
                                    }
                                )
                            } else {
                                Text(
                                    text = "₹ ••••••••",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "CARD NUMBER",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "•••• •••• •••• 4821",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "VALID THRU",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "12/28",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isBalanceVisible) "Tap card to hide balance 👆" else "Tap card to show balance 👆",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                quickActions.forEach { action ->
                    QuickActionButton(action = action)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Income",
                    amount = "₹97,500",
                    change = "+12.5%",
                    isPositive = true,
                    color = Color(0xFF00897B)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Expense",
                    amount = "₹3,568",
                    change = "-4.2%",
                    isPositive = false,
                    color = Color(0xFFE53935)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "See All",
                    fontSize = 13.sp,
                    color = Color(0xFF6200EE),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(transactions) { transaction ->
            TransactionItem(transaction = transaction)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun QuickActionButton(action: QuickAction) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "action_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .clip(RoundedCornerShape(18.dp))
                .background(action.color.copy(alpha = 0.15f))
                .border(
                    width = 1.dp,
                    color = action.color.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable {
                    isPressed = !isPressed
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = action.color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    change: String,
    isPositive: Boolean,
    color: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "stat_progress"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A2E))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = change,
                        fontSize = 11.sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = amount,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isPositive) 0.75f * animatedProgress else 0.35f * animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val itemAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "item_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .graphicsLayer { alpha = itemAlpha }
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A2E))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (transaction.isCredit)
                        Color(0xFF00897B).copy(alpha = 0.15f)
                    else
                        Color(0xFFE53935).copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.icon,
                fontSize = 22.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = transaction.subtitle,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = transaction.amount,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isCredit) Color(0xFF00E676) else Color(0xFFFF5252)
            )
            Text(
                text = transaction.time,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}