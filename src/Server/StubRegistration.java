package Server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import Shared.User;

/**
 * Classe StubRegistration
 * Implementa l'interfaccia Registration
 */

public class StubRegistration extends UnicastRemoteObject implements Registration {
  private static final long serialVersionUID = 1L;
  private ArrayList<User> users;

  /**
   * Se non esiste il database.json viene creato.
   * Se già esiste viene letto e salvato in una ConcurrentHashMap<username, User>
   * @throws IOException
   */
  public StubRegistration() throws IOException {
    Server.filelock.lock();
    File db = new File("database.json");
    if (!db.exists()) db.createNewFile();
    FileReader reader = new FileReader(db.getAbsolutePath());
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Type usersList = new TypeToken<ArrayList<User>>(){}.getType();
    users = gson.fromJson(reader, usersList);
    if (users == null) {
      users = new ArrayList<User>();
    }
    reader.close();
    Server.filelock.unlock();
    for(int i = 0; i < users.size(); i++){
      Server.usersDB.put(users.get(i).getName(), users.get(i));
    }
  }
  
  /**
   * Permette di registrare un utente
   * Viene letto il file .json sotto forma di array.
   * Se esiste già nell'array un utente con lo stesso nome ritorna 0,
   * altrimenti viene aggiunto, vengono aggiornati l'hashmap e il file.
   * @param nickUtente = nome utente
   * @param password   = password
   * @return 1 se la registrazione è avvenuta con successo, 0 altrimenti
   * @throws RemoteException
   */
  public int registra_utente(String nickUtente, String password) throws RemoteException {
    try {
      Server.filelock.lock();
      File db = new File("database.json");
      FileReader reader = new FileReader(db.getAbsolutePath());
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      Type usersList = new TypeToken<ArrayList<User>>(){}.getType();
      users = gson.fromJson(reader, usersList);
      if(users == null){
        users = new ArrayList<User>();
      }
      reader.close();
      User u = new User(nickUtente, password);
      if(users.size()!=0){
        if(users.contains(u)) return 0;
      }
      FileWriter writer = new FileWriter(db.getAbsolutePath());
      users.add(u);
      gson.toJson(users, writer);
      Server.filelock.unlock();
      writer.flush();
      writer.close();
      Server.updateDB(users);
      return 1;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }
}