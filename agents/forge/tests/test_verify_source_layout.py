"""Unit tests for FORGE's source/JAR static preflight (stdlib only)."""
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from verify_source_layout import ContractError, verify_jar, verify_source  # noqa: E402


class VerifySourceLayoutTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'DragonBallUltimate'
        self.root.mkdir()
        self.files = {
            'settings.gradle': "rootProject.name = 'DragonBallUltimate'\n",
            'build.gradle': "plugins { id 'net.minecraftforge.gradle' version '6.0.54' }\n"
                            "java.toolchain.languageVersion = JavaLanguageVersion.of(17)\n"
                            "minecraft { mappings channel: 'official', version: '1.20.1' }\n"
                            "dependencies { minecraft 'net.minecraftforge:forge:1.20.1-47.3.0' }\n",
            'gradle.properties': 'org.gradle.daemon=false\n',
            'gradlew': '#!/bin/sh\n',
            'gradlew.bat': '@echo off\n',
            'gradle/wrapper/gradle-wrapper.properties':
                'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.8-bin.zip\n',
            'src/main/resources/META-INF/mods.toml':
                'modId="dragonballultimate"\nmodId="minecraft"\nmodId="forge"\n',
            'src/main/resources/pack.mcmeta': json.dumps({'pack': {'pack_format': 15}}),
            'src/main/java/com/dragonballultimate/Contracts.java':
                'class Contracts { String hair="DBUH1"; String skin="DBUS1"; }\n',
        }
        wrapper_bytes = io.BytesIO()
        with zipfile.ZipFile(wrapper_bytes, 'w') as archive:
            archive.writestr('org/gradle/wrapper/GradleWrapperMain.class', b'fixture')
        self.files['gradle/wrapper/gradle-wrapper.jar'] = wrapper_bytes.getvalue()
        for name, contents in self.files.items():
            dest = self.root / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(contents.encode() if isinstance(contents, str) else contents)

    def test_valid_directory(self):
        self.assertTrue(verify_source(self.root).startswith('OK'))

    def test_valid_zip_with_project_prefix(self):
        path = Path(self.temp.name) / 'source.zip'
        with zipfile.ZipFile(path, 'w') as archive:
            for name, data in self.files.items():
                archive.writestr('DragonBallUltimate/' + name, data)
        self.assertTrue(verify_source(path).startswith('OK'))

    def test_reject_non_java17_toolchain(self):
        f = self.root / 'build.gradle'
        f.write_text(f.read_text().replace('of(17)', 'of(21)'))
        with self.assertRaisesRegex(ContractError, 'Java 17'):
            verify_source(self.root)

    def test_reject_wrong_forge_version(self):
        f = self.root / 'build.gradle'
        f.write_text(f.read_text().replace('47.3.0', '47.2.0'))
        with self.assertRaisesRegex(ContractError, '47.3.0'):
            verify_source(self.root)

    def test_reject_missing_save_marker(self):
        f = self.root / 'src/main/java/com/dragonballultimate/Contracts.java'
        f.write_text('class Contracts {}')
        with self.assertRaisesRegex(ContractError, 'DBUH1/DBUS1'):
            verify_source(self.root)

    def test_reject_bundled_external_pack(self):
        f = self.root / 'src/main/resources/resourcepacks/DBU-HD.zip'
        f.parent.mkdir(parents=True)
        f.write_bytes(b'ZIP')
        with self.assertRaisesRegex(ContractError, 'Pack externo'):
            verify_source(self.root)

    def test_reject_zip_traversal(self):
        path = Path(self.temp.name) / 'bad.zip'
        with zipfile.ZipFile(path, 'w') as archive:
            archive.writestr('DragonBallUltimate/build.gradle', self.files['build.gradle'])
            archive.writestr('DragonBallUltimate/../escape.txt', 'bad')
        with self.assertRaisesRegex(ContractError, 'inseguro'):
            verify_source(path)

    def test_reject_missing_wrapper_jar(self):
        (self.root / 'gradle/wrapper/gradle-wrapper.jar').unlink()
        with self.assertRaisesRegex(ContractError, 'obrigatorio ausente'):
            verify_source(self.root)

    def test_valid_jar(self):
        jar = Path(self.temp.name) / 'dbu.jar'
        with zipfile.ZipFile(jar, 'w') as archive:
            archive.writestr('META-INF/mods.toml', 'modId="dragonballultimate"')
            archive.writestr('com/dragonballultimate/Db.class', b'bytecode')
        self.assertTrue(verify_jar(jar).startswith('OK'))

    def test_reject_jar_with_hd_pack(self):
        jar = Path(self.temp.name) / 'dbu.jar'
        with zipfile.ZipFile(jar, 'w') as archive:
            archive.writestr('META-INF/mods.toml', 'modId="dragonballultimate"')
            archive.writestr('com/dragonballultimate/Db.class', b'bytecode')
            archive.writestr('resourcepacks/dbu-hd.zip', b'content')
        with self.assertRaisesRegex(ContractError, 'pack externo'):
            verify_jar(jar)

    def test_reject_jar_without_classes(self):
        jar = Path(self.temp.name) / 'dbu.jar'
        with zipfile.ZipFile(jar, 'w') as archive:
            archive.writestr('META-INF/mods.toml', 'modId="dragonballultimate"')
        with self.assertRaisesRegex(ContractError, 'sem classes'):
            verify_jar(jar)


if __name__ == '__main__':
    unittest.main(verbosity=2)
