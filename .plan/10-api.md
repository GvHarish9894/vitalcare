# Cloud API

Firebase is accessed from shared code via the GitLive Firebase Kotlin SDK
(dev.gitlive:firebase-auth, dev.gitlive:firebase-firestore), which wraps the
native Firebase SDKs on Android and iOS behind one common API.

Platform setup required:
- Android: google-services.json + Google Services Gradle plugin
- iOS: GoogleService-Info.plist + Firebase iOS SDK (via CocoaPods or SPM)

Firestore Collections

patients/{patientId}

patients/{patientId}/vitals/{recordId}

Fields mirror local database.

Future REST API: Ktor client (KMP).
