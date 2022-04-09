package com.example.nova;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;

public class MojaAplikacija extends Application implements Application.ActivityLifecycleCallbacks {
    static final String TAG="<Poruka@Grigor83>";
    static final int BAFER=5;
    GlavnaAktivnost glavnaAktivnost;
    MojAdapter mojAdapter, mojAdapterPrivremeni;
    //sljedece varijable sluze sa ucitavanje preferenci
    int sirinaEkrana, visinaEkrana, idPoruke;
    String ime, broj, korijenskiFolderString, id_kanala="stigla poruka";
    SharedPreferences podesavanja;
    //************************************************
    Socket glavniSoket, sporedniSoket;
    ObjectOutputStream glavniIzlaz, sporedniIzlaz;
    ObjectInputStream glavniUlaz, sporedniUlaz;
    boolean konektovan;
    //************************************************
    File folderKontakti;
    LinkedList<EkranKontakta.Kontakt> spisakKontakata, privremeniSpisakKontakata;
    LinkedList<String> neposlatePoruke;
    Notification notifikacija;
    TaskStackBuilder stackBuilder;
    PowerManager pm;
    DocumentFile slikaDocument,parent,blablaonica;
    Bitmap fotografija;

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        spisakKontakata=new LinkedList<>();
        mojAdapter=new MojAdapter(this, spisakKontakata);
        privremeniSpisakKontakata=new LinkedList<>();
        mojAdapterPrivremeni=new MojAdapter(this, privremeniSpisakKontakata);
        neposlatePoruke=new LinkedList<>();

