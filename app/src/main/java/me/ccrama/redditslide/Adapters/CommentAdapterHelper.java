package me.ccrama.redditslide.Adapters;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.cocosw.bottomsheet.BottomSheet;

import net.dean.jraw.ApiException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.managers.ModerationManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.DistinguishedStatus;
import net.dean.jraw.models.Submission;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.ccrama.redditslide.ActionStates;
import me.ccrama.redditslide.Activities.Profile;
import me.ccrama.redditslide.Activities.Website;
import me.ccrama.redditslide.Authentication;
import me.ccrama.redditslide.OpenRedditLink;
import me.ccrama.redditslide.R;
import me.ccrama.redditslide.Reddit;
import me.ccrama.redditslide.SpoilerRobotoTextView;
import me.ccrama.redditslide.TimeUtils;
import me.ccrama.redditslide.UserSubscriptions;
import me.ccrama.redditslide.UserTags;
import me.ccrama.redditslide.Views.CommentOverflow;
import me.ccrama.redditslide.Views.DoEditorActions;
import me.ccrama.redditslide.Views.RoundedBackgroundSpan;
import me.ccrama.redditslide.Visuals.Palette;

/**
 * Created by Carlos on 8/4/2016.
 */
public class CommentAdapterHelper {
    public static String reportReason;

