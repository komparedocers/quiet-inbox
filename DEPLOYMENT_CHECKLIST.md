# QuietInbox - Deployment Checklist

## Pre-Deployment Checklist

### Backend Configuration

- [ ] **Environment Variables Set**
  ```bash
  DATABASE_URL=postgresql://user:pass@host:5432/db
  SECRET_KEY=<strong-random-key>
  REDIS_URL=redis://host:6379
  ```

- [ ] **Database Setup**
  - [ ] PostgreSQL installed and running
  - [ ] Database created
  - [ ] Tables initialized (automatic on first run)

- [ ] **Security**
  - [ ] Change default SECRET_KEY
  - [ ] Use strong database password
  - [ ] Enable HTTPS/SSL
  - [ ] Configure CORS for production domain
  - [ ] Firewall rules configured

### Android App Configuration

- [ ] **AdMob Setup**
  - [ ] Created AdMob account
  - [ ] Registered app in AdMob
  - [ ] Created Banner ad unit
  - [ ] Created Interstitial ad unit
  - [ ] Updated `config.properties` with real Ad Unit IDs
  - [ ] Tested ads on device

- [ ] **Google Play Billing**
  - [ ] Created products in Play Console:
    - [ ] Monthly subscription (quietinbox_pro_monthly)
    - [ ] Yearly subscription (quietinbox_pro_yearly)
    - [ ] Lifetime purchase (quietinbox_pro_lifetime)
  - [ ] Products are active
  - [ ] Tested with test account

- [ ] **App Configuration**
  - [ ] Updated `backend.url` to production URL
  - [ ] Copied `config.properties` to `android/app/src/main/assets/`
  - [ ] Updated `versionCode` in build.gradle
  - [ ] Updated `versionName` in build.gradle

- [ ] **Build Configuration**
  - [ ] Generated signing key
  - [ ] Configured signing in build.gradle
  - [ ] Enabled ProGuard/R8
  - [ ] Tested release build

### Google Play Console

- [ ] **App Listing**
  - [ ] Created app in Play Console
  - [ ] Uploaded app icon (512x512)
  - [ ] Uploaded feature graphic
  - [ ] Added screenshots (4-8 images)
  - [ ] Written app description
  - [ ] Set content rating
  - [ ] Selected category
  - [ ] Set pricing (Free)

- [ ] **Store Presence**
  - [ ] Privacy policy URL provided
  - [ ] Support email provided
  - [ ] Support website (optional)

- [ ] **Release Management**
  - [ ] Internal testing track created
  - [ ] Test users added
  - [ ] APK/AAB uploaded
  - [ ] Release notes written

## Deployment Steps

### 1. Deploy Backend

#### Option A: Docker (Recommended)

```bash
# On server
cd /opt/quietinbox
git clone <your-repo>
cd quiet-inbox/docker
cp .env.example .env
nano .env  # Edit with production values

# Start services
docker compose up -d --build

# Verify
curl https://api.yourdomain.com/health
```

#### Option B: Manual

```bash
# Install dependencies
sudo apt-get update
sudo apt-get install postgresql redis-server python3.11 python3-pip nginx

# Setup database
sudo -u postgres createdb quietinbox
sudo -u postgres createuser -P quietinbox

# Install app
cd /opt/quietinbox
git clone <your-repo>
cd server
pip3 install -r requirements.txt

# Configure systemd service
sudo nano /etc/systemd/system/quietinbox.service
sudo systemctl enable quietinbox
sudo systemctl start quietinbox

# Configure nginx
sudo nano /etc/nginx/sites-available/quietinbox
sudo ln -s /etc/nginx/sites-available/quietinbox /etc/nginx/sites-enabled/
sudo systemctl reload nginx

# Setup SSL
sudo certbot --nginx -d api.yourdomain.com
```

### 2. Build Android App

```bash
# Clean build
cd android
./gradlew clean

# Build release AAB
./gradlew bundleRelease

# Output location:
# app/build/outputs/bundle/release/app-release.aab
```

### 3. Upload to Play Store

1. Open Google Play Console
2. Select your app
3. Go to Release > Production
4. Click "Create new release"
5. Upload AAB file
6. Fill in release notes
7. Review and roll out

### 4. Post-Deployment

- [ ] Test production app thoroughly
- [ ] Monitor crash reports
- [ ] Check AdMob earnings dashboard
- [ ] Monitor server logs
- [ ] Set up monitoring/alerts (optional: Sentry, Firebase)

## Production URLs

- **Backend API**: https://api.yourdomain.com
- **API Docs**: https://api.yourdomain.com/docs
- **Health Check**: https://api.yourdomain.com/health
- **Play Store**: https://play.google.com/store/apps/details?id=com.quietinbox

## Monitoring

### Backend Monitoring

```bash
# View logs
docker compose logs -f api

# Check database connections
docker compose exec postgres psql -U quietinbox -c "SELECT count(*) FROM pg_stat_activity;"

# Redis status
docker compose exec redis redis-cli ping
```

### App Monitoring

- **Google Play Console**: Crashes & ANRs
- **AdMob**: Ad performance, revenue
- **Play Billing**: Subscription metrics

## Backup & Recovery

### Database Backup

```bash
# Backup
docker compose exec postgres pg_dump -U quietinbox quietinbox > backup.sql

# Restore
docker compose exec -T postgres psql -U quietinbox quietinbox < backup.sql
```

### Application Backup

```bash
# Backup entire application
tar -czf quietinbox-backup-$(date +%Y%m%d).tar.gz /opt/quietinbox

# Store offsite (S3, etc.)
aws s3 cp quietinbox-backup-*.tar.gz s3://your-bucket/backups/
```

## Rollback Plan

If something goes wrong:

### Backend Rollback

```bash
# Rollback to previous version
git checkout <previous-commit>
docker compose up -d --build
```

### App Rollback

1. Go to Play Console
2. Release > Production
3. Create new release with previous APK
4. Submit for review

## Support & Maintenance

### Regular Tasks

- [ ] Weekly: Check error logs
- [ ] Weekly: Review AdMob performance
- [ ] Monthly: Database backup
- [ ] Monthly: Security updates
- [ ] Quarterly: Feature updates

### Update Procedure

1. Test updates in staging
2. Update backend first
3. Submit app update
4. Monitor for issues
5. Rollback if needed

## Emergency Contacts

- **Backend Issues**: [Your email]
- **Play Store Issues**: [Your email]
- **AdMob Support**: admob-support@google.com
- **Billing Support**: [Your email]

---

**Deployment Date**: ___________
**Deployed By**: ___________
**Version**: ___________
**Notes**: ___________
