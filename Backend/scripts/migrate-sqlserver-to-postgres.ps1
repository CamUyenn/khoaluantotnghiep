#pragma warning disable PSUseApprovedVerbs
#pragma warning disable PSAvoidUsingPlainTextForPassword
param(
    [string]$SqlServer = "localhost,1433",
    [string]$SqlDatabase = "KLTN",
    [string]$SqlUsername = "sa",
    [securestring]$SqlSecret,
    [string]$PgHost = "localhost",
    [int]$PgPort = 5432,
    [string]$PgDatabase = "KLTN",
    [string]$PgUsername = "postgres",
    [securestring]$PgSecret,
    [string]$WorkDir = "./migration-data",
    [switch]$SkipExport,
    [switch]$SkipImport,
    [switch]$TruncateTarget
)

$ErrorActionPreference = "Stop"

function Get-RequiredCommand {
    param([string]$Name)
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "Command '$Name' is required but was not found in PATH."
    }
}

function ConvertTo-PlainText {
    param([securestring]$Value)

    if ($null -eq $Value) {
        return $null
    }

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Invoke-External {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$FailureMessage
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FailureMessage (exit code: $LASTEXITCODE)"
    }
}

function Test-SqlColumnExists {
    param(
        [string]$TableName,
        [string]$ColumnName,
        [string]$Server,
        [string]$Database,
        [string]$Username,
        [string]$Password
    )

    $query = "SET NOCOUNT ON; SELECT CASE WHEN COL_LENGTH('dbo.$TableName','$ColumnName') IS NULL THEN 0 ELSE 1 END;"
    $output = sqlcmd -S $Server -d $Database -U $Username -P $Password -Q $query -h -1 -W
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to detect column '$TableName.$ColumnName' in SQL Server"
    }

    $flag = ($output | Where-Object { $_ -match '^[01]$' } | Select-Object -First 1)
    return $flag -eq '1'
}

$tableConfigs = @(
    [pscustomobject]@{
        Name        = "users"
        Columns     = @("id", "username", "password", "role", "is_active")
        SourceQuery = "SELECT id, username, [password], [role], CAST(is_active AS INT) AS is_active FROM dbo.USERS ORDER BY id"
    },
    [pscustomobject]@{
        Name        = "patients"
        Columns     = @("id", "user_id", "full_name", "gender", "national_id", "health_insurance_number", "phone_number", "gmail")
        SourceQuery = "SELECT id, user_id, full_name, CAST(gender AS NVARCHAR(10)) AS gender, national_id, health_insurance_number, phone_number, gmail FROM dbo.PATIENTS ORDER BY id"
    },
    [pscustomobject]@{
        Name        = "rooms"
        Columns     = @("id", "room_name", "current_doctor_id")
        SourceQuery = "SELECT id, room_name, current_doctor_id FROM dbo.ROOMS ORDER BY id"
    },
    [pscustomobject]@{
        Name        = "medicines"
        Columns     = @("id", "medicine_name", "unit", "selling_price", "stock_quantity", "is_active")
        SourceQuery = "SELECT id, medicine_name, unit, selling_price, stock_quantity, CAST(is_active AS INT) AS is_active FROM dbo.MEDICINES ORDER BY id"
    },
    [pscustomobject]@{
        Name        = "services"
        Columns     = @("id", "service_name", "current_price", "is_active")
        SourceQuery = "SELECT id, service_name, current_price, CAST(is_active AS INT) AS is_active FROM dbo.SERVICES ORDER BY id"
    },
    [pscustomobject]@{
        Name        = "appointments"
        Columns     = @("id", "patient_id", "doctor_id", "appointment_time", "status", "symptoms")
        SourceQuery = "SELECT id, patient_id, doctor_id, CONVERT(VARCHAR(33), appointment_time, 126) AS appointment_time, status, symptoms FROM dbo.APPOINTMENTS ORDER BY id"
    },
    [pscustomobject]@{
        Name        = "medical_records"
        Columns     = @("id", "appointment_id", "diagnosis", "doctor_advice", "created_at")
        SourceQuery = "SELECT id, appointment_id, diagnosis, doctor_advice, CONVERT(VARCHAR(33), created_at, 126) AS created_at FROM dbo.MEDICAL_RECORDS ORDER BY id"
    },
    [pscustomobject]@{
        Name        = "invoices"
        Columns     = @("id", "medical_record_id", "total_service_fee", "total_medicine_fee", "total_amount", "is_paid", "paid_at", "payment_method")
        SourceQuery = "SELECT id, medical_record_id, total_service_fee, total_medicine_fee, total_amount, CAST(is_paid AS INT) AS is_paid, CONVERT(VARCHAR(33), paid_at, 126) AS paid_at, payment_method FROM dbo.INVOICES ORDER BY id"
    },
    [pscustomobject]@{
        Name        = "medical_record_services"
        Columns     = @("medical_record_id", "service_id", "quantity", "actual_price", "result_note")
        SourceQuery = "SELECT medical_record_id, service_id, quantity, actual_price, result_note FROM dbo.MEDICAL_RECORD_SERVICES ORDER BY medical_record_id, service_id"
    },
    [pscustomobject]@{
        Name        = "prescription_details"
        Columns     = @("medical_record_id", "medicine_id", "quantity", "usage_instructions")
        SourceQuery = "SELECT medical_record_id, medicine_id, quantity, usage_instructions FROM dbo.PRESCRIPTION_DETAILS ORDER BY medical_record_id, medicine_id"
    }
)

