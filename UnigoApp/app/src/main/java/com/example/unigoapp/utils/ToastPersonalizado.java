package com.example.unigoapp.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unigoapp.R;

public class ToastPersonalizado {
    public static void showToast(Context context, String message, int iconResId) {
        View layout = LayoutInflater.from(context).inflate(R.layout.toast_personalizado, null);

        TextView text = layout.findViewById(R.id.tvToast);
        ImageView icon = layout.findViewById(R.id.ivToast);

        text.setText(message);

        if (iconResId != 0) {
            icon.setImageResource(iconResId);
            icon.setVisibility(View.VISIBLE);
        }

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    public static void showToast(Context context, String message) {
        showToast(context, message, 0);
    }
}
