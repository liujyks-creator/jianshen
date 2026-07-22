[CmdletBinding()]
param(
    [string]$ValidatorPath = ''
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ([string]::IsNullOrWhiteSpace($ValidatorPath)) {
    $ValidatorPath = Join-Path (Split-Path -Parent $PSScriptRoot) 'validate-story-gate.ps1'
}
$ValidatorPath = (Resolve-Path -LiteralPath $ValidatorPath).Path
$PowerShellExe = (Get-Command powershell.exe -ErrorAction Stop).Source
$TempRootBase = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\', '/')
$RunRoot = Join-Path $TempRootBase ("TrainFlow-validate-story-gate-tests-{0}" -f [guid]::NewGuid().ToString('N'))
$MarkerName = '.validate-story-gate-test-root'
$script:Passed = 0
$script:Failed = 0
$script:Semantic = 0

function Write-Utf8NoBom {
    param([string]$Path, [string]$Text)
    $parent = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) {
        $null = New-Item -ItemType Directory -Path $parent
    }
    [System.IO.File]::WriteAllText($Path, $Text, [System.Text.UTF8Encoding]::new($false))
}

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)][string]$Repository,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [switch]$AllowFailure
    )
    $previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& git -C $Repository @Arguments 2>&1 | ForEach-Object { [string]$_ })
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
    if (-not $AllowFailure -and $exitCode -ne 0) {
        throw "git -C $Repository $($Arguments -join ' ') failed ($exitCode): $($output -join [Environment]::NewLine)"
    }
    [pscustomobject]@{ ExitCode = $exitCode; Output = @($output) }
}

function Get-GitText {
    param([string]$Repository, [string[]]$Arguments)
    ((Invoke-Git -Repository $Repository -Arguments $Arguments).Output -join "`n").Trim()
}

function Get-FileSha256 {
    param([string]$Path)
    (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToUpperInvariant()
}

function Invoke-Validator {
    param([string[]]$Arguments)
    $previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& $PowerShellExe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $ValidatorPath @Arguments 2>&1 |
            ForEach-Object { [string]$_ })
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
    [pscustomobject]@{ ExitCode = $exitCode; Output = ($output -join "`n") }
}

function Assert-CaseResult {
    param(
        [string]$Name,
        [object]$Result,
        [int]$ExpectedExit,
        [string]$Diagnostic
    )
    $exitMatches = if ($ExpectedExit -eq 0) { $Result.ExitCode -eq 0 } else { $Result.ExitCode -ne 0 }
    $diagnosticMatches = [string]::IsNullOrWhiteSpace($Diagnostic) -or $Result.Output.Contains($Diagnostic)
    if (-not $exitMatches -or -not $diagnosticMatches) {
        $script:Failed++
        throw "CASE FAIL [$Name] expectedExit=$ExpectedExit diagnostic='$Diagnostic' actualExit=$($Result.ExitCode)`n$($Result.Output)"
    }
    $script:Passed++
    Write-Output "PASS [$Name] exit=$($Result.ExitCode) diagnostic='$Diagnostic'"
}

function Write-SemanticLimitation {
    param([string]$Risk, [string]$Reason)
    $script:Semantic++
    Write-Output "SEMANTIC/NOT-MECHANICALLY-TESTABLE [$Risk] $Reason"
}

function New-Envelopes {
    param([object[]]$Entries)
    $categories = @('production', 'debug', 'test', 'docs', 'governance')
    @($categories | ForEach-Object {
        $category = $_
        $matching = @($Entries | Where-Object { $_.category -ceq $category })
        [ordered]@{
            category = $category
            expected = @($matching | Where-Object { $_.required }).Count
            hardMax = $matching.Count
        }
    })
}

function New-ScopeEntry {
    param(
        [string]$Path,
        [string]$Operation,
        [bool]$Required = $true,
        [string]$Category = 'docs'
    )
    [ordered]@{
        path = $Path
        operation = $Operation
        required = $Required
        category = $Category
        responsibility = "black-box fixture for $Path"
    }
}

