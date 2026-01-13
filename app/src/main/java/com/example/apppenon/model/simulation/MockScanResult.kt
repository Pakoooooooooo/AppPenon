package com.example.apppenon.simulation

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import java.lang.reflect.Constructor

/**
 * Crée un ScanResult simulé pour injecter dans le callback BLE.
 * Utilise la réflexion pour créer des objets normalement construits par le système Android.
 */
class MockScanResult(
    private val macAddress: String,
    private val rssi: Int,
    private val scanRecord: ByteArray
) {
    
    /**
     * Crée un BluetoothDevice mock avec l'adresse MAC spécifiée.
     */
    private fun createMockBluetoothDevice(): BluetoothDevice? {
        return try {
            // Utiliser la réflexion pour créer un BluetoothDevice
            val constructor: Constructor<BluetoothDevice> = 
                BluetoothDevice::class.java.getDeclaredConstructor(String::class.java)
            constructor.isAccessible = true
            constructor.newInstance(macAddress)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Crée un ScanRecord mock avec les données brutes.
     */
    private fun createMockScanRecord(): ScanRecord? {
        return try {
            // Utiliser la méthode parseFromBytes qui est publique
            val parseMethod = ScanRecord::class.java.getDeclaredMethod(
                "parseFromBytes",
                ByteArray::class.java
            )
            parseMethod.invoke(null, scanRecord) as? ScanRecord
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convertit en ScanResult Android.
     */
    fun toScanResult(): ScanResult {
        val device = createMockBluetoothDevice()
        val record = createMockScanRecord()
        
        return if (device != null && record != null) {
            ScanResult(device, record, rssi, System.nanoTime())
        } else {
            // Fallback: créer un ScanResult avec réflexion complète
            createScanResultViaReflection()
        }
    }
    
    /**
     * Crée un ScanResult en utilisant la réflexion complète (fallback).
     */
    private fun createScanResultViaReflection(): ScanResult {
        try {
            val device = createMockBluetoothDevice()
            val record = createMockScanRecord()
            
            val constructor = ScanResult::class.java.getDeclaredConstructor(
                BluetoothDevice::class.java,
                ScanRecord::class.java,
                Int::class.java,
                Long::class.java
            )
            constructor.isAccessible = true
            
            return constructor.newInstance(
                device,
                record,
                rssi,
                System.nanoTime()
            )
        } catch (e: Exception) {
            throw RuntimeException("Impossible de créer ScanResult simulé", e)
        }
    }
}