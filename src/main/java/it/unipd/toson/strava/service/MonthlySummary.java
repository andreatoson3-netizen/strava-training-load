//rappresenta la struttura dati immutabile(implementata tramite un record) destinata a raccogliere
//gli indicatori di carico e performance aggregati su base mensile. Definisce un contratto dati pulito e sicuro
//per il trasferimento dei risultati elaborati verso lo strato di presentazione, usando tipi per i campi opzionali
// per distinguere in modo inequivocabile l'assenza di misurazioni da un valore zero reale





package it.unipd.toson.strava.service;

import java.time.YearMonth;//importa la classe per gestire coppie di anno e mese senza specificare il giorno

//dichiara un record Java pubblico cioè una struttura dati immutabile e compatta per gli aggregati mensili
public record MonthlySummary(
    YearMonth month, //memorizza il mese di riferimento temporale per il riepilogo

    int activityCount,//memorizza il conteggio complessivo delle attività di corsa valide registrate nel mese

    double distanceKm,//memorizza la distanza totale percorso nel mese convertita in km

    double durationHours,//memorizza la durata complessiva in movimento accumulata nel mese convertita in ore

    double elevationGainM,//memorizza il dislivello positivo cumulato totale raggiunto nel corso del mese in metri

    Double averageHeartRateBpm,//per la frequenza cardiaca media mensile ponderata(nulla se i dati cardiaci non ci sono)

    Integer relativeEffortTotal,//per la somma complessiva dello sforzo relativo accumulato nel mese

    Double averagePaceMinPerKm,//per il passo medio mensile calcolato sui totali globali di durata e distanza

    Double averagePowerWatts,//per la potenza media ponderata mensile (null se nessun sensore di potenza è disponibile)

    int heartRateActivityCount,//memorizza il numero esatto di attività del mese che possiedono dati di frequenza cardiaca valida

    int powerActivityCount) //memorizza il numero di attività del mese che possiedono registrazioni di potenze valide

 { }
