package com.example

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import java.util.Properties
import net.liftweb.json._
import scala.util.Random

object HiveToKafkaProducer {
  implicit val formats: DefaultFormats.type = DefaultFormats

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: HiveToKafkaProducer <config-file-path> <partition-date>")
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

        val json = JObject(
          JField("id", JString(randomId)),
          JField("correlationId", JString(randomCorrelationId)),
          JField("sourceRef", JString("600002805")),
          JField("specversion", JString("1.0.2")),
          JField("time", JString(s"${timestamp}Z")),
          JField("type", JString("PAGE VIEW")),
          JField("sourceName", JString("Datapoint")),
          JField("version", JString("1.0.2")),
          JField("dataContentType", JString("application/json")),
          JField("data", JObject(
            JField("eventProcessingOptions", JObject(
              JField("consentedData", JString("NA"))
            )),
            JField("eventData", JObject(
              JField("event", JObject(
                JField("datapointAppId", JString("defaultAmexAppID")),
                JField("additionalField1", JString(s"value${Random.nextInt(100)}")),
                JField("additionalField2", JString(s"value${Random.nextInt(100)}"))
              )),
              JField("eventSource", JObject(
                JField("digitalDataSource", JString("EDL")),
                JField("oneAmex", JString("NO")),
                JField("trackit", JString("YES")),
                JField("networkType", JString("Internet"))
              )),
              JField("page", JObject(
                JField("attributes", JObject(
                  JField("PreviousPageName", JString("US|acq|Business|EmployeeCards|BusinessPlatinum|OfferIncentive"))
                ))
              )),
              JField("pageInfo", JObject(
                JField("country", JString("US")),
                JField("language", JString("en")),
                JField("pageName", JString("US|acq|Business|EmployeeCards|BusinessPlatinum|OfferIncentive")),
                JField("url", JString("https://www.americanexpress.com/en-us/business/campaigns/business-platinum/offer-incentive/?sourcecode=A0000HEUJ")),
                JField("businessUnit", JString("acq")),
                JField("locale", JString("en-us"))
              )),
              JField("device", JObject(
                JField("userAgent", JString("Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1")),
                JField("deviceClass", JString("Phone")),
                JField("deviceName", JString("iPhone")),
                JField("deviceBrand", JString("Apple")),
                JField("deviceVersion", JString("iPhone")),
                JField("operatingSystemClass", JString("Mobile")),
                JField("operatingSystemName", JString("iOS")),
                JField("operatingSystemVersion", JString("17.5.1")),
                JField("layoutEngineClass", JString("Browser")),
                JField("layoutEngineName", JString("AppleWebKit")),
                JField("layoutEngineVersion", JString("605.1.15")),
                JField("screenResolution", JString("390x844")),
                JField("colorDepth", JInt(24))
              )),
              JField("geoLocation", JObject(
                JField("country", JString("United States")),
                JField("city", JString("Ann Arbor")),
                JField("continent", JString("North America")),
                JField("timezone", JString("America/Detroit")),
                JField("ipAddress", JString("68.40.205.83")),
                JField("postalCode", JString("48103"))
              )),
              JField("user", JObject(
                JField("userId", JString(java.util.UUID.randomUUID().toString)),
                JField("addsId", JString("N/A")),
                JField("userAgentId", JString(java.util.UUID.randomUUID().toString)),
                JField("authenticated", JString("NO")),
                JField("sessionId", JString(java.util.UUID.randomUUID().toString))
              ))
            ))
          ))
        )

        compactRender(json)
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
