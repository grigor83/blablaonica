package com.example.nova;

import java.util.LinkedList;

public class SadrzajPrepiske extends LinkedList<String> {
    final int limit;

    public SadrzajPrepiske (int limit){
        this.limit=limit;
    }

    @Override
    public boolean add(String s) {
        if (this.size()>limit)
            this.remove(0);
        return super.add(s);
    }
}

