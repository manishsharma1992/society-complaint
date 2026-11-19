"""Garden City Seva's legal pages (/legal/, /hi/legal/, /mr/legal/), in the Markdown subset of legal_texts.py.
English prevails; hi and mr are translations. Have every change reviewed by the lawyer before it goes live.
The Privacy Policy must list everything the app stores.
"""

UI = {
    "en": {"title": "Legal", "updated": "Last updated: 19 November 2026", "note": "", "english": "", "signin": "Sign in",
           "meta": "Terms of Use, Privacy Policy and Grievance Redressal of Garden City Seva."},
    "hi": {"title": "कानूनी जानकारी", "updated": "अंतिम अपडेट: 19 नवंबर 2026",
           "note": "यह अंग्रेज़ी पाठ का अनुवाद है। दोनों में अंतर होने पर अंग्रेज़ी पाठ मान्य होगा।", "english": "अंग्रेज़ी पाठ", "signin": "साइन इन",
           "meta": "गार्डन सिटी सेवा की उपयोग की शर्तें, गोपनीयता नीति और शिकायत निवारण।"},
    "mr": {"title": "कायदेशीर माहिती", "updated": "शेवटचा बदल: 19 नोव्हेंबर 2026",
           "note": "हा इंग्रजी मजकुराचा अनुवाद आहे. दोन्हींत फरक असल्यास इंग्रजी मजकूर ग्राह्य धरला जाईल.", "english": "इंग्रजी मजकूर", "signin": "साइन इन",
           "meta": "गार्डन सिटी सेवाच्या वापराच्या अटी, गोपनीयता धोरण आणि तक्रार निवारण."},
}

