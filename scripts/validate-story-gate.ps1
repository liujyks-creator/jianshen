[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('CaptureManifest', 'DevPreflight', 'ReviewPreflight', 'IntegrationPrePush', 'PostMerge')]
    [string]$Phase,

    [string]$RepositoryPath = (Get-Location).Path,

    [ValidatePattern('^$|^[0-9a-fA-F]{40}$')]
    [string]$AcceptedRulesSha = '',

    [ValidatePattern('^$|^[0-9a-fA-F]{40}$')]
    [string]$StoryBaseSha = '',

    [ValidateSet('', 'new_story', 'repair')]
    [string]$DevMode = '',

    [string]$ExpectedStoryParent = '',

    [string]$ExpectedRemoteStoryTip = '',

    [ValidatePattern('^$|^[0-9a-fA-F]{40}$')]
    [string]$StorySha = '',

    [string]$StoryBranch = '',

    [ValidatePattern('^$|^[0-9a-fA-F]{40}$')]
    [string]$MergeSha = '',

    [string]$StoryId = '',

    [string]$ContractVersion = '',

    [ValidateSet('', 'not_required', 'passed')]
    [string]$EvidenceGate = '',

    [string[]]$RequiredAncestorSha = @(),

    [ValidatePattern('^$|^[0-9a-fA-F]{64}$')]
    [string]$ScopeManifestSha256 = '',

    [string]$ScopeManifestPath = '',

    [string]$GeneratedPromptPath = '',

    [string]$ProtectedManifestPath = '',

    [ValidatePattern('^$|^[0-9a-fA-F]{64}$')]
    [string]$ProtectedManifestSha256 = '',

    [string]$ExpectedProtectedRoot = '',

    [ValidatePattern('^$|^[0-9a-fA-F]{40}$')]
    [string]$ExpectedCaptureHead = '',

    [string[]]$ExpectedAdoptedUserOverlayPath = @(),

    [string]$ExpectedAdoptionAuthorizationReference = '',

    [string[]]$ExpectedProtectedIgnoredPath = @(),

    [string[]]$ExpectedEphemeralIgnoredPath = @(),

    [string[]]$AdoptedUserOverlayPath = @(),

    [string]$AdoptionAuthorizationReference = '',

    [string[]]$ProtectedIgnoredPath = @(),

    [string[]]$EphemeralIgnoredPath = @(),

    [string]$ManifestOutputPath = ''
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RepositoryPath = (Resolve-Path -LiteralPath $RepositoryPath).Path
$CanonicalContractPath = 'docs/process/story-workflow-contract.md'
$CanonicalValidatorPath = 'scripts/validate-story-gate.ps1'
$ScopeCategories = @('production', 'debug', 'test', 'docs', 'governance')

function Invoke-GitAt {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    # Windows PowerShell 5.1 promotes native stderr to ErrorRecord objects when
    # ErrorActionPreference is Stop. Git can write warnings to stderr and still
    # succeed, so capture the native exit code under Continue and restore the
    # caller's strict preference immediately afterward.
    $previousErrorActionPreference = $ErrorActionPreference
    $previousNoReplaceObjects = [Environment]::GetEnvironmentVariable('GIT_NO_REPLACE_OBJECTS', 'Process')
    try {
        $ErrorActionPreference = 'Continue'
        $env:GIT_NO_REPLACE_OBJECTS = '1'
        $combined = @(& git -C $Root @Arguments 2>&1)
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
        if ($null -eq $previousNoReplaceObjects) {
            Remove-Item Env:GIT_NO_REPLACE_OBJECTS -ErrorAction SilentlyContinue
        } else {
            $env:GIT_NO_REPLACE_OBJECTS = $previousNoReplaceObjects
        }
    }
    $standardOutput = @($combined | Where-Object { $_ -isnot [System.Management.Automation.ErrorRecord] } | ForEach-Object { [string]$_ })
    $standardError = @($combined | Where-Object { $_ -is [System.Management.Automation.ErrorRecord] } | ForEach-Object { [string]$_ })
    if (-not $AllowFailure -and $exitCode -ne 0) {
        $diagnostics = @($standardOutput + $standardError)
        throw "git $($Arguments -join ' ') failed ($exitCode): $($diagnostics -join [Environment]::NewLine)"
    }
    [pscustomobject]@{
        ExitCode = $exitCode
        Output = $standardOutput
        ErrorOutput = $standardError
    }
}

function Assert-NoGitObjectReplacement {
    $replaceRefs = @((Invoke-Git -Arguments @('for-each-ref', '--format=%(refname)', 'refs/replace')).Output |
        Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) })
    if ($replaceRefs.Count -gt 0) {
        throw "Git replace refs are forbidden during gate validation: $($replaceRefs -join ', ')"
    }
    $graftsPath = Join-Path (Get-GitCommonDirectory -Root $RepositoryPath) 'info/grafts'
    if ($null -ne (Get-Item -Force -LiteralPath $graftsPath -ErrorAction SilentlyContinue)) {
        throw "Legacy Git grafts are forbidden during gate validation: $graftsPath"
    }
}

function Invoke-Git {
    param([string[]]$Arguments, [switch]$AllowFailure)
    Invoke-GitAt -Root $RepositoryPath -Arguments $Arguments -AllowFailure:$AllowFailure
}

function Get-GitTextAt {
    param([string]$Root, [string[]]$Arguments)
    ((Invoke-GitAt -Root $Root -Arguments $Arguments).Output -join "`n").Trim()
}

function Get-GitText {
    param([string[]]$Arguments)
    (Get-GitTextAt -Root $RepositoryPath -Arguments $Arguments)
}

function Assert-Equal {
    param([string]$Actual, [string]$Expected, [string]$Label)
    if ($Actual -cne $Expected) {
        throw "$Label mismatch. Expected $Expected, got $Actual."
    }
}

