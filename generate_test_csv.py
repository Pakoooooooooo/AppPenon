"""
Génère un CSV de test multi-pénons pour simuler une voile complète.
Format identique aux fichiers record_..._decoded_final.csv.

Scénario (6 pénons, 3 bâbord + 3 tribord) :
  0s - 30s  : Tous ATTACHÉS
 30s - 60s  : P1 + P2 DÉTACHÉS (simule bâbord qui se détache)
 60s - 90s  : Tous ATTACHÉS à nouveau
 90s - 120s : P5 + P6 DÉTACHÉS (simule tribord qui se détache)
120s - 180s : Tous ATTACHÉS

Usage : python generate_test_csv.py
Sortie : test_multi_penon.csv (à charger dans l'app via Paramètres > Mode Simulation)
"""

import struct
from datetime import datetime, timedelta

# ── Constantes du format BLE ──────────────────────────────────────────────────

# Préfixe commun à toutes les trames (flags + nom complet "BladeSENSE_eTT_SAIL")
BLE_PREFIX = bytes([
    0x02, 0x01, 0x06,
    0x14, 0x09, 0x42, 0x6C, 0x61, 0x64, 0x65, 0x53, 0x45, 0x4E, 0x53,
    0x45, 0x5F, 0x65, 0x54, 0x54, 0x5F, 0x53, 0x41, 0x49, 0x4C,
    0x15, 0xFF,
])

# Les 2 premiers octets du manufacturer data = company_id,
# mais ils sont lus par decodePenonData comme les 2 premiers octets du frame_count (uint32 LE).
# Ex : company_id=0x1898 → frame_count_lo = [0x98, 0x18, fc_hi, fc_hi2]
COMPANY_ID = bytes([0x98, 0x18])  # company id du vrai appareil

FRAME_TYPE      = 0x0A
VBAT_RAW        = 295       # int16, unité = centivolts → 2.95 V
MAG_Z_ATTACHED  = 4000      # abs(mean_mag_z) > THRESHOLD → ATTACHÉ
MAG_Z_DETACHED  = 800       # abs(mean_mag_z) < THRESHOLD → DÉTACHÉ
SD_MAG_Z        = 500
MEAN_ACC        = 0
SD_ACC          = 0
MAX_ACC         = 0
ATTACHED_THRESHOLD = 3500


def build_frame(fc_seq: int, mag_z: int) -> str:
    """
    Construit une trame BLE de 46 octets identique au vrai appareil.

    Structure réelle (vérifiée sur les fichiers decoded_final) :
      BLE_PREFIX (26) + COMPANY_ID (2) + fc_high (2) + payload (14) + padding (2) = 46

    decodePenonData lit uint32_LE([company_id_lo, company_id_hi, fc_high_lo, fc_high_hi])
    comme frameCount, puis les champs suivants à partir de l'octet 4.
    """
    fc_high = struct.pack("<H", fc_seq)          # 2 octets : la partie qui s'incrémente
    payload = struct.pack(
        "<BBhhhhhh",
        FRAME_TYPE,    # uint8  frame type  (octet 4 du manufacturer data)
        0x00,          # uint8  padding
        VBAT_RAW,      # int16  vbat
        mag_z,         # int16  mean_mag_z
        SD_MAG_Z,      # int16  sd_mag_z
        MEAN_ACC,      # int16  mean_acc
        SD_ACC,        # int16  sd_acc
        MAX_ACC,       # int16  max_acc
    )
    raw = BLE_PREFIX + COMPANY_ID + fc_high + payload + bytes([0x00, 0x00])
    return " ".join(f"{b:02X}" for b in raw)


def is_detached(penon_idx: int, elapsed_s: float) -> bool:
    """Retourne True si le pénon doit être détaché à cet instant."""
    if penon_idx in (0, 1) and 30 <= elapsed_s < 60:
        return True
    if penon_idx in (4, 5) and 90 <= elapsed_s < 120:
        return True
    return False