function Write-ScopeManifest {
    param(
        [string]$Path,
        [string]$FullBase,
        [object[]]$FullEntries,
        [string]$SegmentBase,
        [object[]]$SegmentEntries
    )
    $manifest = [ordered]@{
        schemaVersion = 2
        fullStory = [ordered]@{
            baseSha = $FullBase
            entries = @($FullEntries)
            envelopes = @(New-Envelopes -Entries $FullEntries)
        }
        currentSegment = [ordered]@{
            baseSha = $SegmentBase
            entries = @($SegmentEntries)
            envelopes = @(New-Envelopes -Entries $SegmentEntries)
        }
    }
    Write-Utf8NoBom -Path $Path -Text ($manifest | ConvertTo-Json -Depth 8)
}

function New-BaseFixture {
    param(
        [string]$Name,
        [switch]$AlterAcceptedValidator
    )
    $root = Join-Path $RunRoot $Name
    $repo = Join-Path $root 'repo'
    $origin = Join-Path $root 'origin.git'
    $artifacts = Join-Path $root 'artifacts'
    $null = New-Item -ItemType Directory -Path $repo
    $null = New-Item -ItemType Directory -Path $artifacts
    $null = New-Item -ItemType Directory -Path $origin
    $null = Invoke-Git -Repository $origin -Arguments @('init', '--bare')
    $null = Invoke-Git -Repository $repo -Arguments @('init')
    $null = Invoke-Git -Repository $repo -Arguments @('config', 'user.name', 'Validator Test')
    $null = Invoke-Git -Repository $repo -Arguments @('config', 'user.email', 'validator@example.invalid')
    $null = Invoke-Git -Repository $repo -Arguments @('config', 'core.autocrlf', 'false')
    $null = Invoke-Git -Repository $repo -Arguments @('checkout', '-b', 'main')

    $validatorBytes = [System.IO.File]::ReadAllBytes($ValidatorPath)
    $validatorDestination = Join-Path $repo 'scripts\validate-story-gate.ps1'
    $null = New-Item -ItemType Directory -Path (Split-Path -Parent $validatorDestination)
    [System.IO.File]::WriteAllBytes($validatorDestination, $validatorBytes)
    if ($AlterAcceptedValidator) {
        [System.IO.File]::AppendAllText($validatorDestination, "`n# fixture accepted validator differs`n", [System.Text.UTF8Encoding]::new($false))
    }
    Write-Utf8NoBom -Path (Join-Path $repo 'docs\process\story-workflow-contract.md') -Text "# Fixture workflow contract`n`n**Contract version:** 1.0`n"
    Write-Utf8NoBom -Path (Join-Path $repo 'README.md') -Text "fixture base`n"
    $null = Invoke-Git -Repository $repo -Arguments @('add', '--', 'scripts/validate-story-gate.ps1', 'docs/process/story-workflow-contract.md', 'README.md')
    $null = Invoke-Git -Repository $repo -Arguments @('commit', '-m', 'Create accepted fixture rules')
    $accepted = Get-GitText -Repository $repo -Arguments @('rev-parse', 'HEAD')
    $null = Invoke-Git -Repository $repo -Arguments @('remote', 'add', 'origin', $origin)
    $null = Invoke-Git -Repository $repo -Arguments @('push', '-u', 'origin', 'main')
    [pscustomobject]@{
        Name = $Name
        Root = $root
        Repository = $repo
        Origin = $origin
        Artifacts = $artifacts
        Accepted = $accepted
        Branch = 'codex/test-story'
    }
}

function Invoke-CaptureManifest {
    param([object]$Fixture)
    $protected = Join-Path $Fixture.Artifacts 'protected.json'
    $result = Invoke-Validator -Arguments @(
        '-Phase', 'CaptureManifest',
        '-RepositoryPath', $Fixture.Repository,
        '-AcceptedRulesSha', $Fixture.Accepted,
        '-ScopeManifestPath', $Fixture.ScopePath,
        '-ScopeManifestSha256', $Fixture.ScopeHash,
        '-AdoptionAuthorizationReference', 'none',
        '-ManifestOutputPath', $protected
    )
    if ($result.ExitCode -ne 0) {
        throw "CaptureManifest failed for $($Fixture.Name):`n$($result.Output)"
    }
    $Fixture | Add-Member -NotePropertyName ProtectedPath -NotePropertyValue $protected -Force
    $Fixture | Add-Member -NotePropertyName ProtectedHash -NotePropertyValue (Get-FileSha256 -Path $protected) -Force
}

