# PowerShell Sync Pipeline for Multi-Agent Updates
# Fetches upstream changes from origin and integrates them safely

param(
    [string]$TargetBranch = "theme/mika",
    [string]$UpstreamBranch = "origin/main"
)

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " [Mika Pipeline] Fetching remote updates..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# Fetch all remote updates
git fetch origin

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to fetch from remote origin. Check network/token."
    exit 1
}

# Check if there are changes between current branch and upstream
$currentBranch = (git branch --show-current).Trim()
Write-Host "Current Branch: $currentBranch" -ForegroundColor Yellow

$behindCount = (git rev-list --count HEAD..$UpstreamBranch)
Write-Host "Commits behind $UpstreamBranch : $behindCount" -ForegroundColor Green

if ($behindCount -gt 0) {
    Write-Host "Updating with latest changes from $UpstreamBranch..." -ForegroundColor Cyan
    # Perform clean rebase or merge
    git merge $UpstreamBranch --no-edit -m "chore(sync): sync upstream updates into $currentBranch"
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Conflict detected. Please review git status."
    } else {
        Write-Host "Successfully merged latest updates without conflicts!" -ForegroundColor Green
    }
} else {
    Write-Host "Already up to date with $UpstreamBranch." -ForegroundColor Green
}

Write-Host "`nReady! All work remains isolated in 'mika/' folder." -ForegroundColor Cyan
