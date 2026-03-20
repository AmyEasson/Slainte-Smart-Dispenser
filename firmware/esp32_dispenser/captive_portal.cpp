#include "captive_portal.h"
#include "config.h"
#include "credentials.h"
#include <WiFi.h>
#include <WebServer.h>

static WebServer webServer(AP_WEB_PORT);
static bool portalDone = false;

static const char PAGE_HTML[] PROGMEM = R"raw(
<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
<title>Slainte Setup</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:system-ui,sans-serif;background:#f5f0e8;padding:24px;display:flex;justify-content:center}
.card{background:#fff;border-radius:12px;padding:28px;max-width:400px;width:100%;box-shadow:0 2px 16px rgba(0,0,0,.06)}
h1{font-size:1.3rem;color:#2d6a4f;margin-bottom:6px}
label{display:block;font-weight:600;margin:12px 0 4px;font-size:.9rem}
input,select{width:100%;padding:10px;border:1px solid #b7d7b0;border-radius:8px;font-size:1rem}
input:focus,select:focus{outline:none;border-color:#2d6a4f}
button{width:100%;margin-top:16px;padding:12px;border:none;border-radius:8px;background:#2d6a4f;color:#fff;font-size:1rem;cursor:pointer}
button:active{background:#1b4332}
.row{display:flex;gap:8px;align-items:end}.row>*{flex:1}
.scan-btn{margin-top:6px;width:auto;padding:8px 14px;font-size:.85rem;background:#52b788;color:#fff;border:none;border-radius:6px;cursor:pointer}
.toggle{margin-top:6px;width:auto;padding:8px 14px;font-size:.85rem;background:#4a6b4a;color:#fff;border:none;border-radius:6px;cursor:pointer}
#manualBox{display:none}
</style></head><body><div class='card'>
<h1>Slainte Setup</h1>
<p style='color:#666;font-size:.9rem;margin-bottom:20px'>Configure Wi-Fi and server</p>
<form id='f' action='/save' method='POST'>
<label>Wi-Fi Network</label>
<div id='scanBox'><select id='ssidSel'><option>Scanning...</option></select>
<div class='row'><button type='button' class='scan-btn' onclick='scan()'>Refresh</button>
<button type='button' class='toggle' onclick='showManual()'>Type manually</button></div></div>
<div id='manualBox'><input type='text' id='ssidText' placeholder='Network name'>
<button type='button' class='toggle' onclick='showScan()'>Pick from list</button></div>
<input type='hidden' id='ssidVal' name='ssid'>
<label>Password</label><input type='password' name='password'>
<label>Server Address</label><input type='text' name='server' placeholder='192.168.1.50 or slainte.local' required>
<label>Port</label><input type='number' name='port' value='8080' min='1' max='65535'>
<button type='submit'>Save & Connect</button>
</form>
<script>
var useManual=false;
function showManual(){useManual=true;document.getElementById('scanBox').style.display='none';document.getElementById('manualBox').style.display='block'}
function showScan(){useManual=false;document.getElementById('scanBox').style.display='block';document.getElementById('manualBox').style.display='none'}
function scan(){fetch('/scan').then(r=>r.json()).then(d=>{var s=document.getElementById('ssidSel');s.innerHTML='';
d.length?(d.forEach(n=>{var o=document.createElement('option');o.value=n;o.textContent=n;s.appendChild(o)})):s.innerHTML='<option>No networks</option>'}).catch(()=>{})}
document.getElementById('f').onsubmit=function(){document.getElementById('ssidVal').value=useManual?document.getElementById('ssidText').value:document.getElementById('ssidSel').value}
scan();
</script>
</div></body></html>
)raw";

static const char PAGE_SAVED[] PROGMEM = R"raw(
<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>
<title>Slainte</title>
<style>body{font-family:system-ui;background:#f5f0e8;display:flex;justify-content:center;align-items:center;min-height:100vh}
.card{background:#fff;padding:32px;text-align:center;border-radius:12px}
h1{color:#2d6a4f;font-size:1.2rem}</style></head><body><div class='card'>
<h1>Saved</h1><p>Dispenser will reboot and connect.</p></div></body></html>
)raw";

static void handleRoot() { webServer.send_P(200, "text/html", PAGE_HTML); }

static void handleScan() {
  int n = WiFi.scanNetworks();
  String j = "[";
  for (int i = 0; i < n; i++) {
    if (i) j += ",";
    j += "\"" + WiFi.SSID(i) + "\"";
  }
  webServer.send(200, "application/json", j + "]");
}

static void handleSave() {
  String ssid = webServer.arg("ssid");
  String pass = webServer.arg("password");
  String server = webServer.arg("server");
  String port = webServer.arg("port");

  if (!ssid.length() || !server.length()) {
    webServer.sendHeader("Location", "/");
    webServer.send(302, "text/plain", "");
    return;
  }

  DeviceConfig cfg;
  memset(&cfg, 0, sizeof(cfg));
  ssid.toCharArray(cfg.ssid, sizeof(cfg.ssid));
  pass.toCharArray(cfg.password, sizeof(cfg.password));
  server.toCharArray(cfg.serverAddr, sizeof(cfg.serverAddr));
  cfg.serverPort = port.length() ? port.toInt() : DEFAULT_SERVER_PORT;

  saveCredentials(cfg);
  webServer.send_P(200, "text/html", PAGE_SAVED);
  delay(1000);
  webServer.stop();
  WiFi.softAPdisconnect(true);
  delay(500);
  portalDone = true;
}

static void handleNotFound() {
  webServer.sendHeader("Location", "http://192.168.4.1/");
  webServer.send(302, "text/plain", "");
}

void startCaptivePortal() {
  portalDone = false;
  WiFi.mode(WIFI_AP_STA);
  WiFi.softAP(AP_SSID);
  delay(500);
  webServer.on("/", HTTP_GET, handleRoot);
  webServer.on("/scan", HTTP_GET, handleScan);
  webServer.on("/save", HTTP_POST, handleSave);
  webServer.onNotFound(handleNotFound);
  webServer.begin();
}

void handleCaptivePortal() { webServer.handleClient(); }
bool portalCredentialsReceived() { return portalDone; }
