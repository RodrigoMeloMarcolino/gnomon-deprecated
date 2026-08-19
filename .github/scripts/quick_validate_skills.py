from pathlib import Path
import re
import sys

for skill in Path(".codex/skills").iterdir():
    content = (skill / "SKILL.md").read_text()
    match = re.match(r"^---\n(.*?)\n---", content, re.DOTALL)
    if not match:
        sys.exit(f"missing frontmatter: {skill}")
    header = match.group(1)
    name = re.search(r"^name: ([a-z0-9-]+)$", header, re.MULTILINE)
    description = re.search(r"^description:", header, re.MULTILINE)
    if not name or not description:
        sys.exit(f"invalid skill: {skill}")
print("versioned skills are valid")
