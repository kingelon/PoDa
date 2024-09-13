package com.example

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
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

    try {
      println("Starting HiveToKafkaProducer...")
      println(s"Using config file: $configFilePath")
      println(s"Using partition date: $partitionDate")

      // Load configuration file
      val config = new Properties()
      config.load(Source.fromFile(configFilePath).bufferedReader())

      val kafkaBootstrapServers = config.getProperty("kafka.bootstrap.servers")
      val kafkaTopic = config.getProperty("kafka.topic")
      val hiveTable = config.getProperty("hive.table")

      println(s"Kafka Bootstrap Servers: $kafkaBootstrapServers")
      println(s"Kafka Topic: $kafkaTopic")
      println(s"Hive Table: $hiveTable")

      // Create a Spark session
      val spark = SparkSession.builder
        .appName("HiveToKafkaProducer")
        .enableHiveSupport()
        .getOrCreate()

      import spark.implicits._

      // Read the data from Hive table for the specified partition date
      val df = spark.read.table(hiveTable)
        .where(s"cstone_last_updatetm = '$partitionDate'")

      // Show some rows from the DataFrame to verify data
      println("DataFrame content preview:")
      df.show(5)

      // Count the rows in the DataFrame to ensure data is being read
      val rowCount = df.count()
      println(s"Number of rows retrieved from Hive table: $rowCount")

      // --- Change: Use 'amex_guid' as the key instead of UUID ---
      // Prepare DataFrame for Kafka by using 'amex_guid' as the key
      // Ensure 'amex_guid' is not null
      val kafkaDF = df.filter($"amex_guid".isNotNull)
        .selectExpr("CAST(amex_guid AS STRING) AS key", "to_json(struct(*)) AS value")

      // Optional: Log the number of records with non-null 'amex_guid'
      val validRowCount = kafkaDF.count()
      println(s"Number of records with non-null 'amex_guid': $validRowCount")

      // Write the DataFrame to Kafka
      kafkaDF.write
        .format("kafka")
        .option("kafka.bootstrap.servers", kafkaBootstrapServers)
        .option("topic", kafkaTopic)
        .save()

      println("Data successfully written to Kafka topic")

      // Stop the Spark session
      spark.stop()
      println("HiveToKafkaProducer completed successfully.")

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"Exception occurred: ${e.getMessage}")
    }
  }
}
