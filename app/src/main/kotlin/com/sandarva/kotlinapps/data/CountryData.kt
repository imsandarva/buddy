package com.sandarva.kotlinapps.data

/**
 * One country, its flag, and the language Buddy should speak when that country is chosen as the
 * live-talk language (see docs/onboarding.md language step). Ported from a past Flutter project's
 * nationality picker (`country.dart`) — same codes and names, with a fourth field (`language`)
 * added for this app's purpose: picking how Buddy talks, not where someone is from.
 */
data class Country(val code: String, val flag: String, val name: String, val language: String)

object CountryData {
    /** ISO alpha-2 code, flag, country name, primary spoken language — in that order, A→Z. */
    val ALL: List<Country> = listOf(
        // A
        Country("af", "🇦🇫", "Afghanistan", "Dari (Persian)"),
        Country("ax", "🇦🇽", "Åland Islands", "Swedish"),
        Country("al", "🇦🇱", "Albania", "Albanian"),
        Country("dz", "🇩🇿", "Algeria", "Arabic"),
        Country("ad", "🇦🇩", "Andorra", "Catalan"),
        Country("ao", "🇦🇴", "Angola", "Portuguese"),
        Country("ai", "🇦🇮", "Anguilla", "English"),
        Country("ag", "🇦🇬", "Antigua and Barbuda", "English"),
        Country("ar", "🇦🇷", "Argentina", "Spanish"),
        Country("am", "🇦🇲", "Armenia", "Armenian"),
        Country("aw", "🇦🇼", "Aruba", "Dutch"),
        Country("au", "🇦🇺", "Australia", "English"),
        Country("at", "🇦🇹", "Austria", "German"),
        Country("az", "🇦🇿", "Azerbaijan", "Azerbaijani"),

        // B
        Country("bs", "🇧🇸", "Bahamas", "English"),
        Country("bh", "🇧🇭", "Bahrain", "Arabic"),
        Country("bd", "🇧🇩", "Bangladesh", "Bengali"),
        Country("bb", "🇧🇧", "Barbados", "English"),
        Country("by", "🇧🇾", "Belarus", "Belarusian"),
        Country("be", "🇧🇪", "Belgium", "Dutch"),
        Country("bz", "🇧🇿", "Belize", "English"),
        Country("bj", "🇧🇯", "Benin", "French"),
        Country("bm", "🇧🇲", "Bermuda", "English"),
        Country("bt", "🇧🇹", "Bhutan", "Dzongkha"),
        Country("bo", "🇧🇴", "Bolivia", "Spanish"),
        Country("ba", "🇧🇦", "Bosnia and Herzegovina", "Bosnian"),
        Country("bw", "🇧🇼", "Botswana", "English"),
        Country("br", "🇧🇷", "Brazil", "Portuguese"),
        Country("vg", "🇻🇬", "British Virgin Islands", "English"),
        Country("bn", "🇧🇳", "Brunei", "Malay"),
        Country("bg", "🇧🇬", "Bulgaria", "Bulgarian"),
        Country("bf", "🇧🇫", "Burkina Faso", "French"),
        Country("bi", "🇧🇮", "Burundi", "Kirundi"),

        // C
        Country("cv", "🇨🇻", "Cabo Verde", "Portuguese"),
        Country("kh", "🇰🇭", "Cambodia", "Khmer"),
        Country("cm", "🇨🇲", "Cameroon", "French"),
        Country("ca", "🇨🇦", "Canada", "English"),
        Country("ky", "🇰🇾", "Cayman Islands", "English"),
        Country("cf", "🇨🇫", "Central African Republic", "French"),
        Country("td", "🇹🇩", "Chad", "French"),
        Country("cl", "🇨🇱", "Chile", "Spanish"),
        Country("cn", "🇨🇳", "China", "Mandarin Chinese"),
        Country("cx", "🇨🇽", "Christmas Island", "English"),
        Country("cc", "🇨🇨", "Cocos (Keeling) Islands", "English"),
        Country("co", "🇨🇴", "Colombia", "Spanish"),
        Country("km", "🇰🇲", "Comoros", "Comorian"),
        Country("cg", "🇨🇬", "Congo (Brazzaville)", "French"),
        Country("cd", "🇨🇩", "Congo (Kinshasa)", "French"),
        Country("cr", "🇨🇷", "Costa Rica", "Spanish"),
        Country("ci", "🇨🇮", "Côte d'Ivoire", "French"),
        Country("hr", "🇭🇷", "Croatia", "Croatian"),
        Country("cu", "🇨🇺", "Cuba", "Spanish"),
        Country("cw", "🇨🇼", "Curaçao", "Dutch"),
        Country("cy", "🇨🇾", "Cyprus", "Greek"),
        Country("cz", "🇨🇿", "Czechia", "Czech"),

        // D
        Country("dk", "🇩🇰", "Denmark", "Danish"),
        Country("dj", "🇩🇯", "Djibouti", "French"),
        Country("dm", "🇩🇲", "Dominica", "English"),
        Country("do", "🇩🇴", "Dominican Republic", "Spanish"),

        // E
        Country("ec", "🇪🇨", "Ecuador", "Spanish"),
        Country("eg", "🇪🇬", "Egypt", "Arabic"),
        Country("sv", "🇸🇻", "El Salvador", "Spanish"),
        Country("gq", "🇬🇶", "Equatorial Guinea", "Spanish"),
        Country("er", "🇪🇷", "Eritrea", "Tigrinya"),
        Country("ee", "🇪🇪", "Estonia", "Estonian"),
        Country("sz", "🇸🇿", "Eswatini", "siSwati"),
        Country("et", "🇪🇹", "Ethiopia", "Amharic"),

        // F
        Country("fk", "🇫🇰", "Falkland Islands", "English"),
        Country("fo", "🇫🇴", "Faroe Islands", "Faroese"),
        Country("fj", "🇫🇯", "Fiji", "English"),
        Country("fi", "🇫🇮", "Finland", "Finnish"),
        Country("fr", "🇫🇷", "France", "French"),
        Country("gf", "🇬🇫", "French Guiana", "French"),
        Country("pf", "🇵🇫", "French Polynesia", "French"),
        Country("tf", "🇹🇫", "French Southern Territories", "French"),

        // G
        Country("ga", "🇬🇦", "Gabon", "French"),
        Country("gm", "🇬🇲", "Gambia", "English"),
        Country("ge", "🇬🇪", "Georgia", "Georgian"),
        Country("de", "🇩🇪", "Germany", "German"),
        Country("gh", "🇬🇭", "Ghana", "English"),
        Country("gi", "🇬🇮", "Gibraltar", "English"),
        Country("gr", "🇬🇷", "Greece", "Greek"),
        Country("gl", "🇬🇱", "Greenland", "Greenlandic"),
        Country("gd", "🇬🇩", "Grenada", "English"),
        Country("gp", "🇬🇵", "Guadeloupe", "French"),
        Country("gu", "🇬🇺", "Guam", "English"),
        Country("gt", "🇬🇹", "Guatemala", "Spanish"),
        Country("gg", "🇬🇬", "Guernsey", "English"),
        Country("gn", "🇬🇳", "Guinea", "French"),
        Country("gw", "🇬🇼", "Guinea-Bissau", "Portuguese"),
        Country("gy", "🇬🇾", "Guyana", "English"),

        // H
        Country("ht", "🇭🇹", "Haiti", "Haitian Creole"),
        Country("hm", "🇭🇲", "Heard Island and McDonald Islands", "English"),
        Country("va", "🇻🇦", "Holy See (Vatican City)", "Italian"),
        Country("hn", "🇭🇳", "Honduras", "Spanish"),
        Country("hk", "🇭🇰", "Hong Kong", "Cantonese"),
        Country("hu", "🇭🇺", "Hungary", "Hungarian"),

        // I
        Country("is", "🇮🇸", "Iceland", "Icelandic"),
        Country("in", "🇮🇳", "India", "Hindi"),
        Country("id", "🇮🇩", "Indonesia", "Indonesian"),
        Country("ir", "🇮🇷", "Iran", "Persian"),
        Country("iq", "🇮🇶", "Iraq", "Arabic"),
        Country("ie", "🇮🇪", "Ireland", "English"),
        Country("im", "🇮🇲", "Isle of Man", "English"),
        Country("il", "🇮🇱", "Israel", "Hebrew"),
        Country("it", "🇮🇹", "Italy", "Italian"),

        // J
        Country("jm", "🇯🇲", "Jamaica", "English"),
        Country("jp", "🇯🇵", "Japan", "Japanese"),
        Country("je", "🇯🇪", "Jersey", "English"),
        Country("jo", "🇯🇴", "Jordan", "Arabic"),

        // K
        Country("kz", "🇰🇿", "Kazakhstan", "Kazakh"),
        Country("ke", "🇰🇪", "Kenya", "Swahili"),
        Country("ki", "🇰🇮", "Kiribati", "English"),
        Country("kw", "🇰🇼", "Kuwait", "Arabic"),
        Country("kg", "🇰🇬", "Kyrgyzstan", "Kyrgyz"),

        // L
        Country("la", "🇱🇦", "Laos", "Lao"),
        Country("lv", "🇱🇻", "Latvia", "Latvian"),
        Country("lb", "🇱🇧", "Lebanon", "Arabic"),
        Country("ls", "🇱🇸", "Lesotho", "Sesotho"),
        Country("lr", "🇱🇷", "Liberia", "English"),
        Country("ly", "🇱🇾", "Libya", "Arabic"),
        Country("li", "🇱🇮", "Liechtenstein", "German"),
        Country("lt", "🇱🇹", "Lithuania", "Lithuanian"),
        Country("lu", "🇱🇺", "Luxembourg", "Luxembourgish"),

        // M
        Country("mo", "🇲🇴", "Macao", "Cantonese"),
        Country("mk", "🇲🇰", "North Macedonia", "Macedonian"),
        Country("mg", "🇲🇬", "Madagascar", "Malagasy"),
        Country("mw", "🇲🇼", "Malawi", "Chichewa"),
        Country("my", "🇲🇾", "Malaysia", "Malay"),
        Country("mv", "🇲🇻", "Maldives", "Dhivehi"),
        Country("ml", "🇲🇱", "Mali", "French"),
        Country("mt", "🇲🇹", "Malta", "Maltese"),
        Country("mh", "🇲🇭", "Marshall Islands", "English"),
        Country("mq", "🇲🇶", "Martinique", "French"),
        Country("mr", "🇲🇷", "Mauritania", "Arabic"),
        Country("mu", "🇲🇺", "Mauritius", "English"),
        Country("yt", "🇾🇹", "Mayotte", "French"),
        Country("mx", "🇲🇽", "Mexico", "Spanish"),
        Country("fm", "🇫🇲", "Micronesia", "English"),
        Country("md", "🇲🇩", "Moldova", "Romanian"),
        Country("mc", "🇲🇨", "Monaco", "French"),
        Country("mn", "🇲🇳", "Mongolia", "Mongolian"),
        Country("me", "🇲🇪", "Montenegro", "Montenegrin"),
        Country("ms", "🇲🇸", "Montserrat", "English"),
        Country("ma", "🇲🇦", "Morocco", "Arabic"),
        Country("mz", "🇲🇿", "Mozambique", "Portuguese"),
        Country("mm", "🇲🇲", "Myanmar (Burma)", "Burmese"),

        // N
        Country("na", "🇳🇦", "Namibia", "English"),
        Country("nr", "🇳🇷", "Nauru", "English"),
        Country("np", "🇳🇵", "Nepal", "Nepali"),
        Country("nl", "🇳🇱", "Netherlands", "Dutch"),
        Country("nc", "🇳🇨", "New Caledonia", "French"),
        Country("nz", "🇳🇿", "New Zealand", "English"),
        Country("ni", "🇳🇮", "Nicaragua", "Spanish"),
        Country("ne", "🇳🇪", "Niger", "French"),
        Country("ng", "🇳🇬", "Nigeria", "English"),
        Country("nu", "🇳🇺", "Niue", "English"),
        Country("nf", "🇳🇫", "Norfolk Island", "English"),
        Country("kp", "🇰🇵", "North Korea", "Korean"),
        Country("mp", "🇲🇵", "Northern Mariana Islands", "English"),
        Country("no", "🇳🇴", "Norway", "Norwegian"),

        // O
        Country("om", "🇴🇲", "Oman", "Arabic"),

        // P
        Country("pk", "🇵🇰", "Pakistan", "Urdu"),
        Country("pw", "🇵🇼", "Palau", "English"),
        Country("ps", "🇵🇸", "Palestine", "Arabic"),
        Country("pa", "🇵🇦", "Panama", "Spanish"),
        Country("pg", "🇵🇬", "Papua New Guinea", "English"),
        Country("py", "🇵🇾", "Paraguay", "Spanish"),
        Country("pe", "🇵🇪", "Peru", "Spanish"),
        Country("ph", "🇵🇭", "Philippines", "Filipino"),
        Country("pn", "🇵🇳", "Pitcairn Islands", "English"),
        Country("pl", "🇵🇱", "Poland", "Polish"),
        Country("pt", "🇵🇹", "Portugal", "Portuguese"),
        Country("pr", "🇵🇷", "Puerto Rico", "Spanish"),

        // Q
        Country("qa", "🇶🇦", "Qatar", "Arabic"),

        // R
        Country("re", "🇷🇪", "Réunion", "French"),
        Country("ro", "🇷🇴", "Romania", "Romanian"),
        Country("ru", "🇷🇺", "Russia", "Russian"),
        Country("rw", "🇷🇼", "Rwanda", "Kinyarwanda"),

        // S
        Country("bl", "🇧🇱", "Saint Barthélemy", "French"),
        Country("sh", "🇱🇸", "Saint Helena, Ascension and Tristan da Cunha", "English"),
        Country("kn", "🇰🇳", "Saint Kitts and Nevis", "English"),
        Country("lc", "🇱🇨", "Saint Lucia", "English"),
        Country("mf", "🇲🇫", "Saint Martin", "French"),
        Country("pm", "🇵🇲", "Saint Pierre and Miquelon", "French"),
        Country("vc", "🇻🇨", "Saint Vincent and the Grenadines", "English"),
        Country("ws", "🇼🇸", "Samoa", "Samoan"),
        Country("sm", "🇸🇲", "San Marino", "Italian"),
        Country("sa", "🇸🇦", "Saudi Arabia", "Arabic"),
        Country("sn", "🇸🇳", "Senegal", "French"),
        Country("rs", "🇷🇸", "Serbia", "Serbian"),
        Country("sc", "🇸🇨", "Seychelles", "French"),
        Country("sl", "🇸🇱", "Sierra Leone", "English"),
        Country("sg", "🇸🇬", "Singapore", "English"),
        Country("sx", "🇸🇽", "Sint Maarten", "English"),
        Country("sk", "🇸🇰", "Slovakia", "Slovak"),
        Country("si", "🇸🇮", "Slovenia", "Slovenian"),
        Country("sb", "🇸🇧", "Solomon Islands", "English"),
        Country("so", "🇸🇴", "Somalia", "Somali"),
        Country("za", "🇿🇦", "South Africa", "English"),
        Country("gs", "🇬🇸", "South Georgia and the South Sandwich Islands", "English"),
        Country("kr", "🇰🇷", "South Korea", "Korean"),
        Country("ss", "🇸🇸", "South Sudan", "English"),
        Country("es", "🇪🇸", "Spain", "Spanish"),
        Country("lk", "🇱🇰", "Sri Lanka", "Sinhala"),
        Country("sd", "🇸🇩", "Sudan", "Arabic"),
        Country("sr", "🇸🇷", "Suriname", "Dutch"),
        Country("sj", "🇸🇯", "Svalbard and Jan Mayen", "Norwegian"),
        Country("se", "🇸🇪", "Sweden", "Swedish"),
        Country("ch", "🇨🇭", "Switzerland", "German"),
        Country("sy", "🇸🇾", "Syria", "Arabic"),

        // T
        Country("tw", "🇹🇼", "Taiwan", "Mandarin Chinese"),
        Country("tj", "🇹🇯", "Tajikistan", "Tajik"),
        Country("tz", "🇹🇿", "Tanzania", "Swahili"),
        Country("th", "🇹🇭", "Thailand", "Thai"),
        Country("tl", "🇹🇱", "Timor-Leste (East Timor)", "Portuguese"),
        Country("tg", "🇹🇬", "Togo", "French"),
        Country("tk", "🇹🇰", "Tokelau", "English"),
        Country("to", "🇹🇴", "Tonga", "Tongan"),
        Country("tt", "🇹🇹", "Trinidad and Tobago", "English"),
        Country("tn", "🇹🇳", "Tunisia", "Arabic"),
        Country("tr", "🇹🇷", "Turkey", "Turkish"),
        Country("tm", "🇹🇲", "Turkmenistan", "Turkmen"),
        Country("tc", "🇹🇨", "Turks and Caicos Islands", "English"),
        Country("tv", "🇹🇻", "Tuvalu", "English"),

        // U
        Country("ug", "🇺🇬", "Uganda", "English"),
        Country("ua", "🇺🇦", "Ukraine", "Ukrainian"),
        Country("ae", "🇦🇪", "United Arab Emirates", "Arabic"),
        Country("gb", "🇬🇧", "United Kingdom", "English"),
        Country("us", "🇺🇸", "United States", "English"),
        Country("uy", "🇺🇾", "Uruguay", "Spanish"),
        Country("uz", "🇺🇿", "Uzbekistan", "Uzbek"),

        // V
        Country("vu", "🇻🇺", "Vanuatu", "English"),
        Country("ve", "🇻🇪", "Venezuela", "Spanish"),
        Country("vn", "🇻🇳", "Vietnam", "Vietnamese"),

        // W
        Country("wf", "🇼🇫", "Wallis and Futuna", "French"),
        Country("eh", "🇪🇭", "Western Sahara", "Arabic"),

        // Y
        Country("ye", "🇾🇪", "Yemen", "Arabic"),

        // Z
        Country("zm", "🇿🇲", "Zambia", "English"),
        Country("zw", "🇿🇼", "Zimbabwe", "English")
    )

    private val byCode: Map<String, Country> = ALL.associateBy { it.code }

    /** Countries sorted by display name — the order the language picker shows them in. */
    val sortedByName: List<Country> by lazy { ALL.sortedBy { it.name } }

    fun find(code: String): Country? = if (code.isBlank()) null else byCode[code.trim().lowercase()]

    /** Buddy's out-of-the-box voice — English, before anyone has picked a language. */
    val DEFAULT: Country = byCode.getValue("us")
}
