"""
Pydantic Schemas for request/response validation
"""

from pydantic import BaseModel, EmailStr
from typing import Optional, List, Dict, Any
from datetime import datetime


# ============================================
# USER SCHEMAS
# ============================================

class UserCreate(BaseModel):
    """Schema for user registration"""
    email: Optional[EmailStr] = None
    password: Optional[str] = None
    device_id: str


class UserResponse(BaseModel):
    """Schema for user response"""
    id: int
    email: Optional[str]
    device_id: str
    is_pro: bool
    created_at: str
    last_sync: Optional[str]

    class Config:
        from_attributes = True


class TokenResponse(BaseModel):
    """Schema for authentication token response"""
    access_token: str
    token_type: str
    user_id: int


# ============================================
# PROFILE SCHEMAS
# ============================================

class ProfileCreate(BaseModel):
    """Schema for profile creation"""
    name: str
    quiet_hours_start: Optional[str] = "22:00"
    quiet_hours_end: Optional[str] = "07:00"
    rules_json: Optional[str] = "{}"
    is_active: bool = False


class ProfileResponse(BaseModel):
    """Schema for profile response"""
    id: int
    user_id: int
    name: str
    quiet_hours_start: Optional[str]
    quiet_hours_end: Optional[str]
    rules_json: Optional[str]
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True


# ============================================
# VIP SCHEMAS
# ============================================

class VIPCreate(BaseModel):
    """Schema for VIP creation"""
    app_package: str
    identifier: str
    priority: int = 1
    bypass_quiet_hours: bool = True


class VIPResponse(BaseModel):
    """Schema for VIP response"""
    id: int
    user_id: int
    app_package: str
    identifier: str
    priority: int
    bypass_quiet_hours: bool
    created_at: datetime

    class Config:
        from_attributes = True


# ============================================
# SYNC SCHEMAS
# ============================================

class SyncPushRequest(BaseModel):
    """Schema for sync push request"""
    items: List[Dict[str, Any]]
    device_timestamp: str


class SyncPushResponse(BaseModel):
    """Schema for sync push response"""
    success: bool
    synced_count: int
    server_timestamp: str


# ============================================
# RECOMMENDATION SCHEMAS
# ============================================

class DeferralRecommendation(BaseModel):
    """Schema for deferral window recommendation"""
    window_name: str
    start_time: str
    end_time: str
    confidence: float
    reason: str
