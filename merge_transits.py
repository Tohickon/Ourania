import json
import os

target = 'OuraniaWindows/src/main/resources/data/interpretations.json'
with open(target, 'r', encoding='utf-8') as f:
    data = json.load(f)

print("Loaded target keys:", data.keys())

with open('interpretations/full_transit_interpretations.json', 'r', encoding='utf-8') as f:
    full_transits = json.load(f)

with open('interpretations/transit_in_houses.json', 'r', encoding='utf-8') as f:
    transit_houses = json.load(f)

# The keys in full_transits need to be mapped to HTML strings: "<b>Title</b><br><i>Summary</i><br><br>FullText"
for k, v in full_transits.items():
    html = f"<b>{v['title']}</b><br><i>{v['summary']}</i><br><br>{v['fullText']}"
    if "transits" not in data:
        data["transits"] = {}
    data["transits"][k] = html

# Also do transit_house
if "transit_house" not in data:
    data["transit_house"] = {}

for k, v in transit_houses.items():
    html = f"<b>{v['title']}</b><br><i>{v['summary']}</i><br><br>{v['fullText']}"
    # The key in JSON is like "transit_house_1". We should map it to "transit_house" object with key "house_1" ?
    # Wait, InterpretationService.java uses "transitHouses" when it sees "transit_house".
    # And then getTransitInHouse uses `key = p + "_" + house;` e.g. "sun_1".
    # But wait! transit_in_houses.json doesn't have planets! It just has "transit_house_1", meaning ANY planet in house 1.
    # Ah. "transit_house_1" is just "transit_house_1".
    # Let's see what keys InterpretationService uses for transit_house.
    
    data["transit_house"][k] = html

with open(target, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4)
print("Merge complete.")
