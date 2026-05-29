# Memory Convention

Project-specific memory files live in `.claude/memory/` in this repo (version controlled), not in `~/.claude/projects/`.

When writing a new memory file for this project:
1. Write the file to `.claude/memory/` in this repo
2. Reference it in `~/.claude/projects/-home-nate-Desktop-git/memory/MEMORY.md` using an absolute path
3. Do NOT create the file under `~/.claude/projects/.../memory/` directly
