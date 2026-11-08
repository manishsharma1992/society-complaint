"""Writes the public homepage in English, Hindi and Marathi from template.html.

Run after changing a text:  python3 frontend/landing/build_landing.py
Output: public/home.html (served at /), public/hi/index.html, public/mr/index.html.
"""
import html
import shutil
import json
import re
from pathlib import Path

from legal_texts import LEGAL, UI, UPDATED

HERE = Path(__file__).parent
PUBLIC = HERE.parent / "public"
SITE = "https://runwalltdfakepromises.com"

LOGO = ('<svg viewBox="72 24 112 216" aria-hidden="true" focusable="false"><path fill="currentColor" fill-rule="evenodd" '
        'd="M92 24 H164 A20 20 0 0 1 184 44 V188 A20 20 0 0 1 164 208 H126 L92 238 V208 A20 20 0 0 1 72 188 V44 '
        'A20 20 0 0 1 92 24 Z M96 56 h24 v24 h-24 Z M136 56 h24 v24 h-24 Z M96 100 h24 v24 h-24 Z M136 100 h24 v24 '
        'h-24 Z M96 144 h24 v24 h-24 Z M136 144 h24 v24 h-24 Z"/></svg>')


TEXTS = {
    "en": {
        "path": "/", "og_locale": "en_IN",
        "meta_title": "Runwal MyCity Residents: complaints and feed, Dombivli",
        "meta_description": "The residents’ platform for Runwal MyCity, Dombivli: document what was promised and what is there, raise complaints with photos, and be heard.",
        "skip": "Skip to content", "language": "Language",
        "nav_how": "How it works", "nav_why": "Speak up, safely", "nav_faq": "Questions", "nav_signin": "Sign in",
        "h1": "You paid for a promise.",
        "lead": "Show what was promised, what is really there, and be heard.",
        "cta_signin": "Sign in with your mobile",
        "signin_note": "Sign in with an SMS code. Your number is never shown to other residents.",
        "alt_podium": "Seating on the podium between the towers",
        "alt_clubhouse": "The clubhouse and its lawn",
        "alt_playground": "Children’s playground with slides on the lawn",
        "alt_towers": "Towers of Runwal MyCity around the landscaped courtyard",
        "alt_skyline": "The towers seen from outside the society",
        "alt_garden": "Garden with lawn and palms beside the towers",
        "cta_voice": "Raise your voice",
        "statement": "Brochures showed gardens, walls, a clubhouse and finished roads. Many residents feel that what they paid for is not what they live with.",
        "story_title": "Put it on record",
        "story_intro": "Every post answers three questions. A moderator checks it before anyone sees it.",
        "s1_t": "What was promised",
        "s1_d": "Quote the brochure, the agreement or the approved plan, with the page.",
        "s2_t": "What is there today",
        "s2_d": "Add your photos, with where and when you took them.",
        "s3_t": "Residents see it",
        "s3_d": "Published as “Verified resident”. Your name and number stay hidden.",
        "card_title": "Example post",
        "card_status": "Waiting for review",
        "card_published": "Published",
        "card_promised_l": "Promised",
        "card_promised": "A boundary wall around Phase 1",
        "card_source_l": "Source",
        "card_source": "Brochure, page —",
        "card_today_l": "Today",
        "card_photo": "Your photo",
        "card_seen_l": "Shown as",
        "card_seen": "Verified resident",
        "p1_t": "Anonymous to everyone",
        "p1_d": "Posts show “Verified resident”, never your name.",
        "p2_t": "Checked before it is shown",
        "p2_d": "A moderator reads every post against the content policy first.",
        "p3_t": "Your number stays private",
        "p3_d": "Only the society admin sees it, once, to approve you.",
        "band_line": "This is our home. It should be what we were promised.",
        "nav_app": "Complaints",
        "app_title": "Complaints, for the whole society",
        "app_intro": "Raise a problem in a minute and follow it until it is fixed. Built for residents, maintenance staff and admins.",
        "app_sample": "Screens from the app, with sample data.",
        "walk": [["Snap and send", "Add up to 5 photos, choose your flat or a common area, and say what is wrong.", "Raising a complaint: adding photos"], ["All your complaints in one place", "See the status of each one, and which are overdue.", "The list of complaints with their status"], ["Every step on record", "Who it was assigned to, when work started, every photo and comment.", "One complaint with its photos and history"], ["Staff see their jobs", "The most urgent first. Staff start their shift with a selfie at the society.", "A staff member’s list of jobs"], ["Admins see the whole picture", "What was fixed, how fast, by whom, and the hours on shift.", "The staff report for admins"]],
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
                                    ("Published as “Verified resident”", "to residents")])],
                      ("Every resident sees it; your name stays hidden", "1 day")),
        "slip_staff": ([("Start", [("Selfie at the society, location checked", "8:00 am"), ("Jobs listed, most urgent first", "8:05 am")]),
                        ("During the day", [("Start, fix, mark resolved", "each job")]),
                        ("End", [("Selfie to end the shift", "4:00 pm"), ("A tower admin compares the selfies", "next day")])],
                       ("Hours and fixes on record", "8 h")),
        "faq_title": "Questions",
        "fact_1": "For owners and tenants of all ten towers",
        "fact_2": "Works in the browser on any phone, nothing to install",
        "fact_3": "English, हिन्दी and मराठी",
        "q1": "Who can use it?", "a1": "Residents of Runwal MyCity, Dombivli: owners and tenants. Sign in with your mobile number, tell us your flat, and the society admin approves you, usually within a day.",
        "q2": "Is this the developer’s app?", "a2": "No. It is an independent app for residents. It is not run by, or affiliated with, Runwal or the builder.",
        "q3": "Who sees my mobile number?", "a3": "Nobody except the society admin, once, to check your sign-up. Other residents see names only, and feed posts never show who wrote them.",
        "q4": "What gets published in the feed?", "a4": "Factual, documented issues: what, where, when, and what was promised. Posts with names, faces, abuse or unproven accusations are not published.",
        "q5": "Which languages does it support?", "a5": "English, हिन्दी and मराठी. Choose at the top of any screen; the app remembers it on your phone.",
        "faq_link": "See how it works",
        "join_title": "Your voice counts.",
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
        "nav_how": "यह कैसे काम करता है", "nav_why": "सुरक्षित होकर बोलें", "nav_faq": "सवाल", "nav_signin": "साइन इन",
        "h1": "आपने एक वादे के लिए पैसे दिए।",
        "lead": "दिखाइए क्या वादा था, असल में क्या है, और अपनी बात कहिए।",
        "cta_signin": "मोबाइल से साइन इन करें",
        "signin_note": "SMS कोड से साइन इन करें। आपका नंबर दूसरे निवासियों को कभी नहीं दिखता।",
        "alt_podium": "टावरों के बीच पोडियम पर बैठने की जगह",
        "alt_clubhouse": "क्लबहाउस और उसका लॉन",
        "alt_playground": "लॉन पर स्लाइड वाला बच्चों का खेल का मैदान",
        "alt_towers": "लैंडस्केप वाले आँगन के चारों ओर रनवाल मायसिटी के टावर",
        "alt_skyline": "सोसाइटी के बाहर से दिखते टावर",
        "alt_garden": "टावरों के पास लॉन और पेड़ों वाला बगीचा",
        "cta_voice": "अपनी आवाज़ उठाएँ",
        "statement": "ब्रोशर में बगीचे, दीवारें, क्लबहाउस और पक्की सड़कें दिखाई गई थीं। कई निवासियों को लगता है कि जिसके लिए पैसे दिए, वह वैसा नहीं है जैसा आज वे जी रहे हैं।",
        "story_title": "इसे दर्ज करें",
        "story_intro": "हर पोस्ट तीन सवालों का जवाब देती है। किसी के देखने से पहले मॉडरेटर इसे जाँचते हैं।",
        "s1_t": "क्या वादा था",
        "s1_d": "ब्रोशर, एग्रीमेंट या मंज़ूर नक्शे से, पेज के साथ लिखें।",
        "s2_t": "आज क्या है",
        "s2_d": "अपनी फ़ोटो जोड़ें, कहाँ और कब ली गई।",
        "s3_t": "निवासी देखते हैं",
        "s3_d": "“सत्यापित निवासी” के नाम से प्रकाशित। आपका नाम और नंबर छिपे रहते हैं।",
        "card_title": "उदाहरण पोस्ट",
        "card_status": "जाँच के इंतज़ार में",
        "card_published": "प्रकाशित",
        "card_promised_l": "वादा",
        "card_promised": "फ़ेज़ 1 के चारों ओर बाउंड्री वॉल",
        "card_source_l": "स्रोत",
        "card_source": "ब्रोशर, पेज —",
        "card_today_l": "आज",
        "card_photo": "आपकी फ़ोटो",
        "card_seen_l": "किस नाम से",
        "card_seen": "सत्यापित निवासी",
        "p1_t": "सबके लिए गुमनाम",
        "p1_d": "पोस्ट पर “सत्यापित निवासी” दिखता है, आपका नाम कभी नहीं।",
        "p2_t": "दिखने से पहले जाँच",
        "p2_d": "मॉडरेटर हर पोस्ट को पहले कंटेंट नियमों से पढ़ते हैं।",
        "p3_t": "आपका नंबर निजी",
        "p3_d": "सिर्फ़ सोसाइटी एडमिन, एक बार, आपको मंज़ूरी देने के लिए देखते हैं।",
        "band_line": "यह हमारा घर है। यह वैसा होना चाहिए जैसा वादा किया गया था।",
        "nav_app": "शिकायतें",
        "app_title": "पूरी सोसाइटी के लिए शिकायतें",
        "app_intro": "एक मिनट में समस्या दर्ज करें और ठीक होने तक नज़र रखें। निवासियों, मेंटेनेंस स्टाफ़ और एडमिन के लिए।",
        "app_sample": "ऐप की स्क्रीन, नमूना डेटा के साथ।",
        "walk": [["फ़ोटो लें और भेजें", "5 तक फ़ोटो जोड़ें, अपना फ़्लैट या कॉमन एरिया चुनें, और बताएँ क्या ख़राब है।", "शिकायत दर्ज करना: फ़ोटो जोड़ना"], ["आपकी सारी शिकायतें एक जगह", "हर शिकायत की स्थिति देखें, और कौन-सी देर से चल रही हैं।", "स्थिति के साथ शिकायतों की सूची"], ["हर कदम दर्ज", "किसे सौंपी गई, काम कब शुरू हुआ, हर फ़ोटो और टिप्पणी।", "फ़ोटो और इतिहास के साथ एक शिकायत"], ["स्टाफ़ अपने काम देखते हैं", "सबसे ज़रूरी पहले। स्टाफ़ सोसाइटी में सेल्फ़ी के साथ शिफ़्ट शुरू करते हैं।", "स्टाफ़ के कामों की सूची"], ["एडमिन पूरी तस्वीर देखते हैं", "क्या ठीक हुआ, कितनी जल्दी, किसने, और शिफ़्ट के घंटे।", "एडमिन के लिए स्टाफ़ रिपोर्ट"]],
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
                                      ("“सत्यापित निवासी” के नाम से प्रकाशित", "निवासियों के लिए")])],
                      ("सब निवासी देखते हैं, आपका नाम छिपा रहता है", "1 दिन")),
        "slip_staff": ([("शुरुआत", [("सोसाइटी में सेल्फ़ी, जगह की जाँच", "8:00"), ("काम की सूची, सबसे ज़रूरी पहले", "8:05")]),
                        ("दिन भर", [("शुरू करें, ठीक करें, ठीक हुई बताएँ", "हर काम")]),
                        ("अंत", [("शिफ़्ट ख़त्म करने की सेल्फ़ी", "4:00"), ("टावर एडमिन सेल्फ़ी मिलाते हैं", "अगले दिन")])],
                       ("घंटे और काम दर्ज", "8 घं")),
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
        "join_title": "आपकी आवाज़ मायने रखती है।",
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
        "nav_how": "हे कसे काम करते", "nav_why": "सुरक्षितपणे बोला", "nav_faq": "प्रश्न", "nav_signin": "साइन इन",
        "h1": "तुम्ही एका वचनासाठी पैसे दिले.",
        "lead": "काय वचन होते, प्रत्यक्षात काय आहे ते दाखवा, आणि तुमचे म्हणणे मांडा.",
        "cta_signin": "मोबाइलने साइन इन करा",
        "signin_note": "SMS कोडने साइन इन करा. तुमचा नंबर इतर रहिवाशांना कधीच दिसत नाही.",
        "alt_podium": "टॉवर्सच्या मधील पोडियमवर बसण्याची जागा",
        "alt_clubhouse": "क्लबहाऊस आणि त्याची हिरवळ",
        "alt_playground": "हिरवळीवर घसरगुंडी असलेले मुलांचे खेळाचे मैदान",
        "alt_towers": "लँडस्केप केलेल्या अंगणाभोवती रनवाल मायसिटीचे टॉवर्स",
        "alt_skyline": "सोसायटीबाहेरून दिसणारे टॉवर्स",
        "alt_garden": "टॉवर्सजवळ हिरवळ आणि झाडांची बाग",
        "cta_voice": "तुमचा आवाज उठवा",
        "statement": "ब्रोशरमध्ये बागा, भिंती, क्लबहाऊस आणि पक्के रस्ते दाखवले होते. अनेक रहिवाशांना वाटते की ज्यासाठी पैसे दिले, ते आज ते जगत असलेल्या वास्तवात दिसत नाही.",
        "story_title": "याची नोंद करा",
        "story_intro": "प्रत्येक पोस्ट तीन प्रश्नांची उत्तरे देते. कोणी पाहण्यापूर्वी मॉडरेटर ती तपासतात.",
        "s1_t": "काय वचन होते",
        "s1_d": "ब्रोशर, करार किंवा मंजूर नकाशातून, पानासह लिहा.",
        "s2_t": "आज काय आहे",
        "s2_d": "तुमचे फोटो जोडा, कुठे आणि कधी काढले ते सांगा.",
        "s3_t": "रहिवासी पाहतात",
        "s3_d": "“सत्यापित रहिवासी” म्हणून प्रकाशित. तुमचे नाव आणि नंबर लपलेले राहतात.",
        "card_title": "उदाहरण पोस्ट",
        "card_status": "तपासणीच्या प्रतीक्षेत",
        "card_published": "प्रकाशित",
        "card_promised_l": "वचन",
        "card_promised": "फेज 1 भोवती कंपाउंड भिंत",
        "card_source_l": "स्रोत",
        "card_source": "ब्रोशर, पान —",
        "card_today_l": "आज",
        "card_photo": "तुमचा फोटो",
        "card_seen_l": "कोणत्या नावाने",
        "card_seen": "सत्यापित रहिवासी",
        "p1_t": "सर्वांसाठी अनामिक",
        "p1_d": "पोस्टवर “सत्यापित रहिवासी” दिसते, तुमचे नाव कधीच नाही.",
        "p2_t": "दिसण्यापूर्वी तपासणी",
        "p2_d": "मॉडरेटर प्रत्येक पोस्ट आधी कंटेंट नियमांनुसार वाचतात.",
        "p3_t": "तुमचा नंबर खासगी",
        "p3_d": "फक्त सोसायटी ॲडमिन, एकदा, तुम्हाला मंजुरी देण्यासाठी पाहतात.",
        "band_line": "हे आमचे घर आहे. ते वचन दिल्याप्रमाणे असायला हवे.",
        "nav_app": "तक्रारी",
        "app_title": "संपूर्ण सोसायटीसाठी तक्रारी",
        "app_intro": "एका मिनिटात समस्या नोंदवा आणि ती सुटेपर्यंत पाठपुरावा करा. रहिवासी, मेंटेनन्स कर्मचारी आणि ॲडमिनसाठी.",
        "app_sample": "ॲपच्या स्क्रीन, नमुना माहितीसह.",
        "walk": [["फोटो काढा आणि पाठवा", "5 पर्यंत फोटो जोडा, तुमचा फ्लॅट किंवा कॉमन एरिया निवडा, आणि काय बिघडले ते सांगा.", "तक्रार नोंदवणे: फोटो जोडणे"], ["तुमच्या सर्व तक्रारी एकाच ठिकाणी", "प्रत्येक तक्रारीची स्थिती पाहा, आणि कोणत्या उशिरा आहेत.", "स्थितीसह तक्रारींची यादी"], ["प्रत्येक टप्पा नोंदवलेला", "कोणाला सोपवली, काम कधी सुरू झाले, प्रत्येक फोटो आणि टिप्पणी.", "फोटो आणि इतिहासासह एक तक्रार"], ["कर्मचारी त्यांची कामे पाहतात", "सर्वात तातडीची आधी. कर्मचारी सोसायटीत सेल्फीसह शिफ्ट सुरू करतात.", "कर्मचाऱ्याच्या कामांची यादी"], ["ॲडमिनना संपूर्ण चित्र दिसते", "काय सुटले, किती लवकर, कोणी, आणि शिफ्टचे तास.", "ॲडमिनसाठी कर्मचारी अहवाल"]],
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
                                       ("“सत्यापित रहिवासी” म्हणून प्रकाशित", "रहिवाशांसाठी")])],
                      ("सर्व रहिवासी पाहतात, तुमचे नाव लपलेले राहते", "1 दिवस")),
        "slip_staff": ([("सुरुवात", [("सोसायटीत सेल्फी, ठिकाणाची तपासणी", "8:00"), ("कामांची यादी, सर्वात तातडीची आधी", "8:05")]),
                        ("दिवसभर", [("सुरू करा, दुरुस्त करा, सुटली म्हणून नोंदवा", "प्रत्येक काम")]),
                        ("शेवट", [("शिफ्ट संपवण्यासाठी सेल्फी", "4:00"), ("टॉवर ॲडमिन सेल्फी जुळवून पाहतात", "दुसऱ्या दिवशी")])],
                       ("तास आणि कामे नोंदवलेली", "8 ता")),
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
        "join_title": "तुमचा आवाज महत्त्वाचा आहे.",
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


