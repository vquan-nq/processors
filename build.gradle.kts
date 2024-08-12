group = "io.processors"
version = "0.0.0"

repositories {
    mavenCentral()
    maven {
        name = "confluent"
        url = uri("https://packages.confluent.io/maven/")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val scalaVersion = "2.13"
val sparkVersion = "3.5.1"
val deltaVersion = "3.2.0"
val hadoopVersion = "3.3.4"
val schemaRegistryVersion = "7.7.0"

dependencies {
    implementation("org.apache.spark:spark-sql_$scalaVersion:$sparkVersion")
    implementation("org.apache.spark:spark-sql-kafka-0-10_$scalaVersion:$sparkVersion")
    implementation("org.apache.spark:spark-streaming_$scalaVersion:$sparkVersion")
    implementation("org.apache.spark:spark-streaming-kafka-0-10_$scalaVersion:$sparkVersion")
    implementation("org.apache.spark:spark-hive_$scalaVersion:$sparkVersion")
    implementation("org.apache.spark:spark-avro_$scalaVersion:$sparkVersion")
    implementation("io.delta:delta-spark_$scalaVersion:$deltaVersion")
    implementation("org.apache.hadoop:hadoop-aws:$hadoopVersion")
    implementation("io.confluent:kafka-schema-registry-client:$schemaRegistryVersion")
}