        podesavanja = PreferenceManager.getDefaultSharedPreferences(this);
        ime=podesavanja.getString("ime",null);
        broj=podesavanja.getString("broj",null);
        sirinaEkrana=podesavanja.getInt("sirinaEkrana",0);
        visinaEkrana=podesavanja.getInt("visinaEkrana",0);
        korijenskiFolderString=podesavanja.getString("korijenski_folder",null);
        idPoruke=podesavanja.getInt("IDporuke",0);
        kreirajNotifikacijskiKanal();
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (ime!=null && broj!=null && korijenskiFolderString!=null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    inicijalizacijaPrograma(0);
                    uspostaviKonekciju();
                }
            }).start();
        }
    }

    //Slijede metode koje se pozivaju samo pri instaliranju i pokretanju novog procesa app:
    //inicijalizacijaPrograma(), kreirajStrukturuDirektorijuma(), ucitajFajloveUProgram. Prva metoda
    //poziva ostale dvije. Ove metode ne ucestvuju u konekciji, U/I tokovima podataka ni u
    //memorisanju novih poruka
    public void inicijalizacijaPrograma(int oznaka){
        //Poziva se iz Postavke (ako se prvi put instalira) ili iz MojeAplikacije  i izvodi se u sporednoj niti. Za to vrijeme, glavna nit
        //prikazuje GlavnuAktivnost i iscrtava njen GUI. Osnovni zadatak ove metode je da kreira
        //direktorijum Kontakti, i u njemu fajlove za svaki kontakt. Ti fajlovi ce sadrzati sve njihove poruke
        parent=DocumentFile.fromTreeUri(this, Uri.parse(korijenskiFolderString));
        blablaonica=parent.findFile("BLABLAONICA");

        if (oznaka!=1)
            ucitajImenik();

        //Sljedeca naredba ce se uvijek izvrsiti, prilikom svakog instanciranja klase MojaAplikacija
        //jer mi je objekat tipa File uvijek potreban, kako bih mogao dohvacati i manipulisati sa
        //fajlovima na memoriji telefona
        folderKontakti=new File(getFilesDir(),"KONTAKTI");
        //Sada mogu da ispitujem da li taj folder zaista postoji na disku
        if (!folderKontakti.exists())
            //Ako ne postoji, to znaci da se app prvi put pokrece i instalira, tako da treba kreirati
            //potrebnu strukturu direktorijuma na telefonu
            kreirajStrukturuDirektorijuma();
        else{
            // Ako taj folder postoji,onda je app vec ranije instalirana i bar jednom pokrenuta.To znaci da
            // imam svu potrebnu strukturu podataka na disku, tako da mogu da predjem na ucitavanje
            // podataka iz fajlova
            //Metod ucitajFajloveUProgram() ce se pokrenuti svaki put kad se proces app ponovo zapocinje,
            //jedino se nece pokrenuti prvi put prilikom instalacije app
            ucitajFajloveUProgram();
        }
    }

    public void ucitajImenik(){
        spisakKontakata.add(new EkranKontakta.Kontakt(this,"Nikola Nikolic","654",spisakKontakata.size()+500));
        spisakKontakata.add(new EkranKontakta.Kontakt( this,"Petar Petrovic", "888",spisakKontakata.size()+500));
        spisakKontakata.add(new EkranKontakta.Kontakt( this,"Zorica Brunclik","1",spisakKontakata.size()+500));

        String ime,broj;
        Cursor cursor=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,null,null,null);
        while (cursor.moveToNext()){
            ime=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            broj=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            broj=broj.replaceAll("[^0-9]", "");
            spisakKontakata.add(new EkranKontakta.Kontakt(this,ime,broj, spisakKontakata.size()+500));
        }
        cursor.close();

        if (glavnaAktivnost!=null){
            glavnaAktivnost.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mojAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void kreirajStrukturuDirektorijuma(){
        //Ovaj metod će se izvršiti samo jednom, pri instaliranju app. Nakon zavrsetka njegovog rada,
        //imacu kreiranu zeljenu strukturu direktorijuma na telefonu. U folderu KONTAKTI bice tri
        // fajla:jedan za memorisanje poruka, drugi za memorisanje neposlatih fajlova i treci za
        //memorisanje datuma posljednje prepiske.
        //Svi ti fajlovi bice prazni pri instalaciji, tako da nema potrebe zvati metod ucitajFajloveUProgram
        //Takodje, ovaj metod ne vrsi ni upisivanje ni citanje podataka iz interne memorije telefona
        if (!folderKontakti.mkdir()){        //kreiram stvarni folder u internoj memoriji
            Log.i("         **********KREIRANJE FOLDERA KONTAKTI NIJE USPJELO!",
                    " PREKIDAM KREIRANJE FAJLOVA O KONTAKTIMA***************");
            return;
        }
        else {
            //Pomocu for-each petlje u folderu KONTAKTI kreiram po jedan fajl za svaki kontakt. Za
            //to koristim jednu te istu varijablu fajl (jedan isti objekat tipa File)
            File fajl;
            ObjectOutputStream upisiObjekat=null;
            Poruka prepiska;
            for (EkranKontakta.Kontakt k: spisakKontakata){
                try {
                    fajl=new File(folderKontakti,k.ime+"_"+k.broj+".txt");
                    fajl.createNewFile();
                    //Nakon sto kreiram fajl nekog kontakta, u njega cu odmah kreirati i upisati jedan objekat
                    //tipa Poruka, koji ce sadrzati cjelokupnu prepisku tog kontakta.
                    upisiObjekat=new ObjectOutputStream(new FileOutputStream(fajl));
                    prepiska=new Poruka();
                    upisiObjekat.writeObject(prepiska);
                    upisiObjekat.close();
                } catch (IOException e) {
                    if (folderKontakti.exists())
                        folderKontakti.delete();
                    Log.i("         **********KREIRANJE FAJLOVA U FOLDERU",
                            " KONTAKTI NIJE USPJELO!********************");
                    e.printStackTrace();
                    return;
                }
            }
            Log.i("         **********KREIRANJE FAJLOVA U FOLDERU",
                    " KONTAKTI JE USPJESNO ZAVRSENO!*************");
        }
    }

    private void ucitajFajloveUProgram(){
        File f=new File(folderKontakti,"fotografija");
        if (f.exists()){
            ucitajProfilnuSliku(Uri.fromFile(f), false);
        }
        //Ovaj metod se pokrece svaki put, osim prilikom instaliranja aplikacije jer tad nema sta da
        //procita, svi fajlovi su sigurno prazni. Mora da se pokrene svaki put jer u programu (u polju
        // sadrzajPrepiske svakog objekta tipa Kontakt) moram imati podatke o njihovim prepiskama.
        for (EkranKontakta.Kontakt k: spisakKontakata){
            try {
                //u ovom metodu, ucitavam sve potrebne podatke za kontakte, ne samo poruke i datum
                ucitajPodatke(k);
            } catch (IOException | ClassNotFoundException e) {
                Log.i("***************FAJLOVI NISU PRONADJENI!",
                        "  PREKIDAM CITANJE FAJLOVA********************");
                e.printStackTrace();
                return;
            }
        }
        Log.i("***************SVI FAJLOVI SU USPJESNO ",
                " UCITANI U PROGRAM! ********************");
    }

    private void ucitajPodatke(EkranKontakta.Kontakt k) throws IOException, ClassNotFoundException {
        //Ovaj metod ucitava samo posljednjih BUFER broj poruka iz prepiske jednog kontakta.
        File fajl;
        fajl=new File(folderKontakti,k.ime+"_"+k.broj+".txt");
        if (fajl.exists()){
            ObjectInputStream ucitajPrepisku= new ObjectInputStream(new FileInputStream(fajl));
            Poruka prepiska=(Poruka) ucitajPrepisku.readObject();
            ucitajPrepisku.close();
            //ako prepiska ima vise poruka od broja koji zelim da ucitam, onda ucitavam samo
            //tacan broj posljednjih poruka
            if (prepiska.poruke.size()>BAFER){
                int kraj=prepiska.poruke.size();
                int pocetak=kraj-BAFER;
                for (int i=pocetak; i<kraj;i++)
                    k.sadrzajPrepiske.add(prepiska.poruke.get(i));
            }
            else{
                //u suprtonom, ako ima manje ili jednako, ucitavam sve poruke iz fajla
                for (String s: prepiska.poruke)
                    k.sadrzajPrepiske.add(s);
            }
            //Ucitavam datum posljednje prepiske, ako postoji
            if (prepiska.datum!=null)
                k.datumPoruke=prepiska.datum;
            //ucitavam neposlate poruke, ako ih ima
            if (!prepiska.neposlatePoruke.isEmpty())
                for (String s:prepiska.neposlatePoruke)
                    neposlatePoruke.add(s);
            //ucitavam i status neposlatih i neisporucenih poruka, ako ih ima
            if (!prepiska.statusPoruka.isEmpty())
                for (StatusPoruke status:prepiska.statusPoruka)
                    k.statusPoruka.add(status);
            //ucitavam i broj neprocitanih poruka, ako ima
            if (prepiska.brojNeprocitanihPoruka>0){
                k.brojNeprocitanihPoruka=prepiska.brojNeprocitanihPoruka;
                for (int i=k.brojNeprocitanihPoruka; i>0; i--){
                    String s=k.sadrzajPrepiske.get(k.sadrzajPrepiske.size()-i);
                    String poruka= s.split(TAG)[2].replaceAll("</br>","\n");
                    prikaziNotifikaciju(k,poruka);
                }
            }
            if (prepiska.slika!=null)
                k.slika=BitmapFactory.decodeByteArray(prepiska.slika,0,prepiska.slika.length);
        }
    }

    public void snimiNajnovijePoruke(final EkranKontakta.Kontakt k){
        // Pozivaju ga dvije niti: jedna je nit za neprestano citanje poruka, a druga je glavna UI nit
        //koja ga poziva nakon svakog pisanja nove poruke za slanje. Ipak, ovaj metod ne smije da bude sinhronizovan
        //jer moram dopustiti mogucnost da istovremeno bude pozvan od vise razlicitih niti. Naime, sta
        //ako istovremeno stignu dvije poruke za razlicite kontakte? Zasto da jedna nit ceka dok druga
        //zavrsi, kad ionako ne koriste iste resurse, tj pisu u razlicite fajlove. Ovo sam rijesio
        //sinhronizovanom naredbom po objektu tipa Kontakt
        if (k.najnovijePoruke.isEmpty())
            return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("**************snimam najnovije poruke kontakta "+k.ime," ");
                synchronized (k){
                    try{
                        File fajl=new File(folderKontakti,k.ime+"_"+k.broj+".txt");
                        Poruka prepiska;
                        ObjectInputStream ucitajObjekat= new ObjectInputStream(new FileInputStream(fajl));
                        prepiska=(Poruka) ucitajObjekat.readObject();
                        ucitajObjekat.close();
                        for (String s: k.najnovijePoruke) {
                            //prvo se snimaju poruke u fajl
                            prepiska.poruke.add(s.replaceAll("\n","</br>"));
                            //a onda se snima i datum, ako je potrebno
                            if (k.datumZaSnimanje!=null){
                                Log.i("snimam datum ",k.datumZaSnimanje);
                                prepiska.datum=k.datumZaSnimanje;
                                k.datumZaSnimanje=null;
                            }
                        }
                        k.najnovijePoruke.clear();

                        ObjectOutputStream upisiObjekat=new ObjectOutputStream(new FileOutputStream(fajl));
                        upisiObjekat.writeObject(prepiska);
                        upisiObjekat.close();
                    } catch (IOException | ClassNotFoundException e) {
                        Log.i("***************GRESKA U UPISIVANJU  ",
                                "************** NAJNOVIJIH PORUKA");
                        e.printStackTrace();
                        return;
                    }
                    //на крају, ако екран контакта није отворен и ако је активна главна активност, освјезава
                    //листу svih kontakata u glavnoj aktivnosti и приказује пристиглу поруку
                    if(!k.aktivan && glavnaAktivnost!=null)
                        glavnaAktivnost.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mojAdapter.notifyDataSetChanged();
                            }
                        });
                }
            }
        }).start();
    }

    public DocumentFile kreirajFolderBlablaonica(){
        if (korijenskiFolderString==null)
            return null;

        DocumentFile root = DocumentFile.fromTreeUri(this, Uri.parse(korijenskiFolderString));
        DocumentFile blablaonica= root.findFile("BLABLAONICA");
        if (blablaonica!=null){
            return blablaonica;
        }
        //kreira se folder blablaonica u folderu koji je korisnik izabrao
        blablaonica= root.createDirectory("BLABLAONICA");
        if (blablaonica.exists() && blablaonica.isDirectory()){
            Log.i("ime napravljenog fajla je",blablaonica.getName());
            return blablaonica;
        }
        else
            return null;
    }

    public boolean posaljiPorukuServeru(final String poruka){
        if(glavniIzlaz!=null){
            synchronized (glavniIzlaz){
                try {
                    glavniIzlaz.writeObject(poruka);
                    glavniIzlaz.flush();
                    return true;
                } catch (IOException e) {
                    Log.i("  SLANJE PORUKE NIJE USPJELO"," ");
                    return false;
                    //e.printStackTrace();
                }
            }
        }
        else{
            Log.i("  SLANJE PORUKE NIJE USPJELO JER IZLAZ NE POSTOJI"," ");
            return false;
        }
    }

    public void posaljiNeposlatePoruke(){
        if (neposlatePoruke.isEmpty())
            return;
        String brojPrimaoca, id;
        for (String poruka: neposlatePoruke) {
            synchronized (glavniIzlaz){
                try {
                    glavniIzlaz.writeObject(poruka);
                    glavniIzlaz.flush();
                    id=poruka.split(TAG)[0];
                    brojPrimaoca=id.split("_")[0];
                    synchronized (spisakKontakata){
                        for (final EkranKontakta.Kontakt primalac:spisakKontakata){
                            if (primalac.broj.equals(brojPrimaoca)){
                                for (StatusPoruke statusPoruke:primalac.statusPoruka)
                                    if (statusPoruke.id.equals(id)){
                                        synchronized (primalac.statusPoruka){
                                            statusPoruke.status=50;
                                            primalac.izmijeniStatus(statusPoruke);
                                            izbrisiNeposlatuPoruku(primalac,poruka);
                                            if (primalac.aktivan)
                                                primalac.ekran.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        primalac.ekran.mojAdapter2.notifyDataSetChanged();
                                                    }
                                                });
                                            else
                                            if (glavnaAktivnost!=null)
                                                glavnaAktivnost.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mojAdapter.notifyDataSetChanged();
                                                    }
                                                });
                                        }
                                        break;
                                    }
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    konektovan=false;
                    e.printStackTrace();
                    return;
                }
            }
        }
        neposlatePoruke.clear();
    }

    public void snimiNeposlatePoruke(String poruka){
        String brojPrimaoca=poruka.split(TAG)[0].split("_")[0];
        File fajl;

        synchronized (spisakKontakata){
            for (EkranKontakta.Kontakt primalac:spisakKontakata)
                if (primalac.broj.equals(brojPrimaoca)) {
                    fajl=new File(folderKontakti,primalac.ime+"_"+primalac.broj+".txt");
                    synchronized (primalac){
                        try{
                            ObjectInputStream ucitajPrepisku= new ObjectInputStream(new FileInputStream(fajl));
                            Poruka prepiska=(Poruka) ucitajPrepisku.readObject();
                            ucitajPrepisku.close();

                            prepiska.neposlatePoruke.add(poruka);
                            Log.i("neposlata poruka je dodata "," ");

                            ObjectOutputStream upisiObjekat=new ObjectOutputStream(new FileOutputStream(fajl));
                            upisiObjekat.writeObject(prepiska);
                            upisiObjekat.flush();
                            upisiObjekat.close();
                        }
                        catch (IOException |ClassNotFoundException e){
                            e.printStackTrace();
                            System.exit(0);
                        }
                    }
                    break;
                }
        }
    }

    private void izbrisiNeposlatuPoruku(EkranKontakta.Kontakt primalac, String poruka){
        File fajl=new File(folderKontakti,primalac.ime+"_"+primalac.broj+".txt");

        synchronized (primalac){
            try{
                ObjectInputStream ucitajPrepisku= new ObjectInputStream(new FileInputStream(fajl));
                Poruka prepiska=(Poruka) ucitajPrepisku.readObject();
                ucitajPrepisku.close();

                for (String neposlataPoruka:prepiska.neposlatePoruke)
                    if (neposlataPoruka.equals(poruka)){
                        prepiska.neposlatePoruke.remove(neposlataPoruka);
                        Log.i("neposlata poruka je izbrisana "," ");
                        break;
                    }

                ObjectOutputStream upisiObjekat=new ObjectOutputStream(new FileOutputStream(fajl));
                upisiObjekat.writeObject(prepiska);
                upisiObjekat.flush();
                upisiObjekat.close();
            }
            catch (IOException |ClassNotFoundException e){
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    public void neprestanoCitajPoruke(){
        new Thread(new Runnable() {
            Object o;
            String primljenaPoruka, brojPosiljaoca, datum, potvrda;
            String[] rasclanjenaPoruka;
            byte[] slikaKontakta;
            @Override
            public void run() {
                while (true){
                    try {
                        o=glavniUlaz.readObject();
                        if (o.getClass()!=String.class){
                            slikaKontakta=(byte[]) o;
                            continue;
                        }

                        primljenaPoruka=(String)o;
                        rasclanjenaPoruka=primljenaPoruka.split(TAG);
                        //Primam samo potvrdu i tad je duzina cijele poruke == 1
                        if (rasclanjenaPoruka.length==1){
                            potvrda=rasclanjenaPoruka[0].trim();
                            posaljiPorukuServeru(potvrda);
                            izbrisiStatusPoruke(potvrda);
                            continue;
                        }

                        if (primljenaPoruka.contains("slika")){
                            synchronized (spisakKontakata){
                                for (EkranKontakta.Kontakt k:spisakKontakata){
                                    if (k.broj.equals(rasclanjenaPoruka[1])){
                                        k.slika=BitmapFactory.decodeByteArray(slikaKontakta,0,slikaKontakta.length);
                                        k.snimiProfilnuFotografiju(slikaKontakta);
                                        if (glavnaAktivnost!=null)
                                            glavnaAktivnost.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mojAdapter.notifyDataSetChanged();
                                                }
                                            });
                                        break;
                                    }
                                }
                            }
                            continue;
                        }

                        //ili, primam cijelu poruku pa moram da vratim potvrdu serveru i
                        //da prikazem poruku u adapteru
                        potvrda=rasclanjenaPoruka[0].trim();
                        posaljiPorukuServeru(potvrda);
                        brojPosiljaoca=rasclanjenaPoruka[1].trim();
                        datum=rasclanjenaPoruka[3].split(" ")[0].trim();
                        //pretrazujem listu kontakata kako bih primljenu poruku prikazao u okviru
                        //prave prepiske, sa onim ko mi je poslao
                        synchronized (spisakKontakata){
                            for (EkranKontakta.Kontakt posiljalac:spisakKontakata){
                                if (posiljalac.broj.equals(brojPosiljaoca)){
                                    primljenaPoruka="poruka"+TAG+posiljalac.ime+TAG+rasclanjenaPoruka[2]+TAG+rasclanjenaPoruka[3];
                                    if (posiljalac.aktivan){
                                        //U metodu dodajPorukuUPrepiske se zapocinje nova nit i sinhronizuje
                                        //se po objektu aktivnog kontakta
                                        posiljalac.ekran.dodajPorukuUPrepiske(primljenaPoruka, datum, false);
                                        boolean upaljen= pm.isInteractive();
                                        if (upaljen && posiljalac.brojNeprocitanihPoruka!=0){
                                            posiljalac.brojNeprocitanihPoruka++;
                                            snimiBrojNeprocitanihPoruka(posiljalac);
                                            prikaziNotifikaciju(posiljalac, rasclanjenaPoruka[2]);
                                        }
                                        if (!upaljen){
                                            posiljalac.brojNeprocitanihPoruka++;
                                            snimiBrojNeprocitanihPoruka(posiljalac);
                                            prikaziNotifikaciju(posiljalac, rasclanjenaPoruka[2]);
                                        }
                                    }
                                    else{
                                        if (!posiljalac.datumPoruke.equals(datum)){
                                            posiljalac.sadrzajPrepiske.add("datum"+TAG+datum);
                                            posiljalac.najnovijePoruke.add("datum"+TAG+datum);
                                            posiljalac.datumZaSnimanje=datum;
                                            posiljalac.datumPoruke=datum;
                                        }
                                        posiljalac.sadrzajPrepiske.add(primljenaPoruka);
                                        posiljalac.najnovijePoruke.add(primljenaPoruka);
                                        posiljalac.brojNeprocitanihPoruka++;
                                        snimiBrojNeprocitanihPoruka(posiljalac);
                                        //Nova nit za snimanje poruka se zapocinje u metodu snimiNajnovijePoruke,
                                        //tako da se ova nit oslobadja za citanje nadolazecih poruka
                                        snimiNajnovijePoruke(posiljalac);
                                        prikaziNotifikaciju(posiljalac, rasclanjenaPoruka[2]);
                                    }
                                    synchronized (spisakKontakata){
                                        if (spisakKontakata.remove(posiljalac))
                                            spisakKontakata.addFirst(posiljalac);
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        Log.i("     GRESKA U CITANJU PORUKA  "," ");
                        konektovan=false;
                        rekonektuj();
                        //Prekida se samo petlja za citanje poruka, ali se ne prekida ova nit programa.
                        //Nit se i dalje nastavlja i pokusace da se rekonektuje na server
                        break;
                    }
                }
            }
        }).start();
    }

    public int noviIDporuke(){
        SharedPreferences.Editor editor= podesavanja.edit();
        idPoruke++;
        editor.putInt("IDporuke",idPoruke);
        editor.apply();
        return idPoruke;
    }

    private void izbrisiStatusPoruke(String IDporuke){
        String brojPrimaoca=IDporuke.split("_")[1];
        String id=brojPrimaoca+"_"+IDporuke.split("_")[2];
        synchronized (spisakKontakata){
            for (final EkranKontakta.Kontakt primalac:spisakKontakata){
                if (primalac.broj.equals(brojPrimaoca)){
                    synchronized (primalac.statusPoruka){
                        for (StatusPoruke s: primalac.statusPoruka){
                            if (s.id.equals(id)){
                                primalac.statusPoruka.remove(s);
                                primalac.izbrisiStatus(s);
                                if (primalac.aktivan)
                                    primalac.ekran.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            primalac.ekran.mojAdapter2.notifyDataSetChanged();
                                        }
                                    });
                                else{
                                    if (glavnaAktivnost!=null)
                                        glavnaAktivnost.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mojAdapter.notifyDataSetChanged();
                                            }
                                        });
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public void snimiBrojNeprocitanihPoruka(final EkranKontakta.Kontakt kontakt){
        new Thread(new Runnable() {
            @Override
            public void run() {
                File fajl;

                fajl=new File(folderKontakti,kontakt.ime+"_"+kontakt.broj+".txt");
                synchronized (kontakt){
                    try{
                        ObjectInputStream ucitajPrepisku= new ObjectInputStream(new FileInputStream(fajl));
                        Poruka prepiska=(Poruka) ucitajPrepisku.readObject();
                        ucitajPrepisku.close();

                        prepiska.brojNeprocitanihPoruka=kontakt.brojNeprocitanihPoruka;

                        ObjectOutputStream upisiObjekat=new ObjectOutputStream(new FileOutputStream(fajl));
                        upisiObjekat.writeObject(prepiska);
                        upisiObjekat.flush();
                        upisiObjekat.close();
                    }
                    catch (IOException |ClassNotFoundException e){
                        e.printStackTrace();
                        System.exit(0);
                    }
                }
            }
        }).start();
    }

    private void kreirajNotifikacijskiKanal() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "mojKanal";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            String description = "kanal za prikazivanje notifikacija";
            Uri uriZvuka=Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bla_bla);
            AudioAttributes atr= new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build();

            NotificationChannel channel = new NotificationChannel(id_kanala, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000,500,1000});
            channel.enableVibration(false);
            channel.setShowBadge(true);
            channel.setSound(uriZvuka,atr);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void prikaziNotifikaciju(EkranKontakta.Kontakt kontakt, String poruka){

        stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(kontakt.notifikacijskiIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(kontakt.id+kontakt.brojNeprocitanihPoruka, PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap icon=null;
        if (kontakt.slika!=null)
             icon= kontakt.slika;

        notifikacija = new NotificationCompat.Builder(getApplicationContext(), id_kanala)
                .setSmallIcon(R.drawable.ikona_aplikacije)
                .setContentTitle(kontakt.ime)
                .setContentText(poruka)
                .setLargeIcon(icon)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLights(Color.GREEN,1000,500)
                .setVibrate(new long[]{0, 1000, 500, 1000,500,1000})
                .setColor(Color.GREEN)
                .setColorized(true)
                .setGroup(String.valueOf(kontakt.id))
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        // notificationId is a unique int for each notification that you must define
        if (kontakt.brojNeprocitanihPoruka==1){
            notificationManager.notify(kontakt.id+kontakt.brojNeprocitanihPoruka, notifikacija);
            return;
        }

        if (kontakt.brojNeprocitanihPoruka>1){
            kontakt.summaryNotification =
                    new NotificationCompat.Builder(getApplicationContext(), id_kanala)
                            .setContentTitle(kontakt.ime)
                            .setSmallIcon(R.drawable.ikona_aplikacije)
                            .setGroup(String.valueOf(kontakt.id))
                            .setGroupSummary(true)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setContentIntent(resultPendingIntent)
                            .setAutoCancel(true)
                            .build();

            notificationManager.notify(kontakt.id+kontakt.brojNeprocitanihPoruka, notifikacija);
            notificationManager.notify(kontakt.id, kontakt.summaryNotification);
            return;
        }
    }



        //*********** ISPOD OVOGA SU METODE KOJE MORAM DA ZAVRSIM ********************

    public void uspostaviKonekciju(){
        //Ovaj metod je vec pozvan iz sporedne niti (koja je kreirana ili u postavkama pri instaliranju
        //app, ili u metodu onCreate ove MojeAplikacije), tako da za njega ne treba kreirati novu nit
        Intent i=new Intent(this,MojServis.class);
        startService(i);
    }

    private synchronized void rekonektuj(){
        if (konektovan)
            return;
        try {
            if (glavniSoket!=null)
                glavniSoket.close();
            if(sporedniSoket!=null)
                sporedniSoket.close();
        } catch (IOException e) {
            Log.i("\n**********SOKET NIJE ISPRAVNO ZATVOREN***************"," ");
            //e.printStackTrace();
            return;
        }

        Log.i(" pokusavam rekonektovanje"," ");
        uspostaviKonekciju();
    }

    public void neprestanoCitajFalove() {
        new Thread(new Runnable() {
            Object o;
            String IDporuke, imeFajla, vrijeme, tipFajla, potvrda;
            String [] polje;
            @Override
            public void run() {
                while(true) {
                    try {
                        o = sporedniUlaz.readObject();
                        if (o instanceof String){
                            polje=o.toString().split(TAG);

                            if (polje.length==1){
                                potvrda=polje[0].trim();
                                posaljiPotvrduServeru(potvrda);
                                izbrisiStatusPoruke(potvrda);
                            }
                            else{
                                potvrda=polje[0].trim();
                                posaljiPotvrduServeru(potvrda);
                                IDporuke=polje[0];
                                vrijeme=polje[1];
                                imeFajla=polje[2];
                                tipFajla=polje[3];
                            }
                        }
                        else
                            snimiPrimljeniFajl(IDporuke, imeFajla, tipFajla, vrijeme, o);
                    } catch (ClassNotFoundException | IOException e) {
                        Log.i("************GRESKA U CITANJU SA",
                                " SPOReDNOG ULAZA");
                        //e.printStackTrace();
                        break;
                    }
                }
            }
        }).start();
    }

    private void posaljiPotvrduServeru(String potvrda) throws IOException {
        if (sporedniIzlaz!=null){
            sporedniIzlaz.writeObject(potvrda);
            sporedniIzlaz.flush();
        }
    }

    private void snimiPrimljeniFajl(final String IDporuke, final String imeFajla, final String tipFajla, final String vrijeme, final Object o){
        new Thread(new Runnable() {
            @Override
            public void run() {
                DocumentFile dokument=null;
                String staza=null;
                if (ispitajDaLiDokumentPostoji(Uri.parse(korijenskiFolderString), "BLABLAONICA")==null)
                    blablaonica= kreirajFolderBlablaonica();

                if (tipFajla.split("/")[0].equals("image")){
                    dokument= blablaonica.createFile("image/jpeg", imeFajla);
                    Bitmap b= skalirajPrimljenuSliku(o);
                    if (!snimiBitmap(dokument.getUri(),b))
                        return;
                    staza=dokument.getUri().toString();
                }
                else  if (tipFajla.split("/")[0].equals("video")){
                    dokument= blablaonica.createFile(tipFajla, imeFajla);
                    OutputStream stream;
                    BufferedOutputStream buff;
                    try {
                        stream=getContentResolver().openOutputStream(dokument.getUri());
                        buff=new BufferedOutputStream(stream);
                        buff.write((byte[])o);
                        buff.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    staza=kreirajVideoBitmap(imeFajla,dokument.getUri());
                }
                else{
                    dokument= blablaonica.createFile(tipFajla, imeFajla);
                    OutputStream stream;
                    BufferedOutputStream buff;
                    try {
                        stream=getContentResolver().openOutputStream(dokument.getUri());
                        buff=new BufferedOutputStream(stream);
                        buff.write((byte[])o);
                        buff.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    staza=dokument.getUri().toString();
                }
                Log.i("primljeni fajl je uspjesno snimljen"," ");

                String brojPosiljaoca=IDporuke.split("_")[0];
                synchronized (spisakKontakata){
                    for (EkranKontakta.Kontakt posiljalac:spisakKontakata)
                        if (posiljalac.broj.equals(brojPosiljaoca)){
                            String porukaZaAdapter=tipFajla+TAG+posiljalac.ime+TAG+dokument.getUri().toString()+TAG+vrijeme+TAG+staza+TAG+imeFajla;
                            if (posiljalac.aktivan){
                                posiljalac.ekran.dodajFajlUPrepiske(porukaZaAdapter, null, false);
                                boolean upaljen= pm.isInteractive();
                                if (upaljen && posiljalac.brojNeprocitanihPoruka!=0){
                                    posiljalac.brojNeprocitanihPoruka++;
                                    snimiBrojNeprocitanihPoruka(posiljalac);
                                    prikaziNotifikaciju(posiljalac,imeFajla);
                                }
                                if (!upaljen){
                                    posiljalac.brojNeprocitanihPoruka++;
                                    snimiBrojNeprocitanihPoruka(posiljalac);
                                    prikaziNotifikaciju(posiljalac,imeFajla);
                                }
                            }
                            else{
                                String datum=vrijeme.split(" ")[0].trim();
                                if (!posiljalac.datumPoruke.equals(datum)){
                                    posiljalac.sadrzajPrepiske.add("datum"+TAG+datum);
                                    posiljalac.najnovijePoruke.add("datum"+TAG+datum);
                                    posiljalac.datumZaSnimanje=datum;
                                    posiljalac.datumPoruke=datum;
                                }
                                posiljalac.sadrzajPrepiske.add(porukaZaAdapter);
                                posiljalac.najnovijePoruke.add(porukaZaAdapter);
                                posiljalac.brojNeprocitanihPoruka++;
                                snimiBrojNeprocitanihPoruka(posiljalac);
                                //Nova nit za snimanje poruka se zapocinje u metodu snimiNajnovijePoruke,
                                //tako da se ova nit oslobadja za citanje nadolazecih poruka
                                snimiNajnovijePoruke(posiljalac);
                                prikaziNotifikaciju(posiljalac,imeFajla);
                            }
                            synchronized (spisakKontakata){
                                if (spisakKontakata.remove(posiljalac))
                                    spisakKontakata.addFirst(posiljalac);
                            }
                            break;
                        }
                }
            }
        }).start();
    }

    private Bitmap skalirajPrimljenuSliku(Object o){
        byte[] sadrzaj= (byte[]) o;
        int konacnaSirina, konacnaVisina;
        //samo ocitavam dimenzije bitmapa iz sadrzaja
        BitmapFactory.Options opcije = new BitmapFactory.Options();
        opcije.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(sadrzaj,0,sadrzaj.length,opcije);
        //odredjujem razmjer smanjenja slike i konacnu visinu i sirinu
        int sirinaSlike=opcije.outWidth;
        int visinaSlike=opcije.outHeight;
        int maxSirina=(int)((visinaEkrana/2)/1.7);
        int maxVisina=(int)((visinaEkrana/2));

        if (sirinaSlike<maxSirina && visinaSlike<maxVisina){
            opcije.inJustDecodeBounds = false; // Decode bitmap with inSampleSize set
            Bitmap b= BitmapFactory.decodeByteArray(sadrzaj,0,sadrzaj.length,opcije);
            b= Bitmap.createScaledBitmap(b, maxSirina, maxVisina,true);   //smanjujem sliku na tacno odredjene dimenzije pogleda
            return b;
        }

        float odnosSirina, odnosVisina;
        odnosSirina=(float) sirinaSlike/maxSirina;
        odnosVisina= (float) visinaSlike/maxVisina;
        int maxOmjer;
        if (odnosSirina>odnosVisina){
            maxOmjer=(int)odnosSirina;
            konacnaSirina=maxSirina;
            konacnaVisina= Math.round(visinaSlike/odnosSirina);
        }
        else{
            Log.i("maksimalna je visina "," ");
            maxOmjer=(int)odnosVisina;
            konacnaVisina=maxVisina;
            konacnaSirina= Math.round (sirinaSlike/odnosVisina);
        }
        int konacanOmjer= pow2Ceil(maxOmjer);
        if (konacanOmjer<2)
            konacanOmjer=2;
        //kreiram smanjeni bitmap
        opcije.inSampleSize =konacanOmjer; // pow2Ceil: A utility function that comes later
        opcije.inJustDecodeBounds = false; // Decode bitmap with inSampleSize set
        Bitmap b= BitmapFactory.decodeByteArray(sadrzaj,0,sadrzaj.length,opcije);
        b= Bitmap.createScaledBitmap(b, konacnaSirina, konacnaVisina,true);   //smanjujem sliku na tacno odredjene dimenzije pogleda
        return b;
    }

    public int pow2Ceil(int number) {
        return 1 << -(Integer.numberOfLeadingZeros(number) + 1); // is equivalent to:
        // return Integer.rotateRight(1, Integer.numberOfLeadingZeros(number) + 1);
    }

    public boolean snimiBitmap(Uri uriFajla, Bitmap b){
        OutputStream stream;
        BufferedOutputStream buff;

        try {
            stream=getContentResolver().openOutputStream(uriFajla);
            buff=new BufferedOutputStream(stream);
            b.compress(Bitmap.CompressFormat.JPEG, 100, buff);
            buff.close();
            Log.i("bitmap je snimljen"," ");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String kreirajVideoBitmap(String imeFajla, Uri uriFajla){
        File noviFajl;
        if (imeFajla!=null)
            noviFajl = new File(getFilesDir(), imeFajla.trim());
        else
            noviFajl=new File(uriFajla.toString());

        if (!noviFajl.exists()){
            int maxSirina=(int)((visinaEkrana/2)/1.7);
            int maxVisina=(int)((visinaEkrana/2));
            MediaMetadataRetriever mMMR = new MediaMetadataRetriever();
            mMMR.setDataSource(this, uriFajla);
            Bitmap bmp=mMMR.getFrameAtTime(0);
            int sirinaSlike=bmp.getWidth();
            int visinaSlike=bmp.getHeight();
            float odnosSirina, odnosVisina;
            odnosSirina=(float) sirinaSlike/maxSirina;
            odnosVisina= (float) visinaSlike/maxVisina;
            int konacnaSirina,konacnaVisina;
            if (odnosSirina>odnosVisina){
                konacnaSirina=maxSirina;
                konacnaVisina= Math.round(visinaSlike/odnosSirina);
            }
            else{
                konacnaVisina=maxVisina;
                konacnaSirina= Math.round (sirinaSlike/odnosVisina);
            }
            bmp=Bitmap.createScaledBitmap(bmp,konacnaSirina,konacnaVisina, false);
            try {
                noviFajl.createNewFile();
                BufferedOutputStream izlaz=new BufferedOutputStream(new FileOutputStream(noviFajl));
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, izlaz);
                izlaz.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return noviFajl.getPath();
    }

    public synchronized void posaljiFajlServeru(final EkranKontakta.Kontakt kontakt, final StatusPoruke statusPoruke, final String idPoruke,
                                                final String vrijeme, final Uri uriFajla, final String imeFajla, final String tipFajla){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (sporedniIzlaz==null){
                    azurirajStatus(kontakt, statusPoruke, -50);
                    Log.i("fajl nije poslat"," ");
                }
                else {
                    byte[] sadrzaj=null;
                    try {//Ucitavam sadrzaj fajla
                        InputStream in = getContentResolver().openInputStream(uriFajla);
                        BufferedInputStream procitajFajl = new BufferedInputStream(in);
                        sadrzaj = new byte[procitajFajl.available()];
                        procitajFajl.read(sadrzaj);
                        procitajFajl.close();
                        //a zatim ga saljem serveru
                        String poruka=idPoruke+TAG+vrijeme+TAG+imeFajla+TAG+tipFajla;
                        sporedniIzlaz.writeObject(poruka);
                        sporedniIzlaz.writeObject(sadrzaj);
                        sporedniIzlaz.flush();
                        Log.i("fajl je uspjesno poslat"," ");
                        azurirajStatus(kontakt, statusPoruke, 50);
                    } catch (IOException e) {
                        azurirajStatus(kontakt, statusPoruke, -50);
                        Log.i("fajl nije poslat"," ");
                        //e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void azurirajStatus(final EkranKontakta.Kontakt kontakt, StatusPoruke statusPoruke, int stanje){
        synchronized (kontakt.statusPoruka){
            statusPoruke.status=stanje;
            kontakt.izmijeniStatus(statusPoruke);
            if (kontakt.aktivan)
                kontakt.ekran.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        kontakt.ekran.mojAdapter2.notifyDataSetChanged();
                    }
                });
            else{
                if (glavnaAktivnost!=null){
                    glavnaAktivnost.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mojAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        }
    }

    public Uri ispitajDaLiDokumentPostoji(Uri korijen, String imeDokumenta){
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(korijen,
                DocumentsContract.getDocumentId(korijen));
        Cursor c = getContentResolver().query(childrenUri,
                new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                null, null, null);
        try {
            while (c.moveToNext()) {
                final String name = c.getString(0);
                if (name.equals(imeDokumenta)){
                    String id=c.getString(1);
                    Uri documentUri=DocumentsContract.buildDocumentUriUsingTree(korijen,id);
                    return documentUri;
                }
            }
        } finally {
            c.close();
        }
        return null;
    }

    public void ucitajProfilnuSliku(Uri uri, boolean posalji){
        try {
            InputStream i= getContentResolver().openInputStream(uri);
            BufferedInputStream buf=new BufferedInputStream(i);
            byte[] sadrzaj= new byte[buf.available()];
            buf.read(sadrzaj);
            buf.close();
            fotografija=BitmapFactory.decodeByteArray(sadrzaj,0,sadrzaj.length);
            if (posalji){
                glavniIzlaz.writeObject(sadrzaj);
                glavniIzlaz.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (GlavnaAktivnost.class==activity.getClass()){
            glavnaAktivnost=(GlavnaAktivnost) activity;
            mojAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (GlavnaAktivnost.class==activity.getClass())
            glavnaAktivnost=null;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }


}
