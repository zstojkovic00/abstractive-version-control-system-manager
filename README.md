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


## TODO:
### Transport Protocol (git://)
Git protocol
- `zsv clone -url <git-url>` - Clone repository
