package com.openvehicles.OVMS.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.luttu.AppPrefes;
import com.openvehicles.OVMS.R;
import com.openvehicles.OVMS.api.ApiService;
import com.openvehicles.OVMS.api.OnResultCommandListener;
import com.openvehicles.OVMS.entities.CarData;
import com.openvehicles.OVMS.entities.CarData.DataStale;
import com.openvehicles.OVMS.ui.settings.CarInfoFragment;
import com.openvehicles.OVMS.ui.settings.FeaturesFragment;
import com.openvehicles.OVMS.ui.settings.GlobalOptionsFragment;
import com.openvehicles.OVMS.ui.settings.LogsFragment;
import com.openvehicles.OVMS.ui.utils.Ui;
import com.openvehicles.OVMS.utils.CarsStorage;

public class CarFragment extends BaseFragment implements OnClickListener, OnResultCommandListener {
	private static final String TAG = "CarFragment";

	private CarData mCarData;
	private String uiCarType = "";
	Menu optionsMenu;
	AppPrefes appPrefes;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// init car data:
		mCarData = CarsStorage.get().getSelectedCarData();

		appPrefes = new AppPrefes(getActivity(), "ovms");
		
		// inflate layout:
		View rootView = inflater.inflate(R.layout.fragment_car, null);

		// set the car background image:
		ImageView iv = (ImageView) rootView.findViewById(R.id.tabCarImageCarOutline);
		iv.setImageResource(Ui.getDrawableIdentifier(getActivity(), "ol_"+mCarData.sel_vehicle_image));

		setHasOptionsMenu(true);