$rootPath = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$resolvedWorkDir = if ([System.IO.Path]::IsPathRooted($WorkDir)) {
    $WorkDir
}
else {
    Join-Path $rootPath $WorkDir
}

$exportDir = Join-Path $resolvedWorkDir "export"
New-Item -ItemType Directory -Path $exportDir -Force | Out-Null

$sqlPasswordText = ConvertTo-PlainText -Value $SqlSecret
if ([string]::IsNullOrWhiteSpace($sqlPasswordText)) {
    $sqlPasswordText = $env:SQL_PASSWORD
}
if (-not $SkipExport -and [string]::IsNullOrWhiteSpace($sqlPasswordText)) {
    throw "SQL Server password is required. Provide -SqlPassword or set SQL_PASSWORD environment variable."
}

$pgPasswordText = ConvertTo-PlainText -Value $PgSecret
if ([string]::IsNullOrWhiteSpace($pgPasswordText)) {
    $pgPasswordText = $env:PGPASSWORD
}
if (-not $SkipImport -and [string]::IsNullOrWhiteSpace($pgPasswordText)) {
    throw "PostgreSQL password is required. Provide -PgPassword or set PGPASSWORD environment variable."
}

if (-not $SkipExport) {
    Get-RequiredCommand -Name "bcp"
    Get-RequiredCommand -Name "sqlcmd"
}
if (-not $SkipImport) {
    Get-RequiredCommand -Name "psql"
}

if (-not $SkipExport) {
    $hasPatientGmail = Test-SqlColumnExists -TableName "PATIENTS" -ColumnName "gmail" -Server $SqlServer -Database $SqlDatabase -Username $SqlUsername -Password $sqlPasswordText
    $hasPatientHealthInsuranceNumber = Test-SqlColumnExists -TableName "PATIENTS" -ColumnName "health_insurance_number" -Server $SqlServer -Database $SqlDatabase -Username $SqlUsername -Password $sqlPasswordText
    $hasInvoicePaymentMethod = Test-SqlColumnExists -TableName "INVOICES" -ColumnName "payment_method" -Server $SqlServer -Database $SqlDatabase -Username $SqlUsername -Password $sqlPasswordText
    $hasMedicalRecordServiceResultNote = Test-SqlColumnExists -TableName "MEDICAL_RECORD_SERVICES" -ColumnName "result_note" -Server $SqlServer -Database $SqlDatabase -Username $SqlUsername -Password $sqlPasswordText

    $patientHealthInsuranceExpr = if ($hasPatientHealthInsuranceNumber) {
        "health_insurance_number"
    }
    else {
        "CAST(NULL AS NVARCHAR(20)) AS health_insurance_number"
    }

    $patientGmailExpr = if ($hasPatientGmail) {
        "gmail"
    }
    else {
        "CAST(NULL AS NVARCHAR(100)) AS gmail"
    }

    $invoicePaymentMethodExpr = if ($hasInvoicePaymentMethod) {
        "payment_method"
    }
    else {
        "CAST(NULL AS NVARCHAR(30)) AS payment_method"
    }

    $medicalRecordServiceResultNoteExpr = if ($hasMedicalRecordServiceResultNote) {
        "result_note"
    }
    else {
        "CAST(NULL AS NVARCHAR(MAX)) AS result_note"
    }

    ($tableConfigs | Where-Object { $_.Name -eq "patients" } | Select-Object -First 1).SourceQuery =
    "SELECT id, user_id, full_name, CAST(gender AS NVARCHAR(10)) AS gender, national_id, $patientHealthInsuranceExpr, phone_number, $patientGmailExpr FROM dbo.PATIENTS ORDER BY id"

    ($tableConfigs | Where-Object { $_.Name -eq "invoices" } | Select-Object -First 1).SourceQuery =
    "SELECT id, medical_record_id, total_service_fee, total_medicine_fee, total_amount, CAST(is_paid AS INT) AS is_paid, CONVERT(VARCHAR(33), paid_at, 126) AS paid_at, $invoicePaymentMethodExpr FROM dbo.INVOICES ORDER BY id"

    ($tableConfigs | Where-Object { $_.Name -eq "medical_record_services" } | Select-Object -First 1).SourceQuery =
    "SELECT medical_record_id, service_id, quantity, actual_price, $medicalRecordServiceResultNoteExpr FROM dbo.MEDICAL_RECORD_SERVICES ORDER BY medical_record_id, service_id"
}

