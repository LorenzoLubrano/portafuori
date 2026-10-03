# Portafuori: specifica di prodotto v1.0

*Stasera cosa esce? Te lo dice Portafuori, la sera prima.*

- **Data:** 23/09/2026
- **Piattaforma:** Android 8.0+ (minSdk 26, targetSdk 36), APK installato a mano (non sul Play Store)
- **Tecnologia:** Kotlin + Jetpack Compose (Material 3)
- **Lingua:** interfaccia in italiano
- **Modello:** gratis per sempre, offline, senza account, senza pubblicità, senza acquisti in-app, senza permesso Internet

---

## 1. Il problema

Nella raccolta porta a porta ogni comune, e spesso ogni zona, ha il suo calendario. Il calendario dice:

- quale mastello esporre e in quale sera;
- la finestra di esposizione (ad esempio dalle 22:00 alle 05:00);
- i ritiri stagionali, come il multimateriale anche il sabato dal 1° maggio al 31 ottobre (Forio, 2025);
- i ritiri mensili, come gli imballaggi metallici il martedì una volta al mese (zone di Cuneo);
- gli spostamenti per le festività.

Quasi sempre arriva come PDF o volantino, da appendere al frigo.

Quando ci si dimentica, il mastello resta pieno per una settimana o più. Quando si sbaglia giorno o mastello, o lo si lascia fuori troppo a lungo, i regolamenti locali prevedono sanzioni: a Quartu Sant'Elena (regole del 2022) da 50 a 500 EUR per giorno o mastello sbagliato e da 100 a 450 EUR per il mastello lasciato fuori. Chi lavora su turni o rientra tardi trova gli orari «fuori dalla realtà per chi lavora» (lettera a La Voce di Imperia, 8/9/2026), e in alcuni comuni si rischia la multa anche solo per ritirare il mastello dopo mezzogiorno.

## 2. Perché le app esistenti non bastano

| Soluzione | Limite |
|---|---|
| App dei gestori e dei comuni | Una app per ogni territorio: il calendario c'è solo dove l'ente l'ha attivata, spesso a pagamento. L'orario delle notifiche di solito non si può scegliere. |
| Promemoria semplici | Solo giorni e cadenza fissi. Non gestiscono stagioni né festività, non hanno widget e non si possono condividere con i vicini. |
| App generiche per i bidoni | Pubblicità e tracciamento, oppure abbandonate. Non conoscono le festività italiane. |
| Evento ricorrente in un calendario | Nessun tipo di mastello, nessun avviso combinato («Plastica + Vetro»), niente stagioni né finestra di esposizione, nessuna domanda sui festivi. |

Su F-Droid non esiste nessuna app di questo tipo, in nessuna lingua.

## 3. Per chi

- Chi vive in un comune non coperto dalle app dei gestori, o dove la zona è sbagliata o introvabile.
- Chi lavora su turni e vuole scegliere l'ora dell'avviso.
- Chi ha due case (seconda casa, casa dei genitori), gli studenti fuori sede, chi si trasferisce.
- Condomini e gruppi WhatsApp di via, dove un vicino inserisce il calendario e gli altri lo importano con un QR.

## 4. Principi

1. **Il promemoria deve arrivare.** L'affidabilità è una funzione, non un dettaglio: l'app controlla se ha funzionato e lo dice.
2. **Lo inserisci una volta sola.** Circa 5 minuti con la procedura guidata, oppure un QR del vicino.
3. **Mai indovinare.** Festivi e cambi di calendario si chiedono all'utente, non si deducono.
4. **Offline e privato.** Nessun permesso Internet, nessun dato lascia il telefono.
5. **Leggibile da tutti.** Ogni mastello ha colore, icona e nome insieme, così non si distingue mai solo dal colore.

## 5. Funzioni della v1 e priorità

### Indispensabili (M): il valore principale dal primo giorno

1. **Mastelli con preset italiani.**
   - Preset: Organico, Plastica e metalli, Carta e cartone, Vetro, Indifferenziato/Secco residuo, Multimateriale, Sfalci e potature, Pannolini e pannoloni.
   - Si possono aggiungere mastelli personalizzati.
   - Colore modificabile (cambia da comune a comune) e icona.
2. **Motore delle regole.**
   - Tipi di regola: settimanale; ogni N settimane da una data di riferimento; N-esimo giorno del mese (anche «ultimo» e «1° e 3°»); date fisse con la modalità «segna date».
   - Ogni regola può avere un periodo stagionale annuale (anche a cavallo di capodanno) e un periodo di validità.
   - Eccezioni su singole date: salta, sposta, aggiungi.
