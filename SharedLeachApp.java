/*
 * Modificação do protocolo LEACH de Heinzelman, para funcionar com aplicações compartilhadas
 *
 * Autor: Gabriel Rodrigues Caldas de Aquino
 *
 * Para o Projeto Final do curso de Ciência da Computação do DCC/IM/UFRJ.
 */
package org.sunspotworld;

import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.util.Utils;

//-------------------------------------
import java.util.Random;
import com.sun.spot.io.j2me.radiogram.*;
//-------------------------------------
import com.sun.spot.peripheral.Spot;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.resources.transducers.LEDColor;
//-------------------------------------------------
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import com.sun.spot.util.IEEEAddress;

//import com.sun.spot.resources.transducers.ISwitch;
/**
 *
 */
public class SharedLeachApp extends MIDlet {
    //Variáveis do SunSpot

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ILightSensor light = (ILightSensor) Resources.lookup(ILightSensor.class);
    private firstOrderRadio meuRadio = new firstOrderRadio();
    private bateriaPrimitiva minhaBateria = new bateriaPrimitiva();
    //Controle do tempo de round
    private long startTime = 0;
    private long tempoDeUmRound = 180000; //180 segundos, ou 3 min
    //Comunicação
    private RadiogramConnection tx = null;
    private Radiogram xdg;
    // Variáveis utilitárias
    private Random rand = new Random();
    private double num_randomico = 0.0;   //variavel auxiliar do numero randomico
    private double threshold;           //threshold de escolha do sensor para CH
    //Constantes da aplicação
    static private double PERCENTUAL = 0.1;    //percentual inicial de CH na rede
    // static private int QNTD_MAX_DE_CH = QNTD_SENSORES_NA_REDE * PERCENTUAL; //Define a quantidade máxima de anuncios de CH que vao ser comparadas
    static private int MAX_VALOR_X = 100;//metros //dita o maior valor da coordenada X do campo
    static private int MAX_VALOR_Y = 100;//metros //dita o maior valor da coordenada Y do campo
    static private int QNTD_SENSORES_NA_REDE = 100; //Define quantos sensores eu vou ter na minha rede
    static private int QNTD_MAX_DE_CH = 20;//(int) (QNTD_SENSORES_NA_REDE * PERCENTUAL * 1.2); //Define a quantidade máxima de anuncios de CH que vao ser comparadas
    static private int QNTD_MAX_DE_NOS_FOLHA = 50; //Define a quantidade máxima que um cluster head comporta
    static private int BS_Coord_X = 200;
    static private int BS_Coord_Y = 100; //Base Station no ponto (200,100)
    //Variáveis referentes a posição na rede e ao consumo de energia
    private int coord_X = 0;
    private int coord_Y = 0;
    private int meuCh_X = 0;
    private int meuCh_Y = 0;
    private double rangeDoCH = (MAX_VALOR_X * MAX_VALOR_Y) / 2;//(QNTD_SENSORES_NA_REDE * PERCENTUAL); //areaTotal/Qntd de clusterheads
    private double raioDeUmCH = 0;
    // Variáveis de controle
    private boolean souCH = false; //define o papel do sensor na rede
    private int round = 0; //contador de rounds do sensor
    private double NUM_ROUNDS_FORA = 1 / PERCENTUAL; //Numero de rounds que o nó deve ficar fora depois de ser CH
    private int estouForaAte = 0; //numero do round que o nó deve voltar a eleição
    private String id_DoMeuCH = null; //Variável que guarda o id do nó que é seu Cluster Head
    private String[] id_do_NoFolha = new String[QNTD_MAX_DE_NOS_FOLHA]; //Array que guarda os ids dos nós folha
    long myAddress = Spot.getInstance().getRadioPolicyManager().getIEEEAddress(); //Endereço do próprio nó
    private int quantidadeRealDeFolhas = 0; //quantidade de nos folha que responderam ao ch. menor que a quantidade total suportada de nos folha pelo ch
    private long meuNumeroTDMA = 999999;
    private long tempoBaseEmSteadyState_TDMA = 100; //Tempo base que é usado para ajustar os timers de TDMA do SteadyState
    private long tempoDePermanenciaEmSteadyState = 60000; //Tempo total que o nó deve ficar em steady state
    //Controle de mensagens
    static private char RECRUTAMENTO_DO_CH = 'R'; //Letra que identifica que a mensagem recebida é de recrutamento por um CH
    static private char INSCRICAO_NO_CLUSTER = 'A'; //Letra que identifica que um nó aceitou entrar em um cluster.
    static private char ENVIO_TDMA = 'E'; //Letra que identifica um envio de schedule de tdma
    static private char DADO_SENSORIADO = 'D'; //Letra que identifica que estou enviando um dado sensoriado ao CH
    static private char ENVIO_BS = 'X'; //Letra que identifica que estou enviando dados a Base Station
    static private char START = 'S'; //Letra que serve para a BS sincronizar os nos no inicio.
    //Variaveis relativas a minha extensao do leach.
    private ApplicationsListener escutarApps = new ApplicationsListener();
    private Thread listenApps = new Thread(escutarApps);
    private boolean aplicacao1 = true;
    private boolean aplicacao2 = true;
    private boolean aplicacao3 = true;
    private boolean aplicacao4 = true;
    private boolean aplicacao5 = true;
    private boolean aplicacao6 = true;
    private boolean aplicacao7 = true;
    private boolean aplicacao8 = true;
    private boolean aplicacao9 = true;
    private boolean aplicacao10 = true;

