# QuietInbox

A unified notification hub that learns what future-you should see now, later, or never. Profiles for work/personal, VIP rules, and calendar-aware deferrals.

## Features
- Notification listener & smart triage (Now / Later / Never)
- Bundles by app/topic; VIP bypass
- Profiles (Work/Personal/Travel)
- Calendar aware: quiet hours & meetings
- Optional cross-device sync & backup

## Stack
- **Android:** Kotlin, NotificationListenerService, Jetpack Compose, Room, WorkManager, Retrofit, Play Billing
- **Backend:** FastAPI, Postgres, Redis, FCM, Nginx
- **AI:** On-device TFLite classifier (keep/later/mute); server suggests deferral windows (opt-in telemetry)

## Setup

### Android
- Enable notification access for QuietInbox.
- (Optional) Connect calendar for meeting-aware deferrals.
- Create profiles and VIP contacts.

### Backend
```bash
cd server
cp ../docker/.env.example .env
docker compose -f ../docker/docker-compose.yml up -d --build
```

## API (excerpt)
```
GET    /v1/profile
POST   /v1/profile
POST   /v1/sync/push
GET    /v1/recommendations/deferral-windows
```

## Monetization
- Free: core inbox, bundles, manual rules
- Pro: multi-profiles, calendar/meeting awareness, sync/backup, analytics

## Privacy
- Notification content stays on device by default. Telemetry is explicit opt-in and minimized.

## Troubleshooting
- Not receiving: confirm Notification Access is enabled.
- DND conflicts: allow QuietInbox as DND exception if needed.
