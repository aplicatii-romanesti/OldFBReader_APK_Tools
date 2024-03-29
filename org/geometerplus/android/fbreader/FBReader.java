/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;

import java.util.*;

//import android.support.v4.content.ContextCompat; //maryhit perms
//import android.support.v13.app.ActivityCompat;//maryhit perms
import android.support.v4.app.ActivityCompat;//maryhit perms
// import androidx.core.app.ActivityCompat;
import android.Manifest;//maryhit perms
import android.content.pm.PackageManager;//maryhit perms
import android.widget.Toast;//maryhit perms
import android.util.Log; //pt LOG pt perms

import android.app.PendingIntent; // pendingIntent (pt restart pt perms)
import android.app.AlarmManager; //AlarmManager(pt restart pt perms)
import android.provider.Settings; //perms: if denied with never ask again, send him to settings...
import android.app.AlertDialog; //perms: message why...

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.library.ZLibrary;

import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;

import org.geometerplus.zlibrary.ui.android.aplicatii.romanesti.R;
import org.geometerplus.zlibrary.ui.android.library.*;

import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.tips.TipsManager;

import org.geometerplus.android.fbreader.library.SQLiteBooksDatabase;
import org.geometerplus.android.fbreader.library.KillerCallback;
import org.geometerplus.android.fbreader.api.*;
import org.geometerplus.android.fbreader.tips.TipsActivity;

import org.geometerplus.android.util.UIUtil;

public final class FBReader extends ZLAndroidActivity {
	private static final String TAG = "PERMISSION_WRITE_EXTERNAL_STORAGE: "; //maryhit
	static final int ACTION_BAR_COLOR = Color.DKGRAY;

	public static final String BOOK_PATH_KEY = "BookPath";

	final static int REPAINT_CODE = 1;
	final static int CANCEL_CODE = 2;

	private boolean myShowStatusBarFlag;
	private boolean myShowActionBarFlag;

