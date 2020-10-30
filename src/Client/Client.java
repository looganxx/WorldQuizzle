package Client;

import java.rmi.NotBoundException;
import javax.swing.JOptionPane;
import Gui.LoginWQ;

/**
 * Classe Client
 * Richiede l'inizializzazione della connessione al client
 * e iniziallizza la GUI per l'utente.
 */
public class Client {
  @SuppressWarnings("all")
  public static void main(String[] args) throws NotBoundException {
    //creazione del clientmanager per la gestione di tutte le comunicazioni con il server
    ClientManager CM = new ClientManager();
    try {
      //creazione connessione con il server
      CM.initClient();
      //creazione interfaccia grafica
      LoginWQ log = new LoginWQ(CM);
    } catch (Exception e) {
      // caso in cui il server sia offline
      JOptionPane.showMessageDialog(null, "Server offline!");
    }
  }
}