# the app's own screens, in walkthrough order (images/app/<lang>/<name>.jpg, captured with sample data)
SCREENS = ["raise", "complaints", "complaint", "jobs", "report"]


def walk(lang, t):
    """The walkthrough: each step with its own screen (shown inline on a phone), and the sticky phone's screens."""
    steps, screens = [], []
    for i, ((title, text, alt), name) in enumerate(zip(t["walk"], SCREENS)):
        src = f'/images/app/{lang}/{name}.jpg'
        on = ' on' if i == 0 else ''
        steps.append(f'<li class="wstep{on}"><h3>{e(title)}</h3><p>{e(text)}</p>'
                     f'<div class="device"><img src="{src}" width="780" height="1560" alt="{e(alt)}" loading="lazy"></div></li>')
        screens.append(f'<img class="{on.strip()}" src="{src}" width="780" height="1560" alt="" loading="lazy">')
    return "\n          ".join(steps), "".join(screens)


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


def md(text):
    """The legal texts' Markdown subset: paragraphs (line breaks kept), "- " lists, "1. " lists with "   - " sub-items, **bold**."""
    inline = lambda x: re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", e(x))
    out = []
    for block in text.strip().split("\n\n"):
        lines = block.split("\n")
        if all(l.startswith("- ") for l in lines):
            out.append("<ul>" + "".join(f"<li>{inline(l[2:])}</li>" for l in lines) + "</ul>")
        elif re.match(r"\d+\. ", lines[0]):
            items = []
            for l in lines:
                if re.match(r"\d+\. ", l):
                    items.append([inline(l.split(". ", 1)[1]), []])
                else:
                    items[-1][1].append(f"<li>{inline(l.strip()[2:])}</li>")
            out.append("<ol>" + "".join(f"<li>{t}" + (f"<ul>{''.join(sub)}</ul>" if sub else "") + "</li>" for t, sub in items) + "</ol>")
        else:
            out.append("<p>" + "<br>".join(inline(l) for l in lines) + "</p>")
    return "\n".join(out)


