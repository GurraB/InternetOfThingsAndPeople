package com.example.julien.iotap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

/**
 * Created by julien on 15/11/17.
 */

public class ErrorFragment extends DialogFragment {
    // Attributes
    private int msg = R.string.default_err_msg;

    // Méthods
    public ErrorFragment setMsg(int msg) {
        this.msg = msg;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.err_dialog_title);
        builder.setMessage(msg);

        return builder.create();
    }
}
