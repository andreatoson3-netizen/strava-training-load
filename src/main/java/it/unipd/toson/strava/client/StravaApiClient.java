//rappresenta il client HTTP dedicato con l'endpoint delle attività delle API di Strava ( /athlete/activities),
//si occupa di assemblare gli URL di richiesta, gestire in modo autonomo la paginazione progressiva dei risultati
// e convertire le risposte JSON in oggetti Java pronti grazie a JacksonMapper.
// gestisce errori di rete e di diversi tentativi(retry ed eventuale retry-after),
// così da gestire problemi temporanei dei server o limitazioni di traffico senza interrompere il programma



package it.unipd.toson.strava.client;


import it.unipd.toson.strava.model.Activity; //importa il modello dati che rappresenta una singola attività sportiva
import java.io.IOException; //importa l'eccezione standard per la gestione degli errori di I/O e rete
import java.net.URI; //importa la classe per la gestione degli indirizzi URI
import java.net.http.HttpClient; //importa il client HTTP moderno per effettuare chiamate di rete
import java.net.http.HttpRequest;//importa la classe per costruire richieste HTTP verso le API
import java.net.http.HttpResponse;//importa la classe per gestire la risposta ricevuta dalle API
import java.time.Duration;//importa la classe per definire intervalli di tempo come i timeout
import java.time.LocalDate;//importa la classe per gestire le date senza informazioni sull'ora
import java.time.ZoneOffset;//importa la classe per gesture i fusi orari
import java.util.ArrayList;//importa l'implementazione di una lista dinamica in memoria
import java.util.List;//importa l'interfaccia List per collezioni di elementi


public final class StravaApiClient
{
    //costante statica che memorizza l'indirizzo di base delle API v3 di Strava
    private static final String API = "https://www.strava.com/api/v3";
    //costante che definisce il numero massimo di attività da richiedere per ogni pagina, cioè 200
    private static final int PAGE_SIZE = 200;
    //inizia la creazione di un'istanza riutilizzabile del client HTTP con configurazione di un timeout di connessione di 20 s
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    private final String accessToken;//memorizza il token di accesso OAuth dell'utente

    //costruttore che riceve il token di accesso come parametro
    public StravaApiClient(String accessToken)
    {
        this.accessToken = accessToken;//assegna il token ricevuto al campo interno della classe
    }

    //metodo che scarica le attività di corsa filtrate per intervallo di date
    public List<Activity> fetchRuns(LocalDate start, LocalDate end) throws IOException, InterruptedException
    {
        // IOException: errori di rete/protocollo. InterruptedException: il
        // Thread.sleep del retry puo' essere interrotto; propagarlo e' corretto.

        //converte la data di inizio alle ore 00:00 in formato timestamp Epoch secondi(UTC)
        long after = start.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        //converte la data successiva alla fine(per inclusione) in timestamp Epoch secondi(UTC)
        long before = end.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);

        //inizializza una lista vuota in cui raccogliere tutte le attività di corse valide trovate
        List<Activity> runs = new ArrayList<>();

