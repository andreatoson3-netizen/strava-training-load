//controlla che un'attività sia considerata una corsa valida e utilizzabile(isUsableRun)
// anche in assenza di dati sulla frequenza cardiaca, purchè possieda i requisiti minimi(tipo, data, distanza,
// tempo in movimento. Verifica, poi, che il metodo hasHeartRate() restituisca true solo se il flag specifico è attivo
// e se il valore della frequenza cardiaca rientra in un intervallo plausibile(escludendo valori troppo anomali
//controlla che le attività di tipo diverso o le corse incomplete vengano correttamente scartate dalle corse usabili

package it.unipd.toson.strava.model;

import org.junit.jupiter.api.Test;//importa @Test di JUnit 5
import static org.junit.jupiter.api.Assertions.*;//importa tutte le asserzioni di JUnit per i controlli logici

class ActivityTest
{
    //Test 1: verifica che una corsa possa essere valida anche senza frequenza cardiaca
    @Test
    void runMayBeUsableWithoutHeartRate()
    {
        Activity a = new Activity();//istanzia un nuovo oggetto Activity vuoto
        a.setType("Run");//imposta il tipo di attività come corsa RUN
        a.setStartDate(java.time.ZonedDateTime.parse("2026-03-15T07:12:04Z"));//imposta la data e l'ora di inizio dell'attività
        a.setDistance(5000.0);//imposta la distanza percorsa in metri (5km)
        a.setMovingTime(1800);//imposta il tempo in movimento in secondi(30 minuti)
        a.setHasHeartrate(false);//specifica che non sono presenti dati di frequenza cardiaca
        assertTrue(a.isUsableRun());//verifica che l'attività sia considerata una corsa utilizzabile
        assertFalse(a.hasHeartRate());//verifica che il controllo sulla presenza del battito cardiaco valido sia esito negativo
    }

    //Test 2:verifica le condizioni di validità per la frequenza cardiaca
    @Test
    void heartRateRequiresFlagAndPlausibleAverage()
    {
        Activity a = new Activity();//istanzia un nuovo oggetto Activity
        a.setHasHeartrate(true);//attiva il flag che indica la presenza della FC
        a.setAverageHeartrate(150.0);//imposta una frequenza cardiaca media plausibile (150 bpm)
        assertTrue(a.hasHeartRate());//verifica che il battito sia valido ed esistente
        a.setAverageHeartrate(25.0);//imposta un valore anomalo e non frequenza per la FC(25 bpm)
        assertFalse(a.hasHeartRate());//verifica che il sistema rifiuti il valore considerandolo non valido nonostante il flag
        a.setAverageHeartrate(150.0);//reimposta un valore plausibile
        a.setHasHeartrate(false);//disattiva il flag
        assertFalse(a.hasHeartRate());//verifica che senza il flag il battito viene ignorato lon stesso
    }

    //Test 3:verifica che attività diverse dalla corsa oppure incomplete non siano usati
    @Test
    void nonRunAndIncompleteRunAreNotUsable()
    {
        Activity a = new Activity();//istanzia un nuovo oggetto Activity
        a.setType("Ride");//imposta l'attività come biciletta
        a.setStartDate(java.time.ZonedDateTime.parse("2026-03-15T07:12:04Z"));//imposta la data di inizio
        a.setDistance(5000.0);//imposta una distanza di 5000 metri
        a.setMovingTime(1800);//imposta il tempo in movimento
        assertFalse(a.isUsableRun());//verifica che un'attività che non sia corsa venga scartata
        a.setType("Run");//cambia il tipo in corsa
        a.setDistance(0.0);//imposta la distanza a zero(attività incompleta oppure non valida)
        assertFalse(a.isUsableRun());//verifica che una corsa con distanza nulla non sia considerata usabile
    }
}
