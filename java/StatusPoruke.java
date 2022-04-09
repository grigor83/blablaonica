package com.example.nova;

import java.io.Serializable;

public class StatusPoruke implements Serializable {
    String id;
    int status;

    public StatusPoruke(String id){
        this.id=id;
        status=0;
    }
}
