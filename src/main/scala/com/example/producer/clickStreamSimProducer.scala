package com.example

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
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

    // Convert DataFrame rows to JSON strings
    val jsonDF = df.toJSON

    // Create Kafka producer properties
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.ACKS_CONFIG, "all")  // Ensures all replicas acknowledge
    props.put(ProducerConfig.RETRIES_CONFIG, "3")  // Retry in case of failure

    // Create Kafka producer
    val kafkaProducer = new KafkaProducer[String, String](props)

    // Publish JSON data to Kafka topic
    jsonDF.collect().foreach { record =>
      val producerRecord = new ProducerRecord[String, String](kafkaTopic, record)
      kafkaProducer.send(producerRecord)
      println(s"Sent record: $record")
    }

    // Close the producer
    kafkaProducer.close()

    // Stop the Spark session
    spark.stop()
  }
}
