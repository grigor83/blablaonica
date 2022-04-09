package com.example.nova;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Iterator;

public class GlavnaAktivnost extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private MojaAplikacija app;
    private ListView spisakKontakata;
    private EditText ukucajIme;
    private ImageButton lupa, podesavanja;
    private Intent i;
    private InputMethodManager imm;
    private Iterator<EkranKontakta.Kontakt> iter;
    private RelativeLayout.LayoutParams params;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glavna_aktivnost);
        app=(MojaAplikacija)getApplication();

        //ako je aktivnost pokrenuta samo da bi se proslijedila poruka
        i=getIntent();
        if (i!=null){
            String poruka= i.getStringExtra("proslijedi poruku");
            if (poruka!=null){
                Log.i("aktivnost je pokrenuta", " zbog prosljedjivanja poruke");
                pokreniAktivnostZbogProsljedjivanjaPoruke(poruka);
                return;
            }
        }
        //pokrenuce se aktivnost postavki u slucaju da je ispunjen jedan od uslova
        if (app.ime==null || app.broj==null || app.korijenskiFolderString==null){
            i = new Intent(this, Postavke.class);
            startActivity(i);
        }

        spisakKontakata=findViewById(R.id.spisakKontakata);
        spisakKontakata.setAdapter(app.mojAdapter);
        spisakKontakata.setOnItemClickListener(this);
        lupa=findViewById(R.id.lupa);
        podesavanja=findViewById(R.id.podesavanja);
        ukucajIme=findViewById(R.id.ukucajIme);
        ukucajIme.setVisibility(View.INVISIBLE);
        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        params= (RelativeLayout.LayoutParams) lupa.getLayoutParams();
    }

    public void prikaziPodesavanja(View v){
        Intent novi=new Intent(getApplicationContext(), Settings.class);
        startActivity(novi);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        EkranKontakta.Kontakt k= (EkranKontakta.Kontakt) spisakKontakata.getItemAtPosition(position);

        i=new Intent(getApplicationContext(), EkranKontakta.class);
        i.putExtra("ime", k.ime);
        startActivity(i);
    }

    public void pokreniAktivnostZbogProsljedjivanjaPoruke(final String poruka){
        spisakKontakata=findViewById(R.id.spisakKontakata);
        spisakKontakata.setAdapter(app.mojAdapter);
        spisakKontakata.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final EkranKontakta.Kontakt k= (EkranKontakta.Kontakt) spisakKontakata.getItemAtPosition(position);
                String datumProslijedjenePoruke=poruka.split(app.TAG)[1].split(" ")[0].trim();

                if (!k.datumPoruke.equals(datumProslijedjenePoruke)){
                    if (!k.aktivan)
                        k.sadrzajPrepiske.add("datum"+app.TAG+datumProslijedjenePoruke);
                    k.najnovijePoruke.add("datum"+app.TAG+datumProslijedjenePoruke);
                    k.datumZaSnimanje=datumProslijedjenePoruke.trim();
                    k.datumPoruke=datumProslijedjenePoruke;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String IDporuke=k.broj+"_"+app.noviIDporuke();
                        //Moram da kreiram posebnu poruku za adapter, koja ce da prikaze samo sate i minute
                        final String porukaZaAdapter ="poruka"+app.TAG+app.ime+app.TAG+ poruka+app.TAG+IDporuke;
                        final StatusPoruke statusPoruke=new StatusPoruke(String.valueOf(IDporuke));

                        synchronized (k.statusPoruka){
                            k.statusPoruka.add(statusPoruke);
                            if (app.posaljiPorukuServeru(IDporuke+app.TAG+poruka))
                                statusPoruke.status=50;
                            else
                                synchronized (app.neposlatePoruke){
                                    app.neposlatePoruke.add(IDporuke+app.TAG+poruka);
                                    app.snimiNeposlatePoruke(IDporuke+app.TAG+poruka);
                                }
                        }
                        k.snimiStatus(statusPoruke);

                        k.sadrzajPrepiske.add(porukaZaAdapter);
                        k.najnovijePoruke.add(porukaZaAdapter);
                        app.snimiNajnovijePoruke(k);
                        if (k.aktivan){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    k.ekran.mojAdapter2.add(porukaZaAdapter);
                                    k.ekran.mojAdapter2.notifyDataSetChanged();
                                }
                            });
                        }
                        synchronized (app.spisakKontakata){
                            if (app.spisakKontakata.remove(k))
                                app.spisakKontakata.addFirst(k);
                        }
                    }
                }).start();

                finish();
            }
        });
    }

    @Override
    public void onBackPressed(){
        if (ukucajIme.getVisibility()==View.VISIBLE){
            lupa.setClickable(true);
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            podesavanja.setVisibility(View.VISIBLE);
            ukucajIme.setVisibility(View.INVISIBLE);
            ukucajIme.getText().clear();
            spisakKontakata.setAdapter(app.mojAdapter);
            return;
        }
        moveTaskToBack(true);
    }

    public void pronadjiKontakt(View view) {
        lupa.setClickable(false);
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        podesavanja.setVisibility(View.INVISIBLE);
        ukucajIme.setVisibility(View.VISIBLE);
        ukucajIme.requestFocus();
        imm.showSoftInput(ukucajIme, 0);
        pretrazuj();
    }

    private void pretrazuj(){
        ukucajIme.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length()>0)
                    synchronized (app.spisakKontakata){
                        iter=app.spisakKontakata.iterator();
                        app.privremeniSpisakKontakata.clear();

                        while(iter.hasNext()){
                            EkranKontakta.Kontakt kontakt=iter.next();
                            if (kontakt.ime.toLowerCase().startsWith(s.toString()))
                                app.privremeniSpisakKontakata.add(kontakt);
                        }
                        spisakKontakata.setAdapter(app.mojAdapterPrivremeni);
                    }
                else
                    spisakKontakata.setAdapter(app.mojAdapter);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
