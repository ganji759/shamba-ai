# Go-to-Market

Date: 2026-11-04. Targets are estimates. They are planning goals, not forecasts. We name no partner as confirmed. Every partner below is a target to approach.

## 1. Pilot design

| Item | Plan |
|---|---|
| Partner | One cooperative with at least 500 members. Crops: cassava or maize first |
| Farmers | 200 enrolled. Include at least 40% women (estimate target) |
| Length | 8 weeks |
| Devices | Farmers use their own Android phones. Test-phone pool of 20 for those without one |
| Support | 1 field lead, 4 lead farmers as agents, 1 agronomist reviewer |
| Languages | Match the region. Kenya or Tanzania: Swahili. Rwanda: Kinyarwanda |
| Consent | Written consent for data sync. Coarse location only. Photos stay on the phone unless the farmer opts in |

### Timeline

| Week | Activity |
|---|---|
| Before 0 | Sign MoU. Pick 200 farmers. Collect 50 baseline field photos per crop for testing. Train 4 lead farmers |
| 0 | Install the app. 2-hour training per group. Baseline survey: crop loss, how farmers find help today |
| 1 to 7 | Farmers use the app. Weekly lead-farmer visit. Weekly check of sync data. Fix bugs fast |
| 4 | Mid-point review. Compare app diagnosis with agronomist on 30 samples |
| 8 | End survey. Interviews with 20 farmers. Review with the cooperative manager |
| After 8 | Report published. Open results on GitHub |

### Metrics

| Metric | How measured | Target (estimate) |
|---|---|---|
| Diagnosis accuracy | Agronomist-confirmed sample of at least 100 cases | At least 85% on cassava or maize |
| Time to diagnosis | App log | Under 3 seconds |
| Weekly active farmers | App log | 50% at week 8 |
| Diagnoses per active farmer | App log | At least 3 |
| Treatment steps followed | Survey | At least 40% |
| Perceived usefulness | Survey, 1 to 5 | At least 4 |
| Offline share of use | App log | Report only |
| Willingness to pay | Cooperative manager interview, price test at USD 0.75 per member | At least a letter of intent |
| Safety | Count of harmful advice incidents | Zero |

### Go or stop rules

- Go: accuracy at least 85%, weekly active at least 40%, no harm incidents, cooperative asks to continue.
- Fix first: accuracy 70 to 85%. Add field images and retrain.
- Stop and rethink: accuracy under 70%, or farmers stop using it after week 3.

## 2. Channels

| Channel | Use | Phase |
|---|---|---|
| Cooperatives | Main route to farmers and first licences | 1 |
| Lead farmers and extension agents | Training and trust | 1 |
| NGO programmes | Bulk deployment, grant-funded | 1 to 2 |
| Agro-dealers | Diagnosis at the shop | 2 |
| GitHub and Hugging Face | Developers, researchers, forks | 1 |
| PyTorch community | Visibility, contributors | 1 |
| App stores and APK sharing | Many farmers share APKs by Bluetooth or SD card. Plan for it (estimate, validate in pilot) | 1 |
| Radio and community meetings | Awareness | 2 |
| Dashboard sales to insurers and input firms | Revenue | 3 |

## 3. Partnerships to approach

These are targets. None has agreed to anything.

- Cooperatives in Kenya, Tanzania or Rwanda for the pilot. Find through county or district agriculture offices.
- PlantVillage Nuru team (Penn State). Ask to share data, compare results and avoid duplicate work.
- Digital Green (Farmer.CHAT). Ask about a link where Farmer.CHAT calls our diagnosis.
- IITA and national agricultural research bodies for cassava and banana disease expertise and label review.
- CABI and FAO for pest and disease knowledge and fall armyworm data (FAMEWS).
- Local universities for agronomy review and student interns.
- Safaricom and other mobile operators as a channel. Long shot for phase 1.
- Funders: see `business-model.md` section 7.

## 4. Marketing messages per persona

| Persona | Message |
|---|---|
| Farmer | "Take a photo. Get help in Swahili. No internet. No cost." |
| Lead farmer or extension agent | "Diagnose in seconds in the field. Show the farmer the steps. Spend your day on the hard cases." |
| Agro-dealer | "Give the right advice the first time. Match every diagnosis to registered products." |
| Cooperative manager | "One flat fee covers every member. See which blocks have disease." |
| NGO programme manager | "Offline, open source, and measurable. Your programme keeps the tool when the grant ends." |
| Insurer or input company | "District-level outbreak signals from real field photos. Anonymised and aggregated." |
| Developer or researcher | "Apache-2.0 plant-health model on ExecuTorch. Fork it. Add your crop." |

Rules for all messages: no claim of accuracy until we measure it. No promise of yield gains. Say "ask an expert" for unclear cases.

## 5. Launch checklist

### GitHub
- [ ] Repository public with Apache-2.0 licence file.
- [ ] README with a 5-minute quick start and device requirements.
- [ ] Model card: data, accuracy per crop, known limits.
- [ ] Contributing guide and code of conduct.
- [ ] Release APK and ExecuTorch model file.
- [ ] Issues labelled "good first issue" and "translation".

### Hugging Face
- [ ] Model repo with model card and licence.
- [ ] Dataset repo for any data we have the right to share.
- [ ] Demo Space with sample leaf photos.

### Documentation
- [ ] Knowledge base format and review process.
- [ ] Privacy notice in 4 languages.
- [ ] Data sync and anonymisation description.

### Demo video
- [ ] 2 minutes. Real leaf, phone in airplane mode, diagnosis, treatment steps in Swahili.
- [ ] Subtitles in English and French.
- [ ] Upload to YouTube and attach to the README.

### PyTorch community blog
- [ ] Post: "Running a crop-disease model offline with ExecuTorch". Include size, latency and quantisation results measured on real phones.
- [ ] Submit to the PyTorch blog or community channels. Acceptance is not guaranteed.

### LinkedIn
- [ ] Founder post with the demo video and the pilot call for partners.
- [ ] Short follow-ups: accuracy results, pilot week notes, lessons.
- [ ] Tag partners only after they agree.

### Before the pilot
- [ ] Agronomist review of the knowledge base for the pilot crop.
- [ ] Safety review of every chemical step.
- [ ] Test on at least 5 low-end Android devices.
- [ ] Consent forms and data-protection check for the pilot country.