    protected void startApp() throws MIDletStateChangeException {
        listenApps.start();
        esperarStart();
        imprimir(Double.toString(minhaBateria.getCargaAtual()), 8); //Criar o track de gasto da bateria
        definePosicaoNoCampo();

        while (true) {
            if (minhaBateria.getCargaAtual() > 0) { //Ou seja só executo o leach se possuir carga em minha bateria.

                escolhaDoPapel_CH();

                imprimir("Sou CH: " + souCH + "| Round: " + round + "| Estou fora ate: " + estouForaAte, 4);

                leds.setOff();

                if (souCH) {
                    leds.setColor(LEDColor.GREEN);
                    leds.setOn();

                    Utils.sleep(10000 + rand.nextInt(5)); //Timer que faz o CH esperar mais tempo que os nós folha

                    try {
                        faseDeAvisoDoCH();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } //Advertisement Phase

                    if (id_do_NoFolha[0] != null) { //caso o primeiro nó folha seja null ele não recebeu mensagem de resposta de ninguem.
                        //e o round do CH deve acabar.
                        criarTDMA_CH();

                        steady_State_CH();
                    }

                } else {
                    leds.setColor(LEDColor.WHITE);
                    leds.setOn();

                    Utils.sleep(5000);

                    int tentativa = 0;
                    while (id_DoMeuCH == null) {//resolver STARVATIONNNNNN!!!!!!!!!!!!!
                        escolhaDoCluster();
                        tentativa++;
                        if (tentativa > 5) {
                            tentativa = 0;
                            break;
                        }
                    } //Advertisement Phase

                    if (id_DoMeuCH != null) {
                        esperarTDMA_Folha();
                        try {
                            steady_State_Folha();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }


                }
                imprimir(Double.toString(minhaBateria.getCargaAtual()), 8); //Criar o track de gasto da bateria
            }
            else {//Carga = 0
                imprimir(Double.toString(0), 8);//imprimo bateria vazia
            }
            imprimir(round + ":" + IEEEAddress.toDottedHex(myAddress) + ":" + souCH + ":" + coord_X + ":" + coord_Y + ":" + meuCh_X + ":" + meuCh_Y, 6);
            limparVariaveisTemporarias();


            while (System.currentTimeMillis() < (startTime + (tempoDeUmRound * (round + 1)))) {
            }
            round++;

        }
    }

    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        // Only called if startApp throws any exception other than MIDletStateChangeException
    }

    protected void escolhaDoPapel_CH() { //função que escolhe se o nó será CH
        num_randomico = rand.nextDouble();
        if (round >= estouForaAte) {

            if (round > (NUM_ROUNDS_FORA - 1)) {
                threshold = 1;
            } else {
                threshold = PERCENTUAL / (1 - (PERCENTUAL * (round % (1 / PERCENTUAL))));
            }
            if (num_randomico <= threshold) {
                souCH = true;  //ele ganhou a eleição de CH.
                estouForaAte = round + (int) NUM_ROUNDS_FORA; //anota o round em que foi CH pela ultima vez

            } else {
                souCH = false; //ele não ganhou a eleição de CH.

            }
        } else {
            threshold = 99999;
            souCH = false;
        }

    }

    private void faseDeAvisoDoCH() throws IOException {
        int i = 0;//indice do vetor de nós folha

        imprimir("Sou CH e estou enviando meu broadcast de recrutamento", 1);

        //Procedimento que faz enviar o anúncio de ser CH.
        try {
            tx = (RadiogramConnection) Connector.open("radiogram://broadcast:123");
            xdg = (Radiogram) tx.newDatagram(45);
            xdg.reset();

            xdg.writeChar(RECRUTAMENTO_DO_CH);

            minhaBateria.gastarEnergia(meuRadio.calcularGastoParaEnviar(xdg.getLength() * 8, rangeDoCH));
            imprimir("Gastei " + meuRadio.calcularGastoParaEnviar(xdg.getLength() * 8, rangeDoCH) + " Para enviar recrutamento", 3);

            xdg.writeInt(coord_X);
            xdg.writeInt(coord_Y);

            tx.send(xdg);
            tx.close();
        } catch (IOException ex) {
            imprimir("Error sending packet: " + ex, 5);
            ex.printStackTrace();
        }
//---------------------------------------------------------------------------

        //Procedimento de espera de retorno das mensagens dos CH.
        RadiogramConnection conn = null;
        try {

            conn = (RadiogramConnection) Connector.open("radiogram://:123");
            Radiogram rdg = (Radiogram) conn.newDatagram(45);

            //tempo na espera para receber mensagens
            long Connection_Timeout = 30000; //Esse timeout deve ser grande, pois esperar os sensores responderem é demorado
            long t = System.currentTimeMillis();
            long end = t + Connection_Timeout;

            conn.setTimeout(Connection_Timeout);

            //-------------------------------------
            imprimir("Esperando join dos nos folha", 1);

            while (i < QNTD_MAX_DE_NOS_FOLHA && System.currentTimeMillis() < end) {

                conn.receive(rdg);

                minhaBateria.gastarEnergia(meuRadio.calcularGastoParaReceber(rdg.getLength() * 8));
                imprimir("Gastei " + meuRadio.calcularGastoParaReceber(rdg.getLength() * 8) + " Recebendo uma confirmacao", 3);

                if (rdg.readChar() == INSCRICAO_NO_CLUSTER) {
                    id_do_NoFolha[i] = rdg.getAddress();
                    i++;
                }
            }
            //Esse loop quase sempre termina com timeout da conexão
            //(( i < QNTD_MAX_DE_NOS_FOLHA ))serve para não estourar o máximo de folhas suportado
            //(( System.currentTimeMillis() < end )) serve para o caso em que eu receba uma mensagem pouco antes do meu timeout e não necessite de outro timeout
            conn.close();
        } catch (IOException ex) {
            imprimir(ex.toString(), 5);
            //ex.printStackTrace();
        } finally {
            //Aqui executa sempre, mesmo caso haja timeout.
            //Logica que trata quais nós eu possuo em meu cluster
            try {
                conn.close();
                i = 0;
                while (i < QNTD_MAX_DE_NOS_FOLHA) {
                    if (id_do_NoFolha[i] != null) { //Se nó folha i == null, então não tenho mais nós folhas pra baixo
                        imprimir("Confirmacao redebida de " + id_do_NoFolha[i], 1);
                        i++;
                        quantidadeRealDeFolhas = i;
                    } else {
                        break;
                    }
                }
                i = 0;
                imprimir("Tenho " + quantidadeRealDeFolhas + " nos folha.", 1);

            } catch (IOException ex) {
            }
        }




    }

