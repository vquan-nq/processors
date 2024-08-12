package io.processors

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.delta.tables.DeltaTable
import org.apache.avro.Schema
import org.apache.spark.api.java.function.VoidFunction2
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.avro.functions.from_avro
import org.apache.spark.sql.functions.*
import org.apache.spark.sql.streaming.Trigger

// spark definition
val conf = mapOf<String, String>(
    "spark.hadoop.fs.s3a.impl" to "org.apache.hadoop.fs.s3a.S3AFileSystem",
    "spark.hadoop.fs.s3a.endpoint" to "http://10.1.1.2:9000",
    "spark.hadoop.fs.s3a.access.key" to "username",
    "spark.hadoop.fs.s3a.secret.key" to "password",
    "spark.hadoop.fs.s3a.path.style.access" to "true",
    "spark.sql.catalogImplementation" to "hive",
    "spark.hive.metastore.uris" to "thrift://10.1.1.5:9083",
    "spark.sql.warehouse.dir" to "s3a://data/delta/.warehouse",
    "spark.sql.extensions" to "io.delta.sql.DeltaSparkSessionExtension",
    "spark.sql.catalog.spark_catalog" to "org.apache.spark.sql.delta.catalog.DeltaCatalog",
    "spark.master" to "local[*]",
    "spark.sql.debug.maxToStringFields" to "1024"
)

val spark = SparkSession
    .builder()
    .config(conf)
    .orCreate

fun init() {
    spark.sql("CREATE DATABASE IF NOT EXISTS equipment")
    spark.sql("USE equipment")
    spark.sql("CREATE TABLE IF NOT EXISTS event(equipment_id LONG, count LONG) USING DELTA LOCATION 's3a://data/delta/equipment.db/event/'")
}

val runOnce = init()

// kafka definition
const val kafkaURL = "10.1.1.7:9092"
const val kafkaSubscribedTopic = "debezium.monitoring.equipment.event"

// schema registry definition
const val schemaRegistryURL = "http://10.1.1.8:8081"
const val schemaRegistrySubject = "$kafkaSubscribedTopic-value"

fun getSchemaFromSchemaRegistry(url: String = schemaRegistryURL, subject: String = schemaRegistryURL): String {
    val client = CachedSchemaRegistryClient(schemaRegistryURL, 1024)
    try {
        val schemaMetadata = client.getLatestSchemaMetadata(schemaRegistrySubject)
        val schema = Schema.Parser().parse(schemaMetadata.schema)
        return schema.toString(true)
    } catch (exception: Exception) {
        exception.printStackTrace()
    }
    return ""
}

// delta definition
val dt = DeltaTable.forName(spark, "equipment.event")
val checkpointLocation = "s3a://data/delta/.checkpoint/monitoring/equipment/event/_checkpoint/"

fun mergeBatchInto(batch: Dataset<Row>, batchId: Long) {
    dt.alias("target")
        .merge(
            batch.alias("source"),
            "target.equipment_id = source.equipment_id"
        )
        .whenNotMatched().insertAll()
        .whenMatched().updateExpr(
            mapOf(
                "equipment_id" to "target.equipment_id",
                "count" to "target.count + source.count"
            )
        )
        .execute()
    dt.toDF().show()
}

fun main(args: Array<String>) {
    val messages = spark
        .readStream()
        .format("kafka")
        .option("kafka.bootstrap.servers", kafkaURL)
        .option("subscribe", kafkaSubscribedTopic)
        .option("startingOffsets", "earliest")
        .load()

    val events = messages
        .withColumn("fixedValue", expr("substring(value, 6, length(value) - 5)"))
        .select(
            from_avro(
                col("fixedValue"),
                getSchemaFromSchemaRegistry()
            ).alias("fields")
        )
        .select(
            col("fields.after.event_id").alias("event_id"),
            col("fields.after.equipment_id").alias("equipment_id")
        )

    val query = events
        .groupBy("equipment_id")
        .agg(
            count("event_id").alias("count")
        )

    query
        .writeStream()
        .format("delta")
        .outputMode("update")
        .option("checkpointLocation", checkpointLocation)
        .trigger(Trigger.ProcessingTime("10 seconds"))
        .foreachBatch(VoidFunction2 { batch, batchId -> mergeBatchInto(batch, batchId) })
        .start()
        .awaitTermination()
}