		return rootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		uiCarType = "";
	}

	@Override
	public void onResume() {
		super.onResume();
		setupCarType(mCarData);
	}


	/**
	 * setupCarType: apply car specific UI changes
	 *
	 * @param pCarData
	 */
	private void setupCarType(CarData pCarData) {

		ImageView img1, img2;
		TextView label;

		Log.d(TAG, "updateCarType: old=" + uiCarType + ", new=" + pCarData.car_type);
		if (uiCarType.equals(pCarData.car_type))
			return;

		img1 = (ImageView) findViewById(R.id.tabCarImageHomelink);
		if (pCarData.car_type.equals("RT")) {
			// UI changes for Renault Twizy:

			// exchange "Homelink" by "Profile":
			if (img1 != null)
				img1.setImageResource(R.drawable.ic_drive_profile);
			label = (TextView) findViewById(R.id.txt_homelink);
			if (label != null)
				label.setText(R.string.textPROFILE);
		}
		else if (pCarData.car_type.equals("RZ")) {
			// UI changes for Renault ZOE:

			// change "Homelink" image:
			if (img1 != null)
				img1.setImageResource(R.drawable.homelinklogo_zoe);
		} else {
			img1.setImageResource(R.drawable.ic_home_link);
		}

		//
		// Configure A/C button:
		//

		img1 = (ImageView) findViewById(R.id.tabCarImageCarACBoxes);
		img2 = (ImageView) findViewById(R.id.tabCarImageAC);

		// The V3 framework does not support capabilities yet, but
		//	the Leaf and Smart is the only car providing command 26 up to now, so:
		if (pCarData.hasCommand(26) || pCarData.car_type.equals("NL") || pCarData.car_type.equals("SE")) {
			// enable
			img1.setVisibility(View.VISIBLE);
			img2.setVisibility(View.VISIBLE);
			if ((pCarData.car_doors5_raw & 0x80) > 0) {
				img2.setImageResource(R.drawable.ic_ac_on);
			} else {
				img2.setImageResource(R.drawable.ic_ac_off);
			}
		} else {
			// disable
			img1.setVisibility(View.INVISIBLE);
			img2.setVisibility(View.INVISIBLE);
		}


		uiCarType = pCarData.car_type;

		// request menu recreation:
		getCompatActivity().invalidateOptionsMenu();
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.car_options, menu);

		optionsMenu = menu;

		// set checkbox:
		optionsMenu.findItem(R.id.mi_show_fahrenheit)
				.setChecked(appPrefes.getData("showfahrenheit").equals("on"));
		optionsMenu.findItem(R.id.mi_show_tpms_bar)
				.setChecked(appPrefes.getData("showtpmsbar").equals("on"));

		if (uiCarType.equals("RT")) {
			// Menu setup for Renault Twizy:
			optionsMenu.findItem(R.id.mi_power_stats).setVisible(true);
			optionsMenu.findItem(R.id.mi_battery_stats).setVisible(true);
			optionsMenu.findItem(R.id.mi_show_diag_logs).setVisible(true);
		} else {
			// defaults:
			optionsMenu.findItem(R.id.mi_power_stats).setVisible(false);
			optionsMenu.findItem(R.id.mi_battery_stats).setVisible(false);
			optionsMenu.findItem(R.id.mi_show_diag_logs).setVisible(false);
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int menuId = item.getItemId();
		boolean newState = !item.isChecked();
		Bundle args;


		switch (menuId) {

			case R.id.mi_power_stats:
				BaseFragmentActivity.show(getActivity(), PowerFragment.class, null,
						Configuration.ORIENTATION_UNDEFINED);
				return true;

			case R.id.mi_battery_stats:
				BaseFragmentActivity.show(getActivity(), BatteryFragment.class, null,
						Configuration.ORIENTATION_UNDEFINED);
				return true;

			case R.id.mi_show_carinfo:
				BaseFragmentActivity.show(getActivity(), CarInfoFragment.class, null,
						Configuration.ORIENTATION_UNDEFINED);
				return true;

			case R.id.mi_aux_battery_stats:
				BaseFragmentActivity.show(getActivity(), AuxBatteryFragment.class, null,
						Configuration.ORIENTATION_UNDEFINED);
				return true;

			case R.id.mi_show_features:
				BaseFragmentActivity.show(getActivity(), FeaturesFragment.class, null,
						Configuration.ORIENTATION_UNDEFINED);
				return true;

			case R.id.mi_show_diag_logs:
				BaseFragmentActivity.show(getActivity(), LogsFragment.class, null,
						Configuration.ORIENTATION_UNDEFINED);
				return true;

			case R.id.mi_show_fahrenheit:
				appPrefes.SaveData("showfahrenheit", newState ? "on" : "off");
				item.setChecked(newState);
				return true;
			
			case R.id.mi_show_tpms_bar:
				appPrefes.SaveData("showtpmsbar", newState ? "on" : "off");
				item.setChecked(newState);
				return true;

			case R.id.mi_globaloptions:
				BaseFragmentActivity.show(getActivity(), GlobalOptionsFragment.class, null,
						Configuration.ORIENTATION_UNDEFINED);
				return true;

			default:
				return false;
		}
	}


	@Override
	public void update(CarData pCarData) {
		mCarData = pCarData;
		setupCarType(pCarData);
		updateLastUpdatedView(pCarData);
		updateCarBodyView(pCarData);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(findViewById(R.id.btn_wakeup));
		registerForContextMenu(findViewById(R.id.txt_homelink));
		registerForContextMenu(findViewById(R.id.tabCarImageHomelink));
		registerForContextMenu(findViewById(R.id.tabCarImageAC));

		findViewById(R.id.btn_lock_car).setOnClickListener(this);
		findViewById(R.id.btn_valet_mode).setOnClickListener(this);

		findViewById(R.id.tabCarText12V).setOnClickListener(this);
		findViewById(R.id.tabCarText12VLabel).setOnClickListener(this);
	}
	
	@Override
	public void registerForContextMenu(View view) {
		super.registerForContextMenu(view);
		view.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		if (mCarData == null) return;

		final int dialogTitle, dialogButton;
		boolean isPinEntry;

		switch (v.getId()) {

			case R.id.btn_lock_car:

				// get dialog mode & labels:
				if (mCarData.car_type.equals("RT")) {
					dialogTitle = R.string.lb_lock_mode_twizy;
					dialogButton = mCarData.car_locked
							? (mCarData.car_valetmode
								? R.string.lb_valet_mode_extend_twizy
								: R.string.lb_unlock_car_twizy)
							: R.string.lb_lock_car_twizy;
					isPinEntry = false;
				} else {
					dialogTitle = mCarData.car_locked ? R.string.lb_unlock_car : R.string.lb_lock_car;
					dialogButton = dialogTitle;
					isPinEntry = true;
				}

				// show dialog:
				Ui.showPinDialog(getActivity(), dialogTitle, dialogButton, isPinEntry,
						new Ui.OnChangeListener<String>() {
							@Override
							public void onAction(String pData) {
								String cmd;
								int resId;
								if (mCarData.car_locked) {
									resId = dialogButton;
									cmd = "22," + pData;
								} else {
									resId = dialogButton;
									cmd = "20," + pData;
								}
								sendCommand(resId, cmd, CarFragment.this);
							}
						});
				break;

			case R.id.btn_valet_mode:

				// get dialog mode & labels:
				if (mCarData.car_type.equals("RT")) {
					dialogTitle = R.string.lb_valet_mode_twizy;
					dialogButton = mCarData.car_valetmode
							? (mCarData.car_locked
								? R.string.lb_unvalet_unlock_twizy
								: R.string.lb_valet_mode_off_twizy)
							: R.string.lb_valet_mode_on_twizy;
					isPinEntry = false;
				} else if(mCarData.car_type.equals("SE")) {
					dialogTitle = R.string.lb_valet_mode_smart;
					dialogButton = mCarData.car_valetmode ? R.string.lb_valet_mode_smart_off : R.string.lb_valet_mode_smart_on;
					isPinEntry = true;
				} else {
					dialogTitle = R.string.lb_valet_mode;
					dialogButton = mCarData.car_valetmode ? R.string.lb_valet_mode_off : R.string.lb_valet_mode_on;
					isPinEntry = true;
				}

				// show dialog:
				Ui.showPinDialog(getActivity(), dialogTitle, dialogButton, isPinEntry,
						new Ui.OnChangeListener<String>() {
							@Override
							public void onAction(String pData) {
								String cmd;
								int resId;
								if (mCarData.car_valetmode) {
									resId = dialogButton;
									cmd = "23," + pData;
								} else {
									resId = dialogButton;
									cmd = "21," + pData;
								}
								sendCommand(resId, cmd, CarFragment.this);
							}
						});
				break;

			case R.id.tabCarText12V:
			case R.id.tabCarText12VLabel:
				BaseFragmentActivity.show(getActivity(), AuxBatteryFragment.class, null,
						Configuration.ORIENTATION_UNDEFINED);
				break;

			default:
				v.performLongClick();
		}
	}

	private static final int MI_WAKEUP		= Menu.FIRST;
	private static final int MI_HL_01		= Menu.FIRST + 1;
	private static final int MI_HL_02		= Menu.FIRST + 2;
	private static final int MI_HL_03		= Menu.FIRST + 3;
	private static final int MI_HL_DEFAULT	= Menu.FIRST + 4;
	private static final int MI_AC_ON		= Menu.FIRST + 5;
	private static final int MI_AC_OFF		= Menu.FIRST + 6;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		switch (v.getId()) {

			case R.id.btn_wakeup:
				if (mCarData.car_type.equals("RT"))
					break; // no wakeup support for Twizy
				menu.setHeaderTitle(R.string.lb_wakeup_car);
				menu.add(0, MI_WAKEUP, 0, R.string.Wakeup);
				menu.add(R.string.Cancel);
				break;

			case R.id.tabCarImageHomelink:
			case R.id.txt_homelink:
				if (mCarData.car_type.equals("RT")) {
					// Renault Twizy: use Homelink for profile switching:
					menu.setHeaderTitle(R.string.textPROFILE);
					menu.add(0, MI_HL_DEFAULT, 0, R.string.Default);
				} else {
					menu.setHeaderTitle(R.string.textHOMELINK);
				}
				menu.add(0, MI_HL_01, 0, "1");
				menu.add(0, MI_HL_02, 0, "2");
				menu.add(0, MI_HL_03, 0, "3");
				menu.add(R.string.Cancel);
				break;

			case R.id.tabCarImageAC:
				menu.setHeaderTitle(R.string.textAC);
				menu.add(0, MI_AC_ON, 0, R.string.mi_ac_on);
				menu.add(0, MI_AC_OFF, 0, R.string.mi_ac_off);
				menu.add(R.string.Cancel);
				break;
		}
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		switch (item.getItemId()) {
		case MI_WAKEUP:
			sendCommand(R.string.msg_wakeup_car, "18", this);			
			return true;
		case MI_HL_01:
			sendCommand(R.string.msg_issuing_homelink, "24,0", this);			
			return true;
		case MI_HL_02:
			sendCommand(R.string.msg_issuing_homelink, "24,1", this);			
			return true;
		case MI_HL_03:
			sendCommand(R.string.msg_issuing_homelink, "24,2", this);			
			return true;
		case MI_HL_DEFAULT:
			sendCommand(R.string.msg_issuing_homelink, "24", this);
			return true;
		case MI_AC_ON:
			sendCommand(R.string.msg_issuing_climatecontrol, "26,1", this);
			return true;
		case MI_AC_OFF:
			sendCommand(R.string.msg_issuing_climatecontrol, "26,0", this);
			return true;
		default:
			return false;
		}
	}
	
	@Override
	public void onResultCommand(String[] result) {
		if (result.length <= 1)
			return;

		int command = Integer.parseInt(result[0]);
		int resCode = Integer.parseInt(result[1]);
		String resText = (result.length > 2) ? result[2] : "";
		String cmdMessage = getSentCommandMessage(result[0]);

		switch (resCode) {
			case 0: // ok
				Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.msg_ok),
						Toast.LENGTH_SHORT).show();
				break;
			case 1: // failed
				Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_failed, resText),
						Toast.LENGTH_SHORT).show();
				break;
			case 2: // unsupported
				Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_unsupported_operation),
						Toast.LENGTH_SHORT).show();
				break;
			case 3: // unimplemented
				Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_unimplemented_operation),
						Toast.LENGTH_SHORT).show();
				break;
		}

		cancelCommand();
	}
	
	// This updates the part of the view with times shown.
	// It is called by a periodic timer so it gets updated every few seconds.
	public void updateLastUpdatedView(CarData pCarData) {
		if ((pCarData == null) || (pCarData.car_lastupdated == null)) return;

		// First the last updated section...
		TextView tv = (TextView)findViewById(R.id.txt_last_updated);
		long now = System.currentTimeMillis();
		long seconds = (now - pCarData.car_lastupdated.getTime()) / 1000;
		long minutes = (seconds)/60;
		long hours = minutes/60;
		long days = minutes/(60*24);
		Log.d(TAG, "Last updated: " + seconds + " secs ago");

		if (pCarData.car_lastupdated == null) {
			tv.setText("");
			tv.setTextColor(0xFFFFFFFF);
		}
		else if (minutes == 0) {
			tv.setText(getText(R.string.live));
			tv.setTextColor(0xFFFFFFFF);
		}
		else if (minutes == 1) {
			tv.setText(getText(R.string.min1));
			tv.setTextColor(0xFFFFFFFF);
		}
		else if (days > 1) {
			tv.setText(String.format(getText(R.string.ndays).toString(),days));
			tv.setTextColor(0xFFFF0000);
		}
		else if (hours > 1) {
			tv.setText(String.format(getText(R.string.nhours).toString(),hours));
			tv.setTextColor(0xFFFF0000);
		}
		else if (minutes > 60) {
			tv.setText(String.format(getText(R.string.nmins).toString(),minutes));
			tv.setTextColor(0xFFFF0000);			
		}
		else {
			tv.setText(String.format(getText(R.string.nmins).toString(),minutes));
			tv.setTextColor(0xFFFFFFFF);
		}

		// Then the parking timer...
//		LinearLayout parkinglayoutv = (LinearLayout)findViewById(R.id.tabCarLayoutParking);
		tv = (TextView)findViewById(R.id.txt_parked_time);
		if ((!pCarData.car_started) && (pCarData.car_parked_time != null)) {
			// Car is parked
//			parkinglayoutv.setVisibility(View.VISIBLE);
			tv.setVisibility(View.VISIBLE);
			
			seconds = (now - pCarData.car_parked_time.getTime()) / 1000;
			minutes = (seconds)/60;
			hours = minutes/60;
			days = minutes/(60*24);

			if (minutes == 0)
				tv.setText(getText(R.string.justnow));
			else if (minutes == 1)
				tv.setText("1 min");
			else if (days > 1)
				tv.setText(String.format(getText(R.string.ndays).toString(),days));
			else if (hours > 1)
				tv.setText(String.format(getText(R.string.nhours).toString(),hours));
			else if (minutes > 60)
				tv.setText(String.format(getText(R.string.nmins).toString(),minutes));
			else
				tv.setText(String.format(getText(R.string.nmins).toString(),minutes));
			} else {
//			parkinglayoutv.setVisibility(View.INVISIBLE);
			tv.setVisibility(View.INVISIBLE);
		}

		// The signal strength indicator
		ImageView iv = (ImageView)findViewById(R.id.img_signal_rssi);
		iv.setImageResource(Ui.getDrawableIdentifier(getActivity(), "signal_strength_" + pCarData.car_gsm_bars));
	}
	
	// This updates the main informational part of the view.
	// It is called when the server gets new data.
	public void updateCarBodyView(CarData pCarData) {
		if ((pCarData == null) || (pCarData.car_lastupdated == null)) return;

		ImageView iv;
		TextView label, tv;

		// Now, the car background image
		iv = (ImageView)findViewById(R.id.tabCarImageCarOutline);

		if (pCarData.sel_vehicle_image.startsWith("car_imiev_")) {
			// Mitsubishi i-MiEV: one ol image for all colors:
			iv.setImageResource(R.drawable.ol_car_imiev);
		}
		else if (pCarData.sel_vehicle_image.startsWith("car_smart_")) {
		 	// smart ED: one ol image for all colors:
			iv.setImageResource(R.drawable.ol_car_smart);
		}
		else if (pCarData.sel_vehicle_image.startsWith("car_kianiro_")) {
			iv.setImageResource(R.drawable.ol_car_kianiro_grey);
		}
		else {
			iv.setImageResource(Ui.getDrawableIdentifier(getActivity(), "ol_" + pCarData.sel_vehicle_image));
		}

		// "12V" box:
		label = (TextView) findViewById(R.id.tabCarText12VLabel);
		tv = (TextView) findViewById(R.id.tabCarText12V);

		tv.setText(String.format("%.1fV", mCarData.car_12vline_voltage));

		if (mCarData.car_12vline_ref <= 1.5 || mCarData.car_charging_12v) {
			// charging / calmdown
			tv.setTextColor(0xFFA9A9FF);
		} else {
			Double diff = mCarData.car_12vline_ref - mCarData.car_12vline_voltage;
			if (diff >= 1.6)
				tv.setTextColor(0xFFFF0000);
			else if (diff >= 1.0)
				tv.setTextColor(0xFFFF6600);
			else
				tv.setTextColor(0xFFFFFFFF);
		}

		// "Ambient" box:
		iv = (ImageView)findViewById(R.id.tabCarImageCarAmbientBox);
		label = (TextView) findViewById(R.id.tabCarTextAmbientLabel);
		tv = (TextView) findViewById(R.id.tabCarTextAmbient);

		if (mCarData.car_type.equals("RT")) {
			pCarData.stale_ambient_temp = DataStale.NoValue;
		}

		if (pCarData.stale_ambient_temp == DataStale.NoValue) {
			iv.setVisibility(View.INVISIBLE);
			label.setVisibility(View.INVISIBLE);
			tv.setText(null);
		} else if ((pCarData.stale_ambient_temp == DataStale.Stale)) {
			iv.setVisibility(View.VISIBLE);
			label.setVisibility(View.VISIBLE);
			tv.setText(pCarData.car_temp_ambient);
			tv.setTextColor(0xFF808080);
		} else {
			iv.setVisibility(View.VISIBLE);
			label.setVisibility(View.VISIBLE);
			tv.setText(pCarData.car_temp_ambient);
			tv.setTextColor(0xFFFFFFFF);
		}

		// TPMS
		//	String tirePressureDisplayFormat = "%s\n%s";
        TextView fltv = (TextView) findViewById(R.id.textFLWheel);
        TextView fltvv = (TextView) findViewById(R.id.textFLWheelVal);

        TextView frtv = (TextView) findViewById(R.id.textFRWheel);
        TextView frtvv = (TextView) findViewById(R.id.textFRWheelVal);
        
        TextView rltv = (TextView) findViewById(R.id.textRLWheel);
        TextView rltvv = (TextView) findViewById(R.id.textRLWheelVal);
        
        TextView rrtv = (TextView) findViewById(R.id.textRRWheel);
        TextView rrtvv = (TextView) findViewById(R.id.textRRWheelVal);
        
    	iv = (ImageView)findViewById(R.id.tabCarImageCarTPMSBoxes);

		if (mCarData.car_type.equals("RT") || mCarData.car_type.equals("SE")) {
			pCarData.stale_tpms = DataStale.NoValue;
		}

		if (pCarData.stale_tpms == DataStale.NoValue) {
        	iv.setVisibility(View.INVISIBLE);
    		fltv.setText(null);
    		frtv.setText(null);
    		rltv.setText(null);
    		rrtv.setText(null);
    		
    		fltvv.setText(null);
    		frtvv.setText(null);
    		rltvv.setText(null);
    		rrtvv.setText(null);
    		
        } else {
        	iv.setVisibility(View.VISIBLE);

        	fltv.setText(pCarData.car_tpms_fl_p);
    		frtv.setText(pCarData.car_tpms_fr_p);
    		rltv.setText(pCarData.car_tpms_rl_p);
    		rrtv.setText(pCarData.car_tpms_rr_p);
    		
        	fltvv.setText(pCarData.car_tpms_fl_t);
    		frtvv.setText(pCarData.car_tpms_fr_t);
    		rltvv.setText(pCarData.car_tpms_rl_t);
    		rrtvv.setText(pCarData.car_tpms_rr_t);
    		
    		if (pCarData.stale_tpms == DataStale.Stale) {
    			fltv.setTextColor(0xFF808080);
    			frtv.setTextColor(0xFF808080);
    			rltv.setTextColor(0xFF808080);
    			rrtv.setTextColor(0xFF808080);

    			fltvv.setTextColor(0xFF808080);
    			frtvv.setTextColor(0xFF808080);
    			rltvv.setTextColor(0xFF808080);
    			rrtvv.setTextColor(0xFF808080);
    			
    		} else {
    			fltv.setTextColor(0xFFFFFFFF);
    			frtv.setTextColor(0xFFFFFFFF);
    			rltv.setTextColor(0xFFFFFFFF);
    			rrtv.setTextColor(0xFFFFFFFF);

    			fltvv.setTextColor(0xFFFFFFFF);
    			frtvv.setTextColor(0xFFFFFFFF);
    			rltvv.setTextColor(0xFFFFFFFF);
    			rrtvv.setTextColor(0xFFFFFFFF);
    		}
        }

    // "Temp PEM" box:
		TextView pemtvl = (TextView) findViewById(R.id.tabCarTextPEMLabel);
		TextView pemtv = (TextView) findViewById(R.id.tabCarTextPEM);
		
		// Renault Zoe, Smart ED: display 12V state
		if (mCarData.car_type.equals("RZ") || mCarData.car_type.equals("SE")) {
			pemtvl.setText(R.string.text12VBATT);
			pemtv.setText(String.format("%.1fV", mCarData.car_12vline_voltage));
      
      if (mCarData.car_12vline_ref <= 1.5) {
				// charging / calmdown
				pemtv.setTextColor(0xFFA9A9FF);
			} else {
				Double diff = mCarData.car_12vline_ref - mCarData.car_12vline_voltage;
				if (diff >= 1.6)
					pemtv.setTextColor(0xFFFF0000);
				else if (diff >= 1.0)
					pemtv.setTextColor(0xFFFF6600);
				else
					pemtv.setTextColor(0xFFFFFFFF);
			}
    } else {
			// Standard car: display PEM temperature
			pemtvl.setText(R.string.textPEM);
      if (pCarData.stale_car_temps == DataStale.NoValue) {
				pemtv.setText("");
			} else {
				pemtv.setText(pCarData.car_temp_pem);
				if (pCarData.stale_car_temps == DataStale.Stale) {
					pemtv.setTextColor(0xFF808080);
				} else {
					pemtv.setTextColor(0xFFFFFFFF);
				}
			}
    }

		// "Temp Motor" box:
		TextView motortvl = (TextView) findViewById(R.id.tabCarTextMotorLabel);
		TextView motortv = (TextView) findViewById(R.id.tabCarTextMotor);
		
		// Renault Zoe, Smart ED: display HVBatt state
		if (mCarData.car_type.equals("RZ") || mCarData.car_type.equals("SE")) {
			motortvl.setText(R.string.textHVBATT);
			motortv.setText(String.format("%.1fV", mCarData.car_battery_voltage));
    } else {
			// Standard car: display Motor temperature
			motortvl.setText(R.string.textMOTOR);
      if (pCarData.stale_car_temps == DataStale.NoValue) {
				motortv.setText("");
			} else {
				motortv.setText(pCarData.car_temp_motor);
				if (pCarData.stale_car_temps == DataStale.Stale) {
					motortv.setTextColor(0xFF808080);
				} else {
					motortv.setTextColor(0xFFFFFFFF);
				}
			}
    }

		// Temperatures
		TextView batterytv = (TextView) findViewById(R.id.tabCarTextBattery);
		TextView chargertv = (TextView) findViewById(R.id.tabCarTextCharger);
		if (pCarData.stale_car_temps == DataStale.NoValue) {
			batterytv.setText("");
			chargertv.setText("");
		} else {
			batterytv.setText(pCarData.car_temp_battery);
			chargertv.setText(pCarData.car_temp_charger);
			if (pCarData.stale_car_temps == DataStale.Stale) {
				batterytv.setTextColor(0xFF808080);
				chargertv.setTextColor(0xFF808080);
			} else {
				batterytv.setTextColor(0xFFFFFFFF);
				chargertv.setTextColor(0xFFFFFFFF);
			}
		}

		String st;
		SpannableString ss;

		// Odometer
		st = String.format("%.1f%s", (float) pCarData.car_odometer_raw / 10, pCarData.car_distance_units);
		ss = new SpannableString(st);
		ss.setSpan(new RelativeSizeSpan(0.67f), st.indexOf(pCarData.car_distance_units), st.length(), 0);
		tv = (TextView) findViewById(R.id.tabCarTextOdometer);
		tv.setText(ss);

		// Speed
		tv = (TextView) findViewById(R.id.tabCarTextSpeed);
		if (!pCarData.car_started) {
			tv.setText("");
		} else {
			st = String.format("%.1f%s", pCarData.car_speed_raw, pCarData.car_speed_units);
			ss = new SpannableString(st);
			ss.setSpan(new RelativeSizeSpan(0.67f), st.indexOf(pCarData.car_speed_units), st.length(), 0);
			tv.setText(ss);
		}

		// Trip
		st = String.format("➟%.1f%s", (float) pCarData.car_tripmeter_raw / 10, pCarData.car_distance_units);
		ss = new SpannableString(st);
		ss.setSpan(new RelativeSizeSpan(0.67f), st.indexOf(pCarData.car_distance_units), st.length(), 0);
		tv = (TextView) findViewById(R.id.tabCarTextTrip);
		tv.setText(ss);

		// Energy
		st = String.format("▴%.1f ▾%.1f kWh",
				Math.floor(pCarData.car_energyused*10)/10,
				Math.floor(pCarData.car_energyrecd*10)/10);
		ss = new SpannableString(st);
		ss.setSpan(new RelativeSizeSpan(0.67f), st.indexOf("kWh"), st.length(), 0);
		tv = (TextView) findViewById(R.id.tabCarTextEnergy);
		tv.setText(ss);

		// Car Hood
		iv = (ImageView) findViewById(R.id.tabCarImageCarHoodOpen);
		iv.setVisibility(pCarData.car_bonnet_open ? View.VISIBLE : View.INVISIBLE);
		
    if (pCarData.sel_vehicle_image.startsWith("car_zoe_")) {
      // Left Door Zoe
      iv = (ImageView) findViewById(R.id.tabCarImageCarLeftDoorOpen);
      iv.setVisibility(pCarData.car_frontleftdoor_open ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.zoe_outline_ld);
      
      // Right Door Zoe
      iv = (ImageView) findViewById(R.id.tabCarImageCarRightDoorOpen);
      iv.setVisibility(pCarData.car_frontrightdoor_open ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.zoe_outline_rd);
      
      // Rear Left Door Zoe
      iv = (ImageView) findViewById(R.id.tabCarImageCarRearLeftDoorOpen);
      iv.setVisibility(pCarData.car_rearleftdoor_open ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.zoe_outline_rld);
      
      // Rear Right Door Zoe
      iv = (ImageView) findViewById(R.id.tabCarImageCarRearRightDoorOpen);
      iv.setVisibility(pCarData.car_rearrightdoor_open ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.zoe_outline_rrd);
      
      // Trunk Zoe
      iv = (ImageView) findViewById(R.id.tabCarImageCarTrunkOpen);
      iv.setVisibility(pCarData.car_trunk_open ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.zoe_outline_tr);
      
      // Headlights Zoe
      iv = (ImageView) findViewById(R.id.tabCarImageCarHeadlightsON);
      iv.setVisibility(pCarData.car_headlights_on ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.zoe_carlights);
    } else if (pCarData.sel_vehicle_image.startsWith("car_smart_")) {
      // Left Door Smart
      iv = (ImageView) findViewById(R.id.tabCarImageCarLeftDoorOpen);
      iv.setVisibility(pCarData.car_frontleftdoor_open ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.smart_outline_ld);
      
      // Right Door Smart
      iv = (ImageView) findViewById(R.id.tabCarImageCarRightDoorOpen);
      iv.setVisibility(pCarData.car_frontrightdoor_open ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.smart_outline_rd);
      
      // Trunk Smart
      iv = (ImageView) findViewById(R.id.tabCarImageCarTrunkOpen);
      iv.setVisibility(pCarData.car_trunk_open ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.smart_outline_tr);
      
      // Headlights Smart
      iv = (ImageView) findViewById(R.id.tabCarImageCarHeadlightsON);
      iv.setVisibility(pCarData.car_headlights_on ? View.VISIBLE : View.INVISIBLE);
      iv.setImageResource(R.drawable.smart_carlights);
    } else {
      // Left Door
      iv = (ImageView) findViewById(R.id.tabCarImageCarLeftDoorOpen);
      iv.setVisibility(pCarData.car_frontleftdoor_open ? View.VISIBLE : View.INVISIBLE);
      
      // Right Door
      iv = (ImageView) findViewById(R.id.tabCarImageCarRightDoorOpen);
      iv.setVisibility(pCarData.car_frontrightdoor_open ? View.VISIBLE : View.INVISIBLE);
      
      // Trunk
      iv = (ImageView) findViewById(R.id.tabCarImageCarTrunkOpen);
      iv.setVisibility(pCarData.car_trunk_open ? View.VISIBLE : View.INVISIBLE);
      
      // Headlights
      iv = (ImageView) findViewById(R.id.tabCarImageCarHeadlightsON);
      iv.setVisibility(pCarData.car_headlights_on ? View.VISIBLE : View.INVISIBLE);
    }

		// Car locked
		if (pCarData.car_type.equals("SE") || pCarData.car_type.equals("RZ")) {
			// Car locked Smart ED 451 and Renault ZOE
			iv = (ImageView) findViewById(R.id.tabCarImageCarLocked);
			iv.setImageResource(pCarData.car_locked ? R.drawable.carlock_clean : R.drawable.carunlock_clean);
		} else {
			iv = (ImageView) findViewById(R.id.tabCarImageCarLocked);
			iv.setImageResource(pCarData.car_locked ? R.drawable.carlock : R.drawable.carunlock);
		}
    
    if (pCarData.car_type.equals("SE")) {
      // Valet mode Smart ED 451
      iv = (ImageView) findViewById(R.id.tabCarImageCarValetMode);
      iv.setImageResource(pCarData.car_valetmode ? R.drawable.smart_on : R.drawable.smart_off);
    } else if (pCarData.car_type.equals("RZ")) {
			// Valet mode Renault ZOE
			iv = (ImageView) findViewById(R.id.tabCarImageCarValetMode);
      iv.setImageResource(pCarData.car_valetmode ? R.drawable.carvaleton_clean : R.drawable.carvaletoff_clean);
		} else {
      // Valet mode
      iv = (ImageView) findViewById(R.id.tabCarImageCarValetMode);
      iv.setImageResource(pCarData.car_valetmode ? R.drawable.carvaleton : R.drawable.carvaletoff);
    }
		
		// Charge Port
	    iv = (ImageView) findViewById(R.id.tabCarImageCarChargePortOpen);
	    if (!pCarData.car_chargeport_open) {
			iv.setVisibility(View.INVISIBLE);
	    } else {
			iv.setVisibility(View.VISIBLE);

			if (pCarData.sel_vehicle_image.startsWith("car_twizy_")) {
				// Renault Twizy:
				iv.setImageResource(R.drawable.ol_car_twizy_chargeport);
			}
			else if (pCarData.sel_vehicle_image.startsWith("car_imiev_")) {
				// Mitsubishi i-MiEV:
				if (pCarData.car_charge_currentlimit_raw > 16)
					iv.setImageResource(R.drawable.ol_car_imiev_charge_quick);
				else
					iv.setImageResource(R.drawable.ol_car_imiev_charge);
			}
			else if (pCarData.sel_vehicle_image.startsWith("car_kianiro_")) {
				// Kia Niro: use i-MiEV charge overlays
				if (pCarData.car_charge_mode.equals("performance"))
					iv.setImageResource(R.drawable.ol_car_imiev_charge_quick);
				else
					iv.setImageResource(R.drawable.ol_car_imiev_charge);
			}
			else if (pCarData.sel_vehicle_image.startsWith("car_zoe_")) {
				// Renault ZOE
				if (pCarData.car_charge_state.equals("charging"))
					iv.setImageResource(R.drawable.ol_car_zoe_chargeport_orange);
				else if (pCarData.car_charge_state.equals("stopped"))
					iv.setImageResource(R.drawable.ol_car_zoe_chargeport_red);
				else if (pCarData.car_charge_state.equals("prepare"))
					iv.setImageResource(R.drawable.ol_car_zoe_chargeport_yellow);
				else
					iv.setImageResource(R.drawable.ol_car_zoe_chargeport_green);
			}
			else {
				// Tesla Roadster:
				if (pCarData.car_charge_substate_i_raw == 0x07) {
					// We need to connect the power cable
					iv.setImageResource(R.drawable.roadster_outline_cu);
				} else if ((pCarData.car_charge_state_i_raw == 0x0d) || (pCarData.car_charge_state_i_raw == 0x0e) || (pCarData.car_charge_state_i_raw == 0x101)) {
					// Preparing to charge, timer wait, or fake 'starting' state
					iv.setImageResource(R.drawable.roadster_outline_ce);
				} else if ((pCarData.car_charge_state_i_raw == 0x01) ||
						(pCarData.car_charge_state_i_raw == 0x02) ||
						(pCarData.car_charge_state_i_raw == 0x0f) ||
						(pCarData.car_charging)) {
					// Charging
					iv.setImageResource(R.drawable.roadster_outline_cp);
				} else if (pCarData.car_charge_state_i_raw == 0x04) {
					// Charging done
					iv.setImageResource(R.drawable.roadster_outline_cd);
				} else if ((pCarData.car_charge_state_i_raw >= 0x15) && (pCarData.car_charge_state_i_raw <= 0x19)) {
					// Stopped
					iv.setImageResource(R.drawable.roadster_outline_cs);
				} else {
					// Fake 0x115 'stopping' state, or something else not understood
					iv.setImageResource(R.drawable.roadster_outline_cp);
				}
			}

	    }

		// A/C status:
		iv = (ImageView) findViewById(R.id.tabCarImageAC);
		if ((pCarData.car_doors5_raw & 0x80) > 0) {
			iv.setImageResource(R.drawable.ic_ac_on);
		} else {
			iv.setImageResource(R.drawable.ic_ac_off);
		}

		// Done.
	}
}
