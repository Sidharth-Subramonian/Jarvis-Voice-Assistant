import os
import re
import requests
import logging
from typing import Dict, Optional
from dotenv import load_dotenv

from config import HA_URL, HA_TOKEN, ATOMBERG_SPEEDS, ENTITY_MAP, logger

load_dotenv()


def _validate_inputs(device_type: str, device_name: str, action: str) -> bool:
    """
    Validate input parameters.
    
    Args:
        device_type: Type of device (light, fan, etc.)
        device_name: Name of the device
        action: Action to perform (on, off, speed, etc.)
        
    Returns:
        True if inputs are valid
    """
    if not device_type or not isinstance(device_type, str):
        logger.warning("Invalid device_type provided")
        return False
    
    if not device_name or not isinstance(device_name, str):
        logger.warning("Invalid device_name provided")
        return False
    
    if not action or not isinstance(action, str):
        logger.warning("Invalid action provided")
        return False
    
    return True


def _resolve_entity(device_type: str, device_name: str) -> tuple[str, str]:
    """
    Resolve device name to Home Assistant entity ID and domain.
    
    Args:
        device_type: Type of device
        device_name: Name of the device
        
    Returns:
        Tuple of (entity_id, domain)
    """
    name_clean = device_name.lower().strip()
    
    # Smart entity mapping
    if any(x in name_clean for x in ["led", "light"]):
        return "light.sidhu_fan_led", "light"
    elif "sidhu" in name_clean or "fan" in name_clean:
        return "fan.sidhu_fan", "fan"
    else:
        # Fallback for future devices
        domain = device_type.lower() if device_type else "homeassistant"
        entity_id = f"{domain}.{name_clean.replace(' ', '_')}"
        return entity_id, domain


def _parse_action(action: str) -> tuple[Optional[str], Optional[int]]:
    """
    Parse action string to determine operation and value.
    
    Args:
        action: Action string (e.g., "on", "off", "speed 3", "60%")
        
    Returns:
        Tuple of (operation, value) where value is optional
    """
    action_clean = action.lower().strip()
    
    # Extract numeric values
    digits = re.findall(r'\d+', action_clean)
    
    if "off" in action_clean or (digits and digits[0] == "0"):
        return "off", None
    elif "on" in action_clean or action_clean in ["1", "true", "yes"]:
        return "on", None
    elif digits:
        return "set_speed", int(digits[0])
    else:
        return None, None


def control_home_assistant(device_type: str, device_name: str, action: str) -> str:
    """
    Control Home Assistant devices (fans, lights, etc.) via REST API.
    
    Args:
        device_type: Type of device ("light", "fan", etc.)
        device_name: Friendly name of the device
        action: Action to perform ("on", "off", speed level, percentage, etc.)
        
    Returns:
        Status message confirming the action or describing any errors
    """
    # Validate environment
    if not HA_URL or not HA_TOKEN:
        logger.error("Home Assistant credentials not configured")
        return "Home Assistant is not configured, sir."
    
    # Validate inputs
    if not _validate_inputs(device_type, device_name, action):
        return "Invalid parameters provided, sir."
    
    # Resolve entity
    entity_id, domain = _resolve_entity(device_type, device_name)
    logger.info(f"Resolved {device_name} to {entity_id} (domain: {domain})")
    
    # Parse action
    operation, value = _parse_action(action)
    
    if not operation:
        logger.warning(f"Could not parse action: {action}")
        return "I didn't understand that command, sir."
    
    # Build request
    headers = {
        "Authorization": f"Bearer {HA_TOKEN}",
        "Content-Type": "application/json",
    }
    
    try:
        if operation == "off":
            endpoint = f"{HA_URL}/services/{domain}/turn_off"
            payload = {"entity_id": entity_id}
            message = f"Done, sir. The {device_name} is now OFF."
        
        elif operation == "on":
            endpoint = f"{HA_URL}/services/{domain}/turn_on"
            payload = {"entity_id": entity_id}
            message = f"Done, sir. The {device_name} is now ON."
        
        elif operation == "set_speed" and domain == "fan":
            # Use Atomberg speed mapping if available
            percentage = ATOMBERG_SPEEDS.get(str(value), value)
            percentage = min(max(percentage, 0), 100)
            
            endpoint = f"{HA_URL}/services/fan/set_percentage"
            payload = {"entity_id": entity_id, "percentage": percentage}
            message = f"Done, sir. {device_name} set to level {value} ({percentage}%)."
        
        elif operation == "set_speed" and domain == "light":
            # For lights, treat as brightness
            brightness = min(max(value, 0), 100)
            
            endpoint = f"{HA_URL}/services/light/turn_on"
            payload = {
                "entity_id": entity_id,
                "brightness": int(brightness * 2.55)  # Convert 0-100 to 0-255
            }
            message = f"Done, sir. {device_name} brightness set to {brightness}%."
        
        else:
            logger.warning(f"Unsupported operation: {operation} for domain {domain}")
            return f"I can't do that with {device_name}, sir."
        
        logger.info(f"HA Request: {endpoint} | Payload: {payload}")
        response = requests.post(endpoint, headers=headers, json=payload, timeout=5)
        
        if response.status_code in [200, 201]:
            logger.info(f"Success: {message}")
            return message
        else:
            error_msg = f"System Error: {response.status_code}"
            logger.error(error_msg)
            return error_msg
            
    except requests.exceptions.Timeout:
        logger.error("Home Assistant request timed out")
        return "Home Assistant is not responding, sir."
    except requests.exceptions.ConnectionError:
        logger.error("Failed to connect to Home Assistant")
        return "I cannot connect to Home Assistant, sir."
    except Exception as e:
        logger.error(f"Unexpected error controlling Home Assistant: {e}")
        return f"An error occurred: {str(e)}"
