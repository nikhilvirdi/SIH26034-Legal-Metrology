# Quick Git Reference - Android Project

## 🚀 Quick Commands

### Commit the Cleanup
```bash
# Stage the gitignore and cleanup
git add .gitignore GIT_CLEANUP_SUMMARY.md QUICK_GIT_REFERENCE.md

# Commit the removal of build artifacts
git commit -m "chore: clean up build artifacts and IDE configs

- Add comprehensive Android .gitignore
- Remove 112 build/cache/IDE files from tracking
- Keep gradle-wrapper.jar (required for builds)
- Files will regenerate on next build"
```

### Commit the ArUco Migration
```bash
# Stage all migration changes
git add mobile-app/ ARUCO_MIGRATION_SUMMARY.md docs/ARUCO_MARKER_GENERATION.md

# Commit the migration
git commit -m "feat: migrate from ARCore to OpenCV ArUco markers

Replace ARCore 3D depth estimation with OpenCV ArUco marker detection.
See ARUCO_MIGRATION_SUMMARY.md for complete details.

Changes:
- Remove ARCore/SceneView dependencies
- Add CameraX 1.3.1 and OpenCV 4.9.0
- Implement ArucoScaleAnalyzer for marker detection
- Rewrite ScanScreen with CameraX
- Add marker generation documentation

Benefits: Universal compatibility, deterministic accuracy, instant calibration"
```

### Push to Remote
```bash
git push origin main
```

## 📋 Common Git Commands

### Check Status
```bash
git status                    # See what's changed
git status --short            # Compact view
git diff                      # See file changes
```

### Undo Mistakes
```bash
# Unstage a file
git restore --staged <file>

# Discard local changes
git restore <file>

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes) - DANGEROUS
git reset --hard HEAD~1
```

### View History
```bash
git log --oneline            # Compact history
git log --oneline --graph    # Visual branch graph
git show <commit-hash>       # Show specific commit
```

## 🔍 What Gets Ignored Now?

### ❌ Never Committed (Security)
- `*.jks`, `*.keystore` - Signing keys
- `local.properties` - SDK paths, secrets
- `keystore.properties` - Signing config

### ❌ Never Committed (Build Artifacts)
- `.gradle/` - Gradle cache
- `build/` - Build output
- `.idea/` - IDE settings
- `*.iml` - Module files

### ✅ Always Committed (Required)
- `gradle-wrapper.jar` - Gradle wrapper
- `*.gradle.kts` - Build scripts
- `settings.gradle.kts` - Project settings
- Source code (`*.kt`, `*.java`)

## 🛠️ Team Onboarding

### For New Team Members
```bash
# 1. Clone the repo
git clone https://github.com/your-org/SIH26034-Legal-Metrology.git

# 2. Navigate to project
cd SIH26034-Legal-Metrology/mobile-app

# 3. Android Studio will create local.properties automatically
# Or create manually:
echo "sdk.dir=/path/to/your/Android/Sdk" > local.properties

# 4. Sync and build
./gradlew build
```

### For Existing Team Members (After Pull)
```bash
# 1. Pull latest changes
git pull origin main

# 2. Clean your local build
cd mobile-app
./gradlew clean

# 3. Delete old IDE config (optional but recommended)
rm -rf .idea
rm -rf .gradle

# 4. Reopen in Android Studio
# Android Studio will regenerate .idea/ automatically
```

## 🚨 If You Accidentally Commit Build Artifacts

```bash
# 1. Remove from Git (keeps local file)
git rm --cached path/to/file

# 2. Add to .gitignore
echo "path/to/file" >> .gitignore

# 3. Commit the fix
git add .gitignore
git commit -m "chore: stop tracking build artifact"

# 4. Push
git push origin main
```

## 📊 Repository Health Check

### Check Repository Size
```bash
# Windows PowerShell
(Get-ChildItem .git -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB

# Git command (any OS)
git count-objects -vH
```

### Check What's Being Tracked
```bash
# List all tracked files
git ls-files

# Count tracked files
git ls-files | wc -l

# Find large files
git ls-files | xargs ls -lh | sort -k5 -rh | head -20
```

### Clean Dangling Objects (Reclaim Space)
```bash
# Remove unreachable objects
git gc --prune=now --aggressive
```

## 🔄 Branch Workflow

### Create Feature Branch
```bash
# Create and switch to new branch
git checkout -b feature/aruco-debugging

# Make changes and commit
git add .
git commit -m "fix: improve ArUco marker detection in low light"

# Push to remote
git push origin feature/aruco-debugging
```

### Merge Feature Back
```bash
# Switch to main
git checkout main

# Pull latest
git pull origin main

# Merge feature
git merge feature/aruco-debugging

# Push
git push origin main

# Delete feature branch
git branch -d feature/aruco-debugging
git push origin --delete feature/aruco-debugging
```

## 🎯 Pro Tips

### Ignore Files Globally (Your Machine Only)
```bash
# Create global gitignore
git config --global core.excludesfile ~/.gitignore_global

# Add OS-specific files
echo ".DS_Store" >> ~/.gitignore_global
echo "Thumbs.db" >> ~/.gitignore_global
echo "*.swp" >> ~/.gitignore_global
```

### Git Aliases (Save Time)
```bash
# Add to ~/.gitconfig or run these commands:
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.cm "commit -m"
git config --global alias.lg "log --oneline --graph --all"

# Now use shortcuts:
git st              # Instead of git status
git co main         # Instead of git checkout main
git cm "message"    # Instead of git commit -m "message"
```

### View Ignored Files
```bash
# See what's being ignored
git status --ignored

# Check if specific file is ignored
git check-ignore -v path/to/file
```

## 📝 Commit Message Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation only
- `style:` - Formatting (no code change)
- `refactor:` - Code restructuring
- `perf:` - Performance improvement
- `test:` - Adding tests
- `chore:` - Maintenance (build, deps)

### Examples
```bash
git commit -m "feat(camera): add ArUco marker detection"
git commit -m "fix(scan): resolve marker lock debounce issue"
git commit -m "docs: add ArUco marker generation guide"
git commit -m "chore: update CameraX to 1.3.1"
```

## 🔐 Security Checklist

Before pushing:
- [ ] No `local.properties` in commit
- [ ] No `*.jks` or `*.keystore` files
- [ ] No API keys or secrets in code
- [ ] No passwords in commit messages
- [ ] No tokens in configuration files

### Check for Secrets
```bash
# Search for potential secrets
git grep -i "api[_-]key"
git grep -i "password"
git grep -i "secret"
git grep -E "[0-9]{16,}" # Long numbers (could be keys)
```

## 🆘 Emergency: Remove Secret from History

**If you accidentally committed a secret:**

```bash
# 1. Use BFG Repo-Cleaner (recommended)
# Download from: https://rtyley.github.io/bfg-repo-cleaner/
java -jar bfg.jar --replace-text passwords.txt
git reflog expire --expire=now --all
git gc --prune=now --aggressive

# 2. Force push (coordinate with team!)
git push origin main --force

# 3. Rotate the compromised secret immediately
```

---

**Generated**: 2026-09-17  
**Project**: SIH26034 Legal Metrology Inspector  
**Git Version**: 2.x
