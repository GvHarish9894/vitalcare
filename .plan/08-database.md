# Database

Room KMP (androidx.room multiplatform + KSP + bundled SQLite driver)
- Entities and DAOs defined once in commonMain.
- Database builder provided per platform via expect/actual
  (Android needs a Context; iOS uses a documents-directory path).

Room Entities
- PatientEntity
- VitalRecordEntity

Indexes
- patientId
- date

DAO
- insert
- update
- delete
- getToday
- getHistory
- getByDateRange

Migration
Never destructive in production.
