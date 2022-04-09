package com.example.nova;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

class MojAdapter extends BaseAdapter {
    MojaAplikacija app;
    LinkedList<EkranKontakta.Kontakt> spisak;

    public MojAdapter(MojaAplikacija app, LinkedList<EkranKontakta.Kontakt> spisak) {
        super();
        this.app = app;
        this.spisak=spisak;
    }

    @Override
    public int getCount() {
        return spisak.size();
    }

    @Override
    public Object getItem(int position) {
        synchronized (spisak){
            return spisak.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Naredbom if se optimizuje prikazivanje ListView-a. Metod getView se poziva svaki put
        //kad lista treba da prikaze novi red na ekranu. Kako se ne bi svaki put red inflantirao
        //tj. opet kreirao njegov graficki prikaz, varijabla convertView se koristi vise puta intentZaPronalazenjeOdgovarajuceApp
        //tako se izgled reda kreira samo jednom, a nakon toga se samo popunjava novim sadrzajima
        //To je prva optimizacija liste. Druga se vrsi pomocu objekta klase ViewHolder. Posto se
        //cesto poziva metod findViewById kako bi se pronasao unutrasnji view u retku liste,
        // a to je veoma zahtjevno, koristi se viewHolder. Njegov cilj je da smanji broj poziva
        //metdda findViewById.ViewHolder je prakticno lagana unutrasnja klasa koja ima direktne
        //reference na unutrasnje view-e retka. Kad se redak inflantira, tom prilikom se reference
        //na njegove view-e snimaju kao tagovi pomocu metoda setTag(). Tako se metod findView poziva samo
        //jednom, kad se layout retka prvi put kreira.
        ViewHolder viewHolder;
        EkranKontakta.Kontakt kontakt= (EkranKontakta.Kontakt) getItem(position);
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(app.getApplicationContext());
            convertView = inflater.inflate(R.layout.redak_glavne_aktivnosti, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.kvacica1.setVisibility(View.INVISIBLE);
        viewHolder.kvacica2.setVisibility(View.INVISIBLE);
        viewHolder.kvacica1.setColorFilter(null);
        viewHolder.kvacica2.setColorFilter(null);
        viewHolder.brojNeprocitanihporuka.setVisibility(View.INVISIBLE);
        viewHolder.inicijali.setVisibility(View.INVISIBLE);
        if (kontakt.slika!=null){
            viewHolder.slikaView.setImageBitmap(kontakt.slika);
        }
        else{
            viewHolder.slikaView.setImageBitmap(null);
            viewHolder.inicijali.setVisibility(View.VISIBLE);
            String[] s= kontakt.ime.split(" ");
            String inicijali="";
            for (int i=0; i<s.length; i++)
                inicijali=inicijali+s[i].charAt(0);
            viewHolder.inicijali.setText(inicijali.toUpperCase());
        }
        viewHolder.imeView.setText(kontakt.ime);
        String posljednjaPoruka;

        if (kontakt.sadrzajPrepiske.size()==0){
            viewHolder.posljednjaPorukaView.setText(null);
            viewHolder.vrijeme.setText(null);
        }

        else{
            if (kontakt.brojNeprocitanihPoruka>0){
                viewHolder.brojNeprocitanihporuka.setText(String.valueOf(kontakt.brojNeprocitanihPoruka));
                viewHolder.brojNeprocitanihporuka.setVisibility(View.VISIBLE);
            }

            posljednjaPoruka = kontakt.sadrzajPrepiske.getLast();
            String[] desifrovanaPoruka=posljednjaPoruka.replaceAll("</br>","\n").split(app.TAG);
            String tip=desifrovanaPoruka[0];
            if (tip.equals("poruka"))
                posljednjaPoruka=desifrovanaPoruka[2];
            else
                posljednjaPoruka=desifrovanaPoruka[0];
            viewHolder.posljednjaPorukaView.setText(posljednjaPoruka);
            prikaziVrijeme(viewHolder,desifrovanaPoruka[3]);
            if (desifrovanaPoruka[1].equals(app.ime)){
                ispitajStatus(viewHolder, kontakt, desifrovanaPoruka[desifrovanaPoruka.length-1]);
            }
        }
        return convertView;
    }

    private void prikaziVrijeme(ViewHolder viewHolder, String s){
        Date date = new Date();
        SimpleDateFormat formater = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String danasnjiDatum=formater.format(date).split(" ")[0].trim();

        String datum=s.split(" ")[0].trim();
        String vrijeme=s.split(" ")[1];

        if (danasnjiDatum.equals(datum))
            viewHolder.vrijeme.setText(vrijeme);
        else{
            String dan=datum.split("-")[0]+".";
            String mjesec=datum.split("-")[1]+".";
            viewHolder.vrijeme.setText(dan+mjesec);
        }
    }

    private void ispitajStatus(ViewHolder viewHolder,EkranKontakta.Kontakt kontakt, String id){
        synchronized (kontakt.statusPoruka){
            for (StatusPoruke s:kontakt.statusPoruka)
                if (s.id.equals(id)){
                    if (s.status==0 || s.status==-50){
                        viewHolder.kvacica1.setVisibility(View.VISIBLE);
                        return;
                    }
                    if (s.status==50){
                        viewHolder.kvacica1.setVisibility(View.VISIBLE);
                        viewHolder.kvacica1.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.svijetloZelena));
                    }
                    return;
                }
            //ako nema statusa te poruke u listi, znaci da je davno vec isporucena i
            //izbrisana iz liste, pa je zato nema. To samo znaci da treba prikazati kvacice
            viewHolder.kvacica1.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.svijetloZelena));
            viewHolder.kvacica2.setColorFilter(ContextCompat.getColor(app.getApplicationContext(), R.color.svijetloZelena));
            viewHolder.kvacica1.setVisibility(View.VISIBLE);
            viewHolder.kvacica2.setVisibility(View.VISIBLE);
        }
    }


    private static class ViewHolder {
        ImageView slikaView, kvacica1, kvacica2;
        TextView imeView, posljednjaPorukaView, vrijeme, brojNeprocitanihporuka, inicijali;

        public ViewHolder(View view) {
            inicijali=view.findViewById(R.id.inicijali);
            slikaView = view.findViewById(R.id.poljeZaSliku);
            imeView = view.findViewById(R.id.imeURetku);
            posljednjaPorukaView = view.findViewById(R.id.posljednjaPoruka);
            kvacica1=view.findViewById(R.id.kvacica1);
            kvacica2=view.findViewById(R.id.kvacica2);
            vrijeme=view.findViewById(R.id.vrijeme);
            brojNeprocitanihporuka=view.findViewById(R.id.broj_neprocitanih_poruka);
        }
    }
}