function Assert-PathEqual {
    param([string]$Actual, [string]$Expected, [string]$Label)
    $actualFull = [System.IO.Path]::GetFullPath($Actual).TrimEnd('\', '/')
    $expectedFull = [System.IO.Path]::GetFullPath($Expected).TrimEnd('\', '/')
    if (-not $actualFull.Equals($expectedFull, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "$Label mismatch. Expected $expectedFull, got $actualFull."
    }
}

function Assert-FullSha {
    param([string]$Sha, [string]$Label)
    if ($Sha -notmatch '^[0-9a-fA-F]{40}$') {
        throw "$Label must be a full 40-character SHA."
    }
}

function Assert-Commit {
    param([string]$Sha, [string]$Label)
    Assert-FullSha -Sha $Sha -Label $Label
    $result = Invoke-Git -Arguments @('cat-file', '-e', "$Sha^{commit}") -AllowFailure
    if ($result.ExitCode -ne 0) {
        throw "$Label is not a commit: $Sha"
    }
}

function Get-GitBlobShaForBytes {
    param([byte[]]$Bytes)

    $header = [System.Text.Encoding]::ASCII.GetBytes("blob $($Bytes.Length)`0")
    $payload = [byte[]]::new($header.Length + $Bytes.Length)
    [System.Array]::Copy($header, 0, $payload, 0, $header.Length)
    [System.Array]::Copy($Bytes, 0, $payload, $header.Length, $Bytes.Length)
    $sha = [System.Security.Cryptography.SHA1]::Create()
    try {
        (($sha.ComputeHash($payload) | ForEach-Object { $_.ToString('x2') }) -join '')
    } finally {
        $sha.Dispose()
    }
}

function Assert-RunningValidatorMatchesAcceptedRules {
    $acceptedBlobResult = Invoke-Git -Arguments @('rev-parse', "${AcceptedRulesSha}:$CanonicalValidatorPath") -AllowFailure
    if ($acceptedBlobResult.ExitCode -ne 0 -or $acceptedBlobResult.Output.Count -ne 1) {
        throw "Accepted validator is missing at ${AcceptedRulesSha}:$CanonicalValidatorPath. Bootstrap governance must use the prior accepted process; a candidate validator cannot approve itself."
    }
    $acceptedBlob = ([string]$acceptedBlobResult.Output[0]).Trim().ToLowerInvariant()
    if ($acceptedBlob -notmatch '^[0-9a-f]{40}$') {
        throw "Accepted validator blob is invalid at ${AcceptedRulesSha}:$CanonicalValidatorPath."
    }
    if ([string]::IsNullOrWhiteSpace($PSCommandPath) -or -not (Test-Path -LiteralPath $PSCommandPath -PathType Leaf)) {
        throw 'Unable to identify the running validator file.'
    }
    $runningBytes = [System.IO.File]::ReadAllBytes($PSCommandPath)
    if ($runningBytes -contains 0) {
        throw 'Running validator contains a NUL byte.'
    }
    if ($runningBytes.Length -ge 3 -and $runningBytes[0] -eq 0xEF -and $runningBytes[1] -eq 0xBB -and $runningBytes[2] -eq 0xBF) {
        throw 'Running validator must be UTF-8 without BOM.'
    }
    $strictUtf8 = [System.Text.UTF8Encoding]::new($false, $true)
    try {
        $runningText = $strictUtf8.GetString($runningBytes)
    } catch {
        throw 'Running validator is not strict UTF-8.'
    }
    $rawBlob = Get-GitBlobShaForBytes -Bytes $runningBytes
    $normalizedBytes = [System.Text.UTF8Encoding]::new($false).GetBytes(($runningText -replace "`r`n", "`n" -replace "`r", "`n"))
    $normalizedBlob = Get-GitBlobShaForBytes -Bytes $normalizedBytes
    if ($acceptedBlob -cne $rawBlob -and $acceptedBlob -cne $normalizedBlob) {
        throw "Running validator blob mismatch. Expected accepted blob $acceptedBlob, got raw=$rawBlob normalized=$normalizedBlob."
    }
    $script:AcceptedValidatorBlob = $acceptedBlob
}

function Resolve-InputPath {
    param([string]$Path, [string]$Label)
    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw "$Label is required."
    }
    $candidate = if ([System.IO.Path]::IsPathRooted($Path)) {
        $Path
    } else {
        Join-Path $RepositoryPath $Path
    }
    [System.IO.Path]::GetFullPath($candidate)
}

function ConvertTo-RepoPath {
    param([string]$Path, [string]$Label)
    $normalized = ($Path.Trim() -replace '\\', '/')
    while ($normalized.StartsWith('./')) {
        $normalized = $normalized.Substring(2)
    }
    if (
        [string]::IsNullOrWhiteSpace($normalized) -or
        [System.IO.Path]::IsPathRooted($Path) -or
        $normalized.StartsWith('/') -or
        $normalized.EndsWith('/') -or
        $normalized -match '(^|/)\.\.(/|$)' -or
        $normalized -match '[\x00-\x1F]' -or
        $normalized.IndexOfAny([char[]]'*?[]') -ge 0
    ) {
        throw "$Label must be an exact repository-relative file path: $Path"
    }
    $normalized
}

function Get-RepositoryWorktreePaths {
    param([string]$Root)
    @((Invoke-GitAt -Root $Root -Arguments @('worktree', 'list', '--porcelain')).Output |
        Where-Object { ([string]$_).StartsWith('worktree ', [System.StringComparison]::Ordinal) } |
        ForEach-Object { [System.IO.Path]::GetFullPath(([string]$_).Substring(9)).TrimEnd('\', '/') })
}

function Get-PrimaryWorktreePath {
    param([string]$Root)
    $worktrees = @(Get-RepositoryWorktreePaths -Root $Root)
    if ($worktrees.Count -eq 0) {
        throw 'Unable to resolve the primary Git worktree.'
    }
    $worktrees[0]
}

function Test-AbsolutePathWithinRoot {
    param([string]$Path, [string]$Root)
    $fullPath = [System.IO.Path]::GetFullPath($Path).TrimEnd('\', '/')
    $fullRoot = [System.IO.Path]::GetFullPath($Root).TrimEnd('\', '/')
    $fullPath.Equals($fullRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
        $fullPath.StartsWith($fullRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)
}

function Assert-ExternalGateArtifactPath {
    param([string]$Path, [string]$Label)
    $full = [System.IO.Path]::GetFullPath($Path)
    Assert-NoReparsePathComponents -Path $full -Label $Label
    $gateRoot = Join-Path (Get-GitCommonDirectory -Root $RepositoryPath) 'codex-story-gates'
    if (Test-AbsolutePathWithinRoot -Path $full -Root $gateRoot) {
        return
    }
    foreach ($worktree in Get-RepositoryWorktreePaths -Root $RepositoryPath) {
        if (Test-AbsolutePathWithinRoot -Path $full -Root $worktree) {
            throw "$Label must be outside every repository worktree: $full"
        }
    }
}

function Assert-NoReparsePathComponents {
    param([string]$Path, [string]$Label)
    $full = [System.IO.Path]::GetFullPath($Path)
    $item = Get-Item -Force -LiteralPath $full -ErrorAction SilentlyContinue
    $current = if ($null -ne $item) { $item.FullName } else { Split-Path -Parent $full }
    while (-not [string]::IsNullOrWhiteSpace($current)) {
        $component = Get-Item -Force -LiteralPath $current -ErrorAction SilentlyContinue
        if ($null -ne $component -and ($component.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "$Label path contains a symlink or reparse-point component: $($component.FullName)"
        }
        $parent = Split-Path -Parent $current
        if ([string]::IsNullOrWhiteSpace($parent) -or $parent -ceq $current) {
            break
        }
        $current = $parent
    }
}

function Assert-ExactJsonProperties {
    param([object]$Value, [string[]]$Expected, [string]$Label)
    $actual = @($Value.PSObject.Properties.Name | Sort-Object)
    $wanted = @($Expected | Sort-Object)
    if (($actual -join "`n") -cne ($wanted -join "`n")) {
        throw "$Label properties mismatch. Expected [$($wanted -join ', ')], got [$($actual -join ', ')]."
    }
}

function ConvertTo-ScopeSet {
    param(
        [object[]]$RawEntries,
        [object[]]$RawEnvelopes,
        [string]$Label
    )

    $entries = [System.Collections.Generic.List[object]]::new()
    $seenPaths = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($entry in @($RawEntries)) {
        Assert-ExactJsonProperties -Value $entry -Expected @('path', 'operation', 'required', 'category', 'responsibility') -Label "$Label entry"
        $pathValue = ConvertTo-RepoPath -Path ([string]$entry.path) -Label "$Label entry path"
        if (-not $seenPaths.Add($pathValue)) {
            throw "$Label contains duplicate path: $pathValue"
        }
        $operation = ([string]$entry.operation).ToLowerInvariant()
        if ($operation -notin @('add', 'modify', 'delete')) {
            throw "Unsupported $Label operation for ${pathValue}: $operation"
        }
        if ($entry.required -isnot [bool]) {
            throw "$Label entry required must be a JSON boolean: $pathValue"
        }
        $category = ([string]$entry.category).ToLowerInvariant()
        if ($category -notin $ScopeCategories) {
            throw "Unsupported $Label category for ${pathValue}: $category"
        }
        $responsibility = ([string]$entry.responsibility).Trim()
        if ([string]::IsNullOrWhiteSpace($responsibility)) {
            throw "$Label entry responsibility is required: $pathValue"
        }
        $entries.Add([pscustomobject]@{
            path = $pathValue
            operation = $operation
            required = [bool]$entry.required
            category = $category
            responsibility = $responsibility
        })
    }
    if ($entries.Count -eq 0) {
        throw "$Label requires at least one entry."
    }

    $envelopes = [System.Collections.Generic.List[object]]::new()
    $seenCategories = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($envelope in @($RawEnvelopes)) {
        Assert-ExactJsonProperties -Value $envelope -Expected @('category', 'expected', 'hardMax') -Label "$Label envelope"
        $category = ([string]$envelope.category).ToLowerInvariant()
        if ($category -notin $ScopeCategories -or -not $seenCategories.Add($category)) {
            throw "$Label envelope category is missing, duplicate, or unsupported: $category"
        }
        if (($envelope.expected -isnot [int]) -and ($envelope.expected -isnot [long])) {
            throw "$Label envelope expected must be a JSON integer: $category"
        }
        if (($envelope.hardMax -isnot [int]) -and ($envelope.hardMax -isnot [long])) {
            throw "$Label envelope hardMax must be a JSON integer: $category"
        }
        $expected = [int]$envelope.expected
        $hardMax = [int]$envelope.hardMax
        if ($expected -lt 0 -or $hardMax -lt 0 -or $expected -gt $hardMax) {
            throw "Invalid $Label envelope for ${category}: expected=$expected hardMax=$hardMax"
        }
        $envelopes.Add([pscustomobject]@{ category = $category; expected = $expected; hardMax = $hardMax })
    }
    $missingCategories = @($ScopeCategories | Where-Object { $_ -notin $seenCategories })
    if ($missingCategories.Count -gt 0) {
        throw "$Label is missing envelopes: $($missingCategories -join ', ')"
    }
    foreach ($category in $ScopeCategories) {
        $requiredCount = @($entries | Where-Object { $_.category -ceq $category -and $_.required }).Count
        $entryCount = @($entries | Where-Object { $_.category -ceq $category }).Count
        $expected = [int](@($envelopes | Where-Object { $_.category -ceq $category })[0].expected)
        if ($requiredCount -gt $expected -or $expected -gt $entryCount) {
            throw "$Label envelope is inconsistent for ${category}: required=$requiredCount expected=$expected entries=$entryCount"
        }
    }

    [pscustomobject]@{
        entries = @($entries)
        envelopes = @($envelopes)
    }
}

function Read-ScopeManifest {
    if ([string]::IsNullOrWhiteSpace($ScopeManifestPath)) {
        throw 'ScopeManifestPath is required for every gate phase.'
    }
    if ($ScopeManifestSha256 -notmatch '^[0-9a-fA-F]{64}$') {
        throw 'ScopeManifestSha256 is required for every gate phase.'
    }
    $path = Resolve-InputPath -Path $ScopeManifestPath -Label 'Scope manifest path'
    Assert-ExternalGateArtifactPath -Path $path -Label 'Scope manifest'
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Scope manifest does not exist: $path"
    }
    $item = Get-Item -Force -LiteralPath $path
    if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw "Scope manifest cannot be a symlink or reparse point: $path"
    }
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash.ToUpperInvariant()
    Assert-Equal -Actual $hash -Expected $ScopeManifestSha256.ToUpperInvariant() -Label 'Scope manifest SHA256'
    $raw = Get-Content -Raw -Encoding UTF8 -LiteralPath $path | ConvertFrom-Json
    Assert-ExactJsonProperties -Value $raw -Expected @('schemaVersion', 'fullStory', 'currentSegment') -Label 'Scope manifest'
    if ([int]$raw.schemaVersion -ne 2) {
        throw "Unsupported scope manifest schema: $($raw.schemaVersion)"
    }
    Assert-ExactJsonProperties -Value $raw.fullStory -Expected @('baseSha', 'entries', 'envelopes') -Label 'Scope manifest fullStory'
    Assert-ExactJsonProperties -Value $raw.currentSegment -Expected @('baseSha', 'entries', 'envelopes') -Label 'Scope manifest currentSegment'
    $fullStoryBaseSha = ([string]$raw.fullStory.baseSha).Trim().ToLowerInvariant()
    $segmentBaseSha = ([string]$raw.currentSegment.baseSha).Trim().ToLowerInvariant()
    Assert-FullSha -Sha $fullStoryBaseSha -Label 'Scope full-Story base SHA'
    Assert-FullSha -Sha $segmentBaseSha -Label 'Scope segment base SHA'
    $story = ConvertTo-ScopeSet -RawEntries @($raw.fullStory.entries) -RawEnvelopes @($raw.fullStory.envelopes) -Label 'Story scope'
    $segment = ConvertTo-ScopeSet -RawEntries @($raw.currentSegment.entries) -RawEnvelopes @($raw.currentSegment.envelopes) -Label 'Current-segment scope'
    foreach ($segmentEntry in $segment.entries) {
        $storyEntry = @($story.entries | Where-Object { $_.path -ceq $segmentEntry.path })
        if ($storyEntry.Count -ne 1) {
            throw "Current-segment path is absent from full Story scope: $($segmentEntry.path)"
        }
        if ($storyEntry[0].category -cne $segmentEntry.category) {
            throw "Current-segment category differs from full Story scope for $($segmentEntry.path): story=$($storyEntry[0].category) segment=$($segmentEntry.category)"
        }
    }

    [pscustomobject]@{
        path = $path
        sha256 = $hash
        fullStoryBaseSha = $fullStoryBaseSha
        storyEntries = @($story.entries)
        storyEnvelopes = @($story.envelopes)
        segmentBaseSha = $segmentBaseSha
        segmentEntries = @($segment.entries)
        segmentEnvelopes = @($segment.envelopes)
    }
}

function Assert-EmptyIndexAt {
    param([string]$Root)
    $paths = Get-GitTextAt -Root $Root -Arguments @('diff', '--cached', '--name-only')
    if ($paths.Length -ne 0) {
        throw "Index is not empty: $paths"
    }
    $unmerged = Get-GitTextAt -Root $Root -Arguments @('ls-files', '-u')
    if ($unmerged.Length -ne 0) {
        throw 'Index contains unmerged entries.'
    }
}

function Assert-EmptyIndex {
    Assert-EmptyIndexAt -Root $RepositoryPath
}

function Assert-CleanTrackedWorktree {
    $diff = Invoke-Git -Arguments @('diff', '--quiet') -AllowFailure
    if ($diff.ExitCode -ne 0) {
        throw 'Tracked worktree is not clean; immutable-tree validation requires an isolated clean worktree.'
    }
    Assert-EmptyIndex
}

function Assert-CleanImmutableWorktree {
    Assert-CleanTrackedWorktree
    $untracked = @((Invoke-Git -Arguments @('-c', 'core.quotePath=false', 'ls-files', '--others', '--exclude-standard')).Output |
        Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) })
    if ($untracked.Count -gt 0) {
        throw "Immutable worktree contains untracked files: $($untracked -join ', ')"
    }
    Assert-IgnoredClassification -Root $RepositoryPath -ProtectedRoots @($script:VerifiedProtectedIgnoredPaths) -EphemeralRoots @($script:VerifiedEphemeralIgnoredPaths)
    foreach ($ignoredRoot in @($script:VerifiedProtectedIgnoredRoots)) {
        $relative = ConvertTo-RepoPath -Path ([string]$ignoredRoot.path) -Label 'Protected ignored root'
        $absolute = Join-Path $RepositoryPath ($relative -replace '/', [System.IO.Path]::DirectorySeparatorChar)
        if (Test-Path -LiteralPath $absolute) {
            Assert-ProtectedIgnoredRoot -Root $RepositoryPath -Expected $ignoredRoot
        }
    }
}

function Assert-AncestorsOfAcceptedRules {
    foreach ($sha in $RequiredAncestorSha) {
        Assert-Commit -Sha $sha -Label 'Prerequisite SHA'
        $result = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $sha, $AcceptedRulesSha) -AllowFailure
        if ($result.ExitCode -ne 0) {
            throw "Prerequisite $sha is not an ancestor of accepted rules $AcceptedRulesSha."
        }
    }
}

function Test-RefExists {
    param([string]$Ref)
    (Invoke-Git -Arguments @('show-ref', '--verify', '--quiet', $Ref) -AllowFailure).ExitCode -eq 0
}

function Assert-ValidStoryBranch {
    if ([string]::IsNullOrWhiteSpace($StoryBranch)) {
        throw "$Phase requires StoryBranch."
    }
    $result = Invoke-Git -Arguments @('check-ref-format', '--branch', $StoryBranch) -AllowFailure
    if ($result.ExitCode -ne 0 -or $result.Output.Count -ne 1 -or ([string]$result.Output[0]).Trim() -cne $StoryBranch) {
        throw "Invalid Story branch name: $StoryBranch"
    }
}

function Assert-StoryHistory {
    Assert-Commit -Sha $StoryBaseSha -Label 'Story base SHA'
    Assert-Equal -Actual $script:ScopeDefinition.fullStoryBaseSha -Expected $StoryBaseSha -Label 'Scope full-Story base'
    $acceptedHistory = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $StoryBaseSha, $AcceptedRulesSha) -AllowFailure
    if ($acceptedHistory.ExitCode -ne 0) {
        throw "Story base $StoryBaseSha is not an ancestor of accepted rules $AcceptedRulesSha."
    }
    $result = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $StoryBaseSha, $StorySha) -AllowFailure
    if ($result.ExitCode -ne 0) {
        throw "Story base $StoryBaseSha is not an ancestor of Story $StorySha."
    }
    Assert-LinearStorySegment -StartSha $StoryBaseSha -EndSha $StorySha -Label 'Full Story'
}

function Assert-CaptureLineage {
    if ([string]::IsNullOrWhiteSpace($DevMode)) {
        throw "$Phase requires DevMode to bind the protected-manifest capture point."
    }
    Assert-Equal -Actual $script:ScopeDefinition.fullStoryBaseSha -Expected $StoryBaseSha -Label 'Scope full-Story base'
    switch ($DevMode) {
        'new_story' {
            Assert-Equal -Actual $ExpectedStoryParent -Expected 'none' -Label 'New Story expected parent'
            Assert-Equal -Actual $script:VerifiedCaptureHead -Expected $StoryBaseSha -Label 'New Story capture HEAD'
            Assert-Equal -Actual $script:ScopeDefinition.segmentBaseSha -Expected $StoryBaseSha -Label 'New Story segment base'
            $storyEntries = @($script:ScopeDefinition.storyEntries | ForEach-Object { '{0}|{1}|{2}|{3}|{4}' -f $_.path,$_.operation,$_.required,$_.category,$_.responsibility } | Sort-Object)
            $segmentEntries = @($script:ScopeDefinition.segmentEntries | ForEach-Object { '{0}|{1}|{2}|{3}|{4}' -f $_.path,$_.operation,$_.required,$_.category,$_.responsibility } | Sort-Object)
            $storyEnvelopes = @($script:ScopeDefinition.storyEnvelopes | ForEach-Object { '{0}|{1}|{2}' -f $_.category,$_.expected,$_.hardMax } | Sort-Object)
            $segmentEnvelopes = @($script:ScopeDefinition.segmentEnvelopes | ForEach-Object { '{0}|{1}|{2}' -f $_.category,$_.expected,$_.hardMax } | Sort-Object)
            if (($storyEntries -join "`n") -cne ($segmentEntries -join "`n") -or ($storyEnvelopes -join "`n") -cne ($segmentEnvelopes -join "`n")) {
                throw 'A new Story must use identical full-Story and current-segment scope definitions.'
            }
            if (-not [string]::IsNullOrWhiteSpace($StorySha)) {
                Assert-LinearStorySegment -StartSha $StoryBaseSha -EndSha $StorySha -Label 'New Story'
            }
        }
        'repair' {
            Assert-FullSha -Sha $ExpectedStoryParent -Label 'Repair expected Story parent'
            Assert-Commit -Sha $ExpectedStoryParent -Label 'Repair expected Story parent'
            Assert-Equal -Actual $script:VerifiedCaptureHead -Expected $ExpectedStoryParent -Label 'Repair capture HEAD'
            Assert-Equal -Actual $script:ScopeDefinition.segmentBaseSha -Expected $ExpectedStoryParent -Label 'Repair segment base'
            $baseHistory = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $StoryBaseSha, $ExpectedStoryParent) -AllowFailure
            if ($baseHistory.ExitCode -ne 0) {
                throw "Story base $StoryBaseSha is not an ancestor of Repair parent $ExpectedStoryParent."
            }
            if (-not [string]::IsNullOrWhiteSpace($StorySha)) {
                $repairHistory = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $ExpectedStoryParent, $StorySha) -AllowFailure
                if ($repairHistory.ExitCode -ne 0) {
                    throw "Repair parent $ExpectedStoryParent is not an ancestor of Story $StorySha."
                }
                Assert-LinearStorySegment -StartSha $ExpectedStoryParent -EndSha $StorySha -Label 'Repair'
            }
        }
    }
}

