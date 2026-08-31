const fs = require('fs');
const path = 'C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\src\\main\\resources\\data\\extra_bodies.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

if (!data.aspects) data.aspects = {};

const newAspects = {
    // ----------------------------------------------------
    // JUPITER
    // ----------------------------------------------------
    "jupiter_conjunction_chiron": "<b>Jupiter Conjunct Chiron:</b> Your capacity for healing is vast and philosophically driven. You find profound meaning and spiritual expansion through the process of acknowledging and treating your deepest wounds.",
    "jupiter_square_chiron": "<b>Jupiter Square Chiron:</b> You may overcompensate for feelings of inadequacy through grandiosity or excessive optimism. True growth comes from accepting that you don't need to be perfectly healed to be worthy of abundance.",
    "jupiter_trine_chiron": "<b>Jupiter Trine Chiron:</b> You naturally attract teachers and experiences that facilitate profound healing. Your innate optimism effortlessly transforms your past pain into wisdom that inspires others.",
    "jupiter_opposition_chiron": "<b>Jupiter Opposite Chiron:</b> You may seek the ultimate 'cure' through gurus, travel, or dogma, often ignoring the wound right in front of you. Balance requires finding faith within your own flawed, human experience.",

    "jupiter_conjunction_north_node": "<b>Jupiter Conjunct North Node:</b> Your evolutionary path is blessed with incredible luck and expansion. You are meant to take massive leaps of faith, embracing optimism and grand visions as you step into your future.",
    "jupiter_conjunction_south_node": "<b>Jupiter Conjunct South Node:</b> You possess an innate, ancient wisdom and perhaps a reliance on luck or philosophical dogma from your past. Your challenge is to not coast on old beliefs, but to constantly seek new truths.",
    "jupiter_square_north_node": "<b>Jupiter Square the Nodes:</b> Your desire for freedom and expansion often clashes with the specific, focused lessons your soul needs to learn. You must learn to discipline your optimism so it serves your karmic growth.",
    "jupiter_trine_north_node": "<b>Jupiter Trine North Node:</b> Opportunities for growth, travel, and higher learning naturally align with your soul's purpose. The universe effortlessly opens doors for you when you follow your karmic path.",

    "jupiter_conjunction_ceres": "<b>Jupiter Conjunct Ceres:</b> You nurture on a grand scale. You show love through immense generosity, abundant feeding, and providing expansive opportunities for those you care about.",
    "jupiter_square_ceres": "<b>Jupiter Square Ceres:</b> There is tension between your desire for boundless freedom and your duties as a caretaker. You may over-promise what you can provide, forcing you to set realistic boundaries on your generosity.",
    "jupiter_trine_ceres": "<b>Jupiter Trine Ceres:</b> Your caretaking abilities are naturally amplified by your optimism. You effortlessly create environments of abundance, warmth, and profound safety for your loved ones.",
    "jupiter_opposition_ceres": "<b>Jupiter Opposite Ceres:</b> You often feel torn between the urge to explore the world and the need to tend the home fires. You must find a way to nurture your own wanderlust without neglecting your responsibilities.",

    "jupiter_conjunction_pallas": "<b>Jupiter Conjunct Pallas:</b> Your strategic mind operates on a macro level. You possess brilliant, visionary intelligence and the ability to recognize massive cultural or philosophical patterns.",
    "jupiter_square_pallas": "<b>Jupiter Square Pallas:</b> Your grand visions may lack the necessary tactical details. You are challenged to ground your brilliant, expansive ideas in practical strategy before they collapse under their own weight.",
    "jupiter_trine_pallas": "<b>Jupiter Trine Pallas:</b> Your intellectual strategies flow beautifully with your overarching beliefs. You effortlessly design massive, successful plans because you understand the broader philosophical implications.",
    "jupiter_opposition_pallas": "<b>Jupiter Opposite Pallas:</b> You may experience conflict between your faith and your logic. True wisdom is found when you integrate your expansive beliefs with cold, objective strategy.",

    "jupiter_conjunction_juno": "<b>Jupiter Conjunct Juno:</b> You seek a marriage or partnership that feels grand, philosophical, and profoundly expansive. You demand absolute fairness and expect your relationship to be a journey of mutual growth.",
    "jupiter_square_juno": "<b>Jupiter Square Juno:</b> Your desire for personal freedom frequently creates friction within your committed partnerships. You must learn to negotiate a commitment that allows for growth without breaking the vows.",
    "jupiter_trine_juno": "<b>Jupiter Trine Juno:</b> Your relationships naturally thrive on shared philosophies and mutual respect. You effortlessly attract a partner who acts as a true equal and co-adventurer.",
    "jupiter_opposition_juno": "<b>Jupiter Opposite Juno:</b> You may project your need for meaning onto a partner, or feel that marriage stifles your freedom. Balance requires realizing that a true partnership expands your world rather than shrinking it.",

    "jupiter_conjunction_vesta": "<b>Jupiter Conjunct Vesta:</b> Your spiritual devotion is vast and all-encompassing. You pour immense energy into your sacred focus, often elevating your specific work to the level of a grand philosophy or religion.",
    "jupiter_square_vesta": "<b>Jupiter Square Vesta:</b> You may struggle with the extreme discipline required for true devotion, preferring to keep your options open. You are challenged to commit fully to one sacred path instead of endlessly seeking.",
    "jupiter_trine_vesta": "<b>Jupiter Trine Vesta:</b> Your capacity for focus naturally expands your worldview. You find profound joy and luck when you dedicate yourself entirely to a specialized, sacred task.",
    "jupiter_opposition_vesta": "<b>Jupiter Opposite Vesta:</b> Your desire for worldly expansion often pulls you away from the solitary focus required by your inner flame. You must learn to alternate between seeking outward truth and tending the inward fire.",

    "jupiter_conjunction_ascendant": "<b>Jupiter Conjunct Ascendant:</b> You possess an outsized, jovial, and immensely charismatic presence. You meet the world with unshakable optimism and naturally inspire confidence in everyone you encounter.",
    "jupiter_square_ascendant": "<b>Jupiter Square Ascendant:</b> Your grand ambitions may clash with how others perceive you. You can come across as arrogant or overly preachy, challenging you to temper your enthusiasm with genuine humility.",
    "jupiter_trine_ascendant": "<b>Jupiter Trine Ascendant:</b> Your physical presence naturally exudes luck and generosity. You effortlessly attract favorable circumstances and people simply by being your authentic, optimistic self.",
    "jupiter_opposition_ascendant": "<b>Jupiter Opposite Ascendant (Conjunct Descendant):</b> You seek expansive, philosophical, and deeply encouraging partners. You often grow the most by merging your life with someone who teaches you to see the bigger picture.",

    "jupiter_conjunction_mc": "<b>Jupiter Conjunct MC:</b> Your career is marked by incredible luck, expansion, and public visibility. You are seen as a generous authority figure, naturally drawing success through teaching, publishing, or moral leadership.",
    "jupiter_square_mc": "<b>Jupiter Square MC:</b> Your desire for personal freedom may clash with professional expectations. You might overextend yourself or promise more than you can deliver in your career, requiring better boundaries.",
    "jupiter_trine_mc": "<b>Jupiter Trine MC:</b> Opportunities for professional advancement flow to you effortlessly. Your optimistic approach to your career ensures steady growth and a highly favorable public reputation.",
    "jupiter_opposition_mc": "<b>Jupiter Opposite MC (Conjunct IC):</b> Your greatest joy and expansion are found within your private life, home, and family. You cultivate a rich, abundant sanctuary away from the demands of the public eye.",

    "jupiter_conjunction_fortune": "<b>Jupiter Conjunct Part of Fortune:</b> This is an incredibly auspicious placement. Your greatest worldly success and joy are amplified by Jupiter's luck, meaning abundance flows to you whenever you follow your authentic joy.",
    "jupiter_conjunction_lilith": "<b>Jupiter Conjunct Lilith:</b> Your darkest, most taboo desires are magnified and unapologetic. You find profound freedom and philosophical meaning in exploring the wild, untamed aspects of human nature.",

    // ----------------------------------------------------
    // SATURN
    // ----------------------------------------------------
    "saturn_conjunction_chiron": "<b>Saturn Conjunct Chiron:</b> Your deepest wounds are tied to themes of authority, inadequacy, and restriction. Healing requires building unshakeable, practical structures and taking absolute responsibility for your own pain.",
    "saturn_square_chiron": "<b>Saturn Square Chiron:</b> There is a rigid friction between your insecurities and your need for control. You may build massive emotional walls to hide your vulnerabilities, challenging you to realize that true strength requires softness.",
    "saturn_trine_chiron": "<b>Saturn Trine Chiron:</b> You possess a disciplined, methodical approach to healing. You easily turn your painful experiences into practical wisdom, making you a reliable authority on overcoming adversity.",
    "saturn_opposition_chiron": "<b>Saturn Opposite Chiron:</b> You often project your fears onto authority figures or the structures of society. Healing comes when you stop fighting external limitations and begin to master your own internal discipline.",

    "saturn_conjunction_north_node": "<b>Saturn Conjunct North Node:</b> Your evolutionary path requires immense discipline, hard work, and the building of lasting structures. You are meant to step into authority and accept heavy responsibilities to fulfill your destiny.",
    "saturn_conjunction_south_node": "<b>Saturn Conjunct South Node:</b> You carry a karmic history of heavy responsibility or rigid restriction. Your challenge is to stop carrying the weight of the world and learn to embrace the unfamiliar freedom of your current path.",
    "saturn_square_north_node": "<b>Saturn Square the Nodes:</b> Your fears and rigid structures frequently block your karmic progress. You are challenged to dismantle outgrown boundaries and face your fear of failure in order to evolve.",
    "saturn_trine_north_node": "<b>Saturn Trine North Node:</b> Your discipline and work ethic naturally support your soul's journey. You effortlessly build the necessary foundations that allow you to steadily climb toward your karmic destiny.",

    "saturn_conjunction_ceres": "<b>Saturn Conjunct Ceres:</b> You show care through structure, provision, and duty. Nurturing is a serious responsibility for you, but you must guard against letting your caretaking become cold, transactional, or overly demanding.",
    "saturn_square_ceres": "<b>Saturn Square Ceres:</b> There is tension between your duties and your emotional need to nurture. You may feel that providing for others restricts your own life, challenging you to find a balance between discipline and unconditional love.",
    "saturn_trine_ceres": "<b>Saturn Trine Ceres:</b> You possess a reliable, steady capacity for care. You effortlessly provide practical, lasting support for your loved ones, making them feel profoundly safe within the structures you build.",
    "saturn_opposition_ceres": "<b>Saturn Opposite Ceres:</b> You often feel torn between the cold demands of the real world and the tender needs of your family. You must learn that discipline and affection are not mutually exclusive.",

    "saturn_conjunction_pallas": "<b>Saturn Conjunct Pallas:</b> Your strategic intelligence is grounded, practical, and incredibly disciplined. You excel at long-term planning, building structural solutions that endure the test of time.",
    "saturn_square_pallas": "<b>Saturn Square Pallas:</b> Your rigid thinking may stifle your creative intelligence. You often encounter structural roadblocks to your strategies, forcing you to learn flexibility and adapt your plans to reality.",
    "saturn_trine_pallas": "<b>Saturn Trine Pallas:</b> Your logic flows seamlessly into practical application. You effortlessly design and execute complex strategies, earning respect for your sheer competence and foresight.",
    "saturn_opposition_pallas": "<b>Saturn Opposite Pallas:</b> You may experience conflicts between your innovative strategies and the established rules. True success requires learning how to dismantle old structures strategically without inviting unnecessary resistance.",

    "saturn_conjunction_juno": "<b>Saturn Conjunct Juno:</b> You view marriage and commitment as a profound, enduring duty. You seek a partnership built on absolute reliability and traditional structures, but must ensure it does not become a joyless obligation.",
    "saturn_square_juno": "<b>Saturn Square Juno:</b> The heavy responsibilities of commitment often clash with your expectations of a relationship. You may feel trapped by your vows or struggle with a partner's restrictions, requiring hard work to find equality.",
    "saturn_trine_juno": "<b>Saturn Trine Juno:</b> Your partnerships are naturally stable and enduring. You effortlessly build a solid, respectable foundation with your mate, weathering any storm through mutual loyalty and discipline.",
    "saturn_opposition_juno": "<b>Saturn Opposite Juno:</b> You may project your fears of commitment onto others, attracting partners who seem overly restrictive or demanding. Balance requires taking personal responsibility for the boundaries within your relationship.",

    "saturn_conjunction_vesta": "<b>Saturn Conjunct Vesta:</b> Your spiritual devotion is marked by extreme austerity and discipline. You possess the focus of an ascetic, capable of denying yourself any pleasure to maintain your sacred, solitary work.",
    "saturn_square_vesta": "<b>Saturn Square Vesta:</b> There is friction between your worldly duties and your need for spiritual focus. You may feel that your responsibilities prevent you from tending your inner flame, challenging you to integrate the sacred into the mundane.",
    "saturn_trine_vesta": "<b>Saturn Trine Vesta:</b> Your discipline naturally enhances your capacity for extreme focus. You easily structure your life around your most important work, finding profound fulfillment in the quiet, steady pursuit of your goals.",
    "saturn_opposition_vesta": "<b>Saturn Opposite Vesta:</b> You often feel that the heavy demands of society pull you away from your true, inner purpose. You must learn to build practical boundaries that protect your solitary focus from external obligations.",

    "saturn_conjunction_ascendant": "<b>Saturn Conjunct Ascendant:</b> You project an aura of serious authority, maturity, and discipline. You meet the world cautiously, building a formidable, unshakeable persona that commands respect and demands accountability.",
    "saturn_square_ascendant": "<b>Saturn Square Ascendant:</b> Your fears and insecurities may clash with your outward demeanor. You might come across as overly rigid or defensive, challenging you to soften your boundaries and allow others to see your humanity.",
    "saturn_trine_ascendant": "<b>Saturn Trine Ascendant:</b> Your discipline and maturity flow effortlessly into your physical presence. You naturally exude competence and reliability, quickly earning the trust of those around you.",
    "saturn_opposition_ascendant": "<b>Saturn Opposite Ascendant (Conjunct Descendant):</b> You seek profound stability and maturity in your partnerships. You may attract older or highly disciplined partners, learning your hardest life lessons through one-on-one commitments.",

    "saturn_conjunction_mc": "<b>Saturn Conjunct MC:</b> Your career is built on intense discipline, ambition, and a slow, steady climb to the top. You are recognized as an absolute authority in your field, bearing the heavy weight of public responsibility.",
    "saturn_square_mc": "<b>Saturn Square MC:</b> Your professional ambitions frequently meet heavy resistance. You may struggle with authority figures or a fear of public failure, forcing you to build your reputation on undeniably solid work.",
    "saturn_trine_mc": "<b>Saturn Trine MC:</b> Your work ethic effortlessly guarantees your professional ascent. You reliably meet your goals, naturally commanding respect and building a lasting, unshakeable public legacy.",
    "saturn_opposition_mc": "<b>Saturn Opposite MC (Conjunct IC):</b> Your heaviest responsibilities and deepest structural foundations are located in your private life and family. You carry the weight of your ancestry, finding your true anchor in the home.",

    "saturn_conjunction_fortune": "<b>Saturn Conjunct Part of Fortune:</b> Your worldly success is hard-won but incredibly enduring. You find profound satisfaction and prosperity through sheer discipline, delayed gratification, and mastering your craft.",
    "saturn_conjunction_lilith": "<b>Saturn Conjunct Lilith:</b> The friction between absolute control and untamed, taboo desire is immense here. You may ruthlessly suppress your darkest urges, only to find that attempting to cage Lilith makes her terrifyingly powerful.",
    
    // ----------------------------------------------------
    // URANUS
    // ----------------------------------------------------
    "uranus_conjunction_chiron": "<b>Uranus Conjunct Chiron:</b> Your deepest wounds involve sudden shocks, alienation, or feeling fundamentally 'different.' Your healing journey is wildly unconventional, often involving radical awakenings or innovative, alternative therapies.",
    "uranus_square_chiron": "<b>Uranus Square Chiron:</b> There is volatile tension between your need for radical freedom and your deepest insecurities. You may sabotage your own healing through sudden rebellions, challenging you to integrate your uniqueness without tearing everything down.",
    "uranus_trine_chiron": "<b>Uranus Trine Chiron:</b> You possess a brilliant, intuitive understanding of trauma and healing. You easily implement radically innovative solutions to old pain, acting as a lightning rod for the awakening of others.",
    "uranus_opposition_chiron": "<b>Uranus Opposite Chiron:</b> You often experience sudden, disruptive events that trigger old wounds. Healing requires you to stop fighting the chaos and realize that unexpected changes are simply clearing the path for a new, truer self.",

    "uranus_conjunction_north_node": "<b>Uranus Conjunct North Node:</b> Your evolutionary path requires absolute authenticity, rebellion, and innovation. You are meant to break the rules, shock the collective, and pioneer entirely new ways of being.",
    "uranus_conjunction_south_node": "<b>Uranus Conjunct South Node:</b> You carry a karmic history of instability, rebellion, or genius. Your challenge is to stop relying on shock value or chaotic detachment, learning instead to ground your brilliance into a steady path forward.",
    "uranus_square_north_node": "<b>Uranus Square the Nodes:</b> Your chaotic need for independence frequently derails your karmic progress. You must learn the difference between authentic innovation and pointless rebellion if you wish to evolve.",
    "uranus_trine_north_node": "<b>Uranus Trine North Node:</b> Sudden insights and unexpected opportunities naturally propel you along your soul's journey. Your radical authenticity effortlessly attracts the people and situations you need to grow.",

    "uranus_conjunction_ceres": "<b>Uranus Conjunct Ceres:</b> Your approach to nurturing is highly unconventional. You provide for others by giving them absolute freedom and radical acceptance, redefining what 'family' and 'care' mean on your own terms.",
    "uranus_square_ceres": "<b>Uranus Square Ceres:</b> There is erratic friction between your need to nurture and your desperate need for space. You may provide care inconsistently or chaotically, challenging you to create a stable environment that still allows for autonomy.",
    "uranus_trine_ceres": "<b>Uranus Trine Ceres:</b> You effortlessly combine caretaking with brilliant innovation. You easily invent new, liberating ways to support your loved ones, acting as a progressive and utterly accepting guardian.",
    "uranus_opposition_ceres": "<b>Uranus Opposite Ceres:</b> You often feel that the demands of family violently oppose your personal freedom. You must learn that you can be profoundly devoted without having to sacrifice your radical individuality.",

    "uranus_conjunction_pallas": "<b>Uranus Conjunct Pallas:</b> Your strategic intelligence is pure genius. You possess a lightning-fast, innovative mind capable of paradigm-shifting insights, easily solving problems in ways no one else would ever consider.",
    "uranus_square_pallas": "<b>Uranus Square Pallas:</b> Your brilliant ideas are often too chaotic or far ahead of their time to be implemented smoothly. You are challenged to ground your erratic genius in workable, step-by-step strategies.",
    "uranus_trine_pallas": "<b>Uranus Trine Pallas:</b> Your radical ideas flow seamlessly into brilliant tactical execution. You naturally perceive the unseen patterns in any system, allowing you to innovate with breathtaking ease and effectiveness.",
    "uranus_opposition_pallas": "<b>Uranus Opposite Pallas:</b> You frequently encounter resistance from established systems when you present your strategies. True brilliance requires learning how to introduce revolutionary ideas without alienating the people who need them.",

    "uranus_conjunction_juno": "<b>Uranus Conjunct Juno:</b> You demand absolute freedom within your commitments. Your approach to marriage or partnership is radically unconventional, requiring a bond that breaks traditional rules and constantly reinvents itself.",
    "uranus_square_juno": "<b>Uranus Square Juno:</b> The traditional expectations of commitment fiercely clash with your need for independence. You may experience sudden breakups or volatile relationships until you negotiate a partnership built on total autonomy.",
    "uranus_trine_juno": "<b>Uranus Trine Juno:</b> You effortlessly maintain an exciting, dynamic partnership. Your relationships thrive on mutual freedom and unexpected adventures, ensuring that your commitments never grow stale.",
    "uranus_opposition_juno": "<b>Uranus Opposite Juno:</b> You may attract erratic or unreliable partners, or project your own fear of entrapment onto the relationship. Finding equilibrium means accepting that a true equal will never ask you to surrender your freedom.",

    "uranus_conjunction_vesta": "<b>Uranus Conjunct Vesta:</b> Your spiritual devotion is completely unorthodox. You focus your intense inner flame on revolutionary ideas or technology, dedicating your life to awakening the collective in radically new ways.",
    "uranus_square_vesta": "<b>Uranus Square Vesta:</b> Your erratic energy often disrupts your ability to focus deeply. You may experience sudden burnout or abruptly abandon your sacred work, challenging you to find stability within your eccentric passions.",
    "uranus_trine_vesta": "<b>Uranus Trine Vesta:</b> Your unique genius naturally supports your most intense devotions. You effortlessly maintain a powerful, innovative focus, allowing you to pioneer new spiritual or intellectual paths.",
    "uranus_opposition_vesta": "<b>Uranus Opposite Vesta:</b> You frequently feel torn between shocking the outside world and retreating into your solitary, sacred work. You must learn to alternate between the chaos of innovation and the silence of the flame.",

    "uranus_conjunction_ascendant": "<b>Uranus Conjunct Ascendant:</b> You possess an electric, highly eccentric, and unforgettable presence. You meet the world as a radical individual, unafraid to shock people and utterly uninterested in blending in.",
    "uranus_square_ascendant": "<b>Uranus Square Ascendant:</b> Your need to rebel frequently clashes with how you are perceived. You might come across as erratic or needlessly contrarian, challenging you to express your uniqueness without unnecessary provocation.",
    "uranus_trine_ascendant": "<b>Uranus Trine Ascendant:</b> Your eccentricities flow effortlessly into a charming, magnetic persona. People are naturally drawn to your authenticity and the exciting, unpredictable energy you bring to any room.",
    "uranus_opposition_ascendant": "<b>Uranus Opposite Ascendant (Conjunct Descendant):</b> You attract brilliant, unconventional, or highly erratic partners. You seek relationships that shock you out of your comfort zone, learning about your own need for freedom through your mates.",

    "uranus_conjunction_mc": "<b>Uranus Conjunct MC:</b> Your career path is highly unconventional, marked by sudden changes in direction and radical innovation. You are known publicly as a rebel, a genius, or an eccentric who refuses to follow the rules of your industry.",
    "uranus_square_mc": "<b>Uranus Square MC:</b> Your erratic behavior or sudden rebellions frequently disrupt your professional stability. You must learn to innovate without destroying your own reputation or needlessly antagonizing authority.",
    "uranus_trine_mc": "<b>Uranus Trine MC:</b> Your unique brilliance naturally propels your career forward. You easily carve out an innovative, highly individualized professional path, gaining public recognition for your originality.",
    "uranus_opposition_mc": "<b>Uranus Opposite MC (Conjunct IC):</b> Your home and private life are highly unconventional or subject to sudden changes. You require absolute freedom within your sanctuary and may have a highly unusual family dynamic.",

    "uranus_conjunction_fortune": "<b>Uranus Conjunct Part of Fortune:</b> Your greatest success and joy arrive through sudden, unexpected lightning strikes of luck. You prosper most when you completely abandon tradition and embrace radical authenticity.",
    "uranus_conjunction_lilith": "<b>Uranus Conjunct Lilith:</b> Your repressed desires are explosive and totally taboo. When you finally unleash your raw power, it arrives like a lightning bolt, shattering any oppressive structures that tried to contain you."
};

Object.assign(data.aspects, newAspects);
fs.writeFileSync(path, JSON.stringify(data, null, 2));
console.log("Added outer planets (Jupiter, Saturn, Uranus) aspects to extra_bodies.json.");