3. **Configurazione guidata con anteprima.**
   - La domanda chiave: «Il tuo calendario indica il giorno in cui esporre (la sera) o il giorno del ritiro?».
   - Prima di salvare, un'anteprima di 4 settimane: «lun 29/09 sera: Plastica + Vetro, ritiro mar 30/09».
4. **Promemoria «Esponi».**
   - Un solo avviso per sera e per profilo, con tutti i mastelli: «Stasera: Plastica e metalli + Vetro. Esponi dalle 22:00 (ritiro entro le 05:00)».
   - L'orario lo sceglie l'utente.
   - Pulsanti: «Fatto» e «Tra 30 min».
5. **Home «Stasera / Domani».**
   - Scheda grande con i mastelli di stasera oppure «Stasera niente», la finestra di esposizione e lo stato (da esporre / esposto).
   - Sotto: domani e i prossimi 7 giorni.
   - Banner quando qualcosa non va.
6. **Calendario mensile.**
   - Pallini colorati per ogni mastello nei giorni di raccolta.
   - Toccando un giorno si apre il dettaglio, da cui saltare, spostare o aggiungere un ritiro.
7. **Festività italiane calcolate offline.**
   - Date fisse, Pasqua e Lunedì dell'Angelo (algoritmo di Meeus), 4 ottobre dal 2026 (Legge 151/2025).
   - L'utente aggiunge le festività locali, come il santo patrono.
   - Se un ritiro cade in un festivo, l'app chiede cosa fare: «Si fa / Salta / Sposta al…».
8. **Affidabilità promemoria.**
   - Lista di controllo con voci verdi o rosse.
   - Guide per marca di telefono.
   - Notifica di prova e prova tra 1 minuto.
   - Registro dei promemoria in ritardo o mancati.
9. **Widget «Stasera».**
   - Formati 2x1 e 4x2, con pulsante «Fatto».
   - Si aggiorna insieme alla catena di sveglie.
10. **Backup e ripristino JSON.**
    - Esportazione e ripristino tramite Storage Access Framework.
    - Mostra la data dell'ultimo backup.

### Importanti (S)

11. **Promemoria «Prepara» e «Ritira il mastello»**, ciascuno a un orario scelto dall'utente.
12. **Condivisione con i vicini.**
    - QR generato sul telefono, da mostrare o da inviare come immagine.
    - Importazione da fotocamera, da uno screenshot in galleria, da un file o da un codice incollato.
    - Anteprima prima di importare.
13. **Profili multipli** (Casa, Seconda casa) e **pausa vacanza** («Sono via dal… al…»).
14. **Backup automatico settimanale** in una cartella scelta dall'utente, che resta anche se si disinstalla l'app.
15. **Controllo annuale.** Dal 1° dicembre l'app chiede: «Il calendario 2027 è uscito? Verifica le regole».
16. **Note del profilo:** orari dell'isola ecologica, numero per gli ingombranti, contatti del gestore. I numeri si chiamano con un tocco.

### Facoltative (C)

