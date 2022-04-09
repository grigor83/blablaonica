package com.example.nova;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DetaljniKontakt extends AppCompatActivity {
    MojaAplikacija app;
    private ImageView slika;
    private TextView ime, broj, inicijali;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detaljni_kontakt);

        app=(MojaAplikacija)getApplication();
        slika=findViewById(R.id.slikaDetaljnogKontakta);
        ime=findViewById(R.id.imeDetaljnogKontakta);
        broj=findViewById(R.id.brojDetaljnogKontakta);
        inicijali=findViewById(R.id.inicijali);

        Intent j=getIntent();
        if (j!=null){
            String brojKontakta= j.getStringExtra("broj");
            if (brojKontakta!=null){
                for (EkranKontakta.Kontakt kontakt : app.spisakKontakata)
                    if (kontakt.broj.equals(brojKontakta)){
                        podesiDetaljanKontakt(kontakt);
                        break;
                    }
            }
            else{
                if (app.fotografija!=null){
                    slika.setImageBitmap(app.fotografija);
                    inicijali.setVisibility(View.INVISIBLE);
                }
                else{
                    String[] s= app.ime.split(" ");
                    String prvaSlova="";
                    for (int k=0; k<s.length; k++)
                        prvaSlova=prvaSlova+s[k].charAt(0);
                    inicijali.setText(prvaSlova.toUpperCase());
                    inicijali.setVisibility(View.VISIBLE);
                    slika.setVisibility(View.INVISIBLE);
                    ime.setVisibility(View.GONE);
                }
                ime.setText(app.ime);
                broj.setText(app.broj);
            }
        }
    }

    public void podesiDetaljanKontakt(EkranKontakta.Kontakt kontakt){
        if (kontakt.slika!=null){
            slika.setImageBitmap(kontakt.slika);
            inicijali.setVisibility(View.INVISIBLE);
        }
        else{
            String[] s= kontakt.ime.split(" ");
            String prvaSlova="";
            for (int i=0; i<s.length; i++)
                prvaSlova=prvaSlova+s[i].charAt(0);
            inicijali.setText(prvaSlova.toUpperCase());
            inicijali.setVisibility(View.VISIBLE);
            slika.setVisibility(View.INVISIBLE);
            ime.setVisibility(View.GONE);
        }

        ime.setText(kontakt.ime);
        broj.setText(kontakt.broj);
    }

}

