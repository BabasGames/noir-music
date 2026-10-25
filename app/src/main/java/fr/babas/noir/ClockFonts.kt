package fr.babas.noir

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import fr.babas.noir.i18n.LocalLang
import fr.babas.noir.i18n.Strings
import java.io.File

data class ClockStyle(val family: FontFamily, val weight: FontWeight, val typeface: Typeface)

/**
 * Polices proposées pour l'horloge : génériques, celles du système qu'on reconnaît (les polices Nothing
 * sont lisibles par toutes les applis dans /system/fonts), et un fichier importé par l'utilisateur.
 */
object ClockFonts {
    const val CUSTOM = "custom"
    private const val SYS = "sys:"

    private val SYSTEM_FONTS = listOf(
        "Ndot-57.otf" to "Nothing Dot 57",
        "Ndot-55.otf" to "Nothing Dot 55",
        "NDot57Caps.otf" to "Nothing Dot 57 Caps",
        "NType82-Regular.otf" to "NType 82",
        "LetteraMonoLL-Light.otf" to "Lettera Mono",
    )

    private val cache = mutableMapOf<String, Typeface?>()

    private fun customFile(context: Context) = File(context.filesDir, "clock_font")

    /** Identifiants disponibles sur ce téléphone, dans l'ordre d'affichage. */
    fun ids(context: Context): List<String> =
        listOf("", "serif", "mono") +
            SYSTEM_FONTS.filter { File("/system/fonts", it.first).canRead() }.map { SYS + it.first } +
            (if (customFile(context).exists()) listOf(CUSTOM) else emptyList())

    fun label(id: String, s: Strings, customName: String): String = when {
        id == "" -> s.fontSystem
        id == "serif" -> s.fontSerif
        id == "mono" -> s.fontMono
        id.startsWith(SYS) -> SYSTEM_FONTS.firstOrNull { SYS + it.first == id }?.second ?: id.removePrefix(SYS)
        else -> s.fontFile(customName)
    }

    fun style(context: Context, id: String, version: String = ""): ClockStyle = when {
        id == "" -> system
        id == "serif" -> ClockStyle(FontFamily.Serif, FontWeight.Light, Typeface.create(Typeface.SERIF, 300, false))
        id == "mono" -> ClockStyle(FontFamily.Monospace, FontWeight.Light, Typeface.create(Typeface.MONOSPACE, 300, false))
        else -> {
            val file = if (id == CUSTOM) customFile(context) else File("/system/fonts", id.removePrefix(SYS))
            // Les polices fichier n'ont souvent qu'une graisse : pas de Thin simulé
            load(file, "$id|$version")?.let { ClockStyle(FontFamily(it), FontWeight.Normal, it) } ?: system
        }
    }

    private val system = ClockStyle(FontFamily.Default, FontWeight.Thin, Typeface.create(Typeface.DEFAULT, 100, false))

    private fun load(file: File, key: String): Typeface? = cache.getOrPut(key) {
        try {
            Typeface.createFromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    /** Copie la police choisie dans l'appli ; false si ce n'est pas une police lisible. */
    fun import(context: Context, uri: Uri): Boolean {
        val tmp = File(context.cacheDir, "font_import")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use(input::copyTo) }
                ?: return false
            Typeface.createFromFile(tmp) // lève une exception si le fichier n'est pas une police
            tmp.copyTo(customFile(context), overwrite = true)
            true
        } catch (e: Exception) {
            false
        } finally {
            tmp.delete()
        }
    }
}

@Composable
fun rememberClockStyle(prefs: Prefs): ClockStyle {
    val context = LocalContext.current
    return remember(prefs.clockFont, prefs.clockFontName) {
        ClockFonts.style(context, prefs.clockFont, prefs.clockFontName)
    }
}

@Composable
fun clockPattern(prefs: Prefs): String = when (prefs.clockFormat) {
    Prefs.FORMAT_24 -> "HH:mm"
    Prefs.FORMAT_12 -> "h:mm"
    else -> if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm"
}

/** Ordre jour/mois et abréviations propres à la langue choisie (« dim. 25 oct. », « Sun, Oct 25 »…). */
@Composable
fun datePattern(prefs: Prefs): String {
    val locale = LocalLang.current.locale
    val skeleton = when (prefs.dateFormat) {
        Prefs.DATE_LONG -> "EEEEdMMMM"
        Prefs.DATE_NUMERIC -> "EEEddMM"
        else -> "EEEdMMM"
    }
    return remember(locale, skeleton) { DateFormat.getBestDateTimePattern(locale, skeleton) }
}
