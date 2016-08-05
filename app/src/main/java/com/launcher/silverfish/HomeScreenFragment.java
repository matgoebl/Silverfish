/*
 * Copyright 2016 Stanislav Pintjuk
 * E-mail: stanislav.pintjuk@gmail.com
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.launcher.silverfish;

import android.app.Activity;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetHostView;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.launcher.silverfish.sqlite.LauncherSQLiteHelper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HomeScreenFragment extends Fragment {
    LauncherSQLiteHelper sqlhelper;

    // Variables for communication with AppWidgetManager
    private int WIDGET_HOST_ID = 1339;
    private int REQUEST_PICK_APPWIDGET = 1340;
    private int REQUEST_CREATE_APPWIDGET = 1341;
    private int REQUEST_BIND_APPWIDGET = 1342;

    private AppWidgetManager mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private PackageManager mPacMan;
    private View rootView;
    private ArrayList<AppDetail> appsList;
    private SquareGridLayout shortcutLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        sqlhelper = new LauncherSQLiteHelper(getActivity().getBaseContext());

        //initiate global variables
        mAppWidgetManager = AppWidgetManager.getInstance(getActivity().getBaseContext());
        mAppWidgetHost = new LauncherAppWidgetHost(getActivity().getApplicationContext(), WIDGET_HOST_ID);
        mAppWidgetHost.startListening();

        mPacMan = getActivity().getPackageManager();
        appsList = new ArrayList<AppDetail>();

        rootView = inflater.inflate(R.layout.activity_home, container, false);
        shortcutLayout = (SquareGridLayout)rootView.findViewById(R.id.shortcut_area);

        // Start listening for shortcut additions
        ((LauncherActivity)getActivity()).setFragmshortcutAddListenertRefreshListener(new ShortcutAddListener() {
            @Override
            public void OnShortcutAdd(String app_name) {
                // insert it into the database and get the row id.
                // TODO: Check if an error has occurred while inserting into database.
                long app_id = sqlhelper.addShortcut(app_name);

                // create shortcut and add it
                ShortcutDetail shortcut = new ShortcutDetail();
                shortcut.name = app_name;
                shortcut.id = app_id;
                if (addAppToView(shortcut)){
                    updateShortcuts();
                }
            }
        });

        addWidgetOnClickListener();
        setOnDragListener();

        loadWidget();
        loadApps();
        updateShortcuts();

        return rootView;
    }

    private void loadWidget() {
        ComponentName cn = sqlhelper.getWidgetContentName();

        Log.d("Widget creation", "Loaded from db: " + cn.getClassName() + " - " + cn.getPackageName());
        // Check that there actually is a widget in the database
        if (cn.getPackageName().equals("") && cn.getClassName().equals("")){
            Log.d("Widget creation", "DB was empty");
            return;
        }
        Log.d("Widget creation", "DB was not empty");

        final List<AppWidgetProviderInfo> infos = mAppWidgetManager.getInstalledProviders();

        //get AppWidgetProviderInfo
        AppWidgetProviderInfo appWidgetInfo = null;
        //just in case you want to see all package and class names of installed widget providers, this code is useful
        for (final AppWidgetProviderInfo info : infos) {
            Log.d("AD3", info.provider.getPackageName() + " / "
                    + info.provider.getClassName());
        }
        //iterate through all infos, trying to find the desired one
        for (final AppWidgetProviderInfo info : infos) {
            if (info.provider.getClassName().equals(cn.getClassName()) && info.provider.getPackageName().equals(cn.getPackageName())) {
                //we found it
                appWidgetInfo = info;
                break;
            }
        }
        if (appWidgetInfo == null) {
            Log.d("Widget creation", "app info was null");
            return; //stop here
        }

        //allocate the hosted widget id
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

        boolean allowed_to_bind = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn);

        // Ask the user to allow this app to have access to their widgets
        if (!allowed_to_bind){
            Log.d("Widget creation", "asking for permission");
            Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            Bundle args = new Bundle();
            args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            args.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, cn);
            args.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, null);
            i.putExtras(args);
            startActivityForResult(i, REQUEST_BIND_APPWIDGET);
            return;
        }else {

            Log.d("Widget creation", "Allowed to bind");
            Log.d("Widget creation", "creating widget");
            //Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            //createWidgetFromId(appWidgetId);
        }
        ////creat the host view
        AppWidgetHostView hostView = mAppWidgetHost.createView(getActivity().getBaseContext(), appWidgetId, appWidgetInfo);
        //set the desired widget
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        placeWidget(hostView);
    }


    private boolean addAppToView(ShortcutDetail shortcut){
        try {
            ApplicationInfo appInfo = mPacMan.getApplicationInfo(shortcut.name,PackageManager.GET_META_DATA);
            AppDetail appDetail = new AppDetail();
            appDetail.label = mPacMan.getApplicationLabel(appInfo);
            appDetail.icon = mPacMan.getApplicationIcon(appInfo);
            appDetail.name = shortcut.name;
            appDetail.id = shortcut.id;

            appsList.add(appDetail);
            return true;

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void removeApp(int app_index, long app_id){
        sqlhelper.removeShorcut(app_id);
        appsList.remove(app_index);
    }

    private void updateShortcuts(){
        int count = appsList.size();
        int size = (int)Math.ceil(Math.sqrt(count));
        shortcutLayout.removeAllViews();

        switch (size){
            case 0:
                // special case: if appsList is empty
                shortcutLayout.setSize(1);
                return;
            case 1:
                // Just make it look better when there are just a few apps
                shortcutLayout.setPadding(0,200,0,0);
                break;
            case 2:
                shortcutLayout.setPadding(0,100,0,0);
                break;
            default:
                shortcutLayout.setPadding(0,0,0,0);
        }

        // Redraw the layout
        shortcutLayout.setSize(size);
        shortcutLayout.requestLayout();
        shortcutLayout.invalidate();

        for (int i = 0; i < appsList.size(); i++){
            final AppDetail app = appsList.get(i);
            View convertView = getActivity().getLayoutInflater().inflate(R.layout.shortcut_item, null);
            ImageView im = (ImageView)convertView.findViewById(R.id.item_app_icon);
            im.setImageDrawable(app.icon);
            TextView tv = (TextView)convertView.findViewById(R.id.item_app_label);
            tv.setText(app.label);
            shortcutLayout.addView(convertView);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = mPacMan.getLaunchIntentForPackage(app.name.toString());
                    startActivity(i);
                }
            });

            // start a drag when an app has been long clicked
            final long app_id = app.id;
            final int app_index = i;
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    String[] mime_types = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                    ClipData data = new ClipData(Constants.DRAG_SHORTCUT_REMOVAL, mime_types, new ClipData.Item(Long.toString(app_id)));
                    data.addItem(new ClipData.Item(Integer.toString(app_index)));

                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view.findViewById(R.id.item_app_icon));
                    view.startDrag(data, shadowBuilder, view, 0);

                    // show removal indicator
                    FrameLayout rem_ind  = (FrameLayout)rootView.findViewById(R.id.remove_indicator);
                    rem_ind.setVisibility(View.VISIBLE);
                    AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
                    animation.setDuration(500);
                    rem_ind.startAnimation(animation);
                    return true;

                }
            });

        }
    }

    private void setOnDragListener(){
        rootView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                switch (dragEvent.getAction()){
                    case DragEvent.ACTION_DRAG_STARTED:
                        // Check that it is a shortcut removal gesture
                        ClipDescription cd = dragEvent.getClipDescription();
                        if (!cd.getLabel().toString().equals(Constants.DRAG_SHORTCUT_REMOVAL)) {
                            return false;
                        }
                        break;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        //Dont do anything
                        break;
                    case DragEvent.ACTION_DRAG_LOCATION:
                        //Dont do anything
                        break;
                    case DragEvent.ACTION_DROP:

                        // if outside of bound, remove the app
                        if (Utils.onBottomScreenEdge(getActivity(), dragEvent.getY())){
                            String appid = dragEvent.getClipData().getItemAt(0).getText().toString();
                            String appindex = dragEvent.getClipData().getItemAt(1).getText().toString();
                            removeApp(Integer.parseInt(appindex), Long.parseLong(appid));
                            updateShortcuts();
                        }

                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        // Hide the remove-indicator
                        FrameLayout rem_ind  = (FrameLayout)rootView.findViewById(R.id.remove_indicator);
                        rem_ind.setVisibility(View.INVISIBLE);
                        break;

                }
                return true;
            }
        });
    }


    private void loadApps() {

        LinkedList<ShortcutDetail> shortcuts = sqlhelper.getAllShortcuts();
        for (ShortcutDetail shortcut : shortcuts){

            boolean success = addAppToView(shortcut);

            // if the shortcut could not be added then the user has probably uninstalled it.
            // so we should remove it from the db
            if (!success){
                Log.d("HomeFragment", "Removing shortcut "+shortcut.name+" from db");
                sqlhelper.removeShorcut(shortcut.id);
            }
        }
    }

    private void addWidgetOnClickListener(){
        // Long click on widget area should start up widget selection
        FrameLayout widget_area = (FrameLayout)rootView.findViewById(R.id.widget_area);
        widget_area.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                selectWidget();
                return true;
            }
        });

    }

    private void selectWidget() {
        // allocate widget id and start widget selection activity
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        addEmptyData(pickIntent); // This is needed work around some weird bug.
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);

    }

    private void addEmptyData(Intent pickIntent) {
        // This is needed work around some weird bug.
        // This will simply add some empty data to the intent.
        ArrayList customInfo = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList customExtras = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }

    private void configureWidget(Intent data) {
        // Get the selected widget information
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            // if the widget wants to be configured then start its configuration activity.
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // otherwise simply create it.
            createWidget(data);
        }
    }

    private void createWidget(Intent data) {
        // get the widget id
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        createWidgetFromId(appWidgetId);

    }

    private void createWidgetFromId(int widget_id) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(widget_id);

        // create the hostview
        AppWidgetHostView hostView = mAppWidgetHost.createView(getActivity().getBaseContext(), widget_id, appWidgetInfo);
        hostView.setAppWidget(widget_id, appWidgetInfo);

        // and place the widget in widget area and save.
        placeWidget(hostView);
        sqlhelper.updateWidget(appWidgetInfo.provider.getPackageName(), appWidgetInfo.provider.getClassName());
    }

    private void placeWidget(AppWidgetHostView hostView) {
        FrameLayout widget_area = (FrameLayout) rootView.findViewById(R.id.widget_area);

        widget_area.removeAllViews();
        widget_area.addView(hostView);

        // let the widget host view take control of the long click action.
        hostView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                selectWidget();
                return true;
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // listen for widget manager response
        if (resultCode == Activity.RESULT_OK ) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                configureWidget(data);
            }
            else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                createWidget(data);
            }else if (requestCode == REQUEST_BIND_APPWIDGET){
                createWidget(data);
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mAppWidgetHost.stopListening();
    }
}