	private static final String PLUGIN_ACTION_PREFIX = "___";
	private final List<PluginApi.ActionInfo> myPluginActions =
		new LinkedList<PluginApi.ActionInfo>();
	private final BroadcastReceiver myPluginInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final ArrayList<PluginApi.ActionInfo> actions = getResultExtras(true).<PluginApi.ActionInfo>getParcelableArrayList(PluginApi.PluginInfo.KEY);
			if (actions != null) {
				synchronized (myPluginActions) {
					final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
					int index = 0;
					while (index < myPluginActions.size()) {
						fbReader.removeAction(PLUGIN_ACTION_PREFIX + index++);
					}
					myPluginActions.addAll(actions);
					index = 0;
					for (PluginApi.ActionInfo info : myPluginActions) {
						fbReader.addAction(
							PLUGIN_ACTION_PREFIX + index++,
							new RunPluginAction(FBReader.this, fbReader, info.getId())
						);
					}
					if (!myPluginActions.isEmpty()) {
						invalidateOptionsMenu();
					}
				}
			}
		}
	};

	@Override
	protected ZLFile fileFromIntent(Intent intent) {
		String filePath = intent.getStringExtra(BOOK_PATH_KEY);
		if (filePath == null) {
			final Uri data = intent.getData();
			if (data != null) {
				filePath = data.getPath();
			}
		}
		return filePath != null ? ZLFile.createFileByPath(filePath) : null;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		DictionaryUtil.init(this);
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		final ZLAndroidLibrary zlibrary = (ZLAndroidLibrary)ZLibrary.Instance();
		myShowStatusBarFlag = zlibrary.ShowStatusBarOption.getValue();
		myShowActionBarFlag = zlibrary.ShowActionBarOption.getValue();

		final ActionBar bar = getActionBar();
		bar.setDisplayOptions(
			ActionBar.DISPLAY_SHOW_CUSTOM,
			ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_TITLE
		);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			bar.setDisplayUseLogoEnabled(false);
		}
		final TextView titleView = (TextView)getLayoutInflater().inflate(R.layout.title_view, null);
		titleView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				fbReader.runAction(ActionCode.SHOW_BOOK_INFO);
			}
		});
		bar.setCustomView(titleView);
		bar.setBackgroundDrawable(new ColorDrawable(ACTION_BAR_COLOR));

		setTitle(fbReader.getTitle());

		if (fbReader.getPopupById(TextSearchPopup.ID) == null) {
			new TextSearchPopup(fbReader);
		}
		if (fbReader.getPopupById(SelectionPopup.ID) == null) {
			new SelectionPopup(fbReader);
		}

		fbReader.addAction(ActionCode.SHOW_LIBRARY, new ShowLibraryAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_LIBRARY_SDCARD, new ShowLibrarySDCardFolderAction(this, fbReader));//maryhit
		fbReader.addAction(ActionCode.SHOW_LIBRARY_OPEN_BOOKS, new ShowLibraryOpenBooksFolderAction(this, fbReader));//maryhit Nicu/maryhit adaugat pentru buton nou direct in directoare
		fbReader.addAction(ActionCode.SHOW_PREFERENCES, new ShowPreferencesAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOK_INFO, new ShowBookInfoAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_TOC, new ShowTOCAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOKMARKS, new ShowBookmarksAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NETWORK_LIBRARY, new ShowNetworkLibraryAction(this, fbReader));
		
		fbReader.addAction(ActionCode.TOGGLE_BARS, new ToggleBarsAction(this, fbReader));
		fbReader.addAction(ActionCode.SEARCH, new SearchAction(this, fbReader));

		fbReader.addAction(ActionCode.SELECTION_SHOW_PANEL, new SelectionShowPanelAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_HIDE_PANEL, new SelectionHidePanelAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new SelectionCopyAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_SHARE, new SelectionShareAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_TRANSLATE, new SelectionTranslateAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_BOOKMARK, new SelectionBookmarkAction(this, fbReader));

		fbReader.addAction(ActionCode.PROCESS_HYPERLINK, new ProcessHyperlinkAction(this, fbReader));

		fbReader.addAction(ActionCode.SHOW_CANCEL_MENU, new ShowCancelMenuAction(this, fbReader));

		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_SYSTEM));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_SENSOR, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_SENSOR));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_PORTRAIT));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_LANDSCAPE));
		if (ZLibrary.Instance().supportsAllOrientations()) {
			fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT));
			fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE, new SetScreenOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE));
		}
    //start maryhit:
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //ask for sdcard perms...

		  if ( ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
          Log.e(TAG, "We do not have the perms for storage, going to request now.");
    		  ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
    		  Log.e(TAG, "We requested perms, waiting the callback onRequestPermissionsResult...");
 		  }
		}//done maryhit
		fbReader.copyBooksToSDCard(this); //maryhit:  passing context
	}

  // maryhit for perms...
    /**
   * start the App Settings Activity so that the user can change
   * settings related to the application such as permissions.
   */
  private void startAppSettingsConfigActivity() {
      // final Intent i = new Intent();
      // i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      // i.addCategory(Intent.CATEGORY_DEFAULT);
      // i.setData(Uri.parse("package:" + getActivity().getPackageName()));
      // i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      // i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); // "The Intent.FLAG_ACTIVITY_NO_HISTORY may sometimes cause a problemous situation on tablets when a pop-up is shown within the settings screen it will close the settings screen. Simply removing that flag will solve this issue."
      // i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
      // getActivity().startActivity(i);

  	//
			Uri uri = new Uri.Builder()
			        .scheme("package")
			        .opaquePart(getPackageName())
			        .build();
			final Intent i = new Intent();
			i.setData(uri);
			i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			//i.addCategory(Intent.CATEGORY_DEFAULT);
			//i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // If set then opens Settings Screen(Activity) as new activity. Otherwise, it will be opened in currently running activity.
			startActivity(i);
			//startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri));
  }
  // maryhit for perms...
  public static void doRestart(Context c) {
        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity 
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called. 
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 300, mPendingIntent);
                        //kill the application
                        android.os.Process.killProcess(android.os.Process.myPid());//maryhit
                        System.exit(0);
                    } else {
                        Log.e(TAG, "Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Log.e(TAG, "Was not able to restart application, PM null");
                }
            } else {
                Log.e(TAG, "Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Was not able to restart application");
        }
  }
	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String[] permissions,
	                                       int[] grantResults) {

			    // if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
	      //     Log.w(TAG, "shouldShowRequestPermissionRationale: true, meaning we asked before and it was deny.Now we ask again.");
       //      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
	      //   } else {
	      //   	Log.w(TAG, "they denied and set neverAskAgain...Write External Storage permission allows us to do store the books.");
	      //     this.startAppSettingsConfigActivity();
	      //     this.doRestart(this);
	      //     ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
	      //     Log.w(TAG, "they denied and set neverAskAgain...Write External Storage permission allows us to do store the books.");
	      //   }


	    if (requestCode == 7) {
	        if(grantResults.length == 1
	           && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
	        	Log.e(TAG, "onRequestPermissionsResult: PERMISSION_GRANTED, going to restart the app.");
	            // We can now safely use the API we requested access to WRITE_EXTERNAL_STORAGE
	        	  //this.recreate(); // we recreate the activity, in order to write to sdcard.
	        	  // Intent intent = getIntent();
	      //   	  getActivity().finish();
							// finish();
							// startActivity(intent);
	        	// this.finishAffinity();
					  // android.os.Process.killProcess(android.os.Process.myPid());
					  // System.exit(1);          
					     this.doRestart(this);
	        } else {
	        	Log.e(TAG, "onRequestPermissionsResult: Perm was denied, checking if it was with neverAskAgain...");

 				    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
	            Log.w(TAG, "shouldShowRequestPermissionRationale: true, meaning it was denied but we can ask again later on.");
	            
	            //msg
              Log.w(TAG, "shouldShowRequestPermissionRationale: true, going to show rationale message");
              String message = "Cartile incluse sunt extrase si folosite pe SDCard-ul dispozitivului. Fara a avea acces la ele, aplicatia nu poate functiona. (varianta aplicatiei pentru versiunile mai noi de Android, nu mai are nevoie de aceasta permisiune). La fel nici aplicatia, 'biblioteca ortodoxa' nu mai foloseste SDCard-ul.";
              DialogInterface.OnClickListener listenerRestart = null;
              listenerRestart = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //this will be executed if User clicks OK button. This is gonna take the user to the App Settings
                    Context c = FBReader.this;
                    //startAppSettingsConfigActivity();
                    FBReader.doRestart(c);
                }
              };

              DialogInterface.OnClickListener listenerClose = null;
              listenerClose = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //this will be executed if User clicks OK button. This is gonna take the user to the App Settings
                    Context c = FBReader.this;
                    //startAppSettingsConfigActivity();
					          android.os.Process.killProcess(android.os.Process.myPid());
					          System.exit(1);          
                }
              };
	            new AlertDialog.Builder(this) //, R.style.AlertDialogTheme)
	            .setCancelable(false)
	            .setMessage(message)
	            .setPositiveButton("Reporneste", listenerRestart)
	            .setNegativeButton("Inchide", listenerClose)
	            .create().show();
              //end msg
	            //this.doRestart(this);
	          }
	          else {
	          	Log.w(TAG, "they denied and set neverAskAgain. Sending him to App setting and exit.");

	          		            //msg
              Log.w(TAG, "shouldShowRequestPermissionRationale: true, going to show rationale message");
              String message = "Cartile incluse sunt extrase si folosite pe SDCard-ul dispozitivului. Fara a avea acces la ele, aplicatia nu poate functiona. (varianta aplicatiei pentru versiunile mai noi de Android, nu mai are nevoie de aceasta permisiune). La fel nici aplicatia, 'biblioteca ortodoxa' nu mai foloseste SDCard-ul.";
              DialogInterface.OnClickListener listenerneverAskAgainRestart = null;
              listenerneverAskAgainRestart = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startAppSettingsConfigActivity();
           //          android.os.Process.killProcess(android.os.Process.myPid());
					  				// System.exit(1);   
                }
              };
              DialogInterface.OnClickListener listenerneverAskAgainClose = null;
              listenerneverAskAgainClose = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //this will be executed if User clicks OK button. This is gonna take the user to the App Settings
                    Context c = FBReader.this;
                    //startAppSettingsConfigActivity();
					          android.os.Process.killProcess(android.os.Process.myPid());
					          System.exit(1);          
                }
              };
	            new AlertDialog.Builder(this) //, R.style.AlertDialogTheme)
	            .setCancelable(false)
	            .setMessage(message)
	            .setPositiveButton("Reporneste", listenerneverAskAgainRestart)
	            .setNegativeButton("Inchide", listenerneverAskAgainClose)
	            .create().show();
              //end msg

	          	// this.startAppSettingsConfigActivity();
						  // android.os.Process.killProcess(android.os.Process.myPid());
					  	// System.exit(1);        
	          }


	        	// this.startAppSettingsConfigActivity();
	        	
	        	  // Toast.makeText(this, "2.Toate cartile vor fi scrise si folosite de pe SDCard-ul telefonului. Pentru aceasta aplicatia are nevoie de permisiunea dvs. Fara aceasta permisiune, aplicatia nu poate functiona.", Toast.LENGTH_LONG).show();
	        	  //this.recreate();
	            // Permission was denied or request was cancelled
	          //this.finishAndRemoveTask(); //api 21+ ; Finishes all activities in this task and removes it from the recent tasks list.
	          //this.finishAffinity(); //api 16+          
	          //getActivity().finish(); //finishes only current activity
					  // moveTaskToBack(true); //move to background

					  // android.os.Process.killProcess(android.os.Process.myPid());
					  // System.exit(1);            
					  // this.startAppSettingsConfigActivity();
	        }
	    }
	}

	@Override
	protected void onNewIntent(Intent intent) {
		final Uri data = intent.getData();
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
			super.onNewIntent(intent);
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())
					&& data != null && "fbreader-action".equals(data.getScheme())) {
			fbReader.doAction(data.getEncodedSchemeSpecificPart(), data.getFragment());
		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final String pattern = intent.getStringExtra(SearchManager.QUERY);
			final Runnable runnable = new Runnable() {
				public void run() {
					final TextSearchPopup popup = (TextSearchPopup)fbReader.getPopupById(TextSearchPopup.ID);
					popup.initPosition();
					fbReader.TextSearchPatternOption.setValue(pattern);
					if (fbReader.getTextView().search(pattern, true, false, false, false) != 0) {
						runOnUiThread(new Runnable() {
							public void run() {
								fbReader.showPopup(popup.getId());
								hideBars();
							}
						});
					} else {
						runOnUiThread(new Runnable() {
							public void run() {
								UIUtil.showErrorMessage(FBReader.this, "textNotFound");
								popup.StartPosition = null;
							}
						});
					}
				}
			};
			UIUtil.wait("search", runnable, this);
		} else {
			super.onNewIntent(intent);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		final ZLAndroidLibrary zlibrary = (ZLAndroidLibrary)ZLibrary.Instance();

		if (zlibrary.ShowStatusBarOption.getValue() != myShowStatusBarFlag ||
			zlibrary.ShowActionBarOption.getValue() != myShowActionBarFlag) {
			finish();
			startActivity(new Intent(this, getClass()));
		}

		SetScreenOrientationAction.setOrientation(this, zlibrary.OrientationOption.getValue());

		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		final RelativeLayout root = (RelativeLayout)findViewById(R.id.root_view);
		((PopupPanel)fbReader.getPopupById(TextSearchPopup.ID)).setPanelInfo(this, root);
		((PopupPanel)fbReader.getPopupById(SelectionPopup.ID)).setPanelInfo(this, root);

		synchronized (myPluginActions) {
			if (!myPluginActions.isEmpty()) {
				int index = 0;
				while (index < myPluginActions.size()) {
					fbReader.removeAction(PLUGIN_ACTION_PREFIX + index++);
				}
				myPluginActions.clear();
				invalidateOptionsMenu();
			}
		}

		sendOrderedBroadcast(
			new Intent(PluginApi.ACTION_REGISTER).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
			null,
			myPluginInfoReceiver,
			null,
			RESULT_OK,
			null,
			null
		);

		new TipRunner().start();
	}

	private class TipRunner extends Thread {
		TipRunner() {
			setPriority(MIN_PRIORITY);
		}

		public void run() {
		final TipsManager manager = TipsManager.Instance();
		switch (manager.requiredAction()) {
			case Initialize:
					startActivity(new Intent(
						TipsActivity.INITIALIZE_ACTION, null, FBReader.this, TipsActivity.class
					));
				break;
			case Show:
					startActivity(new Intent(
						TipsActivity.SHOW_TIP_ACTION, null, FBReader.this, TipsActivity.class
					));
				break;
			case Download:
				manager.startDownloading();
				break;
			case None:
				break;
		}
	}
	}

	@Override
	public void onResume() {
		super.onResume();
		try {
			sendBroadcast(new Intent(getApplicationContext(), KillerCallback.class));
		} catch (Throwable t) {
		}
		PopupPanel.restoreVisibilities(FBReaderApp.Instance());

		hideBars();

		ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_OPENED);
	}

	@Override
	public void onStop() {
		ApiServerImplementation.sendEvent(this, ApiListener.EVENT_READ_MODE_CLOSED);
		PopupPanel.removeAllWindows(FBReaderApp.Instance(), this);
		super.onStop();
	}

	@Override
	protected FBReaderApp createApplication() {
		if (SQLiteBooksDatabase.Instance() == null) {
			new SQLiteBooksDatabase(this, "READER");
		}
		return new FBReaderApp();
	}

	@Override
	public boolean onSearchRequested() {
		final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
		final FBReaderApp.PopupPanel popup = fbreader.getActivePopup();
		fbreader.hideActivePopup();
		final SearchManager manager = (SearchManager)getSystemService(SEARCH_SERVICE);
		manager.setOnCancelListener(new SearchManager.OnCancelListener() {
			public void onCancel() {
				if (popup != null) {
					fbreader.showPopup(popup.getId());
				}
				manager.setOnCancelListener(null);
			}
		});
		startSearch(fbreader.TextSearchPatternOption.getValue(), true, null, false);
		return true;
	}

	public void showSelectionPanel() {
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		final ZLTextView view = fbReader.getTextView();
		((SelectionPopup)fbReader.getPopupById(SelectionPopup.ID))
			.move(view.getSelectionStartY(), view.getSelectionEndY());
		fbReader.showPopup(SelectionPopup.ID);
		hideBars();
	}

	public void hideSelectionPanel() {
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		final FBReaderApp.PopupPanel popup = fbReader.getActivePopup();
		if (popup != null && popup.getId() == SelectionPopup.ID) {
			FBReaderApp.Instance().hideActivePopup();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
		switch (requestCode) {
			case REPAINT_CODE:
			{
				final BookModel model = fbreader.Model;
				if (model != null) {
					final Book book = model.Book;
					if (book != null) {
						book.reloadInfoFromDatabase();
						ZLTextHyphenator.Instance().load(book.getLanguage());
					}
				}
				fbreader.clearTextCaches();
				fbreader.getViewWidget().repaint();
				break;
			}
			case CANCEL_CODE:
				fbreader.runCancelAction(resultCode - 1);
				break;
		}
	}

	private Menu addSubMenu(Menu menu, String id) {
		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		return application.myMainWindow.addSubMenu(menu, id);
	}

	private void addMenuItem(Menu menu, String actionId, String name) {
		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		application.myMainWindow.addMenuItem(menu, actionId, null, name);
	}

	private void addMenuItem(Menu menu, String actionId, int iconId) {
		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		application.myMainWindow.addMenuItem(menu, actionId, iconId, null);
	}

	private void addMenuItem(Menu menu, String actionId) {
		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		application.myMainWindow.addMenuItem(menu, actionId, null, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		addMenuItem(menu, ActionCode.SHOW_LIBRARY_SDCARD, R.drawable.ic_list_library_search);//fbreader);//ic_list_library_folder);//maryhit
		//NOT WORKING		addMenuItem(menu, ActionCode.SHOW_LIBRARY_OPEN_BOOKS, R.drawable.fbreader);//ic_list_library_folder);//maryhit
		addMenuItem(menu, ActionCode.SWITCH_TO_NIGHT_PROFILE, R.drawable.ic_menu_night);
		addMenuItem(menu, ActionCode.SWITCH_TO_DAY_PROFILE, R.drawable.ic_menu_day);
		addMenuItem(menu, ActionCode.INCREASE_FONT);
		addMenuItem(menu, ActionCode.DECREASE_FONT);
		addMenuItem(menu, ActionCode.SHOW_TOC, R.drawable.ic_menu_toc);
		addMenuItem(menu, ActionCode.SHOW_BOOKMARKS, R.drawable.ic_menu_bookmarks);
		addMenuItem(menu, ActionCode.SEARCH, R.drawable.ic_menu_search);
		addMenuItem(menu, ActionCode.SHOW_LIBRARY, R.drawable.ic_menu_library);
		addMenuItem(menu, ActionCode.SHOW_NETWORK_LIBRARY, R.drawable.ic_menu_networklibrary);
		addMenuItem(menu, ActionCode.SHOW_PREFERENCES);
		addMenuItem(menu, ActionCode.SHOW_BOOK_INFO);
		final Menu subMenu = addSubMenu(menu, "screenOrientation");
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_SYSTEM);
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_SENSOR);
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT);
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE);
		if (ZLibrary.Instance().supportsAllOrientations()) {
			addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT);
			addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		}
		synchronized (myPluginActions) {
			int index = 0;
			for (PluginApi.ActionInfo info : myPluginActions) {
				if (info instanceof PluginApi.MenuActionInfo) {
					addMenuItem(
						menu,
						PLUGIN_ACTION_PREFIX + index++,
						((PluginApi.MenuActionInfo)info).MenuItemName
					);
				}
			}
		}

		final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
		application.myMainWindow.refresh();

		return true;
	}

	private NavigationPopup myNavigationPopup;

	boolean barsAreShown() {
		return myNavigationPopup != null;
	}

	void hideBars() {
		if (myNavigationPopup != null) {
			myNavigationPopup.stopNavigation();
			myNavigationPopup = null;
		}

		final ZLAndroidLibrary zlibrary = (ZLAndroidLibrary)ZLibrary.Instance();
		if (!zlibrary.ShowActionBarOption.getValue()) {
			getActionBar().hide();
		}

		if (zlibrary.DisableButtonLightsOption.getValue()) {
			findViewById(R.id.root_view).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		}
	}

	void showBars() {
		getActionBar().show();

		final RelativeLayout root = (RelativeLayout)findViewById(R.id.root_view);
		root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

		if (myNavigationPopup == null) {
			final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
			fbreader.hideActivePopup();
			myNavigationPopup = new NavigationPopup(fbreader);
			myNavigationPopup.runNavigation(this, root);
		}
	}

	@Override
	public void refresh() {
		if (myNavigationPopup != null) {
			myNavigationPopup.update();
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		super.setTitle(title);
		final TextView view = (TextView)getActionBar().getCustomView();
		if (view != null) {
			view.setText(title);
			view.postInvalidate();
		}
	}
}
