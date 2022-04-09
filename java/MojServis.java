package com.example.nova;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.LinkedList;

public class MojServis extends IntentService {

    public MojServis() {
        super("moj servis");
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        MojaAplikacija app=(MojaAplikacija) getApplication();
        if (app.konektovan)
            return;

        SocketAddress sockaddr = new InetSocketAddress("31.223.220.67", 20000);
        int timeout = 5000;

        while(true){
            Log.i("\n**************** POKUSAVAM  DA  USPOSTAVIM  KONEKCIJU  SA SERVEROM  "," ");
            try{
                // Creates an unconnected socket
                app.glavniSoket=new Socket();
                app.glavniSoket.connect(sockaddr, timeout);

                app.glavniIzlaz=new ObjectOutputStream(app.glavniSoket.getOutputStream());
                app.glavniUlaz=new ObjectInputStream(app.glavniSoket.getInputStream());
                app.glavniIzlaz.writeUTF(app.ime);
                app.glavniIzlaz.writeUTF(app.broj);
                LinkedList<String> imenik=new LinkedList<>();
                for (EkranKontakta.Kontakt kontakt:app.spisakKontakata)
                    imenik.add(kontakt.broj);
                app.glavniIzlaz.writeObject(imenik);
                app.glavniIzlaz.flush();
                app.posaljiNeposlatePoruke();
                app.neprestanoCitajPoruke();
                //uspostavlja se sporedna konekcija za slanje fajlova
                app.sporedniSoket=new Socket();
                app.sporedniSoket.connect(sockaddr,timeout);

                app.sporedniIzlaz=new ObjectOutputStream(app.sporedniSoket.getOutputStream());
                app.sporedniUlaz=new ObjectInputStream(app.sporedniSoket.getInputStream());
                app.sporedniIzlaz.writeUTF(app.ime+"_fajl");
                app.sporedniIzlaz.writeUTF(app.broj);
                app.sporedniIzlaz.flush();
                app.neprestanoCitajFalove();
                app.konektovan=true;
                Log.i("*************Konekcija"," je uspostavljena************");
                break;
            }
            catch (IOException e) {
                Log.i("\n********KONEKCIJA  NIJE  USPOSTAVLJENA \n"," ");
                app.konektovan=false;
            }
        }

    }
}
