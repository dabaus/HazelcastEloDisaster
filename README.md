# Hazlecast ELO ratig

Example on how to use hazlecast 
to compute elo ratings in a streaming jet pipeline.
Note that this code currently does not handle database 
transactions properly.

## How to demo

1. clone the repo
2. docker compose up -d
3. ./gradlew bootRun
4. psql -U postgres -h localhost -d hazeldb
5. select * from ratings;
