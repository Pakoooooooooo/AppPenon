#!/usr/bin/env python3
"""
Script de décodage des données Penon depuis CSV
Lit un fichier CSV avec des données hexadécimales et génère un CSV décodé
"""

import csv
import struct
import sys
from pathlib import Path
from typing import Optional, Tuple


class PenonDecoder:
    """Décodeur de trames Penon BLE"""

    def __init__(self):
        self.last_frame_cnt = {}

    def hex_to_bytes(self, hex_string: str) -> bytes:
        """Convertit une chaîne hexadécimale en bytes"""
        # Nettoyer la chaîne (enlever espaces, préfixes 0x, etc.)
        hex_clean = hex_string.replace(" ", "").replace("0x", "").replace(",", "")
        return bytes.fromhex(hex_clean)

    def extract_penon_data(self, ble_packet: bytes) -> Optional[bytes]:
        """
        Extrait les données Penon d'un paquet BLE complet.

        Structure BLE:
        - Header (3 bytes): 02 01 06
        - Device Name AD (variable): longueur + type + nom
        - Manufacturer Data AD: longueur + FF + DONNÉES_PENON (19 bytes)

        IMPORTANT: Les données Penon commencent DIRECTEMENT après 0xFF,
        le "Company ID" fait partie du frame count (uint32) !

        Retourne les données Penon (19 bytes) ou None si non trouvé.
        """
        offset = 0
        while offset < len(ble_packet):
            # Lire la longueur de l'AD
            if offset >= len(ble_packet):
                break

            ad_length = ble_packet[offset]
            if ad_length == 0:
                break

            # Vérifier si c'est Manufacturer Data (type 0xFF)
            if offset + 1 < len(ble_packet) and ble_packet[offset + 1] == 0xFF:
                # Manufacturer Data trouvé
                # Les données Penon commencent immédiatement après 0xFF
                penon_start = offset + 2
                penon_end = offset + 1 + ad_length

                if penon_end <= len(ble_packet):
                    return ble_packet[penon_start:penon_end]

            # Passer au prochain AD
            offset += 1 + ad_length

        return None

    def decode_frame(self, data: bytes, penon_id: str = "unknown") -> Optional[dict]:
        """
        Décode une trame Penon

        Format des données (17 bytes minimum) :
        - Bytes 0-3  : Frame Count (uint32, little-endian)
        - Byte  4    : Frame Type (uint8)
        - Bytes 5-6  : Vbat (int16, little-endian) en millivolts
        - Bytes 7-8  : Mean MagZ (int16, little-endian)
        - Bytes 9-10 : SD MagZ (int16, little-endian)
        - Bytes 11-12: Mean Acc (int16, little-endian)
        - Bytes 13-14: SD Acc (int16, little-endian)
        - Bytes 15-16: Max Acc (int16, little-endian)
        """
        # Si c'est un paquet BLE complet (commence par 02 01 06), extraire les données Penon
        if len(data) > 30 and data[0:3] == b'\x02\x01\x06':
            penon_data = self.extract_penon_data(data)
            if penon_data is None:
                return None
            data = penon_data

        if len(data) < 17:
            return None

        try:
            # Décoder selon le format little-endian
            frame_cnt = struct.unpack('<I', data[0:4])[0]
            frame_type = data[4]
            vbat_mv = struct.unpack('<h', data[5:7])[0]
            mean_mag_z = struct.unpack('<h', data[7:9])[0]
            sd_mag_z = struct.unpack('<h', data[9:11])[0]
            mean_acc = struct.unpack('<h', data[11:13])[0]
            sd_acc = struct.unpack('<h', data[13:15])[0]
            max_acc = struct.unpack('<h', data[15:17])[0]

            # Calculer les valeurs physiques
            vbat = vbat_mv / 1000.0  # Volts
            mean_mag_z_mt = mean_mag_z / 1000.0  # milliTesla
            sd_mag_z_mt = sd_mag_z / 1000.0  # milliTesla
            mean_acc_g = mean_acc / 1000.0  # g
            sd_acc_g = sd_acc / 1000.0  # g
            max_acc_g = max_acc / 1000.0  # g

            # Calculer Flow State (valeur absolue du champ magnétique moyen)
            flow_state = abs(mean_mag_z)

            # Détecter les trames perdues
            lost_frames = 0
            if penon_id in self.last_frame_cnt:
                expected = self.last_frame_cnt[penon_id] + 1
                if frame_cnt > expected:
                    lost_frames = frame_cnt - expected

            self.last_frame_cnt[penon_id] = frame_cnt

            # Déterminer l'état attaché/détaché (threshold par défaut: 3500)
            is_attached = flow_state >= 3500

            return {
                'frame_count': frame_cnt,
                'frame_type': frame_type,
                'vbat': round(vbat, 3),
                'mean_mag_z': mean_mag_z,
                'sd_mag_z': sd_mag_z,
                'mean_acc': mean_acc,
                'sd_acc': sd_acc,
                'max_acc': max_acc,
                'mean_mag_z_mt': round(mean_mag_z_mt, 3),
                'sd_mag_z_mt': round(sd_mag_z_mt, 3),
                'mean_acc_g': round(mean_acc_g, 3),
                'sd_acc_g': round(sd_acc_g, 3),
                'max_acc_g': round(max_acc_g, 3),
                'flow_state': flow_state,
                'is_attached': is_attached,
                'lost_frames': lost_frames
            }

        except Exception as e:
            print(f"Erreur de décodage: {e}", file=sys.stderr)
            return None


