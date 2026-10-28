$ErrorActionPreference = "Stop"
. "$PSScriptRoot\local.secrets.ps1"

if (-not $env:MAX_BOT_TOKEN) {
    throw "Заполните MAX_BOT_TOKEN в scripts\local.secrets.ps1"
}
if (-not $env:MAX_BOT_USERNAME) {
    throw "Заполните MAX_BOT_USERNAME в scripts\local.secrets.ps1"
}
if (-not $env:MAX_WEBHOOK_SECRET) {
    throw "Заполните MAX_WEBHOOK_SECRET в scripts\local.secrets.ps1"
}
if (-not $env:PUBLIC_BASE_URL) {
    throw "Задайте PUBLIC_BASE_URL — HTTPS-адрес приложения"
}

$baseUrl = $env:PUBLIC_BASE_URL.TrimEnd("/")
$apiBase = if ($env:MAX_API_BASE_URL) { $env:MAX_API_BASE_URL.TrimEnd("/") } else { "https://platform-api2.max.ru" }
$webhookUrl = "$baseUrl/api/max/webhook/$($env:MAX_WEBHOOK_SECRET)"

$body = @{
    url = $webhookUrl
    update_types = @("message_created", "bot_started")
    secret = $env:MAX_WEBHOOK_SECRET
} | ConvertTo-Json -Depth 5

try {
    $response = Invoke-RestMethod -Method Post -Uri "$apiBase/subscriptions" `
        -Headers @{ Authorization = $env:MAX_BOT_TOKEN } `
        -ContentType "application/json" `
        -Body $body
    Write-Host "MAX webhook: $($response.ok)"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    $detail = $_.ErrorDetails.Message
    Write-Host "MAX API error. HTTP: $status"
    if ($detail) {
        Write-Host "Response: $detail"
    }
    exit 1
}
