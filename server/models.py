"""
SQLAlchemy Database Models
"""

from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, ForeignKey, Float
from sqlalchemy.orm import relationship
from datetime import datetime
from database import Base


class User(Base):
    """User model"""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=True)
    password_hash = Column(String, nullable=True)
    device_id = Column(String, unique=True, index=True)
    is_pro = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    last_sync = Column(DateTime, nullable=True)

    # Relationships
    profiles = relationship("Profile", back_populates="user", cascade="all, delete-orphan")
    vips = relationship("VIP", back_populates="user", cascade="all, delete-orphan")
    syncs = relationship("NotificationSync", back_populates="user", cascade="all, delete-orphan")


class Profile(Base):
    """Profile model (Work/Personal/Travel)"""
    __tablename__ = "profiles"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    name = Column(String, nullable=False)
    quiet_hours_start = Column(String, nullable=True)  # HH:MM format
    quiet_hours_end = Column(String, nullable=True)    # HH:MM format
    rules_json = Column(Text, nullable=True)           # JSON string of rules
    is_active = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    user = relationship("User", back_populates="profiles")


class VIP(Base):
    """VIP contact model"""
    __tablename__ = "vips"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    app_package = Column(String, nullable=False)
    identifier = Column(String, nullable=False)  # phone number, email, etc.
    priority = Column(Integer, default=1)        # 1-5 priority level
    bypass_quiet_hours = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    user = relationship("User", back_populates="vips")


class NotificationSync(Base):
    """Notification sync records"""
    __tablename__ = "notification_syncs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    device_id = Column(String, nullable=False)
    local_id = Column(String, nullable=False)     # Local notification ID
    sync_type = Column(String, nullable=False)     # profile, vip, notification, etc.
    data_json = Column(Text, nullable=False)       # JSON data
    synced_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    user = relationship("User", back_populates="syncs")


class SyncQueue(Base):
    """Queue for pending sync operations"""
    __tablename__ = "sync_queue"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    operation = Column(String, nullable=False)     # create, update, delete
    entity_type = Column(String, nullable=False)   # profile, vip, etc.
    entity_id = Column(String, nullable=False)
    data_json = Column(Text, nullable=True)
    status = Column(String, default="pending")     # pending, processing, completed, failed
    created_at = Column(DateTime, default=datetime.utcnow)
    processed_at = Column(DateTime, nullable=True)
