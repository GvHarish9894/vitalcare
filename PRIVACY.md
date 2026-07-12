# VitalCare — Privacy Policy

**Effective date:** 12 July 2026
**Last updated:** 12 July 2026

VitalCare ("the app") is a local-first patient-vitals app for Android and iOS.
This policy explains what data the app handles and where it goes. It is written
in plain language because the honest summary is short: **your health data stays
on your device.**

## Summary

- **No account, no sign-up, no backend.** VitalCare has no user login and
  operates no server that stores your data.
- **Your health data never leaves your device** unless *you* choose to export it
  (CSV) or connect your own Google Drive for backup.
- We (the developers) **never receive your health data.** There is no server we
  control that it could be sent to.

## 1. What data the app stores

The following are stored **only on your device**, in the app's private storage:

- **Vitals you record** — SpO₂, heart rate, blood pressure, and any remarks or
  timestamps you add.
- **Fluid entries** — water intake and urine output amounts, times, and notes.
- **Your profile name** (optional) and app preferences (theme, units, reminder
  and backup settings).

This data is protected by the operating system's app sandbox and device
encryption. Uninstalling the app permanently removes all of it.

## 2. Data you can choose to share

VitalCare never transmits your health data automatically. Two optional,
user-initiated features can move a copy of your data off the device:

- **CSV export.** You can export your records to a CSV file and save or share it
  wherever you choose using your device's standard share sheet. What happens to
  that file afterwards is entirely under your control.
- **Google Drive backup (optional).** If you connect Google Drive, the app
  uploads a single backup file to a hidden, app-private folder
  (`appDataFolder`) in **your own** Google Drive account. This copy goes only to
  your Drive — never to us. The app requests the least-privilege
  `drive.file` scope, which lets it access **only the files it created**; it can
  never see the rest of your Drive. You can disconnect Drive at any time, which
  revokes the app's access, and you can delete the backup from your Drive
  yourself.

## 3. Anonymous, health-data-free analytics & crash reporting

To understand general usage and fix crashes, the app may collect **anonymous,
aggregate diagnostics** via Firebase Analytics and Crashlytics. This telemetry:

- **Never** includes any vital value, fluid entry, remark, or your profile name.
- Carries only non-identifying information such as screen names, feature-usage
  counts, app version, and crash stack traces.
- Can be turned **off** at any time in **Settings → Privacy**.

We do not sell data, and we do not use it for advertising. The app contains no
ads.

## 4. Permissions

- **Notifications** — requested only if you enable vitals reminders, so the app
  can remind you to take a reading. Never used for marketing.
- **Internet** — used only for the optional Google Drive backup/restore you
  initiate, and for the anonymous diagnostics above. The app makes no other
  network calls.

VitalCare requests no location, contacts, microphone, camera, or broad storage
permissions.

## 5. Children's privacy

VitalCare is a general-purpose personal health record tool and is not directed
at children. It collects no personal identifiers.

## 6. Data retention and deletion

Because there is no account and no backend, there is nothing for us to retain or
delete. To remove your data:

- **On your device:** delete records in the app, or uninstall the app to remove
  everything.
- **In Google Drive (if used):** disconnect Drive in the app to revoke access,
  and delete the backup file from your Google Drive.

## 7. Not a medical device

VitalCare is a personal record-keeping tool. It is **not a medical device** and
does not diagnose, treat, cure, or prevent any disease, nor provide medical
advice. Always consult a qualified healthcare professional regarding your
health. In an emergency, contact your local emergency services.

## 8. Changes to this policy

If this policy changes, the "Last updated" date above will change and the
updated policy will be published at the same location.

## 9. Contact

For questions about this policy, open an issue at the project's repository:
<https://github.com/GvHarish9894/vitalcare>.