if (-not $SkipExport) {
    Write-Output "Exporting data from SQL Server..."
    foreach ($config in $tableConfigs) {
        $outputFile = Join-Path $exportDir ("{0}.tsv" -f $config.Name)
        $queryArg = '"' + $config.SourceQuery + '"'
        $outputArg = '"' + $outputFile + '"'

        $bcpArgs = @(
            $queryArg,
            "queryout",
            $outputArg,
            "-S", $SqlServer,
            "-d", $SqlDatabase,
            "-U", $SqlUsername,
            "-P", $sqlPasswordText,
            "-c",
            "-C", "65001",
            "-t", "`t",
            "-r", "0x0a"
        )

        Write-Output ("  - Export {0} -> {1}" -f $config.Name, $outputFile)
        Invoke-External -FilePath "bcp" -Arguments $bcpArgs -FailureMessage ("Failed to export table '{0}'" -f $config.Name)
    }
}

if (-not $SkipImport) {
    Write-Output "Importing data into PostgreSQL..."

    $env:PGPASSWORD = $pgPasswordText

    if ($TruncateTarget) {
        $truncateSql = @"
TRUNCATE TABLE
    public.prescription_details,
    public.medical_record_services,
    public.invoices,
    public.medical_records,
    public.appointments,
    public.rooms,
    public.patients,
    public.medicines,
    public.services,
    public.users
RESTART IDENTITY CASCADE;
"@

        $truncateArgs = @(
            "-h", $PgHost,
            "-p", $PgPort,
            "-U", $PgUsername,
            "-d", $PgDatabase,
            "-v", "ON_ERROR_STOP=1",
            "-c", $truncateSql
        )

        Invoke-External -FilePath "psql" -Arguments $truncateArgs -FailureMessage "Failed to truncate PostgreSQL tables"
    }

    foreach ($config in $tableConfigs) {
        $inputFile = Join-Path $exportDir ("{0}.tsv" -f $config.Name)
        if (-not (Test-Path $inputFile)) {
            throw "Missing export file: $inputFile"
        }

        $pgPath = ($inputFile -replace "\\", "/")
        $columns = ($config.Columns -join ", ")
        $copyCommand = "\copy public.{0} ({1}) FROM '{2}' WITH (FORMAT csv, DELIMITER E'\t', NULL '', HEADER false, ENCODING 'UTF8')" -f $config.Name, $columns, $pgPath

        $importArgs = @(
            "-h", $PgHost,
            "-p", $PgPort,
            "-U", $PgUsername,
            "-d", $PgDatabase,
            "-v", "ON_ERROR_STOP=1",
            "-c", $copyCommand
        )

        Write-Output ("  - Import {0} <- {1}" -f $config.Name, $inputFile)
        Invoke-External -FilePath "psql" -Arguments $importArgs -FailureMessage ("Failed to import table '{0}'" -f $config.Name)
    }

    $sequenceTables = @("users", "patients", "rooms", "medicines", "services", "appointments", "medical_records", "invoices")
    foreach ($tableName in $sequenceTables) {
        $resetSql = @"
SELECT setval(
    pg_get_serial_sequence('public.$tableName', 'id'),
    COALESCE((SELECT MAX(id) FROM public.$tableName), 1),
    (SELECT COUNT(*) > 0 FROM public.$tableName)
);
"@
        $resetArgs = @(
            "-h", $PgHost,
            "-p", $PgPort,
            "-U", $PgUsername,
            "-d", $PgDatabase,
            "-v", "ON_ERROR_STOP=1",
            "-c", $resetSql
        )

        Invoke-External -FilePath "psql" -Arguments $resetArgs -FailureMessage ("Failed to reset sequence for '{0}'" -f $tableName)
    }

    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Output "Done."
Write-Output ("Export folder: {0}" -f $exportDir)
#pragma warning restore PSAvoidUsingPlainTextForPassword
#pragma warning restore PSUseApprovedVerbs
