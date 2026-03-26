import json
from dotenv import load_dotenv
import serpapi
import os

def fetch_events(client, query, location):
    results = client.search({
        "engine": "google",
        "q": query,
        "location": location,
        "hl": "en",
        "gl": "us"
    })

    return results.get("events_results", [])


def main():
    load_dotenv()

    API_KEY = os.getenv("SERPAPI_KEY")
    client = serpapi.Client(api_key=API_KEY)

    locations = [
        "New Orleans, Louisiana, United States",
        "Baton Rouge, Louisiana, United States",
        "Shreveport, Louisiana, United States",
        "Lafayette, Louisiana, United States",
        "Lake Charles, Louisiana, United States",
        "Monroe, Louisiana, United States",
        "Alexandria, Louisiana, United States",
        "Houma, Louisiana, United States",
        "Thibodaux, Louisiana, United States",
        "Slidell, Louisiana, United States",
        "Houston, Texas, United States",
        "Beaumont, Texas, United States",
        "Port Arthur, Texas, United States",
        "Dallas, Texas, United States",
        "Austin, Texas, United States",
        "Jackson, Mississippi, United States",
        "Gulfport, Mississippi, United States",
        "Biloxi, Mississippi, United States",
        "Mobile, Alabama, United States"
    ]

    query = "concerts"

    all_events = []

    for loc in locations:
        print(f"Fetching events for {loc}...")
        events = fetch_events(client, query, loc)

        for e in events:
            e["searched_location"] = loc

        all_events.extend(events)

    # Save everything
    with open("events.json", "w") as f:
        json.dump(all_events, f, indent=4)

    print(f"\nSaved {len(all_events)} total events from {len(locations)} locations.")

if __name__ == "__main__":
    main()
