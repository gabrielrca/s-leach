/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunspotworld;

/**
 *
 * @author labnet
 */
public class bateriaPrimitiva {
static private double BateriaInicial = 11050; //joules
    static private double CargaAtual = BateriaInicial;

    double getCargaInicial(){
        return BateriaInicial;
    }
    double getCargaAtual(){
        return CargaAtual;
    }

    void gastarEnergia(double energiaGasta){
        CargaAtual = CargaAtual - energiaGasta;
    }
}
