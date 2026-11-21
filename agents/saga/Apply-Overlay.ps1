#Requires -Version 7.0
[CmdletBinding()]
param([Parameter(Mandatory)][string] $ModSource)

$ErrorActionPreference = 'Stop'
$modSourcePath = (Resolve-Path -LiteralPath $ModSource).Path
$overlay = Join-Path $PSScriptRoot 'overlay'
$modDescriptor = Join-Path $modSourcePath 'src/main/resources/META-INF/mods.toml'
if (!(Test-Path -LiteralPath $modDescriptor) -or
    !(Select-String -LiteralPath $modDescriptor -Pattern 'modId\s*=\s*"dragonballultimate"' -Quiet)) {
    throw 'Selecione a pasta da fonte Forge de Dragon Ball Ultimate.'
}

$filesToCopy = foreach ($source in Get-ChildItem -LiteralPath $overlay -Recurse -File) {
    $relativePath = [IO.Path]::GetRelativePath($overlay, $source.FullName)
    $target = Join-Path $modSourcePath $relativePath
    if (Test-Path -LiteralPath $target) {
        if ((Get-FileHash -LiteralPath $target).Hash -ne (Get-FileHash -LiteralPath $source.FullName).Hash) {
            throw "Colisão com um arquivo existente: $relativePath"
        }
    } else {
        @{ Source = $source.FullName; Target = $target }
    }
}

$langFiles = foreach ($locale in 'pt_br', 'en_us') {
    $target = Join-Path $modSourcePath "src/main/resources/assets/dragonballultimate/lang/$locale.json"
    $existing = Get-Content -LiteralPath $target -Raw | ConvertFrom-Json -AsHashtable
    $additions = Get-Content -LiteralPath (Join-Path $PSScriptRoot "lang/$locale.json") -Raw |
        ConvertFrom-Json -AsHashtable
    $changed = $false
    foreach ($key in $additions.Keys) {
        if (!$existing.Contains($key)) {
            $existing[$key] = $additions[$key]
            $changed = $true
        } elseif ($existing[$key] -cne $additions[$key]) {
            throw "Colisão de tradução em $locale para $key"
        }
    }
    if ($changed) {
        @{ Target = $target; Content = ConvertTo-Json -InputObject $existing -Depth 8 }
    }
}

foreach ($file in $filesToCopy) {
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($file.Target)) | Out-Null
    Copy-Item -LiteralPath $file.Source -Destination $file.Target
}
foreach ($lang in $langFiles) {
    Set-Content -LiteralPath $lang.Target -Value $lang.Content -Encoding utf8NoBOM
}

Write-Output "SAGA aplicado em $modSourcePath"
