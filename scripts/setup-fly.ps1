$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=== Fly.io preflight check started ===" -ForegroundColor Cyan

$currentBranch = git branch --show-current
Write-Host "Current branch: $currentBranch"

if ($currentBranch -eq "main") {
    Write-Host "Warning: you are working directly on main. Use feature/flyio-deploy-config for deploy config changes." -ForegroundColor Yellow
}

if (-not (Test-Path ".\Dockerfile")) {
    throw "Dockerfile is missing. Add Fly.io deploy config first."
}

if (-not (Test-Path ".\fly.toml") -and -not (Test-Path ".\fly.toml.example")) {
    throw "fly.toml or fly.toml.example is missing."
}

$flyProfilePath = ".\api\src\main\resources\application-fly.yml"
if (-not (Test-Path $flyProfilePath)) {
    throw "application-fly.yml is missing."
}
Write-Host "Fly profile found: $flyProfilePath" -ForegroundColor Green

Write-Host "Running Gradle build..." -ForegroundColor Cyan
.\gradlew.bat build
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed."
}

$docker = Get-Command docker -ErrorAction SilentlyContinue
if ($docker) {
    Write-Host "Docker found. Running local Docker build..." -ForegroundColor Cyan
    docker build -t yeni-backoffice-demo .
    if ($LASTEXITCODE -ne 0) {
        throw "Docker build failed."
    }
} else {
    Write-Host "Docker is not installed. Skipping local Docker build." -ForegroundColor Yellow
    Write-Host "You can deploy with Fly.io remote builder: fly deploy --remote-only" -ForegroundColor Yellow
}

$fly = Get-Command fly -ErrorAction SilentlyContinue
if ($fly) {
    Write-Host "Fly CLI found." -ForegroundColor Green
    fly version
} else {
    Write-Host "Fly CLI is not installed. Install Fly CLI and run: fly auth login" -ForegroundColor Yellow
}

Write-Host "=== Fly.io preflight check completed ===" -ForegroundColor Green