function Assert-LinearStorySegment {
    param([string]$StartSha, [string]$EndSha, [string]$Label)
    $merges = @((Invoke-Git -Arguments @('rev-list', '--merges', "$StartSha..$EndSha")).Output |
        Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) })
    if ($merges.Count -gt 0) {
        throw "$Label segment contains merge commits and is not a linear Story history: $($merges -join ', ')"
    }
}

function Get-OverlayCandidateEntries {
    param([string]$Root)
    Assert-EmptyIndexAt -Root $Root
    $items = [System.Collections.Generic.List[object]]::new()
    $dirty = @((Invoke-GitAt -Root $Root -Arguments @('-c', 'core.quotePath=false', 'diff', '--name-status', '--no-renames', '--')).Output)
    foreach ($line in $dirty) {
        $parts = @(([string]$line) -split "`t", 2)
        if ($parts.Count -ne 2) {
            throw "Unable to parse dirty path status: $line"
        }
        $operation = switch ($parts[0]) {
            'M' { 'modify' }
            'D' { 'delete' }
            default { "unsupported:$($parts[0])" }
        }
        $items.Add([pscustomobject]@{
            path = ConvertTo-RepoPath -Path $parts[1] -Label 'Overlay candidate'
            operation = $operation
        })
    }
    $untracked = @((Invoke-GitAt -Root $Root -Arguments @('-c', 'core.quotePath=false', 'ls-files', '--others', '--exclude-standard')).Output)
    foreach ($path in $untracked) {
        $items.Add([pscustomobject]@{
            path = ConvertTo-RepoPath -Path ([string]$path) -Label 'Overlay candidate'
            operation = 'add'
        })
    }
    @($items | Sort-Object -Property path)
}

