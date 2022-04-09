package com.example.nova;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;

public class PreferencaIme extends EditTextPreference {
    TextView imeTextView;
    String ime;

    public PreferencaIme(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        imeTextView =(TextView) holder.findViewById(R.id.imeTextView);
        if (ime!=null)
            imeTextView.setHint(ime);
        else
            imeTextView.setHint("Unesite svoje ime");
    }
}
