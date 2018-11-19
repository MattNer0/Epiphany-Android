package com.neromatt.epiphany.helper;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.neromatt.epiphany.ui.R;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class MyDialog extends DialogFragment {

    private boolean dialog_open = false;
    private int positive_button_label = R.string.create_notebook;

    private MyDialogListener mListener;

    void setDialogListener(MyDialogListener mListener) {
        this.mListener = mListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (dialog_open) return null;
        AlertDialog.Builder builder;

        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.create_notebook_dialog, null);

        Bundle args = getArguments();
        if (args != null) {
            if (args.getInt("positive", 0) > 0)
                positive_button_label = args.getInt("positive");

            EditText notebookName = view.findViewById(R.id.folder_name);
            notebookName.setText(args.getString("placeholder", ""));
        }

        dialog_open = true;
        builder = new AlertDialog.Builder(getActivity())
            .setView(view)
            .setTitle("")
            .setPositiveButton(positive_button_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog_open = false;
                    if (mListener != null) {
                        EditText notebookName = view.findViewById(R.id.folder_name);
                        mListener.onDialogPositiveClick(MyDialog.this, notebookName.getText().toString());
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog_open = false;
                    if(mListener!=null)
                        mListener.onDialogNegativeClick(MyDialog.this);
                }
            });

        return builder.create();
    }

    public interface MyDialogListener {
        void onDialogPositiveClick(MyDialog dialog, String text);
        void onDialogNegativeClick(MyDialog dialog);
    }
}
