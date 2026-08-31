const fs = require('fs');

const extra_bodies = ['chiron', 'north_node', 'south_node', 'ceres', 'pallas', 'juno', 'vesta', 'ascendant', 'descendant', 'mc', 'ic', 'fortune', 'lilith'];
const aspects = ['conjunction', 'sextile', 'square', 'trine', 'opposition'];

const body_meanings = {
    'chiron': 'deepest wound and potential for healing',
    'north_node': 'soul\'s evolutionary path and future growth',
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
};

const body_names = {
    'chiron': 'Chiron', 'north_node': 'North Node', 'south_node': 'South Node',
    'ceres': 'Ceres', 'pallas': 'Pallas', 'juno': 'Juno', 'vesta': 'Vesta',
    'ascendant': 'Ascendant', 'descendant': 'Descendant', 'mc': 'MC', 'ic': 'IC',
    'fortune': 'Part of Fortune', 'lilith': 'Black Moon Lilith'
};

const aspect_meanings = {
    'conjunction': ['fuses with', 'a powerful merging of energies where'],
    'sextile': ['harmonizes with', 'a supportive flow that encourages'],
    'square': ['creates friction with', 'a dynamic tension that challenges and forces growth between'],
    'trine': ['flows effortlessly with', 'a natural and easy synergy between'],
    'opposition': ['polarizes with', 'a push-pull dynamic that requires balance between']
};

const output = {};

for (let i = 0; i < extra_bodies.length; i++) {
    for (let j = i + 1; j < extra_bodies.length; j++) {
        const b1 = extra_bodies[i];
        const b2 = extra_bodies[j];
        for (const a of aspects) {
            const key = b1 + '_' + a + '_' + b2;
            const [verb, desc] = aspect_meanings[a];
            const title = body_names[b1] + ' ' + a.charAt(0).toUpperCase() + a.slice(1) + ' ' + body_names[b2];
            
            output[key] = '<b>' + title + ':</b> Your ' + body_meanings[b1] + ' ' + verb + ' your ' + body_meanings[b2] + '. This creates ' + desc + ' these two areas of your life.';
        }
    }
}

const path = 'src/main/resources/data/extra_bodies.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

if (!data.aspects) data.aspects = {};

Object.assign(data.aspects, output);
fs.writeFileSync(path, JSON.stringify(data, null, 2));

console.log('Added ' + Object.keys(output).length + ' cross aspects.');
