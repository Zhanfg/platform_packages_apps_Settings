"""AOSP Settings functional-entry restoration; source checks only."""
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
class MissingAospEntryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.config={x.get("name"):x.text.strip() for x in ET.parse(ROOT/"res/values/config.xml").getroot() if x.get("name")}
    def test_aosp_storage_manager_intent_restored(self):
        self.assertEqual(self.config["config_storage_manager_settings_enabled"],"true")
        manifest=(ROOT/"AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:exported="@bool/config_storage_manager_settings_enabled"',manifest)
        self.assertIn('android.settings.STORAGE_MANAGER_SETTINGS',manifest)
        self.assertTrue((ROOT/"src/com/android/settings/deletionhelper/AutomaticStorageManagerSettings.java").exists())
    def test_memory_detail_requires_developer_options(self):
        self.assertEqual(self.config["config_show_app_info_settings_memory"],"true")
        src=(ROOT/"src/com/android/settings/applications/appinfo/AppMemoryPreferenceController.java").read_text()
        self.assertIn("DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled",src)
        self.assertIn("mStatsManager.refreshStats",src)
        self.assertIn("no_memory_use_summary",src)
    def test_no_fabricated_oneplus_driver_capability(self):
        config=(ROOT/"res/values/config.xml").read_text()
        self.assertIn('<bool name="config_enableColorTemperature">false</bool>',config)
        self.assertIn('<bool name="has_dock_settings">false</bool>',config)
if __name__=="__main__":unittest.main()
