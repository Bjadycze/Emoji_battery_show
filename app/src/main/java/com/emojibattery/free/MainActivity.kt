package com.emojibattery.free

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Dokud uživatel jazyk ručně nezvolí, výchozí je angličtina (bez ohledu na jazyk systému).
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        setContent {
            EmojiBatteryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { AppScreen() }
            }
        }
    }
}

/* ---------- vzhled ---------- */

/** Adresa repozitáře. */

private const val PROJECT_URL = "https://github.com/Bjadycze/Emoji_battery_show"

private val Mint = Color(0xFF00C48C)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF00291D),
    background = Color(0xFF0F1317),
    onBackground = Color(0xFFE6ECF2),
    surface = Color(0xFF171C22),
    onSurface = Color(0xFFE6ECF2),
    surfaceVariant = Color(0xFF212932),
    onSurfaceVariant = Color(0xFFAEBAC6)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00805D),
    onPrimary = Color.White,
    background = Color(0xFFF2F5F4),
    onBackground = Color(0xFF11171B),
    surface = Color.White,
    onSurface = Color(0xFF11171B),
    surfaceVariant = Color(0xFFE7ECEA),
    onSurfaceVariant = Color(0xFF4A5560)
)

@Composable
fun EmojiBatteryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

/* ---------- obrazovka ---------- */

private enum class Screen { MAIN, MORE }

