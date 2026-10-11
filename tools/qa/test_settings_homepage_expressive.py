"""Static XML QA for Settings homepage motion and spacing.

Source checks only; Android 17 Settings APK build and device UI QA are separate.
"""
from pathlib import Path
import unittest
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
A = "{http://schemas.android.com/apk/res/android}"


class SettingsHomepageTests(unittest.TestCase):
    def test_homepage_has_no_independent_legacy_list_animation(self):
        root = ET.parse(ROOT / "res/layout/settings_homepage.xml").getroot()
        card = next(v for v in root.iter() if v.get(A + "id") == "@+id/card_container")
        self.assertNotIn(A + "layoutAnimation", card.attrib)

    def test_homepage_container_avoids_hidden_offset_and_implicit_transition(self):
        root = ET.parse(ROOT / "res/layout/settings_homepage_container.xml").getroot()
        homepage = next(v for v in root.iter() if v.get(A + "id") == "@+id/homepage_container")
        content = next(v for v in root.iter() if v.get(A + "id") == "@+id/main_content")
        self.assertNotIn(A + "layout_marginTop", homepage.attrib)
        self.assertNotIn(A + "animateLayoutChanges", content.attrib)

    def test_search_title_padding_nonnegative(self):
        root = ET.parse(ROOT / "res/values/dimens.xml").getroot()
        item = next(v for v in root if v.get("name") == "search_bar_title_padding_start")
        self.assertEqual(item.text.strip(), "0dp")


if __name__ == "__main__":
    unittest.main()
