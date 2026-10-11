"""Static contract QA for native OnePlus 6 HDR format policy switch.

NOT an Android Settings APK build or on-device verification.
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
CTRL = ROOT / "src/com/android/settings/display/Op6HdrContentPreferenceController.java"
XML = ROOT / "res/xml/display_settings.xml"
EN = ROOT / "res/values/custom_strings.xml"
ZH = ROOT / "res/values-zh-rCN/uwu_strings.xml"

class HdrControlTest(unittest.TestCase):
    def test_android_real_hdr_policy_api(self):
        s = CTRL.read_text()
        for expected in (
                "getSupportedColorModes()", "getSupportedHdrTypes()",
                "getUserPreferredHdrMode(", "setUserPreferredHdrMode(",
                "HDR_PREFERENCE_SDR_ONLY", "HDR_PREFERENCE_HDR_ALLOWED",
                "OnePlus6", "enchilada"):
            self.assertIn(expected, s, expected)
        for banned in ("setprop ", "wm density", "HDR_CONVERSION_FORCE",
                       "setHdrConversionMode(", "ro.surface_flinger.has_HDR_display"):
            self.assertNotIn(banned, s)

    def test_settings_xml_and_translations(self):
        ns = {"a": "http://schemas.android.com/apk/res/android",
              "s": "http://schemas.android.com/apk/res-auto"}
        root = ET.parse(XML).getroot()
        matches = [el for el in root.iter()
                   if el.get("{%s}key" % ns["a"]) == "op6_hdr_content"]
        self.assertEqual(len(matches), 1)
        el = matches[0]
        self.assertEqual(el.get("{%s}controller" % ns["s"]),
                         "com.android.settings.display.Op6HdrContentPreferenceController")
        for p in (EN, ZH):
            names = {el.attrib.get("name") for el in ET.parse(p).getroot().iter()}
            self.assertIn("op6_hdr_content_title", names)
            self.assertIn("op6_hdr_content_summary", names)

if __name__ == "__main__":
    unittest.main()
