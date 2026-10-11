package com.music.dhvani.data.model

import java.util.Locale

/**
 * Intelligent language detection and classification for Indian & international music.
 * Used for search badge pills, filter chips, and strict source matching (preventing
 * incorrect language audio switches between YouTube and JioSaavn / Lossless).
 */
object SongLanguage {

    private val EXPLICIT_LANG_REGEX = listOf(
        Regex("""(?i)\b(?:hindi|हिन्दी|हिंदी)\b""") to "Hindi",
        Regex("""(?i)\b(?:telugu|తెలుగు)\b""") to "Telugu",
        Regex("""(?i)\b(?:tamil|தமிழ்)\b""") to "Tamil",
        Regex("""(?i)\b(?:kannada|ಕನ್ನಡ)\b""") to "Kannada",
        Regex("""(?i)\b(?:malayalam|മലയാളം)\b""") to "Malayalam",
        Regex("""(?i)\b(?:punjabi|ਪੰਜਾਬੀ)\b""") to "Punjabi",
        Regex("""(?i)\b(?:bhojpuri|भोजपुरी)\b""") to "Bhojpuri",
        Regex("""(?i)\b(?:bengali|bangla|বাংলা)\b""") to "Bengali",
        Regex("""(?i)\b(?:marathi|मराठी)\b""") to "Marathi",
        Regex("""(?i)\b(?:gujarati|ગુજરાતી)\b""") to "Gujarati",
        Regex("""(?i)\b(?:odia|oriya|ଓଡ଼ିଆ)\b""") to "Odia",
        Regex("""(?i)\b(?:urdu|اردو)\b""") to "Urdu",
        Regex("""(?i)\b(?:spanish|español|espanol|castellano)\b""") to "Spanish",
        Regex("""(?i)\b(?:portuguese|português|portugues)\b""") to "Portuguese",
        Regex("""(?i)\b(?:french|français|francais)\b""") to "French",
        Regex("""(?i)\b(?:german|deutsch)\b""") to "German",
        Regex("""(?i)\b(?:italian|italiano)\b""") to "Italian",
        Regex("""(?i)\b(?:korean|한국어)\b""") to "Korean",
        Regex("""(?i)\b(?:japanese|日本語)\b""") to "Japanese",
        Regex("""(?i)\b(?:chinese|mandarin|cantonese|中文)\b""") to "Chinese",
        Regex("""(?i)\b(?:russian|русский)\b""") to "Russian",
        Regex("""(?i)\b(?:arabic|عربي)\b""") to "Arabic",
        Regex("""(?i)\b(?:turkish|türkçe|turkce)\b""") to "Turkish",
        Regex("""(?i)\b(?:english)\b""") to "English",
    )

    // Pan-Indian multi-language song titles (where the title word itself denotes the dub/version)
    private val TITLE_KEYWORDS_HINDI = listOf(
        "naacho naacho", "oo bolega", "chaleya", "zinda banda", "angaaron",
        "dheere dheere", "tu hai champion", "jiyo re bahubali", "jal rahin hain",
        "kaun hain voh", "kesariya", "tum hi ho", "channa mereya", "raataan lambiyan",
        "apna bana le", "o maahi", "lut gaye", "kalank", "shayad", "tera ban jaunga",
        "bekhayali", "ghungroo", "hawayein", "gerua", "zaalima", "deva deva",
        "besharam rang", "jhoome jo pathaan", "satranga", "pehle bhi main", "tauba tauba",
    )

    private val TITLE_KEYWORDS_TELUGU = listOf(
        "naatu naatu", "oo antava", "chalona", "dhumme dhulipelaa", "sooseki",
        "chuttamalle", "daavudi", "kaavalayya", "saahore baahubali", "dhivara",
        "sivuni aana", "kumkumala", "butta bomma", "samajavaragamana", "ramuloo ramulaa",
        "saranga dariya", "kalaavathi", "kurchi madathapetti", "dum masala", "guntur kaaram",
        "tillu anna", "dj tillu", "komuram bheemudo", "manohari",
    )

    private val TITLE_KEYWORDS_TAMIL = listOf(
        "naattu koothu", "oo solriya", "hayyoda", "vandha edam", "koodamela",
        "paththavaikkum", "kaavaalaa", "kaavalaa", "hukum", "theethiriyaai",
        "naa ready", "badass", "ordinary person", "arabic kuthu", "halamithi habibo",
        "jolly o gymkhana", "vaathi coming", "rowdy baby", "enjoy enjaami",
        "marakkuma nenjam", "mallipoo", "megam karukatha", "ranjithame",
        "pathala pathala", "kutty pattalam", "naatu padaigal", "nee vaadi", "dheerane",
    )

