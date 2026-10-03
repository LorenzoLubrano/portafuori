# Portafuori: audit di sicurezza (24/09/2026, versione 1.0.2 → 1.0.3)

## Come è stato fatto

1. **Revisione del codice.** Cinque revisori automatici hanno esaminato ciascuno un fronte diverso:
   - componenti e intent;
   - dati in ingresso;
   - privacy;
   - build e dipendenze;
   - robustezza.

   Ogni problema segnalato è passato poi a un verificatore indipendente, che ha provato a smentirlo leggendo il codice e riproducendolo sull'emulatore. Dei 27 segnalati, dopo l'unione dei doppioni ne restavano 18: 17 sono stati confermati e 1 era la conferma che le dipendenze non hanno vulnerabilità note.
2. **Analisi automatica** con Android Lint (`gradlew lintRelease`).
3. **Test d'attacco** su un emulatore Android 16 separato, prima e dopo le correzioni:
   - codici e file malevoli inviati tramite il menu Condividi;
   - broadcast da un'app estranea;
   - URI `file://`.
4. **Dipendenze** controllate sul database OSV (osv.dev): nessuna vulnerabilità nota nelle versioni usate.

## Modello di minaccia

- Un calendario malevolo o rotto ricevuto da un «vicino»: QR, codice incollato in chat, file `.json`, invio da qualsiasi app.
- Un'altra app installata sullo stesso telefono.
- Un telefono perso o un backup su cloud. I dati sono poco sensibili: calendario, nome della casa, note.
- Dipendenze e configurazione della build.

## Risultati e correzioni

| ID | Gravità | Problema | Correzione (1.0.3) |
|---|---|---|---|
| S1 | **Alta** | Un codice di circa 3 KB, che si decomprime in un campo di testo da 2 MB, rendeva l'app **inutilizzabile per sempre**: crash a ogni avvio, promemoria fermi per tutte le case, e il problema si portava dietro anche nel backup. | Limiti su tutto ciò che si importa: 1 MB di input, 512 KB decompressi, lunghezza di ogni testo, numero di elementi. Gli stessi limiti valgono per ogni salvataggio e per i campi di testo. All'apertura il database accorcia le righe troppo grandi già salvate. |
| S2 | Media | Migliaia di date o eccezioni rallentavano l'app a ogni avvio fino al blocco (ANR), perché i calcoli crescevano col quadrato dei dati. | Calcoli lineari nel motore delle regole, limiti al numero di elementi e pianificazione fuori dal thread principale. |
| S5 | Media | Un codice ricevuto poteva spegnere di nascosto i tuoi avvisi e offrire «Sostituisci tutto». | Gli orari degli avvisi e «Sostituisci tutto» si applicano solo con il pulsante «Ripristina backup», mai per scelta del file. |
| S6 | Media | Valori impossibili (mese 13, «25:99», tipi sconosciuti) facevano crashare l'anteprima dell'importazione. | Validazione completa prima di mostrare l'anteprima, con un messaggio chiaro («Non riesco a importare: …»). |
| S3 | Bassa | L'importazione non era atomica: «Sostituisci tutto» cancellava prima di scrivere, e un doppio tocco duplicava le case. | Un'unica transazione (tutto o niente) e pulsanti disattivati durante l'importazione. |
| S4 | Bassa | Un errore nel motore dei promemoria poteva fermare la catena di sveglie e chiudere l'app. | La prossima sveglia viene sempre reimpostata (nuovo tentativo entro 30 minuti). Gli errori sono isolati e il file delle impostazioni, se corrotto, viene ricreato. |
| S7 | Bassa | Le note passavano ai vicini senza avviso. | La schermata di condivisione le mostra e le dichiara; anche l'anteprima dell'importazione le mostra. |
| S8 | Bassa | File e codici venivano letti senza limiti, sul thread principale, e accettando URI `file://`. | Solo URI `content://`, lettura limitata a 1 MB, fuori dal thread principale. |
| S9 | Bassa | Un'immagine grande (per esempio 8000×8000) scelta per leggere un QR esauriva la memoria. | L'immagine viene ridotta a 2048 px e letta fuori dal thread principale. |
| S10 | Bassa | Date assurde (anno 999999999) facevano crashare il selettore di data. | Date accettate solo tra il 2000 e il 2100; il selettore è limitato allo stesso intervallo. |
| S11 | Bassa | «Sostituisci il calendario» sovrascriveva il nome e le note della tua casa. | Cambiano solo bidoni e giorni; nome, note e avvisi restano i tuoi. |
| S12 | Bassa | Gli avvisi bloccati (notifiche spente o canale disattivato) risultavano «arrivati». | Vengono registrati solo quelli mostrati davvero, e quelli bloccati riappaiono quando le notifiche tornano attive. |
| S13 | Info | Il backup automatico su cloud avveniva anche senza cifratura end-to-end. | Solo backup cifrati, cioè con il blocco schermo attivo; il trasferimento tra telefoni continua a funzionare. |
| S14 | Info | Il ricevitore dei riavvii accettava intent da qualsiasi app (lo segnalava anche Lint). | Accetta solo i 5 eventi di sistema protetti. |
| S15 | Info | C'erano permessi inutili ereditati da WorkManager (`ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`). | Rimossi. |
| S16 | Info | La chiave di firma è in chiaro in un'unica cartella e l'impronta non era pubblicata. | Impronta pubblicata nel README. **Da fare a mano:** copia di sicurezza della chiave fuori dal PC. |
| S17 | Info | La distribuzione di Gradle non veniva verificata. | Hash SHA-256 fissato nel wrapper. |

## Verifica dopo le correzioni

| Attacco | 1.0.2 | 1.0.3 |
|---|---|---|
| Codice «bomba» (90 MB decompressi) | nessun crash, ma tutto decompresso in memoria | «Il codice è troppo grande» |
| Codice avvelenato con note da 2,2 MB | l'app diventa inutilizzabile per sempre | «Il codice è troppo grande» |
| Periodo dell'anno impossibile | crash | «Periodo dell'anno non valido» |
| Orario «25:99» | crash | «Orario non valido» |
| File passato con `file://` | letto | «Non riesco ad aprire il file» |
| Broadcast esplicito da un'app estranea | eseguito | ignorato |
| Falso backup da un vicino che spegne gli avvisi | avvisi spenti e «Sostituisci tutto» offerto | avvisi intatti, solo «Crea una nuova casa» |

Test automatici: 25 su 25 superati, di cui 8 nuovi di sicurezza in `app/src/test/.../share/ShareCodecSecurityTest.kt`.
Lint: nessun problema di sicurezza né errori.
Permessi finali dell'APK: notifiche, avvio, sveglie esatte, esenzione batteria, fotocamera (solo per il QR) e wake lock. **Nessun accesso a Internet.**

## Rischi residui accettati

- **Note condivise.** Un vicino può ancora mandarti note o un nome di casa fuorvianti, per esempio un numero di telefono diverso da quello vero. Ora però li vedi nell'anteprima prima di importare.
- **Android 8.x.** Ignora il limite dei backup cifrati, ma i dati sono poco sensibili.
- **Firma dell'APK.** La sicurezza degli aggiornamenti dipende dalla chiave in `%USERPROFILE%\.android-keys`: va copiata in un posto sicuro fuori dal PC.
