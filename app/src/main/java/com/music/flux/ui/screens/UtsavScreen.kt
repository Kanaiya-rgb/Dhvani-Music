package com.music.flux.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.flux.data.YtMusicRepository
import com.music.flux.data.model.BrowseItem
import com.music.flux.data.model.BrowseType
import com.music.flux.data.model.SearchFilter
import com.music.flux.data.model.SearchResult
import com.music.flux.data.model.Song
import com.music.flux.ui.components.PAGE_GUTTER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

private val SaffronPrimary = Color(0xFFFF7A29)
private val SaffronDark = Color(0xFFC85108)
private val CardSurface = Color(0xFF1C1F24)
private val CardBorder = Color(0xFF2E323A)
private val GoldAccent = Color(0xFFFFB800)

/**
 * Seasonal Indian Festival descriptor with full multi-language support (English, Hindi, Hinglish, Punjabi).
 */
data class FestivalSeason(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val titleHinglish: String,
    val titlePa: String,
    val rituEn: String,
    val rituHi: String,
    val lunarPhaseEn: String,
    val lunarPhaseHi: String,
    val heroQuery: String,
    val heroBadgeEn: String,
    val heroBadgeHi: String,
    val heroSubtitleEn: String,
    val heroSubtitleHi: String,
    val heroSubtitleHinglish: String,
    val carousel1TitleEn: String,
    val carousel1TitleHi: String,
    val carousel1SubtitleEn: String,
    val carousel1SubtitleHi: String,
    val carousel1Query: String,
    val carousel2TitleEn: String,
    val carousel2TitleHi: String,
    val carousel2SubtitleEn: String,
    val carousel2SubtitleHi: String,
    val carousel2Query: String,
    val playlistQuery: String,
) {
    fun title(locale: String): String = when {
        locale.startsWith("hi-Latn", ignoreCase = true) || locale.equals("hinglish", ignoreCase = true) -> titleHinglish
        locale.startsWith("hi", ignoreCase = true) -> titleHi
        locale.startsWith("pa", ignoreCase = true) -> titlePa
        else -> titleEn
    }

    fun ritu(locale: String): String = when {
        locale.startsWith("hi", ignoreCase = true) && !locale.startsWith("hi-Latn", ignoreCase = true) -> rituHi
        else -> rituEn
    }

    fun lunar(locale: String): String = when {
        locale.startsWith("hi", ignoreCase = true) && !locale.startsWith("hi-Latn", ignoreCase = true) -> lunarPhaseHi
        else -> lunarPhaseEn
    }

    fun heroBadge(locale: String): String = when {
        locale.startsWith("hi", ignoreCase = true) && !locale.startsWith("hi-Latn", ignoreCase = true) -> heroBadgeHi
        else -> heroBadgeEn
    }

    fun heroSubtitle(locale: String): String = when {
        locale.startsWith("hi-Latn", ignoreCase = true) || locale.equals("hinglish", ignoreCase = true) -> heroSubtitleHinglish
        locale.startsWith("hi", ignoreCase = true) -> heroSubtitleHi
        else -> heroSubtitleEn
    }

    fun c1Title(locale: String): String = when {
        locale.startsWith("hi", ignoreCase = true) && !locale.startsWith("hi-Latn", ignoreCase = true) -> carousel1TitleHi
        else -> carousel1TitleEn
    }

    fun c1Sub(locale: String): String = when {
        locale.startsWith("hi", ignoreCase = true) && !locale.startsWith("hi-Latn", ignoreCase = true) -> carousel1SubtitleHi
        else -> carousel1SubtitleEn
    }

    fun c2Title(locale: String): String = when {
        locale.startsWith("hi", ignoreCase = true) && !locale.startsWith("hi-Latn", ignoreCase = true) -> carousel2TitleHi
        else -> carousel2TitleEn
    }

    fun c2Sub(locale: String): String = when {
        locale.startsWith("hi", ignoreCase = true) && !locale.startsWith("hi-Latn", ignoreCase = true) -> carousel2SubtitleHi
        else -> carousel2SubtitleEn
    }
}

