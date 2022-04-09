package com.example.nova;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.documentfile.provider.DocumentFile;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class MojAdapter2 extends ArrayAdapter<String> implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    private final int TIP_1=0, TIP_2=1, TIP_3=2,TIP_4=3;
    private String danasnjiDatum;
    private Date date;
    private SimpleDateFormat formatter;
    Context context;
    MojaAplikacija app;
    EkranKontakta.Kontakt aktivniKontakt;
    LinkedList<String> privremenaPrepiska;
    LayoutInflater inflater;
    SwipeRefreshLayout osvjezivac;
    ListView lista;
    private MojAdapter2 mojAdapter2;
    String akronimKorisnika, akronimPosiljaoca;

    public MojAdapter2 (Context context, EkranKontakta.Kontakt aktivniKontakt, LinkedList<String> privremenaPrepiska){
        super(context,0,privremenaPrepiska);
        this.context=context;
        inflater = LayoutInflater.from(context);
        osvjezivac=((EkranKontakta)context).findViewById(R.id.osvjezivac);
        osvjezivac.setOnRefreshListener(this);
        lista=((EkranKontakta)context).findViewById(R.id.prikazPrepiske);
        app=aktivniKontakt.app;
        this.aktivniKontakt=aktivniKontakt;
        this.privremenaPrepiska=privremenaPrepiska;
        formatter = new SimpleDateFormat("dd-MM-yyyy");
        mojAdapter2=this;
        String[] s= mojAdapter2.app.ime.split(" ");
        String prvaSlova="";
        for (int i=0; i<s.length; i++)
            prvaSlova=prvaSlova+s[i].charAt(0);
        akronimKorisnika=prvaSlova.toUpperCase();
        s=aktivniKontakt.ime.split(" ");
        prvaSlova="";
        for (int i=0; i<s.length; i++)
            prvaSlova=prvaSlova+s[i].charAt(0);
        akronimPosiljaoca=prvaSlova.toUpperCase();
    }

    @NonNull
    @Override
    public int getViewTypeCount(){
        return 4;
    }

    public int getItemViewType(int position){
        String[]strings= getItem(position).split(app.TAG);

        if (strings[0].trim().equals("datum"))
            return TIP_1;
        if (strings[0].trim().equals("poruka"))
            return TIP_2;
        if (strings[0].split("/")[0].trim().equals("image") ||
                strings[0].split("/")[0].trim().equals("video"))
            return TIP_3;
        else
            return TIP_4;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        DatumHolder datumHolder;
        PorukaHolder porukaHolder;
        SlikaVideoHolder slikaVideoHolder;
        AudioHolder audioHolder;
        String cijelaPoruka= getItem(position);
        String[] rasclanjenaPoruka=cijelaPoruka.split(app.TAG);

        if (getItemViewType(position)==TIP_1){
            if (convertView==null){
                convertView = inflater.inflate(R.layout.redak_datuma, null);
                datumHolder=new DatumHolder(convertView);
                convertView.setTag(datumHolder);
            }
            date = new Date(); // this object contains the current date value
            danasnjiDatum=formatter.format(date);

            datumHolder=(DatumHolder) convertView.getTag();
            if (danasnjiDatum.equals(cijelaPoruka.split(app.TAG)[1]))
                datumHolder.datum.setText("Danas");
            else
                datumHolder.datum.setText(cijelaPoruka.split(app.TAG)[1]);

            return convertView;
        }

        if (getItemViewType(position)==TIP_2){
            if (convertView==null){
                convertView = inflater.inflate(R.layout.redak_poruke, null);
                porukaHolder = new PorukaHolder (convertView, context, mojAdapter2);
                convertView.setTag(porukaHolder);
            }
            porukaHolder = (PorukaHolder) convertView.getTag();
            String ime=rasclanjenaPoruka[1];
            String poruka=rasclanjenaPoruka[2].replaceAll("</br>","\n");
            porukaHolder.poruka.setText(poruka);
            String datum=rasclanjenaPoruka[3];
            porukaHolder.datum=datum;
            porukaHolder.vrijeme.setText(datum.split(" ")[1].trim());

            porukaHolder.slikaKorisnika.setVisibility(View.INVISIBLE);
            porukaHolder.slikaPosiljaoca.setVisibility(View.INVISIBLE);
            porukaHolder.kvacica1.setVisibility(View.INVISIBLE);
            porukaHolder.kvacica2.setVisibility(View.INVISIBLE);
            porukaHolder.rifres.setVisibility(View.GONE);
            porukaHolder.inicijali.setVisibility(View.INVISIBLE);
            porukaHolder.inicijaliPosiljaoca.setVisibility(View.INVISIBLE);

            RelativeLayout.LayoutParams p= (RelativeLayout.LayoutParams) porukaHolder.slikaPosiljaoca.getLayoutParams();
            p.topMargin=0;
            RelativeLayout.LayoutParams p1= (RelativeLayout.LayoutParams) porukaHolder.slikaKorisnika.getLayoutParams();
            p1.topMargin=0;
            RelativeLayout kontejnerPoruke= (RelativeLayout) porukaHolder.poruka.getParent();
            RelativeLayout.LayoutParams params= (RelativeLayout.LayoutParams) kontejnerPoruke.getLayoutParams();
            params.removeRule(RelativeLayout.RIGHT_OF);
            params.removeRule(RelativeLayout.LEFT_OF);
            params.setMarginStart(0);
            params.setMarginEnd(0);
            params.topMargin=0;

            String prethodniString;
            if (position-1<0)
                prethodniString=getItem(position);
            else
                prethodniString=getItem(position-1);

            if (ime.equals(aktivniKontakt.ime)){
                kontejnerPoruke.setBackground(ContextCompat.getDrawable(context, R.drawable.narandzasta_poruka));
                if (!prethodniString.split(app.TAG)[1].trim().equals(aktivniKontakt.ime)){
                    params.topMargin=25;
                    if (porukaHolder.postojiSlikaPosiljaoca){
                        porukaHolder.slikaPosiljaoca.setVisibility(View.VISIBLE);
                        porukaHolder.slikaPosiljaoca.setImageBitmap(mojAdapter2.aktivniKontakt.slika);
                    }
                    else{
                        porukaHolder.inicijaliPosiljaoca.setText(mojAdapter2.akronimPosiljaoca);
                        porukaHolder.inicijaliPosiljaoca.setVisibility(View.VISIBLE);
                    }
                    p.topMargin=25;
                }
                params.addRule(RelativeLayout.RIGHT_OF, R.id.slikaPosiljaoca);
                params.setMarginStart(10);
                params.setMarginEnd(50);
            }
            else{
                kontejnerPoruke.setBackground(ContextCompat.getDrawable(context, R.drawable.zelena_poruka));
                if (!prethodniString.split(app.TAG)[1].trim().equals(aktivniKontakt.app.ime)){
                    params.topMargin=25;
                    if (porukaHolder.postojiSlikaKorisnika){
                        porukaHolder.slikaKorisnika.setVisibility(View.VISIBLE);
                        porukaHolder.slikaKorisnika.setImageBitmap(mojAdapter2.app.fotografija);
                    }
                    else{
                        porukaHolder.inicijali.setText(mojAdapter2.akronimKorisnika);
                        porukaHolder.inicijali.setVisibility(View.VISIBLE);
                    }
                    p1.topMargin=25;
                }
                params.addRule(RelativeLayout.LEFT_OF, R.id.slikaKorisnika);
                params.setMarginStart(50);
                params.setMarginEnd(10);
                //sad treba da provjerim listu statusa poslatih poruka
                String id= rasclanjenaPoruka[rasclanjenaPoruka.length-1];
                porukaHolder.id=id;
                ispitajStatusPoruke(kontejnerPoruke, porukaHolder,id);
            }

            return convertView;
        }

        ///////////////slika i video/////////////////
        if (getItemViewType(position)==TIP_3){
            if (convertView==null){
                convertView = inflater.inflate(R.layout.slicica, null);
                slikaVideoHolder = new SlikaVideoHolder (convertView, context, mojAdapter2);
                convertView.setTag(slikaVideoHolder);
            }
            slikaVideoHolder = (SlikaVideoHolder) convertView.getTag();

            slikaVideoHolder.tip=rasclanjenaPoruka[0];
            String ime=rasclanjenaPoruka[1];
            slikaVideoHolder.uri=rasclanjenaPoruka[2];
            String datum=rasclanjenaPoruka[3];
            slikaVideoHolder.vrijeme.setText(datum.split(" ")[1].trim());
            slikaVideoHolder.staza=rasclanjenaPoruka[4];
            slikaVideoHolder.imeFajla=rasclanjenaPoruka[5];

            slikaVideoHolder.slikaPosiljaoca.setVisibility(View.INVISIBLE);
            slikaVideoHolder.slikaKorisnika.setVisibility(View.INVISIBLE);
            slikaVideoHolder.rotirajuca.setVisibility(View.INVISIBLE);
            slikaVideoHolder.progres.setVisibility(View.INVISIBLE);
            slikaVideoHolder.slicica.setColorFilter(null);
            slikaVideoHolder.kvacica1.setVisibility(View.INVISIBLE);
            slikaVideoHolder.kvacica2.setVisibility(View.INVISIBLE);
            slikaVideoHolder.inicijaliPosiljaoca.setVisibility(View.INVISIBLE);
            slikaVideoHolder.inicijali.setVisibility(View.INVISIBLE);
            if (slikaVideoHolder.tip.split("/")[0].equals("video"))
                slikaVideoHolder.plej.setVisibility(View.VISIBLE);
            else
                slikaVideoHolder.plej.setVisibility(View.INVISIBLE);

            RelativeLayout kontenejerSlike=(RelativeLayout) slikaVideoHolder.slicica.getParent();
            RelativeLayout.LayoutParams params= (RelativeLayout.LayoutParams) kontenejerSlike.getLayoutParams();
            params.removeRule(RelativeLayout.RIGHT_OF);
            params.removeRule(RelativeLayout.LEFT_OF);
            params.topMargin=0;
            RelativeLayout.LayoutParams p= (RelativeLayout.LayoutParams) slikaVideoHolder.slikaPosiljaoca.getLayoutParams();
            p.topMargin=0;
            RelativeLayout.LayoutParams p1= (RelativeLayout.LayoutParams) slikaVideoHolder.slikaKorisnika.getLayoutParams();
            p1.topMargin=0;

            String prethodniString;
            if (position-1<0)
                prethodniString=getItem(position);
            else
                prethodniString=getItem(position-1);

            if (ime.equals(aktivniKontakt.ime)){
                kontenejerSlike.setBackground(ContextCompat.getDrawable(context, R.drawable.narandzasti_slojeviti_prikaz));
                if (!prethodniString.split(app.TAG)[1].trim().equals(aktivniKontakt.ime)){
                    params.topMargin=30;
                    p.topMargin=30;

                    if (slikaVideoHolder.postojiSlikaPosiljaoca){
                        slikaVideoHolder.slikaPosiljaoca.setVisibility(View.VISIBLE);
                        slikaVideoHolder.slikaPosiljaoca.setImageBitmap(mojAdapter2.aktivniKontakt.slika);
                    }
                    else{
                        slikaVideoHolder.inicijaliPosiljaoca.setText(mojAdapter2.akronimPosiljaoca);
                        slikaVideoHolder.inicijaliPosiljaoca.setVisibility(View.VISIBLE);
                    }
                }
                params.addRule(RelativeLayout.RIGHT_OF, R.id.slikaPosiljaoca);
            }
            else{
                kontenejerSlike.setBackground(ContextCompat.getDrawable(context, R.drawable.zeleni_slojeviti_prikaz));
                if (!prethodniString.split(app.TAG)[1].trim().equals(aktivniKontakt.app.ime)){
                    params.topMargin=30;
                    p1.topMargin=30;
                    if (slikaVideoHolder.postojiSlikaKorisnika){
                        slikaVideoHolder.slikaKorisnika.setVisibility(View.VISIBLE);
                        slikaVideoHolder.slikaKorisnika.setImageBitmap(mojAdapter2.app.fotografija);
                    }
                    else{
                        slikaVideoHolder.inicijali.setText(mojAdapter2.akronimKorisnika);
                        slikaVideoHolder.inicijali.setVisibility(View.VISIBLE);
                    }
                }
                params.addRule(RelativeLayout.LEFT_OF, R.id.slikaKorisnika);
                //sad treba da provjerim listu statusa poslatih fajlova
                String id= rasclanjenaPoruka[rasclanjenaPoruka.length-1];
                slikaVideoHolder.id=id;
                ispitajStatusSlikeVidea(slikaVideoHolder,id);
            }
            slikaVideoHolder.ucitajBitmap(Uri.parse(rasclanjenaPoruka[4]));

            return convertView;
        }

        else {
            if (convertView==null){
                convertView = inflater.inflate(R.layout.audio_redak, null);
                audioHolder = new AudioHolder (convertView, context,mojAdapter2);
                convertView.setTag(audioHolder);
            }
            audioHolder = (AudioHolder) convertView.getTag();

            audioHolder.tip=rasclanjenaPoruka[0];
            String ime=rasclanjenaPoruka[1];
            audioHolder.uri=Uri.parse(rasclanjenaPoruka[2]);
            String datum=rasclanjenaPoruka[3];
            audioHolder.vrijeme.setText(datum.split(" ")[1].trim());
            audioHolder.imeFajla.setText(rasclanjenaPoruka[5]);

            audioHolder.slikaPosiljaoca.setVisibility(View.INVISIBLE);
            audioHolder.slikaKorisnika.setVisibility(View.INVISIBLE);
            audioHolder.rotirajuca.setVisibility(View.INVISIBLE);
            audioHolder.progres.setVisibility(View.INVISIBLE);
            audioHolder.kvacica1.setVisibility(View.INVISIBLE);
            audioHolder.kvacica2.setVisibility(View.INVISIBLE);
            audioHolder.inicijali.setVisibility(View.INVISIBLE);
            audioHolder.inicijaliPosiljaoca.setVisibility(View.INVISIBLE);

            if (audioHolder.tip.split("/")[0].equals("audio"))
                audioHolder.ikonaFajla.setBackground(ContextCompat.getDrawable(context, R.drawable.ic_radio));
            else
                audioHolder.ikonaFajla.setBackground(ContextCompat.getDrawable(context, R.drawable.ic_folder));

            RelativeLayout.LayoutParams params= (RelativeLayout.LayoutParams) audioHolder.kontejner.getLayoutParams();
            params.removeRule(RelativeLayout.RIGHT_OF);
            params.removeRule(RelativeLayout.LEFT_OF);
            params.topMargin=0;
            RelativeLayout.LayoutParams p= (RelativeLayout.LayoutParams) audioHolder.slikaPosiljaoca.getLayoutParams();
            p.topMargin=0;
            RelativeLayout.LayoutParams p1= (RelativeLayout.LayoutParams) audioHolder.slikaKorisnika.getLayoutParams();
            p1.topMargin=0;

            String prethodniString;
            if (position-1<0)
                prethodniString=getItem(position);
            else
                prethodniString=getItem(position-1);

            if (ime.equals(aktivniKontakt.ime)){
                audioHolder.kontejner.setBackground(ContextCompat.getDrawable(context, R.drawable.narandzasti_slojeviti_prikaz));
                if (!prethodniString.split(app.TAG)[1].trim().equals(aktivniKontakt.ime)){
                    params.topMargin=30;
                    p.topMargin=30;
                    if (audioHolder.postojiSlikaPosiljaoca){
                        audioHolder.slikaPosiljaoca.setVisibility(View.VISIBLE);
                        audioHolder.slikaPosiljaoca.setImageBitmap(mojAdapter2.aktivniKontakt.slika);
                    }
                    else{
                        audioHolder.inicijaliPosiljaoca.setText(mojAdapter2.akronimPosiljaoca);
                        audioHolder.inicijaliPosiljaoca.setVisibility(View.VISIBLE);
                    }
                }
                params.addRule(RelativeLayout.RIGHT_OF, R.id.slikaPosiljaoca);
            }
            else{
                audioHolder.kontejner.setBackground(ContextCompat.getDrawable(context, R.drawable.zeleni_slojeviti_prikaz));
                if (!prethodniString.split(app.TAG)[1].trim().equals(aktivniKontakt.app.ime)){
                    params.topMargin=30;
                    p1.topMargin=30;
                    if (audioHolder.postojiSlikaKorisnika){
                        audioHolder.slikaKorisnika.setVisibility(View.VISIBLE);
                        audioHolder.slikaKorisnika.setImageBitmap(mojAdapter2.app.fotografija);
                    }
                    else{
                        audioHolder.inicijali.setText(mojAdapter2.akronimKorisnika);
                        audioHolder.inicijali.setVisibility(View.VISIBLE);
                    }
                }
                params.addRule(RelativeLayout.LEFT_OF, R.id.slikaKorisnika);
                //sad treba da provjerim listu statusa poslatih fajlova
                String id= rasclanjenaPoruka[rasclanjenaPoruka.length-1];
                audioHolder.id=id;
                ispitajStatusFajla(audioHolder,id);
            }

            return convertView;
        }
    }

    @Override
    public void onRefresh() {
        synchronized (aktivniKontakt){
            int staraPozicija= ucitajJosPoruka();
            notifyDataSetChanged();
            lista.setSelection(staraPozicija-1);
            osvjezivac.setRefreshing(false);
        }
    }

    private int ucitajJosPoruka(){
        int i=0;
        ObjectInputStream ucitajObjekat=null;
        Poruka prepiska;
        File fajl;
        fajl=new File(app.folderKontakti,aktivniKontakt.ime+"_"+aktivniKontakt.broj+".txt");

        synchronized (aktivniKontakt){
            try {
                ucitajObjekat = new ObjectInputStream(new FileInputStream(fajl));
                prepiska = (Poruka) ucitajObjekat.readObject();
                ucitajObjekat.close();
                //trebam ucitati jos poruka samo ako je cijela prepiska koja postoji na disku veca od
                //trenutne prikazane prepiske
                int velicinaCijelePrepiske=prepiska.poruke.size();
                int velicinaTrenutnePrepiske=privremenaPrepiska.size();
                if (velicinaCijelePrepiske>velicinaTrenutnePrepiske){
                    int indeksPosljednjeUcitanePoruke=prepiska.poruke.size()-velicinaTrenutnePrepiske;
                    int pocetak=indeksPosljednjeUcitanePoruke-app.BAFER;
                    if (pocetak<0)
                        pocetak=0;
                    for (int j=pocetak; j<indeksPosljednjeUcitanePoruke;j++){
                        privremenaPrepiska.add(i,prepiska.poruke.get(j));
                        i++;
                    }
                }
            }
            catch (IOException |ClassNotFoundException e){
                e.printStackTrace();
            }
        }
        return i;
    }

    @Override
    public void onClick(View v) {
        if (aktivniKontakt.ekran.fragmentJePrikazan){
            aktivniKontakt.ekran.skloniFragment();
            return;
        }
        if (v.getId()==R.id.slikaKorisnika || v.getId()==R.id.inicijali){
            Intent i=new Intent(context.getApplicationContext(), DetaljniKontakt.class);
            context.startActivity(i);
            return;
        }
        if (v.getId()==R.id.slikaPosiljaoca || v.getId()==R.id.inicijaliPosiljaoca){
            Intent i=new Intent(context.getApplicationContext(), DetaljniKontakt.class);
            i.putExtra("broj", aktivniKontakt.broj);
            context.startActivity(i);
            return;
        }
    }

    private void ispitajStatusPoruke(RelativeLayout kontejner, PorukaHolder porukaHolder, String id){
        synchronized (aktivniKontakt){
            for (StatusPoruke s:aktivniKontakt.statusPoruka)
                if (s.id.equals(id)){
                    if (s.status==0){
                        kontejner.setBackground(ContextCompat.getDrawable(context, R.drawable.crvena_poruka));
                        porukaHolder.rifres.setVisibility(View.VISIBLE);
                        porukaHolder.statusPoruke=s;
                    }
                    if (s.status==50){
                        porukaHolder.kvacica1.setVisibility(View.VISIBLE);
                        porukaHolder.kvacica1.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.crna));
                    }
                    return;
                }
            //ako nema statusa te poruke u listi, znaci da je davno vec isporucena i
            //izbrisana iz liste, pa je zato nema. To samo znaci da treba prikazati kvacice
            porukaHolder.kvacica1.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.crna));
            porukaHolder.kvacica2.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.crna));
            porukaHolder.kvacica1.setVisibility(View.VISIBLE);
            porukaHolder.kvacica2.setVisibility(View.VISIBLE);
        }
    }

    private void ispitajStatusSlikeVidea(SlikaVideoHolder slikaVideoHolder, String id){
        synchronized (aktivniKontakt){
            for (StatusPoruke s:aktivniKontakt.statusPoruka)
                if (s.id.equals(id)){
                    if (s.status==-50){
                        slikaVideoHolder.slicica.setColorFilter(R.color.svijetloCrvena);
                        slikaVideoHolder.rotirajuca.setVisibility(View.VISIBLE);
                        slikaVideoHolder.plej.setColorFilter(R.color.svijetloCrvena);
                        return;
                    }
                    if (s.status==0){
                        slikaVideoHolder.progres.setVisibility(View.VISIBLE);
                        slikaVideoHolder.kvacica1.setVisibility(View.VISIBLE);
                        slikaVideoHolder.plej.setColorFilter(null);
                        return;
                    }
                    if (s.status==50){
                        slikaVideoHolder.progres.setVisibility(View.INVISIBLE);
                        slikaVideoHolder.kvacica1.setVisibility(View.VISIBLE);
                        slikaVideoHolder.kvacica1.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.svijetloZelena));
                        slikaVideoHolder.plej.setColorFilter(null);
                    }
                    return;
                }
            //ako nema statusa te poruke u listi, znaci da je davno vec isporucena i
            //izbrisana iz liste, pa je zato nema. To samo znaci da treba prikazati kvacice
            slikaVideoHolder.kvacica1.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.svijetloZelena));
            slikaVideoHolder.kvacica2.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.svijetloZelena));
            slikaVideoHolder.kvacica1.setVisibility(View.VISIBLE);
            slikaVideoHolder.kvacica2.setVisibility(View.VISIBLE);
        }
    }

    private void ispitajStatusFajla(AudioHolder audioHolder, String id){
        synchronized (aktivniKontakt){
            for (StatusPoruke s:aktivniKontakt.statusPoruka)
                if (s.id.equals(id)){
                    if (s.status==-50){
                        audioHolder.rotirajuca.setVisibility(View.VISIBLE);
                        return;
                    }
                    if (s.status==0){
                        audioHolder.progres.setVisibility(View.VISIBLE);
                        audioHolder.kvacica1.setVisibility(View.VISIBLE);
                        audioHolder.rotirajuca.setVisibility(View.INVISIBLE);
                        return;
                    }
                    if (s.status==50){
                        audioHolder.progres.setVisibility(View.INVISIBLE);
                        audioHolder.kvacica1.setVisibility(View.VISIBLE);
                        audioHolder.kvacica1.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.svijetloZelena));
                        audioHolder.rotirajuca.setVisibility(View.INVISIBLE);
                    }
                    return;
                }
            //ako nema statusa te poruke u listi, znaci da je davno vec isporucena i
            //izbrisana iz liste, pa je zato nema. To samo znaci da treba prikazati kvacice
            audioHolder.kvacica1.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.svijetloZelena));
            audioHolder.kvacica2.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.svijetloZelena));
            audioHolder.kvacica1.setVisibility(View.VISIBLE);
            audioHolder.kvacica2.setVisibility(View.VISIBLE);
        }
    }

    private static class DatumHolder {
        TextView datum;

        public DatumHolder(View view){
            datum=view.findViewById(R.id.datum);
        }
    }

    private static class PorukaHolder implements View.OnTouchListener, View.OnLongClickListener, View.OnClickListener {
        private TextView poruka,vrijeme, inicijali, inicijaliPosiljaoca;
        private ImageView slikaPosiljaoca, slikaKorisnika, kvacica1, kvacica2, rifres;
        private float siroviX=0, siroviY=0;
        private EkranKontakta ekran;
        private RelativeLayout k;
        private String id,datum;
        private MojAdapter2 adapter;
        private StatusPoruke statusPoruke;
        boolean postojiSlikaKorisnika, postojiSlikaPosiljaoca;

        public PorukaHolder(View view, Context context, MojAdapter2 mojAdapter2) {
            inicijali=view.findViewById(R.id.inicijali);
            inicijali.setOnClickListener(mojAdapter2);
            inicijaliPosiljaoca=view.findViewById(R.id.inicijaliPosiljaoca);
            inicijaliPosiljaoca.setOnClickListener(mojAdapter2);

            slikaKorisnika = view.findViewById(R.id.slikaKorisnika);
            if (mojAdapter2.app.fotografija!=null){
                postojiSlikaKorisnika=true;
            }
            else{
                postojiSlikaKorisnika=false;
            }
            slikaKorisnika.setOnClickListener(mojAdapter2);
            slikaPosiljaoca = view.findViewById(R.id.slikaPosiljaoca);
            if (mojAdapter2.aktivniKontakt.slika!=null){
                postojiSlikaPosiljaoca=true;
            }
            else{
                postojiSlikaPosiljaoca=false;
            }
            slikaPosiljaoca.setOnClickListener(mojAdapter2);
            poruka = view.findViewById(R.id.poruka);
            vrijeme=view.findViewById(R.id.vrijeme);
            ekran=(EkranKontakta)context;
            k=view.findViewById(R.id.kontejner_poruke);
            k.setOnLongClickListener(this);
            k.setOnTouchListener(this);
            kvacica1=view.findViewById(R.id.kvacica1);
            kvacica2=view.findViewById(R.id.kvacica2);
            rifres=view.findViewById(R.id.rifres);
            rifres.setOnClickListener(mojAdapter2);
            rifres.setOnClickListener(this);
            adapter=mojAdapter2;
        }

        @Override
        public boolean onLongClick(View v) {
            if (!ekran.fragmentJePrikazan){
                Date date = new Date(); // this object contains the current date value
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                ekran.prikaziFragment(siroviX,siroviY, poruka.getText().toString()+
                            ((MojaAplikacija)ekran.getApplication()).TAG+formatter.format(date));
            }
            else
                ekran.skloniFragment();
            return true;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    if (ekran.fragmentJePrikazan){
                        ekran.skloniFragment();
                        return true;
                    }

                    siroviX=event.getRawX();
                    siroviY=event.getRawY();
                    break;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (adapter.app.posaljiPorukuServeru(id+adapter.app.TAG+
                                                    poruka.getText()+adapter.app.TAG+datum))
                    {
                        synchronized (adapter.aktivniKontakt.statusPoruka){
                            if (statusPoruke==null || statusPoruke.status==50)
                                return;
                            statusPoruke.status=50;
                            adapter.aktivniKontakt.izmijeniStatus(statusPoruke);
                            adapter.aktivniKontakt.ekran.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                }
            }).start();
        }
    }

    private static class SlikaVideoHolder implements View.OnClickListener {
        ImageView slikaPosiljaoca, slikaKorisnika, rotirajuca, slicica, plej,
        kvacica1,kvacica2;
        TextView vrijeme, inicijali, inicijaliPosiljaoca;
        ProgressBar progres;
        Context context;
        EkranKontakta ekran;
        ContentResolver cr;
        Bitmap b;
        String uri, id, tip, staza, imeFajla;
        MojAdapter2 mojAdapter2;
        boolean postojiSlikaKorisnika, postojiSlikaPosiljaoca;

        public SlikaVideoHolder(View view, Context context, MojAdapter2 mojAdapter2){
            this.mojAdapter2=mojAdapter2;
            slikaKorisnika = view.findViewById(R.id.slikaKorisnika);
            if (mojAdapter2.app.fotografija!=null){
                postojiSlikaKorisnika=true;
            }
            else{
                postojiSlikaKorisnika=false;
            }
            slikaKorisnika.setOnClickListener(mojAdapter2);
            slikaPosiljaoca = view.findViewById(R.id.slikaPosiljaoca);
            if (mojAdapter2.aktivniKontakt.slika!=null){
                postojiSlikaPosiljaoca=true;
            }
            else{
                postojiSlikaPosiljaoca=false;
            }
            slikaPosiljaoca.setOnClickListener(mojAdapter2);
            rotirajuca=view.findViewById(R.id.rotirajuca);
            rotirajuca.setOnClickListener(this);
            slicica=view.findViewById(R.id.slicica);
            slicica.setOnClickListener(this);
            plej=view.findViewById(R.id.playDugme);
            vrijeme=view.findViewById(R.id.vrijeme);
            progres=view.findViewById(R.id.neodredjeniProgresBar);
            this.context=context;
            ekran=(EkranKontakta) context;
            cr=context.getContentResolver();
            kvacica1=view.findViewById(R.id.kvacica1);
            kvacica2=view.findViewById(R.id.kvacica2);
            inicijali=view.findViewById(R.id.inicijali);
            inicijaliPosiljaoca=view.findViewById(R.id.inicijaliPosiljaoca);
            inicijali.setOnClickListener(mojAdapter2);
            inicijaliPosiljaoca.setOnClickListener(mojAdapter2);
        }

        public void ucitajBitmap(Uri uriBitmapa) {
            if (tip.split("/")[0].equals("video")){
                File file = new File(uriBitmapa.toString());
                synchronized (file) {
                    try(FileInputStream fin = new FileInputStream(file);)
                    {
                        b = BitmapFactory.decodeStream(fin);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else{
                DocumentFile d= DocumentFile.fromSingleUri(context,uriBitmapa);
                if (!d.exists()){
                    slicica.setImageDrawable(null);
                    rotirajuca.setVisibility(View.INVISIBLE);
                    progres.setVisibility(View.INVISIBLE);
                    return;
                }

                InputStream fajl;
                BufferedInputStream buff;
                try {
                    fajl=cr.openInputStream(uriBitmapa);
                    buff=new BufferedInputStream(fajl);
                    b = BitmapFactory.decodeStream(buff);
                    buff.close();
                }
                catch (IOException e){
                    e.printStackTrace();
                    slicica.setImageDrawable(null);
                    return;
                }
            }

            RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), b);
            roundDrawable.setCornerRadius(18);
            slicica.setImageDrawable(roundDrawable);
        }

        @Override
        public void onClick(View v) {
            if (ekran.fragmentJePrikazan){
                ekran.skloniFragment();
                return;
            }

            if (v.getId()==rotirajuca.getId()){
                if (tip.split("/")[0].equals("image"))
                    ekran.posaljiSliku(imeFajla, tip, Uri.parse(uri));
                else{
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Date date = new Date();
                            SimpleDateFormat formater = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                            String poruka=tip+ekran.app.TAG+ekran.app.ime+ekran.app.TAG+Uri.parse(uri)+ekran.app.TAG+formater.format(date)+ekran.app.TAG+staza+
                                    ekran.app.TAG+imeFajla;
                            ekran.dodajFajlUPrepiske(poruka, Uri.parse(uri), true);
                        }
                    }).start();
                }
                return;
            }

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse(uri), tip);
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ekran.startActivity(i);
        }
    }

    private static class AudioHolder implements View.OnClickListener {
        TextView imeFajla, vrijeme, inicijali, inicijaliPosiljaoca;
        ImageView slikaPosiljaoca, slikaKorisnika, rotirajuca, ikonaFajla, kvacica1,kvacica2;
        ProgressBar progres;
        String id, tip;
        EkranKontakta ekran;
        RelativeLayout kontejner;
        Uri uri;
        MojAdapter2 mojAdapter2;
        boolean postojiSlikaKorisnika, postojiSlikaPosiljaoca;

        public AudioHolder (View view, Context context, MojAdapter2 mojAdapter2){
            this.mojAdapter2=mojAdapter2;
            slikaKorisnika = view.findViewById(R.id.slikaKorisnika);
            if (mojAdapter2.app.fotografija!=null)
                postojiSlikaKorisnika=true;
            else
                postojiSlikaKorisnika=false;
            slikaKorisnika.setOnClickListener(mojAdapter2);
            slikaPosiljaoca = view.findViewById(R.id.slikaPosiljaoca);
            if (mojAdapter2.aktivniKontakt.slika!=null)
                postojiSlikaPosiljaoca=true;
            else
                postojiSlikaPosiljaoca=false;
            slikaPosiljaoca.setOnClickListener(mojAdapter2);
            ikonaFajla=view.findViewById(R.id.ikonaFajla);
            ikonaFajla.setOnClickListener(this);
            rotirajuca=view.findViewById(R.id.rotirajuca);
            rotirajuca.setOnClickListener(this);
            imeFajla=view.findViewById(R.id.imeFajla);
            vrijeme=view.findViewById(R.id.vrijeme);
            progres=view.findViewById(R.id.neodredjeniProgresBar);
            kontejner=view.findViewById(R.id.kontejner);
            kontejner.setOnClickListener(this);
            kvacica1=view.findViewById(R.id.kvacica1);
            kvacica2=view.findViewById(R.id.kvacica2);
            ekran=(EkranKontakta)context;
            inicijaliPosiljaoca=view.findViewById(R.id.inicijaliPosiljaoca);
            inicijali=view.findViewById(R.id.inicijali);
            inicijali.setOnClickListener(mojAdapter2);
            inicijaliPosiljaoca.setOnClickListener(mojAdapter2);
        }

        @Override
        public void onClick(View v) {
            if (ekran.fragmentJePrikazan){
                ekran.skloniFragment();
                return;
            }

            if (v.getId()==rotirajuca.getId()) {
                ekran.posaljiFajl(uri, tip, imeFajla.getText().toString());
                return;
            }

            String akcija;
            if (tip.equals("application/vnd.android.package-archive")) {
                akcija=Intent.ACTION_INSTALL_PACKAGE;
            }
            else
                akcija=Intent.ACTION_VIEW;

            Intent i = new Intent(akcija);
            i.setDataAndType(uri, tip);
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Log.i("uri fajla u adapteru je ", uri.toString());

            if (i.resolveActivity(ekran.getPackageManager()) != null)
                ekran.startActivity(i);
            else {
                Toast.makeText(ekran,"No Application available to open this file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
