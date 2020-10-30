package Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import Shared.Message;
import Shared.MessageManager;
import Shared.User;

/**
 * Classe Server
 * È la main class del server.
 * Inizializza il dizionario di parole per le sfide.
 * Avvia lo stub RMI.
 * Crea un threadPool con un array blocking queue.
 * Crea il server socket channel e inizializza il selector.
 * Gestione del selector, nel caso di una qualsiasi richiesta dal client
 * delega il controllo al thread ServerManager che viene avviato nel thread pool.
 * Mette a disposizione del ServerManager varie funzioni di accesso a tutte le variabili
 * condivise tra i thread.
 */

public class Server {
  static ConcurrentHashMap<String, Integer> ports = new ConcurrentHashMap<String, Integer>();
  static ArrayList<Challenge> sfide = new ArrayList<Challenge>();
  static ReentrantLock sfideLock = new ReentrantLock();
  private static int PORT = 6789;
  private static int PORT_RMI = 6790;
  static ReentrantLock filelock = new ReentrantLock();
  
  // nome - User
  static ConcurrentHashMap<String, User> usersDB = new ConcurrentHashMap<String, User>();
  static ServerSocketChannel challengeChannel;

  private static ArrayList<String> online = new ArrayList<String>();
  static ReentrantLock onlineLock = new ReentrantLock();
  private static ThreadPoolExecutor handlers;

  static ArrayList<String> dic = new ArrayList<String>();

  public static void main(String[] args) {
    ServerSocketChannel serverChannel;
    Selector selector;
    @SuppressWarnings("all")
    DatagramSocket socketServerUdp;
    
    filelock.lock();
    File file = new File("dictionary.txt");
    BufferedReader br;
    try {
      br = new BufferedReader(new FileReader(file));
      String st;
      while ((st = br.readLine()) != null) {
        dic.add(st);
      }
      br.close();
    } catch (Exception e2) {
      e2.printStackTrace();
      filelock.unlock();
      return;
    }
    filelock.unlock();

    try {
      Registration R = (Registration) new StubRegistration();
      LocateRegistry.createRegistry(PORT_RMI);
      Registry registry = LocateRegistry.getRegistry(PORT_RMI);
      registry.rebind(Registration.SERVER_NAME, R);
    } catch (RemoteException e) {
      System.out.println("Server error:" + e.getMessage());
    } catch (Exception e1) {
      e1.printStackTrace();
    }

    handlers = new ThreadPoolExecutor(0, 20, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(4));

    try {
      // creazione del socket sulla porta
      serverChannel = ServerSocketChannel.open();
      serverChannel.socket().bind(new InetSocketAddress(PORT));
      serverChannel.configureBlocking(false);
      // apertura selector
      selector = Selector.open();
      // unica possibile operazione: accettare una connessione
      serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    while (true) {
      try {
        selector.select();
      } catch (final IOException ex) {
        ex.printStackTrace();
        break;
      }
      // controllo delle chiavi prese dal selector
      Set<SelectionKey> setsel = selector.selectedKeys();
      Iterator<SelectionKey> iterator = setsel.iterator();
      while (iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        try {
          // se è ready per accettare una nuova connessione
          if (key.isAcceptable()) {
            // creazione connessione con un client
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            //System.out.println("Accepted connection from " + client.getRemoteAddress());
            // configurato non blocking
            client.configureBlocking(false);
            SelectionKey key2 = client.register(selector, SelectionKey.OP_READ);
            key2.attach(new UserAttach());
          } else
          // se è ready per leggere da un SocketChannel
          if (key.isReadable()) {
            // riprendo il channel salvato sulla chiave
            SocketChannel client = (SocketChannel) key.channel();
            UserAttach state = (UserAttach) key.attachment();
            // leggo il messaggio
            Message m = MessageManager.readMsg(client);
            state.request = m;
            //blocco la chiave in modo da lanciare il thread per servire la richiesta.
            key.interestOps(0);
            //lancio il thread gestore
            ServerManager h = new ServerManager(state, key, selector);
            handlers.execute(h);
          } else if (key.isWritable()) {
            // riprendo il channel salvato sulla chiave
            SocketChannel client = (SocketChannel) key.channel();
            UserAttach state = (UserAttach) key.attachment();
            
            //scrittura messaggio
            MessageManager.sendMsg(client, state.response);

            //rimesso in lettura
            key.interestOps(SelectionKey.OP_READ);
          }
        } catch (IOException e) {
          UserAttach state = (UserAttach) key.attachment();
          goOffline(state.username);
          key.cancel();
          try {
            key.channel().close();
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * Aggiorna il database
   * @param users = arraylist di User
   */
  public static void updateDB(ArrayList<User> users) {
    User u;
    for(int i = 0; i < users.size(); i++){
      u = users.get(i);
      usersDB.put(u.getName(), u);
    }
  }

  /**
   * Controlla se nel db esiste un determinato utente per il login
   * @param user = username utente da controllare
   * @param psw = password utente da controllare
   * @return 1 login ok, 3 username error, 4 password error
   */
  public static int checkDB(String user, String psw){
    if(usersDB.containsKey(user) == true){
      User u = usersDB.get(user);
      if(psw.equals(u.getPsw()) == true){
        return 1;
      }
    }else {
      return 3;
    }
    return 4;
  }

  /**
   * Controlla se l'utente con nome "username" è online
   * @param username = username dell'utente da controllare
   * @return true se è online, false se non lo è
   */
  public static boolean checkOnline(String username){
    onlineLock.lock();
    boolean e = online.contains(username);
    onlineLock.unlock();
    return e;
  }

  /**
   * Mette nell'arraylist degli utenti online l'utente con nome "username"
   * @param username = nome dell'utente da inserire
   */
  public static void goOnline(String username) {
    onlineLock.lock();
    online.add(username);
    onlineLock.unlock();
  }

  /**
   * Rimuove dall'arraylist degli offline l'utente con nome "username"
   * @param username = nome dell'utente da rimuovere
   */
  public static void goOffline(String username) {
    onlineLock.lock();
    online.remove(username);
    onlineLock.unlock();
  }

  /**
   * Verifica che all'interno del database ci sia l'utente con nome "user"
   * @param user = nome dell'utente da verificare 
   * @return true se è nel database, false se non è presente
   */
  public static boolean checkUser(String user){
    return usersDB.containsKey(user);
  }

  /**
   * Aggiorna il file .json utilizzando l'hashmap
   * @throws IOException
   */
  public static void updateFile() throws IOException {
    filelock.lock();
    File db = new File("database.json");
    FileWriter writer = new FileWriter(db.getAbsolutePath());
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    ArrayList<User> users = new ArrayList<User>();
    for (String s : usersDB.keySet()) {
      users.add(usersDB.get(s));
    }
    gson.toJson(users, writer);
    filelock.unlock();
    writer.flush();
    writer.close();
  }

  /**
   * Aggiorna il punteggio dell'utente nel database
   * @param x = punteggio da incrementare
   * @param user = utente al quale incrementare il punteggio
   */
  public static void updatePoint(int x, String user) {
    int p = usersDB.get(user).getPunteggio();
    if((p + x) < 0){
      p = 0;
      x = 0;
    } 
    usersDB.get(user).setPunteggio(p+x);
  }
}