import json
import uuid

def generate_uid(prefix):
    return f"_:{prefix}{uuid.uuid4().hex}"

def json_to_rdf(json_data):
    triples = []
    # Event Node
    event_uid = generate_uid('event')
    triples.append(f'{event_uid} <dgraph.type> "Event" .')
    triples.append(f'{event_uid} <id> "{json_data["id"]}" .')
    triples.append(f'{event_uid} <timestamp> "{json_data["timestamp"]}" .')
    triples.append(f'{event_uid} <type> "{json_data["type"]}" .')
    # ... other Event properties
    
    # EventData Node
    event_data = json_data.get('data', {}).get('eventData', {})
    if event_data:
        event_data_uid = generate_uid('eventData')
        triples.append(f'{event_uid} <hasEventData> {event_data_uid} .')
        triples.append(f'{event_data_uid} <dgraph.type> "EventData" .')
        triples.append(f'{event_data_uid} <datapointAppID> "{event_data.get("datapointAppID", "")}" .')
        triples.append(f'{event_data_uid} <clickName> "{event_data.get("clickName", "")}" .')
        
        # PageInfo Node
        page_info = event_data.get('pageInfo', {})
        if page_info:
            page_info_uid = generate_uid('pageInfo')
            triples.append(f'{event_data_uid} <hasPageInfo> {page_info_uid} .')
            triples.append(f'{page_info_uid} <dgraph.type> "PageInfo" .')
            triples.append(f'{page_info_uid} <country> "{page_info.get("country", "")}" .')
            triples.append(f'{page_info_uid} <city> "{page_info.get("city", "")}" .')
            triples.append(f'{page_info_uid} <locale> "{page_info.get("locale", "")}" .')
    
    # Device Node
    device_uid = generate_uid('device')
    triples.append(f'{event_uid} <hasDevice> {device_uid} .')
    triples.append(f'{device_uid} <dgraph.type> "Device" .')
    triples.append(f'{device_uid} <userAgent> "{json_data.get("data", {}).get("userAgent", "")}" .')
    triples.append(f'{device_uid} <deviceClass> "{json_data.get("data", {}).get("deviceClass", "")}" .')
    # ... other Device properties
    
    # GeoLocation Node
    geo_location = json_data.get('data', {}).get('geoLocation', {})
    if geo_location:
        geo_location_uid = generate_uid('geoLocation')
        triples.append(f'{event_uid} <hasGeoLocation> {geo_location_uid} .')
        triples.append(f'{geo_location_uid} <dgraph.type> "GeoLocation" .')
        triples.append(f'{geo_location_uid} <country> "{geo_location.get("country", "")}" .')
        triples.append(f'{geo_location_uid} <region> "{geo_location.get("region", "")}" .')
        triples.append(f'{geo_location_uid} <continent> "{geo_location.get("continent", "")}" .')
    
    # Return all triples for this event
    return triples

# Main processing
import glob

rdf_triples = []
for file_name in glob.glob('data/*.json'):
    with open(file_name, 'r') as f:
        json_data = json.load(f)
        rdf_triples.extend(json_to_rdf(json_data))

# Write to RDF file
with open('data.rdf', 'w') as f:
    for triple in rdf_triples:
        f.write(triple + '\n')
