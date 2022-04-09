package com.example.nova;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;

public class PreferencaBroj extends EditTextPreference {
    TextView brojTextView;
    String broj;

    public PreferencaBroj(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        brojTextView=(TextView) holder.findViewById(R.id.brojTextView);
        if (broj!=null)
            brojTextView.setHint(broj);
        else
            brojTextView.setHint("Unesite svoj broj");
    }
}
