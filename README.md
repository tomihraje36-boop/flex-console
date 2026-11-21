# FLEX – herná konzola z Android telefónu

Launcher (domovská obrazovka) v štýle konzoly:
- po štarte animácia **FLEX / SOFTWARE**
- horná lišta: čas, Wi‑Fi + signál, Bluetooth, batéria (%, nabíjanie), ozubené koliesko = nastavenia
- **Knižnica** – nainštalované hry (aj automaticky nájdené hry v telefóne) + pridané cloud hry
- **Obchod** – katalóg hier (`app/src/main/assets/games.json`), pri každej: LOKÁLNE / CLOUD
  - telefón podporuje obe možnosti → spýta sa, či lokálne alebo Xbox Cloud Gaming
  - podporuje len jednu → rovno ju použije (lokálne = otvorí Google Play, cloud = spustí stream)
  - pri spustení ukáže „Hráš lokálne“ alebo „Hráš cez Xbox Cloud Gaming“
  - dlhé podržanie dlaždice = detail, úprava podpory, zabudnutie voľby
- **Aplikácie** – všetky appky telefónu
- **Nastavenia** – Wi‑Fi, Bluetooth, Batéria, Zvuk, Ovládače, Displej, Systém
- ovládanie dotykom aj gamepadom (D‑pad, A, B)

## Inštalácia do telefónu
1. V telefóne otvor: **https://github.com/tomihraje36-boop/flex-console/releases/latest/download/FLEX.apk**
2. Stiahni → otvor → povoľ „Inštalovať neznáme aplikácie“ pre prehliadač → Inštalovať.
3. Stlač tlačidlo Domov → vyber **FLEX** → **Vždy**.

Každá zmena v repozitári spustí zostavu (záložka **Actions**, ~5–8 min) a nové APK je na tom istom odkaze – nainštaluje sa ako aktualizácia.

## Alternatíva: Android Studio + USB kábel
Otvor priečinok v Android Studio → v telefóne zapni *Možnosti pre vývojárov → Ladenie cez USB* → pripoj kábel → ▶ Run.

## Úprava katalógu hier
`app/src/main/assets/games.json` – položky: `id`, `title`, `package` (Google Play balík),
`local`, `cloud`, `cloudUrl`, `color` (ARGB hex), voliteľne `minAndroid` (API level), `minRamGb`.
