package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;

import com.atakmap.android.gui.PluginSpinner;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.nonatak.NonAtakStationCotGenerator;
import com.paulmandal.atak.forwarder.plugin.ui.EditTextValidator;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.DevicesTabViewModel;

import java.util.Arrays;
import java.util.List;

public class DevicesTab extends RelativeLayout {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + DevicesTab.class.getSimpleName();

    private Context mAtakContext;
    private String mTargetDeviceAddress;

    public DevicesTab(Context context) {
        this(context, null);
    }

    public DevicesTab(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DevicesTab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.devices_layout, this);
    }

    public void bind(LifecycleOwner lifecycleOwner,
                     DevicesTabViewModel devicesTabViewModel,
                     Context pluginContext,
                     Context atakContext) {
        Button refreshButton = findViewById(R.id.button_refresh_connected_devices);
        refreshButton.setOnClickListener(v -> devicesTabViewModel.refreshDevices());

        Button setCommDeviceButton = findViewById(R.id.button_set_comm_device);
        setCommDeviceButton.setOnClickListener(v -> {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(atakContext)
                    .setTitle(pluginContext.getResources().getString(R.string.warning))
                    .setMessage(pluginContext.getResources().getString(R.string.set_comm_device_dialog))
                    .setPositiveButton(pluginContext.getResources().getString(R.string.ok), (DialogInterface dialog, int whichButton) -> devicesTabViewModel.setCommDeviceAddress(mTargetDeviceAddress))
                    .setNegativeButton(pluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());

            alertDialog.show();
        });

        EditText deviceCallsignEditText = findViewById(R.id.edittext_device_callsign);
        deviceCallsignEditText.addTextChangedListener(new EditTextValidator(deviceCallsignEditText) {
            @Override
            public void validate(TextView textView, String text) {
                if (text == null || text.isEmpty()) {
                    textView.setError("You must enter a device callsign");
                    return;
                }

                textView.setError(null);
            }
        });

        EditText pliIntervalSEditText = findViewById(R.id.edittext_pli_interval_s);
        pliIntervalSEditText.addTextChangedListener(new EditTextValidator(pliIntervalSEditText) {
            @Override
            public void validate(TextView textView, String text) {
                try {
                    int pliIntervalS = Integer.parseInt(text);

                    if (pliIntervalS > 0) {
                        textView.setError(null);
                        return;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                textView.setError("PLI Interval must be > 0s");
            }
        });

        EditText screenShutoffDelaySEditText = findViewById(R.id.edittext_screen_shutoff_delay_s);
        screenShutoffDelaySEditText.addTextChangedListener(new EditTextValidator(screenShutoffDelaySEditText) {
            @Override
            public void validate(TextView textView, String text) {
                try {
                    int screenShutoffDelayS = Integer.parseInt(text);

                    if (screenShutoffDelayS > 0) {
                        textView.setError(null);
                        return;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                textView.setError("Screen shutoff delay must be > 0s");
            }
        });

        PluginSpinner teamSpinner = findViewById(R.id.spinner_team);
        ArrayAdapter<String> teamArrayAdapter = new ArrayAdapter<>(pluginContext, R.layout.plugin_spinner_item, Arrays.asList(NonAtakStationCotGenerator.TEAMS));
        teamArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamSpinner.setAdapter(teamArrayAdapter);

        PluginSpinner roleSpinner = findViewById(R.id.spinner_role);
        ArrayAdapter<String> roleArrayAdapter = new ArrayAdapter<>(pluginContext, R.layout.plugin_spinner_item, Arrays.asList(NonAtakStationCotGenerator.ROLES));
        roleArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleArrayAdapter);

        Button writeToNonAtakButton = findViewById(R.id.button_write_to_non_atak);
        writeToNonAtakButton.setOnClickListener(v -> {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(atakContext)
                    .setTitle(pluginContext.getResources().getString(R.string.warning))
                    .setMessage(pluginContext.getResources().getString(R.string.write_to_non_atak_dialog))
                    .setPositiveButton(pluginContext.getResources().getString(R.string.ok), (DialogInterface dialog, int whichButton) -> maybeWriteToNonAtatkDevice(devicesTabViewModel, mTargetDeviceAddress, deviceCallsignEditText.getText().toString(), teamSpinner.getSelectedItemPosition(), roleSpinner.getSelectedItemPosition(), Integer.parseInt(pliIntervalSEditText.getText().toString()), Integer.parseInt(screenShutoffDelaySEditText.getText().toString())))
                    .setNegativeButton(pluginContext.getResources().getString(R.string.cancel), (DialogInterface dialog, int whichButton) -> dialog.cancel());
            alertDialog.show();
        });

        ListView devicesListView = findViewById(R.id.listview_bonded_devices);
        TextView commDevice = findViewById(R.id.textview_comm_device);
        TextView targetDevice = findViewById(R.id.textview_target_device);

        devicesListView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Log.e(TAG, "onItemClickListener: " + position);
            BluetoothDevice bluetoothDevice = (BluetoothDevice) devicesListView.getAdapter().getItem(position);

            String targetDeviceAddress = bluetoothDevice.getAddress();
            targetDevice.setText(String.format("%s %s", bluetoothDevice.getName(), targetDeviceAddress));
            mTargetDeviceAddress = targetDeviceAddress;
        });

        devicesTabViewModel.getBluetoothDevices().observe(lifecycleOwner, bluetoothDevices -> {
            String commDeviceAddress = devicesTabViewModel.getCommDeviceAddress().getValue();

            maybeUpdateCommDevice(commDevice, commDeviceAddress, bluetoothDevices);
            updateDevicesAdapter(devicesListView, pluginContext, bluetoothDevices, commDeviceAddress);
        });

        devicesTabViewModel.getCommDeviceAddress().observe(lifecycleOwner, commDeviceAddress -> {
            List<BluetoothDevice> bluetoothDevices = devicesTabViewModel.getBluetoothDevices().getValue();

            if (bluetoothDevices == null) {
                return;
            }

            maybeUpdateCommDevice(commDevice, commDeviceAddress, bluetoothDevices);
            updateDevicesAdapter(devicesListView, pluginContext, bluetoothDevices, commDeviceAddress);
        });

        ProgressBar deviceWriteProgressBar = findViewById(R.id.progressbar_writing_to_device);
        devicesTabViewModel.getNonAtakDeviceWriteInProgress().observe(lifecycleOwner, nonAtakDeviceWriteInProgress -> deviceWriteProgressBar.setVisibility(nonAtakDeviceWriteInProgress ? View.VISIBLE : View.GONE));

        mAtakContext = atakContext;
    }

    private void maybeUpdateCommDevice(TextView commDevice, String commDeviceAddress, List<BluetoothDevice> bluetoothDevices) {
        if (commDeviceAddress != null) {
            for (BluetoothDevice device : bluetoothDevices) {
                String deviceAddress = device.getAddress();
                if (deviceAddress.equals(commDeviceAddress)) {
                    commDevice.setText(String.format("%s - %s", device.getName(), deviceAddress));
                }
            }
        }
    }

    private void updateDevicesAdapter(ListView devicesListView, Context pluginContext, List<BluetoothDevice> bluetoothDevices, String commDeviceAddress) {
        DevicesDataAdapter devicesDataAdapter = new DevicesDataAdapter(pluginContext, bluetoothDevices, commDeviceAddress);
        devicesListView.setAdapter(devicesDataAdapter);
    }

    private void maybeWriteToNonAtatkDevice(DevicesTabViewModel devicesTabViewModel, String deviceAddress, String deviceCallsign, int teamIndex, int roleIndex, int refreshIntervalS, int screenShutoffDelayS) {
        if (deviceAddress == null || deviceAddress.isEmpty()) {
            Toast.makeText(mAtakContext, "You must select a device to write to", Toast.LENGTH_SHORT).show();
            return;
        }

        if (deviceCallsign == null || deviceCallsign.isEmpty()) {
            Toast.makeText(mAtakContext, "You must enter a device callsign", Toast.LENGTH_SHORT).show();
            return;
        }

        if (refreshIntervalS < 1) {
            Toast.makeText(mAtakContext, "Refresh interval must be > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (screenShutoffDelayS < 1) {
            Toast.makeText(mAtakContext, "Screen shutoff delay must be > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(mAtakContext, "Writing to non-ATAK device", Toast.LENGTH_SHORT).show();
        devicesTabViewModel.writeToNonAtak(deviceAddress, deviceCallsign, teamIndex, roleIndex, refreshIntervalS, screenShutoffDelayS);
    }
}
