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

package com.launcher.silverfish.launcher;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.launcher.silverfish.R;
import com.launcher.silverfish.launcher.appdrawer.TabbedAppDrawerFragment;
import com.launcher.silverfish.launcher.homescreen.HomeScreenFragment;
import com.launcher.silverfish.launcher.settings.SettingsScreenFragment;

public class LauncherPagerAdapter extends FragmentStatePagerAdapter {

    //region Fields

    // Store a context so we can use getString in later methods
    final private Context _context;

    //endregion

    //region Constructor

    public LauncherPagerAdapter(FragmentManager fm, Context c) {
        super(fm);
        _context = c;
    }

    //endregion

    //region Get items

    @Override
    public Fragment getItem(int pageNumber) {
        // Return the right fragment for given page number
        switch (pageNumber) {
            case 1: // First page is the app drawer
                return new TabbedAppDrawerFragment();

            case 0: // Second page is the 'home screen'
                return new HomeScreenFragment();

            case 2: // Third page is the 'settings activity'
                return new SettingsScreenFragment();

            default: // Any other page (such as last) is an empty fragment
                return new Fragment();
        }
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public CharSequence getPageTitle(int pageNumber) {
        switch (pageNumber) {
            case 1:
                return _context.getString(R.string.text_app_drawer);
            case 0:
                return _context.getString(R.string.text_home_screen);
            case 2:
                return _context.getString(R.string.text_settings_screen);
            default:
                return _context.getString(R.string.text_empty);
        }
    }

    //endregion
}
