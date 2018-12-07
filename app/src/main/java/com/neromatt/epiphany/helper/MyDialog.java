package com.neromatt.epiphany.helper;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.neromatt.epiphany.ui.InstantAutoComplete;
import com.neromatt.epiphany.ui.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class MyDialog extends DialogFragment {

    private boolean dialog_open = false;

    private boolean dialog_select = false;
    private HashMap<String, String> select_array;

    private int positive_button_label = R.string.create_notebook;

    private MyDialogListener mListener;
    private Handler mHandler;
    private EditText edit_text;

    public MyDialog() {}

    void setDialogListener(MyDialogListener mListener) {
        this.mListener = mListener;
    }

    private void focusView(EditText view) {
        edit_text = view;

        mHandler = new Handler();
        mHandler.postDelayed(
            new Runnable() {
                public void run() {
                    if (edit_text != null) {
                        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.toggleSoftInputFromWindow(edit_text.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                        edit_text.requestFocus();
                        edit_text.setSelection(edit_text.getText().length());
                    }
                }
            }, 100);
    }

    private void clearFocus() {
        if (edit_text != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edit_text.getApplicationWindowToken(), 0);
        }
    }

    private String findByValue(HashMap<String, String> map, String value) {
        for (String k: map.keySet()) {
            if (map.get(k).equals(value)) return k;
        }
        return "";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (dialog_open) return null;
        AlertDialog.Builder builder;
        LayoutInflater inflater = getActivity().getLayoutInflater();

        Bundle args = getArguments();
        final View view;
        if (args != null) {
            if (args.containsKey("select")) {
                dialog_select = true;
                select_array = (HashMap<String, String>) args.getSerializable("select");
                view = inflater.inflate(R.layout.create_notebook_dialog_select, null);

                ArrayList<String> keys = new ArrayList<>(select_array.keySet());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.my_list_item, keys);
                final InstantAutoComplete textView = view.findViewById(R.id.folder_name);
                textView.setText(findByValue(select_array, args.getString("placeholder", "")));
                textView.setAdapter(adapter);

                focusView(textView);

            } else {
                view = inflater.inflate(R.layout.create_notebook_dialog, null);
                final EditText notebookName = view.findViewById(R.id.folder_name);
                notebookName.setText(args.getString("placeholder", ""));
                focusView(notebookName);
            }

            if (args.getInt("positive", 0) > 0)
                positive_button_label = args.getInt("positive");

        } else {
            view = inflater.inflate(R.layout.create_notebook_dialog, null);
        }

        dialog_open = true;
        builder = new AlertDialog.Builder(getActivity())
            .setView(view)
            .setTitle("")
            .setPositiveButton(positive_button_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog_open = false;
                    clearFocus();

                    if (mListener != null) {
                        if (dialog_select && select_array != null) {
                            InstantAutoComplete notebookName = view.findViewById(R.id.folder_name);
                            mListener.onDialogPositiveClick(MyDialog.this, select_array.get(notebookName.getText().toString()));

                        } else {
                            EditText notebookName = view.findViewById(R.id.folder_name);
                            mListener.onDialogPositiveClick(MyDialog.this, notebookName.getText().toString());
                        }
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog_open = false;
                    clearFocus();

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
