package Server;

/**
 * Classe JModel
 * Rappresenta la risposta in Json da parte del sito di traduzione
 */
public class JModel {
  private Info responseData;

  public class Info {
    private String translatedText;
    
    public String getTranslatedText(){
      return translatedText;
    }
  }
  
  public Info getInfo(){
    return responseData;
  }
}