package com.example.handover;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.handover.activity.HandOverActivity;
import com.example.handover.activity.HandOverService;
import com.example.handover.activity.ReceiverActivity;
import com.example.handover.utils.HotspotControl;
import com.example.handover.utils.Utils;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;

import static com.example.handover.utils.Utils.DEFAULT_PORT_OREO;

public class ShareFragment extends Fragment  {

    FilePickerDialog dialog;
    View v;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_share, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button send = v.findViewById(R.id.bt_send);
        Button receive = v.findViewById(R.id.bt_receive);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFiles(v);
            }
        });

        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveFiles(v);
            }
        });
    }

    public void sendFiles(View view) {
        if (Utils.isShareServiceRunning(getContext())) {
            startActivity(new Intent(getContext(), HandOverActivity.class));
            return;
        }
        DialogProperties properties = new com.example.handover.DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        dialog = new FilePickerDialog(getActivity(), properties);
        dialog.setTitle("Select files to share");

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (null == files || files.length == 0) {
                    Toast.makeText(getActivity(), "Select at least one file", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(getContext(), HandOverActivity.class);
                intent.putExtra(HandOverService.EXTRA_FILE_PATHS, files);
                intent.putExtra(HandOverService.EXTRA_PORT, DEFAULT_PORT_OREO);
                intent.putExtra(HandOverService.EXTRA_SENDER_NAME, "Sri");
                startActivity(intent);
            }
        });
        dialog.show();
    }

    public void receiveFiles(View view) {
        HotspotControl hotspotControl = HotspotControl.getInstance(getContext());
        if (null != hotspotControl && hotspotControl.isEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Sender(Hotspot) mode is active. Please disable it to proceed with Receiver mode");
            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.show();
            return;
        }
        startActivity(new Intent(getContext(), ReceiverActivity.class));
    }

    //Add this method to show Dialog when the required permission has been granted to the app.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (dialog != null) {   //Show dialog if the read permission has been granted.
                        dialog.show();
                    }
                } else {
                    //Permission has not been granted. Notify the user.
                    Toast.makeText(getActivity(), "Permission is Required for getting list of files", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}