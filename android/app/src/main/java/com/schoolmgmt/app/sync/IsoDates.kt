package com.schoolmgmt.app.sync

import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Converts between Room's Long epoch-millis (local storage) and the
 * ISO 8601 strings the backend sends/expects (since Express/Prisma
 * serializes DateTime fields as ISO strings over JSON, not numbers).
 *
 * Uses java.time.Instant rather than SimpleDateFormat: Instant parses
 * ISO 8601 ("2026-06-23T10:15:30.123Z") natively and correctly handles
 * UTC by definition, avoiding SimpleDateFormat's well-documented
 * pitfalls (not thread-safe, defaults to the device's local timezone
 * unless explicitly overridden, locale-dependent calendar quirks).
 *
 * minSdk is 24 in this project; java.time was added natively in API 26.
 * For API 24-25 support, add Android's core library desugaring
 * (compileOptions { isCoreLibraryDesugaringEnabled = true } plus the
 * desugar_jdk_libs dependency) — without it, this will crash with
 * NoClassDefFoundError on API 24/25 devices specifically. If you know
 * your target schools only use newer devices, minSdk could also simply
 * be raised to 26 instead of adding desugaring.
 *
 * NOTE: I was not able to actually compile and execute this in the
 * sandbox (only a JRE is installed, no javac/JDK), so this hasn't been
 * empirically tested the way the backend Node code was. Test this for
 * real once the project is open in Android Studio — in particular,
 * confirm a real backend response round-trips correctly through
 * parseToMillis/toIsoString.
 */
object IsoDates {
    fun parseToMillis(iso: String): Long? = try {
        Instant.parse(iso).toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }

    fun toIsoString(millis: Long): String = Instant.ofEpochMilli(millis).toString()
}