    private void escolhaDoCluster() {
        //Vetores que guardam a intensidade e o endereço do CH que enviou o pedido
        int[] intensidade = new int[QNTD_MAX_DE_CH];
        String[] enderecos = new String[QNTD_MAX_DE_CH];
        int[] ch_x = new int[QNTD_MAX_DE_CH];
        int[] ch_y = new int[QNTD_MAX_DE_CH];

        //Tempo em que o no deve esperar para escutar anúncios do CH
        long Connection_Timeout = 8000; //esse timeout deve ser pequeno pois eu tenho que responder logo aos meu CH
        long t = System.currentTimeMillis();
        long end = t + Connection_Timeout;

        //Variáveis de controle do loop que acha o melhor CH
        int i = 0;
        int melhor_intensidade = 0;

        RadiogramConnection rx = null;

        try {
            rx = (RadiogramConnection) Connector.open("radiogram://:123");
            Radiogram rdg = (Radiogram) rx.newDatagram(45);
            rx.setTimeout(Connection_Timeout);

            //Parte do código que faz o sensor esperar anúncios de cluster head
            while (i < QNTD_MAX_DE_CH && System.currentTimeMillis() < end) {
                try {

                    rx.receive(rdg);

                    minhaBateria.gastarEnergia(meuRadio.calcularGastoParaReceber((rdg.getLength() - 8) * 8));
                    imprimir("Gastei " + meuRadio.calcularGastoParaReceber((rdg.getLength() - 8) * 8) + " Recebendo um broadcast de um CH", 3);

                    if (rdg.readChar() == RECRUTAMENTO_DO_CH) {
                        //intensidade[i] = rdg.getRssi();
                        ch_x[i] = rdg.readInt();
                        ch_y[i] = rdg.readInt();

                        intensidade[i] = (int) ((1 / distanciaAoNo(ch_x[i], ch_y[i])) * 1000); //intensidade do sinal eh o inverso da distancia
                        enderecos[i] = rdg.getAddress();

                        imprimir(" Pacote recebido de " + enderecos[i] + " Com intensidade: " + intensidade[i], 1);
                        i++;

                    }
                } catch (IOException ax) {
                    imprimir(ax.toString(), 5);

                }

            }
        } catch (IOException rv) {
        } finally {
            try {
                rx.close();
            } catch (IOException eex) {
                eex.printStackTrace();
            }
        }
        //Parte do código que procura o ch com menor distância.
        i = 0;

        while (i < QNTD_MAX_DE_CH) {

            imprimir(intensidade[i] + " " + enderecos[i], 1);

            if (intensidade[i] > melhor_intensidade) { //esse eh o atual campeão
                melhor_intensidade = intensidade[i];
                id_DoMeuCH = enderecos[i];
                meuCh_X = ch_x[i];
                meuCh_Y = ch_y[i];
            }
            i++;
        }
        if (id_DoMeuCH != null) { //se o valor do id_DoMeuCH for diferente de null ele achou um CH

            imprimir("Meu CH e o ID: " + id_DoMeuCH + " em: (" + meuCh_X + "," + meuCh_Y + ")", 4);

            //---------------------------------------------------
            Utils.sleep(5000); //devo esperar o meu CH estar pronto para receber uma mensagem minha

            //Enviando a resposta ao CH
            try {
                imprimir("estou enviando join ao CH", 1);

                RadiogramConnection conn = (RadiogramConnection) Connector.open("radiogram://" + id_DoMeuCH + ":123");
                Radiogram rfg = (Radiogram) conn.newDatagram(45);

                rfg.reset();
                rfg.writeChar(INSCRICAO_NO_CLUSTER);

                minhaBateria.gastarEnergia(meuRadio.calcularGastoParaEnviar(rfg.getLength() * 8, distanciaAoNo(meuCh_X, meuCh_Y) * distanciaAoNo(meuCh_X, meuCh_Y)));
                imprimir("Gastei " + meuRadio.calcularGastoParaEnviar(rfg.getLength() * 8, distanciaAoNo(meuCh_X, meuCh_Y) * distanciaAoNo(meuCh_X, meuCh_Y)) + " Para enviar conformacao ao CH", 3);

                conn.send(rfg);
                conn.close();
                //--------------------------------
            } catch (IOException ex) {
                imprimir("Error opening connections: " + ex, 5);
                ex.printStackTrace();
            }
        } else { //Se o valor do id_DoMeuCH for igual a null ele re-executa o procedimento
            imprimir("Nao achei um CH, estou re-executando o procedimento de procura....", 1);
        }


    }

