#!/bin/bash

# location of envived folder
ENVIVED_FOLDER=../../envived/

# get list of changed files since last commit
modifiedFiles=$(git diff HEAD --name-only --diff-filter=AM)

echo "==== Copying over to SVN folder files that were Added and Modified ... ===="
printf "%s\n" "${modifiedFiles[@]}"
cp --parents -r $modifiedFiles $ENVIVED_FOLDER

echo ""
echo "==== Copy done. Here is the list of files that have been deleted or renamed ... ===="
git diff HEAD --name-only --diff-filter=DR
