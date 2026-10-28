$ErrorActionPreference = "Stop"

$secrets = Join-Path $PSScriptRoot "local.secrets.ps1"
if (-not (Test-Path $secrets)) {
    throw "Create scripts\local.secrets.ps1 from local.secrets.example.ps1"
}
. $secrets

if (-not $env:TELEGRAM_BOT_TOKEN) {
    throw "Fill TELEGRAM_BOT_TOKEN in scripts\local.secrets.ps1"
}
if (-not $env:TELEGRAM_WEBHOOK_SECRET) {
    throw "Fill TELEGRAM_WEBHOOK_SECRET in scripts\local.secrets.ps1"
}
if (-not $env:PUBLIC_BASE_URL) {
    throw "Set PUBLIC_BASE_URL to the HTTPS tunnel URL"
}

$baseUrl = $env:PUBLIC_BASE_URL.TrimEnd("/")
$uri = [uri]$baseUrl
$api = "https://api.telegram.org/bot$($env:TELEGRAM_BOT_TOKEN)"
$webhookUrl = "$baseUrl/api/telegram/webhook/$($env:TELEGRAM_WEBHOOK_SECRET)"

try {
    $set = Invoke-RestMethod -Method Post -Uri "$api/setWebhook" -Body @{
        url = $webhookUrl
        secret_token = $env:TELEGRAM_WEBHOOK_SECRET
    }
    if (-not $set.ok) {
        throw "setWebhook returned ok=false"
    }

    $info = Invoke-RestMethod -Uri "$api/getWebhookInfo"
    Write-Host "Webhook set: $($uri.Host)"
    Write-Host "URL: $($info.result.url)"
    Write-Host "Pending updates: $($info.result.pending_update_count)"
    if ($info.result.last_error_message) {
        Write-Host "Last error: $($info.result.last_error_message)"
    }

    $menu = @{
        menu_button = @{
            type = "web_app"
            text = "Open"
            web_app = @{ url = "$baseUrl/app" }
        }
    }
    $setMenu = Invoke-RestMethod -Method Post -Uri "$api/setChatMenuButton" `
        -ContentType "application/json" `
        -Body (ConvertTo-Json -InputObject $menu -Depth 5)
    Write-Host "Menu button: $($setMenu.ok)"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    $detail = $_.ErrorDetails.Message
    Write-Host "Telegram API error. HTTP: $status"
    if ($detail) {
        Write-Host "Response: $detail"
    }
    exit 1
}