assert md("A **b** & c\n\n- x\n- y\n\n1. one\n   - sub\n2. two") == (
    "<p>A <strong>b</strong> &amp; c</p>\n<ul><li>x</li><li>y</li></ul>\n"
    "<ol><li>one<ul><li>sub</li></ul></li><li>two</li></ol>")


def render_legal(lang, t):
    page = (HERE / "legal.html").read_text(encoding="utf-8")
    ui = UI[lang]
    note = (f'<p class="note">{e(ui["legal_note"])} <a href="/legal/" hreflang="en" lang="en">{e(ui["legal_note_link"])}</a></p>'
            if ui["legal_note"] else "")
    values = {**{k: e(v) for k, v in ui.items()}, "lang": lang, "logo": LOGO, "path": t["path"], "language": e(t["language"]),
              "legal_path": legal_path(lang), "updated": e(UPDATED[lang]), "note": note,
              "footer_independent": e(t["footer_independent"]),
              "toc": "".join(f'<li><a href="#{sid}">{e(title)}</a></li>' for sid, title, _ in LEGAL[lang]),
              "sections": "\n    ".join(f'<section id="{sid}" aria-labelledby="{sid}-h"><h2 id="{sid}-h">{e(title)}</h2>{md(body)}</section>'
                                          for sid, title, body in LEGAL[lang]),
              "cur_en": ' aria-current="page"' if lang == "en" else "",
              "cur_hi": ' aria-current="page"' if lang == "hi" else "",
              "cur_mr": ' aria-current="page"' if lang == "mr" else ""}
    out = re.sub(r"\{\{(\w+)\}\}", lambda m: values[m.group(1)], page)
    assert "{{" not in out, "a placeholder has no text"
    return out


