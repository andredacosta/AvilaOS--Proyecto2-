/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.avilaos.structures;

/**
 *
 * @author Elohym Casais
 * @param <T>
 */

public class ListaEnlazada<T> {
    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;
    
    public void agregar(T dato){
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null){
            cabeza = nuevo;
            cola = nuevo;
        }else{
            cola.setSiguiente(nuevo);
            cola = nuevo;
        }
        tamano++;
    }
    
    public boolean eliminar(T dato){
        if (cabeza == null || dato == null){
            return false;
        }
        if(cabeza.getDato().equals(dato)){
            cabeza = cabeza.getSiguiente();
            if (cabeza == null){
                cola = null;
            }
            tamano--;
            return true;
        }
        Nodo<T> anterior = cabeza;
        Nodo<T> actual = cabeza.getSiguiente();
        while (actual != null){
            if (actual.getDato().equals(dato)){
                anterior.setSiguiente(actual.getSiguiente());
                if (actual == cola){
                    cola = anterior;
                }
                tamano--;
                return true;
            }
            anterior = actual;
            actual = actual.getSiguiente();
        }
        return false;
    }
    
    public T obtener(int indice){
        if (indice < 0 || indice>= tamano){
            throw new IndexOutOfBoundsException("Indice invalido: " + indice);
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i <indice; i++){
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }
    
    public Nodo<T> getCabeza(){
        return cabeza;
    }
    
    public int getTamano(){
        return tamano;
    }
    
    public boolean estaVacia(){
        cabeza = null;
        cola = null;
        tamano = 0;
        return false;
    }

    public int tamano() {
        return tamano;
    }
    
}
