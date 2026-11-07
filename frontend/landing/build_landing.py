"""Writes the public homepage in English, Hindi and Marathi from template.html.

Run after changing a text:  python3 frontend/landing/build_landing.py
Output: public/home.html (served at /), public/hi/index.html, public/mr/index.html.
"""
import html
import json
import re
from pathlib import Path

HERE = Path(__file__).parent
PUBLIC = HERE.parent / "public"
SITE = "https://runwalltdfakepromises.com"

LOGO = ('<svg viewBox="72 24 112 216" aria-hidden="true" focusable="false"><path fill="currentColor" fill-rule="evenodd" '
        'd="M92 24 H164 A20 20 0 0 1 184 44 V188 A20 20 0 0 1 164 208 H126 L92 238 V208 A20 20 0 0 1 72 188 V44 '
        'A20 20 0 0 1 92 24 Z M96 56 h24 v24 h-24 Z M136 56 h24 v24 h-24 Z M96 100 h24 v24 h-24 Z M136 100 h24 v24 '
        'h-24 Z M96 144 h24 v24 h-24 Z M136 144 h24 v24 h-24 Z"/></svg>')

# the photograph beside each reason, in order
BLOCK_PHOTOS = [("podium-seating", 1200, 675, "alt_podium"), ("lawn-clubhouse", 1200, 587, "alt_clubhouse"),
                ("skyline", 1200, 675, "alt_skyline"), ("garden", 1200, 675, "alt_garden")]

