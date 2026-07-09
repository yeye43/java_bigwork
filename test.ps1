$ErrorActionPreference = "Stop"

$jdkHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$javac = Join-Path $jdkHome "bin\javac.exe"
$java = Join-Path $jdkHome "bin\java.exe"

New-Item -ItemType Directory -Force -Path "target\classes" | Out-Null
New-Item -ItemType Directory -Force -Path "target\test-classes" | Out-Null

$mainFiles = Get-ChildItem -Recurse -Filter "*.java" -Path "src\main\java" | ForEach-Object { $_.FullName }
$testFiles = Get-ChildItem -Recurse -Filter "*.java" -Path "src\test\java" | ForEach-Object { $_.FullName }

& $javac -encoding UTF-8 -d "target\classes" $mainFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $javac -encoding UTF-8 -cp "target\classes" -d "target\test-classes" $testFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp "target\classes;target\test-classes" edu.jieqi.RuleEngineSelfTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp "target\classes;target\test-classes" edu.jieqi.JsonCodecSelfTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp "target\classes;target\test-classes" edu.jieqi.GameRecordSelfTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp "target\classes;target\test-classes" edu.jieqi.AiRegressionSelfTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
