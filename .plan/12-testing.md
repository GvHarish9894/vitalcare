# Testing

Prefer shared tests in commonTest; they run on every target.
- Android host: `./gradlew :shared:testAndroidHostTest`
- iOS simulator: `./gradlew :shared:iosSimulatorArm64Test`
Use platform test source sets (androidHostTest, iosTest) only for
platform-specific behavior.

Coverage
- Unit Tests (domain / use cases)
- Repository Tests
- DAO Tests (Room KMP, in-memory)
- ViewModel Tests
- Compose UI Tests (shared)
- Offline Sync Tests
- Performance Tests
