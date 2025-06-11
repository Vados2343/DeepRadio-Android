package com.myradio.deepradio

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.myradio.deepradio.domain.MediaManager
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.delay
import javax.inject.Inject

@ActivityScoped
class AdManager @Inject constructor(
    private val activity: Activity,
    private val mediaManager: MediaManager
) {

    private var interstitialAd: InterstitialAd? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastAdTime: Long = 0
    private var wasAdLoadedAndShown = false

    private val _showAdBlockerDialog = mutableStateOf(false)
    val showAdBlockerDialog: State<Boolean> = _showAdBlockerDialog

    init {
        MobileAds.initialize(activity) { initializationStatus: InitializationStatus? ->
            Log.d("AdManager", "AdMob initialized: $initializationStatus")
            MobileAds.setRequestConfiguration(
                MobileAds.getRequestConfiguration().toBuilder()
                    .setTestDeviceIds(listOf("63CAC5CF82CC8E0395AE9AC80D69B761"))
                    .build()
            )
        }
    }

    fun loadInterstitialAd() {
        if (System.currentTimeMillis() - lastAdTime < 3600000) {
            return
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            "ca-app-pub-4731805674244275/4797452254",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    lastAdTime = System.currentTimeMillis()
                    wasAdLoadedAndShown = false
                    handler.removeCallbacks(adBlockerDetectedRunnable)
                    Log.d("AdManager", "Ad loaded successfully")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    Log.e("AdManager", "Ad failed to load: ${adError.code} - ${adError.message}")
                    scheduleAdBlockerDetected()
                }
            }
        )
    }

    fun showInterstitialAd(onAdClosed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    Log.e("AdManager", "Ad failed to show: ${adError.code} - ${adError.message}")
                    scheduleAdBlockerDetected()
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    interstitialAd = null
                    wasAdLoadedAndShown = true
                    handler.removeCallbacks(adBlockerDetectedRunnable)
                }
            }
            interstitialAd?.show(activity)
        } else {
            loadInterstitialAd()
            onAdClosed()
        }
    }

    private fun scheduleAdBlockerDetected() {
      //  if (!wasAdLoadedAndShown) {
      //      handler.postDelayed(adBlockerDetectedRunnable, 3000)
     //   }
    }

    private val adBlockerDetectedRunnable = Runnable {
        _showAdBlockerDialog.value = true
    }

    fun dismissAdBlockerDialog() {
        _showAdBlockerDialog.value = false
    }

    fun closeApp() {
        mediaManager.stop()
        activity.finishAffinity()
    }
}

@Composable
fun AdBlockerDetectedDialog(
    adManager: AdManager
) {
    var countdown by remember { mutableStateOf(30) }
    val activity = LocalContext.current as Activity

    LaunchedEffect(key1 = true) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        adManager.closeApp()
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "scale")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(scale),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Ad Blocker Detected",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = "Мы заметили, что реклама не была загружена. Это наш единственный источник дохода для поддержки и развития приложения.\n\nПожалуйста, отключите блокировщик рекламы и перезапустите приложение.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { countdown / 30f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.errorContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Приложение закроется через $countdown секунд",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { adManager.closeApp() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Закрыть приложение",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}