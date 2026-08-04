# Backup And Restore

## Backup

Use custom-format PostgreSQL dumps:

```powershell
pg_dump --format=custom --no-owner --file repairauto.dump $env:SPRING_DATASOURCE_URL
```

Encrypt backups before moving them outside the database host. Store backups in managed private storage with access logging. Recommended retention: daily backups for 14 days, weekly backups for 8 weeks, and monthly backups for 12 months.

## Restore Drill

1. Create a clean restore database.
2. Restore with:

```powershell
pg_restore --no-owner --dbname $env:RESTORE_DATABASE_URL repairauto.dump
```

3. Run Flyway validation using the backend build.
4. Start the application against the restored database.
5. Verify representative rows:
   - users
   - customers
   - technicians
   - repair requests
   - assignments
   - executions and status history
   - attachment metadata
   - Telegram sessions and update records
   - notification outbox and attempts
   - reviews

## Assumptions

RPO depends on backup frequency. RTO depends on dump size, restore bandwidth, and migration validation time. Temporary dump files must be deleted after drills.
