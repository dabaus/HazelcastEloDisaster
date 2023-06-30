#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 -U postgres <<-EOSQL
	CREATE DATABASE "hazeldb";
EOSQL

psql -v ON_ERROR_STOP=1 -U postgres -d hazeldb <<-EOSQL
	create table if not exists ratings( 
            name varchar(50) primary key, 
            elosum int, 
            nogames int, 
          rating int);

	insert into ratings(name, elosum,nogames,rating) VALUES('Wiliam', 0 ,0, 1000),('Eric', 0 ,0, 1000),('Jakob', 0 ,0, 1000);

EOSQL
	