param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$normalizedBaseUrl = $BaseUrl.TrimEnd("/")
$paths = @(
    "/",
    "/admin/login",
    "/admin/payment-operations",
    "/admin/sales-ledger",
    "/admin/settlements",
    "/admin/database-spec"
)

Write-Host "=== Fly.io URL check started ===" -ForegroundColor Cyan
Write-Host "BaseUrl: $normalizedBaseUrl"

foreach ($path in $paths) {
    $url = "$normalizedBaseUrl$path"
    try {
        $response = Invoke-WebRequest -Uri $url -Method GET -UseBasicParsing -TimeoutSec 45
        Write-Host "PASS $url => $($response.StatusCode)" -ForegroundColor Green
    } catch {
        Write-Host "FAIL $url" -ForegroundColor Red
        Write-Host $_.Exception.Message
    }
}

Write-Host "=== URL check completed ===" -ForegroundColor Green
