# State machine — v3.0

## Modes
- NORMAL: steady GREEN, brightness from nonvolatile settings.
- ALARM: saved ORANGE LED animation; buzzer according to saved duration and pulse mode.
- OFF: all lights and buzzer off; controller remains reachable.

## Allowed transitions
| Current | ALARM_ON | ALARM_RESET | ALL_OFF | POWER_ON |
|---|---|---|---|---|
| NORMAL | ALARM | NORMAL/no-op | OFF | NORMAL/no-op |
| ALARM | ALARM/no-op | NORMAL | **REJECT** | ALARM/no-op |
| OFF | **TBD: reject by default** | OFF/no-op | OFF/no-op | NORMAL |

The firmware must validate this table on every command. The UI disabling a button is not a safety control.

## Bluetooth ownership and reconnection (v3.0)
- **Bluetooth Low Energy only**; no Wi-Fi, local hotspot, or internet.
- One **authenticated owner phone** at a time. While the owner is BLE-connected, deny other phones' control connections.
- On unexpected BLE loss, **preserve the owner identity and reservation** and start a 30-minute continuous disconnected interval (based on ESP32-C3 monotonic runtime).
- During those 30 minutes, the same owner phone is allowed to auto-reconnect, including after temporary range loss or Android service restart. Other phones are denied.
- Every successful authenticated owner reconnection **cancels the expiry countdown**. A later disconnect starts a fresh full 30 minutes.
- If continuously disconnected for 30 minutes, clear owner reservation and permit a new authenticated phone to claim ownership. This should not require resetting the controller.
- A deliberate user disconnect **does not automatically release ownership** unless an explicit authorized release operation is implemented and confirmed.
- Restart behavior: do not grant control to arbitrary phones after power loss. Persist owner identity in NVS and use a documented restart policy (conservatively retain reservation); a safe, authenticated recovery or factory reset procedure must be designed.
- BLE advertising may continue for discovering the device, but advertisement visibility **does not confer access**. Enforce authorization on the ESP32-C3 using secure pairing/bonding and application-level owner checking as applicable; test authentication and bond behavior on Arduino-ESP32.
- Display "Owned by another phone" to unauthorized devices where appropriate; do not show operational controls as available.
- Current alarm and LED mode remain unchanged on Bluetooth loss, unless an independent safety system acts.
- For every accepted command, the ESP32-C3 validates allowed mode transitions, returns an acknowledgement and version, and ignores stale or duplicate requests.
- **Consequence:** Phone B cannot reset an alarm while Phone A is the current owner (including during the 30-minute reconnect window). This supersedes the earlier multi-phone simultaneous-reset requirement and should be reviewed for operational safety before deploying.
- In the first build, support one ESP32-C3 controller and one controlling phone at a time.

## Safety distinction
A test light/buzzer application is not a certified gas alarm. A real gas alarm needs an independent sensing and fail-safe signalling path; OFF restrictions alone do not make the installation safety-rated.

## Source material and v3.0 input design
- Existing v1.4 app **source is available** in the separate private repo `Hans1972/HP-Gas-Alert` under `android/`; firmware v1.4 is under `firmware/`. Use as a reference without editing the older repo.
- Complete Nano v1.9 sketch has been supplied as `HB_Alarm_Controller_v1_9_Alarm_Groen_Buzzer(1).zip`, including 40 effects and existing settings.
- **Four physical encoders are NOT part of v3.0**. No encoder pins, interrupt code, or save button are needed. Configure effect, brightness, orange hue, speed, layout and buzzer through the Android app; store using Preferences/NVS.
- **Physical alarm input D12 is removed**. Mode changes arrive through authorized app commands; controller alone validates transitions.
- USB serial at 115200 baud can remain as **diagnostics only**, and is not a required user interface for changing settings.

## Unresolved hardware / app integration
- Confirm exact XIAO GPIO pins for WS2812 data and buzzer driver before connecting hardware.
- Define the complete settings request/response protocol, persistent schema and BLE bonding/provisioning.
- Desired action for ALARM_ON when controller is OFF (default: reject).
- BLE authenticated owner, 30-minute lease expiry, restart and recovery security tests.
