# LibreBuds 6i v1.0.0

The first versioned stable-release candidate of the FreeBuds 6i-focused fork.

## Highlights
- QuickBuds-inspired interface is the single default style (the previous glass design selector was retired).
- Refined Home / Controls / Details layout, including Audio / Gestures / More sections.
- Compact left / case / right battery rings, rounded and aligned ANC mode selector.
- Quick Settings for Auto pause, Low latency, Audio priority and Multipoint when available.
- Built-in equalizer presets, live 10-band custom EQ curve and preset editor.
- Find Earbuds, multipoint management, per-earbud gestures and other model-specific controls.
- AMOLED black theme and Mint / Ocean blue / Violet / Coral accents.
- Updated README, screenshots and acknowledgments to upstream LibreBuds and QuickBuds.

## Compatibility
- Android 13 or newer.
- FreeBuds 6i support, with other supported models retaining their existing model-specific controls.
- Some settings depend on the earbud firmware and capability reports; the complete feature set is not verified on every model.

## Install / update
- Download the signed `librebuds.apk` attached to this release.
- Existing installations can update in place only if signed by the same signing certificate and with a lower versionCode than this release.
- If the signatures differ, Android will block an in-place upgrade; don't uninstall without considering the loss of local app settings.

## Attribution
Built on [LibreBuds](https://github.com/librebuds/librebuds). The interface is independently implemented using [QuickBuds](https://github.com/spizganed/QuickBuds) as a visual reference. Original GPLv3 licensing and project credits are preserved in README and NOTICE.
