package com.example.nova;

import java.io.Serializable;
import java.util.LinkedList;

public class Poruka implements Serializable {
    SadrzajPrepiske poruke;
    String datum;
    int brojNeprocitanihPoruka;
    LinkedList<StatusPoruke> statusPoruka;
    LinkedList<String> neposlatePoruke;
    byte[] slika;

    //Ogranicice se maksimalna kolicina snimljenih poruka na telefonu na 1 000
    public Poruka(){
        poruke=new SadrzajPrepiske(1000);
        statusPoruka=new LinkedList<>();
        neposlatePoruke=new LinkedList<>();
    }
}
