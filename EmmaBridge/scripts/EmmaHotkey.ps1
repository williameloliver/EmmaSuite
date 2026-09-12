Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$JarPath = Join-Path $PSScriptRoot "..\EmmaBridge.jar"
if (!(Test-Path $JarPath)) { $JarPath = Join-Path (Get-Location) "EmmaBridge.jar" }
$JarPath = (Resolve-Path $JarPath).Path

$code = @"
using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Windows.Forms;

public class EmmaHotkeyWindow : NativeWindow, IDisposable {
    [DllImport("user32.dll")] static extern bool RegisterHotKey(IntPtr hWnd, int id, uint mods, uint vk);
    [DllImport("user32.dll")] static extern bool UnregisterHotKey(IntPtr hWnd, int id);
    const int WM_HOTKEY = 0x0312;
    const uint MOD_CONTROL = 0x0002;
    const uint MOD_SHIFT = 0x0004;
    const int ID = 0x454D;
    string jar;

    public EmmaHotkeyWindow(string path) {
        jar = path;
        CreateHandle(new CreateParams());
        if (!RegisterHotKey(Handle, ID, MOD_CONTROL | MOD_SHIFT, (uint)Keys.E))
            throw new Exception("No se pudo registrar Ctrl+Shift+E");
    }

    protected override void WndProc(ref Message m) {
        if (m.Msg == WM_HOTKEY) {
            ProcessStartInfo p = new ProcessStartInfo();
            p.FileName = "javaw.exe";
            p.Arguments = "-jar \\"" + jar + "\\" --capture";
            p.UseShellExecute = true;
            Process.Start(p);
        }
        base.WndProc(ref m);
    }

    public void Dispose() {
        UnregisterHotKey(Handle, ID);
        DestroyHandle();
    }
}
"@

Add-Type -TypeDefinition $code -ReferencedAssemblies System.Windows.Forms
$ctx = New-Object System.Windows.Forms.ApplicationContext
$hotkey = New-Object EmmaHotkeyWindow($JarPath)
$tray = New-Object System.Windows.Forms.NotifyIcon
$tray.Icon = [System.Drawing.SystemIcons]::Information
$tray.Text = "EmmaBridge - Ctrl+Shift+E"
$tray.Visible = $true
$menu = New-Object System.Windows.Forms.ContextMenuStrip
$exit = $menu.Items.Add("Salir")
$exit.add_Click({
    $tray.Visible = $false
    $hotkey.Dispose()
    $ctx.ExitThread()
})
$tray.ContextMenuStrip = $menu
[System.Windows.Forms.Application]::Run($ctx)
