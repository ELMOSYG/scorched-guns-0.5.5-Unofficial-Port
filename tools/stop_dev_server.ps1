# Stop only the DSH dev server (the Gradle "runServer" JVM), and nothing else.
#
# Why this script exists: DSH is itself launched through a chain of cmd.exe processes
# (explorer -> cmd -> node -> cmd -> node -> cmd -> node). A blanket
# "Get-Process java,cmd | Stop-Process -Force" therefore kills the harness along with the game
# server - which is exactly what made DSH die mid-turn several times (HANDOFF section 38).
# It matches on the JVM's own command line, so it can only ever hit the dev runtime.
#
# Usage:  pwsh -File tools/stop_dev_server.ps1
$targets = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -match 'devlaunch' }

if (-not $targets) {
    Write-Host 'no dev server JVM is running'
    exit 0
}

foreach ($process in $targets) {
    Write-Host "stopping dev server JVM pid $($process.ProcessId)"
    Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
}

Start-Sleep -Seconds 3
$left = (Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -match 'devlaunch' } | Measure-Object).Count
Write-Host "dev server JVMs left: $left"
