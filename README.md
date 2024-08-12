Components:
+ MySQL: source table
+ Debezium (Kafka connector): get data from MySQL binlog
+ Kafka: store messages received from Debezium
+ Hive Metastore + PostgreSQL: catalog
+ Minio: S3-compatible object storage to storage delta tables
+ Conduktor: Kafka monitoring

Running:
+ Run docker-compose.yaml in ./bootstrap
+ Run create-connectors.sh to create debezium connector in ./bootstrap/config/debezium
+ Run main.kt file
+ Insert data into equipment.event table to see changes
+ You can monitor program's progress in Kafka UI, Minio UI,...