function Complete-NewStoryFixture {
    param(
        [object]$Fixture,
        [switch]$ProtectedUntracked
    )
    $entry = New-ScopeEntry -Path 'docs/story.md' -Operation 'add'
    $scope = Join-Path $Fixture.Artifacts 'scope.json'
    Write-ScopeManifest -Path $scope -FullBase $Fixture.Accepted -FullEntries @($entry) -SegmentBase $Fixture.Accepted -SegmentEntries @($entry)
    $Fixture | Add-Member -NotePropertyName ScopePath -NotePropertyValue $scope -Force
    $Fixture | Add-Member -NotePropertyName ScopeHash -NotePropertyValue (Get-FileSha256 -Path $scope) -Force
    if ($ProtectedUntracked) {
        Write-Utf8NoBom -Path (Join-Path $Fixture.Repository 'user-note.txt') -Text "protected user file`n"
    }
    Invoke-CaptureManifest -Fixture $Fixture
    $null = Invoke-Git -Repository $Fixture.Repository -Arguments @('checkout', '-b', $Fixture.Branch)
    Write-Utf8NoBom -Path (Join-Path $Fixture.Repository 'docs\story.md') -Text "story change`n"
    $null = Invoke-Git -Repository $Fixture.Repository -Arguments @('add', '--', 'docs/story.md')
    $null = Invoke-Git -Repository $Fixture.Repository -Arguments @('commit', '-m', 'Implement fixture story')
    $story = Get-GitText -Repository $Fixture.Repository -Arguments @('rev-parse', 'HEAD')
    $null = Invoke-Git -Repository $Fixture.Repository -Arguments @('push', '-u', 'origin', $Fixture.Branch)
    $Fixture | Add-Member -NotePropertyName Story -NotePropertyValue $story -Force
}

function Get-ReviewArguments {
    param([object]$Fixture, [string]$DevMode = 'new_story', [string]$ExpectedParent = 'none')
    $captureHead = if ($ExpectedParent -eq 'none') { $Fixture.Accepted } else { $ExpectedParent }
    @(
        '-Phase', 'ReviewPreflight',
        '-RepositoryPath', $Fixture.Repository,
        '-AcceptedRulesSha', $Fixture.Accepted,
        '-StoryBaseSha', $Fixture.Accepted,
        '-DevMode', $DevMode,
        '-ExpectedStoryParent', $ExpectedParent,
        '-ExpectedRemoteStoryTip', $Fixture.Story,
        '-StorySha', $Fixture.Story,
        '-StoryBranch', $Fixture.Branch,
        '-ScopeManifestPath', $Fixture.ScopePath,
        '-ScopeManifestSha256', $Fixture.ScopeHash,
        '-ProtectedManifestPath', $Fixture.ProtectedPath,
        '-ProtectedManifestSha256', $Fixture.ProtectedHash,
        '-ExpectedProtectedRoot', $Fixture.Repository,
        '-ExpectedCaptureHead', $captureHead,
        '-ExpectedAdoptionAuthorizationReference', 'none'
    )
}

function Get-MergeMessage {
    param([object]$Fixture)
    $validatorBlob = Get-GitText -Repository $Fixture.Repository -Arguments @('rev-parse', "$($Fixture.Accepted):scripts/validate-story-gate.ps1")
    @(
        'Merge fixture story',
        '',
        'Story-Id: TEST-1',
        "Story-Tip: $($Fixture.Story)",
        "Reviewed-Base: $($Fixture.Accepted)",
        'Review-Mode: independent',
        'Review-Result: passed',
        'Evidence-Gate: not_required',
        "Scope-Manifest-SHA256: $($Fixture.ScopeHash)",
        'Workflow-Contract: 1.0',
        "Workflow-Validator: $validatorBlob"
    ) -join "`n"
}