    private val TITLE_KEYWORDS_KANNADA = listOf(
        "halli naatu", "haali naatu", "oo anthiya", "kesariya rangu",
        "shokiwala", "naatu kodi nuvvu", "singara siriye", "varaha roopam",
        "belakina kavithe", "salaam rocky bhai",
    )

    private val TITLE_KEYWORDS_MALAYALAM = listOf(
        "karinthol", "oo chollunno", "kunkumamaake", "illuminati",
        "kuthanthram", "neela nilave", "aavesham",
    )

    private val TITLE_KEYWORDS_SPANISH = listOf(
        "despacito", "bailando", "gasolina", "danza kuduro", "suerte",
        "libre soy", "cuán lejos voy", "cuan lejos voy", "un mundo ideal", "recuérdame",
        "recuerdame", "no se habla de bruno", "subeme la radio", "el perdón", "el perdon",
        "felices los 4", "calma", "tusa", "con calma", "echame la culpa",
    )

    // Dedicated regional artists whose discography is overwhelmingly tied to a single language
    private val ARTISTS_HINDI = listOf(
        "vishal mishra", "arijit singh", "neha kakkar", "jubin nautiyal", "darshan raval",
        "badshah", "kumar sanu", "udit narayan", "alka yagnik", "sonu nigam",
        "mohit chauhan", "pritam", "amit trivedi", "sunidhi chauhan", "neeti mohan",
        "javed ali", "shaan", "mika singh", "lucky ali", "honey singh", "tony kakkar",
        "anuv jain", "prateek kuhad", "jasleen royal", "stebin ben", "shilpa rao",
        "palak muchhal", "tulsi kumar", "dhvani bhanushali", "lata mangeshkar",
        "kishore kumar", "mohammed rafi", "asha bhosle", "jagjit singh", "pankaj udhas",
        "atif aslam", "shreya ghoshal", "kk", "armaan malik", "sachet tandon",
        "sachet-parampara", "parampara tandon", "mithoon", "tanishk bagchi", "papon",
        "yo yo honey singh", "kanika kapoor", "monali thakur", "shankar mahadevan",
        "hariharan", "ayushmann khurrana", "b praak", "jaani", "himesh reshammiya",
        "rochak kohli", "meet bros", "kavita krishnamurthy", "anuradha paudwal",
    )

    private val ARTISTS_TELUGU = listOf(
        "rahul sipligunj", "kaala bhairava", "anurag kulkarni", "ram miriyala", "mangli",
        "s.p. balasubrahmanyam", "devi sri prasad", "s. thaman", "m.m. keeravaani",
        "geetha madhuri", "sunitha", "hemachandra", "revanth", "sri krishna", "roll rida",
        "sid sriram", "chitra", "k.s. chithra", "mano", "penchal das",
    )

    private val ARTISTS_TAMIL = listOf(
        "anirudh ravichander", "yuvan shankar raja", "harris jayaraj", "d. imman",
        "santhosh narayanan", "g.v. prakash", "vijay antony", "dhanush", "pradeep kumar",
        "sean roldan", "yazin nizar", "dhee", "arivu", "andrea jeremiah",
        "sriram parthasarathy", "naresh iyer", "ilaiyaraaja", "james vasanthan",
        "ar rahman", "a.r. rahman",
    )

    private val ARTISTS_ENGLISH = listOf(
        "taylor swift", "ed sheeran", "the weeknd", "drake", "billie eilish",
        "dua lipa", "ariana grande", "justin bieber", "post malone", "bruno mars",
        "coldplay", "eminem", "rihanna", "adele", "maroon 5", "imagine dragons",
        "shawn mendes", "charlie puth", "alan walker", "chainsmokers", "marshmello",
    )

    private val ARTISTS_PUNJABI = listOf(
        "sidhu moose wala", "diljit dosanjh", "karan aujla", "ap dhillon", "b praak",
        "shubh", "gurinder gill", "amrit maan", "ammy virk", "jassie gill", "hardy sandhu",
        "garry sandhu", "guru randhawa", "mankirt aulakh", "jordan sandhu", "jass manak",
        "kaka", "kulwinder billa", "parmish verma", "bohemia",
    )

    private val ARTISTS_KANNADA = listOf(
        "sanjith hegde", "vijay prakash", "arjun janya", "charan raj", "ravi basrur",
        "v. harikrishna", "raghu dixit", "chandan shetty", "sridhar v sambhram",
    )

    private val ARTISTS_MALAYALAM = listOf(
        "job kurian", "sushin shyam", "jassie gift", "vineeth sreenivasan", "k.s. harisankar",
        "sithara krishnakumar", "hesham abdul wahab", "gopi sundar", "shaan rahman", "dabzee",
    )