def legal_path(lang):
    return "/legal/" if lang == "en" else f"/{lang}/legal/"


def render(lang, t):
    page = (HERE / "template.html").read_text(encoding="utf-8")
    values = {**{k: e(v) for k, v in t.items() if isinstance(v, str)}, "lang": lang, "logo": LOGO, "json_ld": json_ld(t),
              "zigzag": zigzag(),
              "statement": " ".join(f'<span class="w">{e(w)}</span>' for w in t["statement"].split()),
              "slips": "\n          ".join(slip(f, t["slip_" + f], i > 0) for i, f in enumerate(("complaint", "feed", "staff"))),
              "walk_steps": walk(lang, t)[0], "walk_screens": walk(lang, t)[1],
              "legal_nav": e(UI[lang]["legal_title"]),
              "legal_links": "".join(f'<a href="{legal_path(lang)}#{sid}">{e(title)}</a>' for sid, title, _ in LEGAL[lang]),
              "cur_en": ' aria-current="page"' if lang == "en" else "",
              "cur_hi": ' aria-current="page"' if lang == "hi" else "",
              "cur_mr": ' aria-current="page"' if lang == "mr" else ""}
    out = re.sub(r"\{\{(\w+)\}\}", lambda m: values[m.group(1)], page)
    assert "{{" not in out, "a placeholder has no text"
    return out