@Composable
fun AppScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var currentScreen by remember { mutableStateOf(Screen.MAIN) }

    BackHandler(enabled = currentScreen == Screen.MORE) {
        currentScreen = Screen.MAIN
    }

    var mode by remember { mutableStateOf(prefs.mode) }
    var setId by remember { mutableStateOf(prefs.setId) }
    var numberIcon by remember { mutableStateOf(prefs.numberIcon) }
    var chargingEmoji by remember { mutableStateOf(prefs.chargingEmoji) }
    var bubblePercent by remember { mutableStateOf(prefs.overlayShowPercent) }
    var bubbleSize by remember { mutableIntStateOf(prefs.overlaySize) }
    var bubbleOpacity by remember { mutableIntStateOf(prefs.overlayOpacity) }

    val battery = liveBattery()
    var previewLevel by remember { mutableIntStateOf(-1) }
    val shownLevel = if (previewLevel < 0) battery.level else previewLevel
    val shownCharging = previewLevel < 0 && battery.charging

    var canOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    var pendingMode by remember { mutableStateOf<Mode?>(null) }

    fun commit(target: Mode) {
        prefs.mode = target
        mode = target
        if (target.running) BatteryMonitorService.start(context)
        else BatteryMonitorService.stop(context)
    }

    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingMode
        pendingMode = null
        if (granted && target != null) commit(target)
    }

    fun applyMode(target: Mode) {
        if (target.showsBubble && !Settings.canDrawOverlays(context)) {
            pendingMode = target
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
            return
        }
        if (target.running && needsNotificationPermission(context)) {
            pendingMode = target
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        commit(target)
    }

    // Po návratu z obrazovky s oprávněním režim dokončíme sami.
    OnResume {
        canOverlay = Settings.canDrawOverlays(context)
        val target = pendingMode
        if (target != null && target.showsBubble && canOverlay) {
            pendingMode = null
            commit(target)
        }
    }

    val set = EmojiSets.byId(setId)
    val glyph = if (shownCharging && chargingEmoji) set.charging else set.forLevel(shownLevel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Horní lišta (CZ/EN + nastavení) – vždy viditelná, nescrolluje s obsahem.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentScreen == Screen.MORE) {
                IconButton(onClick = { currentScreen = Screen.MAIN }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }

            LanguageSwitcher()

            if (currentScreen == Screen.MAIN) {
                IconButton(onClick = { currentScreen = Screen.MORE }) {
                    Icon(Icons.Default.Settings, stringResource(R.string.more_settings))
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            if (currentScreen == Screen.MAIN) {
                MainContent(
                    set = set,
                    glyph = glyph,
                    shownLevel = shownLevel,
                    shownCharging = shownCharging,
                    chargingEmoji = chargingEmoji,
                    previewLevel = previewLevel,
                    onLevelChange = { previewLevel = it },
                    onResetPreview = { previewLevel = -1 },
                    mode = mode,
                    onModeChange = { applyMode(it) },
                    setId = setId,
                    onSetIdChange = {
                        setId = it
                        prefs.setId = it
                    },
                    numberIcon = numberIcon,
                    onNumberIconChange = {
                        numberIcon = it
                        prefs.numberIcon = it
                    },
                    onChargingEmojiChange = {
                        chargingEmoji = it
                        prefs.chargingEmoji = it
                    },
                    bubblePercent = bubblePercent,
                    onBubblePercentChange = {
                        bubblePercent = it
                        prefs.overlayShowPercent = it
                    },
                    bubbleSize = bubbleSize,
                    onBubbleSizeChange = {
                        bubbleSize = it
                        prefs.overlaySize = it
                    },
                    bubbleOpacity = bubbleOpacity,
                    onBubbleOpacityChange = {
                        bubbleOpacity = it
                        prefs.overlayOpacity = it
                    }
                )
            } else {
                MoreContent()
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MainContent(
    set: EmojiSet,
    glyph: String,
    shownLevel: Int,
    shownCharging: Boolean,
    chargingEmoji: Boolean,
    previewLevel: Int,
    onLevelChange: (Int) -> Unit,
    onResetPreview: () -> Unit,
    mode: Mode,
    onModeChange: (Mode) -> Unit,
    setId: String,
    onSetIdChange: (String) -> Unit,
    numberIcon: Boolean,
    onNumberIconChange: (Boolean) -> Unit,
    onChargingEmojiChange: (Boolean) -> Unit,
    bubblePercent: Boolean,
    onBubblePercentChange: (Boolean) -> Unit,
    bubbleSize: Int,
    onBubbleSizeChange: (Int) -> Unit,
    bubbleOpacity: Int,
    onBubbleOpacityChange: (Int) -> Unit
) {
    Column {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.app_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        /* Živý náhled – hlavní prvek obrazovky */
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (set.drawn) {
                    val preview = remember(shownLevel, shownCharging, chargingEmoji) {
                        EmojiRenderer.battery(
                            shownLevel, shownCharging && chargingEmoji, 360, colored = true
                        ).asImageBitmap()
                    }
                    Image(
                        bitmap = preview,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )
                } else {
                    Text(text = glyph, fontSize = 92.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$shownLevel %",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(if (previewLevel < 0) R.string.current_status else R.string.preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = shownLevel.toFloat(),
                    onValueChange = { onLevelChange(it.toInt()) },
                    valueRange = 0f..100f
                )
                if (previewLevel >= 0) {
                    TextButton(onClick = onResetPreview) {
                        Text(stringResource(R.string.show_current))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionTitle(stringResource(R.string.where_to_show))
        SectionCard {
            Mode.entries.forEach { item ->
                ModeRow(
                    mode = item,
                    selected = item == mode,
                    onSelect = { onModeChange(item) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.emoji_set))
        SectionCard {
            EmojiSets.all.forEach { item ->
                SetRow(
                    set = item,
                    selected = item.id == setId,
                    onSelect = { onSetIdChange(item.id) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.status_bar))
        SectionCard {
            SwitchRow(
                title = stringResource(R.string.show_percent_instead),
                subtitle = stringResource(R.string.show_percent_desc),
                checked = numberIcon,
                onChange = onNumberIconChange
            )
            Divider()
            SwitchRow(
                title = stringResource(R.string.charging_emoji),
                subtitle = stringResource(R.string.charging_emoji_desc, set.charging),
                checked = chargingEmoji,
                onChange = onChargingEmojiChange
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.floating_bubble))
        SectionCard {
            if (!mode.showsBubble) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.bubble_not_active),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                SwitchRow(
                    title = stringResource(R.string.bubble_show_percent),
                    subtitle = null,
                    checked = bubblePercent,
                    onChange = onBubblePercentChange
                )
                Divider()
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.bubble_size, bubbleSize), fontWeight = FontWeight.Medium)
                    Slider(
                        value = bubbleSize.toFloat(),
                        onValueChange = { onBubbleSizeChange(it.toInt()) },
                        valueRange = 12f..48f
                    )
                }
                Divider()
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.bubble_bg, bubbleOpacity), fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.bubble_bg_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = bubbleOpacity.toFloat(),
                        onValueChange = { onBubbleOpacityChange(it.toInt()) },
                        valueRange = 25f..100f
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.home_widget))
        SectionCard {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.home_widget_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MoreContent() {
    val context = LocalContext.current
    Column {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.more_settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        SectionCard {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.disappearing_icon), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.disappearing_icon_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    }
                }) { Text(stringResource(R.string.open_battery_settings)) }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.hide_system_battery_title), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.hide_system_battery_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    // Try common intents for status bar settings
                    val intents = listOf(
                        Intent("com.android.settings.STATUS_BAR_SETTINGS"),
                        Intent(Settings.ACTION_DISPLAY_SETTINGS)
                    )
                    for (intent in intents) {
                        try {
                            context.startActivity(intent)
                            break
                        } catch (_: Exception) {
                            continue
                        }
                    }
                }) { Text(stringResource(R.string.hide_system_battery_btn)) }

                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val intents = listOf(
                        Intent("com.android.settings.NOTIFICATION_AND_STATUS_BAR_SETTINGS"),
                        Intent("android.settings.NOTIFICATION_SETTINGS"),
                        Intent("com.hihonor.android.notification.CENTER_SETTINGS"),
                        Intent("com.huawei.android.notification.CENTER_SETTINGS")
                    )
                    for (intent in intents) {
                        try {
                            context.startActivity(intent)
                            break
                        } catch (_: Exception) {
                            continue
                        }
                    }
                }) { Text(stringResource(R.string.hide_system_battery_notif_btn)) }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.about_app))
        SectionCard {
            val version = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: "1.0"
            }
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.app_version, version), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.mit_license),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL))
                            )
                        }
                    },
                    contentPadding = PaddingValues(0.dp)
                ) { Text(stringResource(R.string.view_github)) }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

