package com.example.julien.iotap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by julien on 26/11/17.
 */

public class ResetConfirmFragment extends DialogFragment {
    // Attributes
    OnResetListener m_listener;

    // Events
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            m_listener = (OnResetListener) context;
        } catch (ClassCastException err) {
            throw new ClassCastException(context.toString() + " must implement OnResetListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.reset_button);
        builder.setMessage(R.string.reset_msg);
        builder.setPositiveButton(R.string.reset_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                m_listener.onReset();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        return builder.create();
    }

    // Interface
    interface OnResetListener {
        void onReset();
    }
}
