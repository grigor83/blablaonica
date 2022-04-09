package com.example.nova;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class FragmentDugogPritiska extends Fragment implements View.OnTouchListener {
    public FragmentDugogPritiska() {
        // Required empty public constructor
    }

    FragmentManager fragmentManager;
    TextView kopirajPoruku, proslijediPoruku;
    View rootView;
    String poruka;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView=inflater.inflate(R.layout.fragment_dugog_pritiska, container, false);
        fragmentManager=getFragmentManager();

        kopirajPoruku=rootView.findViewById(R.id.kopiraj_poruku);
        proslijediPoruku=rootView.findViewById(R.id.proslijedi_poruku);
        rootView.setOnTouchListener(this);
        kopirajPoruku.setOnTouchListener(this);
        proslijediPoruku.setOnTouchListener(this);

        return rootView;
    }

    public void onDestroyView() {
        super.onDestroyView();
        rootView.setOnTouchListener(null);
        kopirajPoruku.setOnTouchListener(null);
        proslijediPoruku.setOnTouchListener(null);
    }

    public void onDestroy() {
        super.onDestroy();
        fragmentManager=null;
        rootView=null;
        kopirajPoruku=null;
        proslijediPoruku=null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId()==R.id.layout_dugog_fragmenta)
            return true;
        Context context= rootView.getContext();
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData;
        EkranKontakta ekranKontakta=(EkranKontakta)context;

        if (event.getAction() == MotionEvent.ACTION_DOWN){
            if (v.getId()==R.id.kopiraj_poruku){
                String tag=((MojaAplikacija)ekranKontakta.getApplication()).TAG;
                clipData= ClipData.newPlainText("izabrana poruka",poruka.split(tag)[0]);
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(context, getString(R.string.kopiranje_poruke), Toast.LENGTH_LONG).show();
            }

            if (v.getId()==R.id.proslijedi_poruku) {
                Log.i("izabrao sam ", poruka);
                //ovdje ubaci kod za biranje kontakta iz moje aplikacije kome cu proslijediti poruku
                Intent i=new Intent(context.getApplicationContext(), GlavnaAktivnost.class);
                i.putExtra("proslijedi poruku", poruka);
                startActivity(i);
            }
            ekranKontakta.skloniFragment();
        }

        return true;
    }
}
