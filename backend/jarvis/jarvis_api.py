"""
FastAPI Backend Server for PiConsole Android App
Integrates with Jarvis voice assistant system
"""

import os
import sys
import time
import socket
import psutil
import subprocess
import logging
from datetime import datetime, timedelta
from typing import Optional, Dict, List
from threading import Thread

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import uvicorn

from config import logger, TIMEZONE
from music_bot import play_music, stop_music, is_playing
from ha_bridge import control_home_assistant

# ============================================================================
# SETUP
# ============================================================================

app = FastAPI(title="Jarvis PiConsole Backend", version="1.0.0")

# Store timers and alarms in memory
active_timers: Dict[str, Dict] = {}
active_alarms: Dict[str, Dict] = {}
jarvis_process: Optional[subprocess.Popen] = None

logger.info("PiConsole Backend initialized")


# ============================================================================
# PYDANTIC MODELS
# ============================================================================

class StatusResponse(BaseModel):
    """Device status information"""
    deviceName: str
    ipAddress: str
    isOnline: bool
    uptime: int
    cpuUsage: float
    ramUsage: float
    temperature: float
    diskUsage: float


class TimerRequest(BaseModel):
    """Request to create a timer"""
    label: str
    duration: int  # seconds


class AlarmRequest(BaseModel):
    """Request to create an alarm"""
    label: str
    time: str  # HH:MM format


class DeviceControlRequest(BaseModel):
    """Request to control smart home device"""
    device_type: str  # "fan", "light"
    device_name: str
    action: str  # "on", "off", "speed 1-6", etc.


class MusicRequest(BaseModel):
    """Request to play/stop music"""
    query: Optional[str] = None  # Song name (for play), None for stop


class CommandRequest(BaseModel):
    """Request to send voice command to Jarvis"""
    command: str


# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================

def get_device_status() -> Dict:
    """
    Get current device status including CPU, RAM, temp, etc.
    
    Returns:
        Dictionary with status information
    """
    try:
        # Get IP address
        try:
            ip = socket.gethostbyname(socket.gethostname())
        except Exception:
            ip = "127.0.0.1"
        
        # Get uptime
        uptime = int(time.time() - psutil.boot_time())
        
        # Get CPU and memory usage
        cpu_usage = psutil.cpu_percent(interval=1)
        memory = psutil.virtual_memory()
        ram_usage = memory.percent
        
        # Get temperature (Raspberry Pi specific)
        temperature = get_cpu_temperature()
        
        # Get disk usage
        disk = psutil.disk_usage('/')
        disk_usage = disk.percent
        
        return {
            "deviceName": socket.gethostname(),
            "ipAddress": ip,
            "isOnline": True,
            "uptime": uptime,
            "cpuUsage": cpu_usage,
            "ramUsage": ram_usage,
            "temperature": temperature,
            "diskUsage": disk_usage
        }
    except Exception as e:
        logger.error(f"Error getting device status: {e}")
        raise


def get_cpu_temperature() -> float:
    """
    Get Raspberry Pi CPU temperature.
    
    Returns:
        Temperature in Celsius, or 0.0 if unavailable
    """
    try:
        result = subprocess.run(
            ["vcgencmd", "measure_temp"],
            capture_output=True,
            text=True,
            timeout=5
        )
        if result.returncode == 0:
            # Output format: "temp=45.5'C"
            temp_str = result.stdout.split("=")[1].split("'")[0]
            return float(temp_str)
    except Exception as e:
        logger.debug(f"Could not read CPU temperature: {e}")
    
    return 0.0


def timer_countdown(timer_id: str, duration: int, label: str) -> None:
    """
    Run a timer in the background.
    
    Args:
        timer_id: Unique timer identifier
        duration: Timer duration in seconds
        label: Timer label
    """
    try:
        time.sleep(duration)
        
        if timer_id in active_timers:
            del active_timers[timer_id]
            logger.info(f"Timer '{label}' completed")
            # Could play sound or notify here
    except Exception as e:
        logger.error(f"Timer error: {e}")


def alarm_countdown(alarm_id: str, target_time: str, label: str) -> None:
    """
    Run an alarm in the background.
    
    Args:
        alarm_id: Unique alarm identifier
        target_time: Alarm time in HH:MM format
        label: Alarm label
    """
    try:
        while alarm_id in active_alarms:
            now = datetime.now().strftime("%H:%M")
            
            if now == target_time:
                logger.info(f"Alarm '{label}' triggered")
                del active_alarms[alarm_id]
                # Could play sound or trigger action here
                break
            
            time.sleep(60)  # Check every minute
    except Exception as e:
        logger.error(f"Alarm error: {e}")


# ============================================================================
# ENDPOINTS - STATUS
# ============================================================================

