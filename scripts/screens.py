#!/usr/bin/env python3
"""
Walks through the app on an emulator and saves a screenshot of every screen.
Used by CI (commit message containing [screens]). Also a handy manual test:
  1. start an emulator   2. ./gradlew assembleDebug   3. python3 scripts/screens.py
It installs the fake "Flash Torch Pro" app halfway through to trigger a real incident.
"""
import os
import re
import subprocess
import time

PKG = "com.khalilknaissi.can_my_phone_be_trusted"
THREAT = "com.example.flashtorch"
OUT = "shots"
os.makedirs(OUT, exist_ok=True)


def adb(*args):
    return subprocess.run(["adb", *args], capture_output=True, text=True).stdout


def dump():
    xml = subprocess.run(["adb", "exec-out", "uiautomator", "dump", "/dev/tty"],
                         capture_output=True, text=True).stdout
    return xml


def find(text, xml, exact):
    for node in re.findall(r"<node [^>]*>", xml):
        t = re.search(r' text="([^"]*)"', node)
        d = re.search(r' content-desc="([^"]*)"', node)
        values = [v.group(1) for v in (t, d) if v]
        hit = any(v == text for v in values) if exact else any(text.lower() in v.lower() for v in values)
        if hit:
            b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
            if b:
                x1, y1, x2, y2 = map(int, b.groups())
                if x2 > x1 and y2 > y1:  # skip hidden views (zero size)
                    return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def tap(text, exact=True, timeout=25, name=None):
    end = time.time() + timeout
    while time.time() < end:
        xml = dump()
        pos = find(text, xml, exact)
        if PKG not in xml:  # left the app by mistake: bring it back
            adb("shell", "am", "start", "-n", f"{PKG}/.ui.MainActivity")
            time.sleep(3)
            continue
        if pos:
            adb("shell", "input", "tap", str(pos[0]), str(pos[1]))
            time.sleep(1.5)
            return True
        time.sleep(1)
    tag = name or re.sub(r"\W+", "_", text)[:30]
    open(f"{OUT}/fail_{tag}.xml", "w").write(dump())
    shot(f"fail_{tag}")
    print(f"NOT FOUND: {text}")
    return False


def tab(name):
    """Bottom bar tabs are found by id: their labels are hidden from the screen dump."""
    for _ in range(20):
        m = re.search(r'resource-id="[^"]*:id/nav_' + name + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', dump())
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
            time.sleep(1.5)
            return True
        time.sleep(1)
    print("NOT FOUND: tab", name)
    return False


def shot(name):
    time.sleep(1.2)
    png = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout
    open(f"{OUT}/{name}.png", "wb").write(png)
    print("shot", name)


def scroll_down():
    adb("shell", "input", "swipe", "540", "1700", "540", "700", "400")
    time.sleep(1)


def back():
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(1.5)


def main():
    adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk")
    adb("shell", "settings", "put", "global", "development_settings_enabled", "0")
    adb("logcat", "-c")
    adb("shell", "am", "start", "-n", f"{PKG}/.ui.MainActivity")
    time.sleep(4)

    # First launch
    shot("01_onboarding_what_it_does")
    tap("Next"); shot("02_onboarding_can_see")
    tap("Next"); shot("03_onboarding_alerts")
    tap("Turn on alerts"); shot("04_onboarding_usage")
    tap("Skip"); shot("05_onboarding_lock")
    tap("Not now"); shot("06_baseline_review")
    tap("Record baseline")
    time.sleep(0.4); shot("07_baseline_recording")
    end = time.time() + 90
    while time.time() < end and not find("Go to Status", dump(), True):
        time.sleep(1)
    shot("08_baseline_sealed")
    tap("Go to Status")
    shot("09_status_intact")

    # Something suspicious happens: a sideloaded app with SMS, microphone and accessibility, plus developer options.
    adb("install", "demo-threat/build/outputs/apk/debug/demo-threat-debug.apk")
    for perm in ("READ_SMS", "RECEIVE_SMS", "RECORD_AUDIO"):
        adb("shell", "pm", "grant", THREAT, f"android.permission.{perm}")
    adb("shell", "settings", "put", "secure", "enabled_accessibility_services", f"{THREAT}/{THREAT}.TorchService")
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
    adb("shell", "settings", "put", "global", "development_settings_enabled", "1")
    time.sleep(3)

    tap("Check"); time.sleep(6); shot("10_status_broken")
    tap("Investigate incident"); shot("11_incident_open")
    tap("Start investigating"); shot("12_incident_investigating")
    scroll_down(); shot("13_incident_steps")
    back()

    tab("changes"); shot("14_changes")
    tap("Accessibility service on", exact=False); shot("15_change_detail")
    tap("Flash Torch Pro"); shot("16_app_detail")
    back(); back()
    tab("timeline"); shot("17_timeline")
    tab("incidents"); shot("18_incidents")
    tab("reports"); shot("19_reports_empty")
    tap("New report"); shot("20_report_new")
    tap("Create report"); time.sleep(3); shot("21_report_saved")
    back()
    tab("status")

    # Contain it: uninstall the app, turn developer options off, check again, close.
    adb("uninstall", THREAT)
    adb("shell", "settings", "put", "global", "development_settings_enabled", "0")
    tap("Investigate incident")
    scroll_down()
    tap("Check again"); time.sleep(6); shot("22_incident_rechecked")
    tap("Close…"); shot("23_close_sheet")
    tap("Mark resolved"); shot("24_incident_resolved")
    back(); time.sleep(2); shot("25_status_all_clear")

    tap("Settings"); shot("26_settings")
    scroll_down(); shot("27_settings_more")
    tap("Help and about"); shot("28_help")
    back(); back()
    tap("Notifications", exact=False); shot("29_notifications")
    back()
    tab("reports"); shot("30_reports")

    open(f"{OUT}/logcat_errors.txt", "w").write(adb("logcat", "-d", "-b", "crash") + adb("logcat", "-d", "*:E"))


if __name__ == "__main__":
    main()
