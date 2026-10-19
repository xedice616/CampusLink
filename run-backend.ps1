$ErrorActionPreference='Stop'
Set-Location (Join-Path $PSScriptRoot 'backend')
$mvn=Get-Command mvn -ErrorAction SilentlyContinue
if ($mvn) { & mvn spring-boot:run; exit $LASTEXITCODE }
$ver='3.9.9'
$local=Join-Path $PSScriptRoot ".tools\apache-maven-$ver\bin\mvn.cmd"
if (-not (Test-Path $local)) {
  Write-Host 'Maven not installed. Downloading official Apache Maven (internet required)...'
  New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot '.tools') | Out-Null
  $zip=Join-Path $PSScriptRoot '.tools\maven.zip'
  Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$ver/apache-maven-$ver-bin.zip" -OutFile $zip
  Expand-Archive -Path $zip -DestinationPath (Join-Path $PSScriptRoot '.tools') -Force
  Remove-Item $zip
}
& $local spring-boot:run
exit $LASTEXITCODE
