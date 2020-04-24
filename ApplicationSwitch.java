/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ILightSensor;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.util.Utils;


/**
 *
 * @author labnet
 */
public class ApplicationSwitch extends MIDlet {
    //Thread que escreve os dados escutados na console.
    private baseStationListener escutarBS = new baseStationListener();
    private Thread baseStation = new Thread(escutarBS);
    //Switchs que servem para dar o input das aplicacoes
    private ISwitch sw1 = (ISwitch)Resources.lookup(ISwitch.class, "SW1");
    private ISwitch sw2 = (ISwitch)Resources.lookup(ISwitch.class, "SW2");
    private ILightSensor light = (ILightSensor) Resources.lookup(ILightSensor.class);
    //Representa as aplicações que devem rodar
    private boolean application1 = false;
    private boolean application2 = false;

    static private char APP_VECTOR = 'V';
    static private char START = 'S';

    protected void startApp() throws MIDletStateChangeException {
        enviarStart();
        baseStation.start();
        System.out.println("Base Station do Shared Leach Ligada");
        int entrada = 0;

        try {
            while (true) {

                if (light.getValue() == 0) {
                    application1 = sw1.isClosed();
                    application2 = sw2.isClosed();
                    System.out.println(entrada++ +":" + application1 + ":" + application2);
                    inserirAplicacoesNaRede();
                }
                Utils.sleep(5000);

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

    private void enviarStart() {
        try {
            RadiogramConnection conn = (RadiogramConnection) Connector.open("radiogram://broadcast:123");
            Radiogram rfg = (Radiogram) conn.newDatagram(45);
            rfg.reset();
            rfg.writeChar(START);
            conn.send(rfg);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void inserirAplicacoesNaRede() {

        try {
            RadiogramConnection apps = (RadiogramConnection) Connector.open("radiogram://broadcast:124"); //Canal na porta 124
            Radiogram pkt = (Radiogram) apps.newDatagram(45);

            pkt.reset();

            pkt.writeChar(APP_VECTOR);
            pkt.writeBoolean(application1); //Pacote com: app1(bool),app2(bool)
            pkt.writeBoolean(application2);

            apps.send(pkt);

            apps.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}

class baseStationListener extends Thread {
    public void run(){
        RadiogramConnection tx = null;
        Radiogram xdg;

        try {
            tx = (RadiogramConnection) Connector.open("radiogram://:123");
            xdg = (Radiogram) tx.newDatagram(45);

            while (true) {

                tx.receive(xdg);

                if (xdg.readChar() == 'X') {

                   // System.out.println("Dado do cluster: " + xdg.getAddress() + " Luminosidade: " + xdg.readDouble()+ " Temperatura: " + xdg.readDouble()+ " Umidade: " + xdg.readDouble()+ " AAA: " + xdg.readDouble()+ " BBB: " + xdg.readDouble());


                }


            }
        } catch (IOException yolo) {
            yolo.printStackTrace();
        }

    }

}