17. Glossario personale «Dove lo butto?», modificabile.
18. Modalità insistente (ripete l'avviso dopo 45 minuti se non hai premuto «Fatto») e modalità sveglia (setAlarmClock).
19. Esportazione .ics per i prossimi 12 mesi.
20. Scorciatoia dall'icona e riquadro nelle Impostazioni rapide: «Stasera cosa esce?».

## 6. Schermate

1. **Benvenuto.**
   - Titolo «Stasera cosa esce?».
   - Pulsanti «Configura il mio calendario» e «Importa da un vicino».
   - Nota «Senza Internet, senza account, senza pubblicità».
2. **Configurazione guidata**, in 8 passi:
   1. nome del profilo e zona;
   2. scelta dei mastelli;
   3. «il calendario indica sera di esposizione o giorno di ritiro?»;
   4. giorni di ogni mastello (chip Lun–Dom, più «Altre regole…»);
   5. finestra di esposizione («la sera prima» / «la mattina stessa», dalle / entro le);
   6. orari dei promemoria;
   7. anteprima di 4 settimane;
   8. attivazione delle notifiche e rimando ad Affidabilità.
3. **Home (Oggi).**
   - Selettore del profilo.
   - Scheda Stasera con il pulsante «Fatto».
   - Scheda Domani.
   - Lista dei prossimi 7 giorni.
   - Banner: notifiche disattivate, promemoria in ritardo, festivo da decidere, backup mai fatto.
   - Barra in basso: Oggi · Calendario · Mastelli · Condividi.
4. **Calendario.**
   - Griglia del mese con pallini colorati, festivi evidenziati, segno sulle eccezioni.
   - Legenda dei mastelli.
5. **Dettaglio giorno.**
   - Mastelli del giorno, ognuno con la sua origine («regola: ogni martedì», «spostato da…»).
   - Azioni: Salta, Sposta, Annulla; «+ Raccolta straordinaria»; scelta per i festivi.
6. **Mastelli e regole.**
   - Elenco dei mastelli con un riassunto leggibile («Mar e ven; dal 1/5 al 31/10 anche sab»).
   - Editor del mastello: nome, colore, icona, finestra propria (facoltativa), regole.
   - Editor della regola: tipo, campi, periodo stagionale, validità, anteprima delle prossime 8 date.
   - Modalità «Segna date» per i calendari irregolari.
7. **Festività ed eccezioni.**
   - Festivi dei prossimi 12 mesi, con la decisione per ogni ritiro coinvolto.
   - Regola predefinita del profilo (Chiedimi / Si ritira comunque / Salta).
   - «+ Festività locale».
   - Elenco di tutte le eccezioni.
8. **Affidabilità promemoria.**
   - Voci da controllare: notifiche, sveglie esatte, ottimizzazione batteria, «sospendi attività se inutilizzata», widget presente.
   - Pulsante «Sistema» su ogni voce rossa.
   - Guide illustrate per Samsung, Xiaomi/Redmi/POCO, Huawei/Honor, OPPO/realme/OnePlus.
   - Test immediato e test tra 1 minuto.
   - Storico di 30 giorni: orario previsto e orario di consegna.
9. **Condividi e backup.**
   - QR a tutto schermo con luminosità massima; «Invia come immagine».
   - «Scansiona», «Importa da immagine», «Invia file», «Importa file», «Incolla codice».
   - Anteprima dell'importazione, con scelta «Crea nuovo profilo» o «Sostituisci».
   - Backup manuale, backup automatico e data dell'ultimo backup.
10. **Impostazioni e profilo.**
    - Orari dei promemoria, pausa vacanza, note, tema.
    - Info e licenze; «Nessun dato lascia il telefono».
    - Avviso: «Verifica sempre il calendario ufficiale del tuo comune».
11. **Widget.**
    - 2x1: «Stasera: Plastica + Vetro · dalle 22:00», oppure «Stasera niente · gio: Carta».
    - 4x2: stasera, domani e «Fatto».

## 7. Modello dati (Room + DataStore)

**Tabelle Room**

- **Profile:** id, name, areaNote, zoneId = Europe/Rome, calendarMode (EXPOSE_DAY / COLLECTION_DAY), exposureMode (SERA_PRIMA / MATTINA_STESSA), exposureStart, exposureEnd, holidayPolicy (ASK / KEEP / SKIP), pausedFrom/To, notes, sortOrder.
- **Bin:** id, profileId, presetKey, name, colorArgb, iconKey, override della finestra (facoltativo), sortOrder, archived.
- **CollectionRule:** id, binId, type (WEEKLY / EVERY_N_WEEKS / MONTHLY_NTH / FIXED_DATES), più i campi del tipo:
  - weekdays;
  - weekday + intervalWeeks + anchorDate;
  - weekday + ordinals (-1 = ultimo);
  - dates.

  A questi si aggiungono seasonStart/seasonEnd (MonthDay) e validFrom/validUntil.
- **DateException:** id, profileId, binId, kind (SKIP / MOVE / ADD), date, targetDate, reason (USER / HOLIDAY_DECISION / IMPORT), note.
- **CustomHoliday:** festività locale annuale (MonthDay) o una tantum (LocalDate).
- **ReminderConfig**, una per profilo: expose, prepare e retrieve, ciascuno con abilitazione e orario; insistent; alarmClockMode.
- **DeliveryLog:** slotKey, kind, collectionDate, scheduledAt, postedAt, lateByMinutes, recoveredByWatchdog, acknowledgedAt. Si cancella dopo 60 giorni.
- **Snooze** e **ExposureConfirmation.**

**Dati calcolati, non salvati**

- **Occurrence:** profilo, mastello, data di ritiro, inizio e fine della finestra come ZonedDateTime, origine, festivo, decisione necessaria.
- **ReminderSlot:** tipo (PREPARE / EXPOSE / RETRIEVE / SNOOZE / HOLIDAY_ASK / WIDGET_REFRESH), istante, data, mastelli.

**DataStore:** onboarding, tema, lastBackupAt, autoBackupTreeUri, lastWatchdogRunAt, controllo annuale.

**Formato di scambio:** ShareEnvelope in JSON (kotlinx.serialization), con i campi `v`, `app: "portafuori"`, `kind: profile|backup`, `exportedAt` e `profiles[…]`.

- Nel QR il testo è `PORTAFUORI1:` + base64url(deflate(json)).
- I campi sconosciuti vengono ignorati.
- Gli orari personali dei promemoria entrano solo nei backup, mai nelle condivisioni.

## 8. Motore delle regole (modulo Kotlin puro `:core:rules`)

Per ogni data dell'intervallo richiesto e per ogni mastello:

1. la data vale se almeno una regola corrisponde **e** la data sta nel periodo stagionale e in quello di validità;
2. si applicano le eccezioni: SKIP toglie, MOVE toglie da una data e aggiunge all'altra, ADD aggiunge;
3. se la data è festiva e non c'è una decisione, si applica la regola predefinita del profilo. Con «Chiedimi» il ritiro resta in calendario, segnato come «da decidere».

Dalla data di ritiro si calcolano:

- la finestra di esposizione, dalla sera prima o dalla mattina stessa, come ZonedDateTime in Europe/Rome. Gli orari che non esistono per il cambio dell'ora si spostano in avanti;
- gli orari dei promemoria.

**Test obbligatori**

- Finestra a cavallo della mezzanotte.
- Ultima domenica di marzo e di ottobre.
- 29 febbraio.
- Fine anno.
- Quinto martedì del mese.
- «Ultimo venerdì».
- Stagioni a cavallo di capodanno.
- Ogni 2 settimane con data di riferimento nel passato e nel futuro.
- Pasqua: 05/04/2026, 28/03/2027, 16/04/2028.

## 9. Promemoria affidabili senza Play Services

- **Una sola catena di sveglie.** `rescheduleAll()` calcola i prossimi 45 giorni di promemoria di tutti i profili, più i posticipi e gli aggiornamenti del widget (00:05 e fine finestra), e imposta **una sola** sveglia sul primo evento.
  - Chiamata usata: `setExactAndAllowWhileIdle`.
  - Permessi: `USE_EXACT_ALARM` su API 33+; `SCHEDULE_EXACT_ALARM` con maxSdkVersion 32.
  - Sempre controllato `canScheduleExactAlarms()`; se manca il permesso, si ripiega su `setWindow` con 10 minuti di tolleranza.
  - Modalità «massima affidabilità» facoltativa con `setAlarmClock`.
- **Alla sveglia.** Il receiver (`goAsync`) pubblica tutti gli avvisi dovuti non ancora registrati, scrive nel DeliveryLog, aggiorna il widget e imposta la sveglia successiva.
- **Quando si ricalcola:** avvio del telefono, aggiornamento dell'app, cambio di ora o di fuso, cambio del permesso per le sveglie esatte, ogni apertura dell'app (copre anche l'arresto forzato su Android 15), ogni modifica dei dati, ogni sveglia.
- **Controllo di sicurezza.** Un'attività periodica WorkManager ogni 12 ore ricalcola tutto e cerca i promemoria mancati. Se la finestra di esposizione è ancora aperta, invia subito «Promemoria in ritardo: stasera Plastica, esponi entro le 05:00». In ogni caso l'evento finisce nel registro e compare in Affidabilità. La stessa attività esegue anche il backup settimanale e il controllo annuale.
- **Canali di notifica:**
  - Esposizione mastelli (alta importanza);
  - Ritiro mastelli;
  - Festività e calendario;
  - Servizio (bassa importanza).
- **Azioni sulla notifica:** «Fatto» e «Tra 30 min». Toccare un'azione conta come utilizzo dell'app, quindi evita l'ibernazione e le restrizioni di standby, come fa il widget.
- **Onboarding sull'affidabilità:**
  - permesso notifiche chiesto al momento giusto;
  - richiesta di esenzione dall'ottimizzazione batteria (lecita perché l'app non è sul Play Store);
  - disattivazione di «sospendi attività se inutilizzata»;
  - guide per marca (Samsung «App mai in sospensione», Xiaomi Autostart e «Nessuna restrizione»);
  - notifica di prova.

**Esempi di testo**

- Esponi: «Stasera: Plastica e metalli + Vetro». Testo: «Esponi dalle 22:00, ritiro entro le 05:00 di mercoledì 24».
- Prepara: «Prepara per stasera: Carta». Testo: «Esposizione dalle 22:00».
- Ritira: «Ritira i mastelli: Plastica e metalli, Vetro».
- Festivo (3 giorni prima): «Sabato 1 novembre è festivo: il ritiro dell'organico si fa?». Pulsanti: «Si fa» · «Salta» · «Apri».
- Mattina stessa: «Domattina entro le 06:00: Organico».

## 10. Condivisione e backup

**Condivisione tramite QR**

- Contiene solo il profilo: mastelli, regole, eccezioni, finestra, note. Non contiene gli orari personali.
- Correzione d'errore M.
- Oltre circa 1.800 caratteri l'app propone di inviare un file.
- Generazione e lettura con ZXing core (Java puro, Apache-2.0, nessuna libreria nativa) e CameraX ImageAnalysis.
- Lettura possibile anche da uno screenshot scelto con il Photo Picker.

**Condivisione tramite file**

- File `.json` inviato dal menu Condividi.
- L'app riceve `ACTION_SEND` (text/plain, application/json, application/octet-stream) e `ACTION_VIEW`, e riconosce il contenuto analizzandolo.
- Si può anche incollare il codice copiato da una chat.

**Importazione**

- Mostra sempre l'anteprima di 4 settimane e la data di esportazione.
- Crea un nuovo profilo oppure sostituisce quello scelto.
- Non unisce mai due calendari.

**Backup**

- JSON completo, esportato e ripristinato tramite SAF.
- Backup automatico settimanale in una cartella scelta dall'utente, che conserva gli ultimi 4 file.
- Auto Backup di Android configurato con `dataExtractionRules` che includono il database.

## 11. Permessi

| Permesso | Condizioni e motivo |
|---|---|
| `POST_NOTIFICATIONS` | Chiesto al momento giusto, durante la configurazione. |
| `USE_EXACT_ALARM` | Da API 33 in su. |
| `SCHEDULE_EXACT_ALARM` | Solo fino ad API 32 (maxSdkVersion 32). |
| `RECEIVE_BOOT_COMPLETED` | Per ricalcolare le sveglie all'avvio del telefono. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Solo dalla schermata Affidabilità, dopo una spiegazione. |
| `CAMERA` | Solo per scansionare il QR, chiesto al momento, con `required=false`. |
| `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` | Aggiunti in automatico dalla libreria WorkManager. |

**Nessun permesso INTERNET**, nessuna posizione, contatti o accesso all'archivio: i file passano da SAF e Photo Picker.

## 12. Stack tecnico

- **Linguaggio e librerie:** Kotlin 2.x, Compose BOM, Material 3, Navigation Compose, Room (KSP), DataStore, WorkManager, Glance 1.2.0, CameraX, ZXing core, kotlinx.serialization. java.time è nativo con minSdk 26.
- **Moduli:** `:app` e `:core:rules` (JVM puro, testabile). Dipendenze collegate a mano, senza Hilt.
- **APK:** senza librerie native, quindi senza problemi di allineamento a 16 KB. Dimensione stimata 6–9 MB.
- **Target 36:** edge-to-edge obbligatorio e back predittivo, gestiti con Scaffold e BackHandler.
- **Toolchain:** JVM fissata a 17 o 21.
- **Firma:** keystore di release salvato anche fuori dal PC dal primo giorno.

## 13. Non-obiettivi

- Nessuna banca dati dei calendari dei comuni.
- Nessuno scraping delle app dei gestori, nessuna lettura automatica di PDF o OCR nella v1.
- Nessun server, account o sincronizzazione cloud.
- Nessuna pubblicità, statistica o SDK di crash.
- Nessuno spostamento automatico dei ritiri nei festivi.
- Nessuna IA per riconoscere i rifiuti, nessuna mappa, nessuna prenotazione degli ingombranti, nessuna segnalazione, nessuna TARI.
- Nessuna consulenza su multe e regolamenti.
- Solo italiano, solo Android.
- Import ICS e unione di calendari rimandati.

## 14. Rischi e mitigazioni

| Rischio | Mitigazione |
|---|---|
| Calendario copiato male | Domanda «sera o giorno di ritiro?», anteprime continue, riassunto leggibile di ogni regola, avviso «verifica il calendario ufficiale». |
| Promemoria bloccati dal produttore, dall'ibernazione o da un arresto forzato | Catena unica ricalcolata da ogni punto di ingresso, controllo ogni 12 h, esenzione batteria, guide per marca, widget e pulsante «Fatto», registro dei mancati con recupero tardivo. Test su un Samsung e uno Xiaomi reali. |
| Notifiche negate | Banner rosso con correzione in un tocco. Il widget mostra comunque i mastelli di stasera. |
| Festivi e scioperi imprevedibili | Domanda 3 giorni prima; salta, sposta o aggiungi con un tocco. |
| Calendario cambiato a gennaio | Periodo di validità delle regole e controllo annuale dal 1° dicembre. |
| QR sbagliato che si diffonde nel condominio | Anteprima obbligatoria, data di esportazione, dicitura «importato il…». |
| Configurazione troppo lunga | Preset, chip dei giorni come strada veloce, regole avanzate nascoste, importazione dal vicino in primo piano. Obiettivo: 5 minuti o meno. |
| Concorrenza gratuita | Puntare su ciò che manca agli altri: orario a scelta, tre promemoria, regole stagionali e mensili, festivi, widget, più case, QR tra vicini, niente Internet. |
| Errori di ora legale e mezzanotte | ZonedDateTime in Europe/Rome e test mirati. |
| Perdita dei dati o della chiave di firma | Backup manuale e automatico, Auto Backup, keystore salvato altrove. |
| Verifica sviluppatori Android dal 2027 | Installazione via adb (esente); account gratuito a distribuzione limitata (fino a 20 dispositivi) prima del 2027. |
| Allargamento del progetto | Funzioni M congelate; il resto va in S, C o nella v1.1. |

## 15. Piano di lavoro (circa 2–3 settimane per una persona)

1. **Giorni 1–4:** modulo `:core:rules`, festività, test unitari.
2. **Giorni 5–7:** Room, catena di sveglie, receiver, WorkManager, DeliveryLog, test su emulatore API 36 (`dumpsys alarm`, `deviceidle force-idle`, `am set-standby-bucket`).
3. **Giorni 8–11:** configurazione guidata, Home, Calendario, Dettaglio giorno, editor dei mastelli e delle regole.
4. **Giorni 12–13:** schermata Affidabilità e guide per marca; widget Glance.
5. **Giorni 14–15:** backup JSON; QR e file (S); profili multipli (S).
6. **Giorni 16–18:** rifinitura, TalkBack, tema scuro, prove su Samsung e Xiaomi reali per 7 sere di fila.

## 16. Criteri di accettazione

- Configurare un calendario tipico (5 mastelli, 1 regola stagionale) richiede 5 minuti o meno.
- Per 7 sere consecutive, su Samsung e Xiaomi con le impostazioni suggerite, l'avviso «Esponi» arriva entro 2 minuti dall'orario scelto.
- Dopo il riavvio del telefono o l'arresto forzato e la riapertura dell'app, la sveglia successiva è impostata.
- Un promemoria saltato viene segnalato e, se la finestra è ancora aperta, recuperato.
- Il QR di un profilo con 7 mastelli e 20 eccezioni si legge in meno di 3 secondi. L'anteprima dell'importazione è identica all'originale.
- Tutti i test del modulo regole passano, compresi quelli sul cambio dell'ora.
- Il manifest non contiene il permesso INTERNET.

## 17. Dopo la v1 (v1.1)

- Import ICS (lib-recur) per i gestori che pubblicano calendari Google.
- Glossario condivisibile.
- Esportazione .ics.
- Riquadro nelle Impostazioni rapide.
- Eventuale pubblicazione su F-Droid.

## Fonti principali

- Calendario PDF di Forio (2025)
- Calendari di zona di Cuneo
- Castedduonline, sanzioni di Quartu (2022)
- La Voce di Imperia (8/9/2026)
- F-Droid, ricerca «rifiuti»
- Android Developers: sveglie esatte su Android 14, ibernazione delle app, bucket di standby, permesso notifiche, Glance 1.2.0, Auto Backup
- dontkillmyapp.com (Samsung, Xiaomi)
- Legge 151/2025 (4 ottobre festa nazionale)