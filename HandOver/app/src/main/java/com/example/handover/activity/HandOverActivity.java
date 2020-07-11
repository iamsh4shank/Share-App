package com.example.handover.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.handover.R;
import com.example.handover.utils.DividerItemDecoration;
import com.example.handover.utils.HotspotControl;
import com.example.handover.utils.RecyclerViewArrayAdapter;
import com.example.handover.utils.Utils;
import com.example.handover.utils.WifiUtils;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class HandOverActivity extends AppCompatActivity {

    public static final String TAG = "ShareActivity";
    public static final String PREFERENCES_KEY_SHARED_FILE_PATHS = "sharethem_shared_file_paths";
    public static final String PREFERENCES_KEY_DATA_WARNING_SKIP = "sharethem_data_warning_skip";
    private static final int REQUEST_WRITE_SETTINGS = 1;

    TextView m_sender_wifi_info;
    TextView m_noReceiversText;
    RelativeLayout m_receivers_list_layout;
    RecyclerView m_receiversList;
    SwitchCompat m_apControlSwitch;
    TextView m_showShareList;
    Toolbar m_toolbar;

    private ReceiversListingAdapter m_receiversListAdapter;
    private CompoundButton.OnCheckedChangeListener m_sender_ap_switch_listener;

    private ShareUIHandler m_uiUpdateHandler;
    private BroadcastReceiver m_p2pServerUpdatesListener;

    private HotspotControl hotspotControl;
    private boolean isApEnabled = false;
    private boolean shouldAutoConnect = true;

    private String[] m_sharedFilePaths = null;

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 100;

    //region: Activity Methods
    @SuppressLint("WrongViewCast")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);

        //init UI
        m_sender_wifi_info = (TextView) findViewById(R.id.p2p_sender_wifi_hint);
        m_noReceiversText = (TextView) findViewById(R.id.p2p_no_receivers_text);
        m_showShareList = (TextView) findViewById(R.id.p2p_sender_items_label);
        m_showShareList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSharedFilesDialog();
            }
        });

        m_receivers_list_layout = (RelativeLayout) findViewById(R.id.p2p_receivers_list_layout);
        m_receiversList = (RecyclerView) findViewById(R.id.p2p_receivers_list);
        m_apControlSwitch = (SwitchCompat) findViewById(R.id.p2p_sender_ap_switch);

        m_toolbar = (Toolbar) findViewById(R.id.toolbar);
        m_toolbar.setTitle(getString(R.string.send_title));
        setSupportActionBar(m_toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        hotspotControl = HotspotControl.getInstance(getApplicationContext());
        m_receiversList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        m_receiversList.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.list_divider)));

        //if file paths are found, save'em into preferences. OR find them in prefs
        if (null != getIntent() && getIntent().hasExtra(HandOverService.EXTRA_FILE_PATHS))
            m_sharedFilePaths = getIntent().getStringArrayExtra(HandOverService.EXTRA_FILE_PATHS);
        SharedPreferences prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        if (null == m_sharedFilePaths)
            m_sharedFilePaths = Utils.toStringArray(prefs.getString(PREFERENCES_KEY_SHARED_FILE_PATHS, null));
        else
            prefs.edit().putString(PREFERENCES_KEY_SHARED_FILE_PATHS, new JSONArray(Arrays.asList(m_sharedFilePaths)).toString()).apply();
        m_receiversListAdapter = new ReceiversListingAdapter(new ArrayList<HotspotControl.WifiScanResult>(), m_sharedFilePaths);
        m_receiversList.setAdapter(m_receiversListAdapter);
        m_sender_ap_switch_listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!Utils.isOreoOrAbove()) {
                        //If target version is MM and beyond, you need to check System Write permissions to proceed.
                        if (Build.VERSION.SDK_INT >= 23 &&
                                // if targetSdkVersion >= 23
                                //     ShareActivity has to check for System Write permissions to proceed
                                Utils.getTargetSDKVersion(getApplicationContext()) >= 23 && !Settings.System.canWrite(HandOverActivity.this)) {
                            changeApControlCheckedStatus(false);
                            showMessageDialogWithListner(getString(R.string.p2p_sender_system_settings_permission_prompt), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivityForResult(intent, REQUEST_WRITE_SETTINGS);
                                }
                            }, false, true);
                            return;
                        } else if (!getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).getBoolean(PREFERENCES_KEY_DATA_WARNING_SKIP, false) && Utils.isMobileDataEnabled(getApplicationContext())) {
                            changeApControlCheckedStatus(false);
                            showDataWarningDialog();
                            return;
                        }
                    } else if (!checkLocationPermission()) {
                        changeApControlCheckedStatus(false);
                        return;
                    }
                    enableAp();
                } else {
                    changeApControlCheckedStatus(true);
                    showMessageDialogWithListner(getString(R.string.p2p_sender_close_warning), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "sending intent to service to stop p2p..");
                            resetSenderUi(true);
                        }
                    }, true, false);
                }
            }
        };
        m_apControlSwitch.setOnCheckedChangeListener(m_sender_ap_switch_listener);
        m_p2pServerUpdatesListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isFinishing() || null == intent)
                    return;
                int intentType = intent.getIntExtra(HandOverService.ShareIntents.TYPE, 0);
                if (intentType == HandOverService.ShareIntents.Types.FILE_TRANSFER_STATUS) {
                    String fileName = intent.getStringExtra(HandOverService.ShareIntents.SHARE_SERVER_UPDATE_FILE_NAME);
                    updateReceiverListItem(intent.getStringExtra(HandOverService.ShareIntents.SHARE_CLIENT_IP), intent.getIntExtra(HandOverService.ShareIntents.SHARE_TRANSFER_PROGRESS, -1), intent.getStringExtra(HandOverService.ShareIntents.SHARE_SERVER_UPDATE_TEXT), fileName);
                } else if (intentType == HandOverService.ShareIntents.Types.AP_DISABLED_ACKNOWLEDGEMENT) {
                    shouldAutoConnect = false;
                    resetSenderUi(false);
                }
            }
        };
        registerReceiver(m_p2pServerUpdatesListener, new IntentFilter(HandOverService.ShareIntents.SHARE_SERVER_UPDATES_INTENT_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        //If service is already running, change UI and display info for receiver
        if (Utils.isShareServiceRunning(getApplicationContext())) {
            if (!m_apControlSwitch.isChecked()) {
                Log.e(TAG, "p2p service running, changing switch status and start handler for ui changes");
                changeApControlCheckedStatus(true);
            }
            refreshApData();
            m_receivers_list_layout.setVisibility(View.VISIBLE);
        } else if (m_apControlSwitch.isChecked()) {
            changeApControlCheckedStatus(false);
            resetSenderUi(false);
        }
        //switch on sender mode if not already
        else if (shouldAutoConnect) {
            m_apControlSwitch.setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != m_p2pServerUpdatesListener)
            unregisterReceiver(m_p2pServerUpdatesListener);
        if (null != m_uiUpdateHandler)
            m_uiUpdateHandler.removeCallbacksAndMessages(null);
        m_uiUpdateHandler = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableAp();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        showMessageDialogWithListner(getString(R.string.p2p_receiver_gps_permission_warning), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkLocationPermission();
                            }
                        }, true, true);
                    } else {
                        showMessageDialogWithListner(getString(R.string.p2p_receiver_gps_no_permission_prompt), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                } catch (ActivityNotFoundException anf) {
                                    Toast.makeText(getApplicationContext(), "Settings activity not found", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }, true, true);
                    }
                }
        }
    }
    //endregion: Activity Methods

    //region: Dialog utilities
    public void showMessageDialogWithListner(String message,
                                             DialogInterface.OnClickListener listner, boolean showNegavtive,
                                             final boolean finishCurrentActivity) {
        if (isFinishing())
            return;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setCancelable(false);
        builder.setMessage(Html.fromHtml(message));
        builder.setPositiveButton(getString(R.string.Action_Ok), listner);
        if (showNegavtive)
            builder.setNegativeButton(getString(R.string.Action_cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (finishCurrentActivity)
                                finish();
                            else dialog.dismiss();
                        }
                    });
        builder.show();
    }

    @TargetApi(23)
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
            );
            return false;
        }
        return true;
    }

    public void showDataWarningDialog() {
        if (isFinishing())
            return;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setCancelable(false);
        builder.setMessage(getString(R.string.sender_data_on_warning));
        builder.setPositiveButton(getString(R.string.label_settings), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(
                        Settings.ACTION_SETTINGS));
            }
        });
        builder.setNegativeButton(getString(R.string.label_thats_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changeApControlCheckedStatus(true);
                        enableAp();
                    }
                });
        builder.setNeutralButton(getString(R.string.label_dont_ask), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
                prefs.edit().putBoolean(PREFERENCES_KEY_DATA_WARNING_SKIP, true).apply();
                changeApControlCheckedStatus(true);
                enableAp();
            }
        });
        builder.show();
    }

    /**
     * Shows shared File urls
     */
    void showSharedFilesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Shared Files");
        builder.setItems(m_sharedFilePaths, null);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }
    //endregion: Activity Methods

    //region: Hotspot Control
    private void enableAp() {
        m_sender_wifi_info.setText(getString(R.string.p2p_sender_hint_connecting));
        startP2pSenderWatchService();
        refreshApData();
        m_receivers_list_layout.setVisibility(View.VISIBLE);
    }

    private void disableAp() {
        //Send STOP action to service
        Intent p2pServiceIntent = new Intent(getApplicationContext(), HandOverService.class);
        p2pServiceIntent.setAction(HandOverService.WIFI_AP_ACTION_STOP);
        startService(p2pServiceIntent);
        isApEnabled = false;
    }

    private void startP2pSenderWatchService() {
        Intent p2pServiceIntent = new Intent(getApplicationContext(), HandOverService.class);
        p2pServiceIntent.putExtra(HandOverService.EXTRA_FILE_PATHS, m_sharedFilePaths);
        if (null != getIntent()) {
            p2pServiceIntent.putExtra(HandOverService.EXTRA_PORT, Utils.isOreoOrAbove() ? Utils.DEFAULT_PORT_OREO : getIntent().getIntExtra(HandOverService.EXTRA_PORT, 0));
            p2pServiceIntent.putExtra(HandOverService.EXTRA_SENDER_NAME, getIntent().getStringExtra(HandOverService.EXTRA_SENDER_NAME));
        }
        p2pServiceIntent.setAction(HandOverService.WIFI_AP_ACTION_START);
        startService(p2pServiceIntent);
    }

    private void startHostspotCheckOnService() {
        Intent p2pServiceIntent = new Intent(getApplicationContext(), HandOverService.class);
        p2pServiceIntent.setAction(HandOverService.WIFI_AP_ACTION_START_CHECK);
        startService(p2pServiceIntent);
    }

    private void refreshApData() {
        if (null == m_uiUpdateHandler)
            m_uiUpdateHandler = new ShareUIHandler(this);
        updateApStatus();
        listApClients();
    }

    private void updateApStatus() {
        if (!HotspotControl.isSupported()) {
            m_sender_wifi_info.setText("Warning: Hotspot mode not supported!\n");
        }
        if (hotspotControl.isEnabled()) {
            if (!isApEnabled) {
                isApEnabled = true;
                startHostspotCheckOnService();
            }
            WifiConfiguration config = hotspotControl.getConfiguration();
            String ip = Build.VERSION.SDK_INT >= 23 ? WifiUtils.getHostIpAddress() : hotspotControl.getHostIpAddress();
            if (TextUtils.isEmpty(ip))
                ip = "";
            else
                ip = ip.replace("/", "");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                m_toolbar.setSubtitle(getString(R.string.p2p_sender_subtitle));
            }
            m_sender_wifi_info.setText(getString(R.string.p2p_sender_hint_wifi_connected, config.SSID, config.preSharedKey, "http://" + ip + ":" + hotspotControl.getShareServerListeningPort()));
            if (m_showShareList.getVisibility() == View.GONE) {
                m_showShareList.append(String.valueOf(m_sharedFilePaths.length));
                m_showShareList.setVisibility(View.VISIBLE);
            }
        }
        if (null != m_uiUpdateHandler) {
            m_uiUpdateHandler.removeMessages(ShareUIHandler.UPDATE_AP_STATUS);
            m_uiUpdateHandler.sendEmptyMessageDelayed(ShareUIHandler.UPDATE_AP_STATUS, 1500);
        }
    }
    private synchronized void listApClients() {
        if (hotspotControl == null) {
            return;
        }
        hotspotControl.getConnectedWifiClients(2000,
                new HotspotControl.WifiClientConnectionListener() {
                    public void onClientConnectionAlive(final HotspotControl.WifiScanResult wifiScanResult) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addReceiverListItem(wifiScanResult);
                            }
                        });
                    }

                    @Override
                    public void onClientConnectionDead(final HotspotControl.WifiScanResult c) {
                        Log.e(TAG, "onClientConnectionDead: " + c.ip);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onReceiverDisconnected(c.ip);
                            }
                        });
                    }

                    public void onWifiClientsScanComplete() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (null != m_uiUpdateHandler) {
                                    m_uiUpdateHandler.removeMessages(ShareUIHandler.LIST_API_CLIENTS);
                                    m_uiUpdateHandler.sendEmptyMessageDelayed(ShareUIHandler.LIST_API_CLIENTS, 1000);
                                }
                            }
                        });
                    }
                }

        );
    }

    private void resetSenderUi(boolean disableAP) {
        m_uiUpdateHandler.removeCallbacksAndMessages(null);
        m_sender_wifi_info.setText(getString(R.string.p2p_sender_hint_text));
        m_receivers_list_layout.setVisibility(View.GONE);
        m_showShareList.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            m_toolbar.setSubtitle("");
        }
        if (disableAP)
            disableAp();
        else {
            changeApControlCheckedStatus(false);
        }
        if (null != m_receiversListAdapter)
            m_receiversListAdapter.clear();
        m_noReceiversText.setVisibility(View.VISIBLE);
    }

    private void changeApControlCheckedStatus(boolean checked) {
        m_apControlSwitch.setOnCheckedChangeListener(null);
        m_apControlSwitch.setChecked(checked);
        m_apControlSwitch.setOnCheckedChangeListener(m_sender_ap_switch_listener);
        shouldAutoConnect = checked;
    }

    private void updateReceiverListItem(String ip, int progress, String updatetext, String fileName) {
        View taskListItem = m_receiversList.findViewWithTag(ip);
        if (null != taskListItem) {
            ReceiversListItemHolder holder = new ReceiversListItemHolder(taskListItem);
            if (updatetext.contains("Error in file transfer")) {
                holder.resetTransferInfo(fileName);
                return;
            }
            holder.update(fileName, updatetext, progress);
        } else {
            Log.e(TAG, "no list item found with this IP******");
        }
    }

    private void addReceiverListItem(HotspotControl.WifiScanResult wifiScanResult) {
        List<HotspotControl.WifiScanResult> wifiScanResults = m_receiversListAdapter.getObjects();
        if (null != wifiScanResults && wifiScanResults.indexOf(wifiScanResult) != -1) {
            Log.e(TAG, "duplicate client, try updating connection status");
            View taskListItem = m_receiversList.findViewWithTag(wifiScanResult.ip);
            if (null == taskListItem)
                return;
            ReceiversListItemHolder holder = new ReceiversListItemHolder(taskListItem);
            if (holder.isDisconnected()) {
                Log.d(TAG, "changing disconnected ui to connected: " + wifiScanResult.ip);
                holder.setConnectedUi(wifiScanResult);
            }
        } else {
            m_receiversListAdapter.add(wifiScanResult);
            if (m_noReceiversText.getVisibility() == View.VISIBLE)
                m_noReceiversText.setVisibility(View.GONE);
        }
    }

    private void onReceiverDisconnected(String ip) {
        View taskListItem = m_receiversList.findViewWithTag(ip);
        if (null != taskListItem) {
            ReceiversListItemHolder holder = new ReceiversListItemHolder(taskListItem);
            if (!holder.isDisconnected())
                holder.setDisconnectedUi();
//            m_receiversListAdapter.remove(new WifiApControl.Client(ip, null, null));
        }
        if (m_receiversListAdapter.getItemCount() == 0)
            m_noReceiversText.setVisibility(View.VISIBLE);
    }

    static class ReceiversListItemHolder extends RecyclerView.ViewHolder {
        TextView title, connection_status;

        ReceiversListItemHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.p2p_receiver_title);
            connection_status = (TextView) itemView.findViewById(R.id.p2p_receiver_connection_status);
        }

        void setConnectedUi(HotspotControl.WifiScanResult wifiScanResult) {
            title.setText(wifiScanResult.ip);
            connection_status.setText("Connected");
            connection_status.setTextColor(Color.GREEN);
        }

        void resetTransferInfo(String fileName) {
            View v = itemView.findViewWithTag(fileName);
            if (null == v) {
                Log.e(TAG, "resetTransferInfo - no view found with file name tag!!");
                return;
            }
            ((TextView) v).setText("");
        }

        void update(String fileName, String transferData, int progress) {
            View v = itemView.findViewWithTag(fileName);
            if (null == v) {
                Log.e(TAG, "update - no view found with file name tag!!");
                return;
            }
            if (v.getVisibility() == View.GONE)
                v.setVisibility(View.VISIBLE);
            ((TextView) v).setText(transferData);
        }

        void setDisconnectedUi() {
            connection_status.setText("Disconnected");
            connection_status.setTextColor(Color.RED);
        }

        boolean isDisconnected() {
            return "Disconnected".equalsIgnoreCase(connection_status.getText().toString());
        }
    }

    private static class ReceiversListingAdapter extends RecyclerViewArrayAdapter<HotspotControl.WifiScanResult, ReceiversListItemHolder> {
        String[] sharedFiles;

        ReceiversListingAdapter(ArrayList<HotspotControl.WifiScanResult> objects, String[] sharedFiles) {
            super(objects);
            this.sharedFiles = sharedFiles;
        }

        @Override
        public ReceiversListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout itemView = (LinearLayout) LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.listitem_receivers, parent, false);
            //Add at least those many textviews of shared files list size so that if a receiver decides to download them all at once, list item can manage to show status of all file downloads
            if (null != sharedFiles && sharedFiles.length > 0)
                for (int i = 0; i < sharedFiles.length; i++) {
                    TextView statusView = (TextView) LayoutInflater.from(parent.getContext()).
                            inflate(R.layout.include_sender_list_item, parent, false);
                    statusView.setTag(sharedFiles[i].substring(sharedFiles[i].lastIndexOf('/') + 1, sharedFiles[i].length()));
                    statusView.setVisibility(View.GONE);
                    statusView.setTextColor(Utils.getRandomColor());
                    itemView.addView(statusView);
                }
            return new ReceiversListItemHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ReceiversListItemHolder holder, int position) {
            HotspotControl.WifiScanResult receiver = mObjects.get(position);
            if (null == receiver)
                return;
            holder.itemView.setTag(receiver.ip);
            holder.setConnectedUi(receiver);
        }
    }
    //endregion: Wifi Clients Listing

    //region: UI Handler
    static class ShareUIHandler extends Handler {
        WeakReference<HandOverActivity> mActivity;

        static final int LIST_API_CLIENTS = 100;
        static final int UPDATE_AP_STATUS = 101;

        ShareUIHandler(HandOverActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            HandOverActivity activity = mActivity.get();
            if (null == activity || msg == null || !activity.m_apControlSwitch.isChecked())
                return;
            if (msg.what == LIST_API_CLIENTS) {
                activity.listApClients();
            } else if (msg.what == UPDATE_AP_STATUS) {
                activity.updateApStatus();
            }
        }
    }
    //endregion: UI Handler

}