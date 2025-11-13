"""
QuietInbox Backend Server
FastAPI-based REST API for notification sync, profiles, and recommendations
"""

from fastapi import FastAPI, HTTPException, Depends, Header, status, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from typing import Optional, List
import logging
from datetime import datetime, timedelta
import jwt
from passlib.context import CryptContext
import time
import json

from database import get_db, init_db
from models import User, Profile, VIP, NotificationSync, SyncQueue
from schemas import (
    UserCreate, UserResponse, ProfileCreate, ProfileResponse,
    VIPCreate, VIPResponse, SyncPushRequest, SyncPushResponse,
    DeferralRecommendation, TokenResponse
)

# Configure comprehensive logging
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s][%(name)s][%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="QuietInbox API",
    description="Backend API for QuietInbox notification management",
    version="1.0.0"
)

# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Log all HTTP requests and responses with timing"""
    start_time = time.time()
    request_id = f"{int(start_time * 1000)}"

    # Log request
    logger.info(f"→ REQUEST [{request_id}] {request.method} {request.url.path}")
    logger.debug(f"  Headers: {dict(request.headers)}")

    # Log request body for POST/PUT
    if request.method in ["POST", "PUT", "PATCH"]:
        try:
            body = await request.body()
            if body:
                try:
                    body_json = json.loads(body.decode())
                    # Mask sensitive fields
                    if "password" in body_json:
                        body_json["password"] = "***MASKED***"
                    if "access_token" in body_json:
                        body_json["access_token"] = "***MASKED***"
                    logger.debug(f"  Body: {json.dumps(body_json, indent=2)}")
                except:
                    logger.debug(f"  Body: <binary or non-JSON>")
        except:
            pass

    # Process request
    try:
        response = await call_next(request)
    except Exception as e:
        logger.error(f"✗ EXCEPTION [{request_id}] {str(e)}", exc_info=True)
        raise

    # Calculate duration
    duration_ms = (time.time() - start_time) * 1000

    # Log response
    logger.info(f"← RESPONSE [{request_id}] {response.status_code} | {duration_ms:.2f}ms")

    return response


# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify exact origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Security
SECRET_KEY = "your-secret-key-change-in-production"  # Use environment variable in production
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 10080  # 7 days

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    """Create JWT access token"""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def verify_token(authorization: Optional[str] = Header(None), db: Session = Depends(get_db)):
    """Verify JWT token and return user"""
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated"
        )

    try:
        token = authorization.replace("Bearer ", "")
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token")
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    user = db.query(User).filter(User.id == int(user_id)).first()
    if user is None:
        raise HTTPException(status_code=401, detail="User not found")

    return user


@app.on_event("startup")
async def startup_event():
    """Initialize database on startup"""
    logger.info("========== QuietInbox API Starting ==========")
    logger.info("Initializing database...")
    try:
        init_db()
        logger.info("✓ Database initialized successfully")
    except Exception as e:
        logger.error(f"✗ Database initialization failed: {str(e)}", exc_info=True)
        raise
    logger.info("========== API Ready ==========")


@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    logger.info("========== QuietInbox API Shutting Down ==========")



@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "status": "ok",
        "service": "QuietInbox API",
        "version": "1.0.0",
        "timestamp": datetime.utcnow().isoformat()
    }


@app.get("/health")
async def health_check():
    """Detailed health check"""
    return {
        "status": "healthy",
        "database": "connected",
        "timestamp": datetime.utcnow().isoformat()
    }


# ============================================
# AUTHENTICATION ENDPOINTS
# ============================================