TEXTS = {
    "en": {
        "path": "/", "og_locale": "en_IN",
        "meta_title": "Runwal MyCity Residents: complaints and feed, Dombivli",
        "meta_description": "The residents' app for Runwal MyCity, Dombivli: raise maintenance complaints with photos, follow them until fixed, read a moderated feed of documented issues.",
        "skip": "Skip to content", "language": "Language",
        "nav_how": "How it works", "nav_why": "Why residents use it", "nav_faq": "Questions", "nav_signin": "Sign in",
        "h1": "Runwal MyCity",
        "lead": "The residents' app for the ten towers of Runwal MyCity, Dombivli.",
        "lead_2": "Report a problem with photos, follow it until it is fixed, and read what neighbours have documented.",
        "cta_signin": "Sign in with your mobile",
        "signin_note": "Sign in with an SMS code. Your number is never shown to other residents.",
        "alt_towers": "Towers of Runwal MyCity around the landscaped courtyard",
        "alt_playground": "Children's playground with slides on the lawn",
        "alt_podium": "Seating on the podium between the towers",
        "alt_clubhouse": "The clubhouse and its lawn",
        "alt_skyline": "The towers seen from outside the society",
        "alt_garden": "Garden with lawn and palms beside the towers",
        "how_title": "How it works",
        "how_intro": "Three things happen here. Pick one to see its steps; the times are an example from a typical day.",
        "tab_complaint": "Raise a complaint", "tab_feed": "Document an issue", "tab_staff": "Staff shifts",
        "slip_complaint": ([("Morning", [("You send it, with two photos", "9:10 am"), ("The tower admin assigns Ravi", "9:40 am"),
                                         ("Ravi starts work", "10:05 am")]),
                            ("Afternoon", [("Washer replaced, marked resolved", "1:20 pm")]),
                            ("Evening", [("You check it and close it", "6:30 pm")])],
                           ("Fixed, every step on record", "4 h 10 min")),
        "slip_feed": ([("Day one", [("You write what, where and when, with photos", "10 min"), ("It waits for review", "private")]),
                       ("Day two", [("The moderator checks it against the content policy", "1 day"),
                                    ("Published as \"Verified resident\"", "to residents")])],
                      ("Every resident sees it; your name stays hidden", "1 day")),
        "slip_staff": ([("Start", [("Selfie at the society, location checked", "8:00 am"), ("Jobs listed, most urgent first", "8:05 am")]),
                        ("During the day", [("Start, fix, mark resolved", "each job")]),
                        ("End", [("Selfie to end the shift", "4:00 pm"), ("A tower admin compares the selfies", "next day")])],
                       ("Hours and fixes on record", "8 h")),
        "why_title": "Problems get seen, and fixed",
        "blocks": [("Photos, not phone calls", ["Up to 5 photos per complaint", "Your flat or a common area",
                                                "A category, so it reaches the right person", "Nothing lost in a WhatsApp thread"]),
                   ("Every step on record", ["Who it was assigned to", "When work started", "When it was fixed, and by whom",
                                             "You close it, or say it is not fixed"]),
                   ("Documented, not rumours", ["What, where, when, and what was promised", "Checked by a moderator first",
                                                "No names, faces or personal details", "Three reports hide a post until it is reviewed"]),
                   ("Private by design", ["Only your name, mobile and flat", "Your number is never shown to residents",
                                          "Posts show \"Verified resident\"", "English, हिन्दी and मराठी"])],
        "faq_title": "Questions",
        "fact_1": "For owners and tenants of all ten towers",
        "fact_2": "Works in the browser on any phone, nothing to install",
        "fact_3": "English, हिन्दी and मराठी",
        "q1": "Who can use it?", "a1": "Residents of Runwal MyCity, Dombivli: owners and tenants. Sign in with your mobile number, tell us your flat, and the society admin approves you, usually within a day.",
        "q2": "Is this the developer's app?", "a2": "No. It is an independent app for residents. It is not run by, or affiliated with, Runwal or the builder.",
        "q3": "Who sees my mobile number?", "a3": "Nobody except the society admin, once, to check your sign-up. Other residents see names only, and feed posts never show who wrote them.",
        "q4": "What gets published in the feed?", "a4": "Factual, documented issues: what, where, when, and what was promised. Posts with names, faces, abuse or unproven accusations are not published.",
        "q5": "Which languages does it support?", "a5": "English, हिन्दी and मराठी. Choose at the top of any screen; the app remembers it on your phone.",
        "faq_link": "See how it works",
        "join_title": "Living in Runwal MyCity?",
        "join_text": "Sign in with your mobile number and tell us your flat. The society admin approves you, usually within a day.",
        "foot_lang": "Language", "foot_about": "About",
        "footer_independent": "An independent app for residents. Not run by, or affiliated with, Runwal or the builder.",
        "footer_place": "Runwal MyCity, Kalyan-Shil Road, Dombivli East, Thane, Maharashtra.",
    },
    "hi": {
        "path": "/hi/", "og_locale": "hi_IN",
        "meta_title": "रनवाल मायसिटी निवासी | फ़ोटो के साथ शिकायतें और निवासियों की फ़ीड, डोंबिवली",
        "meta_description": "रनवाल मायसिटी, डोंबिवली के निवासियों का ऐप। फ़ोटो के साथ मेंटेनेंस शिकायत दर्ज करें, ठीक होने तक उस पर नज़र रखें, और दर्ज की गई समस्याओं की जाँची हुई फ़ीड पढ़ें।",
        "skip": "सामग्री पर जाएँ", "language": "भाषा",
        "nav_how": "यह कैसे काम करता है", "nav_why": "निवासी क्यों इस्तेमाल करते हैं", "nav_faq": "सवाल", "nav_signin": "साइन इन",
        "h1": "रनवाल मायसिटी",
        "lead": "रनवाल मायसिटी, डोंबिवली के दस टावरों के निवासियों का ऐप।",
        "lead_2": "फ़ोटो के साथ समस्या दर्ज करें, ठीक होने तक उस पर नज़र रखें, और पड़ोसियों की दर्ज की हुई बातें पढ़ें।",
        "cta_signin": "मोबाइल से साइन इन करें",
        "signin_note": "SMS कोड से साइन इन करें। आपका नंबर दूसरे निवासियों को कभी नहीं दिखता।",
        "alt_towers": "लैंडस्केप वाले आँगन के चारों ओर रनवाल मायसिटी के टावर",
        "alt_playground": "लॉन पर स्लाइड वाला बच्चों का खेल का मैदान",
        "alt_podium": "टावरों के बीच पोडियम पर बैठने की जगह",
        "alt_clubhouse": "क्लबहाउस और उसका लॉन",
        "alt_skyline": "सोसाइटी के बाहर से दिखते टावर",
        "alt_garden": "टावरों के पास लॉन और पेड़ों वाला बगीचा",
        "how_title": "यह कैसे काम करता है",
        "how_intro": "यहाँ तीन काम होते हैं। कदम देखने के लिए एक चुनें; समय एक आम दिन का उदाहरण है।",
        "tab_complaint": "शिकायत दर्ज करें", "tab_feed": "समस्या दर्ज करें", "tab_staff": "स्टाफ़ शिफ़्ट",
        "slip_complaint": ([("सुबह", [("आप दो फ़ोटो के साथ भेजते हैं", "9:10"), ("टावर एडमिन रवि को सौंपते हैं", "9:40"),
                                      ("रवि काम शुरू करते हैं", "10:05")]),
                            ("दोपहर", [("वॉशर बदला, ठीक हुई बताई", "1:20")]),
                            ("शाम", [("आप जाँचकर बंद करते हैं", "6:30")])],
                           ("ठीक हुई, हर कदम दर्ज", "4 घं 10 मि")),
        "slip_feed": ([("पहला दिन", [("आप क्या, कहाँ, कब लिखते हैं, फ़ोटो के साथ", "10 मि"), ("यह जाँच का इंतज़ार करती है", "निजी")]),
                       ("दूसरा दिन", [("मॉडरेटर कंटेंट नियमों से जाँचते हैं", "1 दिन"),
                                      ("\"सत्यापित निवासी\" के नाम से प्रकाशित", "निवासियों के लिए")])],
                      ("सब निवासी देखते हैं, आपका नाम छिपा रहता है", "1 दिन")),
        "slip_staff": ([("शुरुआत", [("सोसाइटी में सेल्फ़ी, जगह की जाँच", "8:00"), ("काम की सूची, सबसे ज़रूरी पहले", "8:05")]),
                        ("दिन भर", [("शुरू करें, ठीक करें, ठीक हुई बताएँ", "हर काम")]),
                        ("अंत", [("शिफ़्ट ख़त्म करने की सेल्फ़ी", "4:00"), ("टावर एडमिन सेल्फ़ी मिलाते हैं", "अगले दिन")])],
                       ("घंटे और काम दर्ज", "8 घं")),
        "why_title": "समस्याएँ दिखती हैं, और ठीक होती हैं",
        "blocks": [("फ़ोन कॉल नहीं, फ़ोटो", ["हर शिकायत में 5 तक फ़ोटो", "आपका फ़्लैट या कॉमन एरिया",
                                            "श्रेणी, ताकि सही व्यक्ति तक पहुँचे", "WhatsApp की बातचीत में कुछ नहीं खोता"]),
                   ("हर कदम दर्ज", ["किसे सौंपी गई", "काम कब शुरू हुआ", "कब और किसने ठीक की",
                                    "आप बंद करें, या बताएँ कि ठीक नहीं हुई"]),
                   ("दर्ज तथ्य, अफ़वाहें नहीं", ["क्या, कहाँ, कब, और क्या वादा था", "पहले मॉडरेटर जाँचते हैं",
                                                "कोई नाम, चेहरा या निजी जानकारी नहीं", "तीन रिपोर्ट पर पोस्ट जाँच तक छिप जाती है"]),
                   ("निजता का ध्यान", ["सिर्फ़ आपका नाम, मोबाइल और फ़्लैट", "आपका नंबर निवासियों को कभी नहीं दिखता",
                                       "पोस्ट पर \"सत्यापित निवासी\" दिखता है", "English, हिन्दी और मराठी"])],
        "faq_title": "सवाल",
        "fact_1": "सभी दस टावरों के मालिकों और किरायेदारों के लिए",
        "fact_2": "किसी भी फ़ोन के ब्राउज़र में, कुछ इंस्टॉल नहीं करना",
        "fact_3": "English, हिन्दी और मराठी",
        "q1": "इसे कौन इस्तेमाल कर सकता है?", "a1": "रनवाल मायसिटी, डोंबिवली के निवासी: मालिक और किरायेदार। मोबाइल नंबर से साइन इन करें, अपना फ़्लैट बताएँ, और सोसाइटी एडमिन आम तौर पर एक दिन में मंज़ूरी देते हैं।",
        "q2": "क्या यह डेवलपर का ऐप है?", "a2": "नहीं। यह निवासियों का स्वतंत्र ऐप है। इसे रनवाल या बिल्डर नहीं चलाते, न इसका उनसे कोई संबंध है।",
        "q3": "मेरा मोबाइल नंबर कौन देखता है?", "a3": "सिर्फ़ सोसाइटी एडमिन, एक बार, आपका पंजीकरण जाँचने के लिए। दूसरे निवासी सिर्फ़ नाम देखते हैं, और फ़ीड पोस्ट में कभी नहीं दिखता कि किसने लिखी।",
        "q4": "फ़ीड में क्या प्रकाशित होता है?", "a4": "तथ्य पर आधारित, दर्ज समस्याएँ: क्या, कहाँ, कब, और क्या वादा था। नाम, चेहरे, अपशब्द या बिना सबूत के आरोप वाली पोस्ट प्रकाशित नहीं होतीं।",
        "q5": "कौन-सी भाषाएँ हैं?", "a5": "English, हिन्दी और मराठी। किसी भी स्क्रीन के ऊपर चुनें; ऐप इसे आपके फ़ोन पर याद रखता है।",
        "faq_link": "देखें यह कैसे काम करता है",
        "join_title": "रनवाल मायसिटी में रहते हैं?",
        "join_text": "अपने मोबाइल नंबर से साइन इन करें और अपना फ़्लैट बताएँ। सोसाइटी एडमिन आम तौर पर एक दिन में मंज़ूरी देते हैं।",
        "foot_lang": "भाषा", "foot_about": "परिचय",
        "footer_independent": "निवासियों का स्वतंत्र ऐप। इसे रनवाल या बिल्डर नहीं चलाते, न इसका उनसे कोई संबंध है।",
        "footer_place": "रनवाल मायसिटी, कल्याण-शिल रोड, डोंबिवली पूर्व, ठाणे, महाराष्ट्र।",
    },
    "mr": {
        "path": "/mr/", "og_locale": "mr_IN",
        "meta_title": "रनवाल मायसिटी रहिवासी | फोटोसह तक्रारी आणि रहिवाशांची फीड, डोंबिवली",
        "meta_description": "रनवाल मायसिटी, डोंबिवलीच्या रहिवाशांचे ॲप. फोटोसह मेंटेनन्स तक्रार नोंदवा, ती सुटेपर्यंत पाठपुरावा करा, आणि नोंदवलेल्या समस्यांची तपासलेली फीड वाचा.",
        "skip": "मजकुराकडे जा", "language": "भाषा",
        "nav_how": "हे कसे काम करते", "nav_why": "रहिवासी हे का वापरतात", "nav_faq": "प्रश्न", "nav_signin": "साइन इन",
        "h1": "रनवाल मायसिटी",
        "lead": "रनवाल मायसिटी, डोंबिवलीच्या दहा टॉवर्समधील रहिवाशांचे ॲप.",
        "lead_2": "फोटोसह समस्या नोंदवा, ती सुटेपर्यंत पाठपुरावा करा, आणि शेजाऱ्यांनी नोंदवलेले वाचा.",
        "cta_signin": "मोबाइलने साइन इन करा",
        "signin_note": "SMS कोडने साइन इन करा. तुमचा नंबर इतर रहिवाशांना कधीच दिसत नाही.",
        "alt_towers": "लँडस्केप केलेल्या अंगणाभोवती रनवाल मायसिटीचे टॉवर्स",
        "alt_playground": "हिरवळीवर घसरगुंडी असलेले मुलांचे खेळाचे मैदान",
        "alt_podium": "टॉवर्सच्या मधील पोडियमवर बसण्याची जागा",
        "alt_clubhouse": "क्लबहाऊस आणि त्याची हिरवळ",
        "alt_skyline": "सोसायटीबाहेरून दिसणारे टॉवर्स",
        "alt_garden": "टॉवर्सजवळ हिरवळ आणि झाडांची बाग",
        "how_title": "हे कसे काम करते",
        "how_intro": "इथे तीन गोष्टी होतात. टप्पे पाहण्यासाठी एक निवडा; वेळा एका सामान्य दिवसाचे उदाहरण आहेत.",
        "tab_complaint": "तक्रार नोंदवा", "tab_feed": "समस्या नोंदवा", "tab_staff": "कर्मचारी शिफ्ट",
        "slip_complaint": ([("सकाळी", [("तुम्ही दोन फोटोसह पाठवता", "9:10"), ("टॉवर ॲडमिन रवीला सोपवतात", "9:40"),
                                       ("रवी काम सुरू करतो", "10:05")]),
                            ("दुपारी", [("वॉशर बदलला, सुटली म्हणून नोंदवली", "1:20")]),
                            ("संध्याकाळी", [("तुम्ही तपासून बंद करता", "6:30")])],
                           ("सुटली, प्रत्येक टप्पा नोंदवलेला", "4 ता 10 मि")),
        "slip_feed": ([("पहिला दिवस", [("तुम्ही काय, कुठे, कधी लिहिता, फोटोसह", "10 मि"), ("ती तपासणीची वाट पाहते", "खासगी")]),
                       ("दुसरा दिवस", [("मॉडरेटर कंटेंट नियमांनुसार तपासतात", "1 दिवस"),
                                       ("\"सत्यापित रहिवासी\" म्हणून प्रकाशित", "रहिवाशांसाठी")])],
                      ("सर्व रहिवासी पाहतात, तुमचे नाव लपलेले राहते", "1 दिवस")),
        "slip_staff": ([("सुरुवात", [("सोसायटीत सेल्फी, ठिकाणाची तपासणी", "8:00"), ("कामांची यादी, सर्वात तातडीची आधी", "8:05")]),
                        ("दिवसभर", [("सुरू करा, दुरुस्त करा, सुटली म्हणून नोंदवा", "प्रत्येक काम")]),
                        ("शेवट", [("शिफ्ट संपवण्यासाठी सेल्फी", "4:00"), ("टॉवर ॲडमिन सेल्फी जुळवून पाहतात", "दुसऱ्या दिवशी")])],
                       ("तास आणि कामे नोंदवलेली", "8 ता")),
        "why_title": "समस्या दिसतात, आणि सुटतात",
        "blocks": [("फोन कॉल नाही, फोटो", ["प्रत्येक तक्रारीत 5 पर्यंत फोटो", "तुमचा फ्लॅट किंवा कॉमन एरिया",
                                          "श्रेणी, म्हणजे योग्य व्यक्तीपर्यंत पोहोचते", "WhatsApp च्या संभाषणात काहीही हरवत नाही"]),
                   ("प्रत्येक टप्पा नोंदवलेला", ["कोणाला सोपवली", "काम कधी सुरू झाले", "कधी आणि कोणी सोडवली",
                                                "तुम्ही बंद करा, किंवा सुटली नाही ते सांगा"]),
                   ("नोंदवलेली तथ्ये, अफवा नाहीत", ["काय, कुठे, कधी, आणि काय वचन होते", "आधी मॉडरेटर तपासतात",
                                                   "नावे, चेहरे किंवा वैयक्तिक माहिती नाही", "तीन तक्रारींनंतर पोस्ट तपासणीपर्यंत लपते"]),
                   ("गोपनीयतेची काळजी", ["फक्त तुमचे नाव, मोबाइल आणि फ्लॅट", "तुमचा नंबर रहिवाशांना कधीच दिसत नाही",
                                        "पोस्टवर \"सत्यापित रहिवासी\" दिसते", "English, हिन्दी आणि मराठी"])],
        "faq_title": "प्रश्न",
        "fact_1": "सर्व दहा टॉवर्समधील मालक आणि भाडेकरूंसाठी",
        "fact_2": "कोणत्याही फोनच्या ब्राउझरमध्ये, काहीही इन्स्टॉल नाही",
        "fact_3": "English, हिन्दी आणि मराठी",
        "q1": "हे कोण वापरू शकते?", "a1": "रनवाल मायसिटी, डोंबिवलीचे रहिवासी: मालक आणि भाडेकरू. मोबाइल नंबरने साइन इन करा, तुमचा फ्लॅट सांगा, आणि सोसायटी ॲडमिन साधारणपणे एका दिवसात मंजुरी देतात.",
        "q2": "हे डेव्हलपरचे ॲप आहे का?", "a2": "नाही. हे रहिवाशांचे स्वतंत्र ॲप आहे. ते रनवाल किंवा बिल्डर चालवत नाहीत, आणि त्यांच्याशी त्याचा संबंध नाही.",
        "q3": "माझा मोबाइल नंबर कोण पाहतो?", "a3": "फक्त सोसायटी ॲडमिन, एकदा, तुमची नोंदणी तपासण्यासाठी. इतर रहिवाशांना फक्त नावे दिसतात, आणि फीड पोस्टवर ती कोणी लिहिली हे कधीच दिसत नाही.",
        "q4": "फीडमध्ये काय प्रकाशित होते?", "a4": "तथ्यांवर आधारित, नोंदवलेल्या समस्या: काय, कुठे, कधी, आणि काय वचन होते. नावे, चेहरे, शिवीगाळ किंवा पुराव्याशिवाय आरोप असलेल्या पोस्ट प्रकाशित होत नाहीत.",
        "q5": "कोणत्या भाषा आहेत?", "a5": "English, हिन्दी आणि मराठी. कोणत्याही स्क्रीनच्या वर निवडा; ॲप ती तुमच्या फोनवर लक्षात ठेवते.",
        "faq_link": "हे कसे काम करते ते पाहा",
        "join_title": "रनवाल मायसिटीत राहता?",
        "join_text": "तुमच्या मोबाइल नंबरने साइन इन करा आणि तुमचा फ्लॅट सांगा. सोसायटी ॲडमिन साधारणपणे एका दिवसात मंजुरी देतात.",
        "foot_lang": "भाषा", "foot_about": "माहिती",
        "footer_independent": "रहिवाशांचे स्वतंत्र ॲप. ते रनवाल किंवा बिल्डर चालवत नाहीत, आणि त्यांच्याशी त्याचा संबंध नाही.",
        "footer_place": "रनवाल मायसिटी, कल्याण-शीळ रोड, डोंबिवली पूर्व, ठाणे, महाराष्ट्र.",
    },
}

