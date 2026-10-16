//legge i file di configurazione(config.properties) e recupera i dati sensibili per connettersi alle API di Strava tramite OAuth 2.0
//(client_id e client_secret) senza scriverli nel codice. Se il file non esiste lancia eccezzione spiegando cosa fare
//Configura l'intervallo di tempo per scaricare e filtrare le attività di corsa dell'atleta e definisce dove salvare i file CSV finali




package it.unipd.toson.strava.client;

import java.io.IOException;//importa la classe  per gestire eccezioni/errori di I/O
import java.io.InputStream;//importa la classe per leggere flussi di dati di input
import java.nio.file.Files;//importa la classe per operare su file
import java.nio.file.Path;//importa la classe per gestire i percorsi dei file in modo sicuro
import java.time.LocalDate;//importa la classe per gestire le date senza fuso orario
import java.util.Properties;//importa la classe per leggere i file di configurazione in formato chiave-valore(.properties)


//configurazione locale dell'applicazione
public final class AppConfig {

    private final String clientId;//identificativo pubblico dell'applicazione registrata su Strava
    private final String clientSecret;//identificazione segreto dell'applicazione: non deve essere condiviso o messo nel codice
                                      //usato solo nel corpo di richieste HTTPS verso l'endpoint token.
    private final String redirectUri;//indirizzo a cui il browser viene rimandato dopo il consenso(deve corrispondere a quello registrato nell'app Strava
    private final Path tokenFile;//percorso del file dei token salvati(di default data/tokens.json)
    private final Path outputDirectory;//directory di destinazione dei report CSV( di default output)
    private final LocalDate startDate;//inizio dell'intervallo da analizzare(incluso)
    private final LocalDate endDate;//fine dell'intervallo da analizzare(incluso)

    //costruttore privato per evitare creazione di nuove istanze. Per ottenere AppConfig bisogna usare load()
    private AppConfig(Properties p)
    {
        clientId = required(p, "client_id");//required() fallisce se manca il valore
        clientSecret = required(p, "client_secret");//stesso ragionamento di clientId

        //callback di default: il server temporaneo del programma ascolta sulla stessa porta così il codice di autorizzazione resta locale
        // (porta 8080 e percorso /callback)

        redirectUri = p.getProperty("redirect_uri", "http://localhost:8080/callback").trim();//legge redirect_uri o usa il localhost di default
                                                                                                             //il metodo trim() serve per eliminare spazi inutili
        tokenFile = Path.of(p.getProperty("token_file", "data/tokens.json"));//imposta il file token e di default data/tokens.json
        outputDirectory = Path.of(p.getProperty("output_directory", "output"));//imposta la cartella di output

        //intervallo di default:ultimi 365 giorni fino ad oggi, così il programma è pronto all'uso senza parametri
        endDate = parseDate(p.getProperty("end_date"), LocalDate.now());//legge la data di fine intervallo oppure imposta quella di oggi se non specificata
        startDate = parseDate(p.getProperty("start_date"), endDate.minusDays(365));//legge la data di inizio intervallo oppure imposta 365 giorni
                                                                                                // prima della fine

        //verifica che la data di fine sia dopo quella di inizio
        if (!endDate.isAfter(startDate))
        {
            throw new IllegalArgumentException("end_date deve essere successiva a start_date");
        }
    }

    //metodo per caricare la config da un file Path. Fa un caricamento con fallimento rapido e messaggio d'aiuto se il file manca
    public static AppConfig load(Path file) throws IOException
    {
        //se il file manca, l'errore dice all'utente cosa fare
        if (!Files.isRegularFile(file))
        {
            throw new IOException("Manca " +file+". Copia config.example.properties in un nuovo file chiamato config.properties e inserisci le credenziali della tua app Strava.");
        }

        Properties p = new Properties();//istanza un oggetto Properties vuoto per contenere le coppie chiave-valore
        //apre uno stream in lettura sul file con try-with-resources: il flusso viene chiuso in automatico anche con lancio eccezione
        try (InputStream in = Files.newInputStream(file))
        {
            p.load(in);//carica ie proprietà dal file di input nello stream
        }
        return new AppConfig(p);//restituisce la nuova istanza di AppConfig con i dati caricati
    }

    //se un valore manca oppure è in placeholder(quindi INSERISCI...) è un errore e quindi viene inviato errore
    //il metodo è un metodo di validazione
    private static String required(Properties p, String key)
    {
        String v = p.getProperty(key, "").trim();//recupera le proprietà associate alla chiave e rimuove gli spazi con trim
        //controlla se il valore è vuoto oppure in placeholder
        if (v.isEmpty() || v.startsWith("INSERISCI_"))
        {
            throw new IllegalArgumentException("Valorizza " + key + " in config.properties");//blocca l'esecuzione
        }
        return v;//restituisce il valore valido della proprietà
    }

    //Se la chiave è assente oppure vuota si usa il fallback; altrimenti si converta la data (ano-mese-giorno).
    // Se si sbaglia il formato è lanciata l'eccezione DatatTimeParseException
    private static LocalDate parseDate(String s, LocalDate fallback)
    {
        return s == null || s.isBlank() ? fallback : LocalDate.parse(s.trim());
    }

    //vari metodi getter per accede in lettura ai campi di configurazione. Non ci sono i setter perchè la configurazione dell'app deve rimanere sempre la stessa
    public String clientId() { return clientId; }
    public String clientSecret() { return clientSecret; } //serve per OAuthManager
    public String redirectUri() { return redirectUri; }
    public Path tokenFile() { return tokenFile; }
    public Path outputDirectory() { return outputDirectory; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }

}
