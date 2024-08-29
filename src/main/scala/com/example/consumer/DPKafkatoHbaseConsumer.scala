package com.example

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010._
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.hadoop.hbase.client.{ConnectionFactory, Put}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Connection

object KafkaToHBaseConsumer {
  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      println("Usage: KafkaToHBaseConsumer <config-file-path>")
      System.exit(1)
    }

    val configFilePath = args(0)

    try {
      println("Starting KafkaToHBaseConsumer...")
      println(s"Using config file: $configFilePath")

      // Load configuration file
      val config = new java.util.Properties()
      config.load(scala.io.Source.fromFile(configFilePath).bufferedReader())

      val kafkaBootstrapServers = config.getProperty("kafka.bootstrap.servers")
      val kafkaTopic = config.getProperty("kafka.topic")
      val hbaseTableName = config.getProperty("hbase.table.name")

      println(s"Kafka Bootstrap Servers: $kafkaBootstrapServers")
      println(s"Kafka Topic: $kafkaTopic")
      println(s"HBase Table: $hbaseTableName")

      // Create Spark session and streaming context
      val spark = SparkSession.builder
        .appName("KafkaToHBaseConsumer")
        .getOrCreate()

      val ssc = new StreamingContext(spark.sparkContext, Seconds(10))

      // Kafka parameters
      val kafkaParams = Map[String, Object](
        "bootstrap.servers" -> kafkaBootstrapServers,
        "key.deserializer" -> classOf[StringDeserializer],
        "value.deserializer" -> classOf[StringDeserializer],
        "group.id" -> "kafka-consumer-group",
        "auto.offset.reset" -> "latest",
        "enable.auto.commit" -> (false: java.lang.Boolean)
      )

      val topics = Array(kafkaTopic)

      // Create Kafka stream
      val stream = KafkaUtils.createDirectStream[String, String](
        ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](topics, kafkaParams)
      )

      // Process each RDD
      stream.foreachRDD { rdd =>
        rdd.foreachPartition { partitionOfRecords =>
          // Set up HBase connection
          val hbaseConfig = HBaseConfiguration.create()
          val connection: Connection = ConnectionFactory.createConnection(hbaseConfig)
          val table = connection.getTable(TableName.valueOf(hbaseTableName))

          partitionOfRecords.foreach { record =>
            // Create a Put object with the row key
            val put = new Put(Bytes.toBytes(record.key()))

            // Add the JSON data to the 'data' column in the 'cf' column family
            put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("data"), Bytes.toBytes(record.value()))

            // Write to HBase
            table.put(put)
          }

          // Close the HBase connection
          table.close()
          connection.close()
        }
      }

      // Start the streaming context
      ssc.start()
      ssc.awaitTermination()

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"Exception occurred: ${e.getMessage}")
    }
  }
}
