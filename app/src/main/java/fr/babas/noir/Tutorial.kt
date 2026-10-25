package fr.babas.noir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val PAGES = 4

/** Premier lancement (ou « Revoir le tuto ») : accès, gestes, paroles, réglages. */
@Composable
fun TutorialScreen(prefs: Prefs, hasAccess: Boolean, onGrant: () -> Unit, onDone: () -> Unit) {
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
    ) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                Column(Modifier.widthIn(max = 520.dp).align(Alignment.CenterHorizontally)) {
                    when (page) {
                        0 -> Welcome(hasAccess, onGrant)
                        1 -> Gestures()
                        2 -> LyricsPage(prefs)
                        else -> ScreenPage()
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDone) { Text("Passer", color = Gray) }
            Spacer(Modifier.weight(1f))
            repeat(PAGES) { i ->
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) Color.White else Dim),
                )
            }
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == PAGES - 1
            OutlinedButton(onClick = {
                if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }) {
                Text(if (last) "C'est parti" else "Suivant", color = Color.White)
            }
        }
    }
}

@Composable
private fun ColumnScope.Title(text: String) {
    Text(text, fontSize = 40.sp, fontWeight = FontWeight.Thin, color = Color.White)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, fontSize = 17.sp, lineHeight = 25.sp, color = Gray)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Welcome(hasAccess: Boolean, onGrant: () -> Unit) {
    Column {
        Title("Noir")
        Body("Un écran de contrôle pour la musique, en noir et blanc, fait pour rester allumé sur le bureau ou en voiture.")
        Body(
            "Il pilote la musique des autres applis (Deezer, Spotify, KDE Connect…). Pour ça, Android demande " +
                "d'autoriser l'accès aux notifications : Noir ne les lit pas, c'est juste la porte d'entrée vers les lecteurs.",
        )
        Spacer(Modifier.height(8.dp))
        if (hasAccess) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Accès accordé", color = Color.White, fontSize = 17.sp)
            }
        } else {
            OutlinedButton(onClick = onGrant) { Text("Autoriser l'accès", color = Color.White) }
        }
    }
}

@Composable
private fun Gestures() {
    Column {
        Title("Gestes")
        listOf(
            "← →" to "Morceau suivant / précédent",
            "↑ ↓" to "Ouvrir / fermer les paroles",
            "Toucher le titre" to "Lecture / pause",
            "Double-tap" to "♥︎ J'aime (si l'appli le permet)",
            "Appui long" to "Ouvrir l'appli qui joue",
            "Toucher l'heure" to "Minuteur de sommeil",
            "Toucher le nom de l'appli" to "Changer de source",
        ).forEach { (gesture, action) ->
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(gesture, color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Text(action, color = Gray, fontSize = 17.sp, modifier = Modifier.weight(1.2f))
            }
        }
    }
}

@Composable
private fun LyricsPage(prefs: Prefs) {
    Column {
        Title("Paroles")
        Body(
            "Glisse vers le haut pour les afficher, synchronisées avec la musique. Elles viennent d'internet " +
                "(LRCLIB, puis NetEase en secours) et restent sur le tel une fois téléchargées.",
        )
        Toggle(
            "Télécharger sur données mobiles",
            "Sinon, hors Wi-Fi, seules les paroles déjà téléchargées s'affichent",
            prefs.lyricsOnMobile,
        ) { prefs.lyricsOnMobile = it }
    }
}

@Composable
private fun ScreenPage() {
    Column {
        Title("Écran")
        Body("En lecture, l'écran reste allumé et se rafraîchit moins souvent pour économiser la batterie.")
        Body(
            "Pour protéger l'AMOLED, l'affichage se décale de quelques pixels de temps en temps. " +
                "Le mode nuit atténue le texte et la pochette, et la veille éteint tout quand rien ne joue.",
        )
        Body("Tout se règle avec la roue dentée ⚙, en haut à droite.")
    }
}
