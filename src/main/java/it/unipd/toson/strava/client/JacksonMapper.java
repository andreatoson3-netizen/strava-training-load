//invece di creare tanti oggetti ObjectMapper ne espone uno condivido (MAPPER).
// Grazie all'aggiunta del modulo JavaTimeModule sa interpretare i formati restituiti da Strava convertendoli nei tipi
//ZonedDataTime di Java. Se Strava dovesse restituire un JSON illeggibile o malformato la classe lancia eccezzione


package it.unipd.toson.strava.client;

import com.fasterxml.jackson.databind.ObjectMapper;//importa la classe per configurare ed eseguire conversione JSON-oggetti Java(data binding)
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;//importa la classe che insegna a leggere e scrivere le classi java.time
                                                            // perchè senza le date non sarebbero convertibili
import it.unipd.toson.strava.model.Activity;//importa il modello che rappresenta la singola attività sportiva di Strava
import java.util.List;//importa l'interfaccia List per gestire elenchi di oggetti


public final class JacksonMapper
{
    //crea un unico ObjectMapper condiviso(secondo la documentazione di Jackson è la strategia che conviene).
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private JacksonMapper() { } //costruttore privato per impedire di creare altre istanze


    //deserializza l'array JSON restituito da /athlete/activities, quindi riceve una stringa JSON e restituisce una lista di attività
    public static List<Activity> activities(String json)
    {   //inizia il blocco try per rilevare errori di sintassi o mappatura del JSON
        try
        {
            //readerForListOf legge un array JSON come List<Activity>
            return MAPPER.readerForListOf(Activity.class).readValue(json);
        }
        //se il parsing fallisce è lanciata un'eccezione con messaggio
        catch (Exception e)
        {
            throw new IllegalArgumentException("Risposta attività JSON non valida", e);
        }
    }

    //metodo per deserializzare la risposta JSON dei token OAuth.
    public static TokenPair tokenPair(String json)
    {
        //blocco di sicurezza per lettura del JSON dei token
        try
        {
            //converte il JSON in un singolo oggetto di tipo TokenPair
            return MAPPER.readValue(json, TokenPair.class);
        }
        //in caso di formattazione viene lanciata eccezione
        catch (Exception e)
        {
            throw new IllegalArgumentException("Risposta token JSON non valida", e);
        }
    }
}
