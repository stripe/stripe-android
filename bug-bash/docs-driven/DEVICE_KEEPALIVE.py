import subprocess
import time

DEVICE = "2B211JEGR05252"
try:
    while True:
        result = subprocess.run(["adb", "-s", DEVICE, "shell", "input", "keyevent", "KEYCODE_WAKEUP"], capture_output=True, text=True, timeout=20)
        subprocess.run(["adb", "-s", DEVICE, "shell", "input", "keyevent", "KEYCODE_UNKNOWN"], capture_output=True, text=True, timeout=20)
        print(time.strftime("%Y-%m-%d %H:%M:%S"), "wake interaction", result.returncode, flush=True)
        time.sleep(90)
except KeyboardInterrupt:
    print("Keepalive stopped", flush=True)