def main():
    penons = [
        {"mac": "AA:BB:CC:DD:EE:01", "name": "Pénon B1"},
        {"mac": "AA:BB:CC:DD:EE:02", "name": "Pénon B2"},
        {"mac": "AA:BB:CC:DD:EE:03", "name": "Pénon B3"},
        {"mac": "AA:BB:CC:DD:EE:04", "name": "Pénon T1"},
        {"mac": "AA:BB:CC:DD:EE:05", "name": "Pénon T2"},
        {"mac": "AA:BB:CC:DD:EE:06", "name": "Pénon T3"},
    ]

    INTERVAL_S  = 1.0
    DURATION_S  = 180
    OUTPUT_FILE = "test_multi_penon.csv"

    start_time = datetime(2025, 1, 1, 12, 0, 0)

    # fc_seq : partie haute du frame_count (uint16, s'incrémente à chaque trame).
    # decodePenonData lit uint32_LE([0x98, 0x18, fc_seq_lo, fc_seq_hi])
    # → frame_count affiché = (fc_seq << 16) | 0x1898
    # Ex : fc_seq=0 → frame_count=6296, fc_seq=1 → frame_count=71832
    fc_seqs = [i * 10 for i in range(len(penons))]  # décalage initial entre pénons
    prev_fc_seqs = [None] * len(penons)

    rows = []
    t = 0.0
    frame_global = 1

    while t < DURATION_S:
        for idx, penon in enumerate(penons):
            ts = start_time + timedelta(seconds=t + idx * (INTERVAL_S / len(penons)))
            ts_str = ts.strftime("%Y-%m-%d %H:%M:%S.") + f"{ts.microsecond // 1000:03d}"

            detached = is_detached(idx, t)
            mag_z    = MAG_Z_DETACHED if detached else MAG_Z_ATTACHED
            fc_seq   = fc_seqs[idx]

            hex_data  = build_frame(fc_seq, mag_z)
            data_size = len(hex_data.split())

            # frame_count tel que lu par decodePenonData
            frame_count_decoded = (fc_seq << 16) | 0x1898

            # ── Colonnes décodées ──────────────────────────────────────────
            vbat_v        = VBAT_RAW / 100.0
            flow_state    = abs(mag_z)
            is_attached   = flow_state >= ATTACHED_THRESHOLD
            mean_mag_z_mt = mag_z    / 1000.0
            sd_mag_z_mt   = SD_MAG_Z / 1000.0
            mean_acc_g    = MEAN_ACC  / 1000.0
            sd_acc_g      = SD_ACC    / 1000.0
            max_acc_g     = MAX_ACC   / 1000.0

            prev_seq = prev_fc_seqs[idx]
            lost_frames = 0 if prev_seq is None else max(0, fc_seq - prev_seq - 1)
            prev_fc_seqs[idx] = fc_seq

            line = (
                f"{ts_str},{penon['mac']},{frame_global},-65,{data_size},"
                f"{hex_data},"
                f"{frame_count_decoded},{FRAME_TYPE},{vbat_v:.3f},"
                f"{mag_z},{SD_MAG_Z},{MEAN_ACC},{SD_ACC},{MAX_ACC},"
                f"{mean_mag_z_mt:.3f},{sd_mag_z_mt:.3f},"
                f"{mean_acc_g:.3f},{sd_acc_g:.3f},{max_acc_g:.3f},"
                f"{flow_state},{is_attached},{lost_frames}"
            )

            rows.append({"ts": ts, "line": line})

            fc_seqs[idx] += 1
            frame_global += 1

        t += INTERVAL_S

    rows.sort(key=lambda r: r["ts"])

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(
            "Timestamp,MAC_Address,Frame_Number,RSSI,Data_Size,Raw_Hex_Data,"
            "frame_count,frame_type,vbat,mean_mag_z,sd_mag_z,mean_acc,sd_acc,max_acc,"
            "mean_mag_z_mt,sd_mag_z_mt,mean_acc_g,sd_acc_g,max_acc_g,"
            "flow_state,is_attached,lost_frames\n"
        )
        for r in rows:
            f.write(r["line"] + "\n")

    print(f"✅ {OUTPUT_FILE} généré : {len(rows)} trames, {len(penons)} pénons, {DURATION_S}s")
    print()
    print("Scénario :")
    print("   0s – 30s  : Tous ATTACHÉS")
    print("  30s – 60s  : Pénon B1 + B2 DÉTACHÉS (bâbord)")
    print("  60s – 90s  : Tous ATTACHÉS")
    print("  90s – 120s : Pénon T2 + T3 DÉTACHÉS (tribord)")
    print(" 120s – 180s : Tous ATTACHÉS")
    print()
    print("MACs simulées :")
    for p in penons:
        print(f'  {p["mac"]}  →  {p["name"]}')
    print()
    print("Dans l'app : Paramètres → Mode Simulation → Choisir test_multi_penon.csv")


if __name__ == "__main__":
    main()
