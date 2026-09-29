import os

replace_map = {
    "Ourania": "Ourania",
    "Ourania": "Ourania",
    "ourania": "ourania"
}

# Add more extensions for the decompiled artifacts
exts = {".java", ".py", ".js", ".json", ".md", ".txt", ".properties", ".bat", ".xml", ".html", ".css", ".sh", ".smali", ".yaml", ".yml", ".code-workspace"}

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        return # Skip binary or non-utf8 files
    
    original = content
    for old, new in replace_map.items():
        content = content.replace(old, new)
        
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated: {filepath}")

for root, dirs, files in os.walk(r"C:\Users\daver\Desktop\Ourania"):
    if ".git" in root or ".idea" in root or "jadx" in root: # Skipping jadx source as it's a huge 3rd party tool
        continue
    for file in files:
        if any(file.endswith(ext) for ext in exts):
            process_file(os.path.join(root, file))
