package com.dmapps.pebbleqey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.SharedPreferences;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.GenericTypeIndicator;
import com.firebase.client.ValueEventListener;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.location.Address;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.RawContacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

	ViewPager mViewPager;
	private static BluetoothAdapter btAdapter;
	private ArrayList<BluetoothDevice> btDeviceList = new ArrayList<BluetoothDevice>();
	static ArrayList<String> btDeviceNameList = new ArrayList<String>();
	static ArrayList<String> btDeviceMACList = new ArrayList<String>();
	private static ArrayAdapter<String> listAdapter;
	private ViewGroup mContainerView;
	TextView t;
	ListView lv;
	static String first = null;
	static String last = null;
	static String phoneNumber = null;	
	String deviceName = "";
	String deviceMAC = "";
	static int add_flag = 0;
	//private final static UUID PEBBLE_APP_UUID = UUID.fromString("3fc7d920-ddf7-41c3-905d-026fe50f128d"); //Msg
	
	private final static UUID PEBBLE_APP_UUID = UUID.fromString("169deece-d033-4199-8a51-250f5d06b58a"); //UI
	//private final static UUID PEBBLE_APP_UUID = UUID.fromString("57d415e2-d682-4e7b-a58e-d69c1676f154"); //PB
	
	private Set<BluetoothDevice>pairedDevices;
	static String A_P_ID = "";
	
	static Boolean mReturningWithResult = false;
	
	//firebase
	private Firebase db_ref = new Firebase("https://crackling-fire-9839.firebaseio.com");
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_button);
		
		mContainerView = (ViewGroup) findViewById(R.id.container);
		TextView textViewFirst = (TextView) findViewById(R.id.textViewFirst);
		TextView textViewLast = (TextView) findViewById(R.id.textViewLast);
		TextView textViewPhone = (TextView) findViewById(R.id.textViewPhone);
		ImageButton button_edit = (ImageButton)findViewById(R.id.imageButtonEdit);     
		
		button_edit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent_card = new Intent(MainActivity.this, CardActivity.class);
				startActivityForResult(intent_card,1);
			}
		});
		
		// SP METHOD
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		// Contact Card
		first = sp.getString("First", null);
		last = sp.getString("Last", null);
		phoneNumber = sp.getString("Phone", null);
		
		if ((first == null) || (last == null) || (phoneNumber == null)) {			
			//Intent
			Intent intent_card = new Intent(this, CardActivity.class);
			startActivityForResult(intent_card,1);
		}
		
		textViewFirst.setText(first);
		textViewLast.setText(last);
		textViewPhone.setText(phoneNumber);
		
		// BLUETOOTH SEGMENT
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_UUID);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(ActionFoundReceiver, filter);
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();

		btDeviceNameList.clear();
		btDeviceMACList.clear();
			
		pairedDevices = btAdapter.getBondedDevices();
		for(BluetoothDevice bt : pairedDevices)	{
			if( bt.getName().startsWith("Pebble ")){ //makes sure that it is a pebble
				Log.e("Paired", bt.getName() + " " + bt.getAddress());  // Gets Paired BT Unique ID
				A_P_ID = bt.getAddress();
			}
		}
		
		StartBTState();
		
		PebbleKit.registerPebbleConnectedReceiver(getApplicationContext(), new BroadcastReceiver() {
			  @Override
			  public void onReceive(Context context, Intent intent) {
			    Log.i("Pebble", "Pebble connected!");
			  }
			});

			PebbleKit.registerPebbleDisconnectedReceiver(getApplicationContext(), new BroadcastReceiver() {
			  @Override
			  public void onReceive(Context context, Intent intent) {
			    Log.i("Pebble", "Pebble disconnected!");
			  }
			});
			
		PebbleDictionary data = new PebbleDictionary();
		data.addUint8(0, (byte) 42);
		data.addString(1, "a string");
		
		

		final Handler handler = new Handler();
		PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
		    @Override
		    public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
		    	btAdapter.startDiscovery();
		    	Log.i("Received", "Received value=" + data.getUnsignedInteger(0) + " for key: 0");
		    	Log.i("Received", "Received value=" + data.getString(1) + " for key: 1");
		    	
		    	PebbleDictionary data2 = new PebbleDictionary();
				data2.addUint8(0, (byte) 42);
				data2.addString(1, "a string");
				
		    	//PebbleKit.sendDataToPebbleWithTransactionId(getApplicationContext(), PEBBLE_APP_UUID, data2, 42);
		    	if (data.getString(1).equals("Pebbleshake")) {
		    		
		    		
		    		///ZY STUFF HERE
		    		
		    		btDeviceMACList.remove(A_P_ID);
		    		Log.e("BT Seen", btDeviceMACList.toString());  // Gets Paired BT Unique ID
		    		this.addData(A_P_ID, System.currentTimeMillis()/1000+"", first+" "+last, phoneNumber, btDeviceMACList);
		    		
		    		/// END ZY
		    	}
		    	PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);


		    }
		    public void addData(String PID, String time, String Name, String num,
					ArrayList<String> pairs) {
				db_ref.child(PID).addChildEventListener(new ChildEventListener() {
					@Override
					public void onChildAdded(DataSnapshot snapshot,
							String previousChildName) {
						String PID = snapshot.getName();

						if (PID == "pid_pairs") {
							ArrayList<String> Pid_list = (ArrayList<String>) snapshot
									.getValue();
							match(Pid_list);
						}

					}

					@Override
					public void onChildChanged(DataSnapshot snapshot,
							String previousChildName) {
						String PID = snapshot.getName();
						if (snapshot.getName().equals("pair")) {

							System.out.println(snapshot.getName());
							db_ref.child(A_P_ID).addListenerForSingleValueEvent(
									new ValueEventListener() {
										@Override
										public void onDataChange(DataSnapshot snapshot) {
											if (snapshot.getValue() != null) {

												String pair_info = (String) ((Map) snapshot
														.getValue()).get("pair");

												Log.e("pair info", pair_info);
												final String[] pair_info_split = pair_info
														.split(";");

												if (pair_info_split[0].equals("second")) {
													// delete me
													// ADD TO CONTACTS RAW VERSION
													ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
										            int rawContactInsertIndex = ops.size();
										
										            ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
										                    .withValue(RawContacts.ACCOUNT_TYPE, null)
										                    .withValue(RawContacts.ACCOUNT_NAME, null).build());
										            ops.add(ContentProviderOperation
										                    .newInsert(Data.CONTENT_URI)
										                    .withValueBackReference(Data.RAW_CONTACT_ID,rawContactInsertIndex)
										                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
										                    .withValue(StructuredName.DISPLAY_NAME, pair_info_split[2]) // Name of the person
										                    .build());
										            ops.add(ContentProviderOperation
										                    .newInsert(Data.CONTENT_URI)
										                    .withValueBackReference(
										                            ContactsContract.Data.RAW_CONTACT_ID,   rawContactInsertIndex)
										                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
										                    .withValue(Phone.NUMBER, pair_info_split[3]) // Number of the person
										                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build()); // Type of mobile number                    
										            try {
										                ContentProviderResult[] res = getBaseContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
										            }
										            catch (RemoteException e)
										            { 
										                // error
										            }
										            catch (OperationApplicationException e) 
										            {
										                // error
										            }
													
													///END ADD CONTACT
													// contact list thing
													Log.e("contact added","CONTACT ADDED!");
													db_ref.child(A_P_ID).removeValue();
												} else {
													
													//SEND TO PHONE FOR PROMPT
													PebbleDictionary data2 = new PebbleDictionary();
													data2.addUint8(0, (byte) 42);
													data2.addString(1, first+" "+last);
													Log.e("Sending","Sending");
											    	PebbleKit.sendDataToPebbleWithTransactionId(getBaseContext(), PEBBLE_APP_UUID, data2, 42);
											    	add_flag = 0;
											    	PebbleKit.registerReceivedDataHandler(getApplicationContext(), new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
													    @Override
													    public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
													    	Log.i("Received", "Received value=" + data.getUnsignedInteger(0) + " for key: 0");
													    	Log.i("Received", "Received value=" + data.getString(1) + " for key: 1");
													    	if (data.getString(1).length() > 0){
														    	if (data.getString(1).equals("yes") && add_flag == 0) {
														    		add_flag = 1;
																	db_ref.child(pair_info_split[1])
																			.child("pair")
																			.setValue(
																					"second;"
																							+ A_P_ID
																							+ ";"
																							+ first+" "+last
																							+ ";"
																							+ phoneNumber);
																	// contact list thing
																	Log.e("contact added","CONTACT ADDED!");
																	db_ref.child(A_P_ID).removeValue();
																	
																	//pair_info_split[2];//name
																	//pair_info_split[3];//number
																	///ADDD CONTACT
																	
																	// ADD TO CONTACTS RAW VERSION
																	ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
														            int rawContactInsertIndex = ops.size();
														
														            ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
														                    .withValue(RawContacts.ACCOUNT_TYPE, null)
														                    .withValue(RawContacts.ACCOUNT_NAME, null).build());
														            ops.add(ContentProviderOperation
														                    .newInsert(Data.CONTENT_URI)
														                    .withValueBackReference(Data.RAW_CONTACT_ID,rawContactInsertIndex)
														                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
														                    .withValue(StructuredName.DISPLAY_NAME, pair_info_split[2]) // Name of the person
														                    .build());
														            ops.add(ContentProviderOperation
														                    .newInsert(Data.CONTENT_URI)
														                    .withValueBackReference(
														                            ContactsContract.Data.RAW_CONTACT_ID,   rawContactInsertIndex)
														                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
														                    .withValue(Phone.NUMBER, pair_info_split[3]) // Number of the person
														                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build()); // Type of mobile number                    
														            try {
														                ContentProviderResult[] res = getBaseContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
														            }
														            catch (RemoteException e)
														            { 
														                // error
														            }
														            catch (OperationApplicationException e) 
														            {
														                // error
														            }
																	
																	///END ADD CONTACT
																} else if(data.getString(1).equals("no")) {
																	Log.e("contact not added","CONTACT NOT ADDED!");
																	// delete me and other person
																	db_ref.child(pair_info_split[1])
																			.removeValue();
																	db_ref.child(A_P_ID)
																			.removeValue();													    		
														    	}
													    	PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
													    	}
													    	


													    }
											    	});
												}
												return;
											}

										}

										@Override
										public void onCancelled(FirebaseError arg0) {
											// TODO Auto-generated method stub

										}
									});

						}
					}

					@Override
					public void onChildRemoved(DataSnapshot snapshot) {

					}

					@Override
					public void onChildMoved(DataSnapshot snapshot,
							String previousChildName) {

					}

					@Override
					public void onCancelled(FirebaseError arg0) {
						// TODO Auto-generated method stub

					}
				});
				Map<String, Object> entry = new HashMap<String, Object>();
				entry.put("PID", PID);
				entry.put("Time", time);
				entry.put("pair", "");
				entry.put("Name", Name);
				entry.put("Number", num);
				entry.put("pid_pairs", pairs);
				db_ref.child(PID).setValue(entry);

			}

			public void match(ArrayList<String> PIDs) {
				for (int i = 0; i < PIDs.size(); i++) {
					String pid = PIDs.get(i);
					db_ref.child(pid).addListenerForSingleValueEvent(
							new ValueEventListener() {
								@Override
								public void onDataChange(DataSnapshot snapshot) {
									if (snapshot.getValue() != null) {

										String PID_match = (String) ((Map) snapshot
												.getValue()).get("PID");
										db_ref.child(PID_match)
												.child("pair")
												.setValue(
														"first;" + A_P_ID + ";" + first+" "+last
																+ ";" + phoneNumber);

										return;
									}

								}

								@Override
								public void onCancelled(FirebaseError arg0) {
									// TODO Auto-generated method stub

								}
							});
					return;
				}

			}
		});
		
	}
	

	private static void CheckBTState() {
		if (btAdapter == null) {
			return;
		} else {
			if (btAdapter.isEnabled()) {
				//Log.e("CheckBTState","BT ON");
			} else {
				//Log.e("CheckBTState","BT OFF");
				btAdapter.enable();
			}

		}
	}
	
	private static void StartBTState() {
		if (btAdapter == null) {
			return;
		} else {
			if (btAdapter.isEnabled()) {
				btAdapter.startDiscovery();
			} else {
				//Log.e("CheckBTState","BT OFF");
				btAdapter.enable();
				CheckBTState();
			}

		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
	    switch(keyCode) {
	        case KeyEvent.KEYCODE_BACK:
	            moveTaskToBack(true);
	            return true;
	    }
	    return false;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if(resultCode == RESULT_OK){
				first = data.getStringExtra("First");				
				last = data.getStringExtra("Last");
				phoneNumber = data.getStringExtra("Phone");
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				SharedPreferences.Editor speditor = sp.edit();
				speditor.putString("First", first);
				speditor.putString("Last", last);
				speditor.putString("Phone", phoneNumber);
				speditor.commit();
				mReturningWithResult = true;				
			}
			if (resultCode == RESULT_CANCELED) {    
				//Write your code if there's no result
				Log.e("OAR", "FALSE");
			}
		}
	}
	
	
	private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			//Log.e("onReceive", action);

			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// Add the name and address to an array adapter to show in a ListView
				Log.e("ACTION FOUND", device.getName() + " " + device.getAddress());

				deviceName = device.getName();
				deviceMAC = device.getAddress();
				if (deviceName == null){
					deviceName = "Blank";
				}
				
				if (deviceName.startsWith("Pebble ")){
					deviceName = deviceName.substring(1);
						
					boolean found = false;
					for(String mac:btDeviceMACList)
					{
						if(mac.equals(deviceMAC))
						{
							found = true;
							break;
						}
					}
					if(!found)
					{
						btDeviceNameList.add(deviceName);
						btDeviceMACList.add(deviceMAC);
					}
				}
				else if( deviceName.startsWith("CPebb "))
				{
					String deviceMac = deviceName.substring(6);
					boolean found = false;
					for(String mac:btDeviceMACList)
					{
						if(mac.equals(deviceMac))
						{
							found = true;
							break;
						}
					}
					if(!found)
					{
						btDeviceMACList.add(deviceMac);
					}
				
				}


			}


		}
	};
}


