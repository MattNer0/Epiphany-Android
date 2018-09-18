package com.neromatt.epiphany.ui.RackFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.neromatt.epiphany.ui.R;

public class CreateRackDialog extends DialogFragment {

    public interface CreateRackDialogListener {
        void onDialogPositiveClick(CreateRackDialog dialog, String text);
        void onDialogNegativeClick(CreateRackDialog dialog);
    }

    // Use this instance of the interface to deliver action events
    CreateRackDialogListener mListener;

    public void setDialogListener(CreateRackDialogListener mListener){
        this.mListener = mListener;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.create_notebook_dialog, null);
        builder.setView(view);
        builder.setPositiveButton(R.string.create_notebook, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if(mListener!=null) {
                    EditText rackName = (EditText) view.findViewById(R.id.folder_name);
                    mListener.onDialogPositiveClick(CreateRackDialog.this, rackName.getText().toString());
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(mListener!=null)
                    mListener.onDialogNegativeClick(CreateRackDialog.this);
            }
        });
        return builder.create();
    }
}
