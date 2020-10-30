package Server;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Classe Challenge
 * Definisce un tipo di sfida.
 * Quando si avvia una sfida viene creato un tipo Challenge che andrà a far parte
 * di una lista condivisa tra i thread che gestiscono la sfida.
 * Solo i thread interessati potranno accedere all'oggetto Challenge che li riguarda.
 * Nella classe si raccolgono gli username degli avversari la lista di parole da mandare
 * e le parole tradotte, il punteggio di entrambi gli sfidanti e due variabili AtomicBoolean
 * che indicano rispettivamente quando le sfide sono finite.
 */

public class Challenge {
  private String sfidante;
  private String sfidato;
  private ArrayList<String> wordlList;
  private ArrayList<String> wordlListTranslated;
  private int sfidanteP = 0;
  private int sfidatoP = 0;
  private AtomicBoolean tSfidante = new AtomicBoolean(false);
  private AtomicBoolean tSfidato = new AtomicBoolean(false);

  public Challenge(String sfidante, String sfidato){
    this.sfidante = sfidante;
    this.sfidato = sfidato;
  }

  /**
   * @return nome dello sfidante
   */
  public String getSfidante(){
    return this.sfidante;
  }

  /**
   * @return nome dello sfidato
   */
  public String getSfidato() {
    return this.sfidato;
  }

  /**
   * @return arraylist delle parole scelte
   */
  public ArrayList<String> getWL() {
    return this.wordlList;
  }

  /**
   * @return arraylist delle parole tradotte
   */
  public ArrayList<String> getWLT() {
    return this.wordlListTranslated;
  }

  /**
   * Incrementa di p il punteggio dello sfidante
   * @param punteggio da aggiungere
   */
  public void setsfidanteP(int p) {
    sfidanteP = sfidanteP + p;
  }

  /**
   * Incrementa di p il punteggio dello sfidato
   * @param p punteggio da aggiungere
   */
  public void setsfidatoP(int p) {
    sfidatoP = sfidatoP + p;
  }

  /**
   * @return punteggio dello sfidante
   */
  public int getsfidanteP() {
    return sfidanteP;
  }

  /**
   * @return punteggio dello sfidato
   */
  public int getsfidatoP() {
    return sfidatoP;
  }

  /**
   * Setta la variabile AtomicBoolean che indica se lo sfidante
   * ha finito o no la sfida.
   * @param b valore booleano su cui settare la variabile
   */
  public void setTsfidante(Boolean b) {
    tSfidante.set(b);;
  }

  /**
   * Setta la variabile AtomicBoolean che indica se lo sfidato 
   * ha finito o no la sfida.
   * @param b valore booleano su cui settare la variabile
   */
  public void setTsfidato(Boolean b) {
    tSfidato.set(b);
  }

  /**
   * @return valore della variabile AtomicBoolean dello sfidante
   */
  public Boolean getTsfidante() {
    return this.tSfidante.get();
  }

  /**
   * @return valore della variabile AtomicBoolean dello sfidato
   */
  public Boolean getTsfidato() {
    return this.tSfidato.get();
  }

  /**
   * Aggiorna le liste di parole per la sfida
   * @param wordlList = lista delle parole scelte
   * @param wordlListTranslated = lista delle parole tradotte
   */
  public void setWL(ArrayList<String> wordlList,ArrayList<String> wordlListTranslated){
    this.wordlList = wordlList;
    this.wordlListTranslated = wordlListTranslated;
  }

  /**
   * Override del metodo equals per stabilire come deve avvenire il confronto
   * tra due oggetti Challenge.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Challenge c = (Challenge) o;
    return Objects.equals(sfidante, c.getSfidante()) && Objects.equals(sfidato, c.getSfidato());
  }

  /**
   * Serve per il confronto tra due tipi Challenge
   */
  public int hashCode() {
    return Objects.hash(sfidante, sfidato);
  }
}