object FestivalCatalog {
    val GANESH_UTSAV = FestivalSeason(
        id = "ganesh_utsav",
        titleEn = "Ganesh Utsav Mahotsav",
        titleHi = "गणेशोत्सव महापर्व",
        titleHinglish = "Ganesh Utsav Mahotsav",
        titlePa = "ਗਣੇਸ਼ ਉਤਸਵ ਮਹਾਪਰਵ",
        rituEn = "Autumn Season • Sharad Ritu",
        rituHi = "शरद ऋतु • Autumn",
        lunarPhaseEn = "Bhadrapada Month • Anant Chaturdashi",
        lunarPhaseHi = "भाद्रपद मास • अनंत चतुर्दशी",
        heroQuery = "Ganesh Chaturthi Utsav songs",
        heroBadgeEn = "✦ Festive Special • Deva Shree Ganesha",
        heroBadgeHi = "✦ उत्सव विशेष • देवा श्री गणेशा",
        heroSubtitleEn = "Dhol Tasha Pathaks, Sukhkarta Dukhharta Aarti, and Lalbaugcha Raja divine music",
        heroSubtitleHi = "ढोल ताशा पथक, सुखकर्ता दुखहर्ता महाआरती एवं लालबागचा राजा पावन दर्शन संगीत",
        heroSubtitleHinglish = "Dhol Tasha Pathak, Sukhkarta Dukhharta Maha Aarti aur Lalbaugcha Raja music",
        carousel1TitleEn = "Sukhkarta Dukhharta • Aarti & Hymns",
        carousel1TitleHi = "सुखकर्ता दुखहर्ता • महाआरती व भजन",
        carousel1SubtitleEn = "Traditional live Aartis and devotional bhajans",
        carousel1SubtitleHi = "लाइव रिकॉर्डिंग्स एवं पारंपरिक आरती",
        carousel1Query = "Ganesh Aarti Sukhkarta Dukhharta Shankar Mahadevan",
        carousel2TitleEn = "Pune & Mumbai Dhol Tasha Beats",
        carousel2TitleHi = "पुणे व मुंबई ढोल ताशा पथक",
        carousel2SubtitleEn = "High-energy festival beats and celebration rhythms",
        carousel2SubtitleHi = "उच्च ऊर्जा लोक संगीत एवं उत्सव धुन",
        carousel2Query = "Ganesh Dhol Tasha Pathak beats",
        playlistQuery = "Ganesh Chaturthi festival songs playlist",
    )

    val SHARAD_NAVRATRI = FestivalSeason(
        id = "navratri",
        titleEn = "Navratri Garba & Durga Puja",
        titleHi = "शारदीय नवरात्रि एवं दुर्गा पूजा",
        titleHinglish = "Navratri Garba & Durga Puja",
        titlePa = "ਨਰਾਤੇ ਗਰਬਾ ਅਤੇ ਦੁਰਗਾ ਪੂਜਾ",
        rituEn = "Autumn Season • Sharad Ritu",
        rituHi = "शरद ऋतु • Autumn",
        lunarPhaseEn = "Ashwin Month • Navratri",
        lunarPhaseHi = "आश्विन मास • नवरात्र",
        heroQuery = "Navratri Garba hits Falguni Pathak",
        heroBadgeEn = "✦ Dandiya Raas • Garba Festival",
        heroBadgeHi = "✦ डांडिया रास • गरबा उत्सव",
        heroSubtitleEn = "Falguni Pathak, Amit Trivedi, Dhol Beats, and Maa Ambe Aarti",
        heroSubtitleHi = "फाल्गुनी पाठक, अमित त्रिवेदी, ढोल एवं मां अम्बे महाआरती",
        heroSubtitleHinglish = "Falguni Pathak, Amit Trivedi, Dhol beats aur Maa Ambe Aarti",
        carousel1TitleEn = "Garba & Dandiya Non-Stop",
        carousel1TitleHi = "गरबा एवं डांडिया नॉनस्टॉप",
        carousel1SubtitleEn = "Gujarati & Bollywood Garba folk anthems",
        carousel1SubtitleHi = "गुजराती एवं बॉलीवुड गरबा धमाल",
        carousel1Query = "Navratri Garba nonstop hits Gujarati Hindi",
        carousel2TitleEn = "Durga Puja Dhaak Beats & Aarti",
        carousel2TitleHi = "दुर्गा पूजा ढाक एवं महाआरती",
        carousel2SubtitleEn = "Kolkata Agomoni, Dhaak beats & Shankh dhun",
        carousel2SubtitleHi = "कोलकाता अगोमनी, ढाक एवं शंख ध्वनि",
        carousel2Query = "Durga Puja Dhaak beats Agomoni",
        playlistQuery = "Navratri Garba Dandiya playlist",
    )

