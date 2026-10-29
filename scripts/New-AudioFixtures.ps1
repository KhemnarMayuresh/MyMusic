param([string]$OutputDirectory = (Join-Path $PSScriptRoot '..\analysis\fixtures'))
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
foreach ($fixture in @(@('Dawn',440,12), @('Night',660,18), @('Dawn reprise',880,8))) {
    $path = Join-Path $OutputDirectory ($fixture[0] + '.wav')
    $rate = 22050
    $samples = $rate * $fixture[2]
    $writer = [IO.BinaryWriter]::new([IO.File]::Create($path))
    try {
        $writer.Write([Text.Encoding]::ASCII.GetBytes('RIFF'))
        $writer.Write([int](36 + $samples * 2))
        $writer.Write([Text.Encoding]::ASCII.GetBytes('WAVEfmt '))
        $writer.Write([int]16); $writer.Write([int16]1); $writer.Write([int16]1)
        $writer.Write([int]$rate); $writer.Write([int]($rate * 2)); $writer.Write([int16]2); $writer.Write([int16]16)
        $writer.Write([Text.Encoding]::ASCII.GetBytes('data')); $writer.Write([int]($samples * 2))
        for ($i=0; $i -lt $samples; $i++) {
            $envelope = [Math]::Min(1.0, [Math]::Min($i / 1102.0, ($samples - $i) / 1102.0))
            $writer.Write([int16](3500 * $envelope * [Math]::Sin(2 * [Math]::PI * $fixture[1] * $i / $rate)))
        }
    } finally { $writer.Dispose() }
}
@'
[00:00.00]Dawn
[00:03.00]A new day begins
[00:08.00]Listen to the light
'@ | Set-Content -LiteralPath (Join-Path $OutputDirectory 'Dawn.lrc') -Encoding utf8
Get-ChildItem -LiteralPath $OutputDirectory -Filter '*.wav' | Get-FileHash -Algorithm SHA256 | Select-Object Hash,Path
