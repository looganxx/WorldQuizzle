package Server;

import Shared.Message;

/**
 * Classe UserAttach
 * Viene usata nel selector per tenere traccia del messaggio ricevuto,
 * quello di risposta e l'username dell'utente.
 */
public class UserAttach {
  Message request;
  Message response;
  String username;
}