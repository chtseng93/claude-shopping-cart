# make-demo2-gif.ps1
$ErrorActionPreference = 'Stop'

# 腳本在 e2e/scripts/，專案根在兩層上
$e2eDir   = Split-Path $PSScriptRoot -Parent
$rootDir  = Split-Path $e2eDir -Parent

$assetGif = Join-Path $rootDir 'assets\demo2.gif'
$docsGif  = Join-Path $rootDir 'docs\demo2.gif'

# 找最新的 demo-video2 錄影
$webm = Get-ChildItem -Path (Join-Path $e2eDir 'test-results') -Recurse -Filter 'video.webm' |
        Where-Object { $_.FullName -match 'demo-video2' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

if (-not $webm) {
    Write-Error '找不到 demo-video2 錄影，請先執行 npm run demo2:record'
    exit 1
}

Write-Host "錄影：$($webm.FullName)"
Write-Host "輸出：$assetGif"

$vf = 'fps=20,scale=1440:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=128[p];[s1][p]paletteuse=dither=bayer'

& ffmpeg -y -i "$($webm.FullName)" -vf $vf -loop 0 "$assetGif"

if ($LASTEXITCODE -ne 0) {
    Write-Error 'ffmpeg 轉換失敗'
    exit 1
}

Copy-Item -Path $assetGif -Destination $docsGif -Force

$size = [math]::Round((Get-Item $assetGif).Length / 1MB, 1)
Write-Host "完成！${size} MB → assets/demo2.gif & docs/demo2.gif"
