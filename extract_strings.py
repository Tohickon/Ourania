import xml.etree.ElementTree as ET

tree = ET.parse(r'c:\Users\daver\Desktop\Ourania\decoded_apk\res\values\strings.xml')
root = tree.getroot()

java_code = '''package com.zodiacomputing.ourania.gui;

import java.util.HashMap;
import java.util.Map;

public class InterpretationData {
    public static final Map<String, String> DATA = new HashMap<>();
    
    static {
'''

for string_elem in root.findall('string'):
    name = string_elem.get('name')
    if name and (name.startswith('Sta') or name.startswith('Dyn')):
        text = string_elem.text
        if text:
            # Escape quotes and backslashes
            text = text.replace('\\', '\\\\').replace('\"', '\\\"').replace('\n', '\\n')
            java_code += f'        DATA.put("{name}", "{text}");\n'

java_code += '''    }
}
'''

with open(r'c:\Users\daver\Desktop\Ourania\OuraniaWindows\src\main\java\com\zodiacomputing\ourania\gui\InterpretationData.java', 'w', encoding='utf-8') as f:
    f.write(java_code)

print("Generated InterpretationData.java")
