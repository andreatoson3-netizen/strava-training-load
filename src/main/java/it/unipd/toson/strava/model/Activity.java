//rappresenta il modello dati di una singola attività sportiva restituita dall'API di Strava.
//fa da struttur dati principale per il trasferimento delle informazioni tra i vari layer dell'applicazione
//mappa i campi ricevuti in formato JSON tramite Jackson(gestendo tipi di dato per evitare ambiguità
// e sfruttando ZonedDataTime per il tempo) e incapsula la logica di validazione e filtraggio di base per riconoscere
// le sessioni di corsa idonee all'analisi di carico


package it.unipd.toson.strava.model;

//importa annotazione @JsonIgnoreProperty per istruire Jackson a ignorare i campi JSON non presenti nella classe
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//importa annotazioni @JsonProperty per collegare i nomi dei campi JSON ai metodi setter Java
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;//importa la classe per gestire data e ora completa con fuso orario


/** Rappresenta una singola attivita' restituita dall'API di Strava. */
@JsonIgnoreProperties(ignoreUnknown = true)//configura Jackson affinchè ignori eventuali campi sconosciuti  dell'API di Strava

public class Activity
{

    private long id;//memorizza l'identificativo numerico dell'attività
    private String name;//memorizza il nome dell'attività assegnato dall'atleta
    private String type;//memorizza la tipologia di sport secondo la classificazione ufficiale di Strava
    private ZonedDateTime startDate;//memorizza l'istanza temporale completa di fuso orario in cui è iniziata l'attività
    private Double distance;//memorizza la distanza percorsa (distingue zero e valore nullo)
    private Integer movingTime;//memorizza la durata effettiva in movimento espressa in secondi
    private Double totalElevationGain;//memorizza il dislivello positivo cumulato complessivo in metri
    private Boolean hasHeartrate;//indica se attività dispone di dati relativi alla frequenza cardiaca
    private Double averageHeartrate;//memorizza la frequenza cardiaca media registrata durante l'allenamento
    private Double maxHeartrate; // memorizza il picco massimo di frequenza cardiaca raggiunto

    private Integer sufferScore;//memorizza lo sforzo relativo calcolato direttamente dai server di Strava
    private Double averageSpeed;//memorizza la velocità media in m/s
    private Double weightedAverageWatts;//memorizza la potenza media in watt

    public long getId() { return id; }//metodo getter per leggere l'identificativo dell'attività
    @JsonProperty("id") public void setId(long v) { id = v; }//metodo setter associato alla proprietà JSON "id" per la deserializzazione

    public String getName() { return name; }//metodo getter per leggere il nome dell'attività assegnato dall'atleta
    @JsonProperty("name") public void setName(String v) { name = v; }//metodo setter associato alla proprietà JSON "name" per la deserializzazione

    public String getType() { return type; }//metodo getter per leggere la tipologia di sport
    @JsonProperty("type") public void setType(String v) { type = v; }//metodo setter associato alla proprietà JSON "type" per la deserializzazione

    public ZonedDateTime getStartDate() { return startDate; }//metodo getter per leggere l'istanza temporale in cui è iniziata l'attività
    //metodo setter associato alla proprietà JSON "start_date" per deserializzazione
    @JsonProperty("start_date") public void setStartDate(ZonedDateTime v) { startDate = v; }

    public Double getDistance() { return distance; }//metodo getter per leggere la distanza percorsa
    //metodo setter associato alla proprietà JSON "distance per deserializzazione
    @JsonProperty("distance") public void setDistance(Double v) { distance = v; }

    public Integer getMovingTime() { return movingTime; }//metodo getter per leggere la durata del tempo in movimento
    //metodo setter associato alla proprietà JSON "moving_time" per deserializzazione
    @JsonProperty("moving_time") public void setMovingTime(Integer v) { movingTime = v; }

    public Double getTotalElevationGain() { return totalElevationGain; }//metodo getter per legegr il dislivello positivo complessivo
    //metodo setter associato alla proprietà "total_elevation_gain" per deserializzazione
    @JsonProperty("total_elevation_gain") public void setTotalElevationGain(Double v) { totalElevationGain = v; }

    public Boolean getHasHeartrate() { return hasHeartrate; }//metodo getter per verificare la presenza della frequenza cardiaca
    //metodo setter associato alla proprietà "has_heartrate" per deserializzazione
    @JsonProperty("has_heartrate") public void setHasHeartrate(Boolean v) { hasHeartrate = v; }

    public Double getAverageHeartrate() { return averageHeartrate; }//metodo getter per leggere FC media
    //metodo setter associato alla proprietà JSON "average_heartrate" per deserializzazione
    @JsonProperty("average_heartrate") public void setAverageHeartrate(Double v) { averageHeartrate = v; }

    public Double getMaxHeartrate() { return maxHeartrate; }//metodo getter per leggere il picco massimo di FC
    //metodo setter associato alla proprietà JSON "max_heartrate" per deserializzazione
    @JsonProperty("max_heartrate") public void setMaxHeartrate(Double v) { maxHeartrate = v; }

    public Integer getSufferScore() { return sufferScore; }//metodo getter per leggere il punteggio di sforzo relativo
    //metodo setter associato alla proprietà JSON "suffer_score" per deserializzazione
    @JsonProperty("suffer_score") public void setSufferScore(Integer v) { sufferScore = v; }

    public Double getAverageSpeed() { return averageSpeed; }//metodo getter per leggere velocità media
    //metodo setter associato alla proprietà JSON "average_speed" per deserializzazione
    @JsonProperty("average_speed") public void setAverageSpeed(Double v) { averageSpeed = v; }

    public Double getWeightedAverageWatts() { return weightedAverageWatts; }//metodo getter per leggere la potenza media in watt
    //metodo setter associato alla proprietà JSON "weighted_average_watts" per deserializzazione
    @JsonProperty("weighted_average_watts") public void setWeightedAverageWatts(Double v) { weightedAverageWatts = v; }

    //metodo per verificare se l'attività è di corsa
    public boolean isRun()
    {
        return "Run".equals(type);//confronta la stringa costante con il tipo dell'attività per evitare eccezioni se type è nullo
    }

    //metodo per validare la presenza e la coerenza dei dati cardiaci
    public boolean hasHeartRate()
    {
        //verifica che il booleano sia true in modo sicuro e che il valore medio della FC sia non nullo e che il valore sia dentro i filtri di soglia minima e massima
        return Boolean.TRUE.equals(hasHeartrate) && averageHeartrate != null && averageHeartrate >= 30 && averageHeartrate <= 250;

    }

    //metodo per stabilire se l'attività possiede i requisiti minimi per essere inclusa nei calcoli

    public boolean isUsableRun()
    {
        //controlla che l'attività sia una corsa che la data di inizio sia definita che la distanza sia >0 e che la durata in movimento sia >0
        return isRun() && startDate != null && distance != null && distance > 0 && movingTime != null && movingTime > 0;

    }
}
