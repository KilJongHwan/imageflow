param(
    [string]$EnvFile = ".env.render-local-worker"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$envPath = Join-Path $scriptDir $EnvFile

if (-not (Test-Path $envPath)) {
    Write-Error "Env file not found: $envPath"
    Write-Host "Copy .env.render-local-worker.example to .env.render-local-worker and fill in the Render values."
    exit 1
}

Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) {
        return
    }

    $parts = $line -split "=", 2
    if ($parts.Length -ne 2) {
        return
    }

    $key = $parts[0].Trim()
    $value = $parts[1].Trim()
    [Environment]::SetEnvironmentVariable($key, $value, "Process")
}

if (-not $env:REDIS_URL) {
    Write-Error "REDIS_URL is missing in $envPath"
    exit 1
}

if (-not $env:BACKEND_BASE_URL) {
    Write-Error "BACKEND_BASE_URL is missing in $envPath"
    exit 1
}

Write-Host "Starting local Render worker"
Write-Host "REDIS_URL=$($env:REDIS_URL)"
Write-Host "BACKEND_BASE_URL=$($env:BACKEND_BASE_URL)"
Write-Host "IMAGE_JOB_QUEUE_KEY=$($env:IMAGE_JOB_QUEUE_KEY)"
Write-Host "WORKER_CONCURRENCY=$($env:WORKER_CONCURRENCY)"

Push-Location $scriptDir
try {
    python worker.py
}
finally {
    Pop-Location
}
