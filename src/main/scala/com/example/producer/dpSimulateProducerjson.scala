package com.example

import org.apache.spark.sql.SparkSession
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import java.util.Properties
import play.api.libs.json.{Json, JsValue}
import scala.util.Random

object HiveToKafkaProducer {
  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      println("Usage: HiveToKafkaProducer <config-file-path>")
      System.exit(1)
    }

    val configFilePath = args(0)

    try {
      println("Starting HiveToKafkaProducer...")

      // Load configuration file
      val config = new Properties()
      config.load(scala.io.Source.fromFile(configFilePath).bufferedReader())

      val kafkaBootstrapServers = config.getProperty("kafka.bootstrap.servers")
      val kafkaTopic = config.getProperty("kafka.topic")

      println(s"Kafka Bootstrap Servers: $kafkaBootstrapServers")
      println(s"Kafka Topic: $kafkaTopic")

      // Create Kafka producer properties
      val props = new Properties()
      props.put("bootstrap.servers", kafkaBootstrapServers)
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

      // Create a Kafka producer
      val producer = new KafkaProducer[String, String](props)

      // Generate random data based on the provided JSON structure
      def generateDummyData(): String = {
        val randomId = java.util.UUID.randomUUID().toString
        val randomCorrelationId = java.util.UUID.randomUUID().toString
        val timestamp = System.currentTimeMillis()

        val json: JsValue = Json.obj(
          "id" -> randomId,
          "correlationId" -> randomCorrelationId,
          "sourceRef" -> "600002805",
          "specversion" -> "1.0.2",
          "time" -> s"${timestamp}Z",
          "type" -> "PAGE VIEW",
          "sourceName" -> "Datapoint",
          "version" -> "1.0.2",
          "dataContentType" -> "application/json",
          "data" -> Json.obj(
            "eventProcessingOptions" -> Json.obj(
              "consentedData" -> "NA"
            ),
            "eventData" -> Json.obj(
              "event" -> Json.obj(
                "datapointAppId" -> "defaultAmexAppID",
                "additionalField1" -> s"value${Random.nextInt(100)}",
                "additionalField2" -> s"value${Random.nextInt(100)}"
              ),
              "eventSource" -> Json.obj(
                "digitalDataSource" -> "EDL",
                "oneAmex" -> "NO",
                "trackit" -> "YES",
                "networkType" -> "Internet"
              ),
              "page" -> Json.obj(
                "attributes" -> Json.obj(
                  "PreviousPageName" -> "US|acq|Business|EmployeeCards|BusinessPlatinum|OfferIncentive"
                )
              ),
              "pageInfo" -> Json.obj(
                "country" -> "US",
                "language" -> "en",
                "pageName" -> "US|acq|Business|EmployeeCards|BusinessPlatinum|OfferIncentive",
                "url" -> "https://www.americanexpress.com/en-us/business/campaigns/business-platinum/offer-incentive/?sourcecode=A0000HEUJ",
                "businessUnit" -> "acq",
                "locale" -> "en-us"
              ),
              "device" -> Json.obj(
                "userAgent" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
                "deviceClass" -> "Phone",
                "deviceName" -> "iPhone",
                "deviceBrand" -> "Apple",
                "deviceVersion" -> "iPhone",
                "operatingSystemClass" -> "Mobile",
                "operatingSystemName" -> "iOS",
                "operatingSystemVersion" -> "17.5.1",
                "layoutEngineClass" -> "Browser",
                "layoutEngineName" -> "AppleWebKit",
                "layoutEngineVersion" -> "605.1.15",
                "screenResolution" -> "390x844",
                "colorDepth" -> 24
              ),
              "geoLocation" -> Json.obj(
                "country" -> "United States",
                "city" -> "Ann Arbor",
                "continent" -> "North America",
                "timezone" -> "America/Detroit",
                "ipAddress" -> "68.40.205.83",
                "postalCode" -> "48103"
              ),
              "user" -> Json.obj(
                "userId" -> java.util.UUID.randomUUID().toString,
                "addsId" -> "N/A",
                "userAgentId" -> java.util.UUID.randomUUID().toString,
                "authenticated" -> "NO",
                "sessionId" -> java.util.UUID.randomUUID().toString
              )
            )
          )
        )

        json.toString()
      }

      // Continuously send data to Kafka
      while (true) {
        val dummyData = generateDummyData()
        val record = new ProducerRecord[String, String](kafkaTopic, null, dummyData)
        producer.send(record)

        println(s"Sent data: $dummyData")
        Thread.sleep(1000) // Adjust the sleep time as needed
      }

      // Close the producer when done (not reached in this example)
      producer.close()

    } catch {
      case e: Exception =>
        e.printStackTrace()
        println(s"Exception occurred: ${e.getMessage}")
    }
  }
}