@app.get("/status", response_model=StatusResponse)
def get_status():
    """
    Get current device status.
    
    Returns:
        StatusResponse with device information
    """
    try:
        return get_device_status()
    except Exception as e:
        logger.error(f"Status endpoint error: {e}")
        raise HTTPException(status_code=500, detail="Failed to get device status")


@app.get("/health")
def health_check():
    """Simple health check endpoint."""
    return {"status": "healthy"}


# ============================================================================
# ENDPOINTS - DEVICE CONTROL
# ============================================================================

@app.post("/device/control")
def control_device(request: DeviceControlRequest):
    """
    Control a smart home device (fan, light, etc.).
    
    Args:
        request: DeviceControlRequest with device info and action
        
    Returns:
        Status of the control action
    """
    try:
        result = control_home_assistant(
            request.device_type,
            request.device_name,
            request.action
        )
        return {"status": "success", "message": result}
    except Exception as e:
        logger.error(f"Device control error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/reboot")
def reboot_device():
    """
    Reboot the Raspberry Pi.
    
    Returns:
        Reboot status
    """
    try:
        logger.warning("Reboot requested via API")
        subprocess.Popen(["sudo", "reboot"])
        return {"status": "rebooting"}
    except Exception as e:
        logger.error(f"Reboot error: {e}")
        raise HTTPException(status_code=500, detail="Failed to reboot")


@app.post("/shutdown")
def shutdown_device():
    """
    Shutdown the Raspberry Pi.
    
    Returns:
        Shutdown status
    """
    try:
        logger.warning("Shutdown requested via API")
        subprocess.Popen(["sudo", "shutdown", "-h", "now"])
        return {"status": "shutting down"}
    except Exception as e:
        logger.error(f"Shutdown error: {e}")
        raise HTTPException(status_code=500, detail="Failed to shutdown")


# ============================================================================
# ENDPOINTS - TIMERS
# ============================================================================

@app.post("/timer/create")
def create_timer(request: TimerRequest, background_tasks: BackgroundTasks):
    """
    Create a new timer.
    
    Args:
        request: TimerRequest with label and duration
        background_tasks: FastAPI background tasks
        
    Returns:
        Timer information including ID
    """
    try:
        timer_id = f"timer_{int(time.time() * 1000)}"
        
        active_timers[timer_id] = {
            "label": request.label,
            "duration": request.duration,
            "created": time.time(),
            "remaining": request.duration
        }
        
        background_tasks.add_task(timer_countdown, timer_id, request.duration, request.label)
        
        logger.info(f"Timer created: {request.label} ({request.duration}s)")
        
        return {
            "status": "created",
            "timer_id": timer_id,
            "label": request.label,
            "duration": request.duration
        }
    except Exception as e:
        logger.error(f"Timer creation error: {e}")
        raise HTTPException(status_code=500, detail="Failed to create timer")


@app.get("/timer/list")
def list_timers():
    """
    Get list of active timers.
    
    Returns:
        List of active timers
    """
    try:
        timers = []
        current_time = time.time()
        
        for timer_id, timer_data in active_timers.items():
            elapsed = current_time - timer_data["created"]
            remaining = max(0, timer_data["duration"] - elapsed)
            
            timers.append({
                "timer_id": timer_id,
                "label": timer_data["label"],
                "remaining": int(remaining),
                "duration": timer_data["duration"]
            })
        
        return {"timers": timers, "count": len(timers)}
    except Exception as e:
        logger.error(f"List timers error: {e}")
        raise HTTPException(status_code=500, detail="Failed to list timers")


@app.delete("/timer/{timer_id}")
def cancel_timer(timer_id: str):
    """
    Cancel an active timer.
    
    Args:
        timer_id: ID of timer to cancel
        
    Returns:
        Cancellation status
    """
    try:
        if timer_id in active_timers:
            label = active_timers[timer_id]["label"]
            del active_timers[timer_id]
            logger.info(f"Timer cancelled: {label}")
            return {"status": "cancelled", "timer_id": timer_id}
        else:
            raise HTTPException(status_code=404, detail="Timer not found")
    except Exception as e:
        logger.error(f"Timer cancellation error: {e}")
        raise HTTPException(status_code=500, detail="Failed to cancel timer")


# ============================================================================
# ENDPOINTS - ALARMS
# ============================================================================

@app.post("/alarm/create")
def create_alarm(request: AlarmRequest, background_tasks: BackgroundTasks):
    """
    Create a new alarm.
    
    Args:
        request: AlarmRequest with label and time (HH:MM)
        background_tasks: FastAPI background tasks
        
    Returns:
        Alarm information including ID
    """
    try:
        alarm_id = f"alarm_{int(time.time() * 1000)}"
        
        active_alarms[alarm_id] = {
            "label": request.label,
            "time": request.time,
            "created": time.time()
        }
        
        background_tasks.add_task(alarm_countdown, alarm_id, request.time, request.label)
        
        logger.info(f"Alarm created: {request.label} at {request.time}")
        
        return {
            "status": "created",
            "alarm_id": alarm_id,
            "label": request.label,
            "time": request.time
        }
    except Exception as e:
        logger.error(f"Alarm creation error: {e}")
        raise HTTPException(status_code=500, detail="Failed to create alarm")


