param(
    [string]$MarkdownPath = "docs/company-submission-portfolio.md",
    [string]$CssPath = "docs/pdf-print-style.css",
    [string]$HtmlPath = "docs/Yeni_Backoffice_Portfolio.html",
    [string]$PdfPath = "docs/Yeni_Backoffice_Portfolio.pdf"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$markdownFile = Join-Path $root $MarkdownPath
$cssFile = Join-Path $root $CssPath
$htmlFile = Join-Path $root $HtmlPath
$pdfFile = Join-Path $root $PdfPath

function Encode-Html([string]$value) {
    return [System.Net.WebUtility]::HtmlEncode($value)
}

function Format-Inline([string]$value) {
    $text = Encode-Html $value
    $text = [regex]::Replace($text, '`([^`]+)`', '<code>$1</code>')
    $text = [regex]::Replace($text, '\*\*([^*]+)\*\*', '<strong>$1</strong>')
    $text = [regex]::Replace($text, '\[([^\]]+)\]\(([^)]+)\)', '<a href="$2">$1</a>')
    return $text
}

$lines = Get-Content -LiteralPath $markdownFile -Encoding UTF8
$body = [System.Text.StringBuilder]::new()
$inCode = $false
$codeLines = [System.Collections.Generic.List[string]]::new()
$inList = $false
$inTable = $false
$tableLines = [System.Collections.Generic.List[string]]::new()

function Close-List {
    if ($script:inList) {
        [void]$script:body.AppendLine("</ul>")
        $script:inList = $false
    }
}

function Write-Table {
    if (-not $script:inTable) {
        return
    }
    [void]$script:body.AppendLine("<table>")
    for ($rowIndex = 0; $rowIndex -lt $script:tableLines.Count; $rowIndex++) {
        if ($rowIndex -eq 1 -and $script:tableLines[$rowIndex] -match '^\|?[\s:-]+\|') {
            continue
        }
        $cells = $script:tableLines[$rowIndex].Trim().Trim('|').Split('|')
        $tag = if ($rowIndex -eq 0) { "th" } else { "td" }
        [void]$script:body.AppendLine("<tr>")
        foreach ($cell in $cells) {
            [void]$script:body.AppendLine("<$tag>$(Format-Inline $cell.Trim())</$tag>")
        }
        [void]$script:body.AppendLine("</tr>")
    }
    [void]$script:body.AppendLine("</table>")
    $script:tableLines.Clear()
    $script:inTable = $false
}

foreach ($line in $lines) {
    if ($line -match '^```') {
        Close-List
        Write-Table
        if ($inCode) {
            [void]$body.AppendLine("<pre><code>$(Encode-Html ($codeLines -join "`n"))</code></pre>")
            $codeLines.Clear()
            $inCode = $false
        } else {
            $inCode = $true
        }
        continue
    }

    if ($inCode) {
        $codeLines.Add($line)
        continue
    }

    if ($line -match '^\|.*\|$') {
        Close-List
        $inTable = $true
        $tableLines.Add($line)
        continue
    }
    Write-Table

    if ($line -eq '<div class="page-break"></div>') {
        Close-List
        [void]$body.AppendLine($line)
        continue
    }

    if ([string]::IsNullOrWhiteSpace($line)) {
        Close-List
        continue
    }

    if ($line -match '^(#{1,3})\s+(.+)$') {
        Close-List
        $level = $matches[1].Length
        [void]$body.AppendLine("<h$level>$(Format-Inline $matches[2])</h$level>")
        continue
    }

    if ($line -match '^>\s*(.+)$') {
        Close-List
        [void]$body.AppendLine("<blockquote>$(Format-Inline $matches[1])</blockquote>")
        continue
    }

    if ($line -match '^\s*[-*]\s+(.+)$') {
        if (-not $inList) {
            [void]$body.AppendLine("<ul>")
            $inList = $true
        }
        [void]$body.AppendLine("<li>$(Format-Inline $matches[1])</li>")
        continue
    }

    if ($line -match '^\s*\d+\.\s+(.+)$') {
        if (-not $inList) {
            [void]$body.AppendLine("<ul>")
            $inList = $true
        }
        [void]$body.AppendLine("<li>$(Format-Inline $matches[1])</li>")
        continue
    }

    Close-List
    [void]$body.AppendLine("<p>$(Format-Inline $line)</p>")
}

Close-List
Write-Table

$css = Get-Content -LiteralPath $cssFile -Raw -Encoding UTF8
$html = @"
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <title>Yeni Backoffice Portfolio</title>
  <style>
$css
  </style>
</head>
<body>
$body
</body>
</html>
"@

[System.IO.File]::WriteAllText($htmlFile, $html, [System.Text.UTF8Encoding]::new($false))

$browserCandidates = @(
    "C:\Program Files\Google\Chrome\Application\chrome.exe",
    "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
)
$browser = $browserCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $browser) {
    throw "Chrome 또는 Edge를 찾지 못했습니다."
}

$htmlUri = ([System.Uri]$htmlFile).AbsoluteUri
& $browser --headless --disable-gpu --no-pdf-header-footer "--print-to-pdf=$pdfFile" $htmlUri

$deadline = [DateTime]::UtcNow.AddSeconds(15)
while (-not (Test-Path -LiteralPath $pdfFile) -and [DateTime]::UtcNow -lt $deadline) {
    Start-Sleep -Milliseconds 250
}

if (-not (Test-Path -LiteralPath $pdfFile)) {
    throw "PDF 파일 생성에 실패했습니다."
}

Get-Item -LiteralPath $htmlFile, $pdfFile | Select-Object FullName, Length, LastWriteTime