function Get-ProtectedCandidatePaths {
    param([string]$Root, [string[]]$AdoptedPaths)
    @(Get-OverlayCandidateEntries -Root $Root |
        Where-Object { $_.path -notin $AdoptedPaths } |
        ForEach-Object { $_.path } |
        Sort-Object -Unique)
}

function Get-IgnoredLeafPaths {
    param([string]$Root)
    @((Invoke-GitAt -Root $Root -Arguments @('-c', 'core.quotePath=false', 'ls-files', '--others', '--ignored', '--exclude-standard')).Output |
        Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) } |
        ForEach-Object { ConvertTo-RepoPath -Path ([string]$_) -Label 'Ignored leaf path' } |
        Sort-Object -Unique)
}

function Test-PathWithinRoot {
    param([string]$Path, [string]$RootPath)
    $Path -ceq $RootPath -or $Path.StartsWith("$RootPath/", [System.StringComparison]::Ordinal)
}

function Assert-IgnoredClassification {
    param(
        [string]$Root,
        [string[]]$ProtectedRoots,
        [string[]]$EphemeralRoots
    )
    $unclassified = [System.Collections.Generic.List[string]]::new()
    foreach ($path in Get-IgnoredLeafPaths -Root $Root) {
        $isClassified = @($ProtectedRoots + $EphemeralRoots | Where-Object { Test-PathWithinRoot -Path $path -RootPath $_ }).Count -gt 0
        if (-not $isClassified) {
            $unclassified.Add($path)
        }
    }
    if ($unclassified.Count -gt 0) {
        throw "Ignored files are not classified as protected or ephemeral: $($unclassified -join ', ')"
    }
}

function Get-GitCommonDirectory {
    param([string]$Root)
    $value = Get-GitTextAt -Root $Root -Arguments @('rev-parse', '--git-common-dir')
    $candidate = if ([System.IO.Path]::IsPathRooted($value)) { $value } else { Join-Path $Root $value }
    [System.IO.Path]::GetFullPath($candidate).TrimEnd('\', '/')
}

function New-ProtectedEntry {
    param([string]$Root, [string]$RelativePath)
    $absolute = Join-Path $Root ($RelativePath -replace '/', [System.IO.Path]::DirectorySeparatorChar)
    $item = Get-Item -Force -LiteralPath $absolute -ErrorAction SilentlyContinue
    if ($null -ne $item) {
        if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Protected candidate cannot be a symlink or reparse point: $RelativePath"
        }
        if ($item.PSIsContainer) {
            throw "Protected candidate is not a regular file: $RelativePath"
        }
        return [pscustomobject]@{
            path = $RelativePath
            kind = 'file'
            length = [long]$item.Length
            sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $absolute).Hash.ToUpperInvariant()
        }
    }
    [pscustomobject]@{
        path = $RelativePath
        kind = 'missing'
        length = 0
        sha256 = ''
    }
}

function Convert-AbsoluteToRepoPath {
    param([string]$Root, [string]$AbsolutePath, [string]$Label)
    $rootPrefix = [System.IO.Path]::GetFullPath($Root).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
    $full = [System.IO.Path]::GetFullPath($AbsolutePath)
    if (-not $full.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "$Label is outside the protected repository root: $full"
    }
    ConvertTo-RepoPath -Path $full.Substring($rootPrefix.Length) -Label $Label
}

function Test-PathOverlap {
    param([string]$Left, [string]$Right)
    $Left -ceq $Right -or $Left.StartsWith("$Right/", [System.StringComparison]::Ordinal) -or $Right.StartsWith("$Left/", [System.StringComparison]::Ordinal)
}

function Assert-SeparatePathClasses {
    param(
        [string[]]$Adopted,
        [string[]]$ProtectedIgnored,
        [string[]]$EphemeralIgnored
    )
    $classes = @(
        [pscustomobject]@{ Name = 'adopted'; Paths = @($Adopted) },
        [pscustomobject]@{ Name = 'protected ignored'; Paths = @($ProtectedIgnored) },
        [pscustomobject]@{ Name = 'ephemeral ignored'; Paths = @($EphemeralIgnored) }
    )
    for ($leftIndex = 0; $leftIndex -lt $classes.Count; $leftIndex++) {
        for ($rightIndex = $leftIndex + 1; $rightIndex -lt $classes.Count; $rightIndex++) {
            foreach ($left in $classes[$leftIndex].Paths) {
                foreach ($right in $classes[$rightIndex].Paths) {
                    if (Test-PathOverlap -Left $left -Right $right) {
                        throw "Path classes overlap: $($classes[$leftIndex].Name) '$left' and $($classes[$rightIndex].Name) '$right'."
                    }
                }
            }
        }
    }
}

