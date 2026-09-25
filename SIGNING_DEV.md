# Fixed DEV Signing

Repository Actions secrets:
1. `GUARDIAN_DEV_KEYSTORE_BASE64`
2. `GUARDIAN_DEV_STORE_PASSWORD`
3. `GUARDIAN_DEV_KEY_ALIAS`
4. `GUARDIAN_DEV_KEY_PASSWORD`

Expected public certificate SHA-256:

`4a40d0075db9691b16814e40d7db589fdfea59a046a006c1a4db07c8901b8986`

Never commit `.jks`, Base64 key material or passwords.

Historical transition:
- 0.1.3 -> 0.1.4 required one final uninstall because 0.1.3 used an ephemeral runner key.
- 0.1.4 -> 0.1.5 in-place update was validated successfully with local state/history preserved.
- 0.1.6 must continue using the same fixed DEV certificate.

Production will use a separate production key.
