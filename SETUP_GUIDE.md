# QuietInbox - Complete Setup Guide

## Overview

QuietInbox is a complete smart notification management system with:
- **Android App** (Java) - Offline-first with sync capability
- **Backend Server** (FastAPI/Python) - REST API with PostgreSQL
- **Revenue Model** - AdMob integration + In-App Purchases
- **Professional UI** - Material Design, user-friendly interface

## Table of Contents

1. [Backend Server Setup](#backend-server-setup)
2. [Android App Setup](#android-app-setup)
3. [Configuration](#configuration)
4. [Deployment](#deployment)
5. [Monetization Setup](#monetization-setup)
6. [Troubleshooting](#troubleshooting)

---

## Backend Server Setup

### Prerequisites
- Docker & Docker Compose (recommended)
- OR: Python 3.11+, PostgreSQL 15+, Redis 7+

### Option 1: Docker Deployment (Recommended)

```bash
# 1. Navigate to docker directory
cd docker

# 2. Copy environment file
cp .env.example .env

# 3. Edit .env file with your values
nano .env

# 4. Start all services
docker compose up -d --build

# 5. Check status
docker compose ps

# 6. View logs
docker compose logs -f api
```

The API will be available at: `http://localhost:8000`

### Option 2: Manual Deployment

```bash
# 1. Install PostgreSQL and Redis
sudo apt-get update
sudo apt-get install postgresql redis-server

# 2. Create database
sudo -u postgres psql
CREATE DATABASE quietinbox;
CREATE USER quietinbox WITH PASSWORD 'quietinbox';
GRANT ALL PRIVILEGES ON DATABASE quietinbox TO quietinbox;
\q

# 3. Install Python dependencies
cd server
pip install -r requirements.txt

# 4. Set environment variables
export DATABASE_URL="postgresql://quietinbox:quietinbox@localhost:5432/quietinbox"
export SECRET_KEY="your-secret-key-here"

# 5. Run server
python main.py

# Or with uvicorn
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Testing the Backend

```bash
# Health check
curl http://localhost:8000/health

# Expected response:
# {"status":"healthy","database":"connected","timestamp":"..."}
```

---

## Android App Setup

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK 24+ (Android 7.0+)

### Building the App

1. **Open Project in Android Studio**
   ```bash
   cd android
   # Open this folder in Android Studio
   ```

2. **Sync Gradle Files**
   - Android Studio will automatically detect the project
   - Click "Sync Now" when prompted
   - Wait for Gradle sync to complete

3. **Configure Backend URL**
   - Edit `android/app/src/main/assets/config.properties`
   - Update `backend.url` to your server URL:
   ```properties
   backend.url=https://your-domain.com
   # Or for local testing:
   # backend.url=http://10.0.2.2:8000 (Android Emulator)
   # backend.url=http://192.168.1.x:8000 (Physical Device)
   ```

4. **Build APK**
   - For Debug: `Build > Build Bundle(s) / APK(s) > Build APK(s)`
   - For Release: `Build > Generate Signed Bundle / APK`

5. **Install on Device**
   - Connect Android device via USB
   - Enable USB Debugging on device
   - Click Run (▶️) in Android Studio

---

## Configuration

### Main Configuration File

Edit `config.properties` in the repository root:

```properties
# ============================================
# ADMOB CONFIGURATION
# ============================================
# Get these from Google AdMob Console
admob.app.id=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
admob.banner.id=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
admob.interstitial.id=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX

# Ad Display Settings
admob.interstitial.frequency=5  # Show after every 5 screens
admob.interstitial.min_interval_seconds=180  # Min 3 minutes between ads

# ============================================
# IN-APP PURCHASE CONFIGURATION
# ============================================
# Get these from Google Play Console
iap.pro.monthly.sku=quietinbox_pro_monthly
iap.pro.yearly.sku=quietinbox_pro_yearly
iap.pro.lifetime.sku=quietinbox_pro_lifetime

# ============================================
# BACKEND SERVER CONFIGURATION
# ============================================
backend.url=https://api.quietinbox.com
backend.timeout.connect=10
backend.timeout.read=30
backend.sync.interval.minutes=60
```

This file is automatically loaded by both the Android app and can be used for backend configuration.

---

## Deployment

### Production Backend Deployment

#### Using Docker Compose (Recommended)

```bash
# 1. Update .env with production values
cd docker
nano .env

# Set production values:
# - Strong SECRET_KEY
# - Secure database password
# - Production domain

# 2. Update nginx.conf with your domain
nano nginx.conf

# 3. Deploy
docker compose -f docker-compose.yml up -d --build

# 4. Setup SSL (Let's Encrypt)
# Add certbot to docker-compose.yml or use standalone
```

#### Using Cloud Platforms

**Heroku:**
```bash
heroku create quietinbox-api
heroku addons:create heroku-postgresql:hobby-dev
heroku addons:create heroku-redis:hobby-dev
git push heroku main
```

**AWS EC2:**
- Launch EC2 instance (Ubuntu 22.04)
- Install Docker & Docker Compose
- Clone repository
- Run docker-compose up

**Google Cloud Run:**
```bash
gcloud builds submit --tag gcr.io/PROJECT-ID/quietinbox-api
gcloud run deploy quietinbox-api --image gcr.io/PROJECT-ID/quietinbox-api
```

### Production Android App Deployment

1. **Prepare for Release**
   - Update `android/app/build.gradle`:
     - Increment `versionCode`
     - Update `versionName`
   - Configure signing keys

2. **Generate Signed APK/Bundle**
   ```bash
   # In Android Studio:
   Build > Generate Signed Bundle / APK
   # Choose AAB (Android App Bundle) for Play Store
   ```

3. **Upload to Google Play Console**
   - Create app in Play Console
   - Upload AAB file
   - Complete store listing
   - Submit for review

---

## Monetization Setup

### AdMob Setup

1. **Create AdMob Account**
   - Go to https://admob.google.com
   - Sign up with your Google account

2. **Create App**
   - Apps > Add App
   - Select Android platform
   - Enter app name: "QuietInbox"

3. **Create Ad Units**
   - Create Banner Ad Unit
     - Name: "QuietInbox Banner"
     - Copy Ad Unit ID
   - Create Interstitial Ad Unit
     - Name: "QuietInbox Interstitial"
     - Copy Ad Unit ID

4. **Update Configuration**
   - Edit `config.properties`
   - Replace test IDs with your production IDs
   - Copy to `android/app/src/main/assets/config.properties`

5. **Test Ads**
   - Use test device IDs during development
   - Never click your own ads in production

### Google Play Billing Setup

1. **Create In-App Products**
   - Go to Google Play Console
   - Monetization > In-app products
   - Create products:

   **Monthly Subscription:**
   - Product ID: `quietinbox_pro_monthly`
   - Price: $4.99/month

   **Yearly Subscription:**
   - Product ID: `quietinbox_pro_yearly`
   - Price: $29.99/year

   **Lifetime Purchase:**
   - Product ID: `quietinbox_pro_lifetime`
   - Price: $49.99 (one-time)

2. **Update SKU IDs**
   - Edit `config.properties` if using different SKUs

3. **Test Purchases**
   - Add test Gmail accounts in Play Console
   - Test subscription flow before going live

---

## App Features

### Offline-First Architecture

- **Works without backend**: App functions fully offline
- **Local database**: All data saved in Room database
- **Automatic sync**: Syncs when connection restored
- **Queue management**: Pending changes queued for sync
- **No data loss**: User can create profiles, VIPs offline

### Error Handling

The app includes comprehensive error handling:
- Network failures → Graceful degradation
- Server downtime → Continue working offline
- Sync failures → Automatic retry with backoff
- Crash prevention → Try-catch blocks throughout

### Ad Strategy (Non-Intrusive)

1. **Banner Ads**
   - Fixed at bottom of each screen
   - Configurable: Can be disabled
   - Small, non-blocking

2. **Interstitial Ads**
   - Smart frequency control
   - Minimum 3 minutes between shows
   - Only after 5+ screen views
   - Never during critical actions

### Pro Features

Free users get:
- Core notification management
- Single default profile
- Basic VIP contacts
- Manual rules

Pro users get:
- Multiple profiles (Work/Personal/Travel)
- Calendar integration
- Cloud sync & backup
- Advanced analytics
- Ad-free experience

---

## Troubleshooting

### Backend Issues

**Database connection fails:**
```bash
# Check PostgreSQL is running
docker compose ps postgres

# Check logs
docker compose logs postgres

# Restart service
docker compose restart postgres
```

**API not accessible:**
```bash
# Check API logs
docker compose logs api

# Verify port mapping
docker compose ps

# Test locally
curl http://localhost:8000/health
```

### Android Issues

**Build fails:**
- Clean project: `Build > Clean Project`
- Invalidate caches: `File > Invalidate Caches / Restart`
- Sync Gradle: `File > Sync Project with Gradle Files`

**Ads not showing:**
- Check AdMob IDs in config.properties
- Verify app registered in AdMob
- Use test device during development

**Notification access not working:**
- Settings > Apps > Special access > Notification access
- Enable QuietInbox

**Sync not working:**
- Check backend URL in config
- Verify backend is running
- Check device network connection
- Review logs in Logcat

### Common Issues

1. **"Network not available" error**
   - Check `backend.url` in config
   - Ensure backend is accessible from device
   - For emulator, use `http://10.0.2.2:8000`
   - For device, use local IP: `http://192.168.x.x:8000`

2. **Billing not working**
   - Ensure app is published (at least in Internal Testing)
   - Add test account in Play Console
   - Products must be active
   - Wait up to 24 hours after product creation

3. **Notifications not being captured**
   - Grant notification access permission
   - Ensure NotificationListenerService is enabled
   - Check if app is in battery optimization whitelist

---

## Development Notes

### Database Migrations

When modifying database schema:
```java
// Update version in AppDatabase.java
@Database(version = 2)  // Increment version

// Add migration
static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE ...");
    }
};
```

### Adding New Features

1. Update database entities if needed
2. Update API endpoints in backend
3. Update API models in Android
4. Implement UI
5. Add to sync manager if needed
6. Update documentation

### Testing

**Backend:**
```bash
# Run tests
cd server
pytest

# Test specific endpoint
curl -X POST http://localhost:8000/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"device_id":"test123"}'
```

**Android:**
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

---

## Support & Documentation

- Architecture: `architecture (1).md`
- API Documentation: `http://your-backend/docs` (FastAPI auto-docs)
- Source code: Well-commented Java & Python

---

## Security Notes

1. **Never commit:**
   - Production AdMob IDs
   - Google Play signing keys
   - Backend SECRET_KEY
   - Database passwords

2. **Use environment variables** for sensitive data

3. **Enable ProGuard** for release builds

4. **Use HTTPS** in production

---

## License

See repository license file.

## Contributors

Built with ❤️ for smart notification management.
