package com.limelight.ui.gamemenu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;

import java.util.List;

final class KeyboardPresetAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<GameMenuQuickBean> items;

    KeyboardPresetAdapter(
            Context context, List<GameMenuQuickBean> items) {
        inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public GameMenuQuickBean getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(
            int position, View convertView, ViewGroup parent) {
        View itemView = convertView;
        ViewHolder holder;
        if (itemView == null) {
            itemView = inflater.inflate(
                    R.layout.item_keyboard_mouse,
                    parent,
                    false);
            holder = new ViewHolder(itemView);
            itemView.setTag(holder);
        }
        else {
            holder = (ViewHolder) itemView.getTag();
        }
        holder.bind(getItem(position));
        return itemView;
    }

    private static final class ViewHolder {
        private final TextView name;
        private final TextView description;

        private ViewHolder(View view) {
            name = view.findViewById(R.id.tv_name);
            description = view.findViewById(R.id.tv_desc);
        }

        private void bind(GameMenuQuickBean item) {
            name.setText(item.getName());
            description.setText(item.getDesc());
        }
    }
}
