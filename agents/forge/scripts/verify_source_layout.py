#!/usr/bin/env python3
"""DBU preflight: source layout and output-JAR safety checks (Python 3 stdlib).

This is a *static* integration gate, not a Forge build or Minecraft test.
Usage:
  python3 verify_source_layout.py --source path/to/DragonBallUltimate
  python3 verify_source_layout.py --source path/to/source.zip
  python3 verify_source_layout.py --jar build/libs/dragonballultimate-1.5.4.jar
"""

import argparse
import io
import json
from pathlib import Path, PurePosixPath
import re
import sys
import zipfile


class ContractError(ValueError):
    pass


def valid_member_name(value):
    """Reject traversal, absolute, NUL and non-POSIX archive member names."""
    if not value or '\x00' in value or '\\' in value or value.startswith('/'):
        return False
    if re.match(r"^[A-Za-z]:", value):
        return False
    pieces = value.split('/')
    return not any(part in ('', '.', '..') for part in pieces if part != pieces[-1] or part)


class SourceTree:
    def __init__(self, path):
        self.path = Path(path)
        if self.path.is_dir():
            self.archive = None
            self.root = ''
            self.names = self._collect_dir_names()
        elif self.path.is_file() and zipfile.is_zipfile(self.path):
            self.archive = zipfile.ZipFile(self.path)
            all_names = [entry.filename for entry in self.archive.infolist() if not entry.is_dir()]
            if not all_names:
                raise ContractError('ZIP de fonte vazio')
            if len(all_names) != len(set(all_names)) or any(not valid_member_name(n) for n in all_names):
                raise ContractError('ZIP com caminho inseguro ou duplicado')
            expected = 'build.gradle'
            if expected in all_names:
                self.root = ''
            else:
                prefixes = {n.split('/')[0] for n in all_names if '/' in n}
                matching = [p + '/' for p in prefixes if p + '/' + expected in all_names]
                if len(matching) != 1 or len(prefixes) != 1:
                    raise ContractError('ZIP deve conter um unico projeto Gradle na raiz')
                self.root = matching[0]
            self.names = {n[len(self.root):] for n in all_names if n.startswith(self.root)}
        else:
            raise ContractError('Fonte deve ser uma pasta ou arquivo ZIP valido')

    def _collect_dir_names(self):
        names = set()
        base = self.path.resolve()
        for file in self.path.rglob('*'):
            if file.is_symlink():
                raise ContractError('Link simbolico nao permitido no preflight: ' + str(file))
            if file.is_file():
                relative = file.relative_to(base).as_posix()
                if not valid_member_name(relative):
                    raise ContractError('Caminho de fonte inseguro: ' + relative)
                names.add(relative)
        return names

    def read(self, name):
        if name not in self.names:
            raise ContractError('Arquivo obrigatorio ausente: ' + name)
        if self.archive is not None:
            return self.archive.read(self.root + name)
        return (self.path / name).read_bytes()

    def text(self, name):
        try:
            return self.read(name).decode('utf-8')
        except UnicodeDecodeError as exc:
            raise ContractError('Texto nao e UTF-8: ' + name) from exc

    def close(self):
        if self.archive is not None:
            self.archive.close()


def require(condition, message):
    if not condition:
        raise ContractError(message)


