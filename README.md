## Git implementation from scratch in Kotlin + Spring Shell.
Project is named according to java conventions :)

## Currently implemented commands

- `zsv init` - Initialize empty zsv repository
- `zsv hash-object -w -f <file>` - Create blob object from file
- `zsv cat-file -f <blob-sha>` - Read blob object content
- `zsv write-tree` - Create tree object from current directory
- `zsv ls-tree -f <tree-sha>` - Display tree object content
- `zsv commit-tree -m <message> -t <tree-sha> -p <parent-sha>` - Create commit object
- `zsv commit -m <message>` - Create commit with current state
- `zsv checkout -b <branchName>` - Create or change branch
- `zsv log` - Log all commits
- `zsv add` - Add file to staging area
- `zsv cat-index` - Read index object content


## TODO:
- `zsv status`
- `zsv merge`
- `zsv add .`
- `zsv push` ? 

### Transport Protocol (git://)
Git protocol
- `zsv clone -url <git-url>` - Clone repository
