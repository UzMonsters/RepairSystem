param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("getWebhookInfo", "setWebhook", "deleteWebhook")]
    [string] $Action,

    [string] $WebhookUrl,

    [string] $BotToken = $env:APP_TELEGRAM_BOT_TOKEN,

    [string] $WebhookSecret = $env:APP_TELEGRAM_WEBHOOK_SECRET,

    [string] $ApiBaseUrl = $env:APP_TELEGRAM_API_BASE_URL
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($BotToken)) {
    throw "APP_TELEGRAM_BOT_TOKEN is required."
}

if ([string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    $ApiBaseUrl = "https://api.telegram.org"
}

if ($Action -eq "setWebhook") {
    if ([string]::IsNullOrWhiteSpace($WebhookSecret) -or $WebhookSecret.Length -lt 32) {
        throw "APP_TELEGRAM_WEBHOOK_SECRET must be set and at least 32 characters."
    }
    if ([string]::IsNullOrWhiteSpace($WebhookUrl) -or -not $WebhookUrl.StartsWith("https://")) {
        throw "WebhookUrl must be HTTPS."
    }
}

$base = $ApiBaseUrl.TrimEnd("/")
$method = switch ($Action) {
    "getWebhookInfo" { "getWebhookInfo" }
    "setWebhook" { "setWebhook" }
    "deleteWebhook" { "deleteWebhook" }
}
$uri = "$base/bot$BotToken/$method"

$body = @{}
if ($Action -eq "setWebhook") {
    $body.url = $WebhookUrl
    $body.secret_token = $WebhookSecret
    $body.allowed_updates = @("message", "callback_query")
}

try {
    if ($body.Count -gt 0) {
        $response = Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json" `
            -Body ($body | ConvertTo-Json -Compress)
    } else {
        $response = Invoke-RestMethod -Method Post -Uri $uri
    }
} catch {
    throw "Telegram webhook operation failed. Check token, URL, network, and provider status."
}

if (-not $response.ok) {
    throw "Telegram webhook operation returned a provider error."
}

$safeResult = [ordered]@{
    ok = $response.ok
    action = $Action
    description = $response.description
    result = $response.result
}
$safeResult | ConvertTo-Json -Depth 8
