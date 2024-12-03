import pandas as pd
import json

def generate_uid(prefix):
    return f"_:{prefix}{uuid.uuid4().hex}"

def json_to_rdf(json_data):
    triples = []
    event_uid = generate_uid('event')
    triples.append(f'{event_uid} <dgraph.type> "Event" .')
    triples.append(f'{event_uid} <id> "{json_data["id"]}" .')
    # Add other parsing logic as per your script
    return triples

# Step 1: Load Excel file
df = pd.read_excel('path_to_your_excel_file.xlsx')
json_rows = df['json_column'].tolist()

# Step 2: Clean JSON and Parse to RDF
rdf_triples = []

for row in json_rows:
    try:
        cleaned_row = row.replace('\\"', '"').replace('\\n', '')
        json_obj = json.loads(cleaned_row)
        triples = json_to_rdf(json_obj)
        rdf_triples.extend(triples)
    except json.JSONDecodeError as e:
        print(f"Error decoding row: {row}\nError: {e}")

# Step 3: Save RDF to file
with open('bulk_data.rdf', 'w') as f:
    for triple in rdf_triples:
        f.write(triple + '\n')

print("RDF triples saved to 'bulk_data.rdf'")
