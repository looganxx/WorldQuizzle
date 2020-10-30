package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia Registration
 * Raccoglie le possibili operazione implementate nello stub per il Client
 */

public interface Registration extends Remote {
  public static final String SERVER_NAME = "WQGame";
  
  /**
   * Permette di registrare un utente
   * @param nickUtente  = nome utente
   * @param password    = password
   * @return  1 se la registrazione è avvenuta con successo, 0 altrimenti
   * @throws RemoteException
   */
  public int registra_utente(String nickUtente, String password) throws RemoteException;
}
