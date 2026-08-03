# Performance Baseline

## Baseline Scope

Representative operations:

- login
- create request
- request list and detail
- assignment
- attachment metadata read
- dashboard overview and trends
- notification worker batch
- review list and summary

## Representative Dataset

Release-readiness dataset used on 2026-08-03:

- 10,000 repair requests
- 8,572 assignments
- 18,572 status-history rows
- 3,000 attachment metadata rows
- 3,000 notification outbox rows and 3,000 delivery attempts
- 1,000 reviews

## Current Baseline

Environment:

- `docker-compose.prod.example.yml`
- PostgreSQL 17, MinIO, production-profile backend
- `APP_DB_POOL_MAX_SIZE=10`
- Telegram disabled for the API latency baseline
- Docker Desktop on local workstation

Measured API latencies:

| Operation | Samples | p50 | p95 | p99 |
| --- | ---: | ---: | ---: | ---: |
| Login | 10 | 234.21 ms | 271.61 ms | 388.66 ms |
| Request list | 30 | 28.75 ms | 54.69 ms | 162.13 ms |
| Request search | 20 | 76.94 ms | 113.72 ms | 334.94 ms |
| Request detail | 30 | 26.19 ms | 45.07 ms | 147.30 ms |
| Dashboard overview | 20 | 15.31 ms | 63.35 ms | 102.31 ms |
| Dashboard trends | 10 | 25.49 ms | 32.59 ms | 33.98 ms |
| Review list | 20 | 24.78 ms | 43.77 ms | 48.45 ms |
| Notification list | 20 | 46.12 ms | 100.19 ms | 115.53 ms |

Runtime observations:

- Backend: 1.33% CPU, 407.3 MiB / 768 MiB
- PostgreSQL: 0.43% CPU, 66.1 MiB
- MinIO: 0.05% CPU, 228.3 MiB
- PostgreSQL active connections observed: 4
- Slow-query sample: status distribution grouped over 10,000 requests in 2.118 ms.

## Acceptance

- No unbounded list endpoint.
- No pool exhaustion.
- No connection leak.
- No sustained error rate.
- Dashboard queries remain indexed and bounded.
- Notification worker throughput remains stable under backlog.
