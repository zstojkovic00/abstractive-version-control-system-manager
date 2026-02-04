## Git implementation from scratch in Kotlin + Spring Shell.
Project is named according to java conventions :)

## How it works

File content is transformed into repository objects through content-addressable storage:

1. **Compression** - File content is compressed using DEFLATE algorithm (zlib)
2. **Hashing** - SHA-1 hash is computed from uncompressed content, serving as unique identifier
3. **Storage** - Compressed content stored in `.zsv/objects/{first-2-chars}/{remaining-38-chars}`

Three object types form a Merkle tree structure:
- **Blob** - Stores file content (`blob {size}\0{content}`)
- **Tree** - Represents directory structure, points to blobs and subtrees (`tree {size}\0{mode} {name}\0{sha}...`)
- **Commit** - Snapshot metadata with pointer to root tree and parent commit

Branching is lightweight - branches are just text files containing commit SHA. Merging uses Last Common Ancestor (LCA) algorithm to determine fast-forward or three-way merge strategy.


### Demo
![demo](demo.gif)

> Note: `demo.cast` appears as "not staged" because it was being modified by asciinema during the recording.


## Prerequisites

1. [SDKMAN!](https://sdkman.io/)

```bash
sdk env install   # Install GraalVM and Gradle from .sdkmanrc
sdk env
```

## Usage

### Run with Gradle
```bash
./gradlew bootRun
```

### Native Image (GraalVM)
```bash
./gradlew nativeCompile
./build/native/nativeCompile/zsv
```

## Commands

- `zsv init` - Initialize empty zsv repository
- `zsv hash-object -w -f <file>` - Create blob object from file
- `zsv cat-file -f <blob-sha>` - Read blob object content
- `zsv write-tree` - Create tree object from current directory
- `zsv ls-tree -f <tree-sha>` - Display tree object content
- `zsv commit-tree -m <message> -t <tree-sha> -p <parent-sha>` - Create commit object
- `zsv commit -m <message>` - Create commit with current state
- `zsv checkout -b <branchName>` - Create or change branch
- `zsv merge -b <branchName>` - Merge branch (fast-forward or three-way)
- `zsv log` - Log all commits
- `zsv add` - Add file to staging area
- `zsv cat-index` - Read index object content
- `zsv status` - Display paths that have differences between the index file and current HEAD commit

## .zsv Directory Structure

```
.zsv/
├── objects/           # Compressed git objects (blob, tree, commit)
│   └── {2chars}/      # Sharded by first 2 SHA chars
│       └── {38chars}  # Remaining SHA as filename
├── refs/
│   └── heads/         # Branch files (contain commit SHA)
│       └── master
├── HEAD               # ref: refs/heads/{current-branch}
└── index              # Binary staging area
```

## Transport Protocol (git://)
- `zsv clone -url <git-url>` - Clone repository via native Git protocol (port 9418)
### TODO
- `zsv push`