function New-ProtectedIgnoredRoot {
    param([string]$Root, [string]$RelativePath)
    $absolute = Join-Path $Root ($RelativePath -replace '/', [System.IO.Path]::DirectorySeparatorChar)
    if (-not (Test-Path -LiteralPath $absolute)) {
        return [pscustomobject]@{ path = $RelativePath; kind = 'missing'; entries = @() }
    }
    $rootItem = Get-Item -Force -LiteralPath $absolute
    if (($rootItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw "Protected ignored path cannot be a symlink or reparse point: $RelativePath"
    }
    if (-not $rootItem.PSIsContainer) {
        return [pscustomobject]@{
            path = $RelativePath
            kind = 'file'
            length = [long]$rootItem.Length
            sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $absolute).Hash.ToUpperInvariant()
            entries = @()
        }
    }

    $files = [System.Collections.Generic.List[object]]::new()
    $pending = [System.Collections.Generic.Stack[string]]::new()
    $pending.Push($rootItem.FullName)
    while ($pending.Count -gt 0) {
        $directory = $pending.Pop()
        foreach ($item in Get-ChildItem -Force -LiteralPath $directory) {
            if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
                $relative = Convert-AbsoluteToRepoPath -Root $Root -AbsolutePath $item.FullName -Label 'Protected ignored entry'
                throw "Protected ignored inventory cannot traverse a symlink or reparse point: $relative"
            }
            if ($item.PSIsContainer) {
                $pending.Push($item.FullName)
            } else {
                $relative = Convert-AbsoluteToRepoPath -Root $Root -AbsolutePath $item.FullName -Label 'Protected ignored file'
                $files.Add([pscustomobject]@{
                    path = $relative
                    length = [long]$item.Length
                    sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $item.FullName).Hash.ToUpperInvariant()
                })
            }
        }
    }
    [pscustomobject]@{
        path = $RelativePath
        kind = 'directory'
        entries = @($files | Sort-Object -Property path)
    }
}

function Assert-ProtectedIgnoredRoot {
    param([string]$Root, [object]$Expected)
    $path = ConvertTo-RepoPath -Path ([string]$Expected.path) -Label 'Protected ignored root'
    $actual = New-ProtectedIgnoredRoot -Root $Root -RelativePath $path
    Assert-Equal -Actual ([string]$actual.kind) -Expected ([string]$Expected.kind) -Label "Protected ignored kind $path"
    switch ([string]$Expected.kind) {
        'missing' { return }
        'file' {
            Assert-Equal -Actual ([string]$actual.length) -Expected ([string]$Expected.length) -Label "Protected ignored length $path"
            Assert-Equal -Actual ([string]$actual.sha256) -Expected ([string]$Expected.sha256).ToUpperInvariant() -Label "Protected ignored hash $path"
            return
        }
        'directory' {
            $expectedEntries = @($Expected.entries | ForEach-Object { '{0}|{1}|{2}' -f $_.path,$_.length,([string]$_.sha256).ToUpperInvariant() })
            $actualEntries = @($actual.entries | ForEach-Object { '{0}|{1}|{2}' -f $_.path,$_.length,$_.sha256 })
            if (($expectedEntries -join "`n") -cne ($actualEntries -join "`n")) {
                throw "Protected ignored directory inventory changed: $path"
            }
            return
        }
        default { throw "Unsupported protected ignored kind for ${path}: $($Expected.kind)" }
    }
}

function Write-ProtectedManifest {
    if ([string]::IsNullOrWhiteSpace($ManifestOutputPath)) {
        throw 'CaptureManifest requires ManifestOutputPath.'
    }
    Assert-EmptyIndex
    Assert-PathEqual -Actual $RepositoryPath -Expected (Get-PrimaryWorktreePath -Root $RepositoryPath) -Label 'CaptureManifest primary worktree'
    $adopted = @($AdoptedUserOverlayPath |
        ForEach-Object { ConvertTo-RepoPath -Path $_ -Label 'Adopted user overlay path' } |
        Sort-Object -Unique)
    if ([string]::IsNullOrWhiteSpace($AdoptionAuthorizationReference)) {
        throw 'CaptureManifest requires AdoptionAuthorizationReference; use explicit none when no overlay is adopted.'
    }
    if ($adopted.Count -gt 0 -and $AdoptionAuthorizationReference -ceq 'none') {
        throw 'Adopted user-overlay paths require an explicit non-none authorization reference.'
    }
    $candidates = @(Get-OverlayCandidateEntries -Root $RepositoryPath)
    foreach ($path in $adopted) {
        $candidate = @($candidates | Where-Object { $_.path -ceq $path })
        if ($candidate.Count -ne 1) {
            throw "Adopted user-overlay path must be an existing tracked-dirty or ordinary-untracked file at capture: $path"
        }
        $storyEntry = @($script:ScopeDefinition.storyEntries | Where-Object { $_.path -ceq $path })
        $segmentEntry = @($script:ScopeDefinition.segmentEntries | Where-Object { $_.path -ceq $path })
        if ($storyEntry.Count -ne 1 -or $segmentEntry.Count -ne 1) {
            throw "Adopted user-overlay path must be in both full-Story and current-segment scope: $path"
        }
        if (-not $segmentEntry[0].required) {
            throw "Adopted user-overlay path must be a required current-segment scope entry: $path"
        }
        if ($segmentEntry[0].operation -cne $candidate[0].operation) {
            throw "Adopted user-overlay operation mismatch for ${path}: segment=$($segmentEntry[0].operation) actual=$($candidate[0].operation)"
        }
    }
    $protectedIgnored = @($ProtectedIgnoredPath |
        ForEach-Object { ConvertTo-RepoPath -Path $_ -Label 'Protected ignored path' } |
        Sort-Object -Unique)
    $ephemeralIgnored = @($EphemeralIgnoredPath |
        ForEach-Object { ConvertTo-RepoPath -Path $_ -Label 'Ephemeral ignored path' } |
        Sort-Object -Unique)
    Assert-SeparatePathClasses -Adopted $adopted -ProtectedIgnored $protectedIgnored -EphemeralIgnored $ephemeralIgnored
    Assert-IgnoredClassification -Root $RepositoryPath -ProtectedRoots $protectedIgnored -EphemeralRoots $ephemeralIgnored
    $paths = @(Get-ProtectedCandidatePaths -Root $RepositoryPath -AdoptedPaths $adopted)
    $entries = @($paths | ForEach-Object { New-ProtectedEntry -Root $RepositoryPath -RelativePath $_ })
    $protectedIgnoredRoots = @($protectedIgnored | ForEach-Object { New-ProtectedIgnoredRoot -Root $RepositoryPath -RelativePath $_ })
    $manifest = [ordered]@{
        schemaVersion = 2
        protectedRoot = $RepositoryPath
        captureHead = Get-GitText -Arguments @('rev-parse', 'HEAD')
        scopeManifestPath = $script:ScopeDefinition.path
        scopeManifestSha256 = $script:ScopeDefinition.sha256
        adoptedPaths = $adopted
        adoptionAuthorizationReference = $AdoptionAuthorizationReference
        protectedIgnoredRoots = $protectedIgnoredRoots
        ephemeralIgnoredPaths = $ephemeralIgnored
        entries = $entries
    }
    $output = Resolve-InputPath -Path $ManifestOutputPath -Label 'Manifest output path'
    Assert-ExternalGateArtifactPath -Path $output -Label 'Protected manifest output'
    if ($null -ne (Get-Item -Force -LiteralPath $output -ErrorAction SilentlyContinue)) {
        throw "Protected manifest output already exists; choose a new immutable path: $output"
    }
    if ($output.Equals($script:ScopeDefinition.path, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Protected manifest output cannot overwrite the scope manifest.'
    }
    $parent = Split-Path -Parent $output
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) {
        throw "Manifest output parent does not exist: $parent"
    }
    [System.IO.File]::WriteAllText(
        $output,
        ($manifest | ConvertTo-Json -Depth 6),
        [System.Text.UTF8Encoding]::new($false)
    )
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $output).Hash.ToUpperInvariant()
    Write-Output "Protected manifest: $output"
    Write-Output "Protected manifest SHA256: $hash"
    Write-Output "Protected entries: $($entries.Count)"
    Write-Output "Protected ignored roots: $($protectedIgnoredRoots.Count)"
}

