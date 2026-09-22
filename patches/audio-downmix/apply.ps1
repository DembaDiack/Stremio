[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$DecodedDirectory
)

# Morphe Manager applies bytecode hooks from the .mpp bundle. Legacy build.ps1
# still requires a module-local apply script even when no smali diff is needed.
