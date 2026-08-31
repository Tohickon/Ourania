const fs = require('fs');
const path = 'C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\src\\main\\resources\\data\\extra_bodies.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

if (!data.aspects) {
    data.aspects = {};
}

const newAspects = {
    // Ceres Aspects
    "sun_conjunction_ceres": "<b>Sun Conjunct Ceres:</b> Your core identity is deeply intertwined with caregiving and nurturing. You shine brightest when you are providing for others, but must ensure you don't lose yourself entirely in the role of the caretaker.",
    "sun_square_ceres": "<b>Sun Square Ceres:</b> There is tension between your ego's desires and your need to nurture or be nurtured. You may struggle with feelings of scarcity or conditionally given love, pushing you to redefine self-worth independently of what you can provide.",
    "sun_trine_ceres": "<b>Sun Trine Ceres:</b> Nurturing comes effortlessly to you, and your vital energy naturally supports the growth of those around you. You possess an innate, warm magnetism that makes others feel safe and deeply seen in your presence.",
    "sun_opposition_ceres": "<b>Sun Opposite Ceres:</b> You often project your need for care onto others, either fiercely mothering partners or demanding to be mothered. Balance requires recognizing that true nourishment must first come from within yourself.",

    "moon_conjunction_ceres": "<b>Moon Conjunct Ceres:</b> Your emotional landscape is fused with the archetype of the Great Mother. You process feelings through tangible acts of care and require physical comfort, food, or touch to feel truly secure in the world.",
    "moon_square_ceres": "<b>Moon Square Ceres:</b> Your deepest emotional needs frequently clash with how you were nurtured or how you nurture others. You might unconsciously use caretaking as a form of control, requiring you to disentangle love from dependency.",
    "moon_trine_ceres": "<b>Moon Trine Ceres:</b> You possess an extraordinary, instinctual ability to soothe and feed the emotional hunger in yourself and others. Your home is naturally a sanctuary, and your empathy is a restorative force.",
    "moon_opposition_ceres": "<b>Moon Opposite Ceres:</b> Your emotional needs often feel at odds with the demands of caretaking. You may feel torn between seeking comfort and providing it, challenged to find a rhythm that honors both your own vulnerability and the needs of your loved ones.",

    "mercury_conjunction_ceres": "<b>Mercury Conjunct Ceres:</b> You nurture through words and ideas. Communication is your love language, and you have a gift for soothing anxieties by naming the problem clearly and offering practical, sensible advice.",
    "mercury_square_ceres": "<b>Mercury Square Ceres:</b> There is friction between your logical mind and your nurturing instincts. You may struggle to verbalize your needs for comfort, or you might over-intellectualize grief instead of allowing yourself to fully feel it.",
    "mercury_trine_ceres": "<b>Mercury Trine Ceres:</b> Your intellect and your capacity for care flow together beautifully. You naturally know exactly what to say to make someone feel supported, and your mind easily grasps the emotional subtext in any situation.",
    "mercury_opposition_ceres": "<b>Mercury Opposite Ceres:</b> You may experience conflicts where logic and emotional care seem mutually exclusive. You often attract situations that require you to bridge the gap between hard facts and the tender need for reassurance.",

    "venus_conjunction_ceres": "<b>Venus Conjunct Ceres:</b> Love, beauty, and nurturing are essentially the same to you. You express affection through devoted care and tangible gifts, requiring a partnership that feels profoundly safe and mutually sustaining.",
    "venus_square_ceres": "<b>Venus Square Ceres:</b> You often experience tension between your romantic desires and your need for maternal or unconditional care. You must be careful not to confuse romantic partnership with a parent-child dynamic.",
    "venus_trine_ceres": "<b>Venus Trine Ceres:</b> You effortlessly attract relationships that are both aesthetically pleasing and deeply nourishing. You bring a luxurious, comforting quality to your partnerships, making love feel like coming home.",
    "venus_opposition_ceres": "<b>Venus Opposite Ceres:</b> Your desire for romance and aesthetic pleasure often directly opposes your practical caretaking duties. Finding equilibrium means honoring your sensual needs without neglecting the deeper, sustaining bonds in your life.",

    "mars_conjunction_ceres": "<b>Mars Conjunct Ceres:</b> Your drive and aggression are powerfully linked to your protective instincts. You are a fierce defender of your loved ones, channeling your immense energy directly into providing for and safeguarding your 'family.'",
    "mars_square_ceres": "<b>Mars Square Ceres:</b> Your assertion of independence often clashes with your need for comfort and care. You may aggressively push away the very nurturing you crave, challenged to integrate your fierce autonomy with your tender vulnerabilities.",
    "mars_trine_ceres": "<b>Mars Trine Ceres:</b> Your willpower smoothly supports your caretaking goals. You easily take decisive, protective action on behalf of others, and your ambition is often fueled by a desire to provide a secure foundation.",
    "mars_opposition_ceres": "<b>Mars Opposite Ceres:</b> You may feel that asserting yourself inherently threatens your sources of comfort or care. You often find yourself in conflicts over provision and must learn to fight for your needs without burning the house down.",

    // Pallas Aspects
    "sun_conjunction_pallas": "<b>Sun Conjunct Pallas:</b> Your core identity is that of the strategist and the creative visionary. You shine through your intellect, pattern recognition, and ability to see the whole board when others only see individual pieces.",
    "sun_square_pallas": "<b>Sun Square Pallas:</b> There is a dynamic tension between your ego and your objective intellect. You may struggle when your brilliant strategies go unrecognized, forcing you to decouple your sense of self-worth from being the smartest person in the room.",
    "sun_trine_pallas": "<b>Sun Trine Pallas:</b> Your vitality naturally fuels your creative intelligence. You effortlessly align your personal goals with brilliant strategy, allowing you to intuitively navigate complex systems and lead with wisdom rather than brute force.",
    "sun_opposition_pallas": "<b>Sun Opposite Pallas:</b> You often project your strategic mind onto others, attracting brilliant partners or formidable intellectual rivals. True balance is achieved when you integrate your own profound wisdom with your conscious actions.",

    "moon_conjunction_pallas": "<b>Moon Conjunct Pallas:</b> Your emotional world is intrinsically linked to problem-solving and pattern recognition. You process feelings through analysis and strategy, finding deep emotional security in understanding exactly how things work.",
    "moon_square_pallas": "<b>Moon Square Pallas:</b> Your emotional needs frequently clash with your cold, strategic logic. You may attempt to out-think your feelings or suppress vulnerability in favor of competence, challenging you to honor both the heart and the mind.",
    "moon_trine_pallas": "<b>Moon Trine Pallas:</b> You possess an extraordinary emotional intelligence that flows seamlessly with logical strategy. You easily intuit the emotional undercurrents of any system, making you a gifted and empathetic counselor or diplomat.",
    "moon_opposition_pallas": "<b>Moon Opposite Pallas:</b> Your instincts often feel directly opposed to your logical strategies. You may feel torn between reacting intuitively and planning meticulously, requiring you to bridge your profound empathy with your tactical brilliance.",

    "mercury_conjunction_pallas": "<b>Mercury Conjunct Pallas:</b> Your mind is a formidable engine of creative intelligence. You possess razor-sharp strategic thinking, easily connecting disparate concepts and communicating complex patterns with striking clarity.",
    "mercury_square_pallas": "<b>Mercury Square Pallas:</b> There is friction between how you communicate and your overarching strategies. You may struggle to explain your intuitive leaps to others, leading to frustration until you learn to translate your brilliant visions into digestible logic.",
    "mercury_trine_pallas": "<b>Mercury Trine Pallas:</b> Your communication style effortlessly supports your strategic goals. You have a natural gift for persuasive, intelligent discourse, easily weaving tactical wisdom into everyday conversations.",
    "mercury_opposition_pallas": "<b>Mercury Opposite Pallas:</b> You often encounter situations where everyday details clash with your grand strategies. You must learn to listen to alternative viewpoints without feeling that your entire intellectual framework is under attack.",

    "venus_conjunction_pallas": "<b>Venus Conjunct Pallas:</b> You seek relationships and aesthetics that are intellectually stimulating and strategically sound. For you, true beauty is found in symmetry, intelligence, and a partner who can match your mental agility.",
    "venus_square_pallas": "<b>Venus Square Pallas:</b> Your desire for romance often conflicts with your need for intellectual independence. You may struggle with the vulnerability of love, preferring to manage relationships strategically rather than surrender to them.",
    "venus_trine_pallas": "<b>Venus Trine Pallas:</b> You effortlessly combine charm with tactical brilliance. You navigate social situations and partnerships with extraordinary grace, naturally attracting relationships based on mutual respect and shared creative visions.",
    "venus_opposition_pallas": "<b>Venus Opposite Pallas:</b> You may feel torn between the irrationality of love and the logic of your strategic mind. You often attract highly intellectual partners and must learn that romance cannot always be solved like a puzzle.",

    "mars_conjunction_pallas": "<b>Mars Conjunct Pallas:</b> You are the archetypal Warrior Queen. Your drive and aggression are seamlessly integrated with brilliant strategy, meaning you rarely fight without a plan and excel at directing your energy toward highly calculated goals.",
    "mars_square_pallas": "<b>Mars Square Pallas:</b> Your impulsive desires often clash with your strategic intellect. You may act rashly when you should plan, or overthink when you should act, challenging you to align your physical drive with your mental foresight.",
    "mars_trine_pallas": "<b>Mars Trine Pallas:</b> Your willpower and strategic mind work in perfect harmony. You naturally execute complex plans with courage and precision, making you an incredibly effective and unshakeable leader.",
    "mars_opposition_pallas": "<b>Mars Opposite Pallas:</b> You often experience conflicts between your aggressive instincts and your intellectual strategies. You may encounter rivals who challenge your plans, teaching you to refine your actions and fight smarter, not harder.",

    // Juno Aspects
    "sun_conjunction_juno": "<b>Sun Conjunct Juno:</b> Your core identity is deeply invested in marriage, commitment, and loyalty. You shine brightest when in a bonded partnership, but must guard against losing your individuality within the relationship.",
    "sun_square_juno": "<b>Sun Square Juno:</b> There is intense friction between your ego's need for independence and your desire for deep commitment. You may struggle with issues of equality and respect in partnerships, forcing you to define exactly what you will not tolerate.",
    "sun_trine_juno": "<b>Sun Trine Juno:</b> Your sense of self flows naturally with your capacity for long-term commitment. You possess a natural grace in partnerships, easily maintaining your own identity while fiercely supporting the bond you share with your mate.",
    "sun_opposition_juno": "<b>Sun Opposite Juno:</b> You often project your need for commitment onto others, attracting partners who either demand loyalty or fear it. True balance is found when you realize that the most important vow you make is the one to yourself.",

    "moon_conjunction_juno": "<b>Moon Conjunct Juno:</b> Your emotional security is entirely wrapped up in your committed partnerships. You feel safest when you are deeply bonded to another, but this intense emotional investment can lead to devastating hurt if trust is ever betrayed.",
    "moon_square_juno": "<b>Moon Square Juno:</b> Your deepest emotional needs frequently clash with the terms of your commitments. You may feel that your relationships do not provide the nurturing you crave, challenging you to address inequality and unfulfilled emotional contracts.",
    "moon_trine_juno": "<b>Moon Trine Juno:</b> Your instincts and your commitments are in profound harmony. You naturally create deeply secure, loyal, and emotionally supportive partnerships that easily weather the storms of life.",
    "moon_opposition_juno": "<b>Moon Opposite Juno:</b> Your emotional needs often feel at odds with the demands of a marriage or partnership. You may struggle to balance your personal vulnerability with the formal obligations of commitment.",

    "mercury_conjunction_juno": "<b>Mercury Conjunct Juno:</b> Your mind is focused on the mechanics of partnership. You thrive on clear, articulate communication within your commitments and consider intellectual rapport to be the non-negotiable foundation of any serious bond.",
    "mercury_square_juno": "<b>Mercury Square Juno:</b> There is tension between how you communicate and the terms of your relationships. You may frequently argue over fairness, respect, or broken promises, challenging you to refine how you negotiate your boundaries.",
    "mercury_trine_juno": "<b>Mercury Trine Juno:</b> You possess a natural talent for relationship negotiation. You easily communicate your needs and expectations, ensuring that your commitments are built on mutual understanding and effortless mental connection.",
    "mercury_opposition_juno": "<b>Mercury Opposite Juno:</b> Your logical mind often opposes the deeper, unspoken contracts of your relationships. You must learn to listen to the emotional subtext of your partner, rather than just arguing the literal terms of the agreement.",

    "venus_conjunction_juno": "<b>Venus Conjunct Juno:</b> Romance and enduring commitment are synonymous for you. You are fiercely loyal and expect absolute fidelity, seeking a love that is beautiful, legally or spiritually binding, and profoundly equal.",
    "venus_square_juno": "<b>Venus Square Juno:</b> Your desire for pleasure often conflicts with the heavy realities of commitment. You may struggle with jealousy, infidelity (whether yours or theirs), or feeling that your relationship has lost its romance in the face of obligation.",
    "venus_trine_juno": "<b>Venus Trine Juno:</b> Your approach to love gracefully supports long-term loyalty. You effortlessly maintain the romance and affection within a committed bond, ensuring that your partnerships are both beautiful and enduring.",
    "venus_opposition_juno": "<b>Venus Opposite Juno:</b> You may experience a stark divide between what attracts you (Venus) and what you actually need to sustain a relationship (Juno). Reconciling these forces requires finding a partner who is both a lover and a true equal.",

    "mars_conjunction_juno": "<b>Mars Conjunct Juno:</b> Your drive and assertion are fiercely directed toward your partnerships. You fight for your commitments, but you may also fight intensely within them, demanding absolute loyalty and equal distribution of power.",
    "mars_square_juno": "<b>Mars Square Juno:</b> Your need for independence violently clashes with your commitments. You may experience power struggles, anger over inequality, or volatile relationships, challenging you to learn how to assert yourself without destroying the bond.",
    "mars_trine_juno": "<b>Mars Trine Juno:</b> Your willpower naturally protects and energizes your partnerships. You easily take action to ensure fairness and loyalty, working seamlessly as a team with your chosen mate.",
    "mars_opposition_juno": "<b>Mars Opposite Juno:</b> You often feel that your personal ambitions are directly opposed to the demands of your relationship. You may attract aggressive partners or project your own anger onto them, requiring you to find a constructive outlet for your competitive drive.",

    // Vesta Aspects
    "sun_conjunction_vesta": "<b>Sun Conjunct Vesta:</b> Your core identity is defined by your devotion and spiritual focus. You possess the intense dedication of a priestess, shining brightest when you are entirely absorbed in a sacred purpose or specialized work.",
    "sun_square_vesta": "<b>Sun Square Vesta:</b> There is a dynamic tension between your ego and your spiritual devotion. You may struggle to integrate your need for personal recognition with the sacrifices required to tend your inner flame, leading to periods of intense burnout.",
    "sun_trine_vesta": "<b>Sun Trine Vesta:</b> Your sense of self flows naturally with your capacity for extreme focus. You effortlessly dedicate your vital energy to a higher purpose, finding profound joy and restoration in the work that matters most to you.",
    "sun_opposition_vesta": "<b>Sun Opposite Vesta:</b> You often project your need for devotion onto others, or feel that your relationships distract you from your sacred focus. True balance requires learning to tend your own inner fire without totally isolating yourself from the world.",

    "moon_conjunction_vesta": "<b>Moon Conjunct Vesta:</b> Your emotional security is found through deep focus, ritual, and devotion. You process feelings by retreating into your sacred work or spiritual practices, finding immense comfort in tending your inner flame.",
    "moon_square_vesta": "<b>Moon Square Vesta:</b> Your deepest emotional needs frequently clash with your intense dedications. You may unconsciously starve your emotional life in favor of your work or spiritual focus, challenging you to realize that self-care is also a sacred duty.",
    "moon_trine_vesta": "<b>Moon Trine Vesta:</b> You possess a natural, instinctual connection to your spiritual purpose. Your emotional world effortlessly supports your intense focus, allowing you to easily integrate ritual and devotion into your daily life.",
    "moon_opposition_vesta": "<b>Moon Opposite Vesta:</b> Your need for emotional connection often feels at odds with your need for isolated, sacred focus. You must learn to bridge the gap between your profound inner devotion and your vulnerable human feelings.",

    "mercury_conjunction_vesta": "<b>Mercury Conjunct Vesta:</b> Your mind is capable of laser-like focus and extreme dedication. You think and communicate with the purity of a priestess, easily dedicating your intellect to highly specialized research, writing, or spiritual study.",
    "mercury_square_vesta": "<b>Mercury Square Vesta:</b> There is friction between your everyday thoughts and your need for pure focus. You may struggle with mental distractions or anxiety that pulls you away from your sacred work, challenging you to discipline your restless mind.",
    "mercury_trine_vesta": "<b>Mercury Trine Vesta:</b> Your intellect flows beautifully with your spiritual devotion. You naturally articulate complex, sacred concepts and easily maintain the profound mental focus required for your most important work.",
    "mercury_opposition_vesta": "<b>Mercury Opposite Vesta:</b> You often find that the mundane details of life constantly interrupt your profound focus. You must learn to integrate everyday communication with the silent, dedicated space required by your inner flame.",

    "venus_conjunction_vesta": "<b>Venus Conjunct Vesta:</b> For you, love is an act of pure devotion. You approach relationships, aesthetics, and values with a sacred focus, requiring a partnership that honors your spiritual dedication and respects your need for periodic withdrawal.",
    "venus_square_vesta": "<b>Venus Square Vesta:</b> Your desire for romantic connection often conflicts with your need for spiritual autonomy and intense focus. You may struggle to balance intimacy with the sacrifices required by your deepest devotions.",
    "venus_trine_vesta": "<b>Venus Trine Vesta:</b> You effortlessly combine romance and aesthetic beauty with profound spiritual dedication. Your relationships naturally support your sacred work, and you bring a pure, devoted quality to everyone you love.",
    "venus_opposition_vesta": "<b>Venus Opposite Vesta:</b> You may feel torn between the pleasures of relationships and the strict requirements of your personal focus. True equilibrium means finding a partner who understands that your withdrawal is not abandonment, but a necessary tending of your own fire.",

    "mars_conjunction_vesta": "<b>Mars Conjunct Vesta:</b> Your drive and aggression are entirely subordinated to your sacred focus. You possess an incredible, almost fanatical willpower when directed toward a specific goal, capable of immense sacrifice to achieve your ultimate purpose.",
    "mars_square_vesta": "<b>Mars Square Vesta:</b> Your impulsive desires frequently clash with your need for spiritual discipline. You may experience intense frustration when your physical drive disrupts your focused work, forcing you to consciously align your energy with your higher intentions.",
    "mars_trine_vesta": "<b>Mars Trine Vesta:</b> Your courage and willpower naturally support your most devoted work. You effortlessly channel your immense energy into your sacred goals, acting with precision, discipline, and unwavering focus.",
    "mars_opposition_vesta": "<b>Mars Opposite Vesta:</b> You often experience conflicts between your aggressive instincts and your need for pure dedication. You may encounter external obstacles that challenge your focus, teaching you to protect your inner flame with calm, calculated strength rather than explosive anger."
};

Object.assign(data.aspects, newAspects);
fs.writeFileSync(path, JSON.stringify(data, null, 2));
console.log("Added " + Object.keys(newAspects).length + " new asteroid aspects to extra_bodies.json.");
