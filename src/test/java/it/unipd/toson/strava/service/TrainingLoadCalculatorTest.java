//controlla che le metriche mensili(distanza, durata, frequenza cardiaca media ponderata, sforzo relativo,
// passo medio e potenza media) vengano calcolate correttamente a partire da più attività
//verifica che il sistema gestisce correttamente le corse prive di frequenza cardiaca, sforzo oppure potenza,
//lasciando a null le relative metriche aggregando e impostando a zero i contatori specifici. Controlla poi i valori
//di dislivello mancanti(null) vengano trattati in modo sicuro



package it.unipd.toson.strava.service;

import it.unipd.toson.strava.model.Activity;//importa il modello dati dell'attività
import org.junit.jupiter.api.Test;//importa l'annotazione @Test di JUnit 5
import java.time.ZonedDateTime;//importa la gestione di data e ora con fuso orario
import java.util.List;//importa l'interfaccia List
import static org.junit.jupiter.api.Assertions.*;//importa tutte le assezioni di JUnit

class TrainingLoadCalculatorTest
{
    //metodo per creare un'attività di corsa con parametri personalizzati
    private Activity run(String date, double km, int seconds, Double hr, Integer effort, Double watts)
    {
        Activity a = new Activity();//crea attività
        a.setType("Run");//imposta il tipo di attività come corsa
        a.setStartDate(ZonedDateTime.parse(date));//imposta la data di inizio
        a.setDistance(km * 1000.0);//converte i chilometri in metri ed imposta la stanza
        a.setMovingTime(seconds);//imposta il tempo in movimento in secondi
        a.setTotalElevationGain(10.0);//imposta un dislivello predefinito di 10 metri
        a.setHasHeartrate(hr != null);//imposta il flag del battito cardiaco in base alla presenza del valore
        a.setAverageHeartrate(hr);//imposta la frequenza cardiaca media
        a.setSufferScore(effort);//imposta lo score di sforzo/sofferenza
        a.setWeightedAverageWatts(watts);//imposta la potenza media ponderata
        return a;
    }

    @Test
    //Test:verifica il calcolo del totale mensili e delle medie ponderate per durata
    void computesMonthlyTotalsAndDurationWeightedMetrics()
    {
        //crea la prima corsa di prova (10km, 3000s, 150 bpm, sforzo 80,200W)
        Activity a = run("2026-03-01T08:00:00Z", 10, 3000, 150.0, 80, 200.0);
        //crea la seconda corsa di prova(5 km,1800s,160bpm,sforzo 50, 250W)
        Activity b = run("2026-03-10T08:00:00Z", 5, 1800, 160.0, 50, 250.0);

        var result = TrainingLoadCalculator.monthly(List.of(a, b));//calcola i riepiloghi mensili passando la lista delle attività
        assertEquals(1, result.size());//verifica che sia stato generato un unico riepilogo per il mese di marzo

        MonthlySummary m = result.get(0);//estrae il riepilogo mensile dalla lista dei risultati
        assertEquals(2, m.activityCount());//verifica che il conteggio delle attività sia 2
        assertEquals(15.0, m.distanceKm(), 1e-9);//verifica che la distanza totale sia 15km(con tolleranza per virgola mobile)
        assertEquals(4800.0 / 3600.0, m.durationHours(), 1e-9);//verifica che la durata totale in ore sia corretta(4800 secondi totali)
        //verifica il calcolo della frequenza cardiaca media ponderata sulla durata
        assertEquals((150.0 * 3000 + 160.0 * 1800) / 4800.0, m.averageHeartRateBpm(), 1e-9);
        assertEquals(130, m.relativeEffortTotal());//verifica la somma dello sforzo relativo(80+50)
        assertEquals(4800.0 / 60.0 / 15.0, m.averagePaceMinPerKm(), 1e-9);//verifica il calcolo del passo medio in minuti al chilometro
        //verifica il calcolo della potenza media ponderata sulla durata
        assertEquals((200.0 * 3000 + 250.0 * 1800) / 4800.0, m.averagePowerWatts(), 1e-9);
    }

    //Test 2: verifica la gestione di corse senza dati cardiaci o di potenza
    @Test
    void keepsRunsWithoutHeartRateAndLeavesOptionalAggregatesNullWhenAbsent()
    {
        //crea una corsa senza frequenza cardiaca, sforzo o potenza
        Activity a = run("2026-04-02T08:00:00Z", 8, 2400, null, null, null);

        var result = TrainingLoadCalculator.monthly(List.of(a));//calcola il riepilogo mensile passando la lista con la singola attività priva di dati opzionali
        assertEquals(1, result.size());//verifica che sia presente un solo riepilogo mensile
        MonthlySummary m = result.get(0);//estrae il riepilogo mensile dalla lista dei risultati
        assertEquals(1, m.activityCount());//verifica il conteggio delle attività
        assertEquals(8.0, m.distanceKm(), 1e-9);//verifica la distanza in km
        assertNull(m.averageHeartRateBpm());//verifica che la frequenza cardiaca media sia null
        assertNull(m.relativeEffortTotal());//verifica che lo sforzo relativo sia null
        assertNull(m.averagePowerWatts());//verifica che la potenza media sia null
        assertEquals(0, m.heartRateActivityCount());//verifica che il contatore delle attività con battito sia a zero
        assertEquals(0, m.powerActivityCount());//verifica che il contatore delle attività con potenza sia a zero
    }

    //Test 3:verifica che un dislivello mancante sia gestito come valore zero
    @Test
    void currentImplementationTreatsMissingElevationAsZero()
    {
        //crea un attività di corsa di prova senza dislivello esplicito nei parametri
        Activity a = run("2026-04-02T08:00:00Z", 8, 2400, null, null, null);

        a.setTotalElevationGain(null);//importa esplicitamente il dislivello totale a null per testare il comportamento limite

        var result = TrainingLoadCalculator.monthly(List.of(a));//calcola il riepilogo mensile passando la lista con l'attività avente dislivello nullo

        //verifica che il dislivello totale nel riepilogo venga convertito e trattato come 0.0 metri
        assertEquals(0.0, result.get(0).elevationGainM(), 1e-9);
    }
}