function Assert-ProtectedManifest {
    if ([string]::IsNullOrWhiteSpace($ProtectedManifestPath)) {
        throw 'ProtectedManifestPath is required for every gate phase.'
    }
    if ($ProtectedManifestSha256 -notmatch '^[0-9a-fA-F]{64}$') {
        throw 'ProtectedManifestSha256 is required for every gate phase.'
    }
    $manifestPath = Resolve-InputPath -Path $ProtectedManifestPath -Label 'Protected manifest path'
    Assert-ExternalGateArtifactPath -Path $manifestPath -Label 'Protected manifest'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw "Protected manifest does not exist: $manifestPath"
    }
    $manifestItem = Get-Item -Force -LiteralPath $manifestPath
    if (($manifestItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw "Protected manifest cannot be a symlink or reparse point: $manifestPath"
    }
    $actualManifestHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $manifestPath).Hash.ToUpperInvariant()
    Assert-Equal -Actual $actualManifestHash -Expected $ProtectedManifestSha256.ToUpperInvariant() -Label 'Protected manifest SHA256'
    $manifest = Get-Content -Raw -Encoding UTF8 -LiteralPath $manifestPath | ConvertFrom-Json
    if ([int]$manifest.schemaVersion -ne 2) {
        throw "Unsupported protected manifest schema: $($manifest.schemaVersion)"
    }
    $root = [System.IO.Path]::GetFullPath([string]$manifest.protectedRoot)
    if (-not (Test-Path -LiteralPath $root -PathType Container)) {
        throw "Protected root does not exist: $root"
    }
    if ([string]::IsNullOrWhiteSpace($ExpectedProtectedRoot)) {
        throw 'ExpectedProtectedRoot is required for every gate phase.'
    }
    if ($ExpectedCaptureHead -notmatch '^[0-9a-fA-F]{40}$') {
        throw 'ExpectedCaptureHead is required for every gate phase.'
    }
    if ([string]::IsNullOrWhiteSpace($ExpectedAdoptionAuthorizationReference)) {
        throw 'ExpectedAdoptionAuthorizationReference is required; use explicit none when no overlay is adopted.'
    }
    Assert-PathEqual -Actual $root -Expected $ExpectedProtectedRoot -Label 'Protected manifest root'
    Assert-PathEqual -Actual $root -Expected (Get-PrimaryWorktreePath -Root $RepositoryPath) -Label 'Protected manifest primary worktree'
    Assert-Equal -Actual ([string]$manifest.captureHead) -Expected $ExpectedCaptureHead -Label 'Protected manifest capture HEAD'
    Assert-Commit -Sha $ExpectedCaptureHead -Label 'Protected manifest capture HEAD'
    Assert-Equal -Actual ([string]$manifest.adoptionAuthorizationReference) -Expected $ExpectedAdoptionAuthorizationReference -Label 'Protected manifest adoption authorization'
    Assert-PathEqual -Actual ([string]$manifest.scopeManifestPath) -Expected $script:ScopeDefinition.path -Label 'Protected manifest scope path'
    Assert-Equal -Actual ([string]$manifest.scopeManifestSha256) -Expected $script:ScopeDefinition.sha256 -Label 'Protected manifest scope SHA256'
    Assert-Equal -Actual (Get-GitCommonDirectory -Root $root) -Expected (Get-GitCommonDirectory -Root $RepositoryPath) -Label 'Protected manifest repository'
    $adopted = @($manifest.adoptedPaths | ForEach-Object { ConvertTo-RepoPath -Path ([string]$_) -Label 'Manifest adopted path' } | Sort-Object -Unique)
    $expectedAdopted = @($ExpectedAdoptedUserOverlayPath | ForEach-Object { ConvertTo-RepoPath -Path ([string]$_) -Label 'Expected adopted path' } | Sort-Object -Unique)
    if (($adopted -join "`n") -cne ($expectedAdopted -join "`n")) {
        throw "Protected manifest adopted paths mismatch. Expected [$($expectedAdopted -join ', ')], got [$($adopted -join ', ')]."
    }
    if ($adopted.Count -gt 0 -and ([string]$manifest.adoptionAuthorizationReference) -ceq 'none') {
        throw 'Protected manifest contains adopted paths without a non-none authorization reference.'
    }
    $protectedIgnoredRoots = @($manifest.protectedIgnoredRoots)
    $protectedIgnored = @($protectedIgnoredRoots | ForEach-Object { ConvertTo-RepoPath -Path ([string]$_.path) -Label 'Manifest protected ignored root' } | Sort-Object -Unique)
    $ephemeralIgnored = @($manifest.ephemeralIgnoredPaths | ForEach-Object { ConvertTo-RepoPath -Path ([string]$_) -Label 'Manifest ephemeral ignored path' } | Sort-Object -Unique)
    $expectedProtectedIgnored = @($ExpectedProtectedIgnoredPath | ForEach-Object { ConvertTo-RepoPath -Path ([string]$_) -Label 'Expected protected ignored root' } | Sort-Object -Unique)
    $expectedEphemeralIgnored = @($ExpectedEphemeralIgnoredPath | ForEach-Object { ConvertTo-RepoPath -Path ([string]$_) -Label 'Expected ephemeral ignored path' } | Sort-Object -Unique)
    if (($protectedIgnored -join "`n") -cne ($expectedProtectedIgnored -join "`n")) {
        throw "Protected ignored roots mismatch. Expected [$($expectedProtectedIgnored -join ', ')], got [$($protectedIgnored -join ', ')]."
    }
    if (($ephemeralIgnored -join "`n") -cne ($expectedEphemeralIgnored -join "`n")) {
        throw "Ephemeral ignored roots mismatch. Expected [$($expectedEphemeralIgnored -join ', ')], got [$($ephemeralIgnored -join ', ')]."
    }
    Assert-SeparatePathClasses -Adopted $adopted -ProtectedIgnored $protectedIgnored -EphemeralIgnored $ephemeralIgnored
    Assert-IgnoredClassification -Root $root -ProtectedRoots $protectedIgnored -EphemeralRoots $ephemeralIgnored
    $expectedEntries = @($manifest.entries)
    $expectedPaths = @($expectedEntries | ForEach-Object { ConvertTo-RepoPath -Path ([string]$_.path) -Label 'Manifest entry' } | Sort-Object -Unique)
    $actualPaths = @(Get-ProtectedCandidatePaths -Root $root -AdoptedPaths $adopted)
    if (($expectedPaths -join "`n") -cne ($actualPaths -join "`n")) {
        throw "Protected dirty/untracked inventory changed. Expected [$($expectedPaths -join ', ')], got [$($actualPaths -join ', ')]."
    }
    foreach ($entry in $expectedEntries) {
        $path = ConvertTo-RepoPath -Path ([string]$entry.path) -Label 'Manifest entry'
        $absolute = Join-Path $root ($path -replace '/', [System.IO.Path]::DirectorySeparatorChar)
        switch ([string]$entry.kind) {
            'file' {
                $item = Get-Item -Force -LiteralPath $absolute -ErrorAction SilentlyContinue
                if ($null -eq $item -or $item.PSIsContainer) {
                    throw "Protected file is missing or changed type: $path"
                }
                if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
                    throw "Protected file became a symlink or reparse point: $path"
                }
                Assert-Equal -Actual ([string]$item.Length) -Expected ([string]$entry.length) -Label "Protected file length $path"
                $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $absolute).Hash.ToUpperInvariant()
                Assert-Equal -Actual $hash -Expected ([string]$entry.sha256).ToUpperInvariant() -Label "Protected file hash $path"
            }
            'missing' {
                if ($null -ne (Get-Item -Force -LiteralPath $absolute -ErrorAction SilentlyContinue)) {
                    throw "Protected missing path now exists: $path"
                }
            }
            default {
                throw "Unsupported protected entry kind for ${path}: $($entry.kind)"
            }
        }
    }
    foreach ($ignoredRoot in $protectedIgnoredRoots) {
        Assert-ProtectedIgnoredRoot -Root $root -Expected $ignoredRoot
    }
    $script:VerifiedProtectedIgnoredRoots = $protectedIgnoredRoots
    $script:VerifiedProtectedIgnoredPaths = $protectedIgnored
    $script:VerifiedEphemeralIgnoredPaths = $ephemeralIgnored
    $script:VerifiedCaptureHead = [string]$manifest.captureHead
}

function Assert-NoPromptPlaceholders {
    if ([string]::IsNullOrWhiteSpace($GeneratedPromptPath)) {
        return
    }
    $path = Resolve-InputPath -Path $GeneratedPromptPath -Label 'Generated prompt path'
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Generated prompt does not exist: $path"
    }
    $text = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
    $matches = [regex]::Matches($text, '\{\{[^{}\r\n]+\}\}|\b(?:TBD|TODO)\b', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if ($matches.Count -gt 0) {
        $values = @($matches | ForEach-Object { $_.Value } | Sort-Object -Unique)
        throw "Generated prompt contains unresolved placeholders: $($values -join ', ')"
    }
}

