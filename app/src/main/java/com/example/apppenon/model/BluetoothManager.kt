package com.example.apppenon.model

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.util.UUID

class BluetoothManager {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null

    // UUID standard pour SPP (Serial Port Profile)
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun connectToDevice(device: BluetoothDevice) {
        Thread {
            try {
                // Créer le socket de connexion
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)

                // Annuler la découverte pour améliorer les performances
                bluetoothAdapter?.cancelDiscovery()

                // Connexion au périphérique
                bluetoothSocket?.connect()

                // Obtenir le flux d'entrée
                inputStream = bluetoothSocket?.inputStream

                // Commencer à écouter les données
                listenForData()

            } catch (e: Exception) {
                e.printStackTrace()
                // Gérer l'erreur de connexion
            }
        }.start()
    }

    private fun listenForData() {
        val buffer = ByteArray(1024)
        var bytes: Int

        while (true) {
            try {
                // Lire les données du flux d'entrée
                bytes = inputStream?.read(buffer) ?: -1

                if (bytes > 0) {
                    // Convertir les bytes en String
                    val receivedData = String(buffer, 0, bytes)

                    // Traiter les données reçues
                    onDataReceived(receivedData)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }
    }

    private fun onDataReceived(data: String) {
        // Traiter les données reçues ici
        // Utilisez un Handler ou LiveData pour mettre à jour l'UI
        println("Données reçues : $data")
    }

    fun disconnect() {
        try {
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun getBluetoothDevices(): List<BluetoothDevice> {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
        // Bluetooth non disponible ou désactivé
        return emptyList()
    }

    // Obtenir les appareils déjà appairés
    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices

    return pairedDevices?.toList() ?: emptyList()
}