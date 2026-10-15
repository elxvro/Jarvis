#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/src/main/assets/voice
sample() {
  espeak-ng -v tr -s 145 -p 40 -a 200 -w "app/src/main/assets/voice/$1.wav" "$2"
  test -s "app/src/main/assets/voice/$1.wav"
}
sample test "Merhaba. Ben Mia. İstersen konuşabilir ya da yazabilirsin."
sample ready "Mia hazır. İstersen adımı söyle."
sample listening "Dinliyorum."
sample stopping "Dinlemeyi kapatıyorum."
sample help "Instagram, Play Konsol, web sitesi, arama veya dur diyebilirsin."
sample instagram "Instagram bağlantısını bildirimine ekledim."
sample play "Play Konsol bağlantısını bildirimine ekledim."
sample website "Web sitesi bağlantısını bildirimine ekledim."
sample phone "Arama ekranını açman için bildirim hazırladım."
sample unknown "Bu komutu henüz gerçekleştiremiyorum."
python3 - <<'PY'
import wave
from pathlib import Path
files = list(Path("app/src/main/assets/voice").glob("*.wav"))
assert len(files) == 10, f"Expected 10 Turkish speech files, got {len(files)}"
for p in files:
  with wave.open(str(p), "rb") as w:
    assert w.getnframes() > 400, f"Invalid spoken file: {p}"
print("Built", len(files), "offline Turkish voice files")
PY
