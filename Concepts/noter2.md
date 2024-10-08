Real-Time Data Processing Framework

Overview:
This framework is designed to handle real-time clickstream events from the Amex cluster, process and map GUIDs (Global Unique Identifiers) to customer IDs, and store this data within the Axioms cluster. The key components include Spark Streaming, HBase for data storage, MySQL for user queries, and an API Gateway for encrypted user interactions.

Architecture:
The framework spans two main clusters: Amex and Axioms for data processing and storage.

Data Flow:

1. Clickstream Events from Amex Cluster:
   - Clickstream data is generated in the Amex cluster.
   - The data is streamed in real-time through Spark Streaming to Axioms, where it is processed.

2. GUID to Customer ID Mapping:
   - Each clickstream event contains a GUID, which identifies devices interacting with our services (such as browsers or laptops).
   - The Spark Streaming job compares the GUIDs against existing data in HBase to check if they can be mapped to a customer ID.
   - This mapping (GUID to customer ID) is stored and updated in an HBase table, which acts like a hash map.

3. Frequent Batch Updates:
   - A batch job is regularly executed to update the HBase table with new GUID to customer ID mappings from the Amex cluster. This ensures that the mappings are always up to date.

4. Data Storage in Axioms Cluster:
   - Once processed, the clickstream data, along with its GUID to customer ID mapping, is written to another HBase table for storage and future reference.

5. User Interaction and API Gateway:
   - An API Gateway provides user access to the data.
   - The gateway handles encrypted requests using a WAF (Web Application Firewall), decrypting the keys stored in a vault to provide access to the customer data.
   - MySQL is used to store metadata and other user-related queries.

Security Considerations:
- All interactions with the system are secured using encryption.
- The WAF and API Gateway ensure that sensitive data is protected, and access is properly managed.

Key Components:

- Spark Streaming:
  - Responsible for streaming clickstream data from the Amex cluster and mapping GUIDs to customer IDs.

- HBase:
  - Stores the mapping of GUIDs to customer IDs and is updated regularly by a batch job.

- Batch Job:
  - Periodically extracts new mappings from the Amex cluster and updates the HBase table in Axioms.

- API Gateway:
  - Provides encrypted access to the data, interacting with the WAF and a vault for decryption.

- MySQL Database:
  - Stores user-related metadata and queries for interaction tracking.