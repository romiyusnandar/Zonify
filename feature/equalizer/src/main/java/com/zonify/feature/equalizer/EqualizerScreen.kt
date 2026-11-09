package com.zonify.feature.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zonify.core.audio.AudioEngineBridge
import com.zonify.core.model.FilterType
import com.zonify.core.model.ParametricBand
import com.zonify.core.ui.theme.AppleMusicCardDark
import com.zonify.core.ui.theme.AppleMusicDarkBackground
import com.zonify.core.ui.theme.AppleMusicPink
import com.zonify.core.ui.theme.BitPerfectCyan

import androidx.compose.material.icons.filled.Usb
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.zonify.core.audio.UsbDacManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val usbDacManager = remember { UsbDacManager.getInstance(context) }
    val connectedDac by usbDacManager.connectedDac.collectAsState()
    val isBitPerfectLocked by usbDacManager.isBitPerfectLocked.collectAsState()

    var isEqEnabled by remember { mutableStateOf(true) }
    var preampDb by remember { mutableFloatStateOf(0f) }

    // Sample default audiophile parametric bands
    var bands by remember {
        mutableStateOf(
            listOf(
                ParametricBand(1, true, 32f, 0f, 1.41f, FilterType.LOW_SHELF),
                ParametricBand(2, true, 64f, 1.5f, 1.41f, FilterType.PEAKING),
                ParametricBand(3, true, 250f, -0.5f, 1.41f, FilterType.PEAKING),
                ParametricBand(4, true, 1000f, 0f, 1.41f, FilterType.PEAKING),
                ParametricBand(5, true, 4000f, 2.0f, 1.41f, FilterType.PEAKING),
                ParametricBand(6, true, 12000f, 1.0f, 1.41f, FilterType.HIGH_SHELF)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audiophile DSP & EQ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleMusicDarkBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = AppleMusicDarkBackground,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hardware USB DAC Status Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (connectedDac != null) BitPerfectCyan.copy(alpha = 0.12f) else AppleMusicCardDark
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = null,
                                tint = if (connectedDac != null) BitPerfectCyan else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = connectedDac?.displayName ?: "Internal Audio Route",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (connectedDac != null) {
                                        "Max: ${(connectedDac?.supportedSampleRates?.maxOrNull() ?: 192000) / 1000.0} kHz • VID: ${connectedDac?.vendorId} PID: ${connectedDac?.productId}"
                                    } else {
                                        "No USB DAC connected. Plug in a USB-C DAC for bit-perfect output."
                                    },
                                    color = if (connectedDac != null) BitPerfectCyan.copy(alpha = 0.8f) else Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Master Switch & Bit-Perfect Mode Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppleMusicCardDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "DSP & Parametric EQ",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Switch(
                                checked = isEqEnabled,
                                onCheckedChange = {
                                    isEqEnabled = it
                                    AudioEngineBridge.setEqualizerEnabled(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AppleMusicPink)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Direct Bit-Perfect Mode",
                                    color = BitPerfectCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Locks Android 14+ mixer directly to DAC without resampling",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = isBitPerfectLocked,
                                onCheckedChange = {
                                    usbDacManager.setBitPerfectMode(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = BitPerfectCyan)
                            )
                        }
                    }
                }
            }

            // Preamp Slider
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppleMusicCardDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Digital Pre-Amp Gain", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(String.format("%+.1f dB", preampDb), color = AppleMusicPink)
                        }
                        Slider(
                            value = preampDb,
                            onValueChange = {
                                preampDb = it
                                AudioEngineBridge.setPreamp(it)
                            },
                            valueRange = -15f..15f,
                            enabled = isEqEnabled
                        )
                    }
                }
            }

            // Parametric Bands
            items(bands) { band ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppleMusicCardDark),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Band ${band.id}: ${band.frequencyHz.toInt()} Hz (${band.filterType.name})",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                String.format("%+.1f dB (Q=%.2f)", band.gainDb, band.qFactor),
                                color = AppleMusicPink,
                                fontSize = 13.sp
                            )
                        }

                        Slider(
                            value = band.gainDb,
                            onValueChange = { newGain ->
                                val updated = band.copy(gainDb = newGain)
                                bands = bands.map { if (it.id == band.id) updated else it }
                                AudioEngineBridge.updateParametricBand(updated)
                            },
                            valueRange = -15f..15f,
                            enabled = isEqEnabled
                        )
                    }
                }
            }
        }
    }
}
