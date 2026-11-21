"""Deterministic synthetic fixtures for pack_gate; no Minecraft runtime."""
import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

from pack_gate import check_mod_tree, check_pack

PNG_HEADER = b"\x89PNG\r\n\x1a\n" + b"\x00\x00\x00\x0dIHDR" + b"\x00\x00\x00\x10\x00\x00\x00\x10"
META = json.dumps({"pack": {"pack_format": 15, "description": "Original preview"}})


class PackGateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "pack.zip"

    def make_pack(self, edits=None, remove=()):
        data = {"pack.mcmeta": META, "pack.png": PNG_HEADER,
                "assets/dbu_preview/textures/entity/skin.png": PNG_HEADER}
        for key in remove:
            data.pop(key, None)
        data.update(edits or {})
        with ZipFile(self.path, "w", ZIP_DEFLATED) as archive:
            for name, content in data.items():
                archive.writestr(name, content)
        return check_pack(self.path)

    def test_valid_preview(self):
        self.assertEqual([], self.make_pack())

    def test_missing_metadata(self):
        self.assertIn("missing root pack.mcmeta", self.make_pack(remove=["pack.mcmeta"]))

    def test_wrong_format(self):
        self.assertTrue(any("pack_format" in msg for msg in self.make_pack({"pack.mcmeta": '{"pack":{"pack_format":12,"description":"x"}}'})))

    def test_invalid_json(self):
        self.assertIn("invalid pack.mcmeta JSON/structure", self.make_pack({"pack.mcmeta": "{"}))

    def test_path_traversal(self):
        self.assertTrue(any("unsafe ZIP path" in msg for msg in self.make_pack({"../escape.txt": "x"})))

    def test_windows_path(self):
        self.assertTrue(any("unsafe ZIP path" in msg for msg in self.make_pack({"assets\\evil.txt": "x"})))

    def test_forbidden_namespace(self):
        self.assertTrue(any("reserved namespace" in msg for msg in self.make_pack({"assets/minecraft/textures/block/a.png": PNG_HEADER})))

    def test_pack_inside_zip(self):
        self.assertTrue(any("outside resource-pack assets" in msg for msg in self.make_pack({"mods/dbu.jar": "x"})))

    def test_bad_png(self):
        self.assertTrue(any("invalid PNG header" in msg for msg in self.make_pack({"pack.png": b"not png"})))

    def test_zero_png_width(self):
        png = PNG_HEADER[:16] + b"\x00\x00\x00\x00" + PNG_HEADER[20:]
        self.assertTrue(any("invalid PNG dimensions" in msg for msg in self.make_pack({"pack.png": png})))

    def test_missing_mod_tree(self):
        self.assertTrue(any("tree missing" in msg for msg in check_mod_tree(Path(self.temp.name))))

    def test_embedded_pack_in_mod_tree(self):
        resource = Path(self.temp.name) / "src/main/resources"
        resource.mkdir(parents=True)
        (resource / "HD.zip").write_bytes(b"x")
        self.assertTrue(any("archive bundled" in msg for msg in check_mod_tree(Path(self.temp.name))))

    def test_clean_mod_resources(self):
        resource = Path(self.temp.name) / "src/main/resources"
        resource.mkdir(parents=True)
        (resource / "pack.mcmeta").write_text(META)
        self.assertEqual([], check_mod_tree(Path(self.temp.name)))


if __name__ == "__main__":
    unittest.main()
