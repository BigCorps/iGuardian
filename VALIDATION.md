# Validation — Android 0.1.4

## Evidence from longer 0.1.3 real-device run

Xiaomi/Redmi Android 16 showed:
- Usage Access active;
- 130 APP intervals by diagnostic time;
- 14 SCREEN_OFF intervals;
- 13 unlocks;
- verified JSON export;
- privacy classifier v2;
- scheduler accepted initially but later not pending.

The run covered about 98% of the period after tracking actually began.

## 0.1.4 acceptance

GitHub Actions must compile/test and verify DEV certificate SHA-256:

`4a40d0075db9691b16814e40d7db589fdfea59a046a006c1a4db07c8901b8986`

If the APK is signed by any other certificate, CI fails.
