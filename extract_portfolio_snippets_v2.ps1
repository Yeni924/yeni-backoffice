# extract_portfolio_snippets_v2.ps1
# Run from project root. Creates docs/portfolio-code-snippets.md

$Root = Get-Location
$OutputDir = Join-Path $Root 'docs'
$OutputFile = Join-Path $OutputDir 'portfolio-code-snippets.md'
$ExcludePattern = '\\(\.git|\.gradle|build|out|target|node_modules|\.idea|\.vscode)\\'
$Fence = '```java'
$FenceEnd = '```'

$Targets = @(
    [pscustomobject]@{
        Title = '01 PaymentGateway interface'
        FileName = 'PaymentGateway.java'
        Markers = @('interface PaymentGateway')
        MaxLines = 60
        Note = 'PG adapter interface. A new PG provider can be added by implementing this interface.'
    },
    [pscustomobject]@{
        Title = '02 PaymentGatewayRegistry'
        FileName = 'PaymentGatewayRegistry.java'
        Markers = @('class PaymentGatewayRegistry', 'EnumMap<PgProvider, PaymentGateway>')
        MaxLines = 80
        Note = 'Registers PaymentGateway implementations by PgProvider and routes calls without changing service logic.'
    },
    [pscustomobject]@{
        Title = '03 Payment approve idempotency'
        FileName = 'PaymentApproveService.java'
        Markers = @('findByApprovalRequestKey', 'IDEMPOTENT_REPLAY', 'idempotencyKey')
        MaxLines = 100
        Note = 'Prevents duplicate approve requests by approvalRequestKey/orderNo and returns an existing result without calling PG again.'
    },
    [pscustomobject]@{
        Title = '04 Payment approve gateway branch'
        FileName = 'PaymentApproveService.java'
        Markers = @('gateway.approve', 'PaymentApproveResult', 'ApproveResult')
        MaxLines = 120
        Note = 'Calls the selected PG gateway and branches SUCCESS, UNKNOWN, and FAILED approval results.'
    },
    [pscustomobject]@{
        Title = '05 Approve unknown and recovery task'
        FileName = 'PaymentApproveService.java'
        Markers = @('APPROVE_UNKNOWN', 'APPROVE_UNKNOWN_CHECK', 'RecoveryTask')
        MaxLines = 120
        Note = 'Does not treat PG timeout/unknown as a final failure. Saves unknown state and records a recovery task.'
    },
    [pscustomobject]@{
        Title = '06 Network cancel required recovery'
        FileName = 'PaymentApproveService.java'
        Markers = @('NETWORK_CANCEL_REQUIRED', 'NETWORK_CANCEL', 'APPROVE_INTERNAL_SAVE_FAILED')
        MaxLines = 120
        Note = 'Handles the case where PG approve succeeded but internal processing failed, leaving a recovery/network-cancel task.'
    },
    [pscustomobject]@{
        Title = '07 Payment cancel idempotency and validation'
        FileName = 'PaymentCancelService.java'
        Markers = @('cancelRequestKey', 'findByCancelRequestKey', 'refundable')
        MaxLines = 120
        Note = 'Prevents duplicate cancel requests and validates refundable amount before calling PG cancel.'
    },
    [pscustomobject]@{
        Title = '08 Payment cancel gateway branch'
        FileName = 'PaymentCancelService.java'
        Markers = @('gateway.cancel', 'CANCEL_UNKNOWN', 'PaymentCancelResult')
        MaxLines = 120
        Note = 'Branches SUCCESS, UNKNOWN, and FAILED cancel results and creates recovery data when result is unknown.'
    },
    [pscustomobject]@{
        Title = '09 SALE ledger creation'
        FileName = 'SalesLedgerService.java'
        Markers = @('createSales', 'SaleType.SALE', 'findBySourceTypeAndSourceId')
        MaxLines = 100
        Note = 'Creates SALE ledger only once for an approved payment using sourceType/sourceId duplicate protection.'
    },
    [pscustomobject]@{
        Title = '10 CANCEL ledger creation'
        FileName = 'SalesLedgerService.java'
        Markers = @('SaleType.CANCEL', 'CANCEL', 'originalSalesTransactionId')
        MaxLines = 110
        Note = 'Creates a negative CANCEL ledger without modifying the original SALE ledger.'
    },
    [pscustomobject]@{
        Title = '11 External send follow-up'
        FileName = 'ExternalSendService.java'
        Markers = @('ExternalSendStatus.READY', 'createExternal', 'requestKey', 'DataIntegrityViolationException')
        MaxLines = 110
        Note = 'Creates external send work after confirmed payment/ledger while keeping it separate from core payment result.'
    },
    [pscustomobject]@{
        Title = '12 Alimtalk notification queue'
        FileName = 'PaymentNotificationService.java'
        Markers = @('AlimtalkQueue', 'messageKey', 'Notification', 'READY')
        MaxLines = 110
        Note = 'Creates notification queue items after payment events so notification failures do not roll back payment result.'
    },
    [pscustomobject]@{
        Title = '13 Recovery task recorder'
        FileName = 'RecoveryTaskRecorder.java'
        Markers = @('REQUIRES_NEW', 'RecoveryTask', 'saveAndFlush', 'taskKey')
        MaxLines = 110
        Note = 'Persists recovery evidence in an independent transaction so it can survive outer transaction failure.'
    },
    [pscustomobject]@{
        Title = '14 Payment recovery service'
        FileName = 'PaymentRecoveryService.java'
        Markers = @('RecoveryTask', 'retry', 'PROCESSING', 'SUCCESS')
        MaxLines = 120
        Note = 'Handles recovery/retry operations for unknown or failed payment-related tasks.'
    },
    [pscustomobject]@{
        Title = '15 Settlement batch from ledger'
        FileName = 'SettlementBatchProcessor.java'
        Markers = @('SettlementStatementResponse process', 'findByBusinessDateAndSettlementIncludedYnFalse', 'markIncludedInSettlement')
        MaxLines = 140
        Note = 'Creates settlement statement from SALE/CANCEL ledger and marks included sales to prevent duplicate settlement.'
    },
    [pscustomobject]@{
        Title = '16 API exception advice'
        FileName = 'ApiExceptionAdvice.java'
        Markers = @('class ApiExceptionAdvice', 'handleBusinessException', 'fieldErrors')
        MaxLines = 120
        Note = 'Standardizes API error responses with ErrorCode, requestId, and fieldErrors.'
    },
    [pscustomobject]@{
        Title = '17 RequestId filter'
        FileName = 'RequestIdFilter.java'
        Markers = @('class RequestIdFilter', 'X-Request-Id', 'MDC')
        MaxLines = 90
        Note = 'Adds requestId to MDC and response header for log tracing.'
    }
)

