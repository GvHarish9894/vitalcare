# Sync Engine

Offline First

Every save:
Room -> Pending

Sync logic (shared, in commonMain):
Pending
-> Upload Firestore
-> Mark Synced

Scheduling (expect/actual — platform triggers the shared sync logic):
- Android: WorkManager
- iOS: BGTaskScheduler
A common SyncScheduler interface abstracts scheduling; the actual upload/mark
work stays shared.

Retry
Exponential backoff

Conflict Resolution
Last updated timestamp wins.
