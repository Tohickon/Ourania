const fs = require('fs');

const extra_bodies = ['chiron', 'north_node', 'south_node', 'ceres', 'pallas', 'juno', 'vesta', 'ascendant', 'descendant', 'mc', 'ic', 'fortune', 'lilith'];
const classical_planets = ['sun', 'moon', 'mercury', 'venus', 'mars', 'jupiter', 'saturn', 'uranus', 'neptune', 'pluto'];

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
    'lilith': 'raw, untamed nature and shadow desires',
    
    'sun': 'core identity and vital energy',
    'moon': 'emotional needs and instinctual reactions',
    'mercury': 'intellect and communication style',
    'venus': 'values, relationships, and aesthetic tastes',
    'mars': 'drive, ambition, and assertive energy',
    'jupiter': 'capacity for growth, optimism, and expansion',
    'saturn': 'sense of discipline, restriction, and maturity',
    'uranus': 'urge for rebellion, innovation, and freedom',
    'neptune': 'dreams, illusions, and spiritual empathy',
    'pluto': 'deepest power, transformation, and primal intensity'
};

const body_names = {
    'chiron': 'Chiron', 'north_node': 'North Node', 'south_node': 'South Node',
    'ceres': 'Ceres', 'pallas': 'Pallas', 'juno': 'Juno', 'vesta': 'Vesta',
    'ascendant': 'Ascendant', 'descendant': 'Descendant', 'mc': 'MC', 'ic': 'IC',
    'fortune': 'Part of Fortune', 'lilith': 'Black Moon Lilith',
    
    'sun': 'Sun', 'moon': 'Moon', 'mercury': 'Mercury', 'venus': 'Venus',
    'mars': 'Mars', 'jupiter': 'Jupiter', 'saturn': 'Saturn',
    'uranus': 'Uranus', 'neptune': 'Neptune', 'pluto': 'Pluto'
};

const aspect_meanings = {
    'sextile': ['harmonizes with', 'a supportive flow that encourages'],
    'quincunx': ['is awkwardly misaligned with', 'a need for constant adjustment and compromise between']
};

const output = {};

// 1. Extra bodies to Classical planets (Sextile and Quincunx)
// For classical planets, the json key is classical_aspect_extra
for (const extra of extra_bodies) {
    for (const classical of classical_planets) {
        for (const a of ['sextile', 'quincunx']) {
            const key = classical + '_' + a + '_' + extra;
            const [verb, desc] = aspect_meanings[a];
            const title = body_names[classical] + ' ' + a.charAt(0).toUpperCase() + a.slice(1) + ' ' + body_names[extra];
            output[key] = '<b>' + title + ':</b> Your ' + body_meanings[classical] + ' ' + verb + ' your ' + body_meanings[extra] + '. This creates ' + desc + ' these two areas of your life.';
        }
    }
}

// 2. Extra bodies to Extra bodies (Quincunx only, as Sextile was already generated)
for (let i = 0; i < extra_bodies.length; i++) {
    for (let j = i + 1; j < extra_bodies.length; j++) {
        const b1 = extra_bodies[i];
        const b2 = extra_bodies[j];
        const a = 'quincunx';
        const key = b1 + '_' + a + '_' + b2;
        const [verb, desc] = aspect_meanings[a];
        const title = body_names[b1] + ' ' + a.charAt(0).toUpperCase() + a.slice(1) + ' ' + body_names[b2];
        
        output[key] = '<b>' + title + ':</b> Your ' + body_meanings[b1] + ' ' + verb + ' your ' + body_meanings[b2] + '. This creates ' + desc + ' these two areas of your life.';
    }
}

const path = 'src/main/resources/data/extra_bodies.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

if (!data.aspects) data.aspects = {};

Object.assign(data.aspects, output);
fs.writeFileSync(path, JSON.stringify(data, null, 2));

console.log('Added ' + Object.keys(output).length + ' missing aspects (Sextiles and Quincunxes).');
