package com.example

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import java.util.Properties
import scala.io.Source

object HiveToKafkaProducer {
  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: HiveToKafkaProducer <config-file-path> <partition-date>")
      System.exit(1)
    }

    val configFilePath = args(0)
    val partitionDate = args(1)

    // Load configuration file
    val config = new Properties()
    config.load(Source.fromFile(configFilePath).bufferedReader())

    val kafkaBootstrapServers = config.getProperty("kafka.bootstrap.servers")
    val kafkaTopic = config.getProperty("kafka.topic")
    val hiveTable = config.getProperty("hive.table")

    // Create a Spark session
    val spark = SparkSession.builder
      .appName("HiveToKafkaProducer")
      .enableHiveSupport()
      .getOrCreate()

    import spark.implicits._

    // Read the data from Hive table for the specified partition date
    val df = spark.read.table(hiveTable)
      .where(s"cstone_last_updatetm = '$partitionDate'")

    // Prepare DataFrame for Kafka by casting columns to String
    val kafkaDF = df.selectExpr("CAST(UUID() AS STRING) AS key", "to_json(struct(*)) AS value")

    // Write the DataFrame to Kafka
    kafkaDF.write
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("topic", kafkaTopic)
      .save()

    spark.stop()
  }
}
