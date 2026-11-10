# Android UI — Gas Alert System v3.0

## Visual continuity with v1.4
Use the original Android UI in `Hans1972/HP-Gas-Alert/android/app/src/main/java/nl/hbelectronics/hpgastest/MainActivity.java` as the **visual reference**. Preserve:
- Full-screen status color changes: green for NORMAL, orange for ALARM, conspicuous fault/unreachable indication.
- Large, prominent translucent rounded main control with white lettering.
- Same typography scale, padding, footer **Made by HB-Electronics.nl · © 2026**, and compact connection/status information.
- Layout readable on small phones; auto-size long button labels and avoid clipped text.
- No redesign to unrelated card-heavy or white UI.

## Main screen: simple, uncluttered
Visible without scrolling:
1. `HP Gas Alert System` and `Version 3.0`.
2. Connection / current controller confirmation.
3. Big primary command: `ALARM AAN` when NORMAL; `ALARM RESET` when ALARM.
4. Clear confirmed mode `NORMAAL · GROEN`, `ALARM · ORANJE`, or `UIT`.
5. A secondary `ALLES UIT` action **enabled only while confirmed NORMAL**. When ALARM, do not permit shutdown.
6. When OFF, show `INSCHAKELEN` to return to NORMAL.
7. Small `Instellingen` entry with gear icon; long help text is **not** on the main screen.

## Settings (secondary screen, collapsible help)
- Brightness for green NORMAL — slider with live numeric preview, saved only after confirming.
- Orange alarm effect (preserve 40 original effects), effect speed, orange hue, brightness and afterglow — sliders with current numeric value.
- Strip layout and pixel color order via discrete choices, precise LED count via numeric input.
- Buzzer mode via discrete choice, duration and pulsed on/off times via sliders.
- Connection/setup and device status.
- `Uitleg en veiligheid` as **collapsed accordion** at the bottom of Settings, including long explanations about green/orange states, buzzer, Bluetooth ownership and reconnection, fallback behavior and safety limitations. No long paragraphs on primary view.
- Settings receive controller acknowledgement and are persisted in Preferences/NVS.

## Bluetooth ownership and reconnect UI
- BLE only; preserve v1.4's foreground-service automatic reconnect when Android permits it.
- Show clear statuses `Bluetooth connected`, `Reconnecting`, `Reserved for this phone (30-minute recovery window)`, or `Controlled by another phone`.
- While a phone owns the controller, other phones may not issue commands. This intentional exclusive lock overrides earlier simultaneous-phone control ideas.
- After the owner's link has remained disconnected for 30 minutes, another authenticated phone can claim control.
- The 30-minute timer resets only on a successful authenticated owner reconnection; it does not reset merely because the app is scanning or trying.
- When disconnected, show status as unknown/unconfirmed rather than pretending an old green indication is live.
- Keep all long pairing, ownership and safety explanations collapsed under `Instellingen → Uitleg en veiligheid`.

## Scope
One XIAO ESP32-C3 in the first v3.0 release. No encoders and no physical alarm input; diagnostics may remain over USB serial. v1.4 repository stays unchanged.

## FIX3 test status
- Firmware FIX3 and Android v3.0 exchange a persistent 32-character owner token through `AUTH` when connecting. The phone can reconnect despite a rotating Android BLE address.
- Firmware emits status heartbeat every three seconds to prevent the app's stale-status watchdog from causing a reconnect loop.
- Android CI build succeeded; hardware integration and BLE privacy/security validation still pending.
- The token is not a substitute for authenticated/encrypted BLE pairing; **bench testing only**.
