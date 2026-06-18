# SQL Server to PostgreSQL Data Migration

This guide migrates data from SQL Server (source) to PostgreSQL (target) table-by-table.

## 1) Type Mapping

Use the following data type mapping when validating target schema:

- SQL Server `int` -> PostgreSQL `integer`
- SQL Server `bit` -> PostgreSQL `boolean`
- SQL Server `datetime` -> PostgreSQL `timestamp`
- SQL Server `nvarchar(n)` / `varchar(n)` -> PostgreSQL `varchar(n)`
- SQL Server `nvarchar(max)` -> PostgreSQL `text`
- SQL Server `decimal(18,0)` -> PostgreSQL `numeric(18,0)`
- SQL Server `IDENTITY` -> PostgreSQL `GENERATED ... AS IDENTITY` or `sequence + default nextval(...)`

## 2) Prerequisites

- Source SQL Server is reachable.
- Target PostgreSQL schema already exists and matches app entities.
- Tools installed and available in PATH:
  - `bcp`
  - `psql`

## 2.1) Additional Schema Scripts

Before running backend with dynamic admin settings, apply:

```powershell
psql -h localhost -U postgres -d KLTN -f .\scripts\add-system-settings-postgres.sql
```

This script is idempotent and also adds the `description` column if your `system_settings` table already exists.

## 3) One-command Migration Script

From project root (`demo/demo`), run:

```powershell
.\scripts\migrate-sqlserver-to-postgres.cmd -TruncateTarget
```

Set passwords via environment variables (recommended):

```powershell
$env:SQL_PASSWORD = "your_sql_password"
$env:PGPASSWORD = "your_pg_password"
```

Optional arguments:

```powershell
.\scripts\migrate-sqlserver-to-postgres.cmd \
  -SqlServer "localhost,1433" \
  -SqlDatabase "KLTN" \
  -SqlUsername "sa" \
  -PgHost "localhost" \
  -PgPort 5432 \
  -PgDatabase "kltn" \
  -PgUsername "postgres" \
  -TruncateTarget
```

If you prefer passing passwords directly, use SecureString:

```powershell
.\scripts\migrate-sqlserver-to-postgres.ps1 \
  -SqlSecret (ConvertTo-SecureString "your_sql_password" -AsPlainText -Force) \
  -PgSecret (ConvertTo-SecureString "your_pg_password" -AsPlainText -Force)
```

Useful modes:

- Export only: `-SkipImport`
- Import only from existing TSV files: `-SkipExport`

Export files are stored at `migration-data/export`.

## 4) Import Order (per table)

The script imports in FK-safe order:

1. `users`
2. `patients`
3. `rooms`
4. `medicines`
5. `services`
6. `appointments`
7. `medical_records`
8. `invoices`
9. `medical_record_services`
10. `prescription_details`

## 5) Manual Import Commands (per table)

If you need to import manually in PostgreSQL:

```sql
\copy public.users (id, username, password, role, is_active) FROM 'migration-data/export/users.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
\copy public.patients (id, user_id, full_name, gender, national_id, health_insurance_number, phone_number, gmail) FROM 'migration-data/export/patients.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
\copy public.rooms (id, room_name, current_doctor_id) FROM 'migration-data/export/rooms.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
\copy public.medicines (id, medicine_name, unit, selling_price, stock_quantity, is_active) FROM 'migration-data/export/medicines.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
\copy public.services (id, service_name, current_price, is_active) FROM 'migration-data/export/services.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
\copy public.appointments (id, patient_id, doctor_id, appointment_time, status, symptoms) FROM 'migration-data/export/appointments.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
\copy public.medical_records (id, appointment_id, diagnosis, doctor_advice, created_at) FROM 'migration-data/export/medical_records.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
\copy public.invoices (id, medical_record_id, total_service_fee, total_medicine_fee, total_amount, is_paid, paid_at, payment_method) FROM 'migration-data/export/invoices.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
\copy public.medical_record_services (medical_record_id, service_id, quantity, actual_price, result_note) FROM 'migration-data/export/medical_record_services.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
\copy public.prescription_details (medical_record_id, medicine_id, quantity, usage_instructions) FROM 'migration-data/export/prescription_details.tsv' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8');
```

Then reset sequences for identity-like tables:

```sql
SELECT setval(pg_get_serial_sequence('public.users', 'id'), COALESCE((SELECT MAX(id) FROM public.users), 1), (SELECT COUNT(*) > 0 FROM public.users));
SELECT setval(pg_get_serial_sequence('public.patients', 'id'), COALESCE((SELECT MAX(id) FROM public.patients), 1), (SELECT COUNT(*) > 0 FROM public.patients));
SELECT setval(pg_get_serial_sequence('public.rooms', 'id'), COALESCE((SELECT MAX(id) FROM public.rooms), 1), (SELECT COUNT(*) > 0 FROM public.rooms));
SELECT setval(pg_get_serial_sequence('public.medicines', 'id'), COALESCE((SELECT MAX(id) FROM public.medicines), 1), (SELECT COUNT(*) > 0 FROM public.medicines));
SELECT setval(pg_get_serial_sequence('public.services', 'id'), COALESCE((SELECT MAX(id) FROM public.services), 1), (SELECT COUNT(*) > 0 FROM public.services));
SELECT setval(pg_get_serial_sequence('public.appointments', 'id'), COALESCE((SELECT MAX(id) FROM public.appointments), 1), (SELECT COUNT(*) > 0 FROM public.appointments));
SELECT setval(pg_get_serial_sequence('public.medical_records', 'id'), COALESCE((SELECT MAX(id) FROM public.medical_records), 1), (SELECT COUNT(*) > 0 FROM public.medical_records));
SELECT setval(pg_get_serial_sequence('public.invoices', 'id'), COALESCE((SELECT MAX(id) FROM public.invoices), 1), (SELECT COUNT(*) > 0 FROM public.invoices));
```
