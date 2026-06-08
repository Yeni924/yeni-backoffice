param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=== Fly.io deploy started ===" -ForegroundColor Cyan

$currentBranch = git branch --show-current
Write-Host "Current branch: $currentBranch"

if (-not (Test-Path ".\fly.toml")) {
    Write-Host "fly.toml is missing." -ForegroundColor Yellow
    Write-Host "Create app config first:"
    Write-Host "fly launch --no-deploy"
    exit 1
}

$fly = Get-Command fly -ErrorAction SilentlyContinue
if (-not $fly) {
    throw "Fly CLI is not installed."
}

Write-Host "Checking Fly auth..." -ForegroundColor Cyan
fly auth whoami

if (-not $SkipBuild) {
    Write-Host "Running Gradle build..." -ForegroundColor Cyan
    .\gradlew.bat build
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed."
    }
}

$confirm = Read-Host "Deploy to Fly.io with remote builder? (y/N)"
if ($confirm -ne "y" -and $confirm -ne "Y") {
    Write-Host "Deploy canceled."
    exit 0
}

fly deploy --remote-only
if ($LASTEXITCODE -ne 0) {
    Write-Host "Deploy failed. Check logs with:" -ForegroundColor Red
    Write-Host "fly logs"
    exit 1
}

Write-Host "Deploy completed!" -ForegroundColor Green
Write-Host "Status: fly status"
Write-Host "Logs: fly logs"
Write-Host "Open app: fly apps open"

fly status
