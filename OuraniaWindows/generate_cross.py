import json

extra_bodies = ['chiron', 'north_node', 'south_node', 'ceres', 'pallas', 'juno', 'vesta', 'ascendant', 'descendant', 'mc', 'ic', 'fortune', 'lilith']
aspects = ['conjunction', 'sextile', 'square', 'trine', 'opposition']

# Predefined meanings for bodies to inject into templates
body_meanings = {
    'chiron': 'deepest wound and potential for healing',
    'north_node': 'soul''s evolutionary path and future growth',
    'south_node': 'past karmic patterns and innate comfort zones',
    'ceres': 'capacity for nurturing and self-care',
    'pallas': 'strategic intelligence and creative problem-solving',
    'juno': 'approach to committed partnerships and loyalty',
    'vesta': 'sense of devotion, focus, and sacred service',
    'ascendant': 'outward personality and physical vitality',
    'descendant': 'expectations of others and partnership dynamics',
    'mc': 'public reputation, career, and life direction',
    'ic': 'private life, roots, and emotional foundations',
    'fortune': 'greatest source of joy, flow, and worldly success',
    'lilith': 'raw, untamed nature and shadow desires'
}

body_names = {
    'chiron': 'Chiron', 'north_node': 'North Node', 'south_node': 'South Node',
    'ceres': 'Ceres', 'pallas': 'Pallas', 'juno': 'Juno', 'vesta': 'Vesta',
    'ascendant': 'Ascendant', 'descendant': 'Descendant', 'mc': 'MC', 'ic': 'IC',
    'fortune': 'Part of Fortune', 'lilith': 'Black Moon Lilith'
}

aspect_meanings = {
    'conjunction': ('fuses with', 'a powerful merging of energies where'),
    'sextile': ('harmonizes with', 'a supportive flow that encourages'),
    'square': ('creates friction with', 'a dynamic tension that challenges and forces growth between'),
    'trine': ('flows effortlessly with', 'a natural and easy synergy between'),
    'opposition': ('polarizes with', 'a push-pull dynamic that requires balance between')
}

output = {}

for i in range(len(extra_bodies)):
    for j in range(i + 1, len(extra_bodies)):
        b1 = extra_bodies[i]
        b2 = extra_bodies[j]
        for a in aspects:
            key = f"{b1}_{a}_{b2}"
            verb, desc = aspect_meanings[a]
            
            title = f"{body_names[b1]} {a.capitalize()} {body_names[b2]}"
            
            # Special case for angles to angles to avoid weird text (though normally they don't aspect except exactly)
            text = f"<b>{title}:</b> The {body_meanings[b1]} {verb} your {body_meanings[b2]}. This creates {desc} these two areas of your life."
            
            output[key] = text

with open('extra_cross_aspects.json', 'w') as f:
    json.dump(output, f, indent=2)

print("Generated " + str(len(output)) + " cross aspects.")
