# Market

Date: 2026-11-04. Every number has a source URL. "Estimate" means we could not verify it. Gaps are listed at the end.

## 1. Smallholders by country

| Country | Number | Source |
|---|---|---|
| Kenya | About 4.5 million small-scale farmers (government figure) | [IFPRI Kenya](https://kenya.ifpri.info/category/news/page/4) |
| Tanzania | About 3.7 million smallholdings, about 19 million people | [FAO](https://www.fao.org/family-farming/countries/tza/fr) |
| Uganda | Not verified | n/a |
| Rwanda | Not verified | n/a |
| DRC | Not verified | n/a |

Do not add these numbers into a regional total. They come from different years and methods. Next step: pull the national census figures for Uganda, Rwanda and DRC.

Serviceable market (estimate): if 1% of the 8.2 million Kenyan and Tanzanian smallholders use the app, that is about 82,000 farmers. We got this by adding the two verified numbers and taking 1%. It is a planning number, not a forecast.

## 2. Crop loss

| Crop | Loss figure | Source |
|---|---|---|
| Cassava | Cassava mosaic and brown streak diseases: over USD 1 billion a year combined | [PubMed review](https://pubmed.ncbi.nlm.nih.gov/28645239/) |
| Cassava | Brown streak in surveyed parts of Tanzania: over 860,000 tonnes, over USD 51 million a year | [IITA document](https://biblio.iita.org/documents/S03ProcGondweEconomicNothomDev.pdf-ae91c43dd5867327acb60e09d3c83386.pdf) |
| Maize | Fall armyworm: USD 9.4 billion a year in Africa (2021 modelled estimate) | [EurekAlert](https://www.eurekalert.org/news-releases/748111) |
| Maize | About 92% of African maize area supports year-round fall armyworm | [Farms.com](https://www.farms.com/news/almost-all-of-africa-s-maize-crop-is-at-risk-from-devastating-fall-armyworm-pest-study-reveals-189535.aspx) |
| Banana | Xanthomonas wilt: 10 to 17% yield loss per year in the Great Lakes region. Up to 100% on a single farm | [Wageningen repository](https://edepot.wur.nl/505782) |
| Banana | Black sigatoka: 20 to 70% loss without fungicide (global figure, not East Africa) | [FAO](https://www.fao.org/tempref/docrep/fao/meeting/018/k6106e.pdf) |

The sources mix years and methods. We use them to show scale, not to price a claim. Beans, tomato, potato and coffee: not researched yet.

## 3. Extension officer to farmer ratios

| Country | Ratio | Source |
|---|---|---|
| Kenya | Estimates 1:1,000 to 1:6,000. WFP gives 1:3,500 against a recommended 1:400 | [WFP](https://innovation.wfp.org/node/375), [Standard](https://www.standardmedia.co.ke/farmkenya/article/2001307098/extension-services-is-this-why-agriculture-is-hurting) |
| Tanzania | 1:1,172 for crop farmers (study). Recommended 1:200 to 500 | [Granthaalayah](https://www.granthaalayahpublication.org/journals/index.php/granthaalayah/article/view/IJRG19_A06_2381) |
| Uganda | Sources conflict. An advocacy statement says the current ratio is about 1:1,500 and asks for 1:500 | [CAPCA statement](https://capcauganda.org/wp-content/uploads/2017/02/Final-Statement-of-farmers-on-Agriculture-Extension-Services-f.pdf%20) |
| Rwanda | Not verified. Rwanda uses a farmer-to-farmer model (Twigire Muhinzi) | [Devex](https://www.devex.com/news/partnerships-aim-to-rebuild-kenya-s-agriculture-extension-services-96716) (search result only) |

These ratios are indicative. They are not official statistics.

## 4. Smartphones, Android and data

| Metric | Value | Source |
|---|---|---|
| Mobile internet use, Sub-Saharan Africa | About 27% of people. Usage gap 60% (2024 edition) | [GSMA](https://www.gsma.com/somic/) via secondary coverage |
| Mobile internet users on 3G smartphone or feature phone | 60% in Sub-Saharan Africa | [GSMA 2025 PDF](https://www.gsma.com/somic/wp-content/uploads/2025/09/The-State-of-Mobile-Internet-Connectivity-2025-Trends-in-Mobile-Internet-Connectivity.pdf) |
| Rural-urban usage gap | 48% | same |
| Smartphone adoption, Sub-Saharan Africa | 54% in 2024. Forecast 81% by 2030. Secondary source | [TechAfrica News](https://techafricanews.com/2026/03/05/2025-vs-2026-mobile-industry-checkpoint-has-anything-actually-changed-for-africa) |
| Android share of African mobile OS | About 81% (Aug 2026) | [StatCounter](https://gs.statcounter.com/os-market-share/mobile-tablet/africa/2025) |

Design consequence: 60% of mobile internet users are on 3G smartphones or feature phones. The model must run on low-end Android. We must test on 2 GB RAM devices. A feature-phone path is out of scope for v2.

Mobile data price per 1 GB, USD (Cable.co.uk survey, as reported 2026):

| Country | USD per GB |
|---|---|
| DRC | 0.52 |
| Rwanda | 0.55 |
| Kenya | 0.59 |
| Ethiopia | 0.68 |
| Tanzania | 0.84 |
| Uganda | 1.11 |

Source: [Les Afriques](https://lesafriques.com/en/2026/10/the-real-price-of-data-in-africa-ranking-of-53-countries-0-38-to-43-75-per-gb/). Other reports give different Uganda numbers (for example USD 0.02 in a 2023 summary: [Intelpoint](https://intelpoint.co/?p=5971)). Plan prices are not the same as what a poor farmer pays for small bundles. Cost as a share of income is higher than these figures suggest. The GSMA says an entry-level handset costs 87% of monthly income for the poorest 20% ([summary](https://www.gsma.com/somic/)).

## 5. Customer segments

| Segment | Who | Why they pay | Priority |
|---|---|---|---|
| Cooperatives | Crop and produce cooperatives with 500 to 10,000 members | Lower member crop loss. Better quality for buyers | 1 |
| NGO and development programmes | Programmes funded by donors that train farmers | Reach, outcome data, offline use | 1 |
| Agro-dealers | Input shops | Diagnosis leads to input sales | 2 |
| Agri-insurers and input companies | Insurers, seed and agrochemical firms | Outbreak data and claim evidence | 3 (phase 3) |
| Governments and extension services | Ministries, county offices | Scale extension | Partner, not buyer in phase 1 |

## 6. Personas

These are planning personas. They are not real people or interviews. Validate each one in the pilot.

**Farmer (Amina, 38, 1 hectare of maize and beans)**
- Has a low-end Android phone. Little or no data credit.
- Needs: a clear diagnosis today, in her language, with steps she can afford.
- Fears: wasting money on wrong chemicals. Cost of data.
- Wins when: the app works in the field with no signal.

**Lead farmer or extension agent (Joseph, 45)**
- Serves 300 to 1,500 farmers. Travels by motorbike.
- Needs: a second opinion in the field. Proof of what he saw.
- Fears: looking wrong in front of farmers.
- Wins when: he can diagnose in seconds and show the farmer the steps.

**Agro-dealer (Grace, 34)**
- Runs an input shop. Farmers bring her sick leaves.
- Needs: correct product advice. Safe-use rules.
- Fears: liability for wrong advice.
- Wins when: treatment steps match legal, registered products.

**Cooperative manager (Peter, 50)**
- Manages 2,000 members. Sells to buyers.
- Needs: fewer losses. Data on which blocks are hit.
- Fears: new cost per member.
- Wins when: a flat annual licence covers all members.

**NGO programme manager (Claire, 41)**
- Runs a donor-funded programme. Reports to funders.
- Needs: reach numbers, adoption data, safeguarding and data protection.
- Fears: tools that stop working when the grant ends.
- Wins when: open-source licence, offline use, anonymised dashboard.

**Agri-insurer or input company analyst (Daniel, 36)**
- Prices risk or plans sales by region.
- Needs: early outbreak signals by district.
- Fears: noisy or biased data.
- Wins when: coverage is large enough to trust the maps.

## 7. Gaps

- Smallholder counts for Uganda, Rwanda, DRC.
- Current 4G and smartphone share by country. The GSMA Mobile Economy 2025 report did not appear in search.
- Losses for beans, tomato, potato, coffee.
- Willingness to pay. Test it in the pilot.
