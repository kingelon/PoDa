package com.example

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010._
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.hadoop.hbase.client.{ConnectionFactory, Put}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client.Connection
import org.apache.log4j.Logger
import scala.collection.JavaConverters._

object KafkaToHBaseConsumer {
  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      println("Usage: KafkaToHBaseConsumer <config-file-path>")
      System.exit(1)
    }

    val configFilePath = args(0)
    val logger = Logger.getLogger(getClass.getName)

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

      import spark.implicits._

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

          try {
            partitionOfRecords.foreach { record =>
              // --- Change: Use the key from Kafka (which is 'amex_guid') as the row key ---
              val rowKey = record.key()

              // Check if the key is not null
              if (rowKey != null) {
                // Parse the JSON value to extract fields
                val jsonString = record.value()
                val jsonDF = spark.read.json(Seq(jsonString).toDS())

                // Collect the data as a Map[String, String]
                val dataMap = jsonDF.first().getValuesMap[Any](jsonDF.columns)

                val put = new Put(Bytes.toBytes(rowKey))

                // --- Change: Store individual fields in HBase columns ---
                // Iterate over the map and add columns to the Put object
                dataMap.foreach { case (column, value) =>
                  if (value != null) {
                    put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes(column), Bytes.toBytes(value.toString))
                  }
                }

                // Write to HBase
                table.put(put)
              } else {
                // Handle case where rowKey is null
                logger.warn("Received record with null key")
              }
            }
          } catch {
            case e: Exception =>
              logger.error("Error processing partition", e)
          } finally {
            // Close the HBase connection
            table.close()
            connection.close()
          }
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
