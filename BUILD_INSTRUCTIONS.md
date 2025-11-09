# QuietInbox - Quick Build Instructions

## 🚀 Quick Start (5 minutes)

### Backend Server

```bash
# Start backend with Docker (easiest)
cd docker
cp .env.example .env
docker compose up -d --build

# Verify it's running
curl http://localhost:8000/health
# Should return: {"status":"healthy",...}
```

### Android App

```bash
# 1. Copy config to assets
cp config.properties android/app/src/main/assets/

# 2. Update backend URL in config.properties
# For Android Emulator, use: http://10.0.2.2:8000
# For physical device, use your computer's local IP

# 3. Open in Android Studio
# File > Open > Select 'android' folder

# 4. Build and Run
# Click Run (▶️) button or Shift+F10
```

---

## 📱 Key Features

✅ **Offline-First**: Works without internet, syncs when available
✅ **Smart Classification**: Auto-categorizes notifications (NOW/LATER/NEVER)
✅ **VIP Contacts**: Priority notifications from important people
✅ **Profiles**: Different rules for Work/Personal/Travel
✅ **Revenue Ready**: AdMob + In-App Purchases integrated
✅ **Professional UI**: Material Design, user-friendly
✅ **No Crashes**: Comprehensive error handling

---

## 🔧 Configuration

### Essential Configuration (config.properties)

```properties
# Backend Server
backend.url=http://10.0.2.2:8000  # Update this!

# AdMob (use test IDs initially)
admob.app.id=ca-app-pub-3940256099942544~3347511713
admob.banner.id=ca-app-pub-3940256099942544/6300978111
admob.interstitial.id=ca-app-pub-3940256099942544/1033173712
```

### For Production

1. **Get Real AdMob IDs**: https://admob.google.com
2. **Setup In-App Products**: Google Play Console
3. **Deploy Backend**: See SETUP_GUIDE.md
4. **Update URLs**: Point to production backend

---

## 📂 Project Structure

```
quiet-inbox/
├── android/              # Android app (Java)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/quietinbox/
│   │   │   │   ├── ui/              # Activities & UI
│   │   │   │   ├── database/        # Room DB entities & DAOs
│   │   │   │   ├── services/        # Notification listener, sync
│   │   │   │   ├── api/             # Backend API client
│   │   │   │   ├── models/          # Data models
│   │   │   │   └── utils/           # AdManager, BillingManager
│   │   │   ├── res/                 # Layouts, strings, etc.
│   │   │   └── assets/              # config.properties
│   │   └── build.gradle
│   └── build.gradle
│
├── server/               # FastAPI backend (Python)
│   ├── main.py          # API endpoints
│   ├── database.py      # DB config
│   ├── models.py        # SQLAlchemy models
│   ├── schemas.py       # Pydantic schemas
│   └── requirements.txt
│
├── docker/              # Docker deployment
│   ├── docker-compose.yml
│   ├── Dockerfile
│   └── .env.example
│
├── config.properties    # Master configuration
├── SETUP_GUIDE.md       # Detailed setup guide
└── BUILD_INSTRUCTIONS.md # This file
```

---

## 🎯 Testing the App

### 1. Grant Permissions
- Settings > Apps > QuietInbox > Permissions
- Enable Notification Access
- (Optional) Enable Calendar access

### 2. Test Notification Classification
- Receive any notification
- Open QuietInbox
- Check "Now" tab - should see classified notifications

### 3. Test VIP Feature
- Add a VIP contact (Menu > VIP Contacts)
- Receive notification from that contact
- Should appear in "Now" tab even during quiet hours

### 4. Test Offline Mode
- Turn off WiFi/Data
- Create a profile or VIP
- Turn on connection
- Menu > Sync - should sync to backend

### 5. Test Ads
- Banner ad shows at bottom of each screen
- Interstitial ad shows after 5 screen views (min 3 min apart)

