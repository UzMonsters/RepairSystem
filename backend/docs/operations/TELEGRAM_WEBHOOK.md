# Telegram Webhook Operations

Use `scripts/telegram-webhook.ps1` for one-shot webhook management. The application does not automatically overwrite Telegram webhook configuration during startup.

## Register

```powershell
.\scripts\telegram-webhook.ps1 -Action setWebhook -WebhookUrl https://api.example.com/api/v1/telegram/webhook
```

The script reads `APP_TELEGRAM_BOT_TOKEN`, `APP_TELEGRAM_WEBHOOK_SECRET`, and optional `APP_TELEGRAM_API_BASE_URL`.

## Verify

```powershell
.\scripts\telegram-webhook.ps1 -Action getWebhookInfo
```

## Delete

```powershell
.\scripts\telegram-webhook.ps1 -Action deleteWebhook
```

## Secret Rotation

1. Generate a new webhook secret.
2. Deploy backend instances with the new secret.
3. Run `setWebhook` with the new secret.
4. Verify with `getWebhookInfo`.

The script validates HTTPS URLs and never prints the bot token.