def verify_source(path):
    tree = SourceTree(path)
    try:
        required = (
            'settings.gradle', 'build.gradle', 'gradle.properties',
            'gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar',
            'gradle/wrapper/gradle-wrapper.properties',
            'src/main/resources/META-INF/mods.toml',
            'src/main/resources/pack.mcmeta',
        )
        for name in required:
            tree.read(name)
        java_files = sorted(name for name in tree.names
                            if name.startswith('src/main/java/') and name.endswith('.java'))
        require(bool(java_files), 'Nenhuma fonte Java do mod encontrada')

        gradle = tree.text('build.gradle')
        wrapper = tree.text('gradle/wrapper/gradle-wrapper.properties')
        mods = tree.text('src/main/resources/META-INF/mods.toml')
        pack = tree.text('src/main/resources/pack.mcmeta')
        require(re.search(r"id\s+['\"]net\.minecraftforge\.gradle['\"]\s+version\s+['\"]6\.0\.54['\"]", gradle),
                'ForgeGradle esperado: 6.0.54')
        require(re.search(r'JavaLanguageVersion\s*\.\s*of\s*\(\s*17\s*\)', gradle),
                'Toolchain deve ser Java 17')
        require(re.search(r"mappings\s+channel\s*:\s*['\"]official['\"]\s*,\s*version\s*:\s*['\"]1\.20\.1['\"]", gradle),
                'Mappings oficiais devem ser 1.20.1')
        require('net.minecraftforge:forge:1.20.1-47.3.0' in gradle,
                'Dependencia Forge deve ser 1.20.1-47.3.0')
        require('gradle-8.8-bin.zip' in wrapper and 'https' in wrapper,
                'Gradle Wrapper esperado: 8.8 HTTPS')
        require(zipfile.is_zipfile(io.BytesIO(tree.read('gradle/wrapper/gradle-wrapper.jar'))),
                'gradle-wrapper.jar esta ausente ou invalido')
        require(re.search(r'modId\s*=\s*[\"\']dragonballultimate[\"\']', mods),
                'mods.toml sem modId dragonballultimate')
        require('modId="minecraft"' in mods and 'modId="forge"' in mods,
                'mods.toml sem dependencias Minecraft/Forge')
        try:
            pack_data = json.loads(pack)
        except json.JSONDecodeError as exc:
            raise ContractError('pack.mcmeta nao e JSON valido') from exc
        require(pack_data.get('pack', {}).get('pack_format') == 15,
                'pack.mcmeta deve ter pack_format 15 (Minecraft 1.20.1)')

        # External HD pack is never bundled as a nested ZIP or resourcepacks folder.
        resources = [n for n in tree.names if n.startswith('src/main/resources/')]
        illegal = [n for n in resources if n.lower().endswith(('.zip', '.jar')) or
                   any(part.lower() == 'resourcepacks' for part in PurePosixPath(n).parts)]
        require(not illegal, 'Pack externo ou arquivo compactado em resources: ' + ', '.join(illegal[:3]))

        # Marker presence is a static regression check, not proof of save compatibility.
        java_text = '\n'.join(tree.text(n) for n in java_files)
        require('DBUH1' in java_text and 'DBUS1' in java_text,
                'Marcadores DBUH1/DBUS1 ausentes do codigo Java; revisar contrato de saves')
        return ('OK - fonte presente; ForgeGradle 6.0.54, Java 17, Forge 47.3.0, '
                f'{len(java_files)} fontes Java, marcadores DBUH1/DBUS1, recursos minimos e pack externo sem ZIP aninhado')
    finally:
        tree.close()


def verify_jar(path):
    file = Path(path)
    require(file.is_file() and file.stat().st_size > 0, 'JAR ausente ou vazio')
    require(zipfile.is_zipfile(file), 'JAR nao e arquivo ZIP valido')
    with zipfile.ZipFile(file) as jar:
        corrupt = jar.testzip()
        require(corrupt is None, 'JAR contem entrada com CRC invalido: ' + str(corrupt))
        names = [info.filename for info in jar.infolist() if not info.is_dir()]
        require(len(names) == len(set(names)) and all(valid_member_name(n) for n in names),
                'JAR com entrada duplicada ou caminho inseguro')
        require('META-INF/mods.toml' in names, 'JAR sem META-INF/mods.toml')
        require(any(n.startswith('com/dragonballultimate/') and n.endswith('.class') for n in names),
                'JAR sem classes do mod')
        illegal = [n for n in names if n.lower().endswith(('.zip', '.jar')) or
                   any(part.lower() == 'resourcepacks' for part in PurePosixPath(n).parts)]
        require(not illegal, 'JAR contem pack externo ou archive aninhado: ' + ', '.join(illegal[:3]))
        return f'OK - integridade ZIP, mods.toml, classes; {len(names)} entradas; pack HD nao embutido'


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--source', metavar='FONTE_PASTA_OU_ZIP')
    group.add_argument('--jar', metavar='ARQUIVO_JAR')
    args = parser.parse_args(argv)
    try:
        outcome = verify_source(args.source) if args.source else verify_jar(args.jar)
        print('[PASS] ' + outcome)
        return 0
    except (ContractError, OSError, zipfile.BadZipFile) as exc:
        print('[FAIL] ' + str(exc), file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