function New-DetachedMerge {
    param([object]$Fixture)
    $null = Invoke-Git -Repository $Fixture.Repository -Arguments @('checkout', '--detach', $Fixture.Accepted)
    $message = Get-MergeMessage -Fixture $Fixture
    $null = Invoke-Git -Repository $Fixture.Repository -Arguments @('merge', '--no-ff', $Fixture.Story, '-m', $message)
    $merge = Get-GitText -Repository $Fixture.Repository -Arguments @('rev-parse', 'HEAD')
    $Fixture | Add-Member -NotePropertyName Merge -NotePropertyValue $merge -Force
}

function Get-IntegrationArguments {
    param([object]$Fixture, [string]$Phase)
    @(
        '-Phase', $Phase,
        '-RepositoryPath', $Fixture.Repository,
        '-AcceptedRulesSha', $Fixture.Accepted,
        '-StoryBaseSha', $Fixture.Accepted,
        '-DevMode', 'new_story',
        '-ExpectedStoryParent', 'none',
        '-ExpectedRemoteStoryTip', $Fixture.Story,
        '-StorySha', $Fixture.Story,
        '-StoryBranch', $Fixture.Branch,
        '-MergeSha', $Fixture.Merge,
        '-StoryId', 'TEST-1',
        '-ContractVersion', '1.0',
        '-EvidenceGate', 'not_required',
        '-ScopeManifestPath', $Fixture.ScopePath,
        '-ScopeManifestSha256', $Fixture.ScopeHash,
        '-ProtectedManifestPath', $Fixture.ProtectedPath,
        '-ProtectedManifestSha256', $Fixture.ProtectedHash,
        '-ExpectedProtectedRoot', $Fixture.Repository,
        '-ExpectedCaptureHead', $Fixture.Accepted,
        '-ExpectedAdoptionAuthorizationReference', 'none'
    )
}

function Publish-Merge {
    param([object]$Fixture)
    $null = Invoke-Git -Repository $Fixture.Repository -Arguments @('push', 'origin', "$($Fixture.Merge):refs/heads/main")
    $null = Invoke-Git -Repository $Fixture.Repository -Arguments @('branch', '-f', 'main', $Fixture.Merge)
    $null = Invoke-Git -Repository $Fixture.Repository -Arguments @('checkout', 'main')
}

function New-CommitWithSameTree {
    param([object]$Fixture, [string]$Parent, [string]$Message)
    $tree = Get-GitText -Repository $Fixture.Repository -Arguments @('show', '-s', '--format=%T', $Parent)
    Get-GitText -Repository $Fixture.Repository -Arguments @('commit-tree', $tree, '-p', $Parent, '-m', $Message)
}

function Run-Case {
    param([string]$Name, [scriptblock]$Body)
    try {
        & $Body
    } catch {
        $script:Failed++
        throw "CASE ERROR [$Name] $($_.Exception.Message)"
    }
}

