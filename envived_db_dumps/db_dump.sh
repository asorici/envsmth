#!/bin/bash

DB_USER="root"
DB_PASS="GroundAce55"
DB_NAME="envived"

if [ $# -lt 1 ]
then
    echo "Error in $0 - not enough arguments supplied. Usage: $0 [dump|restore] [db_file_name]"
exit 1
fi

if [ "$1" == "dump" ]
then
    filename="envived_dump_"$(date +%Y_%m_%d_%H_%M_%S)".sql"
    mysqldump -u $DB_USER -p$DB_PASS $DB_NAME > $filename
else
    if [ "$1" == "restore" -a $# -eq 2 ]
    then
        mysql -u $DB_USER -p$DB_PASS $DB_NAME < $2
    else
        echo "Error in $0 - wrong arguments supplied. Usage: $0 [dump|restore] [db_file_name]"
    fi
fi
