$ErrorActionPreference = "Stop"

$jdkHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$javac = Join-Path $jdkHome "bin\javac.exe"
$java = Join-Path $jdkHome "bin\java.exe"

if (-not (Test-Path $javac)) {
    throw "找不到 javac：$javac"
}

New-Item -ItemType Directory -Force -Path "target\classes" | Out-Null
$files = Get-ChildItem -Recurse -Filter "*.java" -Path "src\main\java" | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d "target\classes" $files
& $java -cp "target\classes" edu.jieqi.Main