    val DIWALI = FestivalSeason(
        id = "diwali",
        titleEn = "Deepawali Mahotsav",
        titleHi = "दीपावली महापर्व",
        titleHinglish = "Deepawali Mahotsav",
        titlePa = "ਦੀਵਾਲੀ ਮਹਾਪਰਵ",
        rituEn = "Pre-Winter • Hemant Season",
        rituHi = "हेमंत ऋतु • Pre-Winter",
        lunarPhaseEn = "Kartik Month • Amavasya",
        lunarPhaseHi = "कार्तिक मास • अमावस्या",
        heroQuery = "Diwali Laxmi Pujan Aarti festive songs",
        heroBadgeEn = "✦ Festival of Lights • Deepawali",
        heroBadgeHi = "✦ प्रकाश पर्व • शुभ दीपावली",
        heroSubtitleEn = "Maha Lakshmi Pujan, Omkar Aarti, and soulful classical illumination ragas",
        heroSubtitleHi = "महालक्ष्मी पूजन, ओंकार आरती एवं घर-आँगन सजाने वाले मधुर शास्त्रीय राग",
        heroSubtitleHinglish = "Maha Lakshmi Pujan, Omkar Aarti aur classical illumination ragas",
        carousel1TitleEn = "Maha Lakshmi Pujan & Aarti",
        carousel1TitleHi = "महालक्ष्मी पूजन एवं अमृत आरती",
        carousel1SubtitleEn = "Vedic chants and midnight temple bells",
        carousel1SubtitleHi = "वैदिक मंत्रोच्चार एवं शुभ आरती",
        carousel1Query = "Laxmi Aarti Anuradha Paudwal Diwali",
        carousel2TitleEn = "Happy Diwali Celebrations",
        carousel2TitleHi = "शुभ दीपावली आनंद संगीत",
        carousel2SubtitleEn = "Joyful family celebration songs",
        carousel2SubtitleHi = "उत्सव एवं मिलन के मधुर गीत",
        carousel2Query = "Happy Diwali festive songs celebrations",
        playlistQuery = "Diwali festive songs playlist",
    )

    val CHHATH_PUJA = FestivalSeason(
        id = "chhath",
        titleEn = "Chhath Puja Mahaparv",
        titleHi = "छठ महापर्व",
        titleHinglish = "Chhath Puja Mahaparv",
        titlePa = "ਛਠ ਪੂਜਾ",
        rituEn = "Pre-Winter • Hemant Season",
        rituHi = "हेमंत ऋतु • Pre-Winter",
        lunarPhaseEn = "Kartik Month • Shukla Shashthi",
        lunarPhaseHi = "कार्तिक मास • शुक्ल षष्ठी",
        heroQuery = "Chhath Puja Sharda Sinha geet",
        heroBadgeEn = "✦ Sun Worship • Chhathi Maiya",
        heroBadgeHi = "✦ सूर्य आराधना • छठी मइया",
        heroSubtitleEn = "Sharda Sinha, Anuradha Paudwal, Uga He Suruj Dev sacred riverside songs",
        heroSubtitleHi = "शारदा सिन्हा, अनुराधा पौडवाल, उग हे सुरुज देव पावन घाट संगीत",
        heroSubtitleHinglish = "Sharda Sinha, Anuradha Paudwal, Uga He Suruj Dev pawan geet",
        carousel1TitleEn = "Chhathi Maiya Folk Songs",
        carousel1TitleHi = "छठी मइया के पारंपरिक गीत",
        carousel1SubtitleEn = "Traditional folk melodies",
        carousel1SubtitleHi = "पारंपरिक लोक धुन",
        carousel1Query = "Chhath Puja classical traditional songs",
        carousel2TitleEn = "Surya Dev Arghya & Evening Prayers",
        carousel2TitleHi = "सूर्य देव अर्घ्य एवं संध्या वंदना",
        carousel2SubtitleEn = "Holy ghat evening atmosphere",
        carousel2SubtitleHi = "पावन घाट संध्या वंदना",
        carousel2Query = "Surya Dev Arghya Chhath geet",
        playlistQuery = "Chhath Puja songs playlist",
    )

    val MAKAR_SANKRANTI = FestivalSeason(
        id = "sankranti",
        titleEn = "Makar Sankranti & Lohri Utsav",
        titleHi = "मकर संक्रांति, लोहड़ी एवं पोंगल",
        titleHinglish = "Makar Sankranti & Lohri Utsav",
        titlePa = "ਮਕਰ ਸੰਕ੍ਰਾਂਤੀ ਅਤੇ ਲੋਹੜੀ",
        rituEn = "Winter • Shishir Season",
        rituHi = "शिशिर ऋतु • Winter",
        lunarPhaseEn = "Magha Month • Uttarayan",
        lunarPhaseHi = "माघ मास • सूर्य उत्तरायण",
        heroQuery = "Makar Sankranti Lohri festive songs",
        heroBadgeEn = "✦ Uttarayan • Kites & Lohri",
        heroBadgeHi = "✦ उत्तरायण • पतंग एवं लोहड़ी",
        heroSubtitleEn = "Sundar Mundariye, Til-Gur Sankranti, and Punjab-Gujarat folk beats",
        heroSubtitleHi = "सुंदर मुंदरिये हो, तिल-गुड़ संक्रांति एवं पंजाब-गुजरात लोक धुन",
        heroSubtitleHinglish = "Sundar Mundariye, Til-Gur Sankranti aur folk celebration beats",
        carousel1TitleEn = "Lohri Bhangra & Folk Songs",
        carousel1TitleHi = "लोहड़ी भांगड़ा एवं लोक संगीत",
        carousel1SubtitleEn = "Punjabi Dhol and campfire folk",
        carousel1SubtitleHi = "पंजाबी ढोल एवं अलाव संगीत",
        carousel1Query = "Lohri Punjabi folk songs Bhangra",
        carousel2TitleEn = "Uttarayan Kite Flying Hits",
        carousel2TitleHi = "उत्तरायण एवं पतंग बाज़ी संगीत",
        carousel2SubtitleEn = "Kai Po Che energetic melodies",
        carousel2SubtitleHi = "पतंगबाजी और उल्लास",
        carousel2Query = "Kai Po Che Makar Sankranti songs",
        playlistQuery = "Makar Sankranti Lohri playlist",
    )

