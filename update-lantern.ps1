# Auto-updates the Dutchess Lanterns mod before Minecraft launches.
# Install: put this file in the instance's "minecraft" folder, then in Prism:
#   Instance > Edit > Settings > Custom commands > Pre-launch command:
#   powershell -ExecutionPolicy Bypass -File "$INST_MC_DIR\update-lantern.ps1"
# Fail-soft: any error (offline, GitHub down) just lets the game launch as-is.

$repo = "WindingDuke77/dutchess-lanterns"
$modsDir = Join-Path $PSScriptRoot "mods"

try {
    if (-not (Test-Path $modsDir)) { exit 0 }

    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $release = Invoke-RestMethod "https://api.github.com/repos/$repo/releases/latest" -TimeoutSec 15
    $asset = $release.assets | Where-Object { $_.name -match '^lantern-[\d.]+\.jar$' } | Select-Object -First 1
    if ($null -eq $asset) { exit 0 }

    $current = Get-ChildItem $modsDir -Filter "lantern-*.jar" | Select-Object -First 1
    if ($null -ne $current -and $current.Name -eq $asset.name) {
        Write-Host "Dutchess Lanterns up to date ($($current.Name))"
        exit 0
    }

    Write-Host "Updating Dutchess Lanterns -> $($asset.name)"
    $staging = Join-Path $env:TEMP $asset.name
    Invoke-WebRequest $asset.browser_download_url -OutFile $staging -TimeoutSec 60

    # only swap after a complete download
    if ((Get-Item $staging).Length -gt 10kb) {
        Get-ChildItem $modsDir -Filter "lantern-*.jar" | Remove-Item -Force
        Move-Item $staging (Join-Path $modsDir $asset.name) -Force
        Write-Host "Installed $($asset.name)"
    }
} catch {
    Write-Host "Lantern update check skipped: $($_.Exception.Message)"
}
exit 0