e = html.escape


def zigzag(teeth=24, depth=10):
    """clip-path points: straight top, a torn receipt edge at the bottom."""
    pts = ["0 0", "100% 0", "100% 100%"]
    for i in range(teeth * 2 - 1, 0, -1):
        y = f"calc(100% - {depth}px)" if i % 2 else "100%"
        pts.append("%.3f%% %s" % (i * 50 / teeth, y))
    pts.append("0 100%")
    return ", ".join(pts)


def slip(flow, data, hidden):
    groups, (total, total_value) = data
    rows = []
    for name, items in groups:
        rows.append(f"<h3>{e(name)}</h3>")
        rows += [f'<p class="row"><span>{e(t)}</span><span class="dots" aria-hidden="true"></span><span>{e(v)}</span></p>'
                 for t, v in items]
    return (f'<div class="slip{" show" if not hidden else ""}" id="slip-{flow}" role="tabpanel" aria-labelledby="tab-{flow}">'
            + "".join(rows) + f'<p class="total"><span>{e(total)}</span><span>{e(total_value)}</span></p></div>')


def blocks(t):
    out = []
    for (title, lines), (img, w, h, alt) in zip(t["blocks"], BLOCK_PHOTOS):
        items = "".join(f"<li>{e(x)}</li>" for x in lines)
        out.append(f'<div class="block" data-rise><h3>{e(title)}</h3><div class="pair">'
                   f'<img src="/images/{img}.jpg" width="{w}" height="{h}" alt="{e(t[alt])}" loading="lazy"><ul>{items}</ul></div></div>')
    return "\n        ".join(out)