@app.get("/alarm/list")
def list_alarms():
    """
    Get list of active alarms.
    
    Returns:
        List of active alarms
    """
    try:
        alarms = []
        
        for alarm_id, alarm_data in active_alarms.items():
            alarms.append({
                "alarm_id": alarm_id,
                "label": alarm_data["label"],
                "time": alarm_data["time"]
            })
        
        return {"alarms": alarms, "count": len(alarms)}
    except Exception as e:
        logger.error(f"List alarms error: {e}")
        raise HTTPException(status_code=500, detail="Failed to list alarms")


@app.delete("/alarm/{alarm_id}")
def cancel_alarm(alarm_id: str):
    """
    Cancel an active alarm.
    
    Args:
        alarm_id: ID of alarm to cancel
        
    Returns:
        Cancellation status
    """
    try:
        if alarm_id in active_alarms:
            label = active_alarms[alarm_id]["label"]
            del active_alarms[alarm_id]
            logger.info(f"Alarm cancelled: {label}")
            return {"status": "cancelled", "alarm_id": alarm_id}
        else:
            raise HTTPException(status_code=404, detail="Alarm not found")
    except Exception as e:
        logger.error(f"Alarm cancellation error: {e}")
        raise HTTPException(status_code=500, detail="Failed to cancel alarm")


# ============================================================================
# ENDPOINTS - MUSIC
# ============================================================================

@app.post("/music/play")
def play_song(request: MusicRequest):
    """
    Play music from YouTube.
    
    Args:
        request: MusicRequest with song query
        
    Returns:
        Playback status
    """
    try:
        if not request.query:
            raise HTTPException(status_code=400, detail="Query required")
        
        result = play_music(request.query)
        logger.info(f"Music playback started: {request.query}")
        
        return {"status": "playing", "query": request.query, "message": result}
    except Exception as e:
        logger.error(f"Music play error: {e}")
        raise HTTPException(status_code=500, detail="Failed to play music")


@app.post("/music/stop")
def stop_song():
    """
    Stop current music playback.
    
    Returns:
        Stop status
    """
    try:
        result = stop_music()
        logger.info("Music playback stopped")
        
        return {"status": "stopped", "message": result}
    except Exception as e:
        logger.error(f"Music stop error: {e}")
        raise HTTPException(status_code=500, detail="Failed to stop music")


@app.get("/music/status")
def music_status():
    """
    Get current music playback status.
    
    Returns:
        Playing status
    """
    try:
        playing = is_playing()
        
        return {"isPlaying": playing, "status": "playing" if playing else "stopped"}
    except Exception as e:
        logger.error(f"Music status error: {e}")
        raise HTTPException(status_code=500, detail="Failed to get music status")


# ============================================================================
# ENDPOINTS - JARVIS COMMANDS
# ============================================================================

@app.post("/jarvis/command")
def send_command(request: CommandRequest):
    """
    Send a voice command to Jarvis.
    Note: This would require integration with the running Jarvis process.
    
    Args:
        request: CommandRequest with voice command
        
    Returns:
        Command execution status
    """
    try:
        # This is a placeholder - actual implementation depends on
        # how Jarvis is structured for IPC
        logger.info(f"Voice command received: {request.command}")
        
        return {
            "status": "received",
            "command": request.command,
            "message": "Command sent to Jarvis"
        }
    except Exception as e:
        logger.error(f"Jarvis command error: {e}")
        raise HTTPException(status_code=500, detail="Failed to send command")


# ============================================================================
# STARTUP AND SHUTDOWN
# ============================================================================

@app.on_event("startup")
async def startup_event():
    """Initialize on server startup."""
    logger.info("PiConsole API Server starting...")


@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on server shutdown."""
    logger.info("PiConsole API Server shutting down...")
    # Could add cleanup logic here


# ============================================================================
# RUN SERVER
# ============================================================================

if __name__ == "__main__":
    # Get config from environment or use defaults
    host = os.getenv("API_HOST", "0.0.0.0")
    port = int(os.getenv("API_PORT", "8000"))
    reload = os.getenv("API_RELOAD", "false").lower() == "true"
    
    logger.info(f"Starting FastAPI server on {host}:{port}")
    
    uvicorn.run(
        "main:app",
        host=host,
        port=port,
        reload=reload,
        log_level="info"
    )
