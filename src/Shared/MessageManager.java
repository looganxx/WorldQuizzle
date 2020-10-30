package Shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Classe MessageManager
 * Invio/ricezione dei messaggi e serializzazione degli oggetti
 */
public class MessageManager {

  /**
   * Ricezione del messaggio. Viene prima ricevuta la dimensione del messaggio
   * e successivamente il messaggio stesso serializzato.
   * Per deserializzare il messaggio viene usata la funzione "getObjectFromByte".
   * @param socket = canale dal quale ricevere il messaggio
   * @return  tipo Message della risposta ricevuta dal Server dopo aver 
   *          deserializzato il messaggio.
   * @throws IOException
   */
  public static Message readMsg(SocketChannel socket) throws IOException {
    if(socket == null) throw new NullPointerException();

    ByteBuffer size = ByteBuffer.allocate(4);
    int read;
    while(size.remaining() != 0){
      read = socket.read(size);
      if (read == -1) throw new IOException();
    }

    ByteBuffer msg = ByteBuffer.allocate(size.getInt(0));
    while(msg.remaining() != 0){
      read = socket.read(msg);
      if (read == -1) throw new IOException();
    }

    return (Message) getObjectFromByte(msg.array());
  }

  /**
   * Invio del messaggio. Viene prima mandata la dimensione del messaggio e
   * successivamente il messaggio stesso serializzato.
   * Per serializzare il messaggio viene usata la funzione "getByteFromObject".
   * @param socket   = canale sul quale mandare il messaggio
   * @param response = messaggio da mandare dopo averlo serializzato
   * @throws IOException
   */
  public static void sendMsg(SocketChannel socket, Message response) throws IOException {
    if(socket == null || response == null) throw new NullPointerException();

    byte b[] = getByteFromObject(response);
    ByteBuffer size = ByteBuffer.allocate(4).putInt(b.length);
    size.flip();

    // Invio la dimensione del messaggio
    while (size.hasRemaining())
      socket.write(size);

    //Invio del messaggio serializzato
    ByteBuffer msg = ByteBuffer.wrap(b);
    while (msg.hasRemaining())
      socket.write(msg);
  }

  /**
   * Funzione che deserializza il messaggio
   * @param b = byte array del messaggio ricevuto dal socket
   * @return oggetto generico deserializzato
   */
  private static Object getObjectFromByte(byte[] b) {
    ByteArrayInputStream bis = new ByteArrayInputStream(b);

    try (ObjectInputStream in = new ObjectInputStream(bis);) {
      return in.readObject();
    } catch (IOException ex) {
      ex.printStackTrace();
      return null;
    } catch (ClassNotFoundException ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * Funzione che serializza un messaggio
   * @param o = oggetto da serializzare
   * @return byte array rappresentante l'oggetto serializzato
   */
  private static byte[] getByteFromObject(Object o) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    try (ObjectOutputStream out = new ObjectOutputStream(bos);) {
      out.writeObject(o);
      out.flush();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return bos.toByteArray();

  }
}