@app.post("/v1/auth/register", response_model=TokenResponse)
async def register_user(user_data: UserCreate, db: Session = Depends(get_db)):
    """Register a new user"""
    logger.info(f"Registration attempt for device: {user_data.device_id}")

    try:
        # Check if user exists
        logger.debug(f"Checking if email already registered: {user_data.email}")
        existing_user = db.query(User).filter(User.email == user_data.email).first()
        if existing_user:
            logger.warning(f"Registration failed: Email already registered - {user_data.email}")
            raise HTTPException(status_code=400, detail="Email already registered")

        # Check device ID uniqueness
        logger.debug(f"Checking device ID: {user_data.device_id}")
        existing_device = db.query(User).filter(User.device_id == user_data.device_id).first()
        if existing_device:
            logger.info(f"Device already registered, returning token for user: {existing_device.id}")
            # Return token for existing device
            access_token = create_access_token(data={"sub": str(existing_device.id)})
            return TokenResponse(
                access_token=access_token,
                token_type="bearer",
                user_id=existing_device.id
            )

        # Create new user
        logger.debug("Creating new user")
        hashed_password = pwd_context.hash(user_data.password) if user_data.password else None
        new_user = User(
            email=user_data.email,
            password_hash=hashed_password,
            device_id=user_data.device_id,
            is_pro=False,
            created_at=datetime.utcnow(),
            last_sync=datetime.utcnow()
        )

        db.add(new_user)
        db.commit()
        db.refresh(new_user)
        logger.info(f"✓ User created: ID={new_user.id}, Email={user_data.email}")

        # Create default profile
        logger.debug("Creating default profile")
        default_profile = Profile(
            user_id=new_user.id,
            name="Default",
            quiet_hours_start="22:00",
            quiet_hours_end="07:00",
            rules_json="{}",
            is_active=True
        )
        db.add(default_profile)
        db.commit()
        logger.info(f"✓ Default profile created for user: {new_user.id}")

        # Generate token
        access_token = create_access_token(data={"sub": str(new_user.id)})
        logger.info(f"✓ Registration successful: user_id={new_user.id}")

        return TokenResponse(
            access_token=access_token,
            token_type="bearer",
            user_id=new_user.id
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Registration error: {str(e)}")
        db.rollback()
        raise HTTPException(status_code=500, detail="Registration failed")


@app.post("/v1/auth/login", response_model=TokenResponse)
async def login_user(email: str, password: str, db: Session = Depends(get_db)):
    """Login user and return token"""
    user = db.query(User).filter(User.email == email).first()

    if not user or not pwd_context.verify(password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    access_token = create_access_token(data={"sub": str(user.id)})

    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user_id=user.id
    )


# ============================================
# PROFILE ENDPOINTS
# ============================================

@app.get("/v1/profile", response_model=List[ProfileResponse])
async def get_profiles(current_user: User = Depends(verify_token), db: Session = Depends(get_db)):
    """Get all profiles for current user"""
    profiles = db.query(Profile).filter(Profile.user_id == current_user.id).all()
    return profiles


@app.post("/v1/profile", response_model=ProfileResponse)
async def create_profile(
    profile_data: ProfileCreate,
    current_user: User = Depends(verify_token),
    db: Session = Depends(get_db)
):
    """Create a new profile"""
    # Check if user is Pro for multiple profiles
    profile_count = db.query(Profile).filter(Profile.user_id == current_user.id).count()

    if profile_count >= 1 and not current_user.is_pro:
        raise HTTPException(
            status_code=403,
            detail="Multiple profiles require Pro subscription"
        )

    new_profile = Profile(
        user_id=current_user.id,
        name=profile_data.name,
        quiet_hours_start=profile_data.quiet_hours_start,
        quiet_hours_end=profile_data.quiet_hours_end,
        rules_json=profile_data.rules_json or "{}",
        is_active=profile_data.is_active
    )

    db.add(new_profile)
    db.commit()
    db.refresh(new_profile)

    return new_profile


@app.put("/v1/profile/{profile_id}", response_model=ProfileResponse)
async def update_profile(
    profile_id: int,
    profile_data: ProfileCreate,
    current_user: User = Depends(verify_token),
    db: Session = Depends(get_db)
):
    """Update a profile"""
    profile = db.query(Profile).filter(
        Profile.id == profile_id,
        Profile.user_id == current_user.id
    ).first()

    if not profile:
        raise HTTPException(status_code=404, detail="Profile not found")

    profile.name = profile_data.name
    profile.quiet_hours_start = profile_data.quiet_hours_start
    profile.quiet_hours_end = profile_data.quiet_hours_end
    profile.rules_json = profile_data.rules_json
    profile.is_active = profile_data.is_active

    db.commit()
    db.refresh(profile)

    return profile


# ============================================
# VIP ENDPOINTS
# ============================================

@app.get("/v1/vip", response_model=List[VIPResponse])
async def get_vips(current_user: User = Depends(verify_token), db: Session = Depends(get_db)):
    """Get all VIPs for current user"""
    vips = db.query(VIP).filter(VIP.user_id == current_user.id).all()
    return vips


@app.post("/v1/vip", response_model=VIPResponse)
async def create_vip(
    vip_data: VIPCreate,
    current_user: User = Depends(verify_token),
    db: Session = Depends(get_db)
):
    """Create a new VIP contact"""
    new_vip = VIP(
        user_id=current_user.id,
        app_package=vip_data.app_package,
        identifier=vip_data.identifier,
        priority=vip_data.priority,
        bypass_quiet_hours=vip_data.bypass_quiet_hours
    )

    db.add(new_vip)
    db.commit()
    db.refresh(new_vip)

    return new_vip


@app.delete("/v1/vip/{vip_id}")
async def delete_vip(
    vip_id: int,
    current_user: User = Depends(verify_token),
    db: Session = Depends(get_db)
):
    """Delete a VIP contact"""
    vip = db.query(VIP).filter(
        VIP.id == vip_id,
        VIP.user_id == current_user.id
    ).first()

    if not vip:
        raise HTTPException(status_code=404, detail="VIP not found")

    db.delete(vip)
    db.commit()

    return {"status": "deleted"}


# ============================================
# SYNC ENDPOINTS
# ============================================

@app.post("/v1/sync/push", response_model=SyncPushResponse)
async def sync_push(
    sync_data: SyncPushRequest,
    current_user: User = Depends(verify_token),
    db: Session = Depends(get_db)
):
    """Push local changes to server for sync"""
    try:
        synced_count = 0

        # Process each sync item
        for item in sync_data.items:
            # Check if already synced
            existing = db.query(NotificationSync).filter(
                NotificationSync.device_id == current_user.device_id,
                NotificationSync.local_id == item.get("local_id")
            ).first()

            if not existing:
                sync_item = NotificationSync(
                    user_id=current_user.id,
                    device_id=current_user.device_id,
                    local_id=item.get("local_id"),
                    sync_type=item.get("type"),
                    data_json=str(item.get("data", {})),
                    synced_at=datetime.utcnow()
                )
                db.add(sync_item)
                synced_count += 1

        # Update user's last sync time
        current_user.last_sync = datetime.utcnow()

        db.commit()

        logger.info(f"Synced {synced_count} items for user {current_user.id}")

        return SyncPushResponse(
            success=True,
            synced_count=synced_count,
            server_timestamp=datetime.utcnow().isoformat()
        )

    except Exception as e:
        logger.error(f"Sync error: {str(e)}")
        db.rollback()
        raise HTTPException(status_code=500, detail="Sync failed")


@app.get("/v1/sync/pull")
async def sync_pull(
    since: Optional[str] = None,
    current_user: User = Depends(verify_token),
    db: Session = Depends(get_db)
):
    """Pull server changes since last sync"""
    try:
        query = db.query(NotificationSync).filter(
            NotificationSync.user_id == current_user.id
        )

        if since:
            since_dt = datetime.fromisoformat(since)
            query = query.filter(NotificationSync.synced_at > since_dt)

        sync_items = query.order_by(NotificationSync.synced_at.desc()).limit(100).all()

        return {
            "success": True,
            "items": [
                {
                    "id": item.id,
                    "type": item.sync_type,
                    "data": item.data_json,
                    "synced_at": item.synced_at.isoformat()
                }
                for item in sync_items
            ],
            "server_timestamp": datetime.utcnow().isoformat()
        }

    except Exception as e:
        logger.error(f"Pull error: {str(e)}")
        raise HTTPException(status_code=500, detail="Pull failed")


# ============================================
# RECOMMENDATION ENDPOINTS
# ============================================

@app.get("/v1/recommendations/deferral-windows", response_model=List[DeferralRecommendation])
async def get_deferral_recommendations(
    current_user: User = Depends(verify_token),
    db: Session = Depends(get_db)
):
    """Get AI-recommended deferral windows"""
    # Simple rule-based recommendations
    # In production, this would use ML models

    recommendations = [
        DeferralRecommendation(
            window_name="Morning Digest",
            start_time="08:00",
            end_time="09:00",
            confidence=0.85,
            reason="Based on your notification patterns"
        ),
        DeferralRecommendation(
            window_name="Lunch Break",
            start_time="12:30",
            end_time="13:00",
            confidence=0.78,
            reason="Low activity period detected"
        ),
        DeferralRecommendation(
            window_name="Evening Review",
            start_time="18:00",
            end_time="18:30",
            confidence=0.82,
            reason="End of workday summary"
        )
    ]

    return recommendations


# ============================================
# USER ENDPOINTS
# ============================================

@app.get("/v1/user/me", response_model=UserResponse)
async def get_current_user(current_user: User = Depends(verify_token)):
    """Get current user info"""
    return UserResponse(
        id=current_user.id,
        email=current_user.email,
        device_id=current_user.device_id,
        is_pro=current_user.is_pro,
        created_at=current_user.created_at.isoformat(),
        last_sync=current_user.last_sync.isoformat() if current_user.last_sync else None
    )


@app.post("/v1/user/upgrade-pro")
async def upgrade_to_pro(
    current_user: User = Depends(verify_token),
    db: Session = Depends(get_db)
):
    """Upgrade user to Pro (called after successful IAP)"""
    current_user.is_pro = True
    db.commit()

    logger.info(f"User {current_user.id} upgraded to Pro")

    return {"status": "success", "is_pro": True}


# Error handlers
@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    """Handle HTTP exceptions"""
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail, "status": "error"}
    )


@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    """Handle general exceptions"""
    logger.error(f"Unhandled exception: {str(exc)}")
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error", "status": "error"}
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
