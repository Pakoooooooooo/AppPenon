package com.example.apppenon.model

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Gère le parsing et la décoding des données brutes Penon BLE.
 * Responsabilités:
 * - Extraire les données de batterie et débit du payload BLE
 * - Décoder et tester différents formats de données (LE/BE, avec/sans offset)
 * - Valider la cohérence des trames reçues
 */
class PenonDataParser {
    
    private val TAG = "eTT-SAIL-BLE"
    private var lastFrameCnt = -1L

    /**
     * Extrait les données principales du Penon: batterie, débit et numéro de trame.
     * @return Triple(batterie_voltage, flowState, frameCount) ou null si erreur
     */
    fun extractPenonData(data: ByteArray): Triple<Float, Int, Long>? {
        return try {
            if (data.size < 17) return null

            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(0)

            val frameCnt = buffer.int.toLong() and 0xFFFFFFFFL
            buffer.get() // frameType
            val vbat = buffer.short.toInt()
            val meanMagZ = buffer.short.toInt()

            val battery = vbat / 1000.0f
            val flowState = meanMagZ

            Triple(battery, flowState, frameCnt)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Décode les données du Penon et retourne un objet PenonDecodedData.
     */
    fun decodePenonData(data: ByteArray): PenonDecodedData? {
        return try {
            if (data.size < 17) return null

            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(0)

            val frameCount = buffer.int.toLong() and 0xFFFFFFFFL
            val frameType = buffer.get().toInt() and 0xFF
            val vbat = buffer.short.toInt()
            val meanMagZ = buffer.short.toInt()
            val sdMagZ = buffer.short.toInt()
            val meanAcc = buffer.short.toInt()
            val sdAcc = buffer.short.toInt()
            val maxAcc = buffer.short.toInt()

            PenonDecodedData(
                frameCount = frameCount,
                frameType = frameType,
                vbat = vbat / 1000.0,
                meanMagZ = meanMagZ,
                sdMagZ = sdMagZ,
                meanAcc = meanAcc,
                sdAcc = sdAcc,
                maxAcc = maxAcc
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur décodage", e)
            null
        }
    }

    /**
     * Parse une trame ETT-SAIL et teste différentes configurations de décodage.
     * Retourne un rapport formaté avec les résultats des tests.
     */
    fun parseETTSailData(data: ByteArray, penonNumber: Int): String {
        return try {
            val fullHex = data.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "Penon $penonNumber - Données complètes: $fullHex")

            var manufacturerData = data

            var offset = 0

            while (offset < data.size - 2) {
                val length = data[offset].toInt() and 0xFF
                if (length == 0) break

                val type = data[offset + 1].toInt() and 0xFF

                if (type == 0xFF) {
                    manufacturerData = data.copyOfRange(offset + 2, minOf(offset + 1 + length, data.size))
                    break
                }

                offset += length + 1
            }

            val dataHex = manufacturerData.joinToString(" ") { "%02X".format(it) }

            if (manufacturerData.size < 17) {
                return "⚠️ Données insuffisantes (${manufacturerData.size} octets)\n" +
                        "Minimum requis: 17 octets\n\n" +
                        "HEX: $dataHex"
            }

            val results = mutableListOf<String>()

            results.add(testDecode(manufacturerData, 0, ByteOrder.LITTLE_ENDIAN, "Sans offset, LE", penonNumber))

            if (manufacturerData.size >= 19) {
                results.add(testDecode(manufacturerData, 2, ByteOrder.LITTLE_ENDIAN, "Offset +2, LE", penonNumber))
            }

            results.add(testDecode(manufacturerData, 0, ByteOrder.BIG_ENDIAN, "Sans offset, BE", penonNumber))

            if (manufacturerData.size >= 19) {
                results.add(testDecode(manufacturerData, 2, ByteOrder.BIG_ENDIAN, "Offset +2, BE", penonNumber))
            }

            buildString {
                appendLine("═══════════════════════════════")
                appendLine("PENON $penonNumber - TESTS DE DÉCODAGE")
                appendLine("═══════════════════════════════")
                results.forEach { appendLine(it) }
                appendLine("═══════════════════════════════")
                appendLine("\nDonnées brutes:")
                appendLine(dataHex)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur de décodage Penon $penonNumber", e)
            "❌ Erreur: ${e.message}\n" +
                    "Taille: ${data.size} octets\n" +
                    "HEX: ${data.joinToString(" ") { "%02X".format(it) }}"
        }
    }

    /**
     * Teste le décodage d'une trame avec une configuration spécifique.
     * Valide la cohérence des données et détecte les trames perdues.
     */
    private fun testDecode(data: ByteArray, offset: Int, order: ByteOrder, label: String, penonNumber: Int): String {
        return try {
            if (data.size < offset + 17) {
                return "[$label] Taille insuffisante"
            }

            val buffer = ByteBuffer.wrap(data).order(order)
            buffer.position(offset)

            val frameCnt = buffer.int.toLong() and 0xFFFFFFFFL
            val frameType = buffer.get().toInt() and 0xFF
            val vbat = buffer.short.toInt()
            val meanMagZ = buffer.short.toInt()
            val sdMagZ = buffer.short.toInt()
            val meanAcc = buffer.short.toInt()
            val maxAcc = buffer.short.toInt()

            val vbatV = vbat / 1000.0
            val isCoherent = frameCnt in 0..100000000 &&
                    vbatV in 2.0..4.5 &&
                    frameType in 0..255

            val lostFrames = if (lastFrameCnt >= 0 && frameCnt > lastFrameCnt) {
                val lost = frameCnt - lastFrameCnt - 1
                if (lost > 0) " ⚠️ $lost trame(s) perdue(s)" else ""
            } else ""

            if (isCoherent && lastFrameCnt < frameCnt) {
                lastFrameCnt = frameCnt
            }

            val coherentMark = if (isCoherent) "✅" else "❌"

            buildString {
                appendLine("\n[$label] $coherentMark")
                appendLine("  Frame: $frameCnt$lostFrames")
                appendLine("  Type: $frameType")
                appendLine("  Vbat: ${"%.3f".format(vbatV)} V")
                appendLine("  MagZ: mean=${meanMagZ/1000.0}mT, sd=${sdMagZ/1000.0}mT")
                appendLine("  Acc: mean=${meanAcc/1000.0}g, max=${maxAcc/1000.0}g")
            }

        } catch (e: Exception) {
            "[$label] Erreur: ${e.message}"
        }
    }

    /**
     * Réinitialise les compteurs de trame.
     */
    fun resetFrameCounters() {
        lastFrameCnt = -1
    }
}
