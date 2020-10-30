package Shared;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Classe Message
 * Rappresentazione di un messaggio scambiato tra client/server
 */

public class Message implements Serializable {
  static final long serialVersionUID = 1L;

  public static final int LOGIN = 0;
  public static final int LOGIN_OK = 1;
  public static final int LOGIN_ERROR = 2;
  public static final int LOGIN_ERROR_USERNAME = 3;
  public static final int LOGIN_ERROR_PASSWORD = 4;
  public static final int LOGIN_ERROR_ALREADY_ONLINE = 5;

  public static final int LOGOUT = 6;
  public static final int LOGOUT_OK = 7;
  public static final int LOGOUT_ERROR = 8;

  public static final int FRIEND_REQUEST = 9;
  public static final int FRIEND_REQUEST_OK = 10;
  public static final int FRIEND_REQUEST_ERROR = 11;
  public static final int FRIEND_ALREADY_FRIENDS = 12;

  public static final int FRIENDS_LIST = 13;
  public static final int FRIENDS_LIST_OK = 14;
  public static final int FRIENDS_LIST_ERROR = 15;

  public static final int PUNTEGGIO = 16;
  public static final int PUNTEGGIO_OK = 17;
  public static final int PUNTEGGIO_ERROR = 18;

  public static final int CLASSIFICA = 19;
  public static final int CLASSIFICA_OK = 20;
  public static final int CLASSIFICA_ERROR = 21;

  public static final int SFIDA = 22;
  public static final int SFIDA_OK = 23;
  public static final int SFIDA_NOT_ONLINE = 24;
  public static final int SFIDA_NOT_FRIEND = 25;
  public static final int SFIDA_NOT_EXIST = 26;
  public static final int SFIDA_ACCEPTED = 27;
  public static final int SFIDA_REJECTED = 28;
  public static final int SFIDA_BUSY = 29;
  public static final int SFIDA_WAIT = 30;
  public static final int SFIDA_RESULT = 31;
  public static final int SFIDA_WORD = 32;
  public static final int SFIDA_WORD_REPLY = 33;
  public static final int SFIDA_TIMER = 34;

  public static final int PORT = 35;
  public static final int PORT_OK = 36;
  public static final int PORT_ERROR = 37;

  public static final int NO_CONNECTION = 38;

  int type;
  byte[] payload;

  /**
   * Tipo di metodo costruttore con la possibilità di inserire nel payload un
   * intero.
   * @param typetipo di messaggio mandato identificato da tutti i codici sopra
   *                 descritti.
   * @param payload  = messaggio contenuto sotto formadi intero
   */
  public Message(int type, int payload) {
    if (type < 0)
      throw new IllegalArgumentException();
    this.type = type;

    // Conversione da int a byte[] usando un buffer
    ByteBuffer b = ByteBuffer.allocate(Integer.BYTES);
    b.putInt(payload);
    this.payload = b.array();
  }

  /**
   * Tipo di metodo costruttore con la possibilità di inserire nel payload una
   * stringa
   * 
   * @param type    = tipo di messaggio mandato identificato da tutti i codici
   *                sopra descritti
   * @param payload = messaggio contenuto sotto forma di stringa
   */
  public Message(int type, String payload) {
    if (type < 0)
      throw new IllegalArgumentException();
    if (payload == null)
      throw new NullPointerException();

    this.type = type;
    this.payload = payload.getBytes();
  }

  /**
   * Tipo di metodo costruttore con la possibilità di inserire nel payload un
   * array di byte.
   * @param type    = tipo di messaggio mandato identificato da tutti i codici
   *                sopra descritti
   * @param payload = messaggio contenuto sotto forma di array di byte
   */
  public Message(int type, byte[] payload) {
    if (type < 0)
      throw new IllegalArgumentException();
    if (payload == null)
      throw new NullPointerException();

    this.type = type;
    this.payload = payload;
  }

  /**
   * @return tipo del messaggio
   */
  public int getType() {
    return type;
  }

  /**
   * @return payload sotto forma di intero
   */
  public int getPayloadInt() {
    ByteBuffer b = ByteBuffer.wrap(payload);
    return b.getInt(0);
  }

  /**
   * @return payload sotto forma di Stringa
   */
  public String getPayloadString() {
    String s = new String(payload);
    return s;
  }

  /**
   * @return payload sotto forma di array di byte
   */
  public byte[] getPayloadByte() {
    return payload;
  }
}