    public static void showOverflowBottomSheet(final CommentAdapter adapter, final Context mContext, final CommentViewHolder holder, final CommentNode baseNode) {

        int[] attrs = new int[]{R.attr.tint};
        final Comment n = baseNode.getComment();
        TypedArray ta = mContext.obtainStyledAttributes(attrs);

        int color = ta.getColor(0, Color.WHITE);
        Drawable profile = mContext.getResources().getDrawable(R.drawable.profile);
        Drawable saved = mContext.getResources().getDrawable(R.drawable.iconstarfilled);
        Drawable gild = mContext.getResources().getDrawable(R.drawable.gild);
        Drawable copy = mContext.getResources().getDrawable(R.drawable.ic_content_copy);
        Drawable share = mContext.getResources().getDrawable(R.drawable.share);
        Drawable parent = mContext.getResources().getDrawable(R.drawable.commentchange);
        Drawable permalink = mContext.getResources().getDrawable(R.drawable.link);
        Drawable report = mContext.getResources().getDrawable(R.drawable.report);

        profile.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        saved.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        gild.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        report.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        copy.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        share.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        parent.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        permalink.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        ta.recycle();

        BottomSheet.Builder b = new BottomSheet.Builder((Activity) mContext)
                .title(Html.fromHtml(n.getBody()));

        if (Authentication.didOnline) {
            b.sheet(1, profile, "/u/" + n.getAuthor());
            String save = mContext.getString(R.string.btn_save);
            if (ActionStates.isSaved(n)) {
                save = mContext.getString(R.string.comment_unsave);
            }
            if (Authentication.isLoggedIn) {
                b.sheet(3, saved, save);
                b.sheet(16, report, mContext.getString(R.string.btn_report));

            }
        }
        b.sheet(5, gild, mContext.getString(R.string.comment_gild))
                .sheet(7, copy, mContext.getString(R.string.misc_copy_text))
                .sheet(23, permalink, mContext.getString(R.string.comment_permalink))
                .sheet(4, share, mContext.getString(R.string.comment_share));
        if (!adapter.currentBaseNode.isTopLevel()) {
            b.sheet(10, parent, mContext.getString(R.string.comment_parent));
        }
        b.listener(new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           switch (which) {
                               case 1: {
                                   //Go to author
                                   Intent i = new Intent(mContext, Profile.class);
                                   i.putExtra(Profile.EXTRA_PROFILE, n.getAuthor());
                                   mContext.startActivity(i);
                               }
                               break;
                               case 3:
                                   //Save comment
                                   saveComment(n, mContext, holder);
                                   break;
                               case 23: {
                                   //Go to comment permalink
                                   String s = "https://reddit.com" + adapter.submission.getPermalink() +
                                           n.getFullName().substring(3, n.getFullName().length()) + "?context=3";
                                   new OpenRedditLink(mContext, s);
                               }
                               break;
                               case 5: {
                                   //Gild comment
                                   Intent i = new Intent(mContext, Website.class);
                                   i.putExtra(Website.EXTRA_URL, "https://reddit.com" + adapter.submission.getPermalink() +
                                           n.getFullName().substring(3, n.getFullName().length()) + "?context=3");
                                   i.putExtra(Website.EXTRA_COLOR, Palette.getColor(n.getSubredditName()));
                                   mContext.startActivity(i);
                               }
                               break;
                               case 16:
                                   //report
                                   reportReason = "";
                                   new MaterialDialog.Builder(mContext).input(mContext.getString(R.string.input_reason_for_report), null, true, new MaterialDialog.InputCallback() {
                                       @Override
                                       public void onInput(MaterialDialog dialog, CharSequence input) {
                                           reportReason = input.toString();
                                       }
                                   }).alwaysCallInputCallback()
                                           .positiveText(R.string.btn_report)
                                           .negativeText(R.string.btn_cancel)
                                           .onNegative(null)
                                           .onPositive(new MaterialDialog.SingleButtonCallback() {
                                               @Override
                                               public void onClick(MaterialDialog dialog, DialogAction which) {
                                                   new AsyncReportTask(adapter.currentBaseNode, adapter.listView).execute();
                                               }
                                           })
                                           .show();
                                   break;
                               case 10:
                                   //View comment parent
                                   viewCommentParent(adapter, holder, mContext, baseNode);
                                   break;
                               case 7:
                                   //Copy text to clipboard
                                   ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                   ClipData clip = ClipData.newPlainText("Comment text", n.getBody());
                                   clipboard.setPrimaryClip(clip);

                                   Toast.makeText(mContext, "Comment text copied", Toast.LENGTH_SHORT).show();
                                   break;
                               case 4:
                                   //Share comment
                                   Reddit.defaultShareText(adapter.submission.getTitle(), "https://reddit.com" + adapter.submission.getPermalink() +
                                           n.getFullName().substring(3, n.getFullName().length()) + "?context=3", mContext);
                                   break;
                           }
                       }
                   }
        );
        b.show();
    }

    private static void viewCommentParent(CommentAdapter adapter, CommentViewHolder holder, Context mContext, CommentNode baseNode) {
        int old = holder.getAdapterPosition();
        int pos = (old < 2) ? 0 : old - 1;
        for (int i = pos - 1; i >= 0; i--) {
            CommentObject o = adapter.currentComments.get(adapter.getRealPosition(i));
            if (o instanceof CommentItem && pos - 1 != i && o.comment.getDepth() < baseNode.getDepth()) {
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                final View dialoglayout = inflater.inflate(R.layout.parent_comment_dialog, null);
                final AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(mContext);
                Comment parent = o.comment.getComment();
                adapter.setViews(parent.getDataNode().get("body_html").asText(), adapter.submission.getSubredditName(), (SpoilerRobotoTextView) dialoglayout.findViewById(R.id.firstTextView), (CommentOverflow) dialoglayout.findViewById(R.id.commentOverflow));
                builder.setView(dialoglayout);
                builder.show();
                break;
            }
        }
    }

    private static void saveComment(final Comment comment, final Context mContext, final CommentViewHolder holder) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (ActionStates.isSaved(comment)) {
                        new AccountManager(Authentication.reddit).unsave(comment);
                        ActionStates.setSaved(comment, false);
                    } else {
                        new AccountManager(Authentication.reddit).save(comment);
                        ActionStates.setSaved(comment, true);
                    }

                } catch (ApiException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Snackbar s;
                if (ActionStates.isSaved(comment)) {
                    s = Snackbar.make(holder.itemView, "Comment saved", Snackbar.LENGTH_LONG);
                    if (Authentication.me.hasGold()) {
                        s.setAction("CATEGORIZE", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                categorizeComment(comment, mContext);
                            }
                        });
                    }
                } else {
                    s = Snackbar.make(holder.itemView, "Comment un-saved", Snackbar.LENGTH_SHORT);
                }
                View view = s.getView();
                TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                s.show();
            }
        }.execute();
    }

    private static void categorizeComment(final Comment comment, final Context mContext) {
        new AsyncTask<Void, Void, List<String>>() {

            Dialog d;

            @Override
            public void onPreExecute() {
                d = new MaterialDialog.Builder(mContext).progress(true, 100).title("Loading categories").show();
            }

            @Override
            protected List<String> doInBackground(Void... params) {
                try {
                    List<String> categories = new ArrayList<String>(new AccountManager(Authentication.reddit).getSavedCategories());
                    categories.add("New category");
                    return categories;
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ArrayList<String>() {{
                        add("New category");
                    }};
                }
            }

            @Override
            public void onPostExecute(final List<String> data) {
                try {
                    new MaterialDialog.Builder(mContext).items(data)
                            .title("Select flair")
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, final View itemView, int which, CharSequence text) {
                                    final String t = data.get(which);
                                    if (which == data.size() - 1) {
                                        new MaterialDialog.Builder(mContext).title("Set category name")
                                                .input("Category name", null, false, new MaterialDialog.InputCallback() {
                                                    @Override
                                                    public void onInput(MaterialDialog dialog, CharSequence input) {

                                                    }
                                                }).positiveText("Set")
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(MaterialDialog dialog, DialogAction which) {
                                                        final String flair = dialog.getInputEditText().getText().toString();
                                                        new AsyncTask<Void, Void, Boolean>() {
                                                            @Override
                                                            protected Boolean doInBackground(Void... params) {
                                                                try {
                                                                    new AccountManager(Authentication.reddit).save(comment, flair);
                                                                    return true;
                                                                } catch (ApiException e) {
                                                                    e.printStackTrace();
                                                                    return false;
                                                                }
                                                            }

                                                            @Override
                                                            protected void onPostExecute(Boolean done) {
                                                                Snackbar s;
                                                                if (done) {
                                                                    if (itemView != null) {
                                                                        s = Snackbar.make(itemView, R.string.submission_info_saved, Snackbar.LENGTH_SHORT);
                                                                        View view = s.getView();
                                                                        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                                                                        tv.setTextColor(Color.WHITE);
                                                                        s.show();
                                                                    }
                                                                } else {
                                                                    if (itemView != null) {
                                                                        s = Snackbar.make(itemView, "Error setting category", Snackbar.LENGTH_SHORT);
                                                                        View view = s.getView();
                                                                        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                                                                        tv.setTextColor(Color.WHITE);
                                                                        s.show();
                                                                    }
                                                                }

                                                            }
                                                        }.execute();
                                                    }
                                                }).negativeText(R.string.btn_cancel)
                                                .show();
                                    } else {
                                        new AsyncTask<Void, Void, Boolean>() {
                                            @Override
                                            protected Boolean doInBackground(Void... params) {
                                                try {
                                                    new AccountManager(Authentication.reddit).save(comment, t);
                                                    return true;
                                                } catch (ApiException e) {
                                                    e.printStackTrace();
                                                    return false;
                                                }
                                            }

                                            @Override
                                            protected void onPostExecute(Boolean done) {
                                                Snackbar s;
                                                if (done) {
                                                    if (itemView != null) {
                                                        s = Snackbar.make(itemView, R.string.submission_info_saved, Snackbar.LENGTH_SHORT);
                                                        View view = s.getView();
                                                        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                                                        tv.setTextColor(Color.WHITE);
                                                        s.show();
                                                    }
                                                } else {
                                                    if (itemView != null) {
                                                        s = Snackbar.make(itemView, "Error setting category", Snackbar.LENGTH_SHORT);
                                                        View view = s.getView();
                                                        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                                                        tv.setTextColor(Color.WHITE);
                                                        s.show();
                                                    }
                                                }
                                            }
                                        }.execute();
                                    }
                                }
                            }).show();
                    if (d != null) {
                        d.dismiss();
                    }
                } catch (Exception ignored) {

                }
            }
        }.execute();
    }

    public static void showModBottomSheet(final CommentAdapter adapter, final Context mContext, final CommentNode baseNode, final Comment comment, final CommentViewHolder holder, final Map<String, Integer> reports, final Map<String, String> reports2) {

        int[] attrs = new int[]{R.attr.tint};
        TypedArray ta = mContext.obtainStyledAttributes(attrs);

        //Initialize drawables
        int color = ta.getColor(0, Color.WHITE);
        Drawable profile = mContext.getResources().getDrawable(R.drawable.profile);
        final Drawable report = mContext.getResources().getDrawable(R.drawable.report);
        final Drawable approve = mContext.getResources().getDrawable(R.drawable.support);
        final Drawable nsfw = mContext.getResources().getDrawable(R.drawable.hide);
        final Drawable pin = mContext.getResources().getDrawable(R.drawable.lock);
        final Drawable distinguish = mContext.getResources().getDrawable(R.drawable.iconstarfilled);
        final Drawable remove = mContext.getResources().getDrawable(R.drawable.close);

        //Tint drawables
        profile.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        report.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        approve.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        nsfw.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        distinguish.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        remove.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        pin.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        ta.recycle();

        //Bottom sheet builder
        BottomSheet.Builder b = new BottomSheet.Builder((Activity) mContext)
                .title(Html.fromHtml(comment.getBody()));

        int reportCount = reports.size() + reports2.size();

        if (reportCount == 0) {
            b.sheet(0, report, "No reports");
        } else {
            b.sheet(0, report, "View " + reportCount + " reports");
        }

        boolean approved = false;
        String whoApproved = "";
        if (comment.getDataNode().get("approved_by").asText().equals("null")) {
            b.sheet(1, approve, "Approve comment");
        } else {
            approved = true;
            whoApproved = comment.getDataNode().get("approved_by").asText();
            b.sheet(1, approve, "Approved by /u/" + whoApproved);
        }

        // b.sheet(2, spam, mContext.getString(R.string.mod_btn_spam)) todo this


        final boolean stickied = comment.getDataNode().has("stickied") && comment.getDataNode().get("stickied").asBoolean();
        if (baseNode.isTopLevel())
            if (!stickied) {
                b.sheet(4, pin, "Sticky comment");
            } else {
                b.sheet(4, pin, "Un-sticky comment");
            }

        final boolean distinguished = !comment.getDataNode().get("distinguished").isNull();
        if (comment.getAuthor().equalsIgnoreCase(Authentication.name)) {
            if (!distinguished) {
                b.sheet(9, distinguish, "Distinguish comment");
            } else {
                b.sheet(9, distinguish, "Un-distinguish comment");
            }
        }


        final String finalWhoApproved = whoApproved;
        final boolean finalApproved = approved;
        b.sheet(6, remove, mContext.getString(R.string.btn_remove))
                .sheet(8, profile, "Author profile")
                .listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                viewReports(mContext, reports, reports2);
                                break;
                            case 1:
                                doApproval(finalApproved, mContext, finalWhoApproved, holder, comment);
                                break;
                            case 4:
                                if (stickied) {
                                    unStickyComment(mContext, holder, comment);
                                } else {
                                    stickyComment(mContext, holder, comment);
                                }
                                break;
                            case 9:
                                if (distinguished) {
                                    unDistinguishComment(mContext, holder, comment);
                                } else {
                                    distinguishComment(mContext, holder, comment);
                                }
                                break;
                            case 6:
                                removeComment(mContext, holder, comment, adapter);
                                break;
                            case 8:
                                Intent i = new Intent(mContext, Profile.class);
                                i.putExtra(Profile.EXTRA_PROFILE, comment.getAuthor());
                                mContext.startActivity(i);
                                break;

                        }
                    }
                });
        b.show();
    }

    private static void distinguishComment(final Context mContext, final CommentViewHolder holder, final Comment comment) {
        new AlertDialogWrapper.Builder(mContext).setTitle(R.string.distinguish_comment)
                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {

                        new AsyncTask<Void, Void, Boolean>() {

                            @Override
                            public void onPostExecute(Boolean b) {
                                if (b) {
                                    dialog.dismiss();
                                    Snackbar s = Snackbar.make(holder.itemView, R.string.comment_distinguished, Snackbar.LENGTH_LONG);
                                    View view = s.getView();
                                    TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                                    tv.setTextColor(Color.WHITE);
                                    s.show();


                                } else {
                                    new AlertDialogWrapper.Builder(mContext)
                                            .setTitle(R.string.err_general)
                                            .setMessage(R.string.err_retry_later).show();
                                }
                            }

                            @Override
                            protected Boolean doInBackground(Void... params) {
                                try {
                                    new ModerationManager(Authentication.reddit).setDistinguishedStatus(comment, DistinguishedStatus.MODERATOR);
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                    return false;

                                }
                                return true;
                            }
                        }.execute();

                    }
                }).setNegativeButton(R.string.btn_no, null).show();
    }

    private static void unDistinguishComment(final Context mContext, final CommentViewHolder holder, final Comment comment) {
        new AlertDialogWrapper.Builder(mContext).setTitle(R.string.undistinguish_comment)
                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {

                        new AsyncTask<Void, Void, Boolean>() {

                            @Override
                            public void onPostExecute(Boolean b) {
                                if (b) {
                                    dialog.dismiss();

                                    Snackbar s = Snackbar.make(holder.itemView, R.string.comment_undistinguished, Snackbar.LENGTH_LONG);
                                    View view = s.getView();
                                    TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                                    tv.setTextColor(Color.WHITE);
                                    s.show();

                                } else {
                                    new AlertDialogWrapper.Builder(mContext)
                                            .setTitle(R.string.err_general)
                                            .setMessage(R.string.err_retry_later).show();
                                }
                            }

                            @Override
                            protected Boolean doInBackground(Void... params) {
                                try {
                                    new ModerationManager(Authentication.reddit).setDistinguishedStatus(comment, DistinguishedStatus.NORMAL);
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                    return false;

                                }
                                return true;
                            }
                        }.execute();

                    }
                }).setNegativeButton(R.string.btn_no, null).show();
    }

    private static void stickyComment(final Context mContext, final CommentViewHolder holder, final Comment comment) {
        new AlertDialogWrapper.Builder(mContext).setTitle(R.string.sticky_comment)
                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {

                        new AsyncTask<Void, Void, Boolean>() {

                            @Override
                            public void onPostExecute(Boolean b) {
                                if (b) {
                                    dialog.dismiss();
                                    Snackbar s = Snackbar.make(holder.itemView, R.string.comment_stickied, Snackbar.LENGTH_LONG);
                                    View view = s.getView();
                                    TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                                    tv.setTextColor(Color.WHITE);
                                    s.show();


                                } else {
                                    new AlertDialogWrapper.Builder(mContext)
                                            .setTitle(R.string.err_general)
                                            .setMessage(R.string.err_retry_later).show();
                                }
                            }

                            @Override
                            protected Boolean doInBackground(Void... params) {
                                try {
                                    new ModerationManager(Authentication.reddit).setSticky(comment, true);
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                    return false;

                                }
                                return true;
                            }
                        }.execute();

                    }
                }).setNegativeButton(R.string.btn_no, null).show();
    }

    public static void viewReports(final Context mContext, final Map<String, Integer> reports, final Map<String, String> reports2) {
        new AsyncTask<Void, Void, ArrayList<String>>() {
            @Override
            protected ArrayList<String> doInBackground(Void... params) {

                ArrayList<String> finalReports = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : reports.entrySet()) {
                    finalReports.add("x" + entry.getValue() + " " + entry.getKey());
                }
                for (Map.Entry<String, String> entry : reports2.entrySet()) {
                    finalReports.add(entry.getKey() + ": " + entry.getValue());
                }
                if (finalReports.isEmpty()) {
                    finalReports.add(mContext.getString(R.string.mod_no_reports));
                }
                return finalReports;
            }

            @Override
            public void onPostExecute(ArrayList<String> data) {
                new AlertDialogWrapper.Builder(mContext).setTitle(R.string.mod_reports).setItems(data.toArray(new CharSequence[data.size()]),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        }.execute();
    }

    public static void doApproval(boolean approved, final Context mContext, String whoApproved, final CommentViewHolder holder, final Comment comment) {
        if (approved) {
            Intent i = new Intent(mContext, Profile.class);
            i.putExtra(Profile.EXTRA_PROFILE, whoApproved);
            mContext.startActivity(i);
        } else {
            new AlertDialogWrapper.Builder(mContext).setTitle(R.string.mod_approve)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {

                            new AsyncTask<Void, Void, Boolean>() {

                                @Override
                                public void onPostExecute(Boolean b) {
                                    if (b) {
                                        dialog.dismiss();
                                        Snackbar.make(holder.itemView, R.string.mod_approved, Snackbar.LENGTH_LONG).show();

                                    } else {
                                        new AlertDialogWrapper.Builder(mContext)
                                                .setTitle(R.string.err_general)
                                                .setMessage(R.string.err_retry_later).show();
                                    }
                                }

                                @Override
                                protected Boolean doInBackground(Void... params) {
                                    try {
                                        new ModerationManager(Authentication.reddit).approve(comment);
                                    } catch (ApiException e) {
                                        e.printStackTrace();
                                        return false;

                                    }
                                    return true;
                                }
                            }.execute();

                        }
                    }).setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).show();
        }
    }

    public static void unStickyComment(final Context mContext, final CommentViewHolder holder, final Comment comment) {
        new AlertDialogWrapper.Builder(mContext).setTitle(R.string.unsticky_comment)
                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {

                        new AsyncTask<Void, Void, Boolean>() {

                            @Override
                            public void onPostExecute(Boolean b) {

                                if (b) {
                                    dialog.dismiss();

                                    Snackbar s = Snackbar.make(holder.itemView, R.string.comment_unstickied, Snackbar.LENGTH_LONG);
                                    View view = s.getView();
                                    TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                                    tv.setTextColor(Color.WHITE);
                                    s.show();


                                } else {
                                    new AlertDialogWrapper.Builder(mContext)
                                            .setTitle(R.string.err_general)
                                            .setMessage(R.string.err_retry_later).show();
                                }
                            }

                            @Override
                            protected Boolean doInBackground(Void... params) {
                                try {
                                    new ModerationManager(Authentication.reddit).setSticky(comment, false);
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                    return false;

                                }
                                return true;
                            }
                        }.execute();

                    }
                }).setNegativeButton(R.string.btn_no, null).show();
    }

    public static void removeComment(final Context mContext, final CommentViewHolder holder, final Comment comment, final CommentAdapter adapter) {
        new AlertDialogWrapper.Builder(mContext).setTitle(R.string.remove_comment)
                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {

                        new AsyncTask<Void, Void, Boolean>() {

                            @Override
                            public void onPostExecute(Boolean b) {
                                if (b) {
                                    dialog.dismiss();
                                    Snackbar s = Snackbar.make(holder.itemView, R.string.comment_removed, Snackbar.LENGTH_LONG);
                                    View view = s.getView();
                                    TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                                    tv.setTextColor(Color.WHITE);
                                    s.show();

                                    adapter.deleted.add(comment.getFullName());
                                    holder.firstTextView.setTextHtml(mContext.getString(R.string.content_removed));
                                    holder.content.setText(R.string.content_removed);
                                } else {
                                    new AlertDialogWrapper.Builder(mContext)
                                            .setTitle(R.string.err_general)
                                            .setMessage(R.string.err_retry_later).show();
                                }
                            }

                            @Override
                            protected Boolean doInBackground(Void... params) {
                                try {
                                    new ModerationManager(Authentication.reddit).remove(comment, false);
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                    return false;

                                }
                                return true;
                            }
                        }.execute();

                    }
                }).setNegativeButton(R.string.btn_no, null).show();
    }

    public static Spannable getScoreString(Comment comment, int offset, Context mContext, CommentViewHolder holder, Submission submission, boolean up, boolean down) {
        final String spacer = " " + mContext.getString(R.string.submission_properties_seperator_comments) + " ";
        SpannableStringBuilder titleString = new SpannableStringBuilder();
        SpannableStringBuilder author = new SpannableStringBuilder(comment.getAuthor());
        final int authorcolor = Palette.getFontColorUser(comment.getAuthor());

        author.setSpan(new TypefaceSpan("sans-serif-condensed"), 0, author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        author.setSpan(new StyleSpan(Typeface.BOLD), 0, author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (comment.getDistinguishedStatus() == DistinguishedStatus.MODERATOR || comment.getDistinguishedStatus() == DistinguishedStatus.ADMIN) {
            author.replace(0, author.length(), " " + comment.getAuthor() + " ");
            author.setSpan(new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_green_300, false), 0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (Authentication.name != null && comment.getAuthor().toLowerCase().equals(Authentication.name.toLowerCase())) {
            author.replace(0, author.length(), " " + comment.getAuthor() + " ");
            author.setSpan(new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_deep_orange_300, false), 0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (submission != null && comment.getAuthor().toLowerCase().equals(submission.getAuthor().toLowerCase())) {
            author.replace(0, author.length(), " " + comment.getAuthor() + " ");
            author.setSpan(new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_blue_300, false), 0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (authorcolor != 0) {
            author.setSpan(new ForegroundColorSpan(authorcolor), 0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        titleString.append(author);
        titleString.append(spacer);

        int scoreColor;
        switch (ActionStates.getVoteDirection(comment)) {
            case UPVOTE:
                scoreColor = (holder.textColorUp);
                break;
            case DOWNVOTE:
                scoreColor = (holder.textColorDown);
                break;
            case NO_VOTE:
                scoreColor = (holder.textColorRegular);
                break;
        }

        //Whether or not this comment was made by this user
        final boolean ownComment = comment.getAuthor().equals(Authentication.name);

        if (up) {
            scoreColor = (holder.textColorUp);

            //User upvoted their own comment--don't mess with what the API returns
            if (ownComment) {
                offset = 0;
            }
        } else if (down) {
            scoreColor = (holder.textColorDown);

            //User downvoted their own comment--offset it by an additional -1
            if (ownComment) {
                --offset;
            }
        } else {
            scoreColor = (holder.textColorRegular);

            //User un-voted their own comment--offset it by an additional -1
            if (ownComment) {
                --offset;
            }
        }

        String scoreText;
        if (comment.isScoreHidden()) {
            scoreText = "[" + mContext.getString(R.string.misc_score_hidden).toUpperCase() + "]";
        } else {
            scoreText = String.format(Locale.getDefault(), "%d", comment.getScore() + offset);
        }

        SpannableStringBuilder score = new SpannableStringBuilder(scoreText);

        if (score == null || score.toString().isEmpty()) {
            score = new SpannableStringBuilder("0");
        }
        if (!scoreText.contains("[")) {
            score.append(String.format(Locale.getDefault(), " %s", mContext.getResources().getQuantityString(R.plurals.points, comment.getScore() + offset)));
        }
        score.setSpan(new ForegroundColorSpan(scoreColor), 0, score.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        titleString.append(score);
        titleString.append((comment.isControversial() ? " †" : ""));

        titleString.append(spacer);
        String timeAgo = TimeUtils.getTimeAgo(comment.getCreated().getTime(), mContext);
        titleString.append((timeAgo == null || timeAgo.isEmpty()) ? "just now" : timeAgo);

        titleString.append(((comment.getEditDate() != null) ? " (edit " + TimeUtils.getTimeAgo(comment.getEditDate().getTime(), mContext) + ")" : ""));
        titleString.append("  ");

        if (comment.getDataNode().get("stickied").asBoolean()) {
            SpannableStringBuilder pinned = new SpannableStringBuilder("\u00A0" + mContext.getString(R.string.submission_stickied).toUpperCase() + "\u00A0");
            pinned.setSpan(new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_green_300, false), 0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        }
        if (comment.getTimesGilded() > 0) {
            //if the comment has only been gilded once, don't show a number
            final String timesGilded = (comment.getTimesGilded() == 1) ? "" : "\u200A" + Integer.toString(comment.getTimesGilded());
            SpannableStringBuilder pinned = new SpannableStringBuilder("\u00A0★" + timesGilded + "\u00A0");
            pinned.setSpan(new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_orange_500, false), 0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        }
        if (UserTags.isUserTagged(comment.getAuthor())) {
            SpannableStringBuilder pinned = new SpannableStringBuilder("\u00A0" + UserTags.getUserTag(comment.getAuthor()) + "\u00A0");
            pinned.setSpan(new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_blue_500, false), 0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        }
        if (UserSubscriptions.friends.contains(comment.getAuthor())) {
            SpannableStringBuilder pinned = new SpannableStringBuilder("\u00A0" + mContext.getString(R.string.profile_friend) + "\u00A0");
            pinned.setSpan(new RoundedBackgroundSpan(mContext, R.color.white, R.color.md_deep_orange_500, false), 0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        }
        if (comment.getAuthorFlair() != null && comment.getAuthorFlair().getText() != null && !comment.getAuthorFlair().getText().isEmpty()) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = mContext.getTheme();
            theme.resolveAttribute(R.attr.activity_background, typedValue, true);
            int color = typedValue.data;
            SpannableStringBuilder pinned = new SpannableStringBuilder("\u00A0" + Html.fromHtml(comment.getAuthorFlair().getText()) + "\u00A0");
            pinned.setSpan(new RoundedBackgroundSpan(holder.firstTextView.getCurrentTextColor(), color, false, mContext), 0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        } else if (comment.getAuthorFlair() != null) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = mContext.getTheme();
            theme.resolveAttribute(R.attr.activity_background, typedValue, true);
            int color = typedValue.data;
            SpannableStringBuilder pinned = new SpannableStringBuilder("\u00A0" + Html.fromHtml(comment.getAuthorFlair().getCssClass()) + "\u00A0");
            pinned.setSpan(new RoundedBackgroundSpan(holder.firstTextView.getCurrentTextColor(), color, false, mContext), 0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        }
        return titleString;
    }

    public static void doCommentEdit(final CommentAdapter adapter, final Context mContext, FragmentManager fm, final CommentNode baseNode) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();

        final View dialoglayout = inflater.inflate(R.layout.edit_comment, null);
        final AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(mContext);

        final EditText e = (EditText) dialoglayout.findViewById(R.id.entry);
        e.setText(StringEscapeUtils.unescapeHtml4(baseNode.getComment().getBody()));

        DoEditorActions.doActions(e, dialoglayout, fm, (Activity) mContext, baseNode.getComment().getBody());

        builder.setView(dialoglayout);
        final Dialog d = builder.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        d.show();
        dialoglayout.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        dialoglayout.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = e.getText().toString();
                new AsyncReplyTask(adapter, baseNode, text, mContext, d).execute();
            }
        });

    }

    public static void deleteComment(final CommentAdapter adapter, final Context mContext, final CommentNode baseNode, final CommentViewHolder holder) {
        new AlertDialogWrapper.Builder(mContext)
                .setTitle(R.string.comment_delete)
                .setMessage(R.string.comment_delete_msg)
                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncDeleteTask(adapter, baseNode, holder, mContext).execute();
                    }
                })
                .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public static class AsyncReplyTask extends AsyncTask<Void, Void, Void> {
        CommentAdapter adapter;
        CommentNode baseNode;
        String text;
        Context mContext;
        Dialog dialog;

        public AsyncReplyTask(CommentAdapter adapter, CommentNode baseNode, String text, Context mContext, Dialog dialog) {
            this.adapter = adapter;
            this.baseNode = baseNode;
            this.text = text;
            this.mContext = mContext;
            this.dialog = dialog;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                new AccountManager(Authentication.reddit).updateContribution(baseNode.getComment(), text);
                adapter.currentSelectedItem = baseNode.getComment().getFullName();
                adapter.dataSet.loadMoreReply(adapter);
                dialog.dismiss();
            } catch (Exception e) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialogWrapper.Builder(mContext)
                                .setTitle(R.string.comment_delete_err)
                                .setMessage(R.string.comment_delete_err_msg)
                                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        doInBackground();
                                    }
                                }).setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                    }
                });
            }
            return null;
        }
    }

    public static class AsyncDeleteTask extends AsyncTask<Void, Void, Boolean> {
        CommentAdapter adapter;
        CommentNode baseNode;
        CommentViewHolder holder;
        Context mContext;

        public AsyncDeleteTask(CommentAdapter adapter, CommentNode baseNode, CommentViewHolder holder, Context mContext) {
            this.adapter = adapter;
            this.baseNode = baseNode;
            this.holder = holder;
            this.mContext = mContext;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                holder.firstTextView.setTextHtml(mContext.getString(R.string.content_deleted));
                holder.content.setText(R.string.content_deleted);
            } else {
                new AlertDialogWrapper.Builder(mContext)
                        .setTitle(R.string.comment_delete_err)
                        .setMessage(R.string.comment_delete_err_msg)
                        .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                doInBackground();
                            }
                        }).setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                new ModerationManager(Authentication.reddit).delete(baseNode.getComment());
                adapter.deleted.add(baseNode.getComment().getFullName());
                return true;
            } catch (ApiException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static class AsyncReportTask extends AsyncTask<Void, Void, Void> {
        private CommentNode baseNode;
        private View contextView;

        public AsyncReportTask(CommentNode baseNode, View contextView) {
            this.baseNode = baseNode;
            this.contextView = contextView;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                new AccountManager(Authentication.reddit).report(baseNode.getComment(), reportReason);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Snackbar s = Snackbar.make(contextView, R.string.msg_report_sent, Snackbar.LENGTH_SHORT);
            View view = s.getView();
            TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            s.show();
        }
    }

    public static void showChildrenObject(final View v) {
        v.setVisibility(View.VISIBLE);
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1f);
        animator.setDuration(250);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) (animation.getAnimatedValue())).floatValue();
                v.setAlpha(value);
                v.setScaleX(value);
                v.setScaleY(value);
            }
        });

        animator.start();
    }

    public static void hideChildrenObject(final View v) {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0);
        animator.setDuration(250);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {


            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) (animation.getAnimatedValue())).floatValue();
                v.setAlpha(value);
                v.setScaleX(value);
                v.setScaleY(value);

            }
        });

        animator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {

            }

            @Override
            public void onAnimationRepeat(Animator arg0) {

            }

            @Override
            public void onAnimationEnd(Animator arg0) {

                v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator arg0) {
                v.setVisibility(View.GONE);

            }
        });

        animator.start();
    }

}
