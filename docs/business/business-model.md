# Business Model

Date: 2026-11-04. All prices in USD. Prices and costs marked "estimate" are our assumptions. Test them in the pilot.

## 1. Lean Canvas

| Block | Content |
|---|---|
| Problem | 1. Crop disease losses. 2. Too few extension officers. 3. No signal, and data costs money. |
| Customer segments | Cooperatives, NGO programmes, agro-dealers. Later: insurers, input companies. Farmers use the app free. |
| Unique value | Leaf photo to diagnosis and treatment in 4 languages, fully offline, with zero cost per diagnosis. |
| Solution | ExecuTorch vision model, offline knowledge base, anonymised sync to outbreak dashboard. Phase 2: optional on-device LLM. |
| Channels | Cooperatives, NGO programmes, agro-dealer networks, GitHub, Hugging Face, PyTorch community. |
| Revenue streams | Licences, white-label, dashboard subscription, fine-tuning projects, grants. |
| Cost structure | Small team, agronomist review, field pilots, devices, dashboard hosting, translation review. |
| Key metrics | Diagnoses per active farmer, accuracy on field images, farmers retained at 8 weeks, paying partners. |
| Unfair advantage | Open-source community, PyTorch expertise, offline-first stack, a vetted multilingual knowledge base, local trust. |

## 2. Revenue streams

1. **B2B2F licences.** A cooperative, NGO or agro-dealer pays a yearly fee. Their farmers use the app free. Fee covers support, training, updates and a branded dashboard view.
2. **White-label app.** A partner gets the app under its own name and colours. One-time setup fee plus a yearly fee.
3. **Outbreak-intelligence dashboard.** A yearly subscription for district-level outbreak maps. Buyers: NGOs, governments, insurers, input companies. Data is anonymised and aggregated.
4. **Custom model fine-tuning.** A partner pays to add a crop, a disease or a language, or to adapt the model to local field images.
5. **Grants.** Fund the open-source core and the first pilots. See section 7.

The code and model stay Apache-2.0. Anyone can run them. Customers pay for trust, support, updates, data and local work.

## 3. Pricing (estimates)

We have no market test yet. These are starting points for the pilot. We built them from cost plus a low price per farmer.

| Offer | Price (USD) | Basis |
|---|---|---|
| Cooperative licence | 1,500 per year for up to 2,000 members (about 0.75 per member) | Estimate. Set below the cost of one extension visit |
| NGO programme licence | 5,000 per year for up to 10,000 farmers, plus training days at 300 per day | Estimate |
| Agro-dealer licence | 120 per shop per year | Estimate |
| White-label app | 8,000 setup plus 3,000 per year | Estimate. Covers branding, store listing, content review |
| Dashboard subscription: district | 2,400 per year | Estimate |
| Dashboard subscription: country | 12,000 per year | Estimate |
| Custom fine-tuning: new crop or language | 10,000 to 25,000 per project | Estimate. Depends on data collection |
| Farmer | Free | Core promise |

## 4. Unit economics: cloud API vs on-device

