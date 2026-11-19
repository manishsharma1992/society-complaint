"""The legal pages (/legal/, /hi/legal/, /mr/legal/), one entry per section, in a small Markdown subset:
paragraphs, "- " lists, "1. " lists with "   - " sub-items, **bold**. English prevails; hi and mr are translations.
Have every change reviewed by the lawyer before it goes live.
"""

UPDATED = {"en": "Last updated: 19 November 2026", "hi": "अंतिम अपडेट: 19 नवंबर 2026", "mr": "शेवटचा बदल: 19 नोव्हेंबर 2026"}

UI = {
    "en": {"legal_title": "Legal", "legal_note": "", "legal_note_link": "", "back_home": "Back to the homepage",
           "legal_meta": "Disclaimer, Terms of Use, Privacy Policy, Community Guidelines and Grievance Redressal of the Runwal MyCity residents’ platform."},
    "hi": {"legal_title": "कानूनी जानकारी", "legal_note": "यह अंग्रेज़ी पाठ का अनुवाद है। दोनों में अंतर होने पर अंग्रेज़ी पाठ मान्य होगा।",
           "legal_note_link": "अंग्रेज़ी पाठ", "back_home": "होमपेज पर वापस",
           "legal_meta": "रनवाल मायसिटी निवासी मंच का अस्वीकरण, उपयोग की शर्तें, गोपनीयता नीति, सामुदायिक दिशानिर्देश और शिकायत निवारण।"},
    "mr": {"legal_title": "कायदेशीर माहिती", "legal_note": "हा इंग्रजी मजकुराचा अनुवाद आहे. दोन्हींत फरक असल्यास इंग्रजी मजकूर ग्राह्य धरला जाईल.",
           "legal_note_link": "इंग्रजी मजकूर", "back_home": "होमपेजवर परत",
           "legal_meta": "रनवाल मायसिटी रहिवासी व्यासपीठाचे अस्वीकरण, वापराच्या अटी, गोपनीयता धोरण, सामुदायिक मार्गदर्शक तत्त्वे आणि तक्रार निवारण."},
}

