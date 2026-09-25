# Privacy Model — Guardian 0.1.8

Privacy Engine precedes Storage.

APP may contain non-private identity.
PRIVATE and SYSTEM never contain identity.
Other Android users remain generic PRIVATE.
ANONYMOUS_BROWSER is never guessed.

Local Intelligence v2 reads only sanitized report data.
Questions are processed in memory and discarded.

WorkManager only schedules the same privacy-first UsageCollector. It adds no content or network access.

Never collected: screenshots/video, typed input, passwords, message/notification content, clipboard, banking content, Settings content, full URLs, page content or identifiable other-user activity.