    private val ARTISTS_BHOJPURI = listOf(
        "khesari lal yadav", "pawan singh", "silpi raj", "ritesh pandey",
        "arvind akela kallu", "neelkamal singh", "ankush raja", "pramod premi yadav",
    )

    private val ARTISTS_GUJARATI = listOf(
        "geeta rabari", "kinjal dave", "kirtidan gadhvi", "jignesh kaviraj", "osman mir", "aditya gadhvi",
    )

    private val ARTISTS_MARATHI = listOf(
        "ajay-atul", "swapnil bandodkar", "avadhoot gupte", "adarsh shinde", "mahesh kale",
    )

    private val ARTISTS_SPANISH = listOf(
        "shakira", "enrique iglesias", "luis fonsi", "bad bunny", "j balvin",
        "daddy yankee", "maluma", "rosalía", "rosalia", "ozuna", "anuel aa",
        "karol g", "ricky martin", "pitbull", "becky g", "nicky jam",
        "sebastian yatra", "rauw alejandro", "camila cabello", "don omar",
    )

    private val ARTISTS_KOREAN = listOf(
        "bts", "blackpink", "twice", "stray kids", "exo", "newjeans",
        "txt", "seventeen", "aespa", "itzy", "red velvet", "iu", "jungkook",
    )

    private val ARTISTS_JAPANESE = listOf(
        "yoasobi", "kenshi yonezu", "lisa", "ado", "eve", "radwimps",
        "aimer", "one ok rock", "vaundy", "official hige dandism", "fujii kaze",
    )

    /**
     * Primary detector that checks:
     * 1. Explicit keyword tags (e.g. "Hindi", "Telugu", "Tamil") in title, artist, or album.
     * 2. Unicode scripts (Telugu, Tamil, Kannada, Malayalam, Devanagari, Gurmukhi, Japanese, etc.).
     * 3. Pan-Indian translated titles (e.g. "Naacho Naacho" = Hindi, "Naatu Naatu" = Telugu, "Naattu Koothu" = Tamil).
     * 4. Dedicated regional artists (e.g. Vishal Mishra = Hindi, Rahul Sipligunj = Telugu, Anirudh = Tamil).
     * 5. Regional movie / album names (e.g. Shokiwala = Kannada, Kutty Pattalam = Tamil).
     */
    fun detect(title: String, artist: String? = null, albumName: String? = null): String? {
        val fullText = buildString {
            append(title)
            if (!artist.isNullOrBlank()) append(" ").append(artist)
            if (!albumName.isNullOrBlank()) append(" ").append(albumName)
        }

        // 1. Explicit language keywords in text
        for ((regex, lang) in EXPLICIT_LANG_REGEX) {
            if (regex.containsMatchIn(fullText)) return lang
        }

        // 2. Unicode script detection
        detectFromScript(fullText)?.let { return it }

        // 3. Pan-Indian known translated titles & international dubs
        val lowerTitle = title.lowercase(Locale.ROOT)
        when {
            TITLE_KEYWORDS_HINDI.any { lowerTitle.contains(it) } -> return "Hindi"
            TITLE_KEYWORDS_TELUGU.any { lowerTitle.contains(it) } -> return "Telugu"
            TITLE_KEYWORDS_TAMIL.any { lowerTitle.contains(it) } -> return "Tamil"
            TITLE_KEYWORDS_KANNADA.any { lowerTitle.contains(it) } -> return "Kannada"
            TITLE_KEYWORDS_MALAYALAM.any { lowerTitle.contains(it) } -> return "Malayalam"
            TITLE_KEYWORDS_SPANISH.any { lowerTitle.contains(it) } -> return "Spanish"
        }

        // 4. Dedicated regional and international artist indicators
        if (!artist.isNullOrBlank()) {
            val lowerArtist = artist.lowercase(Locale.ROOT)
            when {
                ARTISTS_HINDI.any { lowerArtist.contains(it) } -> return "Hindi"
                ARTISTS_TELUGU.any { lowerArtist.contains(it) } -> return "Telugu"
                ARTISTS_TAMIL.any { lowerArtist.contains(it) } -> return "Tamil"
                ARTISTS_PUNJABI.any { lowerArtist.contains(it) } -> return "Punjabi"
                ARTISTS_KANNADA.any { lowerArtist.contains(it) } -> return "Kannada"
                ARTISTS_MALAYALAM.any { lowerArtist.contains(it) } -> return "Malayalam"
                ARTISTS_BHOJPURI.any { lowerArtist.contains(it) } -> return "Bhojpuri"
                ARTISTS_GUJARATI.any { lowerArtist.contains(it) } -> return "Gujarati"
                ARTISTS_MARATHI.any { lowerArtist.contains(it) } -> return "Marathi"
                ARTISTS_SPANISH.any { lowerArtist.contains(it) } -> return "Spanish"
                ARTISTS_KOREAN.any { lowerArtist.contains(it) } -> return "Korean"
                ARTISTS_JAPANESE.any { lowerArtist.contains(it) } -> return "Japanese"
                ARTISTS_ENGLISH.any { lowerArtist.contains(it) } -> return "English"
            }
        }

        // 5. Regional album / movie cues
        if (!albumName.isNullOrBlank()) {
            val lowerAlbum = albumName.lowercase(Locale.ROOT)
            when {
                lowerAlbum.contains("shokiwala") -> return "Kannada"
                lowerAlbum.contains("kutty pattalam") || lowerAlbum.contains("varisu") ||
                    lowerAlbum.contains("thunivu") || lowerAlbum.contains("vikram") ||
                    lowerAlbum.contains("leo") || lowerAlbum.contains("beast") -> return "Tamil"
                lowerAlbum.contains("vaarasudu") || lowerAlbum.contains("thegimpu") ||
                    lowerAlbum.contains("guntur kaaram") -> return "Telugu"
            }
        }

        return null
    }

