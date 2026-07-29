package com.limelight.ui.gamemenu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowMetrics;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.limelight.R;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.ui.gamemenu.adapter.GameMenuQuickKeyboardAdapter;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.limelight.ui.gamemenu.GameListKeyBoardFragment.PREF_KEYBOARD_LIST_NAME;

/**
 * Description
 * Date: 2024-10-20
 * Time: 16:07
 */
public class GameListQuickFragment extends BaseGameMenuDialog {
    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_list;
    }

    private ListView lv_menu;
    private ImageButton ibtn_back;
    private TextView tx_title;

    private String title;
    private Button btn_right;
    private boolean enableClearDefaultSpecial;

    private GameMenuQuickKeyboardAdapter adapter;

    private List<GameMenuQuickBean> gameMenus;

    @Override
    public void bindView(View v) {
        super.bindView(v);
        ibtn_back=v.findViewById(R.id.ibtn_back);
        lv_menu=v.findViewById(R.id.lv_menu);
        tx_title=v.findViewById(R.id.tx_title);
        btn_right=v.findViewById(R.id.btn_right);
        if(!TextUtils.isEmpty(title)){
            tx_title.setText(title);
        }

        ibtn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        btn_right.setVisibility(View.VISIBLE);

        btn_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GameKeyboardUpdateFragment fragment=new GameKeyboardUpdateFragment();
                if(isLandscape(getActivity())){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowMetrics windowMetrics = getActivity().getWindowManager().getCurrentWindowMetrics();
                        Rect bounds = windowMetrics.getBounds();
                        fragment.setWidth(bounds.width());
                    }else{
                        fragment.setWidth(getActivity().getResources().getDisplayMetrics().widthPixels);
                    }
                }else{
                    fragment.setWidth((getActivity().getResources().getDisplayMetrics().heightPixels*2)/3);
                }
                fragment.setDimAmount(0.8f);
                fragment.setTitle(
                        R.string.keyboard_shortcut_setup_title);
                fragment.setKeyFrom(1);
                fragment.setOnClick(new GameKeyboardUpdateFragment.onClick() {
                    @Override
                    public void click(GameMenuQuickBean bean) {
                        saveKeyBoardListData(getActivity(),bean);
                        updateData();
                        notifyShortcutsChanged();
                    }
                });
                fragment.show(getFragmentManager());
            }
        });

        gameMenus = loadShortcutBeans();

        adapter=new GameMenuQuickKeyboardAdapter(getActivity(), gameMenus);
        lv_menu.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        lv_menu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(onClick!=null){
                    onClick.click(gameMenus.get(position));
                }
            }
        });
        lv_menu.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(TextUtils.isEmpty(gameMenus.get(position).getId())){
                    return false;
                }
                new AlertDialog.Builder(getActivity())
                        .setTitle(gameMenus.get(position).getName())
                        .setMessage("是否删除此键值？")
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeKeyBoardListData(getActivity(), gameMenus.get(position));
                                updateData();
                                notifyShortcutsChanged();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
                return false;
            }
        });
    }

    public void setEnableClearDefaultSpecial(boolean enableClearDefaultSpecial) {
        this.enableClearDefaultSpecial = enableClearDefaultSpecial;
    }

    public void updateData(){
        gameMenus.clear();
        gameMenus.addAll(loadShortcutBeans());
        adapter.setDatas(gameMenus);
        adapter.notifyDataSetChanged();
    }

    private List<GameMenuQuickBean> loadShortcutBeans() {
        return GameMenuShortcutCatalog.loadBeans(
                getActivity(), !enableClearDefaultSpecial);
    }

    @Override
    public float getDimAmount() {
        return super.getDimAmount();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private onClick onClick;

    public interface onClick{
        void click(GameMenuQuickBean bean);
    }

    public void setOnClick(GameListQuickFragment.onClick onClick) {
        this.onClick = onClick;
    }

    private Runnable shortcutsChangedListener;

    public void setOnShortcutsChangedListener(Runnable listener) {
        shortcutsChangedListener = listener;
    }

    private void notifyShortcutsChanged() {
        if (shortcutsChangedListener != null) {
            shortcutsChangedListener.run();
        }
    }

    public List<GameMenuQuickBean> getKeyBoardList(Context context){
        SharedPreferences pref = context.getSharedPreferences(PREF_QUICK_LIST_NAME, Activity.MODE_PRIVATE);
        Map<String,String> map= (Map<String, String>) pref.getAll();
        List<GameMenuQuickBean> quickBeans=new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            quickBeans.add(new Gson().fromJson(value,GameMenuQuickBean.class));
        }
        return quickBeans;
    }

    public boolean isLandscape(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels>context.getResources().getDisplayMetrics().heightPixels;
    }

    public void removeKeyBoardListData(Context context,GameMenuQuickBean bean){
        SharedPreferences pref = context.getSharedPreferences(PREF_QUICK_LIST_NAME, Activity.MODE_PRIVATE);
        pref.edit().remove(bean.getId()).apply();
    }

    public static final String PREF_QUICK_LIST_NAME="quick_axi_keyAssemble";
    public static final String PREF_QUICK_LIST_KEY="quick_assemble_key_";

    public void saveKeyBoardListData(Context context,GameMenuQuickBean bean){
        SharedPreferences pref = context.getSharedPreferences(PREF_QUICK_LIST_NAME, Activity.MODE_PRIVATE);
        pref.edit().putString(bean.getId(),new Gson().toJson(bean)).apply();
    }
}
