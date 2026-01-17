# Request UUID Pattern Documentation

## For ESB Team / API Support

This document explains the UUID pattern used by the **SIB ETB Web Portal** application for all API requests.

## UUID Format

**Pattern:** `WPYYMMDDHHMMSSRR`

**Total Length:** 16 characters

### Breakdown

| Component | Length | Description | Example |
|-----------|--------|-------------|---------|
| **WP** | 2 chars | Application Identifier (Web Portal) | `WP` |
| **YYMMDDHHMMSS** | 12 chars | Request Timestamp | `260117163000` |
| **RR** | 2 chars | Random Suffix (for uniqueness) | `45` |

### Complete Example

**UUID:** `WP26011716300045`

Decoded:
- **WP**: Request from Web Portal application
- **260117**: Date = January 17, 2026
- **163000**: Time = 4:30:00 PM (16:30:00)
- **45**: Random suffix

## How to Use This Pattern

### 1. Identifying Web Portal Requests

All requests from the Web Portal start with `WP`. Use this to filter logs:

```bash
# Search for all Web Portal requests
grep "WP" api-logs.txt

# Search for specific request
grep "WP26011716300045" api-logs.txt
```

### 2. Finding Requests by Time Range

The timestamp format is `YYMMDDHHMMSS`, making it easy to search by date/time:

```bash
# All requests on January 17, 2026
grep "WP260117" api-logs.txt

# All requests between 4:00 PM and 5:00 PM on Jan 17, 2026
grep "WP26011716" api-logs.txt

# Specific hour and minute (4:30 PM)
grep "WP2601171630" api-logs.txt
```

### 3. Troubleshooting Customer Issues

When a customer reports an issue:
1. Note the approximate time of the issue
2. Search logs using the pattern: `WP + YYMMDD + HHMM`
3. Look for error responses or exceptions

**Example:**
Customer reports login failed at 4:30 PM on Jan 17, 2026:
```bash
grep "WP2601171630" api-logs.txt | grep -i "error\|exception\|fail"
```

### 4. Tracking Request Flow

Use the UUID to track a request across multiple systems:

```bash
# Find the request in ESB logs
grep "WP26011716300045" esb-logs.txt

# Find in backend system logs
grep "WP26011716300045" backend-logs.txt

# Find in database query logs
grep "WP26011716300045" db-logs.txt
```

## Timestamp Conversion

### Decoding Timestamp

| Format | Position | Example | Meaning |
|--------|----------|---------|---------|
| YY | Chars 3-4 | 26 | Year 2026 |
| MM | Chars 5-6 | 01 | January |
| DD | Chars 7-8 | 17 | 17th day |
| HH | Chars 9-10 | 16 | 4 PM (24-hour) |
| MM | Chars 11-12 | 30 | 30 minutes |
| SS | Chars 13-14 | 00 | 00 seconds |

### Quick Reference Table

| Time | UUID Pattern | Example |
|------|--------------|---------|
| Jan 17, 2026 @ 9:00 AM | WP260117090000XX | WP26011709000023 |
| Jan 17, 2026 @ 12:30 PM | WP260117123000XX | WP26011712300067 |
| Jan 17, 2026 @ 4:45 PM | WP260117164500XX | WP26011716450089 |
| Jan 17, 2026 @ 11:59 PM | WP260117235900XX | WP26011723590012 |

## Log Patterns to Monitor

### Success Pattern
```
UUID: WP26011716300045 | Status: 200 | Response Time: 234ms
```

### Error Pattern
```
UUID: WP26011716300045 | Status: 500 | Error: Connection timeout
```

### Request-Response Correlation
```
[REQUEST]  UUID: WP26011716300045 | Endpoint: /customer/registration
[RESPONSE] UUID: WP26011716300045 | Status: 200 | Duration: 234ms
```

## Common Queries

### Find all requests from Web Portal today
```bash
TODAY=$(date +%y%m%d)
grep "WP${TODAY}" api-logs.txt
```

### Find requests in last hour
```bash
HOUR=$(date -d '1 hour ago' +%y%m%d%H)
grep "WP${HOUR}" api-logs.txt
```

### Count requests per hour
```bash
grep "WP260117" api-logs.txt | cut -c 3-12 | uniq -c
```

### Find slow requests (if response time is logged)
```bash
grep "WP" api-logs.txt | grep -E "Response Time: [0-9]{4,}ms"
```

## Integration with Monitoring Tools

### Splunk Query
```
index=api_logs UUID=WP* | stats count by UUID | sort -count
```

### ELK/Kibana Query
```json
{
  "query": {
    "wildcard": {
      "uuid": "WP*"
    }
  }
}
```

### Grafana Alert Rule
```
count(uuid{application="web-portal"}) by (status_code)
```

## SLA Tracking

Use the timestamp to calculate:
- Request processing time
- Time to first byte (TTFB)
- End-to-end latency

**Example:**
```
UUID: WP26011716300045
Request Time: 2026-01-17 16:30:00.456
Response Time: 2026-01-17 16:30:00.690
Processing Duration: 234ms ✓ (within SLA)
```

## Application Identifier Codes

| Code | Application | Description |
|------|-------------|-------------|
| **WP** | Web Portal | ETB Portal web application (current) |
| MB | Mobile Banking | Mobile app (if implemented) |
| IB | Internet Banking | Legacy internet banking (if applicable) |
| AT | ATM | ATM channel (if integrated) |

**Note:** Currently, only `WP` (Web Portal) is active.

## API Endpoints Using This Pattern

All encrypted API calls from Web Portal use this UUID pattern:

1. Customer Registration API
2. Account Balance API
3. Transaction History API
4. Fund Transfer API
5. Card Services API
6. OTP Generation API
7. KYC Verification API

## Support Contact

For questions or issues related to Web Portal API requests:

- **Development Team:** [Your Team Email]
- **Support Ticket System:** [Your Support URL]
- **Escalation:** [Manager/Lead Contact]

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-17 | Initial UUID pattern implementation |

---

**Note:** Keep this document updated when:
- New application identifiers are added
- UUID format changes
- New API endpoints are integrated
