<#
.SYNOPSIS
  Sincroniza MicroBudget entre el emulador del reloj y el del celular usando ADB,
  sin necesidad de emparejarlos con la app Wear OS.

.DESCRIPTION
  Usa exactamente el mismo protocolo que la sincronización real (PUSH/ACK con
  "gana la última modificación"), pero transporta los paquetes como archivos:
    1. El reloj exporta sus cambios  -> el celular los importa y responde (ACK) -> el reloj aplica el ACK.
    2. El celular exporta sus cambios -> el reloj los importa y responde       -> el celular aplica el ACK.
  Al terminar, ambos dispositivos tienen los mismos datos y sus cambios quedan marcados como sincronizados.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\simular-sync.ps1
  powershell -ExecutionPolicy Bypass -File tools\simular-sync.ps1 -Watch emulator-5554 -Phone emulator-5556
#>
param(
    [string]$Watch = "",
    [string]$Phone = ""
)

$ErrorActionPreference = "Stop"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }

$receiver = "com.microbudget/com.microbudget.data.sync.AdbSyncReceiver"
$remoteDir = "/sdcard/Android/data/com.microbudget/files/sync"
$tmp = Join-Path $PSScriptRoot "..\sync-tmp"
New-Item -ItemType Directory -Force $tmp | Out-Null

# --- Detectar reloj y celular automáticamente ---
if (-not $Watch -or -not $Phone) {
    $serials = & $adb devices | Select-String "^(\S+)\s+device$" | ForEach-Object { $_.Matches[0].Groups[1].Value }
    foreach ($s in $serials) {
        $chars = (& $adb -s $s shell getprop ro.build.characteristics).Trim()
        if ($chars -match "watch") { if (-not $Watch) { $Watch = $s } } elseif (-not $Phone) { $Phone = $s }
    }
}
if (-not $Watch -or -not $Phone) {
    Write-Host "No encontré ambos emuladores encendidos (reloj='$Watch', celular='$Phone')." -ForegroundColor Red
    Write-Host "Inicia los dos AVD desde Android Studio (Device Manager) y vuelve a intentarlo."
    exit 1
}
Write-Host "Reloj:   $Watch"
Write-Host "Celular: $Phone`n"

function Send-Broadcast([string]$serial, [string]$action) {
    $out = & $adb -s $serial shell am broadcast -a "com.microbudget.sync.$action" -n $receiver 2>&1 | Out-String
    $m = [regex]::Match($out, 'data="([^"]*)"')
    $msg = if ($m.Success) { $m.Groups[1].Value } else { $out.Trim() }
    if ($out -notmatch "result=1") { throw "[$serial] $action falló: $msg" }
    return $msg
}

function Sync-OneWay([string]$from, [string]$to, [string]$fromName, [string]$toName) {
    Write-Host "== $fromName -> $toName ==" -ForegroundColor Cyan
    Write-Host ("  {0}: {1}" -f $fromName, (Send-Broadcast $from "EXPORT"))
    & $adb -s $from pull "$remoteDir/outbox.json" "$tmp\outbox.json" | Out-Null
    & $adb -s $to shell mkdir -p $remoteDir | Out-Null
    & $adb -s $to push "$tmp\outbox.json" "$remoteDir/inbox.json" | Out-Null
    Write-Host ("  {0}: {1}" -f $toName, (Send-Broadcast $to "IMPORT"))
    & $adb -s $to pull "$remoteDir/response.json" "$tmp\response.json" | Out-Null
    & $adb -s $from push "$tmp\response.json" "$remoteDir/ack.json" | Out-Null
    Write-Host ("  {0}: {1}" -f $fromName, (Send-Broadcast $from "ACK"))
}

try {
    Sync-OneWay $Watch $Phone "Reloj" "Celular"
    Sync-OneWay $Phone $Watch "Celular" "Reloj"
    Write-Host "`nSincronización completada. Abre la app en ambos dispositivos para ver los cambios." -ForegroundColor Green
} catch {
    Write-Host "`nError: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Verifica que MicroBudget esté instalado y se haya abierto al menos una vez en ambos emuladores."
    exit 1
}
