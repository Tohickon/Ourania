const fs = require('fs');
const path = 'C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\src\\main\\resources\\data\\extra_bodies.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

if (!data.aspects) {
    data.aspects = {};
}

const newAspects = {
    // Ascendant Aspects
    "sun_conjunction_ascendant": "<b>Sun Conjunct Ascendant:</b> Your core identity and outward persona are perfectly aligned. You radiate confidence and natural leadership, meeting the world head-on with a powerful, unmistakable presence.",
    "sun_square_ascendant": "<b>Sun Square Ascendant:</b> There is tension between who you truly are and how the world perceives you. You may feel misunderstood or forced to assert yourself aggressively, challenging you to integrate your authentic self with your public face.",
    "sun_trine_ascendant": "<b>Sun Trine Ascendant:</b> Your vital energy flows effortlessly into your outward persona. You easily attract positive attention and possess a natural, warm magnetism that makes others instantly comfortable in your presence.",
    "sun_opposition_ascendant": "<b>Sun Opposite Ascendant (Conjunct Descendant):</b> You shine brightest in one-on-one relationships, often seeking partners who embody the confidence or solar qualities you feel you lack, until you learn to own your own light.",

    "moon_conjunction_ascendant": "<b>Moon Conjunct Ascendant:</b> You wear your heart on your sleeve. Your emotions are immediately visible to others, and you possess a deeply empathetic, nurturing presence that makes people feel instantly cared for.",
    "moon_square_ascendant": "<b>Moon Square Ascendant:</b> Your emotional needs often clash with your outward demeanor. You may unintentionally project moodiness or defensiveness, requiring you to find healthier ways to express your vulnerabilities publicly.",
    "moon_trine_ascendant": "<b>Moon Trine Ascendant:</b> Your emotional world harmonizes beautifully with your physical presence. You project a soothing, receptive energy that easily puts others at ease and naturally draws supportive people toward you.",
    "moon_opposition_ascendant": "<b>Moon Opposite Ascendant (Conjunct Descendant):</b> You seek profound emotional security through your partnerships. You may attract highly emotional partners or rely heavily on a significant other to soothe your inner world.",

    "mercury_conjunction_ascendant": "<b>Mercury Conjunct Ascendant:</b> You approach the world with intense curiosity and a restless intellect. You are talkative, quick-witted, and your first instinct is to analyze and communicate your immediate environment.",
    "mercury_square_ascendant": "<b>Mercury Square Ascendant:</b> There is friction between how you think and how you present yourself. You may frequently experience misunderstandings or come across as overly critical, challenging you to align your words with your body language.",
    "mercury_trine_ascendant": "<b>Mercury Trine Ascendant:</b> You possess an effortless, charming communication style. You are highly articulate and easily translate your intelligent thoughts into a persona that others find engaging and clear.",
    "mercury_opposition_ascendant": "<b>Mercury Opposite Ascendant (Conjunct Descendant):</b> You seek mental stimulation and lively debate in your one-on-one relationships. You are drawn to articulate partners who challenge your ideas and keep your mind active.",

    "venus_conjunction_ascendant": "<b>Venus Conjunct Ascendant:</b> You project an aura of immense charm, beauty, and grace. You meet the world with a desire for harmony and possess a natural aesthetic sensibility that draws people to you effortlessly.",
    "venus_square_ascendant": "<b>Venus Square Ascendant:</b> Your desire to be liked may clash with your authentic self-presentation. You might struggle with vanity or compromise too much for the sake of peace, challenging you to find true, unapologetic self-love.",
    "venus_trine_ascendant": "<b>Venus Trine Ascendant:</b> Your physical presence naturally exudes warmth and attractiveness. You navigate social situations with extreme ease, effortlessly creating a harmonious and pleasant environment wherever you go.",
    "venus_opposition_ascendant": "<b>Venus Opposite Ascendant (Conjunct Descendant):</b> You define yourself heavily through your partnerships. You seek beautiful, harmonious relationships and may project your own loving qualities onto your chosen mate.",

    "mars_conjunction_ascendant": "<b>Mars Conjunct Ascendant:</b> You meet the world with raw, dynamic energy and fierce independence. You are a natural pioneer, projecting an aura of courage, physical vitality, and unapologetic assertiveness.",
    "mars_square_ascendant": "<b>Mars Square Ascendant:</b> Your intense drive often comes across as aggression or impatience to others. You may encounter frequent conflicts or power struggles, forcing you to learn how to assert yourself without unnecessary hostility.",
    "mars_trine_ascendant": "<b>Mars Trine Ascendant:</b> Your physical energy and willpower flow smoothly into your outward actions. You are bold, highly effective, and easily inspire others with your confident, pioneering spirit.",
    "mars_opposition_ascendant": "<b>Mars Opposite Ascendant (Conjunct Descendant):</b> You often attract highly assertive or combative partners. You may project your own anger onto others, challenging you to own your personal power rather than fighting it in a mate.",

    // MC Aspects
    "sun_conjunction_mc": "<b>Sun Conjunct MC:</b> Your core identity is deeply tied to your career and public image. You are meant to be seen and recognized for your authority, naturally taking on leadership roles and striving for significant worldly achievement.",
    "sun_square_mc": "<b>Sun Square MC:</b> There is tension between your personal ego and your public responsibilities. You may experience conflicts with authority figures or struggle to balance your private identity with the demands of your career.",
    "sun_trine_mc": "<b>Sun Trine MC:</b> Your natural vitality smoothly supports your professional ambitions. Opportunities for public recognition come easily, and your authentic self is well-received by the world at large.",
    "sun_opposition_mc": "<b>Sun Opposite MC (Conjunct IC):</b> Your true identity is rooted in your private life and family heritage. While you may have public ambitions, your core vitality is drawn from establishing a deeply secure, personal foundation.",

    "moon_conjunction_mc": "<b>Moon Conjunct MC:</b> Your emotional needs are intertwined with your public life and career. You may be known for your empathy or work in a caretaking profession, but your changing moods are often highly visible to the public.",
    "moon_square_mc": "<b>Moon Square MC:</b> Your need for emotional security frequently clashes with your professional ambitions. You may struggle to balance the demands of your public role with the profound need for a safe, private sanctuary.",
    "moon_trine_mc": "<b>Moon Trine MC:</b> You have a natural instinct for what the public wants. Your emotional intelligence effortlessly supports your career goals, allowing you to connect intuitively with audiences or authority figures.",
    "moon_opposition_mc": "<b>Moon Opposite MC (Conjunct IC):</b> Your deepest emotional fulfillment is found at home and with family. You prioritize your private sanctuary over public recognition, finding your anchor in your roots and heritage.",

    "mercury_conjunction_mc": "<b>Mercury Conjunct MC:</b> Your intellect and communication skills are the focal point of your career. You are likely known for your ideas, writing, or speaking, easily adapting to the demands of your public life with a sharp mind.",
    "mercury_square_mc": "<b>Mercury Square MC:</b> There is friction between your ideas and your professional path. You may struggle with miscommunications affecting your public image, challenging you to think carefully before speaking in professional settings.",
    "mercury_trine_mc": "<b>Mercury Trine MC:</b> Your communication style effortlessly enhances your professional reputation. You easily articulate your ambitions and network effectively, using your intellect to steadily climb the ladder.",
    "mercury_opposition_mc": "<b>Mercury Opposite MC (Conjunct IC):</b> Your mind is focused deeply on your roots, family, and private life. You may work from home or possess a rich, intellectual inner life that you keep hidden from the public eye.",

    "venus_conjunction_mc": "<b>Venus Conjunct MC:</b> You bring beauty, charm, and diplomacy to your public life. You are well-liked in your career and may be drawn to professions involving art, aesthetics, or public relations, smoothing over conflicts with grace.",
    "venus_square_mc": "<b>Venus Square MC:</b> Your desire for pleasure and harmony may conflict with your professional responsibilities. You might struggle with the optics of your relationships affecting your public image, requiring a balance between love and duty.",
    "venus_trine_mc": "<b>Venus Trine MC:</b> Your natural charm makes your professional ascent smooth and pleasant. You easily attract helpful alliances and favorable public attention, using your social grace to advance your career.",
    "venus_opposition_mc": "<b>Venus Opposite MC (Conjunct IC):</b> You seek beauty and harmony primarily within your home and private life. You prioritize creating a loving, aesthetically pleasing sanctuary over chasing public accolades.",

    "mars_conjunction_mc": "<b>Mars Conjunct MC:</b> Your drive and ambition are intensely focused on your career. You are a fierce competitor in the public arena, relentlessly pursuing success and unafraid to take bold, independent action to reach the top.",
    "mars_square_mc": "<b>Mars Square MC:</b> Your aggressive instincts often clash with authority or your public image. You may experience professional conflicts or a reputation for impulsivity, challenging you to channel your fiery energy constructively.",
    "mars_trine_mc": "<b>Mars Trine MC:</b> Your willpower and courage perfectly support your professional goals. You take decisive, highly effective action in your career, easily overcoming obstacles on your path to success.",
    "mars_opposition_mc": "<b>Mars Opposite MC (Conjunct IC):</b> Your deepest drive is focused on protecting and securing your home. You may expend tremendous energy on your private life, fiercely defending your family or asserting independence within your roots.",

    // Part of Fortune Aspects
    "sun_conjunction_fortune": "<b>Sun Conjunct Part of Fortune:</b> Your worldly success and deepest joy are directly tied to your core identity. You find the greatest ease in life when you authentically express yourself and shine your unique light.",
    "moon_conjunction_fortune": "<b>Moon Conjunct Part of Fortune:</b> Your prosperity and happiness are rooted in your emotional security. You find incredible luck and ease when you honor your intuition and nurture your deepest feelings.",
    "mercury_conjunction_fortune": "<b>Mercury Conjunct Part of Fortune:</b> Your greatest success and joy come through your intellect, writing, and communication. Sharing your ideas and connecting with others is your path of least resistance to prosperity.",
    "venus_conjunction_fortune": "<b>Venus Conjunct Part of Fortune:</b> Your luck and worldly ease are intimately linked to beauty, harmony, and partnership. You find true joy when you surround yourself with aesthetics and cultivate loving relationships.",
    "mars_conjunction_fortune": "<b>Mars Conjunct Part of Fortune:</b> Your prosperity is unlocked through courage, action, and physical drive. You find your greatest success when you boldly pioneer new paths and assert your independence.",
    
    // Lilith Aspects
    "sun_conjunction_lilith": "<b>Sun Conjunct Lilith:</b> Your core identity is fused with a raw, primal power that refuses to be tamed. You possess a dark, magnetic charisma, but must be careful not to let your exiled desires completely consume your ego.",
    "moon_conjunction_lilith": "<b>Moon Conjunct Lilith:</b> Your emotional world contains deep, often taboo passions and an intense intuition. You possess a fierce, untamed emotional strength, but may struggle with feelings of rejection or suppressed rage.",
    "mercury_conjunction_lilith": "<b>Mercury Conjunct Lilith:</b> Your mind naturally gravitates toward the forbidden or the hidden truths of the world. You have a piercing, provocative communication style that can easily unearth what others wish to keep buried.",
    "venus_conjunction_lilith": "<b>Venus Conjunct Lilith:</b> Your approach to love and beauty is intensely passionate, deeply magnetic, and decidedly unconventional. You refuse to conform to polite relationship expectations, seeking raw authenticity over superficial harmony.",
    "mars_conjunction_lilith": "<b>Mars Conjunct Lilith:</b> Your drive and anger are explosive, deeply primal, and incredibly powerful. You possess a terrifying courage when fighting against oppression, but must learn to master this raw energy so it doesn't become self-destructive."
};

Object.assign(data.aspects, newAspects);
fs.writeFileSync(path, JSON.stringify(data, null, 2));
console.log("Added " + Object.keys(newAspects).length + " new angles/points aspects to extra_bodies.json.");
