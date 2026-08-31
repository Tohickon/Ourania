const fs = require('fs');
const path = 'C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\src\\main\\resources\\data\\extra_bodies.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

if (!data.aspects) data.aspects = {};

const newAspects = {
    // ----------------------------------------------------
    // NEPTUNE
    // ----------------------------------------------------
    "neptune_conjunction_chiron": "<b>Neptune Conjunct Chiron:</b> Your deepest wounds are highly sensitive and emotionally porous. You possess an almost psychic empathy, but your healing journey requires learning how to dissolve the illusions that keep you tethered to past pain.",
    "neptune_square_chiron": "<b>Neptune Square Chiron:</b> There is a confusing tension between your immense compassion and your deep insecurities. You may struggle with escapism or playing the victim/savior, challenging you to ground your healing in reality rather than fantasy.",
    "neptune_trine_chiron": "<b>Neptune Trine Chiron:</b> You possess an extraordinary, mystical capacity for healing. Your empathy flows effortlessly, allowing you to soothe the collective unconscious and channel divine comfort to those who suffer.",
    "neptune_opposition_chiron": "<b>Neptune Opposite Chiron:</b> You often project your own pain onto a romanticized savior, or attempt to 'rescue' people who drain you. True healing requires seeing yourself and others clearly, without the rose-colored glasses.",

    "neptune_conjunction_north_node": "<b>Neptune Conjunct North Node:</b> Your evolutionary path requires complete surrender to the divine. You are meant to develop your intuition, artistic gifts, and spiritual compassion as you step toward a profoundly mystical destiny.",
    "neptune_conjunction_south_node": "<b>Neptune Conjunct South Node:</b> You carry a karmic history of escapism, illusion, or deep spiritual retreat. Your challenge is to avoid dissolving into past fantasies and instead use your innate mystical wisdom to navigate the real world.",
    "neptune_square_north_node": "<b>Neptune Square the Nodes:</b> Your tendency toward illusion or escapism frequently derails your karmic progress. You must learn to distinguish true spiritual guidance from deceptive fantasies if you wish to evolve.",
    "neptune_trine_north_node": "<b>Neptune Trine North Node:</b> Your intuition effortlessly guides your soul's journey. Spiritual insights, artistic inspiration, and divine synchronicities naturally align to propel you toward your destiny.",

    "neptune_conjunction_ceres": "<b>Neptune Conjunct Ceres:</b> Your caretaking is boundless and deeply spiritual. You nurture others with a transcendent, unconditional love, but must be careful not to dissolve your own boundaries in the process.",
    "neptune_square_ceres": "<b>Neptune Square Ceres:</b> There is friction between your profound empathy and your practical duties as a caretaker. You may feel overwhelmed by the suffering of others, struggling to provide tangible support when you are emotionally flooded.",
    "neptune_trine_ceres": "<b>Neptune Trine Ceres:</b> You effortlessly combine mystical compassion with deeply comforting care. You naturally create a healing, spiritually restorative sanctuary for those you love.",
    "neptune_opposition_ceres": "<b>Neptune Opposite Ceres:</b> You often feel torn between the desire to transcend the physical world and the need to tend to practical, worldly caretaking. Balance is found by bringing spiritual presence into everyday acts of love.",

    "neptune_conjunction_pallas": "<b>Neptune Conjunct Pallas:</b> Your strategic intelligence is highly intuitive and visionary. You perceive the hidden, subtle patterns behind reality, allowing you to solve problems through inspiration rather than pure logic.",
    "neptune_square_pallas": "<b>Neptune Square Pallas:</b> Your brilliant intuition often clashes with logical planning. You may struggle to execute your visionary strategies because the practical details dissolve into confusion or illusion.",
    "neptune_trine_pallas": "<b>Neptune Trine Pallas:</b> Your creative intelligence flows seamlessly with your spiritual intuition. You effortlessly weave mystical insights into brilliant, holistic strategies that inspire those around you.",
    "neptune_opposition_pallas": "<b>Neptune Opposite Pallas:</b> You frequently encounter situations where your logical strategies are undermined by deceit or confusion. True wisdom requires you to trust your intuition as much as your intellect.",

    "neptune_conjunction_juno": "<b>Neptune Conjunct Juno:</b> You seek a marriage that is a true spiritual union. Your approach to commitment is highly romantic and deeply empathetic, but you must guard against idealizing your partner to the point of illusion.",
    "neptune_square_juno": "<b>Neptune Square Juno:</b> There is a painful friction between your romantic fantasies and the reality of commitment. You may experience deceit, disillusionment, or boundary issues within your partnerships until you learn to see your mate clearly.",
    "neptune_trine_juno": "<b>Neptune Trine Juno:</b> Your relationships naturally thrive on profound spiritual connection and unconditional love. You effortlessly maintain a deeply romantic, soulful bond with your committed partner.",
    "neptune_opposition_juno": "<b>Neptune Opposite Juno:</b> You may attract partners who are evasive, addicted, or overly dependent, projecting your own desire for a savior onto the relationship. Finding balance means establishing firm boundaries within your marriage.",

    "neptune_conjunction_vesta": "<b>Neptune Conjunct Vesta:</b> Your spiritual devotion is transcendent. You pour your intense focus into mystical, artistic, or compassionate work, acting as a true conduit for the divine.",
    "neptune_square_vesta": "<b>Neptune Square Vesta:</b> Your profound empathy and spiritual yearnings often disrupt your ability to maintain discipline. You may struggle to focus your inner flame when you are constantly absorbing the chaotic emotions of the collective.",
    "neptune_trine_vesta": "<b>Neptune Trine Vesta:</b> Your intuitive gifts naturally support your sacred focus. You effortlessly dedicate yourself to spiritual or creative work, finding profound joy in quiet, mystical contemplation.",
    "neptune_opposition_vesta": "<b>Neptune Opposite Vesta:</b> You frequently feel that the overwhelming needs of the world pull you away from your solitary devotions. You must learn that you cannot heal the collective if you allow your own inner fire to be drowned.",

    "neptune_conjunction_ascendant": "<b>Neptune Conjunct Ascendant:</b> You possess an elusive, highly magnetic, and chameleon-like presence. You easily adapt to your surroundings, projecting an aura of mystery and profound empathy that draws people in.",
    "neptune_square_ascendant": "<b>Neptune Square Ascendant:</b> Your deep sensitivities often clash with how the world perceives you. You may struggle with a fragile identity, feeling misunderstood or projecting confusing signals to others.",
    "neptune_trine_ascendant": "<b>Neptune Trine Ascendant:</b> Your spiritual and artistic nature flows effortlessly into your outward persona. You naturally exude a gentle, glamorous, or soothing energy that instantly disarms those around you.",
    "neptune_opposition_ascendant": "<b>Neptune Opposite Ascendant (Conjunct Descendant):</b> You seek the divine through your partnerships, often attracting highly artistic, spiritual, or confused individuals. You must be careful not to lose yourself completely in the people you love.",

    "neptune_conjunction_mc": "<b>Neptune Conjunct MC:</b> Your career is intrinsically linked to art, spirituality, or healing. You project a glamorous or mystical public image, often serving as a blank canvas onto which the public projects its own dreams.",
    "neptune_square_mc": "<b>Neptune Square MC:</b> There is friction between your need to drift and your professional responsibilities. You may experience confusion regarding your career path or suffer from a misunderstood public reputation.",
    "neptune_trine_mc": "<b>Neptune Trine MC:</b> Your intuition effortlessly guides your professional life. You easily attract public favor through your artistic or compassionate work, enjoying a naturally inspiring public image.",
    "neptune_opposition_mc": "<b>Neptune Opposite MC (Conjunct IC):</b> Your true spiritual sanctuary is found within your home and private life. You cultivate a deeply mystical, private inner world, often seeking absolute retreat from the public eye.",

    "neptune_conjunction_fortune": "<b>Neptune Conjunct Part of Fortune:</b> Your worldly success and joy are tied to your imagination and compassion. You prosper most when you surrender to the flow of the universe and trust your divine intuition.",
    "neptune_conjunction_lilith": "<b>Neptune Conjunct Lilith:</b> The friction between spiritual transcendence and raw, taboo desire creates a powerful allure. Your exiled, dark feminine power is expressed through a deeply intoxicating, almost hypnotic magnetism.",

    // ----------------------------------------------------
    // PLUTO
    // ----------------------------------------------------
    "pluto_conjunction_chiron": "<b>Pluto Conjunct Chiron:</b> Your deepest wounds are tied to themes of profound power, control, and survival. Healing requires a total psychological death and rebirth, unearthing your darkest pain to transform it into unshakeable strength.",
    "pluto_square_chiron": "<b>Pluto Square Chiron:</b> There is volatile, obsessive tension between your insecurities and your need for control. You may destructively try to dominate others to avoid feeling vulnerable, forcing a painful but necessary psychological reckoning.",
    "pluto_trine_chiron": "<b>Pluto Trine Chiron:</b> You possess an extraordinary, relentless capacity for deep psychological healing. You effortlessly dive into the underworld of the psyche, transforming profound trauma into profound empowerment.",
    "pluto_opposition_chiron": "<b>Pluto Opposite Chiron:</b> You often attract intense power struggles that trigger your deepest wounds. Healing requires you to stop fighting external forces of control and instead confront the shadows within yourself.",

    "pluto_conjunction_north_node": "<b>Pluto Conjunct North Node:</b> Your evolutionary path requires absolute transformation. You are meant to dive into the depths of power and psychology, repeatedly burning down your old life to rise from the ashes.",
    "pluto_conjunction_south_node": "<b>Pluto Conjunct South Node:</b> You carry a karmic history of intense trauma, obsession, or the misuse of power. Your challenge is to stop relying on manipulation or absolute control, learning instead to trust the process of evolution.",
    "pluto_square_north_node": "<b>Pluto Square the Nodes:</b> Your obsessive need for control frequently blocks your karmic progress. You must learn to surrender your grip on the past and allow the necessary, terrifying transformations to occur.",
    "pluto_trine_north_node": "<b>Pluto Trine North Node:</b> Intense experiences and profound psychological insights naturally propel you along your soul's journey. Your willingness to face the darkness effortlessly attracts the right evolutionary opportunities.",

    "pluto_conjunction_ceres": "<b>Pluto Conjunct Ceres:</b> Your caretaking is fierce, obsessive, and all-consuming. You love with an intensity that borders on the possessive, mirroring the myth of Demeter's desperate, world-ending grief over the loss of her child.",
    "pluto_square_ceres": "<b>Pluto Square Ceres:</b> There is violent tension between your need to nurture and your need for control. You may experience devastating losses or intense power struggles over those you care for, challenging you to love without clutching.",
    "pluto_trine_ceres": "<b>Pluto Trine Ceres:</b> You effortlessly combine profound psychological insight with deeply protective care. You are a fiercely loyal guardian, naturally transforming the lives of those you nurture.",
    "pluto_opposition_ceres": "<b>Pluto Opposite Ceres:</b> You often experience conflicts where caretaking feels like a battle for survival. You must learn to navigate the space between total devotion and the terrifying reality of letting go.",

    "pluto_conjunction_pallas": "<b>Pluto Conjunct Pallas:</b> Your strategic intelligence is ruthless and penetrating. You possess the mind of a master detective or a formidable general, capable of seeing through any deception to the absolute core of a problem.",
    "pluto_square_pallas": "<b>Pluto Square Pallas:</b> Your brilliant strategies are often undermined by paranoia or an obsessive need to dominate. You are challenged to use your piercing intellect for transformation rather than destruction.",
    "pluto_trine_pallas": "<b>Pluto Trine Pallas:</b> Your tactical genius flows seamlessly with your understanding of power dynamics. You effortlessly outmaneuver opponents, using your profound psychological insight to execute flawless strategies.",
    "pluto_opposition_pallas": "<b>Pluto Opposite Pallas:</b> You frequently encounter formidable opposition to your plans. True wisdom requires you to recognize when a battle of wills is destroying the very objective you set out to achieve.",

    "pluto_conjunction_juno": "<b>Pluto Conjunct Juno:</b> You seek a marriage that is intense, transformative, and absolute. Your partnerships are characterized by deep psychological bonding, unwavering loyalty, and occasionally, fierce power struggles.",
    "pluto_square_juno": "<b>Pluto Square Juno:</b> There is a painful, obsessive friction between your desire for commitment and your need for control. You may experience jealousy, betrayal, or intense power dynamics within your relationships until you learn to trust.",
    "pluto_trine_juno": "<b>Pluto Trine Juno:</b> Your relationships naturally thrive on profound depth and total honesty. You effortlessly maintain a powerful, unshakeable bond with your partner, weathering any transformation together.",
    "pluto_opposition_juno": "<b>Pluto Opposite Juno:</b> You may project your darkest fears onto your partner, attracting relationships that feel like a matter of life and death. Finding equilibrium means stopping the cycle of manipulation and embracing genuine vulnerability.",

    "pluto_conjunction_vesta": "<b>Pluto Conjunct Vesta:</b> Your spiritual devotion is intense, secretive, and all-consuming. You pour a terrifying amount of energy into your sacred focus, often transforming yourself completely through your dedication to the work.",
    "pluto_square_vesta": "<b>Pluto Square Vesta:</b> Your obsessive tendencies frequently clash with your need for pure discipline. You may experience intense crises of faith or destructive compulsions that pull you away from tending your inner flame.",
    "pluto_trine_vesta": "<b>Pluto Trine Vesta:</b> Your profound psychological strength naturally supports your most intense devotions. You effortlessly maintain a powerful, unwavering focus, allowing you to achieve remarkable transformations through your work.",
    "pluto_opposition_vesta": "<b>Pluto Opposite Vesta:</b> You frequently feel that external power struggles threaten to extinguish your sacred focus. You must learn to protect your inner fire fiercely without becoming consumed by paranoia.",

    "pluto_conjunction_ascendant": "<b>Pluto Conjunct Ascendant:</b> You possess an intense, intimidating, and deeply magnetic presence. You meet the world with an aura of hidden power, demanding absolute authenticity and frequently provoking strong reactions in others.",
    "pluto_square_ascendant": "<b>Pluto Square Ascendant:</b> Your need for control frequently clashes with how you present yourself. You might come across as overly secretive or ruthlessly intense, challenging you to soften your armor and allow others in.",
    "pluto_trine_ascendant": "<b>Pluto Trine Ascendant:</b> Your profound psychological depth flows effortlessly into a commanding persona. You naturally exude a quiet, unshakeable authority that commands respect without needing to say a word.",
    "pluto_opposition_ascendant": "<b>Pluto Opposite Ascendant (Conjunct Descendant):</b> You attract intense, powerful, or manipulative partners. You learn about your own capacity for destruction and rebirth through the crucible of one-on-one relationships.",

    "pluto_conjunction_mc": "<b>Pluto Conjunct MC:</b> Your career is marked by immense ambition, power, and periodic, total transformations. You are recognized publicly as a formidable force, often drawn to professions involving psychology, research, or absolute control.",
    "pluto_square_mc": "<b>Pluto Square MC:</b> Your intense drive for professional dominance often meets ruthless opposition. You may experience public crises or vicious power struggles, forcing you to rebuild your reputation from the ashes.",
    "pluto_trine_mc": "<b>Pluto Trine MC:</b> Your understanding of power dynamics naturally propels your career forward. You effortlessly navigate the complexities of your profession, quietly asserting control and achieving profound public success.",
    "pluto_opposition_mc": "<b>Pluto Opposite MC (Conjunct IC):</b> Your home and private life are the true sites of your deepest transformations. You guard your sanctuary fiercely, experiencing profound psychological rebirths away from the public eye.",

    "pluto_conjunction_fortune": "<b>Pluto Conjunct Part of Fortune:</b> Your greatest worldly success and joy are found in the depths. You prosper most when you embrace total transformation, fearlessly investigating the hidden truths of the world.",
    "pluto_conjunction_lilith": "<b>Pluto Conjunct Lilith:</b> The fusion of the lord of the underworld with the exiled feminine creates an utterly terrifying, uncompromising power. You possess a raw, dangerous magnetism that refuses to submit to anyone."
};

Object.assign(data.aspects, newAspects);
fs.writeFileSync(path, JSON.stringify(data, null, 2));
console.log("Added outer planets (Neptune, Pluto) aspects to extra_bodies.json.");
