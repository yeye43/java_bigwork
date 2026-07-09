$ErrorActionPreference = "Stop"

$jdkHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$javac = Join-Path $jdkHome "bin\javac.exe"
$java = Join-Path $jdkHome "bin\java.exe"
$games = if ($args.Count -gt 0) { $args[0] } else { "50" }
$maxPlies = if ($args.Count -gt 1) { $args[1] } else { "220" }
$thinkMillis = if ($args.Count -gt 2) { $args[2] } else { "180" }
$trainingFile = if ($args.Count -gt 3) { $args[3] } else { "records\ai-training.tsv" }
$liveFile = if ($args.Count -gt 4) { $args[4] } else { "records\ai-learning.tsv" }

New-Item -ItemType Directory -Force -Path "target\classes" | Out-Null
New-Item -ItemType Directory -Force -Path "records" | Out-Null

$files = Get-ChildItem -Recurse -Filter "*.java" -Path "src\main\java" | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d "target\classes" $files
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& $java -cp "target\classes" edu.jieqi.ai.SelfPlayTrainer $games $maxPlies $thinkMillis $trainingFile $liveFile
