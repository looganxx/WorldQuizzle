package Shared;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Classe User
 * Viene usata per la gestione degli utenti nell'hash map.
 * Contiene l'username e password dell'utente, la sua lista di amici
 * e il punteggio ottenuto.
 */
public class User implements Comparable<User>{
  private String name;
  private String psw;
  private ArrayList<String> friends;
  private int punteggio_utente;

  public User(String name, String psw){
    this.name = name;
    this.psw = psw;
    this.friends = new ArrayList<String>();
    this.punteggio_utente = 0;
  }

  /**
   * @return username dell'utente
   */
  public String getName() {
    return this.name;
  }
  
  /**
   * @return password dell'utente
   */
  public String getPsw(){
    return this.psw;
  }

  /**
   * Aggiorna il punteggio dell'utente
   * 
   * @param p punteggio da assegnare all'utente
   */
  public void setPunteggio(int p) {
    this.punteggio_utente = p;
  }

  /**
   * @return punteggio dell'utente
   */
  public int getPunteggio() {
    return this.punteggio_utente;
  }

  /**
   * @return lista degli amici dell'utente
   */
  public ArrayList<String> getFriends() {
    return friends;
  }

  /**
   * Override del metodo equals per stabilire come deve avvenire il confronto tra
   * due oggetti User.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    User u = (User) o;
    return Objects.equals(name, u.getName());
  }

  /**
   * Serve per il confronto tra due tipi User
   */
  public int hashCode() {
    return Objects.hash(name);
  }


  /**
   * Override del metodo compareTo.
   * Serve per organizzare la lista di User in base al punteggio
   * nel momento in cui va restituita la lista degli amici con il loro punteggio.
   */
  @Override
  public int compareTo(User o) {
    if(this.getPunteggio() < o.getPunteggio()) return 1;
    else{
      if(this.getPunteggio() == o.getPunteggio()) return 0;
    }
    return -1;  
  }
}