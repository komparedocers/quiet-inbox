# QuietInbox Architecture

## Brief
QuietInbox intercepts Android notifications, classifies them into Now/Later/Never, and learns a personal deferral strategy.

## Client Architecture
- Services:
  - `NotifListenerService` → raw events
  - `Classifier` (on-device embeddings + logistic/TFLite)
  - `Router` (rules + model → lane assignment)
- UI:
  - Feed with Now/Later tabs
  - Profiles switcher
  - VIP editor, Rules editor, Calendar settings
- Data:
  - Room: NotificationEvent, Profile, VIP, Rules
  - EncryptedPrefs: tokens, flags
- Background:
  - WorkManager: deferral delivery, daily batch, sync
- Permissions:
  - Notification access, Calendar read (optional), Alarms/Exact alarms

## Backend
- Services:
  - `profile-sync`: settings & VIPs
  - `telemetry`: minimal, anonymous feature counts
  - `recommendation`: suggested deferral windows by cluster
  - `push`: FCM scheduling for batched digests
- Storage:
  - Postgres: User, Profile, VIP, Schedules
  - Redis: job queues
- Auth:
  - OAuth2/JWT; device bind; E2EE option for profile backup

## ML
- Features: app, sender, topic embedding, time-of-day, day-of-week, historical user action, meeting state
- Training: on-device continual fine-tuning (cached gradients or threshold updates)
- Evaluation: user correction loop (swipe re-classifies and retrains)

## Data Model (simplified)
- **NotificationEvent**(id, app, title, text, topic, received_at, action, confidence)
- **Profile**(id, quiet_hours, rules_json)
- **VIP**(id, app, identifier, priority)

## Flows
1. Notification → Classifier → Router (apply VIP + profile rules) → Feed
2. Deferrals → scheduled WorkManager jobs → FCM nudge if needed
3. User feedback → on-device model update

## NFRs
- Latency < 30ms classification on mid devices
- Zero content upload by default
- Crash-safe queues; idempotent sync

## Risks
- App vendor policy shifts → keep manual rules robust
- Battery impact → foreground service avoidance; batching; backoff
