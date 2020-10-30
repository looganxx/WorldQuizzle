package Server;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe MyTimer
 * Viene creato il timer nel thread di sfida.
 * Quando viene avviato dopo T2 setta la variabile di iterazione del ciclo
 * ad un valore TIMER_CODE che notifica la fine della sfida.
 */
public class MyTimer extends TimerTask{
  AtomicInteger i;

  public MyTimer(AtomicInteger i){
    this.i = i;
  }

  @Override
  public void run() {
    i.set(TSfida.TIMER_CODE);
  }
  
}