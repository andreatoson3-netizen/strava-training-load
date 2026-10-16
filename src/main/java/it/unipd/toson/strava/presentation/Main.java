//rappresenta il punto di ingresso dell'applicazione da riga di comando.Coordina il flusso operativo
//carica la configurazione locale, gestisce il processo di autentificazione OAuth(recuperando o rinnovando il token),
// attiva il client API per scaricare le attività di corsa, esegue il filtraggio temporale e la validazione,
// calcola gli indicatori di carico mensile e provvede infine all'esportazione dei dati in formato CSV,
// fornendo un riscontro a terminale.




package it.unipd.toson.strava.presentation;


import it.unipd.toson.strava.client.*;//importa la classe per migliorare leggibilità
import it.unipd.toson.strava.model.Activity;//importa il modello dati dell'attività sportiva
import it.unipd.toson.strava.service.ActivityAnalyzer;//importa il servizio di filtraggio e analisi delle attività
import it.unipd.toson.strava.service.MonthlySummary;//importa il modello per il riepilogo mensile aggregato
import it.unipd.toson.strava.service.TrainingLoadCalculator;//importa il motore di calcolo di allenamento
import java.nio.file.Path;//importa la classe per la gestione indipendente dei percorsi di file system
import java.time.LocalDate;//importa la classe per la gestione delle date senza fuso orario
import java.util.List;//importa l'interfaccia List per la gestione delle collezioni


public final class Main
{
    //costruttore privato per impedire la creazione di istanze
    private Main() { }


    public static void main(String[] args) throws Exception
    {
        //carica i parametri di configurazione leggendo il file locale predefinito
        AppConfig config = AppConfig.load(Path.of("config.properties"));
        // legge la configurazione locale. "config.properties" e' relativo alla cartella di lavoro, cosi' il
        // progetto resta portabile. Se il file manca, AppConfig produce un errore con le istruzioni per crearlo

        //esegue un controllo per verificare la presenza del client ID
        if (config.clientId().isBlank())
        {
            //lancia eccezioni se le credenziali risultano assenti
            throw new IllegalStateException("client_id non configurato");
        }

        //inizializza il gestore OAuth passando la configurazione di accesso
        OAuthManager oauth = new OAuthManager(config);
        //ottiene un token di accesso valido(riusandolo, rinnovandolo oppure avviando il login
        TokenPair token = oauth.validToken();
        //recupera la data di inizio dell'intervallo di analisi
        LocalDate start = config.startDate();
        //recupera la data di fine dell'intervallo di analisi
        LocalDate end = config.endDate();
        //stampa a terminale un messaggio di feedback per l'utente
        System.out.printf("Acquisizione attività di corsa dal %s al %s...%n", start, end);
        //avvia l'acqisizione e filtraggio delle attività
        List<Activity> activities = ActivityAnalyzer.inDateRange(
                //1)si crea il client HTTP con il token di accesso ricevuto
                new StravaApiClient(token.getAccessToken())
                //2) il client scarica le attività di corsa paginandole gestendo retry e restituisce oggetti deserializzati;
                .fetchRuns(start, end),
                //3)fornisce la data iniziale per il filtro di validazione locale
                start,
                //4)fornisce la data finale per il filtro di validazione locale
                end);

        //elabora e calcola gli aggregati mensili(volume,FC ponderata,sforzo relativo,passo,potenza) di carico a partire dalle corse filtrate
        List<MonthlySummary> monthly = TrainingLoadCalculator.monthly(activities);
        //esporta i dati dettagliati e i riepiloghi mensili all'interno dei rispettivi file CSV(activities.csv per i dati grezzi filtrati e monthly_summary.csv)
        ReportPrinter.write(config.outputDirectory(), activities, monthly);
        //stampa in console il resoconto finale con il riepilogo dei risultati e la posizione dei file
        ReportPrinter.printSummary(activities, monthly, config.outputDirectory());
    }

}
