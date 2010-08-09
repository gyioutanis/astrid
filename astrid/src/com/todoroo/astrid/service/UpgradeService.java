package com.todoroo.astrid.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.webkit.WebView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.activity.TaskListActivity;


public final class UpgradeService {

    @Autowired
    private DialogUtilities dialogUtilities;

    public UpgradeService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Perform upgrade from one version to the next. Needs to be called
     * on the UI thread so it can display a progress bar and then
     * show users a change log.
     *
     * @param from
     * @param to
     */
    public void performUpgrade(final Context context, final int from) {
        if(from == 135)
            AddOnService.recordOem();

        // pop up a progress dialog
        final ProgressDialog dialog;
        if(context instanceof Activity)
            dialog = dialogUtilities.progressDialog(context,
                    context.getString(R.string.DLG_upgrading));
        else
            dialog = null;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(from < 136)
                        new Astrid2To3UpgradeHelper().upgrade2To3(context, from);

                    if(from < 146)
                        new Astrid2To3UpgradeHelper().upgrade3To3_1(context, from);

                } finally {
                    if(context instanceof Activity) {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            public void run() {
                                if(dialog != null)
                                    dialog.dismiss();

                                // display changelog
                                showChangeLog(context, from);
                                if(context instanceof TaskListActivity)
                                    ((TaskListActivity)context).loadTaskListContent(true);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    /**
     * Return a change log string. Releases occur often enough that we don't
     * expect change sets to be localized.
     *
     * @param from
     * @param to
     * @return
     */
    @SuppressWarnings("nls")
    public void showChangeLog(Context context, int from) {
        if(!(context instanceof Activity) || from == 0)
            return;

        StringBuilder changeLog = new StringBuilder();

        if(from <= 135)
            newVersionString(changeLog, "3.1.0 (8/9/10)", new String[] {
                    "Astrid is brand new inside and out! In addition to a new " +
                    "look and feel, a new add-on system allows Astrid to become " +
                    "more powerful, while other improvements have made it faster " +
                    "and easier to use. Hope you like it!",
                    "This update contains for free all of Astrid " +
                    "Power Pack's features for evaluation purposes",
                    "If you liked the old version, you can also go back by " +
                    "<a href='http://bit.ly/oldastrid'>clicking here</a>",
            });
        if(from > 135 && from <= 145)
            newVersionString(changeLog, "3.1.0 (8/9/10)", new String[] {
                    "Linkify phone numbers, e-mails, and web pages",
                    "Swipe L => R to go from tasks to filters",
                    "Moved task priority bar to left side",
                    "Added ability to create fixed alerts for a task",
                    "Restored tag hiding when tag begins with underscore (_)",
                    "FROYO: disabled moving app to SD card, it would break alarms and widget",
                    "Also gone: a couple force closes, bugs with repeating tasks",
                    "... enjoy! - we ♥ astrid team",
            });
        if(from > 135 && from <= 144)
            newVersionString(changeLog, "3.0.6 (8/4/10)", new String[] {
                    "This update contains for free all of the " +
                        "powerpack's features for evaluation purposes",
                    "Fixed widget not updating when tasks are edited",
                    "Added a setting for displaying task notes in the list",
            });

        if(changeLog.length() == 0)
            return;

        changeLog.append("</body></html>");
        String changeLogHtml = "<html><body style='color: white'>" + changeLog;

        WebView webView = new WebView(context);
        webView.loadData(changeLogHtml, "text/html", "utf-8");
        webView.setBackgroundColor(0);

        new AlertDialog.Builder(context)
        .setTitle(R.string.UpS_changelog_title)
        .setView(webView)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(android.R.string.ok, null)
        .show();
    }

    /**
     * Helper for adding a single version to the changelog
     * @param changeLog
     * @param version
     * @param changes
     */
    @SuppressWarnings("nls")
    private void newVersionString(StringBuilder changeLog, String version, String[] changes) {
        changeLog.append("<font style='text-align: center; color=#ffaa00'><b>Version ").append(version).append(":</b></font><br><ul>");
        for(String change : changes)
            changeLog.append("<li>").append(change).append("</li>\n");
        changeLog.append("</ul>");
    }

    // --- database upgrade logic

}
