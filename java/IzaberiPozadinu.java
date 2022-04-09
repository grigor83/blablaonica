package com.example.nova;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class IzaberiPozadinu extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.izaberi_pozadinu);
    }

    public void izaberi(View v){
        String pozadina=null;
        SharedPreferences podesavanja;
        podesavanja = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor= podesavanja.edit();

        if (v.getId()==R.id.zvjezdanoNebo)
            pozadina="zvjezdanoNebo";
        if (v.getId()==R.id.osnovna)
            pozadina="osnovna";
        if (v.getId()==R.id.zelena)
            pozadina="zelena";

        if (pozadina!=null){
            editor.putString("pozadina",pozadina);
            editor.apply();
            finish();
        }
    }
}
