#!/bin/bash

# Create server directory
mkdir git-test-server
cd git-test-server || exit

# Create bare repository
mkdir test-repo.git
cd test-repo.git || exit
git init --bare
cd ..

# Create working repository with test data
mkdir test-repo
cd test-repo || exit
git init

echo "Test" > test.txt
git add .
git commit -m "Initial commit"

# Connect and push to our "server" repository
git remote add origin ../test-repo.git
git push origin master
cd ..

# Start git daemon
git daemon --reuseaddr --verbose --base-path=. --export-all