$ErrorActionPreference = "Stop"

$source = if ($args.Count -gt 0) { $args[0] } else { "records\xiangqi" }
$target = if ($args.Count -gt 1) { $args[1] } else { "records\ai-learning.tsv" }
$reward = if ($args.Count -gt 2) { $args[2] } else { "30" }

$javac = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javac.exe"
$java = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe"

if (!(Test-Path "target\classes")) {
    New-Item -ItemType Directory -Force -Path "target\classes" | Out-Null
}

$files = Get-ChildItem -Recurse -Filter "*.java" -Path "src\main\java" | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d "target\classes" $files
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& $java -cp "target\classes" edu.jieqi.ai.XiangqiRecordTrainer $source $target $reward
