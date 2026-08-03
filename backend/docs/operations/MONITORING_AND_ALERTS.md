# Monitoring And Alerts

Expose metrics only on a protected management network.

Recommended alerts:

- Application not ready: check database, storage, Flyway, and recent deployment.
- High HTTP 5xx rate: inspect trace IDs, dependency health, and recent changes.
- High authentication failure rate: inspect ingress logs and throttling metrics.
- Database pool exhaustion: reduce instance count or pool size, inspect slow queries.
- Database unavailable: fail over or restore service.
- Object storage unavailable: pause uploads, verify credentials and bucket policy.
- Notification dead count above threshold: inspect failure categories and Telegram status.
- Oldest pending notification age above threshold: inspect worker enablement and leases.
- Notification lease recovery spike: inspect worker restarts and external timeouts.
- Stale attachment uploads: inspect storage health and cleanup metrics.
- Cleanup failures: inspect storage permissions and database locks.
- Disk usage: prune Docker/test artifacts and verify backup rotation.
- High memory pressure: inspect JVM memory, traffic, and upload volume.
- High request latency: inspect database pool, slow queries, and downstream timeouts.
- Backup failure or restore drill overdue: escalate to operations owner.
