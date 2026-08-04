# Telegram Webhook Operations

Use `scripts/telegram-webhook.ps1` for one-shot webhook management. The application does not automatically overwrite Telegram webhook configuration during startup.

## Register

```powershell
.\scripts\telegram-webhook.ps1 -Bot customer -Action setWebhook -WebhookUrl https://api.example.com/api/v1/telegram/webhook/customer
.\scripts\telegram-webhook.ps1 -Bot technician -Action setWebhook -WebhookUrl https://api.example.com/api/v1/telegram/webhook/technician
```

The script reads bot-specific variables:

- `APP_TELEGRAM_CUSTOMER_BOT_TOKEN`
- `APP_TELEGRAM_CUSTOMER_WEBHOOK_SECRET`
- `APP_TELEGRAM_TECHNICIAN_BOT_TOKEN`
- `APP_TELEGRAM_TECHNICIAN_WEBHOOK_SECRET`
- optional `APP_TELEGRAM_API_BASE_URL`

Legacy `APP_TELEGRAM_BOT_TOKEN` and `APP_TELEGRAM_WEBHOOK_SECRET` are still accepted only as explicit fallbacks for the customer bot.

## Verify

```powershell
.\scripts\telegram-webhook.ps1 -Bot customer -Action getWebhookInfo
.\scripts\telegram-webhook.ps1 -Bot technician -Action getWebhookInfo
```

## Delete

```powershell
.\scripts\telegram-webhook.ps1 -Bot customer -Action deleteWebhook
.\scripts\telegram-webhook.ps1 -Bot technician -Action deleteWebhook
```

## Secret Rotation

1. Generate a new webhook secret.
2. Deploy backend instances with the new secret.
3. Run `setWebhook` with the new secret.
4. Verify with `getWebhookInfo`.

The script validates HTTPS URLs and never prints the bot token.