/* ---------- dílčí prvky ---------- */

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ModeRow(mode: Mode, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(mode.labelRes), fontWeight = FontWeight.Medium)
            Text(
                stringResource(mode.hintRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SetRow(set: EmojiSet, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(set.nameRes), fontWeight = FontWeight.Medium)
            if (set.drawn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf(5, 30, 55, 80, 100).forEach { level ->
                        val bmp = remember(level) {
                            EmojiRenderer.battery(level, false, 120, colored = true).asImageBitmap()
                        }
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            } else {
                Text(set.preview(), fontSize = 20.sp)
            }
        }
    }
}

/* ---------- pomocné ---------- */

@Composable
private fun liveBattery(): BatteryInfo {
    val context = LocalContext.current
    var info by remember { mutableStateOf(BatteryInfo.current(context)) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                info = BatteryInfo.from(intent)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return info
}

@Composable
private fun OnResume(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) action()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun LanguageSwitcher() {
    val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CZ",
            color = if (currentLocale == "cs") Mint else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (currentLocale == "cs") FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .clickable {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("cs"))
                }
                .padding(8.dp)
        )
        Text("|", color = MaterialTheme.colorScheme.surfaceVariant)
        Text(
            text = "EN",
            color = if (currentLocale == "en") Mint else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (currentLocale == "en") FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .clickable {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                }
                .padding(8.dp)
        )
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
