/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunspotworld;

/**
 *
 * @author labnet
 */
public class firstOrderRadio {
    static private double CONST_Eelec = 50 * 0.000000001;   //50  nJ/bit
   static private double CONST_Eamp = 100 * 0.000000000001;//100 pJ/bit/m²

   double calcularGastoParaReceber(int k){ //Calcula gasto para receber k-bits de mensagem
        return CONST_Eelec * k;
   }

   double calcularGastoParaEnviar(int k, double a){ //a = area de um cluster

        return (CONST_Eelec * k) + (CONST_Eamp * k * a);//Calcula gasto para enviar k-bits de mensagem em uma area a
   }

}
