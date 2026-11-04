# Pitch Outline

Date: 2026-11-04. Numbers link to sources in `market.md` and `business-model.md`. Numbers marked "estimate" are not verified.

## Tagline

Point your phone at a sick leaf and get a diagnosis in your language, with no internet.

## Slide 1: Title

- Shamba AI v2: offline crop-health assistant for East African smallholders.
- Open source, Apache-2.0. Runs on PyTorch ExecuTorch.
- Model v1: cassava, maize, beans, banana. Next: coffee, tomato, potato.
- Swahili, English, French. Next: Kinyarwanda.
- Founder: PyTorch community lead, based in East Africa and DRC.

## Slide 2: Problem

- Cassava mosaic and brown streak cost over USD 1 billion a year.
- Fall armyworm costs Africa about USD 9.4 billion a year in maize (2021 modelled estimate).
- Kenya has between 1 officer per 1,000 and 1 per 6,000 farmers. The recommended ratio is 1:400.
- Farmers find out what is wrong with a crop after it is too late.

## Slide 3: Why current tools fail

- Cloud apps need signal. About 27% of people in Sub-Saharan Africa use mobile internet.
- Data costs USD 0.52 to 1.11 per GB in our target countries.
- Chat and SMS tools charge per message or need a connection.
- Few tools cover Kinyarwanda and vetted local treatment steps.

## Slide 4: Solution

- Photograph a leaf. A small vision model on the phone returns a diagnosis.
- Vetted treatment steps come from an offline knowledge base in 4 languages.
- Next: an optional on-device LLM for follow-up questions, grounded in the knowledge base.
- Online, anonymised results sync to an outbreak dashboard.

## Slide 5: Why on-device wins

- Works with no signal.
- Zero data cost for the farmer. Photos stay on the phone.
- Zero marginal inference cost. Cloud vision lists USD 1.50 per 1,000 images.
- Cost per partner stays flat as users grow.
- Android is about 81% of African mobile OS share.

## Slide 6: Product and technology

- PyTorch model, exported with ExecuTorch (1.0 general availability, Oct 2025).
- Target: under 3 seconds on a 2 GB RAM Android phone (estimate, to be measured).
- Knowledge base reviewed by agronomists.
- Sync queue sends anonymised, coarse-location records when online.

## Slide 7: Market and customers

- Kenya: about 4.5 million small-scale farmers. Tanzania: about 3.7 million smallholdings.
- We sell to cooperatives, NGO programmes and agro-dealers. Farmers pay nothing.
- Later: insurers and input companies buy outbreak data.
- Competitors: Nuru (offline, free, cassava first), Plantix (closed). We add four crops in one model, an open licence, and a web app that runs the same model in the browser.

## Slide 8: Business model

- B2B2F licences: cooperative about USD 1,500 per year (estimate).
- White-label app and dashboard subscriptions.
- Custom fine-tuning for new crops and regions.
- Grants fund the open core in phase 1.

## Slide 9: Traction plan

- Month 0 to 2: open release on GitHub and Hugging Face. Pilot design.
- Month 2 to 4: 8-week pilot. One cooperative, 200 farmers.
- Month 4 to 6: results, first paid licence, grant applications.
- Targets: 85% accuracy on cassava and maize, 50% weekly active, 1 paid partner (estimates).

## Slide 10: Ask

- One cooperative to run the pilot.
- Grant funding. Estimate USD 60,000 to 120,000 for 12 months.
- Agronomists to review treatment steps.
- Contributors and test phones.

## 60-second spoken pitch

A cassava farmer in Kenya sees yellow spots on her leaves. The extension officer is two days away and covers thousands of farmers. The app that could help needs internet. She has none, and data is costly.

Shamba AI fixes this. She takes a photo. A small model on her own phone says what is wrong. It gives treatment steps in Swahili, vetted by agronomists. It works with no signal. It costs her nothing.

Each diagnosis costs us nothing to run, because it runs on the phone. That lets cooperatives and NGOs give the app to every member for a flat yearly fee. When phones go online, anonymised results build an outbreak map.

The model and code are open source on PyTorch ExecuTorch. We sell service around it: licences, dashboards and custom models.

We are starting with one cooperative, 200 farmers, 8 weeks. We need a pilot partner and grant funding. Help us put an agronomist in every pocket.