    val MAHA_SHIVRATRI = FestivalSeason(
        id = "shivratri",
        titleEn = "Maha Shivratri Mahotsav",
        titleHi = "महाशिवरात्रि महोत्सव",
        titleHinglish = "Maha Shivratri Mahotsav",
        titlePa = "ਮਹਾਸ਼ਿਵਰਾਤਰੀ ਮਹੋਤਸਵ",
        rituEn = "Winter - Spring Season",
        rituHi = "शिशिर - वसंत ऋतु",
        lunarPhaseEn = "Phalguna Month • Krishna Chaturdashi",
        lunarPhaseHi = "फाल्गुन मास • कृष्ण चतुर्दशी",
        heroQuery = "Maha Shivratri bhajans Har Har Shambhu",
        heroBadgeEn = "✦ Lord of Lords • Shiva Bhakti",
        heroBadgeHi = "✦ देवाधिदेव महादेव • शिव आराधना",
        heroSubtitleEn = "Shiv Tandav Stotram, Mahamrityunjaya Mantra, and Mahakal Bhasma Aarti",
        heroSubtitleHi = "शिव तांडव स्तोत्रम्, महामृत्युंजय मंत्र एवं उज्जैन-काशी महाकाल भस्म आरती",
        heroSubtitleHinglish = "Shiv Tandav Stotram, Mahamrityunjaya Mantra aur Mahakal Bhasma Aarti",
        carousel1TitleEn = "Shiv Tandav & Sacred Chants",
        carousel1TitleHi = "शिव तांडव एवं महामृत्युंजय",
        carousel1SubtitleEn = "Shankar Mahadevan & classic recordings",
        carousel1SubtitleHi = "शंकर महादेवन एवं शास्त्रीय मंत्र",
        carousel1Query = "Shiv Tandav Stotram Shankar Mahadevan",
        carousel2TitleEn = "Har Har Mahadev Devotionals",
        carousel2TitleHi = "कैलाश वासी महादेव भजन",
        carousel2SubtitleEn = "Devotional melodies from the Himalayas",
        carousel2SubtitleHi = "हिमालयी भक्ति रचनाएं",
        carousel2Query = "Shiv bhajan Hansraj Raghuwanshi Jubin Nautiyal",
        playlistQuery = "Maha Shivratri bhajans playlist",
    )

    val HOLI = FestivalSeason(
        id = "holi",
        titleEn = "Holi Rangotsav & Braj Mahotsav",
        titleHi = "होली एवं ब्रज धाम रंगोत्सव",
        titleHinglish = "Holi Rangotsav & Braj Mahotsav",
        titlePa = "ਹੋਲੀ ਰੰਗੋਤਸਵ",
        rituEn = "Spring • Vasant Season",
        rituHi = "वसंत ऋतु • Spring",
        lunarPhaseEn = "Phalguna Purnima • Rangotsav",
        lunarPhaseHi = "फाल्गुन पूर्णिमा • रंगोत्सव",
        heroQuery = "Holi festive songs Rang barse",
        heroBadgeEn = "✦ Festival of Colors • Braj ki Holi",
        heroBadgeHi = "✦ रंगोत्सव • ब्रज की होली",
        heroSubtitleEn = "Mathura-Vrindavan Lathmar Holi, Rasiya, Rang Barse, and festive Bollywood hits",
        heroSubtitleHi = "मथुरा-वृंदावन लठमार होली, रसिया, रंग बरसे एवं क्लासिक बॉलीवुड धमाल",
        heroSubtitleHinglish = "Mathura Vrindavan Holi, Rasiya, Rang Barse aur Bollywood hits",
        carousel1TitleEn = "Braj Ki Holi & Rasiya",
        carousel1TitleHi = "ब्रज की होली एवं रसिया",
        carousel1SubtitleEn = "Authentic Vrindavan folk geet",
        carousel1SubtitleHi = "वृंदावन पावन लोक संगीत",
        carousel1Query = "Braj ki Holi Vrindavan folk geet",
        carousel2TitleEn = "Rang Barse • Holi Dance Party",
        carousel2TitleHi = "रंग बरसे • होली डांस पार्टी",
        carousel2SubtitleEn = "High-energy festival dance music",
        carousel2SubtitleHi = "उमंग और रंगों का उत्सव",
        carousel2Query = "Holi dance hits Rang Barse Balam Pichkari",
        playlistQuery = "Holi party songs playlist",
    )