    private void criarTDMA_CH() {
        long num_tdma = 0;
        RadiogramConnection conn = null;
        Radiogram rfg = null;

        Utils.sleep(5000);

        imprimir("Criando o TDMA", 1);

        for (int i = 0; i < quantidadeRealDeFolhas; i++) {
            num_tdma = i + 1;
            try {
                conn = (RadiogramConnection) Connector.open("radiogram://" + id_do_NoFolha[i] + ":123");
                rfg = (Radiogram) conn.newDatagram(45);

                rfg.reset();
                rfg.writeChar(ENVIO_TDMA);
                rfg.writeLong(num_tdma);

                minhaBateria.gastarEnergia(meuRadio.calcularGastoParaEnviar(rfg.getLength() * 8, rangeDoCH));
                imprimir("Gastei " + meuRadio.calcularGastoParaEnviar(rfg.getLength() * 8, rangeDoCH) + " Para enviar numero TDMA", 3);

                conn.send(rfg);
                conn.close();

            } catch (IOException cc) {
                cc.printStackTrace();
            }
        }

        imprimir("Entrando em Steady State...", 1);


    }

    private void esperarTDMA_Folha() {
        imprimir("Dormindo e esperando pelo CH me avisar quando devo sensoriar", 1);

        try {
            RadiogramConnection rx = (RadiogramConnection) Connector.open("radiogram://:123");
            Radiogram rdg = (Radiogram) rx.newDatagram(45);

            while (true) {
                rx.receive(rdg);

                minhaBateria.gastarEnergia(meuRadio.calcularGastoParaReceber(rdg.getLength() * 8));
                imprimir("Gastei " + meuRadio.calcularGastoParaReceber(rdg.getLength() * 8) + " Recebendo um broadcast de o TDMA", 3);

                if (rdg.readChar() == ENVIO_TDMA && rdg.getAddress().equals(id_DoMeuCH)) {
                    meuNumeroTDMA = rdg.readLong();
                    break;

                }
            }

            imprimir("Meu tdma schedule e: " + meuNumeroTDMA, 1);
            rx.close();
        } catch (IOException rr) {
            rr.printStackTrace();
        }
    }

