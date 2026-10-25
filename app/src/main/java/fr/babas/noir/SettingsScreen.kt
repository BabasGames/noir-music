package fr.babas.noir

import androidx.compose.foundation.background
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
fun SettingsScreen(prefs: Prefs, displayInfo: String, onOpenAccess: () -> Unit, onBack: () -> Unit) {
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
        Spacer(Modifier.height(12.dp))

        Toggle("Afficher la pochette", null, prefs.showArt) { prefs.showArt = it }
        Toggle("Pochette en noir et blanc", null, prefs.grayArt, enabled = prefs.showArt) { prefs.grayArt = it }
        Toggle(
            "Écran toujours allumé",
            "Pendant la lecture uniquement ; en pause, l'écran s'éteint normalement",
            prefs.keepOn,
        ) { prefs.keepOn = it }
        Toggle("Rafraîchissement réduit", displayInfo, prefs.lowRefresh) { prefs.lowRefresh = it }
        Toggle(
            "Anti-marquage",
            "Décale l'affichage de quelques pixels chaque minute",
            prefs.antiBurn,
        ) { prefs.antiBurn = it }
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

        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onOpenAccess) { Text("Accès aux notifications", color = Color.White) }
    }
}

@Composable
private fun Toggle(
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