LEGAL = {
    "en": [
        ("terms", "Terms of Use", """
By using Garden City Seva you agree to the following:

1. **Purpose**: Garden City Seva is a tool for residents, maintenance staff and administrators of Runwal Garden City to raise and follow maintenance complaints. It is not a commercial service. It is not run by, or affiliated with, Runwal or the builder.
2. **Your account**: Use your own mobile number, give your real name and flat, and do not share your sign-in code with anyone.
3. **What you submit**: You are responsible for the text and photographs you submit. You confirm that:
   - The content is true to the best of your knowledge;
   - You took the photographs or have the right to upload them;
   - You will not submit false or malicious complaints.
4. **Licence**: By submitting content, you allow the operators to show it to the people who handle or may see the complaint.
5. **Prohibited content**: No personal abuse, hate speech, confidential personal data of others, or photographs of people without their consent.
6. **Emergencies**: This app is not an emergency service. For a fire, a medical emergency or any danger to life, call the emergency services.
7. **Handling of complaints**: Administrators may assign, reject or close complaints. The app does not promise that a complaint will be fixed by a particular time. The operators may remove content or end access if the app is misused.
8. **No liability**: The app is provided “as is”. The operators are not liable for any loss arising from its use or from content submitted by users.
9. **Intermediary**: The operators act as an intermediary under the Information Technology Act, 2000. They will respond to valid legal notices and court orders in accordance with applicable law.
10. **Governing law**: These terms are governed by the laws of India. Courts in Thane shall have jurisdiction.
"""),
        ("privacy", "Privacy Policy", """
Garden City Seva collects only the personal data needed to confirm that you are a resident or staff member of Runwal Garden City, and to handle maintenance complaints.

**Data collected**:

- Mobile number (for OTP sign-in and admin approval)
- Your name and your flat (for approval, and so that complaints reach the right tower)
- What you submit: the text, photographs and comments of complaints
- For maintenance staff only: a selfie and the phone’s location when a shift starts and ends, and the times of each shift

**Purpose**: Confirming residents and staff, handling complaints, and confirming staff shifts.

**Who sees what**:

- A complaint about a flat is seen by that flat’s household, the person who raised it, the staff and the administrators.
- A complaint about a common area is seen by everyone in the society.
- Other users see names, never mobile numbers. Mobile numbers are visible only to the society administrator who reviews a sign-up.
- Shift selfies are seen by the tower and society administrators, who compare them to confirm a shift. There is no automatic face matching.

**Photographs**: Photographs are resized and their location data removed on your phone before upload.

**Legal basis**: Consent and legitimate use for running the society’s maintenance service (Digital Personal Data Protection Act, 2023).

**Where it is stored**: On servers in Singapore (the database on Neon, photographs on Amazon Web Services Lightsail), with access limited to the operators.

**Translation**: When someone taps “Translate” on a complaint, its text (title, description and comments) is sent to a machine translation service only to translate it: Google Cloud Translation for Marathi, Amazon Translate (Singapore) for English and Hindi. Only the text is sent, never photographs, or the name and mobile number from your account. These services may process the text outside India. Translations are not stored. Please do not write personal details in the text of a complaint.

**Retention**: Mobile numbers are kept only as long as needed for verification and account management. Complaints are kept as the society’s maintenance record. Staff selfies and locations are deleted automatically 90 days after the shift started; shift times and reviews are kept. You may request deletion by contacting the Grievance Officer below.

**Sharing**: We do not sell or share personal data with third parties except as required by law or court order. When sign-in codes are sent by SMS, your number is passed to the SMS provider only to deliver the code. Text that someone asks to translate is passed to the translation service only to translate it (see Translation above).

**Your rights**: You may request access, correction, or erasure of your personal data by writing to the Grievance Officer below.

We do not use cookies for tracking. The app keeps your sign-in and your language choice in your browser’s storage, only so that it works.
"""),
        ("grievance", "Grievance Redressal", """
In accordance with the Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules, 2021:

**Grievance Officer**: To be announced
**Email**: To be announced
**Contact**: To be announced

Grievances about this app or these policies will be acknowledged within 24 hours and resolved within 7 days, subject to applicable law. Maintenance problems should be raised as complaints in the app.
"""),
    ],
    "hi": [
        ("terms", "उपयोग की शर्तें", """
गार्डन सिटी सेवा का उपयोग करके आप निम्नलिखित से सहमत होते हैं:

1. **उद्देश्य**: गार्डन सिटी सेवा, रनवाल गार्डन सिटी के निवासियों, मेंटेनेंस स्टाफ़ और एडमिन के लिए मेंटेनेंस शिकायतें दर्ज करने और उन पर नज़र रखने का साधन है। यह कोई व्यावसायिक सेवा नहीं है। इसे रनवाल या बिल्डर नहीं चलाते, न इसका उनसे कोई संबंध है।
2. **आपका खाता**: अपना ही मोबाइल नंबर इस्तेमाल करें, अपना सही नाम और फ़्लैट बताएँ, और अपना साइन-इन कोड किसी को न दें।
3. **आप जो भेजते हैं**: आप जो टेक्स्ट और फ़ोटो भेजते हैं, उसकी ज़िम्मेदारी आपकी है। आप पुष्टि करते हैं कि:
   - आपकी जानकारी के अनुसार सामग्री सच है;
   - फ़ोटो आपने ली हैं या आपको उन्हें अपलोड करने का अधिकार है;
   - आप झूठी या दुर्भावनापूर्ण शिकायतें नहीं भेजेंगे।
4. **लाइसेंस**: सामग्री भेजकर आप संचालकों को उसे उन लोगों को दिखाने की अनुमति देते हैं जो शिकायत का निपटारा करते हैं या उसे देख सकते हैं।
5. **निषिद्ध सामग्री**: कोई व्यक्तिगत अपशब्द, नफ़रत भरी भाषा, दूसरों का गोपनीय निजी डेटा, या लोगों की सहमति के बिना उनकी फ़ोटो नहीं।
6. **आपात स्थिति**: यह ऐप आपातकालीन सेवा नहीं है। आग, चिकित्सा आपात स्थिति या जान के ख़तरे में आपातकालीन सेवाओं को फ़ोन करें।
7. **शिकायतों का निपटारा**: एडमिन शिकायतें सौंप सकते हैं, अस्वीकार कर सकते हैं या बंद कर सकते हैं। ऐप यह वादा नहीं करता कि कोई शिकायत किसी तय समय तक ठीक हो जाएगी। ऐप के दुरुपयोग पर संचालक सामग्री हटा सकते हैं या पहुँच बंद कर सकते हैं।
8. **कोई दायित्व नहीं**: ऐप “जैसा है वैसा” उपलब्ध कराया जाता है। इसके उपयोग से या उपयोगकर्ताओं की भेजी सामग्री से होने वाले किसी भी नुकसान के लिए संचालक ज़िम्मेदार नहीं हैं।
9. **मध्यस्थ**: संचालक सूचना प्रौद्योगिकी अधिनियम, 2000 के तहत मध्यस्थ के रूप में काम करते हैं। वे लागू कानून के अनुसार वैध कानूनी नोटिस और न्यायालय के आदेशों का जवाब देंगे।
10. **लागू कानून**: ये शर्तें भारत के कानूनों के अधीन हैं। ठाणे के न्यायालयों का अधिकार क्षेत्र होगा।
"""),
        ("privacy", "गोपनीयता नीति", """
गार्डन सिटी सेवा केवल उतना निजी डेटा लेती है, जितना यह पुष्टि करने के लिए ज़रूरी है कि आप रनवाल गार्डन सिटी के निवासी या स्टाफ़ सदस्य हैं, और मेंटेनेंस शिकायतों के निपटारे के लिए।

**लिया जाने वाला डेटा**:

- मोबाइल नंबर (OTP से साइन इन और एडमिन की मंज़ूरी के लिए)
- आपका नाम और आपका फ़्लैट (मंज़ूरी के लिए, और ताकि शिकायतें सही टावर तक पहुँचें)
- आप जो भेजते हैं: शिकायतों का टेक्स्ट, फ़ोटो और टिप्पणियाँ
- केवल मेंटेनेंस स्टाफ़ के लिए: शिफ़्ट शुरू और ख़त्म होते समय एक सेल्फ़ी और फ़ोन की लोकेशन, और हर शिफ़्ट का समय

**उद्देश्य**: निवासियों और स्टाफ़ की पुष्टि, शिकायतों का निपटारा और स्टाफ़ की शिफ़्ट की पुष्टि।

**कौन क्या देखता है**:

- किसी फ़्लैट की शिकायत उस फ़्लैट का परिवार, उसे दर्ज करने वाला व्यक्ति, स्टाफ़ और एडमिन देखते हैं।
- कॉमन एरिया की शिकायत सोसाइटी के सभी लोग देखते हैं।
- दूसरे उपयोगकर्ता नाम देखते हैं, मोबाइल नंबर कभी नहीं। मोबाइल नंबर केवल साइन-अप की जाँच करने वाले सोसाइटी एडमिन को दिखते हैं।
- शिफ़्ट की सेल्फ़ी टावर और सोसाइटी एडमिन देखते हैं, जो शिफ़्ट की पुष्टि के लिए उनकी तुलना करते हैं। चेहरों का कोई स्वचालित मिलान नहीं होता।

**फ़ोटो**: अपलोड से पहले आपके फ़ोन पर ही फ़ोटो छोटी की जाती हैं और उनका लोकेशन डेटा हटाया जाता है।

**कानूनी आधार**: सोसाइटी की मेंटेनेंस सेवा चलाने के लिए सहमति और वैध उपयोग (डिजिटल व्यक्तिगत डेटा संरक्षण अधिनियम, 2023)।

**डेटा कहाँ रखा जाता है**: सिंगापुर के सर्वरों पर (डेटाबेस Neon पर, फ़ोटो Amazon Web Services Lightsail पर), जिन तक केवल संचालकों की पहुँच है।

**अनुवाद**: जब कोई शिकायत पर “अनुवाद करें” दबाता है, तो उसका टेक्स्ट (शीर्षक, विवरण और टिप्पणियाँ) केवल अनुवाद के लिए एक मशीन अनुवाद सेवा को भेजा जाता है: मराठी के लिए Google Cloud Translation, अंग्रेज़ी और हिंदी के लिए Amazon Translate (सिंगापुर)। केवल टेक्स्ट भेजा जाता है; फ़ोटो, या आपके खाते का नाम और मोबाइल नंबर कभी नहीं। ये सेवाएँ टेक्स्ट को भारत के बाहर प्रोसेस कर सकती हैं। अनुवाद सहेजे नहीं जाते। कृपया शिकायत के टेक्स्ट में निजी जानकारी न लिखें।

**अवधि**: मोबाइल नंबर केवल तब तक रखे जाते हैं जब तक पुष्टि और खाते के प्रबंधन के लिए ज़रूरी हों। शिकायतें सोसाइटी के मेंटेनेंस रिकॉर्ड के रूप में रखी जाती हैं। स्टाफ़ की सेल्फ़ी और लोकेशन शिफ़्ट शुरू होने के 90 दिन बाद अपने आप हटा दी जाती हैं; शिफ़्ट का समय और समीक्षाएँ रखी जाती हैं। हटाने का अनुरोध आप नीचे दिए गए शिकायत अधिकारी से कर सकते हैं।

**साझा करना**: हम निजी डेटा न बेचते हैं, न किसी तीसरे पक्ष से साझा करते हैं, सिवाय कानून या न्यायालय के आदेश के। जब साइन-इन कोड SMS से भेजे जाएँगे, तब आपका नंबर केवल कोड पहुँचाने के लिए SMS सेवा प्रदाता को दिया जाएगा। जिस टेक्स्ट का अनुवाद माँगा जाता है, वह केवल अनुवाद के लिए अनुवाद सेवा को दिया जाता है (ऊपर अनुवाद देखें)।

**आपके अधिकार**: आप नीचे दिए गए शिकायत अधिकारी को लिखकर अपने निजी डेटा तक पहुँच, सुधार या उसे मिटाने का अनुरोध कर सकते हैं।

हम ट्रैकिंग के लिए कुकीज़ का उपयोग नहीं करते। ऐप आपका साइन-इन और भाषा का चुनाव केवल आपके ब्राउज़र के स्टोरेज में रखता है, ताकि वह काम करे।
"""),
        ("grievance", "शिकायत निवारण", """
सूचना प्रौद्योगिकी (मध्यस्थ दिशानिर्देश और डिजिटल मीडिया आचार संहिता) नियम, 2021 के अनुसार:

**शिकायत अधिकारी**: जल्द घोषित किया जाएगा
**ईमेल**: जल्द घोषित किया जाएगा
**संपर्क**: जल्द घोषित किया जाएगा

इस ऐप या इन नीतियों से जुड़ी शिकायतों की प्राप्ति 24 घंटे के भीतर स्वीकार की जाएगी और लागू कानून के अधीन 7 दिनों के भीतर उनका निपटारा किया जाएगा। मेंटेनेंस की समस्याएँ ऐप में शिकायत के रूप में दर्ज करें।
"""),
    ],
    "mr": [
        ("terms", "वापराच्या अटी", """
गार्डन सिटी सेवा वापरून तुम्ही पुढील गोष्टींना संमती देता:

1. **उद्देश**: गार्डन सिटी सेवा हे रनवाल गार्डन सिटीचे रहिवासी, मेंटेनन्स कर्मचारी आणि ॲडमिन यांच्यासाठी मेंटेनन्सच्या तक्रारी नोंदवण्याचे आणि त्यांचा पाठपुरावा करण्याचे साधन आहे. ही व्यावसायिक सेवा नाही. ते रनवाल किंवा बिल्डर चालवत नाहीत, आणि त्यांच्याशी त्याचा संबंध नाही.
2. **तुमचे खाते**: स्वतःचाच मोबाइल नंबर वापरा, तुमचे खरे नाव आणि फ्लॅट सांगा, आणि तुमचा साइन-इन कोड कोणालाही देऊ नका.
3. **तुम्ही पाठवलेला मजकूर**: तुम्ही पाठवलेल्या मजकुराची आणि फोटोंची जबाबदारी तुमची आहे. तुम्ही पुष्टी करता की:
   - तुमच्या माहितीनुसार मजकूर खरा आहे;
   - फोटो तुम्ही काढले आहेत किंवा ते अपलोड करण्याचा तुम्हाला अधिकार आहे;
   - तुम्ही खोट्या किंवा दुष्ट हेतूच्या तक्रारी पाठवणार नाही.
4. **परवाना**: मजकूर पाठवून तुम्ही संचालकांना तो तक्रार हाताळणाऱ्या किंवा ती पाहू शकणाऱ्या लोकांना दाखवण्याची परवानगी देता.
5. **प्रतिबंधित मजकूर**: वैयक्तिक शिवीगाळ, द्वेषपूर्ण भाषा, इतरांची गोपनीय वैयक्तिक माहिती किंवा लोकांच्या संमतीशिवाय त्यांचे फोटो नाहीत.
6. **आणीबाणी**: हे ॲप आपत्कालीन सेवा नाही. आग, वैद्यकीय आणीबाणी किंवा जिवाला धोका असल्यास आपत्कालीन सेवांना फोन करा.
7. **तक्रारींची हाताळणी**: ॲडमिन तक्रारी सोपवू शकतात, नाकारू शकतात किंवा बंद करू शकतात. तक्रार ठरावीक वेळेत दूर होईल असे वचन ॲप देत नाही. ॲपचा गैरवापर झाल्यास संचालक मजकूर काढून टाकू शकतात किंवा प्रवेश बंद करू शकतात.
8. **कोणतेही दायित्व नाही**: ॲप “जसे आहे तसे” उपलब्ध करून दिले जाते. त्याच्या वापरामुळे किंवा वापरकर्त्यांनी पाठवलेल्या मजकुरामुळे झालेल्या कोणत्याही नुकसानीस संचालक जबाबदार नाहीत.
9. **मध्यस्थ**: संचालक माहिती तंत्रज्ञान अधिनियम, 2000 अंतर्गत मध्यस्थ म्हणून काम करतात. लागू कायद्यानुसार ते वैध कायदेशीर नोटिसा आणि न्यायालयीन आदेशांना प्रतिसाद देतील.
10. **लागू कायदा**: या अटी भारताच्या कायद्यांच्या अधीन आहेत. ठाणे येथील न्यायालयांना अधिकारक्षेत्र असेल.
"""),
        ("privacy", "गोपनीयता धोरण", """
तुम्ही रनवाल गार्डन सिटीचे रहिवासी किंवा कर्मचारी आहात याची पुष्टी करण्यासाठी आणि मेंटेनन्सच्या तक्रारी हाताळण्यासाठी जितकी आवश्यक आहे, तितकीच वैयक्तिक माहिती गार्डन सिटी सेवा घेते.

**घेतली जाणारी माहिती**:

- मोबाइल नंबर (OTP ने साइन इन आणि ॲडमिनच्या मंजुरीसाठी)
- तुमचे नाव आणि तुमचा फ्लॅट (मंजुरीसाठी, आणि तक्रारी योग्य टॉवरपर्यंत पोहोचाव्यात म्हणून)
- तुम्ही पाठवलेला मजकूर: तक्रारींचा मजकूर, फोटो आणि टिप्पण्या
- फक्त मेंटेनन्स कर्मचाऱ्यांसाठी: शिफ्ट सुरू होताना आणि संपताना एक सेल्फी आणि फोनचे ठिकाण, आणि प्रत्येक शिफ्टची वेळ

**उद्देश**: रहिवासी आणि कर्मचाऱ्यांची पुष्टी, तक्रारींचा निपटारा आणि कर्मचाऱ्यांच्या शिफ्टची पुष्टी.

**कोण काय पाहते**:

- फ्लॅटची तक्रार त्या फ्लॅटचे कुटुंब, ती नोंदवणारी व्यक्ती, कर्मचारी आणि ॲडमिन पाहतात.
- कॉमन एरियाची तक्रार सोसायटीतील सर्वजण पाहतात.
- इतर वापरकर्त्यांना नावे दिसतात, मोबाइल नंबर कधीच नाही. मोबाइल नंबर फक्त साइन-अप तपासणाऱ्या सोसायटी ॲडमिनना दिसतात.
- शिफ्टचे सेल्फी टॉवर आणि सोसायटी ॲडमिन पाहतात, आणि शिफ्टच्या पुष्टीसाठी त्यांची तुलना करतात. चेहऱ्यांची कोणतीही स्वयंचलित जुळवणी होत नाही.

**फोटो**: अपलोडपूर्वी तुमच्या फोनवरच फोटो लहान केले जातात आणि त्यांचा लोकेशन डेटा काढला जातो.

**कायदेशीर आधार**: सोसायटीची मेंटेनन्स सेवा चालवण्यासाठी संमती आणि वैध वापर (डिजिटल वैयक्तिक माहिती संरक्षण अधिनियम, 2023).

**माहिती कुठे ठेवली जाते**: सिंगापूरमधील सर्व्हरवर (डेटाबेस Neon वर, फोटो Amazon Web Services Lightsail वर), जिथे फक्त संचालकांना प्रवेश आहे.

**भाषांतर**: एखाद्या तक्रारीवर कोणी “भाषांतर करा” दाबल्यास, तिचा मजकूर (शीर्षक, वर्णन आणि टिप्पण्या) फक्त भाषांतरासाठी यंत्र भाषांतर सेवेकडे पाठवला जातो: मराठीसाठी Google Cloud Translation, इंग्रजी आणि हिंदीसाठी Amazon Translate (सिंगापूर). फक्त मजकूर पाठवला जातो; फोटो, किंवा तुमच्या खात्यातील नाव आणि मोबाइल नंबर कधीच नाही. या सेवा मजकुरावर भारताबाहेर प्रक्रिया करू शकतात. भाषांतरे साठवली जात नाहीत. कृपया तक्रारीच्या मजकुरात वैयक्तिक माहिती लिहू नका.

**कालावधी**: मोबाइल नंबर फक्त पडताळणी आणि खाते व्यवस्थापनासाठी आवश्यक असेपर्यंत ठेवले जातात. तक्रारी सोसायटीच्या मेंटेनन्स नोंदी म्हणून ठेवल्या जातात. कर्मचाऱ्यांचे सेल्फी आणि ठिकाण शिफ्ट सुरू झाल्यापासून 90 दिवसांनी आपोआप काढून टाकले जातात; शिफ्टच्या वेळा आणि पुनरावलोकने ठेवली जातात. काढून टाकण्याची विनंती तुम्ही खाली दिलेल्या तक्रार अधिकाऱ्याकडे करू शकता.

**माहिती देणे**: आम्ही वैयक्तिक माहिती विकत नाही किंवा कोणत्याही त्रयस्थ पक्षाला देत नाही, कायद्याने किंवा न्यायालयाच्या आदेशाने आवश्यक असेल तेव्हा सोडून. साइन-इन कोड SMS ने पाठवले जातील तेव्हा तुमचा नंबर फक्त कोड पोहोचवण्यासाठी SMS सेवा पुरवठादाराला दिला जाईल. ज्या मजकुराचे भाषांतर मागितले जाते, तो फक्त भाषांतरासाठी भाषांतर सेवेला दिला जातो (वर भाषांतर पाहा).

**तुमचे अधिकार**: खाली दिलेल्या तक्रार अधिकाऱ्याला लिहून तुम्ही तुमच्या वैयक्तिक माहितीत प्रवेश, दुरुस्ती किंवा ती पुसून टाकण्याची विनंती करू शकता.

आम्ही ट्रॅकिंगसाठी कुकीज वापरत नाही. ॲप चालावे म्हणून ते तुमचे साइन-इन आणि भाषेची निवड फक्त तुमच्या ब्राउझरच्या स्टोरेजमध्ये ठेवते.
"""),
        ("grievance", "तक्रार निवारण", """
माहिती तंत्रज्ञान (मध्यस्थ मार्गदर्शक तत्त्वे आणि डिजिटल माध्यम आचारसंहिता) नियम, 2021 नुसार:

**तक्रार अधिकारी**: लवकरच जाहीर केले जाईल
**ईमेल**: लवकरच जाहीर केले जाईल
**संपर्क**: लवकरच जाहीर केले जाईल

या ॲपबद्दल किंवा या धोरणांबद्दलच्या तक्रारींची पोच 24 तासांत दिली जाईल आणि लागू कायद्याच्या अधीन राहून 7 दिवसांत त्यांचे निराकरण केले जाईल. मेंटेनन्सच्या समस्या ॲपमध्ये तक्रार म्हणून नोंदवा.
"""),
    ],
}
