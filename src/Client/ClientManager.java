package Client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicBoolean;

import Server.Registration;
import Shared.Message;
import Shared.MessageManager;

/**
 * Classe Client Manager
 * Gestisce tutte le operazioni principali offerte da WQ
 */

public class ClientManager {
  private static int PORT = 6789; // porta di connessione TCP con il server
  private static int PORT_RMI = 6790; // porta di connessione all'RMI per la registrazione
  private SocketChannel socket; // socket TCP 
  private DatagramSocket socketClientUdp; // socket UDP per la ricezione di sfide
  private String user; //nome dell'utente che effettua il login
  private Registration R; //oggetto registration dello stub RMI
  public AtomicBoolean busy = new AtomicBoolean(false); //indica se l'utente è impegnato in una partita

  /**
   * Metodo costruttore della classe Client Manager
   * viene creata la connessione per la registrazione con la porta RMI
   */
  public ClientManager() {
    try {
      Registry registry = LocateRegistry.getRegistry(PORT_RMI);
      R = (Registration) registry.lookup(Registration.SERVER_NAME);
    } catch (RemoteException e) {
      System.out.println("Server error: " + e.getMessage());
    } catch (NotBoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * @return username
   */
  public String getUser() {
    return this.user;
  }
  /**
   * Inizializza la connessione TCP e UDP
   * @throws IOException in caso di mancata creazione delle connessioni
   */
  public void initClient() throws IOException {
    // richiesta e creazione connessione per TCP e UDP
    SocketAddress address = new InetSocketAddress("localhost", PORT);
    socket = SocketChannel.open(address);
    socketClientUdp = new DatagramSocket();
  }

  /**
   * @return socket UDP creato nel metodo initClient
   */
  public DatagramSocket getSocket(){
    return socketClientUdp;
  }

  /**
   * @return socket TCP creato nel metodo initClient
   */
  public SocketChannel getSocketC() {
    return socket;
  }

  /**
   * Registra l'utente utilizzando l'interfaccia RMI che sfrutta il metodo
   * implementato nello stub
   * @param username = nome dell'utente
   * @param password = password dell'utente
   * @return 1 se la registrazione ha avuto successo, 0 altrimenti
   * @throws RemoteException
   */
  public int register(String username, String password) throws RemoteException {
    return R.registra_utente(username, password);
  }
  
  /**
   * Effettua il login dell'utente
   * 
   * @param username = nome dell'utente
   * @param psw      = password dell'utente
   * @return messaggio ricevuto dal server alla richiesta di connessione
   * @throws IOException
   */
  public Message login(String username, String psw) throws IOException {
    if(username == null || psw == null) throw new NullPointerException();
    Message msg = new Message(Message.LOGIN, username + " " + psw);
    MessageManager.sendMsg(this.socket, msg);
    Message rcv = MessageManager.readMsg(socket);
    this.user = username;
    return rcv;
  }

  /**
   * Manda la porta UDP del client sulla quale può ricevere le richieste di sfida
   * @param username = nome dell'utente
   * @return messaggio ricevuto dal server se la porta è stata correttamente ricevuta o meno
   * @throws IOException
   */
  public Message sendPortUdp(String username) throws IOException {
    if (username == null) throw new NullPointerException();
    Message msg = new Message(Message.PORT, socketClientUdp.getLocalPort());
    MessageManager.sendMsg(this.socket, msg);
    Message rcv = MessageManager.readMsg(socket);
    return rcv;
  }

  /**
   * Aggiunge un amico alla lista di amici
   * @param nickUtente = nome dell'utente che richiede la sfida
   * @param nickAmico = nome dell'utente da aggiungere agli amici
   * @return  messaggio contenente l'esito della richiesta
   * @throws IOException
   */
  public Message aggiungi_amico(String nickUtente, String nickAmico) throws IOException {
    if(nickUtente == null || nickAmico == null) throw new NullPointerException();
    Message msg = new Message(Message.FRIEND_REQUEST, nickUtente + " " + nickAmico);
    MessageManager.sendMsg(this.socket, msg);
    Message rcv = MessageManager.readMsg(socket);
    return rcv;
  }

  /**
   * Effettua il logout dell'utente
   * @param nickUtente = nome dell'utente che effettuta il logout
   * @return esito della richiesta di logout
   * @throws IOException
   */
  public Message logout(String nickUtente) throws IOException {
    // ! cancellare tutti i thread attivi per il client
    if (nickUtente == null) throw new NullPointerException();
    Message msg = new Message(Message.LOGOUT, nickUtente);
    MessageManager.sendMsg(this.socket, msg);
    Message rcv = MessageManager.readMsg(socket);
    return rcv;
  }

  /**
   * Richiesta della lista degli amici
   * @param nickUtente = nome dell'utente che vuole vedere i suoi amici
   * @return messaggio serializzato contenete un json
   * @throws IOException
   */
  public Message lista_amici(String nickUtente) throws IOException {
    if (nickUtente == null) throw new NullPointerException();
    Message msg = new Message(Message.FRIENDS_LIST, nickUtente);
    MessageManager.sendMsg(this.socket, msg);
    Message rcv = MessageManager.readMsg(socket);
    return rcv;
  }

  /**
   * Richiesta del punteggio totalizzato dall'utente nickUtente
   * @param nickUtente = nome dell'utente
   * @return messaggio contenente il punteggio dell'utente
   * @throws IOException
   */
  public Message punteggio(String nickUtente) throws IOException {
    if (nickUtente == null)
      throw new NullPointerException();
    Message msg = new Message(Message.PUNTEGGIO, nickUtente);
    MessageManager.sendMsg(this.socket, msg);
    Message rcv = MessageManager.readMsg(socket);
    return rcv;
  }

  /**
   * Richiesta della classifica degli amici dell'utente nickUtente
   * @param nickUtente = nome dell'utente
   * @return  messaggio serializzato contenente un json della classifica degli amici
   *          ordinata in base al punteggio
   * @throws IOException
   */
  public Message classifica(String nickUtente) throws IOException {
    if (nickUtente == null) throw new NullPointerException();
    Message msg = new Message(Message.CLASSIFICA, nickUtente);
    MessageManager.sendMsg(this.socket, msg);
    Message rcv = MessageManager.readMsg(socket);
    return rcv;
  }

  /**
   * Richiesta di sfida all'utente nickAmico
   * 
   * @param nickUtente = nome dell'utente
   * @param nickAmico = nome dell'amico che si vuole sfidare
   * @return messaggio contenente l'esito della sfida
   * @throws IOException
   */
  public Message sfida(String nickUtente, String nickAmico) throws IOException {
    if (nickUtente == null || nickAmico == null) throw new NullPointerException();
    Message msg = new Message(Message.SFIDA, nickUtente + " " + nickAmico);
    MessageManager.sendMsg(this.socket, msg);
    Message rcv = MessageManager.readMsg(socket);
    return rcv;
  }

  /**
   * Richiesta di sfida accettata, questa funzione viene chiamata solo dallo sfidante
   * che ha accettato una sfida in seguito a una richiesta
   * @param nickSfidante = nome dell'utente da cui è stata ricevuta la sfida
   * @return  messaggio che rispecchia l'esito della sfida: se è possibile cominciarla
   *          oppure se è stata annullata nel caso in cui lo sfidante non abbia risposto
   *          in tempo alla sfida, oppure se il pacchetto UDP non è arrivato.
   * @throws IOException
   */
  public Message sfidaAccepted(String nickSfidante) throws IOException {
    if (nickSfidante == null)
      throw new NullPointerException();
    Message msg = new Message(Message.SFIDA_ACCEPTED, nickSfidante);
    MessageManager.sendMsg(this.socket, msg);
    Message rcv = MessageManager.readMsg(socket);
    return rcv;
  }

}