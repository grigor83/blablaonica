package com.example.nova;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class EkranKontakta extends AppCompatActivity implements View.OnTouchListener {
    final int USLIKAJ=1, IZABERI_FAJL=2;
    MojaAplikacija app;
    private RelativeLayout cijeliEkran;
    private TextView imeAktivnogKorisnika;
    private ListView prikazPrepiske;
    private EditText napisanaPoruka;
    private ImageButton uslikaj,posaljiFajl, strelica;
    private LinearLayout kontejnerFragmenta;
    private Kontakt aktivniKontakt;
    MojAdapter2 mojAdapter2;
    private String datum, displayName,tip;
    private Date date;
    private SimpleDateFormat formater;
    boolean noviDatum, fragmentJePrikazan;
    FragmentDugogPritiska fragment;
    private NotificationManagerCompat notificationManager;
    String vrijemeSlikanja, imeFotografije;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ekran_kontakta);
        app=(MojaAplikacija)getApplication();
        cijeliEkran=findViewById(R.id.cijeliEkran);
        imeAktivnogKorisnika=findViewById(R.id.imeAktivnogKontakta);
        prikazPrepiske=findViewById(R.id.prikazPrepiske);
        napisanaPoruka=findViewById(R.id.napisanaPoruka);
        strelica=findViewById(R.id.strelica);
        posaljiFajl=findViewById(R.id.posaljiFajl);
        uslikaj=findViewById(R.id.uslikaj);
        kontejnerFragmenta=findViewById(R.id.kontejner_fragmenta);

        odrediPozadinu();

        //Dobijanje podatka iz intenta i pronalazenje kontakta iz polja spisakKontakata
        final String ime= getIntent().getExtras().getString("ime");
        imeAktivnogKorisnika.setText(ime);

        synchronized (app.spisakKontakata){
            for (Kontakt kontakt:app.spisakKontakata){
                if (kontakt.ime.equals(ime)){
                    aktivniKontakt=kontakt;
                    aktivniKontakt.aktivan=true;
                    aktivniKontakt.ekran=this;
                    aktivniKontakt.brojNeprocitanihPoruka=0;
                    app.snimiBrojNeprocitanihPoruka(aktivniKontakt);
                    notificationManager = NotificationManagerCompat.from(this);
                    notificationManager.cancel(aktivniKontakt.id);
                    notificationManager.cancel(aktivniKontakt.id+1);
                    aktivniKontakt.summaryNotification=null;
                    break;
                }
            }
        }
        //sad treba kreirati adapter i u njega ucitati sadrzaj prepiske tog kontakta
        //Adapteru ce se kao argument proslijediti kopija trenutne prepiske,kako bi se
        //omogucilo da se svajpovanjem ucita vise od 50 poruka koje inace moze da primi varijabla
        //kontakta sadrzaj prepiske
        LinkedList<String> privremenaPrepiska=new LinkedList<>();
        privremenaPrepiska=(LinkedList<String>) aktivniKontakt.sadrzajPrepiske.clone();
        mojAdapter2=new MojAdapter2(this, aktivniKontakt,privremenaPrepiska);
        prikazPrepiske.setAdapter(mojAdapter2);
        //dodajem oslusckivac za prikazivanje/sklanjanje fragmenta
        cijeliEkran.setOnTouchListener(this);
        prikazPrepiske.setOnTouchListener(this);
        napisanaPoruka.setOnTouchListener(this);
        imeAktivnogKorisnika.setOnTouchListener(this);
        strelica.setOnTouchListener(this);
        posaljiFajl.setOnTouchListener(this);
        uslikaj.setOnTouchListener(this);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        odrediPozadinu();
        aktivniKontakt.brojNeprocitanihPoruka=0;
        app.snimiBrojNeprocitanihPoruka(aktivniKontakt);
        notificationManager.cancel(aktivniKontakt.id);
        notificationManager.cancel(aktivniKontakt.id);
        aktivniKontakt.summaryNotification=null;
    }

    public void posaljiPorukuStrelicom(View v){
        String poruka = napisanaPoruka.getText().toString().trim();
        if (poruka.length()>0){
            date = new Date(); // this object contains the current date value
            formater = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            datum=formater.format(date).split(" ")[0].trim();

            dodajPorukuUPrepiske(poruka, datum, true);

            napisanaPoruka.getText().clear();
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(napisanaPoruka.getWindowToken(), 0);
        }
    }

    public void dodajPorukuUPrepiske(final String poruka, final String datum, final boolean posalji){
        new Thread(new Runnable() {
            String porukaZaAdapter;
            @Override
            public void run() {
                synchronized (app.spisakKontakata){
                    if (app.spisakKontakata.remove(aktivniKontakt))
                        app.spisakKontakata.addFirst(aktivniKontakt);
                }
                synchronized (aktivniKontakt){
                    //Posto ovaj metod moze pozvati i metod za neprestano citanje poruka, moram to
                    //ispitati sljedecom if naredbom. U slucaju da ja saljem poruku pritiskom na strelicu,
                    //moram je konstruisati u odgovarajucem obliku. U suprotnom, samo preuzimam poruku
                    //koja je vec pripremljena u niti neprestanog citanja poruka.
                    if (posalji){
                        String id=aktivniKontakt.broj+"_"+app.noviIDporuke();
                        porukaZaAdapter ="poruka"+app.TAG+app.ime+app.TAG+poruka+app.TAG+formater.format(date)+app.TAG+id;
                        StatusPoruke statusPoruke=new StatusPoruke(id);
                        synchronized (aktivniKontakt.statusPoruka){
                            aktivniKontakt.statusPoruka.add(statusPoruke);
                            if (app.posaljiPorukuServeru(id+app.TAG+poruka+app.TAG+formater.format(date)))
                                statusPoruke.status=50;
                            else{
                                synchronized (app.neposlatePoruke){
                                    app.neposlatePoruke.add(id+app.TAG+poruka+app.TAG+formater.format(date));
                                    app.snimiNeposlatePoruke(id+app.TAG+poruka+app.TAG+formater.format(date));
                                }
                            }
                            aktivniKontakt.snimiStatus(statusPoruke);
                        }
                    }
                    else
                        porukaZaAdapter =poruka;
                    //Prvo se dodaje datum u prepisku, ako je potrebno
                    if (!aktivniKontakt.datumPoruke.equals(datum)){
                        aktivniKontakt.najnovijePoruke.add("datum"+app.TAG+datum);
                        aktivniKontakt.sadrzajPrepiske.add("datum"+app.TAG+datum);
                        noviDatum=true;
                        aktivniKontakt.datumZaSnimanje=datum;
                        aktivniKontakt.datumPoruke=datum;
                    }
                    //A onda i sama poruka
                    aktivniKontakt.najnovijePoruke.add(porukaZaAdapter);
                    aktivniKontakt.sadrzajPrepiske.add(porukaZaAdapter);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (noviDatum){
                                mojAdapter2.add("datum"+app.TAG+datum);
                                noviDatum=false;
                            }
                            mojAdapter2.add(porukaZaAdapter);
                            prikazPrepiske.setSelection(prikazPrepiske.getCount()-1);
                        }
                    });
                    app.snimiNajnovijePoruke(aktivniKontakt);
                }
            }
        }).start();
    }

    public void prikaziFragment(float x, float y, String poruka){
        if (fragmentJePrikazan)
            return;

        fragmentJePrikazan=true;

        int trecinaSirine=prikazPrepiske.getMeasuredWidth()/3;
        int polovinaVisine=prikazPrepiske.getHeight()/2;

        RelativeLayout.LayoutParams parametri= (RelativeLayout.LayoutParams) kontejnerFragmenta.getLayoutParams();

        if (x>trecinaSirine*2){
            parametri.addRule(RelativeLayout.ALIGN_PARENT_END);
        }
        else if (x>trecinaSirine){
            parametri.removeRule(RelativeLayout.ALIGN_PARENT_END);
            parametri.setMarginStart((int)x-trecinaSirine/2);
        }
        else {
            parametri.removeRule(RelativeLayout.ALIGN_PARENT_END);
            parametri.setMarginStart((int)x);
        }

        if (y>polovinaVisine){
            kontejnerFragmenta.setY(y-polovinaVisine/3);
        }
        else{
            kontejnerFragmenta.setY(y);
        }

        kontejnerFragmenta.setLayoutParams(parametri);

        fragment=new FragmentDugogPritiska();
        FragmentManager fragmentManager=getSupportFragmentManager();
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
        fragment.poruka=poruka;

        fragmentTransaction.add(R.id.kontejner_fragmenta, fragment).commit();
    }

    public void skloniFragment(){
        if (!fragmentJePrikazan)
            return;

        if (fragment!=null){
            FragmentManager fragmentManager=getSupportFragmentManager();
            FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
            fragmentTransaction.remove(fragment).commit();
        }

        fragment=null;
        fragmentJePrikazan=false;
    }

    protected void onDestroy(){
        super.onDestroy();
        cijeliEkran.setOnTouchListener(null);
        prikazPrepiske.setOnTouchListener(null);
        napisanaPoruka.setOnTouchListener(null);
        imeAktivnogKorisnika.setOnTouchListener(null);
        strelica.setOnTouchListener(null);
        posaljiFajl.setOnTouchListener(null);
        uslikaj.setOnTouchListener(null);
        aktivniKontakt.aktivan=false;
        aktivniKontakt.ekran=null;
        aktivniKontakt=null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!fragmentJePrikazan)
            return false;

        skloniFragment();
        return true;
    }

    public void izaberiFajl(View view) {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,IZABERI_FAJL);
    }

    public void uslikaj(View view) {
        if (app.ispitajDaLiDokumentPostoji(Uri.parse(app.korijenskiFolderString), "BLABLAONICA")==null)
            app.blablaonica= app.kreirajFolderBlablaonica();

        vrijemeSlikanja= new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imeFotografije ="JPEG_"+ vrijemeSlikanja;
        app.slikaDocument= app.blablaonica.createFile("image/jpeg", imeFotografije);
        imeFotografije=app.slikaDocument.getName();
        if (app.slikaDocument==null)
            return;

        Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //provjeri da li ima app za kameru koja treba da odgovori na intent
        if (i.resolveActivity(getPackageManager())!=null){
            i.putExtra(MediaStore.EXTRA_OUTPUT, app.slikaDocument.getUri());
            startActivityForResult(i,USLIKAJ);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==USLIKAJ){
            if (app.slikaDocument.length()==0){
                //ako je fajl prazan, treba ga obrisati jer sta ce mi prazan fajl bez sadrzaja na telefonu
                app.slikaDocument.delete();
                return;
            }
            Uri uriSlike=app.slikaDocument.getUri();
            //ovaj metod se koristi ako je na telefonu instaliran Android 10 ili veci
            //laksi je, jer ne moram odredjivati ni rotaciju, ni nista
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                int maxSirina=(int)((app.visinaEkrana/2)/1.7);
                int maxVisina=app.visinaEkrana/2;
                try {
                    Bitmap thumbnail =
                            getApplicationContext().getContentResolver().loadThumbnail(
                                    uriSlike, new Size(maxSirina, maxVisina), null);
                    app.snimiBitmap(uriSlike,thumbnail);
                    //argument ponovnoSlanjeIstogFajla znaci da je ova slika vec ranije poslata,
                    //tako da vec imam kreiran bitmap,određenu njegovu rotaciju i snimljen je u dokument
                    //Zbog toga, ove korake treba preskočiti i samo dodati fajl u prepiske i poslati ga
                    posaljiSliku(imeFotografije, "image/jpeg", uriSlike);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            //dodajem fajl kao poruku u prepiske i saljem fajl
            //posaljiSliku(imeFotografije, uriSlike, true);
            Bitmap bitmap = kreirajBitmap(uriSlike);
            app.snimiBitmap(uriSlike,bitmap);
            posaljiSliku(imeFotografije, "image/jpeg", uriSlike);
        }

        if (requestCode==IZABERI_FAJL){
            if (data==null)
                return;
            final Uri uriFajla=data.getData();

            Cursor cursor = getContentResolver().query(uriFajla, null, null,
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                tip= cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE));
            }
            cursor.close();

            if (tip.split("/")[0].equals("image")){
                if (app.ispitajDaLiDokumentPostoji(Uri.parse(app.korijenskiFolderString), "BLABLAONICA")==null)
                    app.blablaonica= app.kreirajFolderBlablaonica();
                //kreiram novi dokument u kojem ce biti snimljen bitmap, kako bi originalna
                //fotografija bila sačuvana
                DocumentFile d;
                Uri noviUri=app.ispitajDaLiDokumentPostoji(app.blablaonica.getUri(),displayName);
                if (noviUri==null) {
                    d=app.blablaonica.createFile(tip,displayName);
                    Bitmap bitmap=kreirajBitmap(uriFajla);
                    app.snimiBitmap(d.getUri(),bitmap);
                    posaljiSliku(displayName, tip, d.getUri());
                }
                else{
                    posaljiSliku(displayName, tip, noviUri);
                }
                return;
            }

            if (tip.split("/")[0].equals("video")){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String staza= app.kreirajVideoBitmap(displayName,uriFajla);
                        date = new Date();
                        formater = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                        String poruka=tip+app.TAG+app.ime+app.TAG+uriFajla+app.TAG+formater.format(date)+app.TAG+staza+app.TAG+displayName;
                        dodajFajlUPrepiske(poruka, uriFajla, true);
                    }
                }).start();
                return;
            }

            posaljiFajl(uriFajla,tip,displayName);
        }
    }

    public void posaljiSliku(final String imeSlike, final String tip, final Uri uriFajla){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //dodaje se fajl u prepiske i salje serveru
                date = new Date();
                formater = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                String poruka=tip+app.TAG+app.ime+app.TAG+uriFajla+app.TAG+formater.format(date)+app.TAG+uriFajla+app.TAG+imeSlike;
                dodajFajlUPrepiske(poruka, uriFajla, true);
            }
        }).start();
    }

    public void posaljiFajl(final Uri uriFajla, final String tip, final String imeFajla){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String staza= uriFajla.toString();
                //*************************ova dva reda su nova, umjesto uriFajla koristim contentUri da bih zadrzao pristup
                //File newFile = new File(staza, imeFajla);
                //Uri contentUri = FileProvider.getUriForFile(app.getApplicationContext(), "com.example.blablaonica.fileprovider", newFile);

                date = new Date();
                formater = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                //String poruka=tip+app.TAG+app.ime+app.TAG+contentUri+app.TAG+formater.format(date)+app.TAG+staza+app.TAG+imeFajla;
                String poruka=tip+app.TAG+app.ime+app.TAG+uriFajla+app.TAG+formater.format(date)+app.TAG+staza+app.TAG+imeFajla;
                dodajFajlUPrepiske(poruka, uriFajla, true);
            }
        }).start();
    }

    public Bitmap kreirajBitmap(Uri uriSlike){
        Bitmap b;
        try {
            InputStream stream = getContentResolver().openInputStream(uriSlike);
            BufferedInputStream buff=new BufferedInputStream(stream);
            ExifInterface exif = new ExifInterface(buff);
            String orijentacija = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            int orientation = orijentacija != null ? Integer.parseInt(orijentacija) : ExifInterface.ORIENTATION_NORMAL;
            int rotationAngle = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationAngle = 270;
            }
            buff.close();

            if (rotationAngle!=0){
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                b= smanjiBitmap(uriSlike, rotationAngle);
                b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
            }
            else
                b=smanjiBitmap(uriSlike,0);

            b= Bitmap.createScaledBitmap(b, aktivniKontakt.konacnaSirina, aktivniKontakt.konacnaVisina,true);   //smanjujem sliku na tacno odredjene dimenzije pogleda
            return b;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Bitmap smanjiBitmap(Uri uriFajla, int ugao) throws IOException {
        //ucitavam bitmap iz strima u sadrzaj
        InputStream stream = getContentResolver().openInputStream(uriFajla);
        BufferedInputStream procitajFajl = new BufferedInputStream(stream);
        byte[] sadrzaj= new byte[procitajFajl.available()];
        procitajFajl.read(sadrzaj);
        procitajFajl.close();
        //samo ocitavam dimenzije bitmapa iz sadrzaja
        BitmapFactory.Options opcije = new BitmapFactory.Options();
        opcije.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(sadrzaj,0,sadrzaj.length,opcije);
        //odredjujem razmjer smanjenja slike i konacnu visinu i sirinu
        int sirinaSlike, visinaSlike;
        if (ugao==90 || ugao==270){
            sirinaSlike=opcije.outHeight;
            visinaSlike=opcije.outWidth;
        }
        else{
            sirinaSlike=opcije.outWidth;
            visinaSlike=opcije.outHeight;
        }
        int maxSirina=(int)((app.visinaEkrana/2)/1.7);
        float odnosSirina, odnosVisina;
        odnosSirina=(float) sirinaSlike/maxSirina;
        odnosVisina= (float) visinaSlike/(app.visinaEkrana/2);
        int maxOmjer;
        if (odnosSirina>odnosVisina){
            maxOmjer=(int)odnosSirina;
            aktivniKontakt.konacnaSirina=maxSirina;
            aktivniKontakt.konacnaVisina= Math.round(visinaSlike/odnosSirina);
        }
        else{
            Log.i("maksimalna je visina "," ");
            maxOmjer=(int)odnosVisina;
            aktivniKontakt.konacnaVisina=app.visinaEkrana/2;
            aktivniKontakt.konacnaSirina= Math.round (sirinaSlike/odnosVisina);
        }
        int konacanOmjer=app.pow2Ceil(maxOmjer);
        if (konacanOmjer<2)
            konacanOmjer=2;
        //kreiram smanjeni bitmap
        opcije.inSampleSize =konacanOmjer; // pow2Ceil: A utility function that comes later
        opcije.inJustDecodeBounds = false; // Decode bitmap with inSampleSize set
        Bitmap b= BitmapFactory.decodeByteArray(sadrzaj,0,sadrzaj.length,opcije);

        return b;
    }

    public void dodajFajlUPrepiske(final String poruka, final Uri uriFajla, boolean posalji){
        final String datum=poruka.split(app.TAG)[3].split(" ")[0];
        final String porukaZaAdapter;

        if (posalji){
            final String id=aktivniKontakt.broj+"_"+app.noviIDporuke();
            final StatusPoruke statusPoruke=new StatusPoruke(id);
            final String tipFajla=poruka.split(app.TAG)[0];
            final String imeFajla=poruka.split(app.TAG)[poruka.split(app.TAG).length-1];
            porukaZaAdapter=poruka+app.TAG+id;
            synchronized (aktivniKontakt.statusPoruka){
                aktivniKontakt.statusPoruka.add(statusPoruke);
                aktivniKontakt.snimiStatus(statusPoruke);
            }
            app.posaljiFajlServeru(aktivniKontakt, statusPoruke, id, poruka.split(app.TAG)[3],uriFajla, imeFajla, tipFajla);
        }
        else
            porukaZaAdapter=poruka;

        synchronized (aktivniKontakt){
            //Prvo se dodaje datum u prepisku, ako je potrebno
            if (!aktivniKontakt.datumPoruke.equals(datum)){
                aktivniKontakt.najnovijePoruke.add("datum"+app.TAG+datum);
                aktivniKontakt.sadrzajPrepiske.add("datum"+app.TAG+datum);
                noviDatum=true;
                aktivniKontakt.datumZaSnimanje=datum;
                aktivniKontakt.datumPoruke=datum;
            }
            //A onda i sama poruka
            aktivniKontakt.najnovijePoruke.add(porukaZaAdapter);
            aktivniKontakt.sadrzajPrepiske.add(porukaZaAdapter);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (noviDatum){
                        mojAdapter2.add("datum"+app.TAG+datum);
                        noviDatum=false;
                    }
                    mojAdapter2.add(porukaZaAdapter);
                    prikazPrepiske.setSelection(prikazPrepiske.getCount()-1);
                }
            });
            app.snimiNajnovijePoruke(aktivniKontakt);
        }
        //na kraju, ovaj kontakt podizem na prvo mjesto u listi kontakata
        synchronized (app.spisakKontakata){
            if (app.spisakKontakata.remove(aktivniKontakt))
                app.spisakKontakata.addFirst(aktivniKontakt);
        }
    }

    public void prikaziPodesavanja(View v){
        Intent novi=new Intent(getApplicationContext(), Settings.class);
        startActivity(novi);
    }

    private void odrediPozadinu(){
        SharedPreferences podesavanja;
        podesavanja = PreferenceManager.getDefaultSharedPreferences(this);
        String pozadina=podesavanja.getString("pozadina", null);

        if (pozadina==null)
            return;

        if (pozadina.equals("osnovna")){
            RelativeLayout cijeliEkran= findViewById(R.id.cijeliEkran);
            cijeliEkran.setBackground(ContextCompat.getDrawable(this, R.drawable.glavna_aktivnost_pozadina));
        }

        if (pozadina.equals("zvjezdanoNebo")){
            RelativeLayout cijeliEkran= findViewById(R.id.cijeliEkran);
            cijeliEkran.setBackground(ContextCompat.getDrawable(this, R.drawable.pozadina));
        }

        if (pozadina.equals("zelena")){
            RelativeLayout cijeliEkran= findViewById(R.id.cijeliEkran);
            cijeliEkran.setBackground(ContextCompat.getDrawable(this, R.drawable.pozadina_za_postavke));
        }
    }

    //      STATICKA UGNIJEŽĐENA KLASA
    static class Kontakt {
        MojaAplikacija app;
        EkranKontakta ekran;
        String ime, broj, datumPoruke, datumZaSnimanje;
        SadrzajPrepiske sadrzajPrepiske;  //sadrzace najvise 50 poruka u ramu
        LinkedList<String> najnovijePoruke;
        boolean aktivan;
        LinkedList<StatusPoruke> statusPoruka;
        int brojNeprocitanihPoruka, konacnaSirina, konacnaVisina, id;
        Notification summaryNotification;
        Intent notifikacijskiIntent;
        Bitmap slika;

        public Kontakt(MojaAplikacija app, String ime, String broj, int id){
            this.app=app;
            this.ime=ime;
            this.broj=broj;
            this.id=id;
            datumPoruke = "nema poruka";
            sadrzajPrepiske = new SadrzajPrepiske(app.BAFER);
            najnovijePoruke=new LinkedList<>();
            statusPoruka=new LinkedList<>();
            //Kreiranje intenta za notifikaciju. Kad se klikne na notifikaciju, ona ce
            //lansirati ovaj intent
            notifikacijskiIntent = new Intent(app.getApplicationContext(), EkranKontakta.class);
            notifikacijskiIntent.putExtra("ime", ime);
        }

        public String toString() {
            return ime;
        }

        public void snimiStatus(StatusPoruke status){
            File fajl;

            fajl=new File(app.folderKontakti,ime+"_"+broj+".txt");
            synchronized (this){
                try{
                    ObjectInputStream ucitajPrepisku= new ObjectInputStream(new FileInputStream(fajl));
                    Poruka prepiska=(Poruka) ucitajPrepisku.readObject();
                    ucitajPrepisku.close();

                    prepiska.statusPoruka.add(status);

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

        public void izmijeniStatus(StatusPoruke status){
            File fajl;
            fajl=new File(app.folderKontakti,ime+"_"+broj+".txt");

            synchronized (this) {
                try {
                    ObjectInputStream ucitajPrepisku = new ObjectInputStream(new FileInputStream(fajl));
                    Poruka prepiska = (Poruka) ucitajPrepisku.readObject();
                    ucitajPrepisku.close();

                    for (StatusPoruke s : prepiska.statusPoruka) {
                        if (s.id.equals(status.id)) {
                            s.status=status.status;
                            break;
                        }
                    }

                    ObjectOutputStream upisiObjekat = new ObjectOutputStream(new FileOutputStream(fajl));
                    upisiObjekat.writeObject(prepiska);
                    upisiObjekat.flush();
                    upisiObjekat.close();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }

        public void izbrisiStatus(StatusPoruke status){
            File fajl;
            fajl=new File(app.folderKontakti,ime+"_"+broj+".txt");

            synchronized (this){
                    try{
                        ObjectInputStream ucitajPrepisku= new ObjectInputStream(new FileInputStream(fajl));
                        Poruka prepiska=(Poruka) ucitajPrepisku.readObject();
                        ucitajPrepisku.close();

                        for (StatusPoruke s:prepiska.statusPoruka){
                            if (s.id.equals(status.id)){
                                prepiska.statusPoruka.remove(s);
                                break;
                            }
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

        public void snimiProfilnuFotografiju(byte[] slika){
            File fajl;
            fajl=new File(app.folderKontakti,this.ime+"_"+this.broj+".txt");

            synchronized (this) {
                try {
                    ObjectInputStream ucitajPrepisku = new ObjectInputStream(new FileInputStream(fajl));
                    Poruka prepiska = (Poruka) ucitajPrepisku.readObject();
                    ucitajPrepisku.close();

                    prepiska.slika= slika;

                    ObjectOutputStream upisiObjekat = new ObjectOutputStream(new FileOutputStream(fajl));
                    upisiObjekat.writeObject(prepiska);
                    upisiObjekat.flush();
                    upisiObjekat.close();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }
    }
}
