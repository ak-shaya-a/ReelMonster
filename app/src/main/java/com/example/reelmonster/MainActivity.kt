package com.example.reelmonster

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reelmonster.ui.theme.ReelMonsterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ReelMonsterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        onStartOverlay = { checkOverlayPermissionAndStart() },
                        onStopOverlay = { stopOverlayService() }
                    )
                }
            }
        }
    }

    private fun checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverApps(this)) {
            Toast.makeText(this, "Please allow 'Display over other apps'", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            startOverlayService()
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, MonsterOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, MonsterOverlayService::class.java)
        stopService(intent)
    }
}

@Composable
fun DashboardScreen(onStartOverlay: () -> Unit, onStopOverlay: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("ReelMonsterPrefs", Context.MODE_PRIVATE) }
    
    var todayCount by remember { mutableStateOf(sharedPref.getInt("today_count", 0)) }
    
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "today_count") {
                todayCount = prefs.getInt("today_count", 0)
            }
        }
        sharedPref.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPref.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val minutesLost = todayCount * 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Today", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$todayCount", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD81B60))
            Text(text = "reels • ${minutesLost / 60}h ${minutesLost % 60}m lost", fontSize = 16.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Last 7 days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.6f, 0.8f, (todayCount.coerceAtMost(100) / 100f).coerceAtLeast(0.1f))
                heights.forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight(fraction)
                            .background(Color(0xFFEC407A), shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = getMonsterQuote(todayCount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onStartOverlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD81B60)),
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("Summon Monster")
            }
            OutlinedButton(
                onClick = onStopOverlay,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("Hide Monster")
            }
        }
    }
}

fun getMonsterQuote(count: Int): String {
    return when {
        count == 0 -> "I'm hungry! Feed me some reels!"
        count < 10 -> "Yum! Just a little snack."
        count < 30 -> "Tasty reels! But shouldn't you be working?"
        count < 60 -> "Burp! Your homework says hi."
        else -> "I'm stuffed! GO OUTSIDE!"
    }
}