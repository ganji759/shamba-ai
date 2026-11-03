# Shamba AI v2: Executive Summary

Date: 2026-11-04. Status: MVP and phase 1 plan. Licence: Apache-2.0 (model and code).

Numbers marked "estimate" are not verified. Each estimate says how we got it. Sources are in `market.md`, `competition.md` and `business-model.md`.

## Problem

Smallholder farmers in East Africa lose crops to disease and pests. They have little expert help.

- Cassava mosaic and brown streak diseases together cost over USD 1 billion a year (review article, [PubMed](https://pubmed.ncbi.nlm.nih.gov/28645239/)).
- Fall armyworm costs Africa an estimated USD 9.4 billion a year in lost maize yield, a modelled figure from a CABI-linked study ([EurekAlert](https://www.eurekalert.org/news-releases/748111)).
- Extension officer ratios are far above the recommended level. Kenya estimates range from 1:1,000 to 1:6,000 farmers, and the recommended ratio is 1:400 ([WFP](https://innovation.wfp.org/node/375), [Standard](https://www.standardmedia.co.ke/farmkenya/article/2001307098/extension-services-is-this-why-agriculture-is-hurting)).
- In Sub-Saharan Africa about 27% of people use mobile internet (GSMA, [summary](https://www.gsma.com/somic/)). Many farms have no stable signal.

## Solution

A farmer photographs a leaf. A small vision model runs on the phone with PyTorch ExecuTorch. No internet is needed.

- The app gives a diagnosis and vetted treatment steps.
- Languages in v1: Swahili, English, French. Kinyarwanda is next on the roadmap.
- The knowledge base is stored on the device.
- Roadmap (phase 2): an optional small on-device LLM answers follow-up questions, grounded in the knowledge base.
- When online, anonymised results sync to a dashboard for outbreak maps.
- Crops in model v1: cassava, maize, beans, banana (14 classes, East African field photos). Roadmap: coffee, tomato, potato.

## Why on-device AI wins here

| Factor | Cloud app | Shamba AI (on-device) |
|---|---|---|
| Connectivity | Fails with no signal | Works offline |
| Data cost | Farmer pays to upload photos. 1 GB costs USD 0.52 to 1.11 in our target countries ([Cable.co.uk via Les Afriques](https://lesafriques.com/en/2026/10/the-real-price-of-data-in-africa-ranking-of-53-countries-0-38-to-43-75-per-gb/)) | Zero data per diagnosis |
| Privacy | Farm photos and location leave the phone | Photos stay on the phone. Only anonymised results sync |
| Marginal cost | Per-call API fee. Cloud vision lists USD 1.50 per 1,000 images ([Google](https://cloud.google.com/vision/pricing)) | Zero marginal inference cost |
| Phone | Needs good network | Runs on Android. Android holds about 81% of African mobile OS share ([StatCounter](https://gs.statcounter.com/os-market-share/mobile-tablet/africa/2025)) |

Cost detail is in `business-model.md`. Cloud cost per diagnosis is small for one farmer. It grows with every user and every follow-up question. On-device cost does not.

## Target customers

We sell to organisations that serve farmers (B2B2F). Farmers use the app free.

1. Cooperatives and farmer groups.
2. NGO and development programmes.
3. Agro-dealers.
4. Agri-insurers and input companies that need outbreak data.

## Business model

- B2B2F licences and support for cooperatives, NGOs and agro-dealers.
- White-label app.
- Outbreak-intelligence dashboard subscription.
- Custom model fine-tuning for new crops and regions.
- Grants for the open-source core.

Prices are in `business-model.md`.

## Traction plan

- Phase 1 (months 0 to 6): one 8-week pilot. One cooperative, 200 farmers. Open release on GitHub and Hugging Face.
- Phase 2 (months 6 to 18): scale to several cooperatives and NGO programmes. First paid licences.
- Phase 3 (months 18 to 36): platform. Partners add crops and languages. Dashboard sold to insurers and input companies.

Details are in `go-to-market.md`.

## Ask

- Pilot partner: one cooperative that will enrol 200 farmers.
- Grant funding for phase 1. Targets are in `business-model.md`.
- Field agronomists to review the treatment knowledge base.
- Open-source contributors for models, translations and test devices.

Amount of the ask: estimate USD 60,000 to 120,000 for 12 months. We built this from a small team of 3 to 4 people in East Africa, plus device, field and review costs. Replace it with a real budget before any submission.
