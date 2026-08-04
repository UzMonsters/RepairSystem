# Incident Runbook

## First Steps

1. Check `/actuator/health/readiness`.
2. Identify the trace ID from the failing response.
3. Inspect sanitized application logs for the trace ID.
4. Check PostgreSQL connectivity and pool saturation.
5. Check object-storage availability.
6. Check Telegram provider status if notifications or bots are affected.

## Safe Mitigations

- Disable notification worker with `APP_NOTIFICATION_WORKER_ENABLED=false` during provider incidents.
- Disable cleanup jobs with `APP_CLEANUP_ENABLED=false` if cleanup causes unexpected load.
- Increase instances only if PostgreSQL connection capacity supports the total pool size.
- Do not delete business records during incident response.

## Escalation

Escalate when readiness is down for more than the agreed SLO window, backups fail, restore drill fails, dead notification counts rise, or customer-impacting requests cannot be created.
