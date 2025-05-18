package com.ehunzango.unigo.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import com.ehunzango.unigo.R;
import java.util.List;

public class ImageSpinnerAdapter  extends ArrayAdapter<SpinnerImageItem>
{

    private final Context context;
    private final List<SpinnerImageItem> items;

    public ImageSpinnerAdapter(Context context, List<SpinnerImageItem> items)
    {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.transport_spinner_item, parent, false);
        }

        ImageView icon = convertView.findViewById(R.id.icon);
        SpinnerImageItem item = items.get(position);
        icon.setImageResource(item.iconResId);

        // Fijar un color constante para los iconos
        icon.setColorFilter(R.color.icon_tint);

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent); // Vista seleccionada
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent); // Lista desplegable
    }
}