    val RAM_NAVAMI = FestivalSeason(
        id = "ram_navami",
        titleEn = "Shri Ram Navami Mahotsav",
        titleHi = "श्री राम नवमी एवं चैत्र नवरात्र",
        titleHinglish = "Shri Ram Navami Mahotsav",
        titlePa = "ਸ਼੍ਰੀ ਰਾਮ ਨਵਮੀ",
        rituEn = "Spring Season",
        rituHi = "वसंत ऋतु • Spring",
        lunarPhaseEn = "Chaitra Month • Shukla Navami",
        lunarPhaseHi = "चैत्र मास • शुक्ल नवमी",
        heroQuery = "Shri Ram bhajans Ram Siya Ram",
        heroBadgeEn = "✦ Maryada Purushottam • Shri Ram",
        heroBadgeHi = "✦ मर्यादा पुरुषोत्तम • श्री राम",
        heroSubtitleEn = "Ram Siya Ram, Bhaye Pragat Kripala, Ayodhya Dham evening prayers",
        heroSubtitleHi = "राम सिया राम, भय प्रगट कृपाला, अयोध्या धाम संध्या आरती",
        heroSubtitleHinglish = "Ram Siya Ram, Bhaye Pragat Kripala aur Ayodhya Dham aarti",
        carousel1TitleEn = "Shri Ram Stuti & Amritwani",
        carousel1TitleHi = "श्री राम अमृतवाणी एवं स्तुति",
        carousel1SubtitleEn = "Revered verses and praises",
        carousel1SubtitleHi = "पावन स्तुति एवं वंदना",
        carousel1Query = "Shri Ram Stuti Bhaye Pragat Kripala",
        carousel2TitleEn = "Awadh Dham Mangal Gaan",
        carousel2TitleHi = "अवध धाम मंगल गान",
        carousel2SubtitleEn = "Ayodhya devotional hymns",
        carousel2SubtitleHi = "अयोध्या धाम मधुर भजन",
        carousel2Query = "Ayodhya Ram Mandir bhajans Mangal Bhavan",
        playlistQuery = "Ram Navami bhajans playlist",
    )

    val JANMASHTAMI = FestivalSeason(
        id = "janmashtami",
        titleEn = "Krishna Janmashtami Utsav",
        titleHi = "श्री कृष्ण जन्माष्टमी उत्सव",
        titleHinglish = "Krishna Janmashtami Utsav",
        titlePa = "ਸ਼੍ਰੀ ਕ੍ਰਿਸ਼ਨ ਜਨਮ ਅਸ਼ਟਮੀ",
        rituEn = "Monsoon - Autumn Season",
        rituHi = "वर्षा - शरद ऋतु",
        lunarPhaseEn = "Bhadrapada Month • Krishna Ashtami",
        lunarPhaseHi = "भाद्रपद मास • कृष्ण अष्टमी",
        heroQuery = "Krishna Janmashtami bhajans Achyutam Keshavam",
        heroBadgeEn = "✦ Natkhat Kanha • Makhan Chor",
        heroBadgeHi = "✦ नटखट कान्हा • माखनचोर",
        heroSubtitleEn = "Achyutam Keshavam, Govind Bolo Hari Gopal Bolo, and Dahi Handi anthems",
        heroSubtitleHi = "अच्युतम् केशवम्, गोविंद बोलो हरि गोपाल बोलो, दही हांडी उत्सव",
        heroSubtitleHinglish = "Achyutam Keshavam, Govind Bolo Hari Gopal Bolo aur Dahi Handi",
        carousel1TitleEn = "Govinda Aala Re • Dahi Handi",
        carousel1TitleHi = "गोविंदा आला रे • दही हांडी",
        carousel1SubtitleEn = "Celebration rhythms and dances",
        carousel1SubtitleHi = "दही हांडी उमंग संगीत",
        carousel1Query = "Govinda aala re Janmashtami songs",
        carousel2TitleEn = "Radha Krishna Divine Flute Bhajans",
        carousel2TitleHi = "राधा कृष्ण प्रेम रस भजन",
        carousel2SubtitleEn = "Serene flute and classical ragas",
        carousel2SubtitleHi = "बांसुरी एवं मधुर शास्त्रीय राग",
        carousel2Query = "Radha Krishna madhur bhajans flute",
        playlistQuery = "Krishna Janmashtami bhajans playlist",
    )

