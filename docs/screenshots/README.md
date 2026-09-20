# Screenshot assets

Drop real, unedited screenshots of the shipped app into **this folder**, using the
exact filenames below. The gallery in the root [`README.md`](../../README.md) already
references these paths — it is kept commented out so the README never renders broken
images. Once the files are here, uncomment the single `SCREENSHOT GALLERY` block in
the root README and the gallery goes live.

> Only genuine screenshots of the real app belong here. Do not add mockups, renders
> or composites — the project is presented as research/SIH work and the gallery is
> read as evidence.

## Required files

| Filename | Screen | How to reach it |
|---|---|---|
| `01-explore.png` | Explore (home) | Launch the app → **Explore** tab |
| `02-map.png` | Full risk map | **Map** tab |
| `03-risk-areas.png` | Risk Areas list | **Risk Areas** tab |
| `04-area-details.png` | Area detail sheet | **Risk Areas** → tap any monitored area |
| `05-alerts.png` | Alert inbox | **Alerts** tab |
| `06-alert-detail.png` | Alert detail | **Alerts** → tap an alert |
| `07-search.png` | Place search | Tap the search bar on **Explore** or **Map** |
| `08-offline.png` | Offline / cached mode | Enable airplane mode, then open **Risk Areas** |
| `09-more.png` | More / settings | **More** tab |
| `10-dashboard.png` | Authority dashboard | <https://www.landguard.online/> in a desktop browser |

## Capture guide

Phone screens: portrait, stock Android screenshot (no frames, no drop shadows, no
device mockups). Keep the status bar. `1080 × 2400` or whatever your device produces
natively is fine — GitHub scales them inside the gallery table.

From a connected device you can also capture with adb:

```bash
adb exec-out screencap -p > docs/screenshots/01-explore.png
```

Keep each file under ~500 KB so the repository stays light; `pngquant` or any lossless
PNG optimiser is enough. Do not commit video or `.gif` walkthroughs here.
