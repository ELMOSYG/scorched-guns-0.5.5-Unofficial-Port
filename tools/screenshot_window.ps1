# Capture the screen (activating the Minecraft window first) so a client-side overlay can be
# checked without a human looking at the monitor. Used once, for the blindness overlay.
param([string]$Out = 'build-logs\shot.png')

Add-Type -AssemblyName System.Windows.Forms, System.Drawing
Add-Type @'
using System;
using System.Runtime.InteropServices;
public class DshWin {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
}
'@

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like '*Minecraft*' } | Select-Object -First 1
if ($proc) {
    [DshWin]::ShowWindow($proc.MainWindowHandle, 9) | Out-Null
    [DshWin]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
    Write-Output ("activated window: " + $proc.MainWindowTitle)
    Start-Sleep -Milliseconds 1200
} else {
    Write-Output 'no Minecraft window found; capturing the whole screen'
}

$bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$bitmap = New-Object System.Drawing.Bitmap($bounds.Width, $bounds.Height)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
$bitmap.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$bitmap.Dispose()
Write-Output ("saved " + $Out)
