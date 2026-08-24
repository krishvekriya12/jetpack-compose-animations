package com.setubandhtech.jetpack_compose_animations

import android.os.Bundle
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.setubandhtech.jetpack_compose_animations.animatedvisibility.AnimatedVisibilityDemo
import com.setubandhtech.jetpack_compose_animations.arcreactor.IronManArcReactor
import com.setubandhtech.jetpack_compose_animations.bankingdashboard.BankingDashboard
import com.setubandhtech.jetpack_compose_animations.bottombar.BottomBarAnimation
import com.setubandhtech.jetpack_compose_animations.buttonclick.ButtonClickAnimation
import com.setubandhtech.jetpack_compose_animations.captainshield.CaptainAmericaShield
import com.setubandhtech.jetpack_compose_animations.cardflip.CardFlipAnimation
import com.setubandhtech.jetpack_compose_animations.diwalifireworks.DiwaliFireworks
import com.setubandhtech.jetpack_compose_animations.dnahelix.DNAHelixAnimation
import com.setubandhtech.jetpack_compose_animations.ganeshchaturthi.GaneshChaturthi
import com.setubandhtech.jetpack_compose_animations.holicolors.HoliColorSplash
import com.setubandhtech.jetpack_compose_animations.liquidfill.LiquidFillButton
import com.setubandhtech.jetpack_compose_animations.lottie.LottieAnimationDemo
import com.setubandhtech.jetpack_compose_animations.newyearcountdown.NewYearCountdown
import com.setubandhtech.jetpack_compose_animations.particleexplosion.ParticleExplosion
import com.setubandhtech.jetpack_compose_animations.sharedelement.SharedElementTransition
import com.setubandhtech.jetpack_compose_animations.shimmer.ShimmerCard
import com.setubandhtech.jetpack_compose_animations.shimmer.ShimmerEffect
import com.setubandhtech.jetpack_compose_animations.spidermanweb.SpiderManWebShooter
import com.setubandhtech.jetpack_compose_animations.swipetodelete.SwipeToDelete
import com.setubandhtech.jetpack_compose_animations.thanossnap.ThanosSnap
import com.setubandhtech.jetpack_compose_animations.thorlightning.ThorLightningEffect
import com.setubandhtech.jetpack_compose_animations.ui.theme.JetpackcomposeanimationsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetpackcomposeanimationsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BankingDashboard()
                }
            }
        }
    }
}

