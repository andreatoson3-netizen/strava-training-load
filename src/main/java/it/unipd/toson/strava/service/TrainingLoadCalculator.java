//gestisce la logica di calcolo e di aggregazione temporale dei dati di allenamento operando come una funzione
//raggruppa le attività valide per mese sfruttando TreeMap per preservare l'ordine cronologico e impiega il pattern
//Accumulator per sommare i volumi in modo progressivo
//calcola metriche avanzate come le medie ponderate per durata(per frequenza cardiaca e potenza) e i totali mensili,
// gestendo i campi opzionali con valori nulli





package it.unipd.toson.strava.service;

import it.unipd.toson.strava.model.Activity;//importa il modello dati dell'attività sportiva
import java.time.YearMonth;//importa la classe per rappresentare i mesi
import java.util.ArrayList;//importa la classe ArrayList per la gestione di collezioni dinamiche
import java.util.List;//importa l'interfaccia List per le liste di oggetti
import java.util.Map;//importa l'interfaccia Map per strutture dati a chiave-valore
import java.util.TreeMap;//importa la classe TreeMap per mappe ordinate in automatico(i mesi escono in ordine cronologico)


public final class TrainingLoadCalculator
{

    //costruttore privato per non creare istanze e quindi si usano solo metodi statici
    private TrainingLoadCalculator() { }

    //metodo per calcolare gli aggregati mensili a partire dalla lista delle attività. Un mese senza attività non compare
    public static List<MonthlySummary> monthly(List<Activity> activities)
    {
        //inizializza una mappa ordinata per associare a ogni mese il rispettivo accumulatore di dati
        Map<YearMonth, Accumulator> byMonth = new TreeMap<>();

        //itera su ogni attività presente nel dataset
        for (Activity a : activities)
        {
            //verifica se l'attività non è una corsa valida
            if (!a.isUsableRun())
            {
                continue;//se l'attività non è usabile allora non entrano in accumulo

            }
            //estrae il mese e l'anno direttamente dalla data di inizio dell'attività
            YearMonth month = YearMonth.from(a.getStartDate());
            //recupera l'accumulatore per il mese corrente(creandolo se non esiste) e aggiunge i dati dell'attività
            //quindi computeIfAbsent() crea l'accumulatore la prima volta che un mese compare e poi add() lo aggiorna per le volte dopo
            byMonth.computeIfAbsent(month, ignored -> new Accumulator()).add(a);
        }
        //inizializza una lista in cui raccogliere i riepiloghi mensili completi
        List<MonthlySummary> result = new ArrayList<>();
        //converte ciascun accumulatore completato nel record MonthlySummary corrispondente(la conversione avviene solo alla fine) e lo aggiunge alla lista
        byMonth.forEach((month, acc) -> result.add(acc.toSummary(month)));
        return result;//restituisce la lista completi dei riepiloghi mensili ordinati cronologicamente
    }

    //classe interna per l'accumulo e la somma progressiva dei dati mensili
    private static final class Accumulator
    {
        //contatori per il numero totale di attività, quelle con frequenza cardiaca e quelle con potenza
        int count, hrCount, powerCount;

        //variabili per metri percorsi, secondi in movimento e metri di dislivello
        double distanceM, durationS, elevationM;

        //somme per la media ponderata della frequenza cardiaca(somma durate e somma FC per durata)
        //per la media ponderata: (sommaFC * durata)/somma durate. La divisione avviene in toSummary
        double hrDurationSum, hrWeightedSum;

        //somme per la media ponderata della potenza(somma durate e somma potenza per durata)
        double powerDurationSum, powerWeightedSum;

        int effortSum;//variabile per sommare i punteggi di sforzo relativo
        boolean hasEffort;//booleano per verificare se è stato registrato almeno un valore di sforzo nel mese

        //metodo senza specificatore perchè può essere usato solo da TrainingLoadCalculator
        //metodo per accumulare dati di una solo attività all'interno del mese
        void add(Activity a)
        {
            count++;//incrementa il contatore delle attività del mese
            double seconds = a.getMovingTime();//salva la durata in movimento dell'attività convertita in double
            distanceM += a.getDistance();//aggiunge la distanza dell'attività al totale parziale dei metri
            durationS += seconds;//aggiunge la durata dell'attività al totale parziale dei secondi
            //aggiunge il dislivello positivo sommando zero se il dato è assente
            elevationM += a.getTotalElevationGain() == null ? 0 : a.getTotalElevationGain();

            //verifica se l'attività possiede dati cardiaci validi e usabili
            if (a.hasHeartRate())
            {
                hrCount++;//incrementa il contatore delle attività con frequenza cardiaca
                hrDurationSum += seconds;//aggiunge la durata al denominatore della media ponderata cardiaca
                hrWeightedSum += a.getAverageHeartrate() * seconds;//aggiunge il prodotto tra frequenza media e durata al numeratore ponderato
            }
            //controlla se l'attività contiene un punteggio di sforzo relativo valido
            if (a.getSufferScore() != null)
            {
                hasEffort = true;//segnala che c'è almeno un valore di sforzo nel mese
                effortSum += a.getSufferScore();//aggiunge il punteggio di sforzo alla somma accumulata
            }
            //verifica se l'attività possiede dati di potenza validi e maggiori di zero
            if (a.getWeightedAverageWatts() != null && a.getWeightedAverageWatts() > 0) {
                powerCount++;//incrementa il contatore delle attività con misuratore di potenza
                powerDurationSum += seconds;//aggiunge la durata al denominatore della media ponderata di potenza
                powerWeightedSum += a.getWeightedAverageWatts() * seconds;//aggiunge il prodotto tra potenza media e durata al numeratore ponderato della potenza
            }
        }

        //metodo per finalizzare l'accumulo per trasformarlo nel record di riepiloghi mensile immutabile
        MonthlySummary toSummary(YearMonth month)
        {
            double km = distanceM / 1000.0;//converte la distanza totale accumulata da metri a chilometri

            //calcola la frequenza cardiaca media ponderata per durata, oppure restituisce null se non ci sono dati cardiaci
            //con media ponderata=somma(FC*secondi)/somma(secondi)
            Double hr = hrDurationSum > 0 ? hrWeightedSum / hrDurationSum : null;

            //calcola il passo medio mensile in minuti per chilometro basandosi sui totali, oppure restituisce null se la distanza è zero
            //con passo medio mensile=minuti totali/km totali
            Double pace = km > 0 ? durationS / 60.0 / km : null;

            //calcola la potenza media ponderata mensile, oppure restituisce null se non ci sono dati di potenza
            Double power = powerDurationSum > 0 ? powerWeightedSum / powerDurationSum : null;

            //crea e restituisce una nuova istanza del record MonthlySummary con tutti i valori calcolati e gestisce l'inclusione dello sfrozo totale condizionandola alla sua effettiva presenza nel mese
            return new MonthlySummary(month, count, km, durationS / 3600.0, elevationM,
                    hr, hasEffort ? effortSum : null, pace, power, hrCount, powerCount);
            // hasEffort ? effortSum : null: se nessuna corsa aveva sforzo relativo, il campo e' null (celle vuota nel CSV)
        }
    }
}
