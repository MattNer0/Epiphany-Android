package com.neromatt.epiphany.helper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.neromatt.epiphany.ui.R;

public class IconHelper {
    private static String getRandomMaterialColor(Context context, String typeColor) {
        String returnColor = "#ff999999";
        int arrayId = context.getResources().getIdentifier("mdcolor_" + typeColor, "array", context.getPackageName());

        if (arrayId != 0) {
            TypedArray colors = context.getResources().obtainTypedArray(arrayId);
            int index = (int) (Math.random() * colors.length());
            returnColor = colors.getString(index);
            colors.recycle();
        }
        return returnColor;
    }

    private static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        try {
            display.getRealSize(size);
        } catch (NoSuchMethodError err) {
            display.getSize(size);
        }
        return size;
    }

    public static String setIcon(RelativeLayout iconContainer, String message, int grid_columns, String color) {
        if (iconContainer == null) return color;

        ImageView imgProfile = (ImageView) iconContainer.getChildAt(0);
        TextView iconText = (TextView) iconContainer.getChildAt(1);

        if (color == null) color = getRandomMaterialColor(iconContainer.getContext(), "500");

        String[] words = message.split(" ");
        if (words.length == 2) {
            iconText.setText(words[0].substring(0, 1) + words[1].substring(0, 1));
        } else {
            iconText.setText(message.substring(0, 2));
        }
        imgProfile.setImageResource(R.drawable.bg_circle);
        imgProfile.setColorFilter(Color.parseColor(color));

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imgProfile.getLayoutParams();
        if (grid_columns > 1) {
            Point screen = getScreenSize(iconContainer.getContext());
            params.width = Math.min(screen.x / grid_columns - 10, 300);
            params.height = Math.min(screen.x / grid_columns - 10, 300);
            imgProfile.setLayoutParams(params);

            iconText.setTextSize(TypedValue.COMPLEX_UNIT_PX, params.width * 0.35f);
        } /*else {
            params.width = (int) iconContainer.getContext().getResources().getDimension(R.dimen.icon_width_height);
            params.height = (int) iconContainer.getContext().getResources().getDimension(R.dimen.icon_width_height);
            imgProfile.setLayoutParams(params);

            iconText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) iconContainer.getContext().getResources().getDimension(R.dimen.icon_text));
        }*/

        return color;
    }
}
