# Experimental boAt support

Nirvana Ion ANC (WuQi service 7034): paired device discovery, dedicated foreground RFCOMM transactions, battery readings, ANC/transparency/off, and wear detection with readback. Controls remain disabled until status is confirmed. No Huawei commands routed to recognised boAt devices. No automatic writes. EQ encoder exists but UI is withheld until acknowledgement handling is validated. No other chipset or model is claimed supported. Physical testing and CI required. Reference: https://github.com/bezalelsamuel/boat-tws-commands/blob/main/chipsets/wuqi.md

## Airdopes 800 HiDef
Exact model recognition and Android connection state only. Official support: https://support.boat-lifestyle.com/articles/airdopes-true-wireless/airdopes-800-hidef/68cbb4a4efbb094d9620a2a2 . The original 800 is associated with BES; HiDef transport and chipset remain unconfirmed. Offline BES frame decoder is based on https://github.com/bezalelsamuel/boat-tws-commands/blob/main/chipsets/bes.md . No BES commands are transmitted. Obtain model-specific SDP/GATT and official-app captures with user consent, validate battery, EQ, multipoint, wear and touch operations separately; never probe unknown commands. No ANC controls for HiDef.

## Multi-chipset discovery
Registry distinguishes model/service evidence from transmit authorization. Offline capture decoders: BES, Bluetrum, Airoha RACE, CEVA; standard BLE battery value validation. JieLi RCSP and GAIA outer framing/transport remain unverified; no fake drivers. No automatic probing, reset, firmware, or cross-chipset writes. HiDef's exact model reference explicitly lists chipset unknown: https://github.com/bezalelsamuel/boat-tws-commands/blob/main/models/Airdopes%20800%20HiDef/README.md .