    private void steady_State_CH() {

        imprimir("Dados Sensoriados pelo Cluster:", 3);

        long finalDoSteadyState = tempoDePermanenciaEmSteadyState;    //Tempo que o CH permanece em Steady State recebendo as informaçoes
        long t = System.currentTimeMillis();//Tempo base atual do sistema
        long end = t + finalDoSteadyState;  //Depois desse tempo ele parte para um novo round.

        int dadoLuminosidade = 0;
        int dadoTemperatura = 0;
        int dadoUmidade = 0;
        int dadoAAA = 0;
        int dadoBBB = 0;

        int dadoAgregadoLuminosidade = 0;
        int dadoAgregadoTemperatura = 0;
        int dadoAgregadoUmidade = 0;
        int dadoAgregadoAAA = 0;
        int dadoAgregadoBBB = 0;

        int qntdLuminosidade = 0;
        int qntdTemperatura = 0;
        int qntdUmidade = 0;
        int qntdAAA = 0;
        int qntdBBB = 0;

        double mediaDasLeiturasLuminosidade = 0;
        double mediaDasLeiturasTemperatura = 0;
        double mediaDasLeiturasUmidade = 0;
        double mediaDasLeiturasAAA = 0;
        double mediaDasLeiturasBBB = 0;

        int msgsRecebidas = 0;


        RadiogramConnection rx = null;
        Radiogram rdg = null;

        try {
            rx = (RadiogramConnection) Connector.open("radiogram://:123");
            rdg = (Radiogram) rx.newDatagram(45);
            rx.setTimeout(tempoBaseEmSteadyState_TDMA * QNTD_MAX_DE_NOS_FOLHA * 2); //espero 2 vezes o tempo total para me enviarem
        } catch (IOException oo) {
            oo.printStackTrace();
        }

        consultaAppsRodando(); //antes de entrar no loop do steadystate eu procuro ver qual aplicacao esta rodando
        while (true) {
            try {

                // while (System.currentTimeMillis() < end) { //Loop que fica no steady state.

                if (aplicacao1 || aplicacao2 ||aplicacao3) { //Se App1 ou App2 estiver rodando eu faço toda a logica. senao durmo.
                    rx.receive(rdg);

                    minhaBateria.gastarEnergia(meuRadio.calcularGastoParaReceber(rdg.getLength() * 8));
                    imprimir("Gastei " + meuRadio.calcularGastoParaReceber(rdg.getLength() * 8) + " Recebendo um dado sensoriado", 3);

                    if (rdg.readChar() == DADO_SENSORIADO) {

                        dadoLuminosidade = rdg.readInt();
                        dadoTemperatura = rdg.readInt();
                        dadoUmidade = rdg.readInt();
                        dadoAAA = rdg.readInt();
                        dadoBBB = rdg.readInt();

                        msgsRecebidas++;

                        imprimir("Dado de: " + rdg.getAddress() + " Luminosidade: " + dadoLuminosidade + " Temperatura: " + dadoTemperatura + " Umidade: " + dadoUmidade+ " AAA: " + dadoAAA+ " BBB: " + dadoBBB, 7);

                        if ((aplicacao1 || aplicacao2 || aplicacao4 || aplicacao7) && dadoLuminosidade != -999) {
                            //a condicao tem um && -999 pq eu posso receber um valor nao sincronizado e meu CH ler o -999 como valor de fato
                            dadoAgregadoLuminosidade = dadoAgregadoLuminosidade + dadoLuminosidade;
                            qntdLuminosidade++;
                            imprimir("Guardando valor Luminosidade", 7);
                        }
                        if ((aplicacao1||aplicacao3 || aplicacao5 || aplicacao8) && dadoTemperatura != -999) {
                            dadoAgregadoTemperatura = dadoAgregadoTemperatura + dadoTemperatura;
                            qntdTemperatura++;
                            imprimir("Guardando valor Temperatura", 7);
                        }
                        if ((aplicacao2||aplicacao3||aplicacao6||aplicacao9)&& dadoUmidade != -999) {
                            dadoAgregadoUmidade = dadoAgregadoUmidade + dadoUmidade;
                            qntdUmidade++;
                            imprimir("Guardando valor Umidade", 7);
                        }
                        if ((aplicacao4||aplicacao5||aplicacao6||aplicacao10)&& dadoAAA != -999) {
                            dadoAgregadoAAA = dadoAgregadoAAA + dadoAAA;
                            qntdAAA++;
                            imprimir("Guardando valor AAA", 7);
                        }
                        if ((aplicacao7||aplicacao8||aplicacao9||aplicacao10)&& dadoBBB != -999) {
                            dadoAgregadoBBB = dadoAgregadoBBB + dadoBBB;
                            qntdBBB++;
                            imprimir("Guardando valor BBB", 7);
                        }

                    }



                } else {
                    imprimir("Sem aplicações durante o steady state.. sleep mode", 7);
                    Utils.sleep(tempoBaseEmSteadyState_TDMA * quantidadeRealDeFolhas);

                }


            } catch (IOException oo) {
                imprimir(oo.toString(), 5);
            } finally {


                if (msgsRecebidas == quantidadeRealDeFolhas) {//Quando eu tiver recebido as informações referentes a cada nó MSGS RECEBIDAS == qntd real de folhas

                    if ((aplicacao1 || aplicacao2 || aplicacao4 || aplicacao7) && qntdLuminosidade != 0) {
                        //devo verificar se a qntd das leituras eh diferente de zero pois posso ter uma falta de sincronismo e dividir por zero
                        imprimir("Agregando Luminosidade", 7);
                        mediaDasLeiturasLuminosidade = dadoAgregadoLuminosidade / qntdLuminosidade;
                    } else {
                        imprimir("Nao agrego Luminosidade", 7);
                        mediaDasLeiturasLuminosidade = -999;
                    }

                    if ((aplicacao1||aplicacao3 || aplicacao5 || aplicacao8)  && qntdTemperatura != 0) {
                        imprimir("Agregando Temperatura", 7);
                        mediaDasLeiturasTemperatura = dadoAgregadoTemperatura / qntdTemperatura;
                    } else {
                        imprimir("Nao agrego Temperatura", 7);
                        mediaDasLeiturasTemperatura = -999;
                    }

                    if ((aplicacao2||aplicacao3||aplicacao6||aplicacao9) && qntdUmidade != 0) {
                        imprimir("Agregando Umidade", 7);
                        mediaDasLeiturasUmidade = dadoAgregadoUmidade / qntdUmidade;
                    } else {
                        imprimir("Nao agrego Umidade", 7);
                        mediaDasLeiturasUmidade = -999;
                    }

                    if ((aplicacao4||aplicacao5||aplicacao6||aplicacao10) && qntdAAA != 0) {
                        imprimir("Agregando AAA", 7);
                        mediaDasLeiturasAAA = dadoAgregadoAAA / qntdAAA;
                    } else {
                        imprimir("Nao agrego AAA", 7);
                        mediaDasLeiturasAAA = -999;
                    }

                    if ((aplicacao7||aplicacao8||aplicacao9||aplicacao10) && qntdBBB != 0) {
                        imprimir("Agregando BBB", 7);
                        mediaDasLeiturasBBB = dadoAgregadoBBB / qntdBBB;
                    } else {
                        imprimir("Nao agrego BBB", 7);
                        mediaDasLeiturasBBB = -999;
                    }

                    imprimir("Enviando a Base Station os dados agregados", 7);
                    enviarABaseStation(mediaDasLeiturasLuminosidade, mediaDasLeiturasTemperatura, mediaDasLeiturasUmidade, mediaDasLeiturasAAA, mediaDasLeiturasBBB);


                    dadoLuminosidade = 0;
                    dadoTemperatura = 0;
                    dadoUmidade = 0;

                    dadoAgregadoLuminosidade = 0;
                    dadoAgregadoTemperatura = 0;
                    dadoAgregadoUmidade = 0;

                    qntdLuminosidade = 0;
                    qntdTemperatura = 0;
                    qntdUmidade = 0;

                    mediaDasLeiturasLuminosidade = 0;
                    mediaDasLeiturasTemperatura = 0;
                    mediaDasLeiturasUmidade = 0;

                    msgsRecebidas = 0;

                }
                consultaAppsRodando(); //Devo reconsultar quais apps estao rodando
                if (!(System.currentTimeMillis() < end)) {
                    //Se o tempo passar eu dou break
                    break;
                }

            }
        }
        //Fechando a conexao e zerando
        try {
            rx.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        dadoLuminosidade = 0;
        dadoTemperatura = 0;
        dadoUmidade = 0;

        dadoAgregadoLuminosidade = 0;
        dadoAgregadoTemperatura = 0;
        dadoAgregadoUmidade = 0;

        qntdLuminosidade = 0;
        qntdTemperatura = 0;
        qntdUmidade = 0;

        mediaDasLeiturasLuminosidade = 0;
        mediaDasLeiturasTemperatura = 0;
        mediaDasLeiturasUmidade = 0;

        msgsRecebidas = 0;
    }

    private void steady_State_Folha() throws IOException {

        Utils.sleep(2000);

        long finalDoSteadyState = (long) (tempoDePermanenciaEmSteadyState * 1.1);    //Tempo que o CH permanece em Steady State recebendo as informaçoes
        long t = System.currentTimeMillis();//Tempo base atual do sistema
        long end = t + finalDoSteadyState;  //Depois desse tempo ele parte para um novo round.

        long base = tempoBaseEmSteadyState_TDMA; //base de tempo que cada nó deve começar a transmitir.
        long inicio = base * meuNumeroTDMA;      //dita qnto tempo o nó devera esperar para começar o procedimento
        long retorno = base * QNTD_MAX_DE_NOS_FOLHA; //dita qnto tempo o nó devera voltar a executar o procedimento

        RadiogramConnection conn = (RadiogramConnection) Connector.open("radiogram://" + id_DoMeuCH + ":123");
        Radiogram rfg = (Radiogram) conn.newDatagram(45);


        Utils.sleep(inicio);
        consultaAppsRodando();//antes de entrar no loop do steadystate eu procuro ver qual aplicacao esta rodando
        while (System.currentTimeMillis() < end) {

            if (aplicacao1 || aplicacao2||aplicacao3) { //se estiver rodando a aplicacao 1 ou 2 ele envia o pacote com leitura.
                rfg.reset();
                rfg.writeChar(DADO_SENSORIADO);

                rfg.writeInt(sensoriarLuminosidade());
                rfg.writeInt(sensoriarTemperatura());
                rfg.writeInt(sensoriarUmidade());
                rfg.writeInt(sensoriarAAA());
                rfg.writeInt(sensoriarBBB());

                minhaBateria.gastarEnergia(meuRadio.calcularGastoParaEnviar(rfg.getLength() * 8, distanciaAoNo(meuCh_X, meuCh_Y) * distanciaAoNo(meuCh_X, meuCh_Y)));
                imprimir("Gastei " + meuRadio.calcularGastoParaEnviar(rfg.getLength() * 8, distanciaAoNo(meuCh_X, meuCh_Y) * distanciaAoNo(meuCh_X, meuCh_Y)) + " Para enviar dado sensoriado", 3);
                imprimir("Enviando pacote com leituras das aplicacoes ", 7);
                conn.send(rfg);
            } else { //se nao tiver rodando nenhuma aplicacao ele simplesmente nao faz nada.
                imprimir("Nao ha aplicacoes rodando, sleep mode " + aplicacao1 + " " + aplicacao2, 7);
            }


            Utils.sleep(retorno);
            consultaAppsRodando();//revejo quais apps estao rodando
        }
        imprimir("Fim do SteadyState da folha", 3);
        conn.close();



    }

    private void consultaAppsRodando() {

        aplicacao1 = escutarApps.app1EstaRodando();

        aplicacao2 = escutarApps.app2EstaRodando();

        imprimir("apps: " + aplicacao1 + ":" + aplicacao2, 7);

    }

    private int sensoriarTemperatura() { //Retorna o valor entre -40 até 60 graus celcius
        if (aplicacao1||aplicacao3 || aplicacao5 || aplicacao8) {
            imprimir("lendo temperatura", 7);
            return rand.nextInt(101) - 40; // temperatura em celcius
        } else {
            imprimir("nao lendo temperatura", 7);
            return -999; //retornando um valor que eu considero ser nulo
        }
    }

    private int sensoriarLuminosidade() {//Retorna um valor da leitura de 0 - 750
        if (aplicacao1 || aplicacao2 || aplicacao4 || aplicacao7) {
            imprimir("lendo luminosidade", 7);
            return rand.nextInt(751); //luminosidade
        } else {
            imprimir("nao lendo luminosidade", 7);
            return -999;//retornando um valor que eu considero ser nulo
        }

    }

    private int sensoriarUmidade() { //Retorna o valor entre 0% a 100% de umidade relativa do ar
        if (aplicacao2||aplicacao3||aplicacao6||aplicacao9) {
            imprimir("lendo umidade", 7);
            return rand.nextInt(101); //umidade relativa do ar
        } else {
            imprimir("nao lendo umidade", 7);
            return -999;//retornando um valor que eu considero ser nulo
        }
    }

    private int sensoriarAAA() { //Retorna o valor entre 0% a 100% de umidade relativa do ar
        if (aplicacao4||aplicacao5||aplicacao6||aplicacao10) {// colocar depois as aplicacoes
            imprimir("lendo AAA", 7);
            return rand.nextInt(99); //aspecto aaa
        } else {
            imprimir("nao lendo AAA", 7);
            return -999;//retornando um valor que eu considero ser nulo
        }
    }

    private int sensoriarBBB() { //Retorna o valor entre 0% a 100% de umidade relativa do ar
        if (aplicacao7||aplicacao8||aplicacao9||aplicacao10) {// colocar depois as aplicacoes
            imprimir("lendo BBB", 7);
            return rand.nextInt(88); //aspecto bbb
        } else {
            imprimir("nao lendo BBB", 7);
            return -999;//retornando um valor que eu considero ser nulo
        }
    }
    // private void sensoriarEnviar(boolean app1, boolean app2){
    //App1 = temperatura , luminosidade;
    //App2 = umidade , luminosidade

    // }
    private void limparVariaveisTemporarias() {
        id_DoMeuCH = null;
        for (int i = 0; i < QNTD_MAX_DE_NOS_FOLHA; i++) { //VARIAVEL MAIS IMPORTANTE QUE DEVE SER LIMPA
            id_do_NoFolha[i] = null;                          //POIS GUARDA OS MEUS NÓS FOLHA
        }                                                 //E NAO DEVO TER NO FOLHA DA ITERACAO ANTERIOR
        quantidadeRealDeFolhas = 0;
        meuNumeroTDMA = 999999;
        meuCh_X = 0;
        meuCh_Y = 0;

    }

    private void definePosicaoNoCampo() {
        String finalEndereco = IEEEAddress.toDottedHex(myAddress).substring(17);
        int finalID = Integer.parseInt(finalEndereco);

        switch(finalID){
            case 0:
		coord_X = 8;
		coord_Y = 28;
		break;
	case 1:
		coord_X = 46;
		coord_Y = 85;
		break;
	case 2:
		coord_X = 50;
		coord_Y = 10;
		break;
	case 3:
		coord_X = 81;
		coord_Y = 37;
		break;
	case 4:
		coord_X = 49;
		coord_Y = 85;
		break;
	case 5:
		coord_X = 90;
		coord_Y = 14;
		break;
	case 6:
		coord_X = 10;
		coord_Y = 90;
		break;
	case 7:
		coord_X = 33;
		coord_Y = 90;
		break;
	case 8:
		coord_X = 97;
		coord_Y = 84;
		break;
	case 9:
		coord_X = 56;
		coord_Y = 78;
		break;
	case 10:
		coord_X = 76;
		coord_Y = 22;
		break;
	case 11:
		coord_X = 60;
		coord_Y = 82;
		break;
	case 12:
		coord_X = 49;
		coord_Y = 32;
		break;
	case 13:
		coord_X = 6;
		coord_Y = 84;
		break;
	case 14:
		coord_X = 16;
		coord_Y = 81;
		break;
	case 15:
		coord_X = 82;
		coord_Y = 10;
		break;
	case 16:
		coord_X = 94;
		coord_Y = 64;
		break;
	case 17:
		coord_X = 67;
		coord_Y = 4;
		break;
	case 18:
		coord_X = 4;
		coord_Y = 88;
		break;
	case 19:
		coord_X = 29;
		coord_Y = 69;
		break;
	case 20:
		coord_X = 74;
		coord_Y = 62;
		break;
	case 21:
		coord_X = 20;
		coord_Y = 62;
		break;
	case 22:
		coord_X = 41;
		coord_Y = 77;
		break;
	case 23:
		coord_X = 62;
		coord_Y = 52;
		break;
	case 24:
		coord_X = 72;
		coord_Y = 48;
		break;
	case 25:
		coord_X = 75;
		coord_Y = 77;
		break;
	case 26:
		coord_X = 54;
		coord_Y = 77;
		break;
	case 27:
		coord_X = 57;
		coord_Y = 97;
		break;
	case 28:
		coord_X = 80;
		coord_Y = 78;
		break;
	case 29:
		coord_X = 45;
		coord_Y = 42;
		break;
	case 30:
		coord_X = 48;
		coord_Y = 91;
		break;
	case 31:
		coord_X = 87;
		coord_Y = 77;
		break;
	case 32:
		coord_X = 68;
		coord_Y = 9;
		break;
	case 33:
		coord_X = 6;
		coord_Y = 67;
		break;
	case 34:
		coord_X = 48;
		coord_Y = 96;
		break;
	case 35:
		coord_X = 64;
		coord_Y = 26;
		break;
	case 36:
		coord_X = 33;
		coord_Y = 73;
		break;
	case 37:
		coord_X = 98;
		coord_Y = 78;
		break;
	case 38:
		coord_X = 6;
		coord_Y = 15;
		break;
	case 39:
		coord_X = 3;
		coord_Y = 54;
		break;
	case 40:
		coord_X = 19;
		coord_Y = 57;
		break;
	case 41:
		coord_X = 22;
		coord_Y = 46;
		break;
	case 42:
		coord_X = 24;
		coord_Y = 11;
		break;
	case 43:
		coord_X = 6;
		coord_Y = 67;
		break;
	case 44:
		coord_X = 82;
		coord_Y = 69;
		break;
	case 45:
		coord_X = 53;
		coord_Y = 14;
		break;
	case 46:
		coord_X = 50;
		coord_Y = 90;
		break;
	case 47:
		coord_X = 91;
		coord_Y = 15;
		break;
	case 48:
		coord_X = 35;
		coord_Y = 74;
		break;
	case 49:
		coord_X = 86;
		coord_Y = 46;
		break;
	case 50:
		coord_X = 74;
		coord_Y = 44;
		break;


        }

      //  coord_X = rand.nextInt(MAX_VALOR_X + 1); //exclusivo no valor superior. logo para um campo de 100 metros, tenho que dar valor max 101
     //   coord_Y = rand.nextInt(MAX_VALOR_Y + 1); //exclusivo no valor superior. logo para um campo de 100 metros, tenho que dar valor max 101
        imprimir("Sensor " + IEEEAddress.toDottedHex(myAddress) + " na posicao (" + coord_X + "," + coord_Y + ")", 4);

    }

    private double distanciaAoNo(double Xb, double Yb) { //calcula distancia ao sensor ao ponto  (Xb,Yb)
        double Xa = coord_X;
        double Ya = coord_Y;

        double x2 = (Xa - Xb) * (Xa - Xb);
        double y2 = (Ya - Yb) * (Ya - Yb);

        return Math.sqrt(x2 + y2);

    }

    private void enviarABaseStation(double mediaDasLeiturasLuminosidade, double mediaDasLeiturasTemperatura, double mediaDasLeiturasUmidade, double mediaDasLeiturasAAA,double mediaDasLeiturasBBB) {
        double dist = 0;
        try {
            //  meuRadio.calcularGastoParaEnviar(64, );
            RadiogramConnection conn = (RadiogramConnection) Connector.open("radiogram://broadcast:123"); //Base station fica na porta 1234
            Radiogram rfg = (Radiogram) conn.newDatagram(45);


            rfg.reset();
            rfg.writeChar(ENVIO_BS);
            rfg.writeDouble(mediaDasLeiturasLuminosidade);
            rfg.writeDouble(mediaDasLeiturasTemperatura);
            rfg.writeDouble(mediaDasLeiturasUmidade);
            rfg.writeDouble(mediaDasLeiturasAAA);
            rfg.writeDouble(mediaDasLeiturasBBB);

            dist = distanciaAoNo(BS_Coord_X, BS_Coord_Y);

            minhaBateria.gastarEnergia(meuRadio.calcularGastoParaEnviar(rfg.getLength() * 8, dist * dist));

            imprimir("Gastei " + meuRadio.calcularGastoParaEnviar(rfg.getLength() * 8, dist * dist) + " Para enviar dados agregados a BS", 3);

            conn.send(rfg);
            conn.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void imprimir(String str, int tipo) {
        switch (tipo) {
            case 1://controle das mensagens de set-up
               // System.out.println(str);
                break;
            case 2://controle das mensagens do steady state
               // System.out.println(str);
                break;
            case 3://controle do gasto de energia
                leds.setOff();
                leds.setOn();
                //     System.out.println(str);
                break;
            case 4: //controle da localizacao dos nós e formacao dos clusters
                //      System.out.println(str);
                break;
            case 5://controle das mensagens de erro
                //        System.out.println(str);
                break;
            case 6: //Output da formação dos clusters, para criar a planilha excel
                //       System.out.println(str);
                break;
            case 7: //Output do funcionamento do SHARED LEACH
                // System.out.println(str);
                break;
            case 8: //Output do gasto energético para construir um decaimento por tempo
                System.out.println(":" + IEEEAddress.toDottedHex(myAddress) + ":" + (System.currentTimeMillis() - startTime) + ":" + round + ":" + souCH + ":" + coord_X + ":" + coord_Y + ":" + meuCh_X + ":" + meuCh_Y + ":" + str+ ":" + aplicacao1 + ":"+ aplicacao2+ ":" + aplicacao3);
                break;
        }
    }

    private void esperarStart() {


        try {
            RadiogramConnection kks = (RadiogramConnection) Connector.open("radiogram://:123");
            Radiogram xcv = (Radiogram) kks.newDatagram(45);
            while (true) {
                kks.receive(xcv);

                if (xcv.readChar() == START) {
                    startTime = System.currentTimeMillis();
                    break;
                }

            }
            kks.close();
        } catch (IOException cew) {
        }

    }
}

class ApplicationsListener extends Thread {

    private boolean app1 = false;
    private boolean app2 = false;

    public void run() {
        RadiogramConnection tx = null;
        Radiogram xdg;

        try {
            tx = (RadiogramConnection) Connector.open("radiogram://:124"); //porta do envio do app switch
            xdg = (Radiogram) tx.newDatagram(45);

            while (true) {

                tx.receive(xdg);

                if (xdg.readChar() == 'V') {
                    app1 = xdg.readBoolean();
                    app2 = xdg.readBoolean();
                }
            }

        } catch (IOException yolo) {
            yolo.printStackTrace();
        }

    }

    boolean app1EstaRodando() {
        return app1;
    }

    boolean app2EstaRodando() {
        return app2;
    }
}

