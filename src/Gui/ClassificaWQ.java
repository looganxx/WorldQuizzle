package Gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Type;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import Client.ClientManager;
import Shared.Message;
import Shared.User;

/**
 * Classe ClassificaWQ 
 * Viene mostrata la classifica in base al punteggio
 * ottenuto da tutti gli amici dell'utente che ha effettuato l'accesso. 
 * Viene fatta la lettura della risposta del Server, viene prima deserializzato 
 * il messaggio, letto come json di tipo ArrayList di User e successivamente
 * viene mostrato all'utente scorrendo l'array.
 * In fondo al form vi è il bottone "backHome" per tornare al form "home".
 */

public class ClassificaWQ {
  private JFrame home;
  private Message esito;
  @SuppressWarnings("all")
  private ClientManager CM;
  private JFrame classifica;
  private JButton backHome;
  
  public ClassificaWQ(ClientManager CM, JFrame home, Message esito) {
    this.CM = CM;
    this.home = home;
    this.esito = esito;
    classifica = new JFrame("Word Quizzle");
    initialize();
    classifica.setVisible(true);
  }

  private void initialize() {
    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    classifica.setSize((int) dimension.getWidth() / 4, (int) (dimension.getHeight() * (0.6)));
    classifica.setLocation(home.getX(), home.getY());
    classifica.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    classifica.getContentPane().setLayout(null);
    classifica.getContentPane().setBackground(Color.blue);

    JLabel title = new JLabel("Word Quizzle", JLabel.CENTER);
    title.setBounds(20, 10, 300, 40);
    title.setFont(new Font("Tahoma", Font.PLAIN, 30));
    title.setForeground(Color.white);
    classifica.getContentPane().add(title);

    JLabel presentation = new JLabel("Classifica Amici", JLabel.CENTER);
    presentation.setBounds(60, 60, 200, 30);
    presentation.setFont(new Font("Tahoma", Font.PLAIN, 16));
    presentation.setForeground(Color.white);
    classifica.getContentPane().add(presentation);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String s = esito.getPayloadString();
    Type listC = new TypeToken<ArrayList<User>>() {
    }.getType();
    ArrayList<User> classificaA = gson.fromJson(s, listC);

    for (int i = 0; i < classificaA.size(); i++) {
      User u = classificaA.get(i);
      JLabel lbUser = new JLabel(u.getName());
      lbUser.setBounds(100, (int) (100 + (20 * (i))), 105, 21);
      lbUser.setForeground(Color.white);
      lbUser.setFont(new Font("Tahoma", Font.PLAIN, 12));
      classifica.getContentPane().add(lbUser);

      JLabel lbScore = new JLabel("" + u.getPunteggio());
      lbScore.setBounds(200, (int) (100 + (20 * (i))), 105, 21);
      lbScore.setForeground(Color.white);
      lbScore.setFont(new Font("Tahoma", Font.PLAIN, 12));
      classifica.getContentPane().add(lbScore);
    }

    backHome = new JButton("Home");
    backHome.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        home.setLocation(classifica.getX(), classifica.getY());
        home.setVisible(true);
        classifica.setVisible(false);
      }
    });
    backHome.setBounds(80, 400, 180, 25);
    classifica.getContentPane().add(backHome);
  }

}