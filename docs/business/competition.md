# Competition

Date: 2026-11-04. Facts come from web search results. Many are from 2019 to 2025. Check each product before any public claim. We list only products we found evidence for. "Not verified" means the search did not confirm it.

## Table

| Product | Offline? | Languages | Pricing | Open or closed | What it does | Our difference |
|---|---|---|---|---|---|---|
| PlantVillage Nuru (Penn State, FAO, CGIAR partners) | Yes. Diagnoses cassava offline, syncs later ([Penn State](https://www.psu.edu/news/research/story/grant-will-support-expanded-use-artificial-intelligence-crop-health)) | Swahili, French, Twi, Hindi, English (2019 to 2020 lists) ([PlantwisePlus](https://blog.plantwise.org/2020/03/13/nuru-ai-expansion-supporting-farmers-to-diagnose-crop-diseases)) | Free to farmers (not verified in detail) | Not verified | Cassava first. Expanding to potato, yam, banana, fall armyworm | We cover 7 crops. We add Kinyarwanda. We add vetted treatment steps, an on-device LLM and an Apache-2.0 licence. Nuru is the closest competitor and a possible partner |
| Plantix (PEAT, Berlin) | Partly. Images can be captured offline. Full offline diagnosis not confirmed ([Gründerküche](https://www.gruenderkueche.de/news/kranke-pflanzen-frueh-erkennen-plantix-soll-mit-66-millionen-euro-weiter-wachsen/)) | About 18 to 20, depending on source | Free app. Business tools priced on request. Commissions from linked dealers (directory source, not verified) | Closed | Photo diagnosis for many crops, strong in India | Fully offline by design. Kinyarwanda. Open model. No dealer commission bias |
| Digital Green Farmer.CHAT | Needs internet. It runs on WhatsApp or Telegram, then a dedicated interface ([FY25 report](https://digitalgreen.org/wp-content/uploads/2025/07/Digital-Green-FY25-Annual-Report.pdf)) | English, Hindi, Telugu, Swahili, Amharic, Afaan Oromo (changes over time) ([Gooey.AI](https://help.gooey.ai/farmerchat), [Digital Green](https://digitalgreen.org/farmer.chat/)) | Free to farmers. Funded by partners (not verified) | Not verified | Chatbot answers questions. Not photo-based diagnosis in our evidence | Diagnosis from a photo, offline. Chat is optional and local. Complementary: Farmer.CHAT could be a channel when online |
| Apollo Agriculture (Kenya) | No evidence of offline AI | Not verified | Sells inputs and credit. USD 40 million Series B in 2022 ([TechCrunch](https://techcrunch.com/2022/03/21/kenya-based-agritech-apollo-raises-40-million-in-softbank-led-round-joined-by-chan-zuckerberg-initiative-cdc)) | Closed | Credit, inputs and advice for farmers. About 100,000 farmers by end 2021 | Not a direct competitor. Possible customer or partner for outbreak data |
| iCow (Green Dreams Tech, with Safaricom) | SMS and USSD (*285#), so no data needed ([Business Daily](https://www.businessdailyafrica.com/bd/corporate/technology/safaricom-to-launch-app-for-dairy-farmers--2035390)) | Not verified | Subscription SMS. Claimed 600,000 in database, 60,000 active (source dates vary) | Closed | Livestock and farm tips by SMS | We diagnose crops from a photo. iCow focuses on livestock tips |
| Safaricom DigiFarm | Not verified | Not verified | Free registration. Credit and input services. Registrations between 0.67 and 1.5 million. 117,000 active in one year ([Safaricom 2022 report](https://safaricom.co.ke/sustainabilityreport_2022/ebook/49)) | Closed | Inputs, credit, market links. Safaricom and Opportunity International piloted an AI chatbot for up to 1,000 farmers in 2025 ([Techweez](https://techweez.com/2025/02/10/safaricom-and-opportunity-international-launch-farmerai-for-kenyan-farmers/)) | Kenya only. We work across borders and offline. Possible channel partner |
| KaraAgro AI | No. Drone-based | Not verified | Mid to large farms pay. Subsidised for smallholders ([TechCabal](https://techcabal.com/2025/09/05/how-african-farmers-are-using-ai/)) | Closed | Started with a photo app, now drone scanning. Ghana-founded. No Kenya deployment found | Different customer. We serve smallholders on phones |
| UjuziKilimo (Kenya) | Not verified | Not verified | Not verified | Closed | Soil-testing kits and app. About 30,000 farmers (older source) ([Extruct](https://www.extruct.ai/hub/ujuzikilimo-com)) | Soil, not leaf disease. Complementary |
| KilimoAI (Tanzania) | Not verified | Not verified | Not verified | Not verified | Crop disease detection reported in Tanzanian media ([The Citizen](https://www.thecitizen.co.tz/tanzania/news/national/kilimoai-empowers-farmers-with-crop-disease-detection-5312654)) | Check before launch. Likely a direct competitor in Tanzania |

Note: the task named "KaraAgro" and "Ujuzi Kilimo". We found no evidence that they are linked. They are separate companies.

## Honest read

- Nuru is the strongest competitor. It is offline, free and backed by research institutions. Our edge is breadth (4 crops in v1, 7 planned), treatment steps in three languages (Kinyarwanda next), the same model on Android and in the browser, and an open licence that a partner can fork.
- Plantix has scale and a large database. It is closed and its offline mode is unclear.
- Chatbots and SMS tools need connectivity or pay per message. Our unit cost is zero.
- Bigger platforms (Safaricom, Apollo) are channels more than rivals.

## Open questions

- Is Nuru's model open? Not verified.
- Does Plantix run full diagnosis offline? Not verified. Test the app.
- Accuracy compared to Nuru on cassava. Run our own benchmark on the same test images.
