package com.example.nova;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Settings extends AppCompatActivity {
    private ImageView profilnaSlika;
    private TextView ime, broj;
    private MojaAplikacija app;
    private File fajl;
    private final int USLIKAJ_PROFILNU_SLIKU=100;
    private int konacnaSirina, konacnaVisina;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        app=(MojaAplikacija) getApplication();
        profilnaSlika=findViewById(R.id.profilna_slika);
        ime=findViewById(R.id.ime);
        broj=findViewById(R.id.broj);

        if (app.fotografija!=null){
            RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(getResources(), app.fotografija);
            roundDrawable.setCornerRadius(70);
            profilnaSlika.setImageDrawable(roundDrawable);
        }
        ime.setText(app.ime);
        broj.setText(app.broj);
    }

    public void promijeniProfilnuSliku(View v){
        fajl=new File(app.folderKontakti,"fotografija");
        if (!fajl.exists()){
            try {
                fajl.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //provjeri da li ima app za kameru koja treba da odgovori na intent
        if (i.resolveActivity(getPackageManager())!=null) {
            Uri uri = FileProvider.getUriForFile(this, "com.example.blablaonica.fileprovider", fajl);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(i, USLIKAJ_PROFILNU_SLIKU);
        }
    }

    public void promijeniPozadinu(View v){
        Intent i=new Intent(getApplicationContext(), IzaberiPozadinu.class);
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==USLIKAJ_PROFILNU_SLIKU && resultCode==RESULT_OK){
            if (fajl.length()==0){
                return;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    app.fotografija=kreirajBitmap(Uri.fromFile(fajl));
                    app.snimiBitmap(Uri.fromFile(fajl), app.fotografija);
                    final RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(getResources(), app.fotografija);
                    roundDrawable.setCornerRadius(70);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            profilnaSlika.setImageDrawable(roundDrawable);
                        }
                    });
                    try {
                        InputStream i= getContentResolver().openInputStream(Uri.fromFile(fajl));
                        BufferedInputStream buf=new BufferedInputStream(i);
                        byte[] sadrzaj= new byte[buf.available()];
                        buf.read(sadrzaj);
                        buf.close();
                        app.glavniIzlaz.writeObject(sadrzaj);
                        app.glavniIzlaz.flush();
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
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

            b= Bitmap.createScaledBitmap(b, konacnaSirina, konacnaVisina,true);   //smanjujem sliku na tacno odredjene dimenzije pogleda
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
            konacnaSirina=maxSirina;
            konacnaVisina= Math.round(visinaSlike/odnosSirina);
        }
        else{
            Log.i("maksimalna je visina "," ");
            maxOmjer=(int)odnosVisina;
            konacnaVisina=app.visinaEkrana/2;
            konacnaSirina= Math.round (sirinaSlike/odnosVisina);
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

    public void otvoriSliku(View view) {
        fajl=new File(app.folderKontakti,"fotografija");
        if (!fajl.exists()){
            try {
                fajl.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Uri uri= FileProvider.getUriForFile(this, "com.example.blablaonica.fileprovider", fajl);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "image/*");
        i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);
    }
}
