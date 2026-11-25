# Experimental boAt support

First milestone: isolated WuQi read-only protocol foundation for Nirvana Ion ANC, requiring exact device name and documented service UUID. Not integrated into Android discovery or control sessions yet. No commands sent to earbuds by this milestone; Huawei path unchanged.

Reference: https://github.com/bezalelsamuel/boat-tws-commands/blob/main/chipsets/wuqi.md (protocol facts, independently implemented). The reference reports real-device testing on Nirvana Ion ANC only. Other model/chipset variants must not be inferred from marketing names.

Next: validate reply payload layouts against captures, integrate separate RFCOMM session with serialized requests and timeouts, expose only confirmed capabilities in existing UI, run regression tests and test on physical boAt hardware. No reset, unknown commands, firmware commands or automatic writes. Other chipsets remain unsupported pending separate evidence.
