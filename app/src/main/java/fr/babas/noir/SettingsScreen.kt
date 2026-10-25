package fr.babas.noir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    prefs: Prefs,
    displayInfo: String,
    candidateApps: List<Pair<String, String>>,
    onOpenAccess: () -> Unit,
    onTutorial: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour", tint = Color.White)
            }
            Text("Paramètres", fontSize = 22.sp, color = Color.White)
        }

        Section("Disposition")
        Toggle("Afficher la pochette", null, prefs.showArt) { prefs.showArt = it }
        Toggle(
            "Inverser pochette et contrôles",
            "Portrait : contrôles en haut · paysage : pochette à droite",
            prefs.swapArt,
            enabled = prefs.showArt,
        ) { prefs.swapArt = it }
        Toggle("Pochette en noir et blanc", null, prefs.grayArt, enabled = prefs.showArt) { prefs.grayArt = it }
        Choice("Orientation", listOf("Auto", "Portrait", "Paysage"), prefs.orientation) { prefs.orientation = it }

        Section("Affichage")
        Choice("Horloge", listOf("Masquée", "Moyenne", "Grande"), prefs.clockSize) { prefs.clockSize = it }
        Choice(
            "Taille du texte",
            TEXT_SCALES.map { it.second },
            TEXT_SCALES.indexOfFirst { it.first == prefs.textScale }.coerceAtLeast(0),
        ) { prefs.textScale = TEXT_SCALES[it].first }
        Toggle("Date et batterie", "Sous l'heure · toucher l'heure lance le minuteur de sommeil", prefs.showDate) {
            prefs.showDate = it
        }
        Toggle("Nom de l'appli source", "Le toucher permet de changer d'appli ou de l'ouvrir", prefs.showSource) {
            prefs.showSource = it
        }
        Toggle("Barre de progression", null, prefs.showProgress) { prefs.showProgress = it }
        Toggle("Volume", null, prefs.showVolume) { prefs.showVolume = it }

        Section("Nuit")
        Choice("Mode nuit", listOf("Non", "Toujours", "Horaires", "Pénombre"), prefs.nightMode) { prefs.nightMode = it }
        Text(
            when (prefs.nightMode) {
                Prefs.NIGHT_SCHEDULE -> "Texte gris et pochette assombrie pendant les heures choisies"
                Prefs.NIGHT_DARK -> "S'active dans le noir, d'après le capteur de luminosité (quelques secondes de délai)"
                else -> "Texte gris au lieu de blanc, pochette assombrie : pour la nuit, et moins d'usure AMOLED"
            },
            fontSize = 13.sp,
            color = Gray,
        )
        if (prefs.nightMode == Prefs.NIGHT_SCHEDULE) {
            HourStepper("Début", prefs.nightStart) { prefs.nightStart = it }
            HourStepper("Fin", prefs.nightEnd) { prefs.nightEnd = it }
        }
        Toggle(
            "Luminosité de nuit",
            "Baisse l'écran quand le mode nuit est actif",
            prefs.nightBrightnessOn,
            enabled = prefs.nightMode != Prefs.NIGHT_OFF,
        ) { prefs.nightBrightnessOn = it }
        if (prefs.nightBrightnessOn && prefs.nightMode != Prefs.NIGHT_OFF) {
            Slider(
                value = prefs.nightBrightness,
                onValueChange = { prefs.nightBrightness = it },
                valueRange = 0.01f..0.5f,
                colors = noirSliderColors(),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        Section("Écran")
        Choice("Toujours allumé", listOf("Non", "En lecture", "Toujours"), prefs.keepOnMode) { prefs.keepOnMode = it }
        Toggle("Rafraîchissement réduit", displayInfo, prefs.lowRefresh) { prefs.lowRefresh = it }
        Toggle(
            "Décalage périodique",
            "Anti-marquage : déplace tout l'affichage de quelques pixels, au hasard",
            prefs.antiBurn,
        ) { prefs.antiBurn = it }
        if (prefs.antiBurn) {
            Choice("Fréquence", Prefs.BURN_INTERVALS.map { it.second }, prefs.burnInterval) { prefs.burnInterval = it }
            Choice(
                "Amplitude",
                Prefs.BURN_AMPLITUDES.map { "${it.second} (±${it.first})" },
                prefs.burnAmplitude,
            ) { prefs.burnAmplitude = it }
        }
        Choice("Veille quand rien ne joue", listOf("Non", "Horloge seule", "Écran noir"), prefs.standbyMode) {
            prefs.standbyMode = it
        }
        if (prefs.standbyMode != Prefs.STANDBY_OFF) {
            Choice("Après", Prefs.STANDBY_DELAYS.map { it.second }, prefs.standbyDelay) { prefs.standbyDelay = it }
        }
        Toggle("Luminosité personnalisée", "Ignore la luminosité système dans l'appli", prefs.customBrightness) {
            prefs.customBrightness = it
        }
        if (prefs.customBrightness) {
            Slider(
                value = prefs.brightness,
                onValueChange = { prefs.brightness = it },
                valueRange = 0.01f..1f,
                colors = noirSliderColors(),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        Section("Musique")
        Toggle("Gestes", "Double-tap : ♥︎ · appui long : ouvrir l'appli qui joue", prefs.gestures) { prefs.gestures = it }
        Text("Appli préférée", fontSize = 17.sp, color = Color.White)
        Text("Suivie en priorité dès qu'elle a une session, même en pause", fontSize = 13.sp, color = Gray)
        Spacer(Modifier.height(4.dp))
        val apps = listOf("" to "Automatique (ce qui joue)") + candidateApps
        apps.forEach { (pkg, label) ->
            Radio(label, selected = prefs.preferredApp == pkg) { prefs.preferredApp = pkg }
        }
        // Appli choisie auparavant mais plus détectée : la garder visible pour pouvoir la retirer
        if (prefs.preferredApp.isNotEmpty() && apps.none { it.first == prefs.preferredApp }) {
            Radio(prefs.preferredApp, selected = true) {}
        }

        Section("Paroles")
        Toggle("Paroles", "Glisser vers le haut ou bouton en bas à droite · sources : LRCLIB, NetEase", prefs.lyrics) {
            prefs.lyrics = it
        }
        Toggle(
            "Sur données mobiles",
            "Sinon, seules les paroles déjà téléchargées s'affichent hors Wi-Fi",
            prefs.lyricsOnMobile,
            enabled = prefs.lyrics,
        ) { prefs.lyricsOnMobile = it }
        Choice(
            "Taille des paroles",
            TEXT_SCALES.map { it.second },
            TEXT_SCALES.indexOfFirst { it.first == prefs.lyricsScale }.coerceAtLeast(0),
        ) { prefs.lyricsScale = TEXT_SCALES[it].first }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onOpenAccess) { Text("Accès aux notifications", color = Color.White) }
            OutlinedButton(onClick = onTutorial) { Text("Revoir le tuto", color = Color.White) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private val TEXT_SCALES = listOf(0.85f to "Petit", 1f to "Normal", 1.25f to "Grand", 1.5f to "Très grand")

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(title.uppercase(), fontSize = 12.sp, letterSpacing = 1.5.sp, color = Gray)
    Spacer(Modifier.height(4.dp))
}

/** Rangée de choix exclusifs, façon boutons segmentés. */
@Composable
private fun Choice(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(title, fontSize = 17.sp, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Dim, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50)),
        ) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    Modifier
                        .weight(1f)
                        .background(if (on) Color.White else Color.Black)
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, fontSize = 14.sp, color = if (on) Color.Black else Gray, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Radio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .border(2.dp, if (selected) Color.White else Dim, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Color.Transparent),
        )
        Text(label, fontSize = 16.sp, color = if (selected) Color.White else Gray)
    }
}

/** Heure de 0 à 23 réglée par − / +, qui boucle (23 h + 1 = 0 h). */
@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, color = Color.White, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((hour + 23) % 24) }) { Icon(Icons.Rounded.Remove, "Moins", tint = Gray) }
        Text("$hour h", fontSize = 17.sp, color = Color.White, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onChange((hour + 1) % 24) }) { Icon(Icons.Rounded.Add, "Plus", tint = Gray) }
    }
}

@Composable
fun Toggle(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = if (enabled) Color.White else Dim)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Gray,
                uncheckedTrackColor = Color.Black,
                uncheckedBorderColor = Dim,
            ),
        )
    }
}

@Composable
fun AccessScreen(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Noir", fontSize = 40.sp, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text(
            "Pour voir et piloter la musique des autres applis (Deezer, Spotify…), " +
                "Android demande d'activer l'accès aux notifications pour Noir.",
            color = Gray,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onGrant) { Text("Ouvrir le réglage", color = Color.White) }
    }
}
