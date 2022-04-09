package com.example.nova;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
//import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class Postavke extends AppCompatActivity {
    final int IZABERI_INSTALACIONI_FOLDER =1;
    private MojaAplikacija app;
    SharedPreferences settings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.postavke);
        app=(MojaAplikacija) getApplication();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        ucitajDimenzijeEkrana();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.kontejnerPostavki, new FragmentPostavki())
                .commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
        System.exit(0);
    }

    private void ucitajDimenzijeEkrana(){
        app.sirinaEkrana=settings.getInt("sirinaEkrana",0);
        app.visinaEkrana=settings.getInt("visinaEkrana",0);

        if (app.sirinaEkrana==0 || app.visinaEkrana==0){
            //ako ne postoje dimenzije ekrana u preferencama, treba ih kreirati
            app.sirinaEkrana=getResources().getDisplayMetrics().widthPixels;
            app.visinaEkrana=getResources().getDisplayMetrics().heightPixels;
            //a onda i sacuvati
            SharedPreferences.Editor editor= settings.edit();
            editor.putInt("sirinaEkrana",app.sirinaEkrana);
            editor.putInt("visinaEkrana",app.visinaEkrana);
            editor.apply();
        }
        //definisanje login ekrana za manje ekrane
        if (app.sirinaEkrana<500){
            TextView naslov=findViewById(R.id.naslov);
            naslov.setTextSize(50);
            ImageView ikona=findViewById(R.id.logo);
            ikona.setVisibility(View.GONE);
        }
    }

    public void uloguj(View v){
        ucitajPostavke();
    }

    private void ucitajPostavke(){
        String ime,broj;
        ime=settings.getString("ime",null);
        broj=settings.getString("broj",null);
        if (ime!=null && broj!=null){
            app.ime=ime.trim();
            app.broj=broj.trim();

            if (app.korijenskiFolderString!=null){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        app.inicijalizacijaPrograma(0);
                        app.uspostaviKonekciju();
                    }
                }).start();

                finish();
            }
            else
                pribaviDozvole();
        }
    }

    private void pribaviDozvole(){
                                    //ZA VERZIJE ANDROIDA<10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            //trazim dozvolu za pisanje (samim tim i za citanje) na internoj memoriji telefona na
            //verzijama ispod Androida 10
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                else if (app.korijenskiFolderString==null){
                    //kreira se intent koji ce korisniku na Androidu<10 traziti dozvolu za instalaciju
                    //app u folderu DCIM
                    StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                    StorageVolume volume= null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        volume = sm.getPrimaryStorageVolume();
                        Intent i= volume.createAccessIntent(Environment.DIRECTORY_DCIM);
                        startActivityForResult(i, IZABERI_INSTALACIONI_FOLDER);
                    }
                }
            }
        }
                                    //ZA ANDROID 10 I VISE
        else{
            //Na Androidu 10 ne mogu da provjerim dozvole na isti nacin kao gore. Zbog toga moram da
            //ispitam da li postoji korijenski folder u preferencama. Na osnovu tog uslova cu odrediti
            //da li je potrebno prikazati dijalog za trazenje dozvola od korisnika
            if (app.korijenskiFolderString==null)
                prikaziInformativniDijalog();
        }
    }

    private void prikaziInformativniDijalog(){
        //ovaj metod prikazuje informativni dijalog korisniku kako bi on znao sta se trazi do njega
        final AlertDialog alert = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.izaberite_instalacioni_folder))
                .setPositiveButton("Select", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Ako korisnik potvrdi na select dugme,otvara se sistemski UI piker na cijeloj
                        // internoj memoriji telefona. Zatim korisnik bira folder u kojem ce se instalirati app
                        dialogInterface.dismiss();
                        StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                        StorageVolume volume = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            volume = sm.getPrimaryStorageVolume();
                        }
                        //nakon ovog ce se korisniku prikazati UI piker, a ostatak koda je u onActivityResult
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startActivityForResult(volume.createOpenDocumentTreeIntent(), IZABERI_INSTALACIONI_FOLDER);
                        }
                    }
                })
                .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast t= Toast.makeText(Postavke.this, getString(R.string.morate_omoguciti_dozvole),Toast.LENGTH_SHORT);
                        t.setGravity(Gravity.CENTER, 0, 0);
                        t.show();
                        dialogInterface.dismiss();
                        finishAffinity();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1000);
                                    System.exit(0);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }).show();
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Dozvole su omogucene!
                //kreira se intent koji ce korisniku na Androidu<10 traziti dozvolu za instalaciju
                //app u folderu DCIM
                StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                StorageVolume volume = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    volume = sm.getPrimaryStorageVolume();
                    Intent i = volume.createAccessIntent(Environment.DIRECTORY_DCIM);
                    startActivityForResult(i, IZABERI_INSTALACIONI_FOLDER);
                }

            } else {
                // permission denied, boo! Disable the
                Toast t = Toast.makeText(this, getString(R.string.morate_omoguciti_dozvole), Toast.LENGTH_SHORT);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
                finishAffinity();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            System.exit(0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
        if (requestCode == 2) {
            app.ucitajImenik();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    app.inicijalizacijaPrograma(1);
                    app.uspostaviKonekciju();
                }
            }).start();

            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IZABERI_INSTALACIONI_FOLDER) {
            if (data == null) {
                Toast t= Toast.makeText(this, getString(R.string.upozorenje_instalacioni_folder),Toast.LENGTH_SHORT);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
                finishAffinity();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            System.exit(0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return;
            }
            //grantUriPermission(getPackageName(), data.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            //pribavlja se trajna dozvola za citanje i pisanje u izabranom folderu i svemu sto on sadrzi
            getContentResolver().takePersistableUriPermission(data.getData(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            //onda se uri tog korijenskog foldera koji je korisnik izabrao memorise kao string
            //on ce mi kasnije sluziti za sve operacije citanja i pisanja fajlova u folderu blablaonica,
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, data.getData());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("korijenski_folder", pickedDir.getUri().toString());
            editor.apply();

            app.korijenskiFolderString = settings.getString("korijenski_folder", null);
            app.kreirajFolderBlablaonica();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},2);
            }
        }
    }

    //staticka ugnjezdjena klasa fragmenta za prikaz postavki
    public static class FragmentPostavki extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener{
        private PreferencaIme ime;
        private PreferencaBroj broj;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prilagodjene_preference, rootKey);

            ime =(PreferencaIme) findPreference("ime");
            ime.setOnPreferenceChangeListener(this);
            broj= (PreferencaBroj) findPreference("broj");
            broj.setOnPreferenceChangeListener(this);
            //u slucaju da su vec unesena neka podesavanja, prikazuje ih
            SharedPreferences podesavanja= PreferenceManager.getDefaultSharedPreferences(getContext());
            if (podesavanja.contains("ime"))
                ime.ime=podesavanja.getString("ime",null);
            if (podesavanja.contains("broj"))
                broj.broj=podesavanja.getString("broj",null);

            broj.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                }
            });

            ime.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                }
            });
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String s=newValue.toString();
            if (s!=null){
                if (preference.getClass()==PreferencaIme.class)
                    ime.ime=s;
                if (preference.getClass()==PreferencaBroj.class)
                    broj.broj=s;
                return true;
            }
            else
                return false;
        }

        public void onDestroy(){
            super.onDestroy();
            ime.setOnPreferenceChangeListener(null);
            broj.setOnPreferenceChangeListener(null);
        }
    }
}