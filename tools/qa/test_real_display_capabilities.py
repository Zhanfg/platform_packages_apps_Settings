"""Source-level protection for real HDR and refresh-mode detection.

These tests cannot prove what the device HAL actually reports or compile
Android Java/Kotlin. Run a full ROM build and the device capability probe.
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
D = ROOT / "src/com/android/settings/display"


class RealDisplayModesTest(unittest.TestCase):
    def test_refresh_uses_modes_of_current_resolution_only(self):
        s = (D / "RefreshRateUtils.java").read_text()
        self.assertIn("display.getSupportedModes()", s)
        self.assertIn("mode.getPhysicalWidth() == activeMode.getPhysicalWidth()", s)
        self.assertIn("mode.getPhysicalHeight() == activeMode.getPhysicalHeight()", s)
        self.assertIn("mRefreshRates.size() > 1", s)
        self.assertIn("if (!isVrrPossible())", s)
        self.assertIn("if (!mRefreshRates.contains(refreshRate))", s)
        self.assertNotIn("return getCurrentRefreshRate() > DEFAULT_REFRESH_RATE;", s)

    def test_real_low_refresh_modes_not_discarded(self):
        s = (D / "RefreshRateSettings.java").read_text()
        self.assertNotIn(".filter(r -> r >= RefreshRateUtils.DEFAULT_REFRESH_RATE)", s)

    def test_hdr_info_uses_hal_not_fake_flag(self):
        s = (D / "DisplayCapabilitiesPreferenceController.java").read_text()
        self.assertIn("getHdrCapabilities()", s)
        self.assertIn("getSupportedHdrTypes()", s)
        self.assertIn("manager.getDisplay(Display.DEFAULT_DISPLAY)", s)
        self.assertIn("HDR_TYPE_HDR10", s)
        self.assertIn("HDR_TYPE_HLG", s)
        self.assertNotIn("Settings.System.put", s)
        self.assertNotIn("setRefreshRate(", s)

    def test_hdr_brightness_is_not_force_enabled(self):
        s = (D / "HdrBrightnessUtils.java").read_text()
        self.assertIn("display.isHdr() && display.isHdrSdrRatioAvailable()", s)

    def test_settings_xml_and_localization(self):
        screen = ET.parse(ROOT / "res/xml/display_settings.xml").getroot()
        items = [x for x in screen.iter()
                 if x.attrib.get("{http://schemas.android.com/apk/res/android}key")
                 == "display_capabilities"]
        self.assertEqual(len(items), 1)
        for path in ("res/values/custom_strings.xml",
                     "res/values-zh-rCN/uwu_strings.xml"):
            resources = ET.parse(ROOT / path).getroot()
            keys = {x.attrib.get("name") for x in resources}
            for name in ("display_capabilities_title",
                         "display_capabilities_summary",
                         "display_capabilities_hdr_unreported",
                         "display_capabilities_unavailable"):
                self.assertIn(name, keys)

if __name__ == "__main__":
    unittest.main()