Known price: Google Cloud Vision lists label detection at USD 1.50 per 1,000 images after the first 1,000 free each month ([Google](https://cloud.google.com/vision/pricing)). That is a generic API. A custom plant model on a hosted endpoint has its own cost. We have no verified price for it.

Mobile data per GB: Kenya 0.59, Rwanda 0.55, Tanzania 0.84 ([Les Afriques](https://lesafriques.com/en/2026/10/the-real-price-of-data-in-africa-ranking-of-53-countries-0-38-to-43-75-per-gb/)).

Assumptions (estimate):
- Photo upload: 0.3 MB compressed, so 0.0003 GB.
- Follow-up question cloud cost: USD 0.001 per turn. We have no verified source. It is a typical order of magnitude for a small hosted LLM. Check the price page of your provider.
- One farmer: 20 diagnoses and 40 follow-up questions per year.

| Item | Cloud app | Shamba AI on-device |
|---|---|---|
| Inference per diagnosis | 0.0015 | 0 |
| Data per diagnosis, paid by farmer (0.0003 GB x 0.59) | about 0.0002 | 0 |
| Follow-up question | about 0.001 (estimate) | 0 |
| One farmer, one year | 20 x 0.0015 + 40 x 0.001 = 0.07 | 0 |
| 100,000 farmers, one year | about 7,000 | 0 |
| 1,000,000 farmers, one year | about 70,000 | 0 |

Plain reading:
- Per call, cloud is cheap. At pilot size the saving is small.
- The cost grows with usage and cannot be capped. A flat partner licence cannot cover an open-ended per-call bill without risk. On-device cost stays flat.
- The farmer also saves data and works with no signal. This matters more than the API fee.
- Our real costs are fixed: team, review, dashboard hosting for small synced records, support. Dashboard hosting cost: estimate under USD 100 per month at pilot scale, since each record is a few hundred bytes.

Margin example (estimate): one cooperative licence at 1,500 per year, with support cost of 400 per year, leaves 1,100. Ten cooperatives leave 11,000. A team needs more than that. Licences alone do not fund the company in phase 1. Grants carry phase 1.

## 5. Three-phase roadmap

| Phase | Time | Goal | Output |
|---|---|---|---|
| 1. MVP pilot | Months 0 to 6 | Prove accuracy and use in the field | Open release. One 8-week pilot with 200 farmers. Knowledge base reviewed by agronomists. First grant |
| 2. Scale | Months 6 to 18 | 5 to 10 partners in 2 to 3 countries | Paid licences. Field-tuned models for cassava, maize, banana. Dashboard v1. 5,000 to 20,000 farmers (estimate) |
| 3. Platform | Months 18 to 36 | Others build on Shamba | Partner SDK. Community-contributed crops and languages. Dashboard subscriptions to insurers and input firms |

## 6. KPIs

| Area | KPI | Phase 1 target (estimate) |
|---|---|---|
| Model | Top-1 accuracy on field photos, per crop | At least 85% on cassava and maize |
| Model | Latency on a 2 GB RAM Android phone | Under 3 seconds |
| Use | Weekly active farmers share | 50% at week 8 |
| Use | Diagnoses per active farmer | At least 3 |
| Trust | Farmers who follow treatment steps | At least 40% |
| Outcome | Farmers who report less loss | Measure, no target yet |
| Business | Partners who sign a paid licence after pilot | At least 1 |
| Business | Grant funding secured | At least 1 grant |
| Community | GitHub stars, contributors, Hugging Face downloads | 5 external contributors |

## 7. Grant and accelerator targets

Verified facts come from search. Where we found nothing current, we say so. Always read the live call page before you plan around a date or amount.

| Funder | What we verified | Fit | Next step |
|---|---|---|---|
| Lacuna Fund | Funds ML datasets in agriculture, language, health, climate. Agriculture rounds in 2020 and 2021. Recent rounds were other domains. Past budgets in the climate round: USD 50,000 to 300,000 ([Lacuna](https://lacunafund.org/apply/), [Terra Viva](https://www.terravivagrants.org/datasets-climate-and-forests/)) | Strong. We need labelled field images | Watch for a new agriculture call |
| IDRC AI4D Africa | Agriculture call (CAD 1.2 million hub) ran in 2021 and is closed. Successor program "AI4D: Responsible AI, Empowering People" with the UK FCDO ([AI4D](https://africa.ai4d.ai/calls/agriculture-food-systems/)) | Medium. Needs an African research partner | Check ai4d.ai for open calls |
| Meta Llama Impact grant | USD 20,000 per winner in 2025 for Sub-Saharan startups and researchers, with Data Science Africa ([Nairametrics](https://nairametrics.com/2025/03/19/meta-announces-llama-impact-grant-for-sub-saharan-african-startups-researchers/)). We use a small LLM, so check Llama licence fit | Medium | Apply when the next round opens |
| Catalyst Fund | Now an equity fund. Pre-seed to Series A. Portfolio has agtech firms. Fund size reports conflict (USD 30 to 40 million) ([Wamda](https://www.wamda.com/2026/07/catalyst-fund-secures-30-million-africas-climatetech-startups)) | Later (phase 2), after revenue | Prepare traction data |
| Mozilla (Common Voice) | USD 400,000 awards for Kiswahili voice tech, up to USD 50,000 each, 2022 round ([Mozilla](https://www.mozillafoundation.org/en/blog/common-voice-announces-400000-in-awards-for-kiswahili-voice-technologies/)). Closed | Low for now. Useful for voice input later | Watch for new calls |
| Villgro Africa | Listed as a Responsible AI innovation program on VC4A ([VC4A](https://vc4a.com/villgro-africa/responsible-ai-innovation/)). Terms not verified | Possible | Read the program page |
| Gates Foundation | Funds open voice data in Kinyarwanda, Kiswahili and Luganda via Mozilla ([Mozilla](https://www.mozillafoundation.org/ca/what-we-fund/programs/common-voice-kiswahili-awards/)). Agriculture grants not verified here | Possible, usually via partners | Approach through a partner NGO |
| Google.org | Not verified | Unknown | Research before listing |
| AGRA | Not verified | Unknown. Likely partner for farmer reach | Research before listing |
| PyTorch Foundation | Not verified. ExecuTorch 1.0 reached general availability on 22 Oct 2025 ([PyTorch blog](https://pytorch.org/blog/introducing-executorch-1-0/)) | Community and visibility. Funding not verified | Ask in the community. Submit a blog post |

## 8. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Model is wrong on real field photos | Harm to farmers, loss of trust | Field-test on real images. Show confidence. Say "ask an expert" below a threshold. Agronomist-reviewed advice only |
| Unsafe chemical advice | Health and legal risk | Use only registered products. Review by local agronomists. Prefer cultural and integrated steps |
| Low-end phones too slow | Poor use | Quantise. Test on 2 GB RAM. Keep a lighter model |
| On-device LLM gives wrong answers | Misinformation | Ground it in the knowledge base. Make it optional. Show source steps |
| Weak willingness to pay | No revenue | Test price in pilot. Grants carry phase 1. Sell data and services, not seats |
| Open-source copying | Others use our model | This is allowed. We sell support, data, trust and local work |
| Competitor is free (Nuru) | Hard to win users | Partner where possible. Win on crop breadth, browser plus Android reach, treatment steps |
| Data privacy | Legal and trust risk | Anonymise. Coarse location. Consent in app. Check each country's data law |
| Founder bandwidth | Slow delivery | Recruit a field lead and an agronomist early |
| Language quality | Bad translations | Native review for Swahili and French now, Kinyarwanda when added |