function Find-SourceFile {
    param([string]$FileName)

    $files = Get-ChildItem -Path $Root -Recurse -Filter $FileName -File -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch $ExcludePattern } |
        Sort-Object @{ Expression = { if ($_.FullName -match '\\src\\main\\') { 0 } else { 1 } } }, FullName

    return $files | Select-Object -First 1
}

function Find-MarkerLine {
    param(
        [string[]]$Lines,
        [string[]]$Markers
    )

    for ($i = 0; $i -lt $Lines.Count; $i++) {
        foreach ($marker in $Markers) {
            if ($Lines[$i].Contains($marker)) {
                return [pscustomobject]@{ Index = $i; Marker = $marker }
            }
        }
    }
    return $null
}

function Find-BlockStartLine {
    param(
        [string[]]$Lines,
        [int]$MarkerLine
    )

    $start = $MarkerLine
    $min = [Math]::Max(0, $MarkerLine - 45)

    for ($i = $MarkerLine; $i -ge $min; $i--) {
        $t = $Lines[$i].Trim()

        if ($t.StartsWith('@')) {
            $start = $i
            continue
        }

        if ($t -match '^(public|private|protected)\s+' -or
            $t -match '^(class|interface)\s+' -or
            $t -match '^@Transactional' -or
            $t -match '^@Override') {
            $start = $i
        }

        if (($t -match '^(public|private|protected)\s+.*\)' -or $t -match '^(public|private|protected)\s+.*\{') -and $i -le $MarkerLine) {
            $start = $i
            break
        }
    }

    return $start
}

function Extract-Snippet {
    param(
        [string[]]$Lines,
        [int]$Start,
        [int]$MaxLines
    )

    $end = [Math]::Min($Lines.Count - 1, $Start + $MaxLines - 1)
    $depth = 0
    $started = $false

    for ($i = $Start; $i -lt $Lines.Count; $i++) {
        $line = $Lines[$i]
        $open = ([regex]::Matches($line, '\{')).Count
        $close = ([regex]::Matches($line, '\}')).Count

        if ($open -gt 0) {
            $started = $true
        }

        if ($started) {
            $depth += $open
            $depth -= $close

            if ($depth -le 0 -and $i -gt $Start) {
                $end = $i
                break
            }
        }

        if (($i - $Start + 1) -ge $MaxLines) {
            $end = $i
            break
        }
    }

    return [pscustomobject]@{
        Start = $Start
        End = $end
        Code = ($Lines[$Start..$end] -join "`n")
    }
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$Md = New-Object System.Collections.Generic.List[string]
$Md.Add('# Portfolio Code Snippets')
$Md.Add('')
$Md.Add('Extracted from actual implementation files.')
$Md.Add('')

foreach ($target in $Targets) {
    $Md.Add('## ' + $target.Title)
    $Md.Add('')
    $Md.Add('- Note: ' + $target.Note)

    $file = Find-SourceFile $target.FileName
    if ($null -eq $file) {
        $Md.Add('- File not found: ' + $target.FileName)
        $Md.Add('')
        continue
    }

    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $lines = $content -split "`r?`n"
    $markerResult = Find-MarkerLine -Lines $lines -Markers $target.Markers

    $relative = Resolve-Path -Path $file.FullName -Relative
    $Md.Add('- File: ' + $relative)

    if ($null -eq $markerResult) {
        $Md.Add('- Marker not found. Tried: ' + ($target.Markers -join ', '))
        $Md.Add('')
        continue
    }

    $Md.Add('- Marker: ' + $markerResult.Marker)
    $start = Find-BlockStartLine -Lines $lines -MarkerLine $markerResult.Index
    $snippet = Extract-Snippet -Lines $lines -Start $start -MaxLines $target.MaxLines

    $Md.Add('- Lines: ' + ($snippet.Start + 1) + '-' + ($snippet.End + 1))
    $Md.Add('')
    $Md.Add($Fence)
    $Md.Add($snippet.Code.TrimEnd())
    $Md.Add($FenceEnd)
    $Md.Add('')
}

$Md -join "`n" | Set-Content -Path $OutputFile -Encoding UTF8

Write-Host ''
Write-Host '[OK] Portfolio code snippets extracted:'
Write-Host $OutputFile