    val SAWAN_SHIV = FestivalSeason(
        id = "sawan",
        titleEn = "Pavitra Sawan Shiv Bhakti",
        titleHi = "पावन सावन मास एवं शिव आराधना",
        titleHinglish = "Pavitra Sawan Shiv Bhakti",
        titlePa = "ਸਾਵਣ ਸ਼ਿਵ ਭਗਤੀ",
        rituEn = "Monsoon • Varsha Season",
        rituHi = "वर्षा ऋतु • Monsoon",
        lunarPhaseEn = "Shravana Month • Somwar",
        lunarPhaseHi = "श्रावण मास • सोमवारी",
        heroQuery = "Sawan Shiv bhajan Har Har Mahadev",
        heroBadgeEn = "✦ Bol Bam • Shravan Somwar",
        heroBadgeHi = "✦ बोल बम • श्रावण सोमवार",
        heroSubtitleEn = "Gangajal Abhishekam, Kanwar Yatra, and classical Megh Malhar monsoon songs",
        heroSubtitleHi = "गंगाजल अभिषेक, कांवड़ यात्रा एवं सावन मल्हार शास्त्रीय संगीत",
        heroSubtitleHinglish = "Gangajal Abhishek, Kanwar Yatra aur Megh Malhar classical songs",
        carousel1TitleEn = "Sawan Somwar Shiv Aarti",
        carousel1TitleHi = "सावन सोमवार शिव आरती",
        carousel1SubtitleEn = "Temple aartis and prayers",
        carousel1SubtitleHi = "मंदिर आरती एवं प्रार्थना",
        carousel1Query = "Sawan Somwar Shiv Aarti songs",
        carousel2TitleEn = "Megh Malhar Monsoon Ragas",
        carousel2TitleHi = "मेघ मल्हार एवं वर्षा राग",
        carousel2SubtitleEn = "Indian classical monsoon ragas",
        carousel2SubtitleHi = "भारतीय शास्त्रीय वर्षा राग",
        carousel2Query = "Megh Malhar classical Indian monsoon",
        playlistQuery = "Sawan Shiv bhajan playlist",
    )

    val ALL_SEASONS = listOf(
        GANESH_UTSAV,
        SHARAD_NAVRATRI,
        DIWALI,
        CHHATH_PUJA,
        MAKAR_SANKRANTI,
        MAHA_SHIVRATRI,
        HOLI,
        RAM_NAVAMI,
        JANMASHTAMI,
        SAWAN_SHIV,
    )
}

/**
 * Dynamically identifies the currently active festival season by current calendar month and day.
 */
fun detectCurrentFestivalSeason(): FestivalSeason {
    val cal = Calendar.getInstance()
    val month = cal.get(Calendar.MONTH) + 1 // 1..12
    val day = cal.get(Calendar.DAY_OF_MONTH)

    return when (month) {
        1 -> FestivalCatalog.MAKAR_SANKRANTI
        2 -> FestivalCatalog.MAHA_SHIVRATRI
        3 -> FestivalCatalog.HOLI
        4 -> FestivalCatalog.RAM_NAVAMI
        5 -> FestivalCatalog.RAM_NAVAMI
        6 -> FestivalCatalog.SAWAN_SHIV
        7 -> FestivalCatalog.SAWAN_SHIV
        8 -> FestivalCatalog.JANMASHTAMI
        9 -> FestivalCatalog.GANESH_UTSAV
        10 -> if (day < 25) FestivalCatalog.SHARAD_NAVRATRI else FestivalCatalog.DIWALI
        11 -> if (day < 14) FestivalCatalog.DIWALI else FestivalCatalog.CHHATH_PUJA
        12 -> FestivalCatalog.MAKAR_SANKRANTI
        else -> FestivalCatalog.GANESH_UTSAV
    }
}

/**
 * Dedicated "Utsav" Festive Celebration screen.
 * Seamlessly adapts its language to English, Hindi, Hinglish, or Punjabi.
 * Queries live YouTube Music audio streams with zero dummy data.
 */
