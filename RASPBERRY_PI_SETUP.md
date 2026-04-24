# Raspberry Pi Setup Guide for PiConsole

To make the PiConsole Android app work, you need a backend server running on your Raspberry Pi. This guide will help you set up a Python FastAPI server that handles all the requests.

## 1. Prerequisites
Ensure your Raspberry Pi has Python installed. You can check by running:
```bash
python3 --version
```

## 2. Install Required Packages
Run the following commands on your Raspberry Pi:
```bash
pip3 install fastapi uvicorn psutil
```

## 3. Create the Backend Server
Create a file named `main.py` on your Pi:

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import psutil
import time
import socket
import os

app = FastAPI()

# --- Models ---
class TimerRequest(BaseModel):
    label: str
    duration: int

class AlarmRequest(BaseModel):
    label: str
    time: str

# --- Endpoints ---

@app.get("/status")
def get_status():
    return {
        "deviceName": socket.gethostname(),
        "ipAddress": socket.gethostbyname(socket.gethostname()),
        "isOnline": True,
        "uptime": str(time.time() - psutil.boot_time()), # Simple uptime calculation
        "cpuUsage": psutil.cpu_percent(),
        "ramUsage": psutil.virtual_memory().percent,
        "temperature": 45.5 # Example temp, use 'vcgencmd measure_temp' for real data
    }

@app.post("/find-phone")
def find_phone():
    # Implement logic to ring phone (e.g., via Firebase or local notification)
    return {"status": "ringing"}

@app.post("/reboot")
def reboot():
    os.system("sudo reboot")
    return {"status": "rebooting"}

@app.post("/shutdown")
def shutdown():
    os.system("sudo shutdown -h now")
    return {"status": "shutting down"}

# Add other endpoints for Timers, Alarms, and Media as defined in ApiService.kt

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

## 4. Run the Server
Start the server with:
```bash
python3 main.py
```

## 5. Configure the Android App
1. Find your Raspberry Pi's IP address (e.g., `192.168.1.10`).
2. Open `RetrofitClient.kt` in the Android project.
3. Change `BASE_URL` to point to your Pi:
   ```kotlin
   private const val BASE_URL = "http://192.168.1.10:8000/"
   ```
4. Build and run the app!

## 6. Security Note
Make sure your Raspberry Pi and Android phone are on the same local network. If you want to access it from outside, you'll need a VPN or port forwarding (not recommended for beginners due to security risks).