LEGAL = {
    "en": [
        ("disclaimer", "Disclaimer", """
This website (runwalltdfakepromises.com) is an independent, resident-driven platform created by and for owners and tenants of Runwal MyCity (and nearby towers) in the Dombivli / Diva area. It is **not** affiliated with, endorsed by, or connected to Runwal Group, Horizon Projects Private Limited, or any of their entities.

All posts, photographs, and comments are submitted by individual residents. The operators of this website do not independently verify every claim. Content reflects the personal observations and experiences of the persons who submitted it. It is provided for community information and documentation purposes only.

- Nothing on this site constitutes legal advice, an official complaint, or a finding of fact by any authority.
- Residents are encouraged to raise formal complaints with MahaRERA, the concerned consumer commission, the society/federation, and other appropriate authorities.
- The website operators do not guarantee the accuracy, completeness, or currency of any post.
- Photographs are submitted by residents and depict conditions as they existed at the time of capture. Conditions may change.

Use of the “Runwal” name or project names is solely for identification and descriptive purposes. All trademarks remain the property of their respective owners.

The operators reserve the right to remove or refuse any content that appears false, abusive, or unlawful.
"""),
        ("terms", "Terms of Use", """
By using this website you agree to the following:

1. **Purpose**: This is a private community documentation platform for residents of the named projects. It is not a commercial service.
2. **User-generated content**: You are solely responsible for the accuracy of any text or photograph you submit. You confirm that:
   - The content is true to the best of your knowledge;
   - You own the photographs or have the right to upload them;
   - You will not upload false, exaggerated, or malicious statements.
3. **Licence**: By posting, you grant the website operators a non-exclusive licence to display the content on the site for the purpose of community documentation.
4. **Prohibited content**: No personal abuse, hate speech, confidential personal data of others, or content that is clearly false or defamatory.
5. **Moderation**: The operators may review, edit, or remove content at their discretion and without notice.
6. **No liability**: The operators are not liable for any loss arising from reliance on content posted by residents. The site is provided “as is.”
7. **Intermediary**: The operators act as an intermediary under the Information Technology Act, 2000. They will respond to valid legal notices and court orders in accordance with applicable law.
8. **Governing law**: These terms are governed by the laws of India. Courts in Thane shall have jurisdiction.
"""),
        ("privacy", "Privacy Policy", """
This website collects limited personal data: only what is needed to verify that a user is a resident of the project, and to run the feed.

**Data collected**:

- Mobile number (for OTP sign-in and admin approval)
- Your name and your flat (for approval)
- Content you voluntarily post: the text, photographs and videos of feed posts

**Purpose**: Resident verification and moderation. Mobile numbers are visible only to designated society administrators for approval and are not published publicly. Other residents see names only; feed posts are shown as “Verified resident”, never with your name.

**Legal basis**: Consent and legitimate use for community verification (Digital Personal Data Protection Act, 2023).

**Where it is stored**: On servers in Singapore (the database on Neon, photographs and videos on Amazon Web Services Lightsail), with access limited to the website operators.

**Translation**: When someone taps “Translate” on a feed post, its text is sent to a machine translation service only to translate it: Google Cloud Translation for Marathi, Amazon Translate (Singapore) for English and Hindi. Only the text is sent, never photographs, videos, or the name and mobile number from your account. These services may process the text outside India. Translations of published feed posts are kept with the post. Please do not write personal details in the text of a post.

**Retention**: Mobile numbers are retained only as long as needed for verification and account management. You may request deletion by contacting the Grievance Officer below.

**Sharing**: We do not sell or share personal data with third parties except as required by law or court order. When sign-in codes are sent by SMS, your number is passed to the SMS provider only to deliver the code. Text that someone asks to translate is passed to the translation service only to translate it (see Translation above).

**Your rights**: You may request access, correction, or erasure of your personal data by writing to the Grievance Officer below.

**Grievance contact**: see Grievance Redressal below.

We do not use cookies for tracking. The app keeps your sign-in and your language choice in your browser’s storage, only so that the site works.
"""),
        ("guidelines", "Community Guidelines", """
Posts must:

- Clearly state what was promised (with source if possible: brochure page, agreement clause, plan, etc.)
- Describe or show what exists on the ground today
- Include recent photographs where relevant
- Avoid naming individual employees or residents in an abusive manner

We will remove:

- Content that appears deliberately false
- Personal attacks, abuse, or threats
- Photographs of private areas or identifiable faces without consent
- Any material that violates Indian law

Repeated violations may result in permanent loss of posting access.
"""),
        ("grievance", "Grievance Redressal", """
In accordance with the Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules, 2021:

**Grievance Officer**: To be announced
**Email**: To be announced
**Contact**: To be announced

Complaints regarding content or these policies will be acknowledged within 24 hours and resolved within 7 days, subject to applicable law.
"""),
    ],
    "hi": [
        ("disclaimer", "अस्वीकरण", """
यह वेबसाइट (runwalltdfakepromises.com) एक स्वतंत्र, निवासियों द्वारा चलाया जाने वाला मंच है, जिसे डोंबिवली / दिवा क्षेत्र में रनवाल मायसिटी (और आसपास के टावरों) के मालिकों और किरायेदारों ने, उन्हीं के लिए बनाया है। इसका रनवाल ग्रुप, होराइज़न प्रोजेक्ट्स प्राइवेट लिमिटेड या उनकी किसी भी संस्था से कोई संबंध, समर्थन या जुड़ाव **नहीं** है।

सभी पोस्ट, फ़ोटो और टिप्पणियाँ अलग-अलग निवासी भेजते हैं। इस वेबसाइट के संचालक हर दावे की स्वतंत्र रूप से जाँच नहीं करते। सामग्री उसे भेजने वाले व्यक्तियों के निजी अवलोकन और अनुभव दर्शाती है। यह केवल सामुदायिक जानकारी और दस्तावेज़ीकरण के लिए है।

- इस साइट पर कुछ भी कानूनी सलाह, आधिकारिक शिकायत, या किसी प्राधिकरण का तथ्य-निष्कर्ष नहीं है।
- निवासियों को सलाह दी जाती है कि वे औपचारिक शिकायतें महारेरा, संबंधित उपभोक्ता आयोग, सोसाइटी/फ़ेडरेशन और अन्य उचित प्राधिकरणों के पास दर्ज करें।
- वेबसाइट संचालक किसी भी पोस्ट की सटीकता, पूर्णता या ताज़गी की गारंटी नहीं देते।
- फ़ोटो निवासी भेजते हैं और वे खींचे जाने के समय की स्थिति दिखाती हैं। स्थिति बदल सकती है।

“रनवाल” नाम या प्रोजेक्ट के नामों का उपयोग केवल पहचान और विवरण के लिए है। सभी ट्रेडमार्क उनके संबंधित मालिकों की संपत्ति हैं।

संचालक ऐसी किसी भी सामग्री को हटाने या अस्वीकार करने का अधिकार रखते हैं जो झूठी, अपमानजनक या गैरकानूनी लगे।
"""),
        ("terms", "उपयोग की शर्तें", """
इस वेबसाइट का उपयोग करके आप निम्नलिखित से सहमत होते हैं:

1. **उद्देश्य**: यह नामित प्रोजेक्ट्स के निवासियों के लिए एक निजी सामुदायिक दस्तावेज़ीकरण मंच है। यह कोई व्यावसायिक सेवा नहीं है।
2. **उपयोगकर्ता द्वारा बनाई गई सामग्री**: आप जो भी टेक्स्ट या फ़ोटो भेजते हैं, उसकी सटीकता की पूरी ज़िम्मेदारी आपकी है। आप पुष्टि करते हैं कि:
   - आपकी जानकारी के अनुसार सामग्री सच है;
   - फ़ोटो आपकी हैं या आपको उन्हें अपलोड करने का अधिकार है;
   - आप झूठे, बढ़ा-चढ़ाकर कहे गए या दुर्भावनापूर्ण बयान अपलोड नहीं करेंगे।
3. **लाइसेंस**: पोस्ट करके आप वेबसाइट संचालकों को सामुदायिक दस्तावेज़ीकरण के लिए साइट पर सामग्री दिखाने का गैर-विशिष्ट लाइसेंस देते हैं।
4. **निषिद्ध सामग्री**: कोई व्यक्तिगत अपशब्द, नफ़रत भरी भाषा, दूसरों का गोपनीय निजी डेटा, या साफ़ तौर पर झूठी या मानहानिकारक सामग्री नहीं।
5. **मॉडरेशन**: संचालक अपने विवेक से, बिना सूचना के, सामग्री की समीक्षा कर सकते हैं, उसे संपादित कर सकते हैं या हटा सकते हैं।
6. **कोई दायित्व नहीं**: निवासियों द्वारा पोस्ट की गई सामग्री पर भरोसा करने से होने वाले किसी भी नुकसान के लिए संचालक ज़िम्मेदार नहीं हैं। साइट “जैसी है वैसी” उपलब्ध कराई जाती है।
7. **मध्यस्थ**: संचालक सूचना प्रौद्योगिकी अधिनियम, 2000 के तहत मध्यस्थ के रूप में काम करते हैं। वे लागू कानून के अनुसार वैध कानूनी नोटिस और न्यायालय के आदेशों का जवाब देंगे।
8. **लागू कानून**: ये शर्तें भारत के कानूनों के अधीन हैं। ठाणे के न्यायालयों का अधिकार क्षेत्र होगा।
"""),
        ("privacy", "गोपनीयता नीति", """
यह वेबसाइट सीमित निजी डेटा लेती है: केवल उतना, जितना यह जाँचने के लिए ज़रूरी है कि उपयोगकर्ता प्रोजेक्ट का निवासी है, और फ़ीड चलाने के लिए।

**लिया जाने वाला डेटा**:

- मोबाइल नंबर (OTP से साइन इन और एडमिन की मंज़ूरी के लिए)
- आपका नाम और आपका फ़्लैट (मंज़ूरी के लिए)
- आपके द्वारा स्वेच्छा से पोस्ट की गई सामग्री: फ़ीड पोस्ट का टेक्स्ट, फ़ोटो और वीडियो

**उद्देश्य**: निवासियों की पुष्टि और मॉडरेशन। मोबाइल नंबर केवल नामित सोसाइटी एडमिन को मंज़ूरी के लिए दिखते हैं और सार्वजनिक नहीं किए जाते। दूसरे निवासी केवल नाम देखते हैं; फ़ीड पोस्ट “सत्यापित निवासी” के रूप में दिखती हैं, आपके नाम के साथ कभी नहीं।

**कानूनी आधार**: सामुदायिक पुष्टि के लिए सहमति और वैध उपयोग (डिजिटल व्यक्तिगत डेटा संरक्षण अधिनियम, 2023)।

**डेटा कहाँ रखा जाता है**: सिंगापुर के सर्वरों पर (डेटाबेस Neon पर, फ़ोटो और वीडियो Amazon Web Services Lightsail पर), जिन तक केवल वेबसाइट संचालकों की पहुँच है।

**अनुवाद**: जब कोई फ़ीड पोस्ट पर “अनुवाद करें” दबाता है, तो उसका टेक्स्ट केवल अनुवाद के लिए एक मशीन अनुवाद सेवा को भेजा जाता है: मराठी के लिए Google Cloud Translation, अंग्रेज़ी और हिंदी के लिए Amazon Translate (सिंगापुर)। केवल टेक्स्ट भेजा जाता है; फ़ोटो, वीडियो, या आपके खाते का नाम और मोबाइल नंबर कभी नहीं। ये सेवाएँ टेक्स्ट को भारत के बाहर प्रोसेस कर सकती हैं। प्रकाशित फ़ीड पोस्ट के अनुवाद पोस्ट के साथ रखे जाते हैं। कृपया पोस्ट के टेक्स्ट में निजी जानकारी न लिखें।

**अवधि**: मोबाइल नंबर केवल तब तक रखे जाते हैं जब तक पुष्टि और खाते के प्रबंधन के लिए ज़रूरी हों। हटाने का अनुरोध आप नीचे दिए गए शिकायत अधिकारी से कर सकते हैं।

**साझा करना**: हम निजी डेटा न बेचते हैं, न किसी तीसरे पक्ष से साझा करते हैं, सिवाय कानून या न्यायालय के आदेश के। जब साइन-इन कोड SMS से भेजे जाएँगे, तब आपका नंबर केवल कोड पहुँचाने के लिए SMS सेवा प्रदाता को दिया जाएगा। जिस टेक्स्ट का अनुवाद माँगा जाता है, वह केवल अनुवाद के लिए अनुवाद सेवा को दिया जाता है (ऊपर अनुवाद देखें)।

**आपके अधिकार**: आप नीचे दिए गए शिकायत अधिकारी को लिखकर अपने निजी डेटा तक पहुँच, सुधार या उसे मिटाने का अनुरोध कर सकते हैं।

**शिकायत संपर्क**: नीचे शिकायत निवारण देखें।

हम ट्रैकिंग के लिए कुकीज़ का उपयोग नहीं करते। ऐप आपका साइन-इन और भाषा का चुनाव केवल आपके ब्राउज़र के स्टोरेज में रखता है, ताकि साइट काम करे।
"""),
        ("guidelines", "सामुदायिक दिशानिर्देश", """
पोस्ट में ये होना चाहिए:

- साफ़ बताएँ कि क्या वादा किया गया था (हो सके तो स्रोत के साथ: ब्रोशर का पेज, एग्रीमेंट की धारा, नक्शा आदि)
- आज ज़मीन पर क्या है, उसका वर्णन करें या उसे दिखाएँ
- जहाँ ज़रूरी हो, हाल की फ़ोटो जोड़ें
- किसी कर्मचारी या निवासी का नाम अपमानजनक तरीके से न लें

हम ये हटा देंगे:

- जानबूझकर झूठी लगने वाली सामग्री
- व्यक्तिगत हमले, अपशब्द या धमकियाँ
- निजी जगहों की, या पहचाने जा सकने वाले चेहरों की, बिना सहमति ली गई फ़ोटो
- भारतीय कानून का उल्लंघन करने वाली कोई भी सामग्री

बार-बार उल्लंघन करने पर पोस्ट करने की अनुमति स्थायी रूप से छीनी जा सकती है।
"""),
        ("grievance", "शिकायत निवारण", """
सूचना प्रौद्योगिकी (मध्यस्थ दिशानिर्देश और डिजिटल मीडिया आचार संहिता) नियम, 2021 के अनुसार:

**शिकायत अधिकारी**: जल्द घोषित किया जाएगा
**ईमेल**: जल्द घोषित किया जाएगा
**संपर्क**: जल्द घोषित किया जाएगा

सामग्री या इन नीतियों से जुड़ी शिकायतों की प्राप्ति 24 घंटे के भीतर स्वीकार की जाएगी और लागू कानून के अधीन 7 दिनों के भीतर उनका निपटारा किया जाएगा।
"""),
    ],
    "mr": [
        ("disclaimer", "अस्वीकरण", """
ही वेबसाइट (runwalltdfakepromises.com) हे डोंबिवली / दिवा परिसरातील रनवाल मायसिटी (आणि जवळच्या टॉवर्स) मधील मालक आणि भाडेकरूंनी, त्यांच्याचसाठी तयार केलेले स्वतंत्र, रहिवाशांकडून चालवले जाणारे व्यासपीठ आहे. त्याचा रनवाल ग्रुप, होरायझन प्रोजेक्ट्स प्रायव्हेट लिमिटेड किंवा त्यांच्या कोणत्याही संस्थेशी कोणताही संबंध, पाठिंबा किंवा जोड **नाही**.

सर्व पोस्ट, फोटो आणि टिप्पण्या वैयक्तिक रहिवासी पाठवतात. या वेबसाइटचे संचालक प्रत्येक दाव्याची स्वतंत्रपणे पडताळणी करत नाहीत. मजकूर तो पाठवणाऱ्या व्यक्तींची वैयक्तिक निरीक्षणे आणि अनुभव दर्शवतो. तो केवळ सामुदायिक माहिती आणि नोंदीसाठी आहे.

- या साइटवरील काहीही कायदेशीर सल्ला, अधिकृत तक्रार किंवा कोणत्याही प्राधिकरणाचा तथ्य-निष्कर्ष नाही.
- रहिवाशांनी औपचारिक तक्रारी महारेरा, संबंधित ग्राहक आयोग, सोसायटी/फेडरेशन आणि इतर योग्य प्राधिकरणांकडे नोंदवाव्यात, असे प्रोत्साहन दिले जाते.
- वेबसाइट संचालक कोणत्याही पोस्टच्या अचूकतेची, पूर्णतेची किंवा ताजेपणाची हमी देत नाहीत.
- फोटो रहिवासी पाठवतात आणि ते काढले तेव्हाची स्थिती दाखवतात. स्थिती बदलू शकते.

“रनवाल” नाव किंवा प्रोजेक्टच्या नावांचा वापर केवळ ओळख आणि वर्णनासाठी आहे. सर्व ट्रेडमार्क त्यांच्या संबंधित मालकांची मालमत्ता आहेत.

खोटा, अपमानजनक किंवा बेकायदेशीर वाटणारा कोणताही मजकूर काढून टाकण्याचा किंवा नाकारण्याचा अधिकार संचालक राखून ठेवतात.
"""),
        ("terms", "वापराच्या अटी", """
ही वेबसाइट वापरून तुम्ही पुढील गोष्टींना संमती देता:

1. **उद्देश**: हे नमूद प्रोजेक्ट्सच्या रहिवाशांसाठी खासगी सामुदायिक नोंद-व्यासपीठ आहे. ही व्यावसायिक सेवा नाही.
2. **वापरकर्त्यांनी तयार केलेला मजकूर**: तुम्ही पाठवलेल्या कोणत्याही मजकुराच्या किंवा फोटोच्या अचूकतेची संपूर्ण जबाबदारी तुमची आहे. तुम्ही पुष्टी करता की:
   - तुमच्या माहितीनुसार मजकूर खरा आहे;
   - फोटो तुमचे आहेत किंवा ते अपलोड करण्याचा तुम्हाला अधिकार आहे;
   - तुम्ही खोटी, अतिशयोक्त किंवा दुष्ट हेतूची विधाने अपलोड करणार नाही.
3. **परवाना**: पोस्ट करून तुम्ही वेबसाइट संचालकांना सामुदायिक नोंदीसाठी साइटवर मजकूर दाखवण्याचा अनन्य-नसलेला परवाना देता.
4. **प्रतिबंधित मजकूर**: वैयक्तिक शिवीगाळ, द्वेषपूर्ण भाषा, इतरांची गोपनीय वैयक्तिक माहिती किंवा स्पष्टपणे खोटा किंवा बदनामीकारक मजकूर नाही.
5. **नियंत्रण**: संचालक त्यांच्या विवेकानुसार, सूचना न देता, मजकुराचे परीक्षण करू शकतात, तो संपादित करू शकतात किंवा काढून टाकू शकतात.
6. **कोणतेही दायित्व नाही**: रहिवाशांनी पोस्ट केलेल्या मजकुरावर विसंबून झालेल्या कोणत्याही नुकसानीस संचालक जबाबदार नाहीत. साइट “जशी आहे तशी” उपलब्ध करून दिली जाते.
7. **मध्यस्थ**: संचालक माहिती तंत्रज्ञान अधिनियम, 2000 अंतर्गत मध्यस्थ म्हणून काम करतात. लागू कायद्यानुसार ते वैध कायदेशीर नोटिसा आणि न्यायालयीन आदेशांना प्रतिसाद देतील.
8. **लागू कायदा**: या अटी भारताच्या कायद्यांच्या अधीन आहेत. ठाणे येथील न्यायालयांना अधिकारक्षेत्र असेल.
"""),
        ("privacy", "गोपनीयता धोरण", """
वापरकर्ता प्रोजेक्टचा रहिवासी आहे याची पडताळणी करण्यासाठी आणि फीड चालवण्यासाठी जितकी आवश्यक आहे, तितकीच मर्यादित वैयक्तिक माहिती ही वेबसाइट घेते.

**घेतली जाणारी माहिती**:

- मोबाइल नंबर (OTP ने साइन इन आणि ॲडमिनच्या मंजुरीसाठी)
- तुमचे नाव आणि तुमचा फ्लॅट (मंजुरीसाठी)
- तुम्ही स्वेच्छेने पोस्ट केलेला मजकूर: फीड पोस्टचा मजकूर, फोटो आणि व्हिडिओ

**उद्देश**: रहिवाशांची पडताळणी आणि नियंत्रण. मोबाइल नंबर फक्त नेमलेल्या सोसायटी ॲडमिनना मंजुरीसाठी दिसतात आणि सार्वजनिक केले जात नाहीत. इतर रहिवाशांना फक्त नावे दिसतात; फीड पोस्ट “सत्यापित रहिवासी” म्हणून दिसतात, तुमच्या नावासह कधीच नाही.

**कायदेशीर आधार**: सामुदायिक पडताळणीसाठी संमती आणि वैध वापर (डिजिटल वैयक्तिक माहिती संरक्षण अधिनियम, 2023).

**माहिती कुठे ठेवली जाते**: सिंगापूरमधील सर्व्हरवर (डेटाबेस Neon वर, फोटो आणि व्हिडिओ Amazon Web Services Lightsail वर), जिथे फक्त वेबसाइट संचालकांना प्रवेश आहे.

**भाषांतर**: एखाद्या फीड पोस्टवर कोणी “भाषांतर करा” दाबल्यास, त्याचा मजकूर फक्त भाषांतरासाठी यंत्र भाषांतर सेवेकडे पाठवला जातो: मराठीसाठी Google Cloud Translation, इंग्रजी आणि हिंदीसाठी Amazon Translate (सिंगापूर). फक्त मजकूर पाठवला जातो; फोटो, व्हिडिओ, किंवा तुमच्या खात्यातील नाव आणि मोबाइल नंबर कधीच नाही. या सेवा मजकुरावर भारताबाहेर प्रक्रिया करू शकतात. प्रकाशित फीड पोस्टची भाषांतरे पोस्टसोबत ठेवली जातात. कृपया पोस्टच्या मजकुरात वैयक्तिक माहिती लिहू नका.

**कालावधी**: मोबाइल नंबर फक्त पडताळणी आणि खाते व्यवस्थापनासाठी आवश्यक असेपर्यंत ठेवले जातात. काढून टाकण्याची विनंती तुम्ही खाली दिलेल्या तक्रार अधिकाऱ्याकडे करू शकता.

**माहिती देणे**: आम्ही वैयक्तिक माहिती विकत नाही किंवा कोणत्याही त्रयस्थ पक्षाला देत नाही, कायद्याने किंवा न्यायालयाच्या आदेशाने आवश्यक असेल तेव्हा सोडून. साइन-इन कोड SMS ने पाठवले जातील तेव्हा तुमचा नंबर फक्त कोड पोहोचवण्यासाठी SMS सेवा पुरवठादाराला दिला जाईल. ज्या मजकुराचे भाषांतर मागितले जाते, तो फक्त भाषांतरासाठी भाषांतर सेवेला दिला जातो (वर भाषांतर पाहा).

**तुमचे अधिकार**: खाली दिलेल्या तक्रार अधिकाऱ्याला लिहून तुम्ही तुमच्या वैयक्तिक माहितीत प्रवेश, दुरुस्ती किंवा ती पुसून टाकण्याची विनंती करू शकता.

**तक्रार संपर्क**: खाली तक्रार निवारण पाहा.

आम्ही ट्रॅकिंगसाठी कुकीज वापरत नाही. साइट चालावी म्हणून ॲप तुमचे साइन-इन आणि भाषेची निवड फक्त तुमच्या ब्राउझरच्या स्टोरेजमध्ये ठेवते.
"""),
        ("guidelines", "सामुदायिक मार्गदर्शक तत्त्वे", """
पोस्टमध्ये हे असावे:

- काय वचन दिले होते ते स्पष्टपणे सांगा (शक्य असल्यास स्रोतासह: ब्रोशरचे पान, कराराचे कलम, नकाशा इ.)
- आज प्रत्यक्ष जागेवर काय आहे त्याचे वर्णन करा किंवा ते दाखवा
- आवश्यक तिथे अलीकडचे फोटो जोडा
- कोणत्याही कर्मचाऱ्याचे किंवा रहिवाशाचे नाव अपमानास्पद पद्धतीने घेऊ नका

आम्ही हे काढून टाकू:

- जाणूनबुजून खोटा वाटणारा मजकूर
- वैयक्तिक हल्ले, शिवीगाळ किंवा धमक्या
- खासगी जागांचे, किंवा ओळखता येणाऱ्या चेहऱ्यांचे, संमतीशिवाय काढलेले फोटो
- भारतीय कायद्याचे उल्लंघन करणारा कोणताही मजकूर

वारंवार उल्लंघन केल्यास पोस्ट करण्याचा अधिकार कायमचा जाऊ शकतो.
"""),
        ("grievance", "तक्रार निवारण", """
माहिती तंत्रज्ञान (मध्यस्थ मार्गदर्शक तत्त्वे आणि डिजिटल माध्यम आचारसंहिता) नियम, 2021 नुसार:

**तक्रार अधिकारी**: लवकरच जाहीर केले जाईल
**ईमेल**: लवकरच जाहीर केले जाईल
**संपर्क**: लवकरच जाहीर केले जाईल

मजकूर किंवा या धोरणांबद्दलच्या तक्रारींची पोच 24 तासांत दिली जाईल आणि लागू कायद्याच्या अधीन राहून 7 दिवसांत त्यांचे निराकरण केले जाईल.
"""),
    ],
}