if __name__ == "__main__":
    targets = {"en": PUBLIC / "home.html", "hi": PUBLIC / "hi" / "index.html", "mr": PUBLIC / "mr" / "index.html"}
    # Motion (motion.dev), served from this site; installed as a dev dependency of the frontend
    (PUBLIC / "vendor").mkdir(exist_ok=True)
    shutil.copy(HERE.parent / "node_modules" / "motion" / "dist" / "motion.js", PUBLIC / "vendor" / "motion.js")
    keys = set(TEXTS["en"])
    for lang, t in TEXTS.items():
        assert set(t) == keys, f"{lang} texts differ: {keys ^ set(t)}"
        assert len(t["walk"]) == len(SCREENS)
        targets[lang].parent.mkdir(parents=True, exist_ok=True)
        targets[lang].write_text(render(lang, t), encoding="utf-8")
        print("wrote", targets[lang].relative_to(HERE.parent))
        legal = PUBLIC / legal_path(lang).strip("/") / "index.html"
        assert [sid for sid, _, _ in LEGAL[lang]] == [sid for sid, _, _ in LEGAL["en"]], f"{lang} legal sections differ"
        legal.parent.mkdir(parents=True, exist_ok=True)
        legal.write_text(render_legal(lang, t), encoding="utf-8")
        print("wrote", legal.relative_to(HERE.parent))
