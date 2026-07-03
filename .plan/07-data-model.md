# Data Model

Patient
- id
- name
- email

VitalRecord
- id
- patientId
- date
- time
- spo2
- heartRate
- systolicBP
- diastolicBP
- remarks
- createdAt
- updatedAt
- syncStatus

SyncStatus
- Pending
- Syncing
- Synced
- Failed