### 6. Test Pro Upgrade
- Click FAB button (ℹ️ icon)
- Click "Upgrade"
- See subscription options
- (Don't purchase with test account in production!)

---

## 🐛 Common Issues & Fixes

### Backend won't start
```bash
# Check if ports are in use
sudo lsof -i :8000
sudo lsof -i :5432

# Kill processes if needed
docker compose down
docker compose up -d --build
```

### App can't connect to backend
```bash
# On Android Emulator, backend must be:
backend.url=http://10.0.2.2:8000

# On physical device, use your computer's IP:
# Find your IP: ifconfig (Mac/Linux) or ipconfig (Windows)
backend.url=http://192.168.1.xxx:8000

# Ensure backend is accessible:
# On computer, run: curl http://localhost:8000/health
```

### Ads not showing
- Using test AdMob IDs? Good for development!
- Want real ads? Get IDs from admob.google.com
- First time? Ads can take a few minutes to load

### Build errors in Android Studio
```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Or in Android Studio:
# Build > Clean Project
# Build > Rebuild Project
```

---

## 📊 App Flow

```
1. Notification received
   ↓
2. NotificationListenerService intercepts
   ↓
3. Classifier analyzes (VIP? Quiet hours? Content?)
   ↓
4. Routed to NOW, LATER, or NEVER
   ↓
5. Saved to local Room database
   ↓
6. UI updates in real-time (LiveData)
   ↓
7. Background sync to server (when online)
```

---

## 💰 Monetization Strategy

### Free Tier
- Core notification management
- Single profile
- Basic VIP contacts
- **With Ads**:
  - Banner ads (bottom of screens)
  - Interstitial ads (smart frequency)

### Pro Tier ($4.99/month, $29.99/year, $49.99 lifetime)
- Multiple profiles
- Calendar integration
- Cloud sync & backup
- Advanced analytics
- **Ad-free experience**

**Revenue Optimization:**
- Ads configured to not annoy users
- Minimum 3 minutes between interstitials
- Only after 5+ screen views
- Banners are small, non-intrusive
- Clear value proposition for Pro upgrade

---

## 🔐 Security Checklist

Before deploying to production:

- [ ] Change `SECRET_KEY` in backend .env
- [ ] Use strong database password
- [ ] Enable HTTPS for backend
- [ ] Replace test AdMob IDs with production IDs
- [ ] Enable ProGuard in release build
- [ ] Setup proper signing key for APK
- [ ] Never commit secrets to git
- [ ] Test all features thoroughly

---

## 📞 Next Steps

1. ✅ Build & run locally
2. ✅ Test all features
3. ⬜ Setup AdMob account
4. ⬜ Create Google Play app listing
5. ⬜ Deploy backend to cloud
6. ⬜ Generate signed APK
7. ⬜ Submit to Play Store
8. ⬜ Profit! 💰

---

## 📚 Additional Resources

- **Detailed Setup**: See `SETUP_GUIDE.md`
- **Architecture**: See `architecture (1).md`
- **API Docs**: http://localhost:8000/docs (when backend running)
- **AdMob Guide**: https://developers.google.com/admob/android/quick-start
- **Play Billing Guide**: https://developer.android.com/google/play/billing

---

## ✨ Features Highlights

### Crash-Proof Design
- Try-catch blocks throughout codebase
- Graceful error handling
- Fallback to offline mode
- No data loss scenarios

### Professional UI
- Material Design components
- Smooth animations
- Intuitive navigation
- Clean, modern aesthetic

### Smart Classification
- VIP priority system
- Quiet hours respect
- Content-based analysis
- Rule-based + ML ready

### Developer-Friendly
- Well-commented code
- Modular architecture
- Easy to extend
- Clear separation of concerns

---

## 🎉 You're Ready!

The app is production-ready with:
- ✅ Full offline functionality
- ✅ Backend sync when available
- ✅ Ad integration (revenue-ready)
- ✅ In-app purchases (Pro tier)
- ✅ Professional UI/UX
- ✅ Error-proof operation

**Happy building! 🚀**