def json_ld(t):
    """WebSite + WebApplication: what the site is, for search engines."""
    return json.dumps({
        "@context": "https://schema.org",
        "@graph": [
            {"@type": "WebSite", "@id": SITE + "/#website", "url": SITE + "/", "name": "Runwal MyCity Residents",
             "inLanguage": ["en", "hi", "mr"]},
            {"@type": "WebApplication", "name": "Runwal MyCity Residents", "url": SITE + t["path"],
             "description": t["meta_description"], "inLanguage": t["path"].strip("/") or "en",
             "applicationCategory": "LifestyleApplication", "operatingSystem": "Web browser",
             "areaServed": {"@type": "Place", "name": "Runwal MyCity, Dombivli East, Thane, Maharashtra, India"}},
        ],
    }, ensure_ascii=False)


def render(lang, t):
    page = (HERE / "template.html").read_text(encoding="utf-8")
    values = {**{k: e(v) for k, v in t.items() if isinstance(v, str)}, "lang": lang, "logo": LOGO, "json_ld": json_ld(t),
              "zigzag": zigzag(), "blocks": blocks(t),
              "slips": "\n          ".join(slip(f, t["slip_" + f], i > 0) for i, f in enumerate(("complaint", "feed", "staff"))),
              "cur_en": ' aria-current="page"' if lang == "en" else "",
              "cur_hi": ' aria-current="page"' if lang == "hi" else "",
              "cur_mr": ' aria-current="page"' if lang == "mr" else ""}
    out = re.sub(r"\{\{(\w+)\}\}", lambda m: values[m.group(1)], page)
    assert "{{" not in out, "a placeholder has no text"
    return out


if __name__ == "__main__":
    targets = {"en": PUBLIC / "home.html", "hi": PUBLIC / "hi" / "index.html", "mr": PUBLIC / "mr" / "index.html"}
    keys = set(TEXTS["en"])
    for lang, t in TEXTS.items():
        assert set(t) == keys, f"{lang} texts differ: {keys ^ set(t)}"
        assert len(t["blocks"]) == len(BLOCK_PHOTOS)
        targets[lang].parent.mkdir(parents=True, exist_ok=True)
        targets[lang].write_text(render(lang, t), encoding="utf-8")
        print("wrote", targets[lang].relative_to(HERE.parent))