@Composable
fun UtsavScreen(
    listState: LazyListState,
    contentPadding: PaddingValues,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onOpenDetail: (String, String, String, String?, BrowseType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentLocale = try {
        val firstLocale = AppCompatDelegate.getApplicationLocales().get(0)
        firstLocale?.toLanguageTag() ?: firstLocale?.language
    } catch (_: Throwable) {
        null
    } ?: Locale.getDefault().toLanguageTag()

    val isEnglish = currentLocale.startsWith("en", ignoreCase = true)
    val isHindi = currentLocale.startsWith("hi", ignoreCase = true) && !currentLocale.startsWith("hi-Latn", ignoreCase = true)
    val isHinglish = currentLocale.startsWith("hi-Latn", ignoreCase = true) || currentLocale.equals("hinglish", ignoreCase = true)
    val isPunjabi = currentLocale.startsWith("pa", ignoreCase = true)

    // Current ongoing festival determined automatically by current date
    val defaultSeason = remember { detectCurrentFestivalSeason() }
    var activeSeason by remember { mutableStateOf(defaultSeason) }

    // Live search states for real playable tracks from YouTube Music
    var heroTracks by remember(activeSeason) { mutableStateOf<List<Song>?>(null) }
    var carousel1Tracks by remember(activeSeason) { mutableStateOf<List<Song>?>(null) }
    var carousel2Tracks by remember(activeSeason) { mutableStateOf<List<Song>?>(null) }
    var festivePlaylists by remember(activeSeason) { mutableStateOf<List<BrowseItem>?>(null) }
    var isLoading by remember(activeSeason) { mutableStateOf(true) }

    // Localized UI strings
    val calendarBadge = when {
        isHindi -> "✦ ऋतु चक्र • उत्सव कैलेंडर"
        isHinglish -> "✦ RITU CHAKRA • FESTIVE CALENDAR"
        isPunjabi -> "✦ ਰਿਤੂ ਚੱਕਰ • ਉਤਸਵ ਕੈਲੰਡਰ"
        else -> "✦ FESTIVE CALENDAR"
    }

    val activeSeasonChipPrefix = when {
        isHindi -> "✨ चालू उत्सव"
        isHinglish -> "✨ Active Utsav"
        isPunjabi -> "✨ ਮੌਜੂਦਾ ਤਿਉਹਾਰ"
        else -> "✨ Active Festival"
    }

    val c1Badge = if (isHindi) "भक्ति रस" else "Devotional"
    val c2Badge = if (isHindi) "उत्सव स्पेशल" else "Celebration"
    val plBadge = if (isHindi) "प्लेलिस्ट" else "Playlists"

    val playlistSectionTitle = when {
        isHindi -> "विशेष उत्सव प्लेलिस्ट"
        isHinglish -> "Special Festive Playlists"
        isPunjabi -> "ਖ਼ਾਸ ਉਤਸਵ ਪਲੇਲਿਸਟ"
        else -> "Featured Festival Playlists"
    }

    val playlistSectionSub = when {
        isHindi -> "सम्पूर्ण संग्रह एवं भक्ति संकलन"
        else -> "Curated festive collections & devotionals"
    }

    val playAllText = when {
        isHindi -> "सभी बजाएं"
        isPunjabi -> "ਸਭ ਸੁਣੋ"
        else -> "Play All"
    }

    val loadingText = when {
        isHindi -> "लोड हो रहा है..."
        isPunjabi -> "ਲੋਡ ਹੋ ਰਿਹਾ ਹੈ..."
        else -> "Loading festive songs..."
    }

    // Fetch real songs and playlists asynchronously whenever the festival changes
    LaunchedEffect(activeSeason) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val hero = YtMusicRepository.search(activeSeason.heroQuery, SearchFilter.SONGS)
                .getOrNull()?.filterIsInstance<SearchResult.Track>()?.map { it.song }

            val c1 = YtMusicRepository.search(activeSeason.carousel1Query, SearchFilter.SONGS)
                .getOrNull()?.filterIsInstance<SearchResult.Track>()?.map { it.song }

            val c2 = YtMusicRepository.search(activeSeason.carousel2Query, SearchFilter.SONGS)
                .getOrNull()?.filterIsInstance<SearchResult.Track>()?.map { it.song }

            val pl = YtMusicRepository.search(activeSeason.playlistQuery, SearchFilter.PLAYLISTS)
                .getOrNull()?.filterIsInstance<SearchResult.Browse>()?.map { it.item }

            withContext(Dispatchers.Main) {
                heroTracks = hero?.take(10)
                carousel1Tracks = c1?.take(10)
                carousel2Tracks = c2?.take(10)
                festivePlaylists = pl?.take(8)
                isLoading = false
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1013)),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── 1. Festive Header & Dynamic Ritu Chakra ──────────────────────────────
        item(key = "festive_calendar_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Pill: Festive Calendar
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CardSurface)
                            .border(1.dp, SaffronPrimary.copy(alpha = 0.35f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = calendarBadge,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 11.5.sp,
                            ),
                            color = SaffronPrimary,
                        )
                    }

                    // Active Lunar / Ritu indicator
                    Text(
                        text = activeSeason.ritu(currentLocale),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }

                // Festival Switcher horizontal chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Default / currently live ongoing season chip
                    item {
                        val isDefaultActive = activeSeason.id == defaultSeason.id
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isDefaultActive) SaffronPrimary else CardSurface)
                                .border(
                                    1.dp,
                                    if (isDefaultActive) SaffronPrimary else CardBorder,
                                    CircleShape,
                                )
                                .clickable { activeSeason = defaultSeason }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$activeSeasonChipPrefix (${defaultSeason.title(currentLocale).split(" ").first()})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                ),
                                color = if (isDefaultActive) Color.Black else Color.White,
                            )
                        }
                    }

                    // Other Indian festivals
                    items(FestivalCatalog.ALL_SEASONS) { season ->
                        if (season.id != defaultSeason.id) {
                            val isSelected = activeSeason.id == season.id
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) SaffronPrimary else CardSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) SaffronPrimary else CardBorder,
                                        CircleShape,
                                    )
                                    .clickable { activeSeason = season }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = season.title(currentLocale),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                    ),
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                                )
                            }
                        }
                    }
                }

                // ── 2. Dynamic Hero Card for Active Festival ──────────────────────
                val topTrack = heroTracks?.firstOrNull()
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SaffronPrimary.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Ambient gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            SaffronDark.copy(alpha = 0.35f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(SaffronPrimary.copy(alpha = 0.2f))
                                        .border(0.5.dp, SaffronPrimary.copy(alpha = 0.5f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = activeSeason.heroBadge(currentLocale),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = SaffronPrimary,
                                    )
                                }

                                Text(
                                    text = activeSeason.lunar(currentLocale),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.5.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            }

                            // Festival Title & Subtitle in active language
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = activeSeason.title(currentLocale),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp,
                                    ),
                                    color = Color.White,
                                )
                                Text(
                                    text = activeSeason.heroSubtitle(currentLocale),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            // If loaded, display top live track preview and play button
                            if (topTrack != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            heroTracks?.let { onPlaySongs(it, 0) }
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    AsyncImage(
                                        model = topTrack.thumbnailUrl,
                                        contentDescription = topTrack.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = topTrack.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = topTrack.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(SaffronPrimary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.Black,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            } else if (isLoading) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = SaffronPrimary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = loadingText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3. Real Song Carousel 1 ──────────────────────────────────────────────
        item(key = "carousel_1_header") {
            FestiveSectionHeader(
                title = activeSeason.c1Title(currentLocale),
                subtitle = activeSeason.c1Sub(currentLocale),
                badge = c1Badge,
                playAllText = playAllText,
                onPlayAll = {
                    carousel1Tracks?.let { if (it.isNotEmpty()) onPlaySongs(it, 0) }
                },
            )
        }

        item(key = "carousel_1_list") {
            val list = carousel1Tracks
            if (list != null && list.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(list) { index, song ->
                        RealSongCard(
                            song = song,
                            onClick = { onPlaySongs(list, index) },
                        )
                    }
                }
            } else if (isLoading) {
                FestiveShimmerRow()
            }
        }

        // ── 4. Real Song Carousel 2 ──────────────────────────────────────────────
        item(key = "carousel_2_header") {
            FestiveSectionHeader(
                title = activeSeason.c2Title(currentLocale),
                subtitle = activeSeason.c2Sub(currentLocale),
                badge = c2Badge,
                playAllText = playAllText,
                onPlayAll = {
                    carousel2Tracks?.let { if (it.isNotEmpty()) onPlaySongs(it, 0) }
                },
            )
        }

        item(key = "carousel_2_list") {
            val list = carousel2Tracks
            if (list != null && list.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(list) { index, song ->
                        RealSongCard(
                            song = song,
                            onClick = { onPlaySongs(list, index) },
                        )
                    }
                }
            } else if (isLoading) {
                FestiveShimmerRow()
            }
        }

        // ── 5. Real Playlists ───────────────────────────────────────────────────
        item(key = "playlists_header") {
            val playlists = festivePlaylists
            if (!playlists.isNullOrEmpty()) {
                FestiveSectionHeader(
                    title = playlistSectionTitle,
                    subtitle = playlistSectionSub,
                    badge = plBadge,
                    playAllText = playAllText,
                    onPlayAll = null,
                )
            }
        }

        item(key = "playlists_list") {
            val playlists = festivePlaylists
            if (!playlists.isNullOrEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(playlists) { item ->
                        RealPlaylistCard(
                            item = item,
                            onClick = {
                                onOpenDetail(
                                    item.browseId,
                                    item.title,
                                    item.subtitle,
                                    item.thumbnailUrl,
                                    item.type,
                                )
                            },
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(30.dp))
        }
    }
}

/**
 * Section title header for festival rows with optional "Play All" action.
 */
@Composable
private fun FestiveSectionHeader(
    title: String,
    subtitle: String,
    badge: String,
    playAllText: String,
    onPlayAll: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SaffronPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                        ),
                        color = SaffronPrimary,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (onPlayAll != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CardSurface)
                    .border(0.5.dp, CardBorder, CircleShape)
                    .clickable(onClick = onPlayAll)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play all",
                        tint = SaffronPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = playAllText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = SaffronPrimary,
                    )
                }
            }
        }
    }
}

/**
 * Real song card with real thumbnail, title, artist, and click-to-play.
 */
@Composable
private fun RealSongCard(
    song: Song,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardSurface)
                .border(0.5.dp, CardBorder, RoundedCornerShape(16.dp)),
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Play icon overlay in bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = SaffronPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                ),
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Real playlist card that navigates to the playlist detail page.
 */
@Composable
private fun RealPlaylistCard(
    item: BrowseItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardSurface)
                .border(0.5.dp, CardBorder, RoundedCornerShape(16.dp)),
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Playlist badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlaylistPlay,
                    contentDescription = "Playlist",
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Shimmer placeholders shown while live festival tracks are loading from YouTube Music.
 */
@Composable
private fun FestiveShimmerRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(4) {
            Column(
                modifier = Modifier.width(140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface.copy(alpha = 0.6f))
                        .border(0.5.dp, CardBorder, RoundedCornerShape(16.dp)),
                )
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                )
            }
        }
    }
}