function Get-RangeChanges {
    param([string]$BaseSha, [string]$EndSha, [string]$Label)

    $changed = [System.Collections.Generic.List[object]]::new()
    foreach ($line in @((Invoke-Git -Arguments @('-c', 'core.quotePath=false', 'diff', '--name-status', '--no-renames', "$BaseSha...$EndSha", '--')).Output)) {
        $parts = @(([string]$line) -split "`t", 2)
        if ($parts.Count -ne 2) {
            throw "Unable to parse $Label diff name-status: $line"
        }
        $operation = switch ($parts[0]) {
            'A' { 'add' }
            'M' { 'modify' }
            'D' { 'delete' }
            default { throw "Unsupported $Label diff operation '$($parts[0])': $($parts[1])" }
        }
        $changed.Add([pscustomobject]@{
            path = ConvertTo-RepoPath -Path $parts[1] -Label "$Label changed path"
            operation = $operation
        })
    }
    @($changed)
}

function Get-HistoryTouchedPaths {
    param([string]$BaseSha, [string]$EndSha, [string]$Label)

    $paths = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $commits = @((Invoke-Git -Arguments @('rev-list', '--reverse', "$BaseSha..$EndSha")).Output |
        Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) })
    foreach ($commit in $commits) {
        foreach ($line in @((Invoke-Git -Arguments @('-c', 'core.quotePath=false', 'diff-tree', '--no-commit-id', '--name-status', '-r', '--no-renames', ([string]$commit).Trim(), '--')).Output |
            Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) })) {
            $parts = @(([string]$line) -split "`t", 2)
            if ($parts.Count -ne 2 -or $parts[0] -notin @('A', 'M', 'D')) {
                throw "Unable to parse or authorize $Label commit-history operation: $line"
            }
            $null = $paths.Add((ConvertTo-RepoPath -Path $parts[1] -Label "$Label history path"))
        }
    }
    @($paths | Sort-Object)
}

function Assert-ScopeSet {
    param(
        [string]$BaseSha,
        [string]$EndSha,
        [object[]]$Entries,
        [object[]]$Envelopes,
        [string]$Label
    )

    $changed = @(Get-RangeChanges -BaseSha $BaseSha -EndSha $EndSha -Label $Label)
    $touched = @(Get-HistoryTouchedPaths -BaseSha $BaseSha -EndSha $EndSha -Label $Label)
    if ($changed.Count -eq 0) {
        throw "$Label final scope is empty."
    }
    if ($touched.Count -eq 0) {
        throw "$Label commit-history scope is empty."
    }
    foreach ($path in $touched) {
        if (@($Entries | Where-Object { $_.path -ceq $path }).Count -ne 1) {
            throw "$Label commit history touched a non-allowlisted path: $path"
        }
    }
    foreach ($change in $changed) {
        $allowed = @($Entries | Where-Object { $_.path -ceq $change.path })
        if ($allowed.Count -ne 1) {
            throw "$Label final scope contains a non-allowlisted path: $($change.path)"
        }
        if ($allowed[0].operation -cne $change.operation) {
            throw "$Label operation mismatch for $($change.path): expected=$($allowed[0].operation) actual=$($change.operation)"
        }
    }
    foreach ($entry in @($Entries | Where-Object { $_.required })) {
        if (@($changed | Where-Object { $_.path -ceq $entry.path }).Count -ne 1) {
            throw "Required $Label entry is absent from the final delta: $($entry.path)"
        }
    }
    foreach ($envelope in $Envelopes) {
        $actual = @($touched | ForEach-Object {
            $path = [string]$_
            @($Entries | Where-Object { $_.path -ceq $path })[0]
        } | Where-Object { $_.category -ceq $envelope.category }).Count
        if ($actual -gt [int]$envelope.hardMax) {
            throw "$Label commit-history scope exceeds hardMax for $($envelope.category): actual=$actual hardMax=$($envelope.hardMax)"
        }
        Write-Output "$Label envelope $($envelope.category): expected=$($envelope.expected) historyTouched=$actual hardMax=$($envelope.hardMax)"
    }
    $diffCheck = Invoke-Git -Arguments @('diff', '--check', "$BaseSha...$EndSha") -AllowFailure
    if ($diffCheck.ExitCode -ne 0) {
        throw "$Label diff check failed: $($diffCheck.Output -join [Environment]::NewLine)"
    }
    $changedText = @($changed | ForEach-Object { "$($_.operation):$($_.path)" })
    Write-Output "$Label final changed paths ($($changed.Count)): $($changedText -join ', ')"
    Write-Output "$Label history-touched paths ($($touched.Count)): $($touched -join ', ')"
}

function Assert-StoryScope {
    Assert-ScopeSet -BaseSha $StoryBaseSha -EndSha $StorySha -Entries @($script:ScopeDefinition.storyEntries) -Envelopes @($script:ScopeDefinition.storyEnvelopes) -Label 'Full Story'
    Assert-ScopeSet -BaseSha $script:ScopeDefinition.segmentBaseSha -EndSha $StorySha -Entries @($script:ScopeDefinition.segmentEntries) -Envelopes @($script:ScopeDefinition.segmentEnvelopes) -Label 'Current segment'
}

function Assert-MergeTopologyAndTree {
    $parents = @((Get-GitText -Arguments @('show', '-s', '--format=%P', $MergeSha)) -split ' ')
    if ($parents.Count -ne 2) {
        throw "Merge commit must have exactly two parents; got $($parents.Count)."
    }
    Assert-Equal -Actual $parents[0] -Expected $AcceptedRulesSha -Label 'Merge first parent'
    Assert-Equal -Actual $parents[1] -Expected $StorySha -Label 'Merge second parent'
    $expectedTreeResult = Invoke-Git -Arguments @('merge-tree', '--write-tree', $AcceptedRulesSha, $StorySha) -AllowFailure
    if ($expectedTreeResult.ExitCode -ne 0) {
        throw "Exact base/story merge is conflicted or unavailable: $($expectedTreeResult.Output -join [Environment]::NewLine)"
    }
    $expectedTree = ([string]$expectedTreeResult.Output[0]).Trim()
    $actualTree = Get-GitText -Arguments @('show', '-s', '--format=%T', $MergeSha)
    Assert-Equal -Actual $actualTree -Expected $expectedTree -Label 'Merge tree'
}

function Assert-MergeReceipt {
    foreach ($value in @($StoryId, $ContractVersion, $EvidenceGate)) {
        if ([string]::IsNullOrWhiteSpace($value)) {
            throw 'IntegrationPrePush/PostMerge require StoryId, ContractVersion, and EvidenceGate.'
        }
    }
    $contractPath = $CanonicalContractPath
    $contractResult = Invoke-Git -Arguments @('show', "${AcceptedRulesSha}:$contractPath") -AllowFailure
    if ($contractResult.ExitCode -ne 0) {
        throw "Accepted workflow contract is missing at ${AcceptedRulesSha}:$contractPath. Bootstrap governance must use the prior accepted process rather than this merge gate."
    }
    $contractText = $contractResult.Output -join "`n"
    $versionMatch = [regex]::Match($contractText, '(?m)^\*\*Contract version:\*\*\s*(\S+)\s*$')
    if (-not $versionMatch.Success) {
        throw "Accepted workflow contract version is not parseable at ${AcceptedRulesSha}:$contractPath."
    }
    Assert-Equal -Actual $ContractVersion -Expected $versionMatch.Groups[1].Value -Label 'Accepted workflow contract version'
    $message = Get-GitText -Arguments @('show', '-s', '--format=%B', $MergeSha)
    $required = [ordered]@{
        'Story-Id' = $StoryId
        'Story-Tip' = $StorySha
        'Reviewed-Base' = $AcceptedRulesSha
        'Review-Mode' = 'independent'
        'Review-Result' = 'passed'
        'Evidence-Gate' = $EvidenceGate
        'Scope-Manifest-SHA256' = $ScopeManifestSha256.ToUpperInvariant()
        'Workflow-Contract' = $ContractVersion
        'Workflow-Validator' = $script:AcceptedValidatorBlob
    }
    $lines = @($message -split "`r?`n")
    foreach ($key in $required.Keys) {
        $matches = @($lines | Where-Object {
            $separator = $_.IndexOf(':')
            $separator -gt 0 -and $_.Substring(0, $separator).Equals($key, [System.StringComparison]::OrdinalIgnoreCase)
        })
        if ($matches.Count -ne 1) {
            throw "Merge receipt must contain exactly one $key trailer; found $($matches.Count)."
        }
        $expectedLine = "$($key): $($required[$key])"
        if ($matches[0] -cne $expectedLine) {
            throw "Merge receipt trailer mismatch. Expected '$expectedLine', got '$($matches[0])'."
        }
    }
    $requiredLines = @($required.Keys | ForEach-Object { "$($_): $($required[$_])" })
    if ($lines.Count -lt ($requiredLines.Count + 2)) {
        throw 'Merge receipt trailer block is incomplete.'
    }
    $blockStart = $lines.Count - $requiredLines.Count
    $tail = @($lines[$blockStart..($lines.Count - 1)])
    if (($tail -join "`n") -cne ($requiredLines -join "`n")) {
        throw 'Merge receipt trailers must be one contiguous final block in canonical order.'
    }
    if ($blockStart -lt 2 -or -not [string]::IsNullOrWhiteSpace($lines[$blockStart - 1])) {
        throw 'Merge receipt trailer block requires a blank separator after a non-empty commit subject/body.'
    }
    if (@($lines[0..($blockStart - 2)] | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }).Count -eq 0) {
        throw 'Merge receipt requires a non-empty commit subject before the trailer block.'
    }
}