def decode_csv(input_file: str, output_file: str, hex_column: str = 'hex_data',
               penon_id_column: str = None):
    """
    Décode un fichier CSV contenant des données Penon hexadécimales

    Args:
        input_file: Chemin du fichier CSV d'entrée
        output_file: Chemin du fichier CSV de sortie
        hex_column: Nom de la colonne contenant les données hexadécimales
        penon_id_column: Nom de la colonne contenant l'ID du Penon (optionnel)
    """
    decoder = PenonDecoder()

    # Lire le fichier d'entrée
    with open(input_file, 'r', encoding='utf-8') as f_in:
        reader = csv.DictReader(f_in)

        if hex_column not in reader.fieldnames:
            print(f"Erreur: Colonne '{hex_column}' introuvable dans le CSV", file=sys.stderr)
            print(f"Colonnes disponibles: {', '.join(reader.fieldnames)}", file=sys.stderr)
            return False

        # Préparer le fichier de sortie
        output_fields = list(reader.fieldnames)
        # Ajouter les nouvelles colonnes décodées
        new_fields = [
            'frame_count', 'frame_type', 'vbat',
            'mean_mag_z', 'sd_mag_z', 'mean_acc', 'sd_acc', 'max_acc',
            'mean_mag_z_mt', 'sd_mag_z_mt', 'mean_acc_g', 'sd_acc_g', 'max_acc_g',
            'flow_state', 'is_attached', 'lost_frames'
        ]
        output_fields.extend(new_fields)

        rows_processed = 0
        rows_decoded = 0

        with open(output_file, 'w', newline='', encoding='utf-8') as f_out:
            writer = csv.DictWriter(f_out, fieldnames=output_fields)
            writer.writeheader()

            for row in reader:
                rows_processed += 1

                # Obtenir les données hex
                hex_data = row.get(hex_column, '').strip()
                if not hex_data:
                    continue

                # Obtenir l'ID du Penon si spécifié
                penon_id = row.get(penon_id_column, 'unknown') if penon_id_column else 'unknown'

                try:
                    # Convertir et décoder
                    data_bytes = decoder.hex_to_bytes(hex_data)
                    decoded = decoder.decode_frame(data_bytes, penon_id)

                    if decoded:
                        # Ajouter les données décodées
                        row.update(decoded)
                        writer.writerow(row)
                        rows_decoded += 1

                except Exception as e:
                    print(f"Erreur ligne {rows_processed}: {e}", file=sys.stderr)
                    continue

        print(f"✅ Traitement terminé:")
        print(f"   - Lignes traitées: {rows_processed}")
        print(f"   - Lignes décodées: {rows_decoded}")
        print(f"   - Fichier de sortie: {output_file}")

        return True


def main():
    """Point d'entrée principal"""
    if len(sys.argv) < 3:
        print("Usage: python decode_penon_csv.py <input.csv> <output.csv> [hex_column] [penon_id_column]")
        print()
        print("Arguments:")
        print("  input.csv         : Fichier CSV d'entrée avec données hexadécimales")
        print("  output.csv        : Fichier CSV de sortie avec données décodées")
        print("  hex_column        : Nom de la colonne contenant les données hex (défaut: 'hex_data')")
        print("  penon_id_column   : Nom de la colonne avec l'ID du Penon (optionnel)")
        print()
        print("Exemple:")
        print("  python decode_penon_csv.py raw_data.csv decoded_data.csv hex_data penon_id")
        print()
        print("Format du CSV d'entrée:")
        print("  timestamp,penon_id,hex_data")
        print("  1234567890,Penon1,01000000 05 E803 1234 5678 9ABC DEF0 1122")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2]
    hex_column = sys.argv[3] if len(sys.argv) > 3 else 'hex_data'
    penon_id_column = sys.argv[4] if len(sys.argv) > 4 else None

    # Vérifier que le fichier d'entrée existe
    if not Path(input_file).exists():
        print(f"Erreur: Le fichier '{input_file}' n'existe pas", file=sys.stderr)
        sys.exit(1)

    # Décoder le CSV
    success = decode_csv(input_file, output_file, hex_column, penon_id_column)

    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
