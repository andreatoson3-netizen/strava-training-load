
Prototipo Java 17 per acquisire le attivita' di corsa di un singolo atleta tramite Strava API v3,
filtrare i record utilizzabili e produrre file CSV con dati grezzi e aggregati mensili

------------------------------------------------------------------------------------------------------------------------

REQUISITI 

JDK 17 o successivo(il progetto è stato testato con JDK 17) e Maven 3 (oppure apertura del progetto in Intellij IDEA,
che gestisce Maven da sola)

------------------------------------------------------------------------------------------------------------------------

APERTURA DEL PROGETTO IN INTELLIJ IDEA

1)File > Open, selezionare la cartella che contiene pom.xml(non i singoli file) e confermare "Open as Project"
2)Attendere il download delle dipendenze Maven (barra di avanzamento in basso alla pagina. 
3)File>Project Structure>Project:impostare Project SDK su JDK 17(o successivo)
4)Settings>Build Tools>Maven>Importing: JDK for importer=17/18
5)Il codice si trova nella directory Maven "src/main/java"(e i rispettivi Unit Test in "src/test/java")

------------------------------------------------------------------------------------------------------------------------

COME E' STATA CREATA L'APPLICAZIONE SU STRAVA(CREDENZIALI)

1)Accedere a https://www.strava.com con il proprio account
2)Aprire https://www.strava.com/settings/api
3)Compilare il modulo "My API Application":
  -Application Name:Tesi Analisi Allenamento 
  -Category:Analisi delle prestazioni
  -Company:None
  -Website:si può indicare http://localhost
  -Authorization Callback Domain: "localhost"
4)Accettare i termini e cliccare Create
5)Copiare dalla pagina i due valori: "Client ID"(un numero) e "Client Secret"(una stringa che non deve essere condivisa)
  che dovranno essere inseriti in config.properties

NB: L'ACCESSO ALL'API STRAVA RICHIEDE UN ABBONAMENTO ATTIVO

------------------------------------------------------------------------------------------------------------------------

CONFIGURAZIONE E AVVIO DEL PROGRAMMA

1)Copiare "config.example.properties" e rinominare la copia nella radice del progetto
2)Compilare i campi:
  -"client_id":il Client ID copiato da Strava
  -"client_secret":il Client Secret copiato da Strava
  -"redirect_uri":lasciare http://localhost:8080/callback (coerente con il dominio localhost registrato su Strava; 
   il programma apre un server locale sulla porta)
3)Facoltativo:"strat_date" ed "end_date" in formato YYYY-MM-DD per fissare l'intervallo; altrimenti usato ultimo anno 
4)NON condividere "config.properties", "data/' nè "output/": sono già esclusi grazie a ".gitignore"

Viene poi effettuato l'avvio del programma: da Intellij cliccando la freccia verde sul metodo "main" della classe "Main"
(in src/main/java/it/unipd/toson/strava/presentation), altrimenti da terminale tramite mvn exec:java

Successivamente il programma genera nel terminale un URL di autorizzazione: dovrebbe aprire direttamente il browser,
altrimenti seguire le istruzioni, aprire il link, effettuare il login(inserire il codice una tantum inviato da Strava) 
e infine cliccare su "Autorizza" per autorizzare l'applicazione. 
Nel caso in cui dovesse essersi generato un errore, controllare il proprio terminale per risolverlo.
Se non dovesse essersi generato alcun tipo di errore, dovrebbe generarsi "Autorizzazione completata": da questo momneto 
è possibile chiudere la pagina web e tornare al proprio programma. 
Al terminale vengono creati:
-"output/activities.csv":una riga per ogni attività di corsa, con i campi astratti
-"output/monthly_summary.csv": una riga per ogni mese con gli aggregati 
-"output/report_attivita.txt":report leggibile per tutte le attività di corsa
-"output/report_mensile.txt":report leggibile per ogni mesre con gli aggregati

ERRORI CHE POTREBBERO VERIFICARSI NEL RUN DEL METODO MAIN

-"Redirect URI mismatch":verificare che su Strava il callback domain sia "localhost" e che nel file non ci siano spazi 
  o refusi
-Il browser non si apre:copiare l'URL stampato nel terminale e aprirlo manualmente

------------------------------------------------------------------------------------------------------------------------

TEST UNITARI 

"mvn test" oppure in Intellij, clic destro su "src/test/java">Run "All Tests". 
La suite di test copre modello, parsing JSON, filtri, aggregazioni, configurazione, token store ed esportazione CSV

------------------------------------------------------------------------------------------------------------------------

METRICHE E LIMITI INTERPRETATIVI 

Il report mensile include conteggio attività, distanza totale, durata, dislivello positivo, frequenza cardiaca media 
ponderata per durata, somma dei valori di sforzo relativo disponibili, passo medio derivato da distanza e durata, potenza
ponderata solo quando il campo è presente. 
I campi opzionali di frequenza cardiaca, sforzo relativo e potenza restano mancanti quando assenti e sono esportati 
come celle vuote.
Eccezione: il dislivello mancante viene trattato come zero nell'aggregazione mensile

Il prototipo non calcola il tempo nelle zone cardiache o di potenza: l'endpoint delle zone per singola attività è indicato
dalla documentazione Strava come funzionalità Summit. 
Il "suffer_score" e i watt possono mancare, in particolare per attività senza i dati corrispondenti. 
I risultati descrivono i dati registrati, non costituiscono una valutazione clinica nè dimostrano da soli un nesso causale
tra allenamenti e miglioramento. La soglia di validità della FC(30-250bpm) è un filtro tecnico modificabile