$inside = Get-GitText -Arguments @('rev-parse', '--is-inside-work-tree')
Assert-Equal -Actual $inside -Expected 'true' -Label 'Repository path'
Assert-NoGitObjectReplacement
Assert-Commit -Sha $AcceptedRulesSha -Label 'Accepted rules SHA'
Assert-RunningValidatorMatchesAcceptedRules
$script:ScopeDefinition = Read-ScopeManifest

if ($Phase -eq 'CaptureManifest') {
    Write-ProtectedManifest
    exit 0
}

Assert-NoPromptPlaceholders
Assert-ProtectedManifest
Assert-EmptyIndex

Invoke-Git -Arguments @('fetch', '--prune', 'origin') | Out-Null
$originMain = Get-GitText -Arguments @('rev-parse', 'refs/remotes/origin/main')
$localMain = Get-GitText -Arguments @('rev-parse', 'refs/heads/main')
Assert-AncestorsOfAcceptedRules

switch ($Phase) {
    'DevPreflight' {
        Assert-ValidStoryBranch
        if ([string]::IsNullOrWhiteSpace($DevMode)) {
            throw 'DevPreflight requires DevMode.'
        }
        Assert-Commit -Sha $StoryBaseSha -Label 'Story base SHA'
        Assert-Equal -Actual $originMain -Expected $AcceptedRulesSha -Label 'origin/main'
        Assert-Equal -Actual $localMain -Expected $AcceptedRulesSha -Label 'main'
        Assert-CaptureLineage
        switch ($DevMode) {
            'new_story' {
                Assert-Equal -Actual $StoryBaseSha -Expected $AcceptedRulesSha -Label 'New Story base'
                Assert-Equal -Actual $ExpectedRemoteStoryTip -Expected 'absent' -Label 'New Story expected remote tip'
                Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', 'HEAD')) -Expected $StoryBaseSha -Label 'New Story preflight HEAD'
                if (Test-RefExists -Ref "refs/heads/$StoryBranch") {
                    throw "New Story local branch already exists: $StoryBranch"
                }
                if (Test-RefExists -Ref "refs/remotes/origin/$StoryBranch") {
                    throw "New Story remote branch already exists: origin/$StoryBranch"
                }
            }
            'repair' {
                Assert-FullSha -Sha $ExpectedStoryParent -Label 'Repair expected Story parent'
                Assert-FullSha -Sha $ExpectedRemoteStoryTip -Label 'Repair expected remote Story tip'
                Assert-Commit -Sha $ExpectedStoryParent -Label 'Repair expected Story parent'
                Assert-Equal -Actual $ExpectedRemoteStoryTip -Expected $ExpectedStoryParent -Label 'Repair remote tip/parent'
                $history = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $StoryBaseSha, $ExpectedStoryParent) -AllowFailure
                if ($history.ExitCode -ne 0) {
                    throw "Story base $StoryBaseSha is not an ancestor of Repair parent $ExpectedStoryParent."
                }
                $alreadyMerged = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $ExpectedStoryParent, $AcceptedRulesSha) -AllowFailure
                if ($alreadyMerged.ExitCode -eq 0) {
                    throw "Repair parent is already an ancestor of accepted main; use a new Story instead of advancing the merged branch: $ExpectedStoryParent"
                }
                Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', 'HEAD')) -Expected $ExpectedStoryParent -Label 'Repair worktree HEAD'
                Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', "refs/heads/$StoryBranch")) -Expected $ExpectedStoryParent -Label 'Repair local Story tip'
                Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', "refs/remotes/origin/$StoryBranch")) -Expected $ExpectedRemoteStoryTip -Label 'Repair remote Story tip'
            }
        }
        Write-Output "PASS mechanical subset DevPreflight acceptedRules=$AcceptedRulesSha storyBase=$StoryBaseSha mode=$DevMode"
    }
    'ReviewPreflight' {
        Assert-Commit -Sha $StorySha -Label 'Story SHA'
        Assert-StoryHistory
        Assert-CaptureLineage
        Assert-ValidStoryBranch
        Assert-Equal -Actual $originMain -Expected $AcceptedRulesSha -Label 'origin/main'
        Assert-Equal -Actual $localMain -Expected $AcceptedRulesSha -Label 'main'
        Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', 'HEAD')) -Expected $StorySha -Label 'Review worktree HEAD'
        Assert-CleanImmutableWorktree
        Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', "refs/heads/$StoryBranch")) -Expected $StorySha -Label 'Local Story tip'
        Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', "refs/remotes/origin/$StoryBranch")) -Expected $StorySha -Label 'Remote Story tip'
        $alreadyMerged = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $StorySha, $AcceptedRulesSha) -AllowFailure
        if ($alreadyMerged.ExitCode -eq 0) {
            throw "Story SHA is already an ancestor of the review base: $StorySha"
        }
        Assert-StoryScope
        Write-Output "PASS mechanical subset ReviewPreflight reviewBase=$AcceptedRulesSha storyBase=$StoryBaseSha story=$StorySha"
    }
    'IntegrationPrePush' {
        Assert-Commit -Sha $StorySha -Label 'Story SHA'
        Assert-StoryHistory
        Assert-CaptureLineage
        Assert-Commit -Sha $MergeSha -Label 'Merge SHA'
        Assert-ValidStoryBranch
        Assert-Equal -Actual $originMain -Expected $AcceptedRulesSha -Label 'origin/main'
        Assert-Equal -Actual $localMain -Expected $AcceptedRulesSha -Label 'main'
        Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', "refs/heads/$StoryBranch")) -Expected $StorySha -Label 'Local Story tip'
        Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', "refs/remotes/origin/$StoryBranch")) -Expected $StorySha -Label 'Remote Story tip'
        Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', 'HEAD')) -Expected $MergeSha -Label 'Integration worktree HEAD'
        Assert-CleanImmutableWorktree
        Assert-MergeTopologyAndTree
        Assert-MergeReceipt
        Assert-StoryScope
        Write-Output "PASS mechanical subset IntegrationPrePush merge=$MergeSha story=$StorySha"
    }
    'PostMerge' {
        Assert-Commit -Sha $StorySha -Label 'Story SHA'
        Assert-StoryHistory
        Assert-CaptureLineage
        Assert-Commit -Sha $MergeSha -Label 'Merge SHA'
        Assert-Equal -Actual $originMain -Expected $MergeSha -Label 'origin/main'
        Assert-Equal -Actual $localMain -Expected $MergeSha -Label 'main'
        Assert-Equal -Actual (Get-GitText -Arguments @('rev-parse', 'HEAD')) -Expected $MergeSha -Label 'Post-merge worktree HEAD'
        Assert-CleanImmutableWorktree
        Assert-MergeTopologyAndTree
        Assert-MergeReceipt
        Assert-StoryScope
        foreach ($sha in $RequiredAncestorSha) {
            $merged = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $sha, $MergeSha) -AllowFailure
            if ($merged.ExitCode -ne 0) {
                throw "Prerequisite $sha is not an ancestor of merge $MergeSha."
            }
        }
        $storyAncestor = Invoke-Git -Arguments @('merge-base', '--is-ancestor', $StorySha, 'refs/heads/main') -AllowFailure
        if ($storyAncestor.ExitCode -ne 0) {
            throw "Story SHA is not an ancestor of main: $StorySha"
        }
        Write-Output "PASS mechanical subset PostMerge merge=$MergeSha story=$StorySha"
    }
}
