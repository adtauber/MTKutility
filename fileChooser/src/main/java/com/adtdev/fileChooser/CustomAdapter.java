package com.adtdev.fileChooser;

import android.content.Context;
import android.graphics.drawable.Drawable;
//import android.support.design.widget.Snackbar;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class CustomAdapter extends ArrayAdapter<DataModel> {

//    private ArrayList<DataModel> dataSet;
//    Context mContext;

    private static final int LOCALsel = 0;
    private static final int FTPsel = 1;
    private Context mContext;
    private int id;
    private List<DataModel> items;

    // View lookup cache
    private static class ViewHolder {
        ImageView icon;
        TextView name;
        TextView data;
        TextView date;
    }

    public CustomAdapter(Context context, int textViewResourceId, List<DataModel> objects) {
        super(context, R.layout.file_view, objects);

        mContext = context;
        id = textViewResourceId;
        items = objects;
    }

    private int lastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        DataModel dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view

        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.file_view, parent, false);
            viewHolder.name = convertView.findViewById(R.id.name);
            viewHolder.data = convertView.findViewById(R.id.data);
            viewHolder.date = convertView.findViewById(R.id.date);
            viewHolder.icon = convertView.findViewById(R.id.icon);
            viewHolder.icon.setLongClickable(true);
            result = convertView;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        result.startAnimation(animation);
        lastPosition = position;

        // Cache row position inside the button using `setTag`
        viewHolder.icon.setTag(position);
        // Attach the click event handlers
        viewHolder.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (Integer) view.getTag();
                Object object = getItem(position);
                DataModel dataModel = (DataModel) object;
                //return object to a FileChooser method
                FileChooser act = (FileChooser) mContext;
                act.startsendBack(dataModel);
            }
        });

        viewHolder.icon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int position = (Integer) view.getTag();
                Object object = getItem(position);
                String ns = ((DataModel) object).getName();
                DataModel dataModel = (DataModel) object;
                //return object to a FileChooser method
                FileChooser act = (FileChooser) mContext;
                act.longpress(dataModel);
                //absorb keypress to prevent onClick activating
                return true;
            }
        });

        String uri = "drawable/" + dataModel.getimage();
        int imageResource = mContext.getResources().getIdentifier(uri, null, mContext.getPackageName());
        Drawable image = mContext.getResources().getDrawable(imageResource);
        viewHolder.icon.setImageDrawable(image);
        viewHolder.name.setText(dataModel.getName());
        viewHolder.data.setText(dataModel.getdata());
        viewHolder.date.setText(dataModel.getdate());
        return convertView;
    }
}
