param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("getWebhookInfo", "setWebhook", "deleteWebhook")]
    [string] $Action,

    [string] $WebhookUrl,

    [ValidateSet("customer", "technician")]
    [string] $Bot = "customer",

    [string] $BotToken,

    [string] $WebhookSecret,

    [string] $ApiBaseUrl = $env:APP_TELEGRAM_API_BASE_URL
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($BotToken)) {
    if ($Bot -eq "technician") {
        $BotToken = $env:APP_TELEGRAM_TECHNICIAN_BOT_TOKEN
    } else {
        $BotToken = $env:APP_TELEGRAM_CUSTOMER_BOT_TOKEN
        if ([string]::IsNullOrWhiteSpace($BotToken)) {
            $BotToken = $env:APP_TELEGRAM_BOT_TOKEN
        }
    }
}

if ([string]::IsNullOrWhiteSpace($WebhookSecret)) {
    if ($Bot -eq "technician") {
        $WebhookSecret = $env:APP_TELEGRAM_TECHNICIAN_WEBHOOK_SECRET
    } else {
        $WebhookSecret = $env:APP_TELEGRAM_CUSTOMER_WEBHOOK_SECRET
        if ([string]::IsNullOrWhiteSpace($WebhookSecret)) {
            $WebhookSecret = $env:APP_TELEGRAM_WEBHOOK_SECRET
        }
    }
}

if ([string]::IsNullOrWhiteSpace($BotToken)) {
    throw "Telegram bot token is required. Set APP_TELEGRAM_CUSTOMER_BOT_TOKEN or APP_TELEGRAM_TECHNICIAN_BOT_TOKEN."
}

if ([string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    $ApiBaseUrl = "https://api.telegram.org"
}

if ($Action -eq "setWebhook") {
    if ([string]::IsNullOrWhiteSpace($WebhookSecret) -or $WebhookSecret.Length -lt 32) {
        throw "Telegram webhook secret must be set and at least 32 characters."
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
    bot = $Bot
    description = $response.description
    result = $response.result
}
$safeResult | ConvertTo-Json -Depth 8
