const fs = require('fs');

const missing_bodies = ['descendant', 'ic'];
const classical_planets = ['sun', 'moon', 'mercury', 'venus', 'mars', 'jupiter', 'saturn', 'uranus', 'neptune', 'pluto'];
const aspects = ['conjunction', 'sextile', 'square', 'trine', 'quincunx', 'opposition'];

const body_meanings = {
    'descendant': 'expectations of others and partnership dynamics',
    'ic': 'private life, roots, and emotional foundations',
    
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
    'descendant': 'Descendant', 'ic': 'IC',
    
    'sun': 'Sun', 'moon': 'Moon', 'mercury': 'Mercury', 'venus': 'Venus',
    'mars': 'Mars', 'jupiter': 'Jupiter', 'saturn': 'Saturn',
    'uranus': 'Uranus', 'neptune': 'Neptune', 'pluto': 'Pluto'
};

const aspect_meanings = {
    'conjunction': ['fuses with', 'a powerful merging of energies where'],
    'sextile': ['harmonizes with', 'a supportive flow that encourages'],
    'square': ['creates friction with', 'a dynamic tension that challenges and forces growth between'],
    'trine': ['flows effortlessly with', 'a natural and easy synergy between'],
    'quincunx': ['is awkwardly misaligned with', 'a need for constant adjustment and compromise between'],
    'opposition': ['polarizes with', 'a push-pull dynamic that requires balance between']
};

const output = {};

for (const extra of missing_bodies) {
    for (const classical of classical_planets) {
        for (const a of aspects) {
            const key = classical + '_' + a + '_' + extra;
            const [verb, desc] = aspect_meanings[a];
            const title = body_names[classical] + ' ' + a.charAt(0).toUpperCase() + a.slice(1) + ' ' + body_names[extra];
            output[key] = '<b>' + title + ':</b> Your ' + body_meanings[classical] + ' ' + verb + ' your ' + body_meanings[extra] + '. This creates ' + desc + ' these two areas of your life.';
        }
    }
}

const path = 'src/main/resources/data/extra_bodies.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

if (!data.aspects) data.aspects = {};

Object.assign(data.aspects, output);
fs.writeFileSync(path, JSON.stringify(data, null, 2));

console.log('Added ' + Object.keys(output).length + ' missing aspects for Descendant and IC.');
