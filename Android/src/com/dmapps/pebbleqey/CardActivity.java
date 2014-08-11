package com.dmapps.pebbleqey;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class CardActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.card);
		
		//  GET EMAIL
		/*
		final AccountManager manager = AccountManager.get(this);
	    final Account[] accounts = manager.getAccountsByType("com.google");
	    final int size = accounts.length;
	    String[] names = new String[size];
	    for (int i = 0; i < size; i++) {
	    	names[i] = accounts[i].name;
	    	Log.e("NAME", names[i]);
	    }*/
		
		TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		String mPhoneNumber = tMgr.getLine1Number();
		
		// get the button resource in the xml file and assign it to a local variable of type Button
		Button button_save = (Button)findViewById(R.id.button_save);
		
		EditText editTextFirst = (EditText) findViewById(R.id.editTextFirst);
		EditText editTextLast = (EditText) findViewById(R.id.editTextLast);
		EditText editTextPhone = (EditText) findViewById(R.id.editTextPhone);
		
		
		editTextFirst.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		editTextLast.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		String f_temp = sp.getString("First", null);
		String l_temp = sp.getString("Last", null);
		String p_temp = sp.getString("Phone", null);
		
		if (f_temp != null) editTextFirst.setText(f_temp);
		if (l_temp != null) editTextLast.setText(l_temp);
		if (p_temp != null){
			editTextPhone.setText(p_temp);
		} else {
			editTextPhone.setText(mPhoneNumber);
		}

		button_save.setOnClickListener( new OnClickListener() {
			public void onClick(View viewParam)
			{

				EditText editTextFirst = (EditText) findViewById(R.id.editTextFirst);
				EditText editTextLast = (EditText) findViewById(R.id.editTextLast);
				EditText editTextPhone = (EditText) findViewById(R.id.editTextPhone);
				
				final String sFirst = editTextFirst.getText().toString();
				final String sLast = editTextLast.getText().toString();
				final String sPhone = editTextPhone.getText().toString();
								
				Intent output = getIntent();
				output.putExtra("First", sFirst);
				output.putExtra("Last", sLast);
				output.putExtra("Phone", sPhone);
				setResult(RESULT_OK, output);
				finish();
			}
		});
	}
}
