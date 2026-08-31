const fs = require('fs');
const path = 'C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\src\\main\\resources\\data\\extra_bodies.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

if (!data.aspects) {
    data.aspects = {};
}

const newAspects = {
    "sun_conjunction_chiron": "<b>Sun Conjunct Chiron:</b> Your core identity is deeply entwined with a foundational wound, meaning you cannot hide your vulnerabilities. By learning to accept your imperfections, your very presence becomes a source of healing and profound wisdom for others.",
    "sun_square_chiron": "<b>Sun Square Chiron:</b> There is a dynamic tension between your ego and your deepest insecurities. You may overcompensate for feelings of inadequacy, but this constant friction forces you to develop incredible resilience and a hard-won sense of self-worth.",
    "sun_trine_chiron": "<b>Sun Trine Chiron:</b> Your sense of self flows naturally with your capacity to heal. You intuitively understand that vulnerability is a strength, making you a gifted teacher or guide who easily helps others find their own inner light.",
    "sun_opposition_chiron": "<b>Sun Opposite Chiron:</b> You often project your own wounds onto others, attracting people who need healing or feeling hurt by those you admire. Balance is found when you realize that the healer you seek outside is actually within you.",

    "moon_conjunction_chiron": "<b>Moon Conjunct Chiron:</b> Your emotional world and instincts are extraordinarily sensitive, often carrying the imprint of early childhood or maternal wounds. This deep emotional permeability makes you intensely empathetic, capable of nurturing others in ways you yourself need.",
    "moon_square_chiron": "<b>Moon Square Chiron:</b> Emotional security feels elusive, as your deepest needs frequently clash with your sorest vulnerabilities. You may struggle with self-soothing, but this persistent emotional challenge ultimately teaches you how to become your own most fierce and tender protector.",
    "moon_trine_chiron": "<b>Moon Trine Chiron:</b> You possess an innate emotional wisdom and a natural talent for holding space for the pain of others. Your subconscious gracefully processes hurt, allowing you to use your own past experiences as a wellspring of comfort and empathy.",
    "moon_opposition_chiron": "<b>Moon Opposite Chiron:</b> Your emotional needs often feel at odds with your healing journey, leading to a tendency to either over-nurture others to avoid your own pain or feel emotionally isolated. Wholeness comes from turning your immense compassion inward.",

    "mercury_conjunction_chiron": "<b>Mercury Conjunct Chiron:</b> Your voice and intellect are directly connected to your deepest vulnerabilities. You may have struggled with being heard or feeling misunderstood, but this gives you a profound ability to speak healing truths and communicate with piercing insight.",
    "mercury_square_chiron": "<b>Mercury Square Chiron:</b> There is a tension between how you think and where you feel inadequate, often manifesting as a fear of speaking up or hyper-criticism. This mental friction challenges you to heal your relationship with your own mind and voice.",
    "mercury_trine_chiron": "<b>Mercury Trine Chiron:</b> Your mind naturally gravitates toward understanding the mechanics of healing. You have a gift for therapeutic communication, easily finding the right words to soothe, advise, and untangle complex emotional knots for yourself and others.",
    "mercury_opposition_chiron": "<b>Mercury Opposite Chiron:</b> Conversations often trigger old wounds, or you may find yourself constantly arguing with others' pain. You learn best when you realize that listening is just as healing as having the right answers.",

    "venus_conjunction_chiron": "<b>Venus Conjunct Chiron:</b> Your approach to love, beauty, and values is intimately tied to your deepest wound. Relationships may feel inherently vulnerable, but this placement grants you an extraordinary capacity to love people exactly as they are, flaws and all.",
    "venus_square_chiron": "<b>Venus Square Chiron:</b> You experience a profound tension between your desire for connection and your fear of unworthiness. You may attract painful relationship dynamics until you learn that your value is inherent and cannot be earned through suffering.",
    "venus_trine_chiron": "<b>Venus Trine Chiron:</b> You possess a natural grace in matters of the heart and a gift for bringing harmony to broken situations. Your relationships are often sources of mutual healing, and you effortlessly see the beauty in imperfection.",
    "venus_opposition_chiron": "<b>Venus Opposite Chiron:</b> You tend to seek healing through your partners, or you attract partners who require 'saving.' True balance is achieved when you stop trying to fix or be fixed through romance, and instead cultivate self-love.",

    "mars_conjunction_chiron": "<b>Mars Conjunct Chiron:</b> Your drive and assertion are fused with your deepest vulnerability. You may struggle with anger or feel your courage is wounded, but you are fiercely protective of the underdog and fight hardest for those who cannot defend themselves.",
    "mars_square_chiron": "<b>Mars Square Chiron:</b> There is a volatile friction between your willpower and your insecurities. You might act out defensively or struggle to assert your boundaries, forcing you to consciously redefine what true strength and healthy anger look like.",
    "mars_trine_chiron": "<b>Mars Trine Chiron:</b> You easily channel your energy into healing or advocacy. Your actions are instinctively aligned with a higher, restorative purpose, allowing you to pioneer new therapeutic methods or boldly lead others through their pain.",
    "mars_opposition_chiron": "<b>Mars Opposite Chiron:</b> You often experience conflict that triggers deep feelings of inadequacy, projecting your anger onto others or attracting aggressive behavior. Healing requires integrating your assertiveness rather than fearing your own power.",

    "sun_conjunction_north_node": "<b>Sun Conjunct North Node:</b> Your core identity and ego are directly aligned with your soul's evolutionary path. You are meant to shine brightly, step into leadership, and boldly embody your unique purpose, even when it feels terrifyingly new.",
    "sun_conjunction_south_node": "<b>Sun Conjunct South Node:</b> You carry a deeply ingrained sense of self and innate talents from the past. While you easily command authority, your evolutionary path requires you to let go of ego-driven validation and learn to support others from behind the scenes.",
    "sun_square_north_node": "<b>Sun Square the Nodes:</b> Your ego and conscious desires frequently clash with your karmic path. You may feel torn between who you are expected to be and what your soul needs to learn, requiring a profound restructuring of your identity to move forward.",
    "sun_trine_north_node": "<b>Sun Trine North Node:</b> Your natural self-expression flows effortlessly toward your karmic destiny. Opportunities for growth and leadership present themselves easily, and your creative vitality naturally supports your soul's evolutionary journey.",

    "moon_conjunction_north_node": "<b>Moon Conjunct North Node:</b> Your emotional needs and instincts are pulling you directly toward your karmic future. You are learning to trust your feelings and nurture yourself in entirely new ways, stepping out of your emotional comfort zone to find true security.",
    "moon_conjunction_south_node": "<b>Moon Conjunct South Node:</b> You have a profound, instinctual reliance on past emotional patterns. While you possess incredible empathy and historical wisdom, you must avoid retreating into familiar emotional dependencies when faced with the unknown.",
    "moon_square_north_node": "<b>Moon Square the Nodes:</b> Your deep-seated emotional habits create friction with your evolutionary path. You often have to choose between emotional safety and necessary growth, learning to untangle your authentic needs from inherited family conditioning.",
    "moon_trine_north_node": "<b>Moon Trine North Node:</b> Your emotional instincts are beautifully harmonized with your soul's direction. You intuitively attract the right emotional support and home environments that naturally foster your karmic growth.",

    "mercury_conjunction_north_node": "<b>Mercury Conjunct North Node:</b> Your voice, intellect, and ideas are the key to your evolutionary path. You are meant to learn, communicate, and share information in new, innovative ways, prioritizing curiosity over established dogma.",
    "mercury_conjunction_south_node": "<b>Mercury Conjunct South Node:</b> You possess an old, innate wisdom and a mind that easily grasps familiar concepts. However, your challenge is to stop relying on intellectual assumptions and open your mind to entirely new ways of thinking and listening.",
    "mercury_square_north_node": "<b>Mercury Square the Nodes:</b> Your habitual ways of thinking and communicating often obstruct your karmic progress. You are challenged to recognize when your logical mind is making excuses that keep you from stepping into your true path.",
    "mercury_trine_north_node": "<b>Mercury Trine North Node:</b> Your communication style and intellectual pursuits effortlessly support your life's purpose. You easily connect with the right people and ideas that propel you forward on your soul's journey.",

    "venus_conjunction_north_node": "<b>Venus Conjunct North Node:</b> Your karmic path involves embracing love, beauty, and partnership in fresh, perhaps unconventional ways. You are learning to value yourself highly and attract relationships that push you to evolve rather than stagnate.",
    "venus_conjunction_south_node": "<b>Venus Conjunct South Node:</b> You have a deep, innate understanding of romance and aesthetics, but you may rely too heavily on relationships for your sense of worth. Your evolutionary task is to find self-sufficiency and avoid falling back into comfortable, yet outgrown, relational patterns.",
    "venus_square_north_node": "<b>Venus Square the Nodes:</b> Matters of the heart and financial security frequently create tension with your spiritual growth. You must learn to navigate the friction between what you desire and what your soul actually needs to evolve.",
    "venus_trine_north_node": "<b>Venus Trine North Node:</b> Your values and relationships naturally align with your karmic destiny. You effortlessly attract beneficial partnerships and resources that aid you on your path to growth.",

    "mars_conjunction_north_node": "<b>Mars Conjunct North Node:</b> Your evolutionary path requires courage, action, and the willingness to fight for what you want. You are learning to assert your independence and channel your passions constructively into new frontiers.",
    "mars_conjunction_south_node": "<b>Mars Conjunct South Node:</b> You possess innate strength, combativeness, or leadership skills from the past. Your challenge is to avoid reacting with unnecessary aggression or impulsivity, learning instead to use your immense drive for cooperative or higher purposes.",
    "mars_square_north_node": "<b>Mars Square the Nodes:</b> Your drive and anger often pull you off your karmic path, leading to frustrating conflicts or impulsive actions. You are challenged to consciously align your willpower with your soul's true direction, rather than fighting against it.",
    "mars_trine_north_node": "<b>Mars Trine North Node:</b> Your courage and drive effortlessly fuel your evolutionary journey. You have a natural instinct for taking the right risks and asserting yourself in ways that propel you toward your destiny."
};

Object.assign(data.aspects, newAspects);

fs.writeFileSync(path, JSON.stringify(data, null, 2));
console.log("Updated extra_bodies.json successfully with " + Object.keys(newAspects).length + " new aspects.");
