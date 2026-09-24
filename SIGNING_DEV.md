# Fixed DEV Signing

Before pushing 0.1.4, create these repository Actions secrets from the separate KEEP-PRIVATE bundle:

1. `GUARDIAN_DEV_KEYSTORE_BASE64`
2. `GUARDIAN_DEV_STORE_PASSWORD`
3. `GUARDIAN_DEV_KEY_ALIAS`
4. `GUARDIAN_DEV_KEY_PASSWORD`

Expected public certificate SHA-256:

`4a40d0075db9691b16814e40d7db589fdfea59a046a006c1a4db07c8901b8986`

Never upload `.jks`, Base64 key material or passwords to the public repository.

0.1.3 -> 0.1.4 requires one final uninstall.
0.1.4 -> later DEV builds should update in place.
Production will use a separate key.
