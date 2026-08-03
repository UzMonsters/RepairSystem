# Release Checklist

- `clean check bootJar` passes.
- Empty database migrates through latest Flyway version.
- Previous-version database upgrades successfully.
- Docker image builds.
- Production profile rejects missing secrets.
- Production-profile smoke passes.
- Release-candidate smoke passes.
- Backup/restore drill passes.
- Dependency scan reviewed.
- Container scan reviewed.
- SBOM generated and archived.
- Coverage remains at least line 80% and branch 55%.
- Docker containers, volumes, images, dumps, and harness files are cleaned up.
- Telegram webhook registered or intentionally disabled.
- Monitoring and alerting configured.
- Rollback plan reviewed.
