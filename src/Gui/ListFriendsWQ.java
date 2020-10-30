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

/**
 * Classe ListFriendsWQ
 * Mostra la lista degli amici dell'utente collegato.
 * Viene fatta la lettura della risposta del Server, viene prima 
 * deserializzato il messaggio, letto come json di tipo ArrayList di stringhe
 * e successivamente viene mostrato all'utente scorrendo l'array.
 * In fondo al form vi è il bottone "backHome" per tornare al form "home".
 */

public class ListFriendsWQ {
  private JFrame home;
  private Message esito;
  @SuppressWarnings("all")
  private ClientManager CM;
  private JFrame list;
  private JButton backHome;

  public ListFriendsWQ(ClientManager CM, JFrame home, Message esito) {
    this.CM = CM;
    this.home = home;
    this.esito = esito;
    list = new JFrame("Word Quizzle");
    initialize();
    list.setVisible(true);
  }

  private void initialize() {
    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    list.setSize((int) dimension.getWidth() / 4, (int) (dimension.getHeight() * (0.6)));
    list.setLocation(home.getX(), home.getY());
    list.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    list.getContentPane().setLayout(null);
    list.getContentPane().setBackground(Color.blue);

    JLabel title = new JLabel("Word Quizzle", JLabel.CENTER);
    title.setBounds(20, 10, 300, 40);
    title.setFont(new Font("Tahoma", Font.PLAIN, 30));
    title.setForeground(Color.white);
    list.getContentPane().add(title);

    JLabel presentation = new JLabel("Lista Amici", JLabel.CENTER);
    presentation.setBounds(60, 60, 200, 30);
    presentation.setFont(new Font("Tahoma", Font.PLAIN, 16));
    presentation.setForeground(Color.white);
    list.getContentPane().add(presentation);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String s = esito.getPayloadString();
    Type listF = new TypeToken<ArrayList<String>>() {
    }.getType();
    ArrayList<String> friends = gson.fromJson(s, listF);

    for (int i = 0; i < friends.size(); i++) {
      JLabel lbUser = new JLabel(friends.get(i));
      lbUser.setBounds(140, (int) (100+(20*(i))), 105, 21);
      lbUser.setForeground(Color.white);
      lbUser.setFont(new Font("Tahoma", Font.PLAIN, 12));
      list.getContentPane().add(lbUser);
    }

    backHome = new JButton("Home");
    backHome.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        home.setLocation(list.getX(), list.getY());
        home.setVisible(true);
        list.setVisible(false);
      }
    });
    backHome.setBounds(80, 400, 180, 25);
    list.getContentPane().add(backHome);
  }

  
}