    private fun detectFromScript(text: String): String? {
        for (ch in text) {
            when (ch.code) {
                in 0x0C00..0x0C7F -> return "Telugu"
                in 0x0B80..0x0BFF -> return "Tamil"
                in 0x0C80..0x0CFF -> return "Kannada"
                in 0x0D00..0x0D7F -> return "Malayalam"
                in 0x0A00..0x0A7F -> return "Punjabi"
                in 0x0A80..0x0AFF -> return "Gujarati"
                in 0x0980..0x09FF -> return "Bengali"
                in 0x0B00..0x0B7F -> return "Odia"
                in 0x0600..0x06FF -> return "Urdu"
                in 0x3040..0x309F, in 0x30A0..0x30FF -> return "Japanese"
                in 0xAC00..0xD7AF -> return "Korean"
                in 0x0900..0x097F -> return "Hindi"
            }
        }
        return null
    }

    val SONG_FAMILIES: List<List<String>> = listOf(
        listOf("naatu naatu", "naacho naacho", "naattu koothu", "haali naatu", "halli naatu", "karinthol"),
        listOf("oo antava", "oo bolega", "oo solriya", "oo anthiya", "oo chollunno"),
        listOf("chaleya", "hayyoda", "chalona"),
        listOf("zinda banda", "vandha edam", "dhumme dhulipelaa"),
        listOf("chuttamalle", "dheere dheere", "paththavaikkum"),
        listOf("angaaron", "sooseki", "koodamela"),
        listOf("kaavaalaa", "kaavalaa", "kaavalayya", "tu hai champion"),
        listOf("saahore baahubali", "jiyo re bahubali"),
        listOf("dhivara", "jal rahin hain", "dheerane"),
        listOf("sivuni aana", "kaun hain voh"),
        listOf("kesariya", "kumkumala", "theethiriyaai", "kesariya rangu", "kunkumamaake"),
        listOf("satranga", "nee vaadi", "ney veyrey"),
        listOf("arabic kuthu", "halamithi habibo"),
        listOf("srivalli"),
        listOf("pushpa pushpa"),
        listOf("saami saami"),
        listOf("fear song"),
        listOf("daavudi"),
        listOf("kissik"),
        listOf("peelings"),
        listOf("naa ready"),
        listOf("hukum"),
        listOf("butta bomma"),
        listOf("samajavaragamana"),
        listOf("despacito"),
        listOf("bailando"),
        listOf("waka waka", "esto es áfrica", "esto es africa"),
        listOf("hips don't lie", "será será", "sera sera"),
        listOf("whenever wherever", "suerte"),
        listOf("let it go", "libre soy", "libérée délivrée", "liberee delivree"),
        listOf("how far i'll go", "cuán lejos voy", "cuan lejos voy"),
        listOf("a whole new world", "un mundo ideal"),
        listOf("remember me", "recuérdame", "recuerdame"),
        listOf("we don't talk about bruno", "no se habla de bruno"),
        listOf("el perdón", "forgiveness", "el perdon"),
        listOf("subeme la radio"),
        listOf("danza kuduro", "dança kuduro"),
    )

    fun getCrossLanguageFamily(title: String): List<String>? {
        val lower = title.lowercase(Locale.ROOT)
        return SONG_FAMILIES.firstOrNull { family ->
            family.any { lower.contains(it) }
        }
    }
}

/** Convenience top-level function for detecting language from song metadata. */
fun detectSongLanguage(title: String, artist: String? = null, albumName: String? = null): String? =
    SongLanguage.detect(title, artist, albumName)