        //inizia un ciclo infinito per gestire la paginazione delle API partendo dalla pagina 1
        // (l'uscita dal ciclo avviene per break quando la pagina è vuota oppure incompleta)
        for (int page = 1; ; page++)
        {
            //costruisce l'URL completo dell'endpoint inserendo il timestamp di inizio (after)
            String url = API + "/athlete/activities?after=" + after
                + "&before=" + before      //aggiunge all'URL il timestamp limite superiore (before)
                + "&page=" + page          //aggiunge il numero della pagina corrente da scaricare
                + "&per_page=" + PAGE_SIZE;//specifica quanti elementi richiedere per singola pagina

            //invia la richiesta HTTP gestendo eventuali tentativi ripetuti in caso di errore
            HttpResponse<String> response = sendWithRetry(url);

            List<Activity> batch = JacksonMapper.activities(response.body());//converte la risposta JSON string delle attività
                                                                             // in una lista di oggetti Java Activity

            //controlla se il blocco di attività restituito è vuoto, se lo è interrompe il ciclo con break
            if (batch.isEmpty())
            {
                break;
            }
            //itera su ogni attività ricevuta nel blocco corrente
            for (Activity a : batch)
            {
                //verifica se l'attività corrente è una corsa valida e utilizzabile
                if (a.isUsableRun())
                {
                    runs.add(a);//se è una corsa valida, la aggiunge alla lista finale dei risultati
                }
            }
            //controlla se il numero di elementi ricevuti è inferiore alla dimensione massima della pagina
            if (batch.size() < PAGE_SIZE)
            {
                break;//se lo è allora si è arrivati all'ultima pagina disponibile e il ciclo viene interrotto
            }
        }
        return runs;//restituisce la lista completa delle corse filtrate trovate nell'intervallo
    }

    //metodo per inviare richieste GET gestendo logiche di ripetizione in caso di errore e classifica le risposte distinguendo in errori permanenti e temporanei
    private HttpResponse<String> sendWithRetry(String url) throws IOException, InterruptedException {
        //ciclo di 5 tentativi massimo
        for (int attempt = 0; attempt < 5; attempt++)
        {   //inizia la costruzione di una richiesta HTTP GET per l'URL specificato
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(45))     //imposta un timeout di attesa della risposta a 45 secondi(se risposta non arriva entro 45 s è un fallimento)
                    .header("Authorization", "Bearer " + accessToken) //inserisce l'intestazione di autorizzazione OAuth con il token Bearer
                    .header("Accept", "application/json") //specifica che si desidera una risposta in formato JSON
                    .header("User-Agent", "UNIPD-Strava-Training-Load-Prototype/1.0")//imposta un'intestazione User-Agent identificativa del client
                    .GET().build(); //configura il metodo come GET e finalizza la costruzione della richiesta

            HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString()); //invia la richiesta HTTP e attende la risposta testuale

            int status = response.statusCode();//estrae il codice di stato numerico HTTP dalla risposta
            //se la richiesta è andata a buon fine(HTTP 200 ok) restituisce la risposta
            if (status == 200)
            {
                return response;
            }
            //se si è verificato un errore di autentificazione(HTTP 401 Unauthorized) lancia un'eccezione che blocca e spiega che il token è scaduto
            if (status == 401)
            {
              throw new IOException("HTTP 401: token non valido o scaduto. Riavvia il programma;"+ "se persiste,rimuovi il file token locale e autorizza nuovamente.");
            }
            //se l'accesso è negato(HTTP 403 Forbidden) lancia un'eccezione per permessi insufficenti
            if (status == 403)
            {
                throw new IOException("HTTP 403: accesso negato. Controlla scope OAuth e permessi dell'attivita'.");
            }
            //se si è superato il  limite di richieste(429) o di un errore del server(5xx)
            if (status == 429 || status >= 500)
            {   //controlla se questo era l'ultimo tentativo disponibile
                if (attempt == 4)
                {
                    //lancia eccezione se i tentativi sono esauriti
                    throw new IOException("API Strava non disponibile dopo 5 tentativi (HTTP "+ status + ").");

                }
                long wait = retryDelay(response, attempt);//calcola i secondi di attesa per un nuovo tentativo
                System.err.println("Risposta HTTP " + status + ". Nuovo tentativo tra " + wait + " s...");//stampa un avviso di errore
                Thread.sleep(wait * 1000L);//sospende il thread per il numero di secondi calcolato convertito in millisecondi
                continue;//passa al tentativo dopo del ciclo(stesso URL)
            }
            // Viene lanciata ecccezzione per qualsiasi altro tipo di errore non gestito(con 4xx)
            throw new IOException("Richiesta API fallita (HTTP " + status + "): " + response.body());
        }
        //lancia eccezzione se finisce il ciclo senza avere successo
        throw new IOException("Richiesta API interrotta dopo i tentativi disponibili.");

    }

    //metodo per calcolare il tempo di attesa prima di un nuovo tentativo
    //prima verifica l'intestazione Retry-After fornita dal server di Strava, poi applica
    //un backoff esponenziale con massimo 30 secondi
    private static long retryDelay(HttpResponse<?> r, int attempt)
    {
        //tenta di leggere l'intestazione Retry-After fornita dal server Strava
        String retryAfter=r.headers().firstValue("Retry-After").orElse("");
        try
        {
            //se presente rispetta l'indicazione di Strava limitandola tra un minimo di 1 e un massimo di 60
            return Math.min(60, Math.max(1, Long.parseLong(retryAfter)));
        }
        catch(NumberFormatException ignored)
        {
            //se l'header non c'è o non ha numero valido, applica il backoff,quindi attesa raddoppia(2^1=2s , 2^2=4s ....) ad ogni tentativo fallito con massimo 30 s
            return Math.min(30, 2L<<attempt);
        }

    }

}