try {
    $null = New-Item -ItemType Directory -Path $RunRoot
    Write-Utf8NoBom -Path (Join-Path $RunRoot $MarkerName) -Text "owned by validate-story-gate.Tests.ps1`n"

    Run-Case 'review success' {
        $f = New-BaseFixture -Name 'review-success'
        Complete-NewStoryFixture -Fixture $f
        Assert-CaseResult -Name 'review success' -Result (Invoke-Validator -Arguments (Get-ReviewArguments -Fixture $f)) -ExpectedExit 0 -Diagnostic 'PASS mechanical subset ReviewPreflight'
    }

    Run-Case 'moving Story tip' {
        $f = New-BaseFixture -Name 'moving-story'
        Complete-NewStoryFixture -Fixture $f
        $advanced = New-CommitWithSameTree -Fixture $f -Parent $f.Story -Message 'Advance remote Story ref'
        $null = Invoke-Git -Repository $f.Repository -Arguments @('push', 'origin', "$advanced`:refs/heads/$($f.Branch)")
        Assert-CaseResult -Name 'moving Story tip' -Result (Invoke-Validator -Arguments (Get-ReviewArguments -Fixture $f)) -ExpectedExit 1 -Diagnostic 'Remote Story tip mismatch'
    }

    Run-Case 'moving main' {
        $f = New-BaseFixture -Name 'moving-main'
        Complete-NewStoryFixture -Fixture $f
        $advanced = New-CommitWithSameTree -Fixture $f -Parent $f.Accepted -Message 'Advance remote main ref'
        $null = Invoke-Git -Repository $f.Repository -Arguments @('push', 'origin', "$advanced`:refs/heads/main")
        Assert-CaseResult -Name 'moving main' -Result (Invoke-Validator -Arguments (Get-ReviewArguments -Fixture $f)) -ExpectedExit 1 -Diagnostic 'origin/main mismatch'
    }

    Run-Case 'non-empty index' {
        $f = New-BaseFixture -Name 'non-empty-index'
        Complete-NewStoryFixture -Fixture $f
        Write-Utf8NoBom -Path (Join-Path $f.Repository 'docs\story.md') -Text "staged mutation`n"
        $null = Invoke-Git -Repository $f.Repository -Arguments @('add', '--', 'docs/story.md')
        Assert-CaseResult -Name 'non-empty index' -Result (Invoke-Validator -Arguments (Get-ReviewArguments -Fixture $f)) -ExpectedExit 1 -Diagnostic 'Index is not empty'
    }

    Run-Case 'protected inventory deletion' {
        $f = New-BaseFixture -Name 'protected-deletion'
        Complete-NewStoryFixture -Fixture $f -ProtectedUntracked
        Remove-Item -LiteralPath (Join-Path $f.Repository 'user-note.txt')
        Assert-CaseResult -Name 'protected inventory deletion' -Result (Invoke-Validator -Arguments (Get-ReviewArguments -Fixture $f)) -ExpectedExit 1 -Diagnostic 'Protected dirty/untracked inventory changed'
    }

    Run-Case 'accepted validator identity' {
        $f = New-BaseFixture -Name 'validator-identity' -AlterAcceptedValidator
        $entry = New-ScopeEntry -Path 'docs/story.md' -Operation 'add'
        $scope = Join-Path $f.Artifacts 'scope.json'
        Write-ScopeManifest -Path $scope -FullBase $f.Accepted -FullEntries @($entry) -SegmentBase $f.Accepted -SegmentEntries @($entry)
        $result = Invoke-Validator -Arguments @('-Phase','CaptureManifest','-RepositoryPath',$f.Repository,'-AcceptedRulesSha',$f.Accepted,'-ScopeManifestPath',$scope,'-ScopeManifestSha256',(Get-FileSha256 $scope),'-AdoptionAuthorizationReference','none','-ManifestOutputPath',(Join-Path $f.Artifacts 'protected.json'))
        Assert-CaseResult -Name 'accepted validator identity' -Result $result -ExpectedExit 1 -Diagnostic 'Running validator blob mismatch'
    }

    Run-Case 'unauthorized path touched then restored' {
        $f = New-BaseFixture -Name 'touch-restore'
        Complete-NewStoryFixture -Fixture $f
        Write-Utf8NoBom -Path (Join-Path $f.Repository 'README.md') -Text "temporary unauthorized change`n"
        $null = Invoke-Git -Repository $f.Repository -Arguments @('add','--','README.md')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('commit','-m','Touch unauthorized path')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('checkout',$f.Accepted,'--','README.md')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('commit','-am','Restore unauthorized path')
        $f.Story = Get-GitText -Repository $f.Repository -Arguments @('rev-parse','HEAD')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('push','origin',"HEAD:refs/heads/$($f.Branch)")
        Assert-CaseResult -Name 'unauthorized path touched then restored' -Result (Invoke-Validator -Arguments (Get-ReviewArguments -Fixture $f)) -ExpectedExit 1 -Diagnostic 'commit history touched a non-allowlisted path: README.md'
    }

    Run-Case 'Repair current-segment escape' {
        $f = New-BaseFixture -Name 'repair-segment'
        $null = Invoke-Git -Repository $f.Repository -Arguments @('checkout','-b',$f.Branch)
        Write-Utf8NoBom -Path (Join-Path $f.Repository 'docs\story.md') -Text "initial story`n"
        $null = Invoke-Git -Repository $f.Repository -Arguments @('add','--','docs/story.md')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('commit','-m','Initial Story')
        $parent = Get-GitText -Repository $f.Repository -Arguments @('rev-parse','HEAD')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('push','-u','origin',$f.Branch)
        $full = @((New-ScopeEntry 'docs/story.md' 'add'), (New-ScopeEntry 'docs/extra.md' 'add'))
        $segment = @((New-ScopeEntry 'docs/story.md' 'modify' $false))
        $scope = Join-Path $f.Artifacts 'scope.json'
        Write-ScopeManifest -Path $scope -FullBase $f.Accepted -FullEntries $full -SegmentBase $parent -SegmentEntries $segment
        $f | Add-Member ScopePath $scope
        $f | Add-Member ScopeHash (Get-FileSha256 $scope)
        Invoke-CaptureManifest -Fixture $f
        Write-Utf8NoBom -Path (Join-Path $f.Repository 'docs\extra.md') -Text "out of Repair segment`n"
        $null = Invoke-Git -Repository $f.Repository -Arguments @('add','--','docs/extra.md')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('commit','-m','Escape Repair segment')
        $f | Add-Member Story (Get-GitText -Repository $f.Repository -Arguments @('rev-parse','HEAD'))
        $null = Invoke-Git -Repository $f.Repository -Arguments @('push','origin',"HEAD:refs/heads/$($f.Branch)")
        Assert-CaseResult -Name 'Repair current-segment escape' -Result (Invoke-Validator -Arguments (Get-ReviewArguments -Fixture $f -DevMode repair -ExpectedParent $parent)) -ExpectedExit 1 -Diagnostic 'Current segment commit history touched a non-allowlisted path: docs/extra.md'
    }

    Run-Case 'extra merge-tree edit' {
        $f = New-BaseFixture -Name 'merge-tree-mismatch'
        Complete-NewStoryFixture -Fixture $f
        New-DetachedMerge -Fixture $f
        Write-Utf8NoBom -Path (Join-Path $f.Repository 'extra.txt') -Text "unreviewed merge edit`n"
        $null = Invoke-Git -Repository $f.Repository -Arguments @('add','--','extra.txt')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('commit','--amend','--no-edit')
        $f.Merge = Get-GitText -Repository $f.Repository -Arguments @('rev-parse','HEAD')
        Assert-CaseResult -Name 'extra merge-tree edit' -Result (Invoke-Validator -Arguments (Get-IntegrationArguments -Fixture $f -Phase 'IntegrationPrePush')) -ExpectedExit 1 -Diagnostic 'Merge tree mismatch'
    }

    Run-Case 'push/ref advancement rejection' {
        $f = New-BaseFixture -Name 'push-ref-advance'
        Complete-NewStoryFixture -Fixture $f
        New-DetachedMerge -Fixture $f
        $advanced = New-CommitWithSameTree -Fixture $f -Parent $f.Accepted -Message 'Concurrent main advance'
        $null = Invoke-Git -Repository $f.Repository -Arguments @('push','origin',"$advanced`:refs/heads/main")
        Assert-CaseResult -Name 'push/ref advancement rejection' -Result (Invoke-Validator -Arguments (Get-IntegrationArguments -Fixture $f -Phase 'IntegrationPrePush')) -ExpectedExit 1 -Diagnostic 'origin/main mismatch'
    }

    Run-Case 'Integration and PostMerge success' {
        $f = New-BaseFixture -Name 'postmerge-success'
        Complete-NewStoryFixture -Fixture $f
        New-DetachedMerge -Fixture $f
        Assert-CaseResult -Name 'IntegrationPrePush success' -Result (Invoke-Validator -Arguments (Get-IntegrationArguments -Fixture $f -Phase 'IntegrationPrePush')) -ExpectedExit 0 -Diagnostic 'PASS mechanical subset IntegrationPrePush'
        Publish-Merge -Fixture $f
        $postMergeArguments = @(Get-IntegrationArguments -Fixture $f -Phase 'PostMerge')
        $postMergeArguments += @('-RequiredAncestorSha', $f.Accepted)
        Assert-CaseResult -Name 'PostMerge success' -Result (Invoke-Validator -Arguments $postMergeArguments) -ExpectedExit 0 -Diagnostic 'PASS mechanical subset PostMerge'
    }

    Run-Case 'PostMerge synchronization rejection' {
        $f = New-BaseFixture -Name 'postmerge-sync'
        Complete-NewStoryFixture -Fixture $f
        New-DetachedMerge -Fixture $f
        Publish-Merge -Fixture $f
        $advanced = New-CommitWithSameTree -Fixture $f -Parent $f.Merge -Message 'Advance after merge'
        $null = Invoke-Git -Repository $f.Repository -Arguments @('push','origin',"$advanced`:refs/heads/main")
        Assert-CaseResult -Name 'PostMerge synchronization rejection' -Result (Invoke-Validator -Arguments (Get-IntegrationArguments -Fixture $f -Phase 'PostMerge')) -ExpectedExit 1 -Diagnostic 'origin/main mismatch'
    }

    Run-Case 'sealed-like path requires exact authorization' {
        $f = New-BaseFixture -Name 'sealed-authorization'
        Complete-NewStoryFixture -Fixture $f
        Write-Utf8NoBom -Path (Join-Path $f.Repository 'archive\sealed.md') -Text "sealed history`n"
        $null = Invoke-Git -Repository $f.Repository -Arguments @('add','--','archive/sealed.md')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('commit','-m','Touch sealed-like path')
        $f.Story = Get-GitText -Repository $f.Repository -Arguments @('rev-parse','HEAD')
        $null = Invoke-Git -Repository $f.Repository -Arguments @('push','origin',"HEAD:refs/heads/$($f.Branch)")
        Assert-CaseResult -Name 'sealed-like path requires exact authorization' -Result (Invoke-Validator -Arguments (Get-ReviewArguments -Fixture $f)) -ExpectedExit 1 -Diagnostic 'commit history touched a non-allowlisted path: archive/sealed.md'
    }

    Write-SemanticLimitation -Risk 'stale evidence/artifact identity' -Reason 'The CLI binds Evidence-Gate and merge receipt text but has no artifact/source/timestamp inputs; evidence sufficiency and staleness require independent semantic Review.'
    Write-SemanticLimitation -Risk 'sealed classification semantics' -Reason 'The validator enforces exact scope authorization, as tested, but it does not know whether an allowed path is historically classified sealed.'
    Write-SemanticLimitation -Risk 'reviewer self-fix and shared-worktree delegation' -Reason 'Immutable refs, clean trees, and accepted validator identity are mechanical; actor identity and delegation history are outside the validator CLI.'
    Write-SemanticLimitation -Risk 'candidate governance approval' -Reason 'Fixtures test the candidate as if previously accepted; they do not and cannot approve the candidate governance Story under its own proposed rules.'

    Write-Output "SUMMARY mechanicalPass=$script:Passed mechanicalFail=$script:Failed semanticLimitations=$script:Semantic"
    if ($script:Failed -ne 0) {
        exit 1
    }
} finally {
    $fullRunRoot = [System.IO.Path]::GetFullPath($RunRoot).TrimEnd('\', '/')
    $marker = Join-Path $fullRunRoot $MarkerName
    $underTemp = $fullRunRoot.StartsWith($TempRootBase + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)
    if ($underTemp -and (Test-Path -LiteralPath $marker -PathType Leaf)) {
        Remove-Item -LiteralPath $fullRunRoot -Recurse -Force
    } elseif (Test-Path -LiteralPath $fullRunRoot) {
        Write-Error "Refusing cleanup because test-root ownership verification failed: $fullRunRoot"
    }
}
