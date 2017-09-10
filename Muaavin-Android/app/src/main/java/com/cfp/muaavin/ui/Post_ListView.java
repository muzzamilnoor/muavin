package com.cfp.muaavin.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cfp.muaavin.facebook.AsyncResponsePosts;
import com.cfp.muaavin.facebook.FacebookUtil;
import com.cfp.muaavin.helper.PrefManager;
import com.cfp.muaavin.helper.border;
import com.cfp.muaavin.loaders.PostsLoadAsyncTask;
import com.cfp.muaavin.adapter.Posts_CustomAdapter;
import com.cfp.muaavin.facebook.Post;
import com.cfp.muaavin.adapter.Users_CustomAdapter;
import com.cfp.muaavin.facebook.User;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;


public class Post_ListView extends Fragment implements AsyncResponsePosts {

    ArrayList<Post> User_Posts;
    ListView lv ;
    boolean ClipBoardOption ;
    Context context;
    boolean IsTwitterData;
    boolean GroupPostOption;
    Button loadPosts;
    ProgressDialog progressDialog = null;

    public ArrayList<Post> SelectivePosts = new ArrayList<Post>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view  = inflater.inflate(R.layout.activity_list_view, container, false);
        context = getActivity();
        SelectivePosts = (ArrayList<Post>)  getArguments().getSerializable("user_posts");
        User_Posts = new ArrayList<Post>();

        ClipBoardOption = getArguments().getBoolean("ClipBoardOption", false);
        GroupPostOption = getArguments().getBoolean("GroupPostOption", false);
        IsTwitterData =   getArguments().getBoolean("isTwitterData", false);
        loadPosts=(Button) view.findViewById(R.id.LoadPosts);
        lv=(ListView) view.findViewById(R.id.listView1);
        Posts_CustomAdapter c = new Posts_CustomAdapter(getActivity(), SelectivePosts , User.getLoggedInUserInformation().id,ClipBoardOption,GroupPostOption,this);
        lv.setAdapter(c);

        loadPosts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PostsLoadAsyncTask.nextResultsRequests != null)
                {
                    new PostsLoadAsyncTask("Timeline Posts",context, Post_ListView.this, User.getLoggedInUserInformation().id, ClipBoardOption, "me", new ArrayList<Post>(), new ArrayList<User>()).execute(new ArrayList<Post>());
                }
            }
        });
        return  view;
    }

    public void loadPosts(View view)
    {
        if (PostsLoadAsyncTask.nextResultsRequests != null)
        {
            new PostsLoadAsyncTask("Timeline Posts",context, Post_ListView.this, User.getLoggedInUserInformation().id, ClipBoardOption, "me", new ArrayList<Post>(), new ArrayList<User>()).execute(new ArrayList<Post>());
        }
    }

    @Override
    public void getUserAndPostData(ArrayList<Post> result, String option)
    {
        SelectivePosts = FacebookUtil.Posts;
        if((!GroupPostOption)&&(!ClipBoardOption))
        SelectivePosts = Users_CustomAdapter.getSelectivePosts(FacebookUtil.ReportPostDetail.infringing_user_id, SelectivePosts);
        Posts_CustomAdapter c = new Posts_CustomAdapter( getActivity(), SelectivePosts, User.getLoggedInUserInformation().id,ClipBoardOption,GroupPostOption, this);
        lv.setAdapter(c);
        //((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
    }

    @Override
    public void postLink(String type, String postLink, String userProfile, String message, String userName, int check) {


        if(type.equals("Link Posting")) {

            takeScreenshot(type,postLink,userProfile,message,userName,check);
        }

        if(type.equals("Photo Posting")) {
            takeScreenshot(type,postLink,userProfile,message,userName,check);
        }
    }

    private void takeScreenshot(String type, final String post, final String user, final String message, final String userName, final int check) {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String path = Environment.getExternalStorageDirectory().toString() + "/Muavin";
            File f = new File(path);
            if(f.exists()){}
            else
                f.mkdir();

            String mPath = Environment.getExternalStorageDirectory().toString() + "/Muavin/" + now + ".jpg";

            // create bitmap screen capture
            View v1 = getActivity().getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);
            final File imageFile = new File(mPath);
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            if(type.equals("Link Posting"))
                showDialog(bitmap, post,user, message, userName, check);

            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

            outputStream.flush();
            outputStream.close();


            MediaScannerConnection.scanFile(context,
                    new String[]{imageFile.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });

            //       Toast.makeText(context, "Screenshot saved", Toast.LENGTH_SHORT).show();


            if(type.equals("Photo Posting")) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                builder.setMessage("Proceed with posting?")
                        .setCancelable(false)
                        .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(context, UploadImages.class);
                                intent.putExtra("file_name",imageFile.getName());
                                intent.putExtra("post_url",post);
                                intent.putExtra("user_profile",user);
                                intent.putExtra("message",message);
                                intent.putExtra("user_name",userName);
                                intent.putExtra("check",check);

                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do nothing
                                return;
                            }
                        });
                android.app.AlertDialog alert = builder.create();
                alert.show();
            }
            // openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            Toast.makeText(context, "Unable to take screenshot", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void showDialog(final Bitmap bitmap, final String post, final String user, final String message, final String userName, final int check){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add description...");
        final EditText input = new EditText(context);
        input.setSingleLine(false);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(3);
        input.setMaxLines(10);
        input.setVerticalScrollBarEnabled(true);
        input.setMovementMethod(ScrollingMovementMethod.getInstance());
        input.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 30;
        params.rightMargin = 30;
        params.topMargin = 10;
        params.bottomMargin = 10;
        input.setBackgroundDrawable(new border(0xff999999, 10));
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                postPostLink(bitmap, post,user,input.getText().toString(), message, userName,check);
                dialog.cancel();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //      postPostLink(bitmap, post,user,"", message, userName);
                dialog.cancel();
            }
        });

        builder.show();

    }

    public void postPostLink(final Bitmap bitmap, String post, final String user, final String caption, final String message, final String userName, final int check){

        showLoading(context);
        Bundle params = new Bundle();

        if(PrefManager.getInstance(context).isAnonymous()){
            if(check==0) {
                String ids[] = post.split("_");
                if (caption == null || caption.equals(""))
                    params.putString("message", " A comment of " + userName + " ( https://www.facebook.com/app_scoped_user_id/" + user + " )" + " has been notified : " + message + "\n Visit post " + "http://www.facebook.com/"+ids[0]+"/posts/"+ids[1]);
                else
                    params.putString("message", " A comment of " +
                            userName + " ( https://www.facebook.com/app_scoped_user_id/" + user + " ) " + "has been notified : " + message + "\nNotifier's remarks : " + caption + " \n " + "Visit post " + "http://www.facebook.com/"+ids[0]+"/posts/"+ids[1]);
            }
            else if(check ==5)
            {
                if(caption==null||caption.equals(""))
                    params.putString("message", " A comment of "+userName+" ( https://twitter.com/"+user+" )"+" has been notified : "+message+"\n Visit post "+"https://twitter.com/"+post);
                else
                    params.putString("message", " A comment of "+
                            userName+" ( https://twitter.com/"+user+" ) "+"has been notified : "+message+"\nNotifier's remarks : "+caption+" \n "+"Visit post "+"https://twitter.com/"+post);
            }
        }
        else{
            String[] arr = {User.getLoggedInUserInformation().id};
            params.putStringArray("tags", arr);

            if(check==0) {
                String ids[] = post.split("_");

                if (caption == null || caption.equals(""))
        params.putString("message", User.getLoggedInUserInformation().name + " ( https://www.facebook.com/app_scoped_user_id/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " + userName + " ( https://www.facebook.com/app_scoped_user_id/" + user + " )" + " : " + message + "\n Visit post " + "http://www.facebook.com/"+ids[0]+"/posts/"+ids[1]);
        else
        params.putString("message", User.getLoggedInUserInformation().name + " ( https://www.facebook.com/app_scoped_user_id/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " +
                userName + " ( https://www.facebook.com/app_scoped_user_id/" + user + " ) " + " : " + message + "\nNotifier's remarks : " + caption + " \n " + "Visit post " +"http://www.facebook.com/"+ids[0]+"/posts/"+ids[1]);
        }
        else if(check ==5)
        {
            if(caption==null||caption.equals(""))
                params.putString("message", User.getLoggedInUserInformation().name +" ( https://twitter.com/"+User.getLoggedInUserInformation().id+ " ) has notified a comment of "+userName+" ( https://twitter.com/"+user+" )"+" : "+message+"\n Visit post "+"https://twitter.com/"+post);
            else
                params.putString("message", User.getLoggedInUserInformation().name +" ( https://twitter.com/"+User.getLoggedInUserInformation().id+ " ) has notified a comment of "+
                        userName+" ( https://twitter.com/"+user+" ) "+" : "+message+"\nNotifier's remarks : "+caption+" \n "+"Visit post "+"https://twitter.com/"+post);
        }
        }
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, blob);
        byte[] bitmapdata = blob.toByteArray();
        params.putByteArray("picture", bitmapdata);
        params.putString("link", "https://www.google.com/");
        new GraphRequest(
                new AccessToken(MenuActivity.pageToken, AccessToken.getCurrentAccessToken().getApplicationId(), AccessToken.getCurrentAccessToken().getUserId(), AccessToken.getCurrentAccessToken().getPermissions(), AccessToken.getCurrentAccessToken().getDeclinedPermissions(), AccessToken.getCurrentAccessToken().getSource(), AccessToken.getCurrentAccessToken().getExpires(), AccessToken.getCurrentAccessToken().getLastRefresh()),
                "/"+ MenuActivity.pageId+"/photos",
                params,
                HttpMethod.POST,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        if (response.getError() == null) {
                            hideLoading();
                            Toast.makeText(context, "Post notified successfully", Toast.LENGTH_LONG).show();

                        } else {
                            Bundle params = new Bundle();
/*
                            if(caption.equals("")||caption==null)
                                params.putString("caption", User.getLoggedInUserInformation().name+" ( https://web.facebook.com/"+User.getLoggedInUserInformation().id+ " ) has reported the following: \n Offender Details -> "+userName+" ( https://web.facebook.com/"+user+" ) "+" \n Comment -> "+message);
                            else
                                params.putString("caption", User.getLoggedInUserInformation().name+" ( https://web.facebook.com/"+User.getLoggedInUserInformation().id+ " ) has reported the following:\n"+
                                        caption+"\n" + "Offender Details -> "+userName+" ( https://web.facebook.com/"+user+" ) "+" \n Comment -> "+message);
*/
                            if(PrefManager.getInstance(context).isAnonymous())
                                {
                            if(check==0) {
                                if (caption == null || caption.equals(""))
                                    params.putString("message", " A comment of " + userName + " ( https://www.facebook.com/app_scoped_user_id/" + user + " )" + " has been notified : " + message);
                                else
                                    params.putString("message", " A comment of " +
                                            userName + " ( https://www.facebook.com/app_scoped_user_id/" + user + " ) " + "has been notified : " + message + "\nNotifier's remarks : " + caption);
                            }
                            else if(check==5)
                            {
                                if (caption == null || caption.equals(""))
                                    params.putString("message", " A comment of " + userName + " ( https://twitter.com/" + user + " )" + " has been notified : " + message);
                                else
                                    params.putString("message",  " A comment of " +
                                            userName + " ( https://twitter.com/" + user + " ) " + "has been notified : " + message + "\nNotifier's remarks : " + caption);
                            }}
                                else
                                {
                                    String[] arr = {User.getLoggedInUserInformation().id};
                                    params.putStringArray("tags", arr);
                                    if(check==0) {
                                        if (caption == null || caption.equals(""))
                                            params.putString("message", User.getLoggedInUserInformation().name + " ( https://www.facebook.com/app_scoped_user_id/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " + userName + " ( https://www.facebook.com/app_scoped_user_id/" + user + " )" + " : " + message);
                                        else
                                            params.putString("message", User.getLoggedInUserInformation().name + " ( https://www.facebook.com/app_scoped_user_id/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " +
                                                    userName + " ( https://www.facebook.com/app_scoped_user_id/" + user + " ) " + " : " + message + "\nNotifier's remarks : " + caption);
                                    }
                                    else if(check==5)
                                    {
                                        if (caption == null || caption.equals(""))
                                            params.putString("message", User.getLoggedInUserInformation().name + " ( https://twitter.com/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " + userName + " ( https://twitter.com/" + user + " )" + " : " + message);
                                        else
                                            params.putString("message", User.getLoggedInUserInformation().name + " ( https://twitter.com/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " +
                                                    userName + " ( https://twitter.com/" + user + " ) " + " : " + message + "\nNotifier's remarks : " + caption);
                                    }
                                }
//                            params.putString("link", "https://web.facebook.com/" + User.getLoggedInUserInformation().name);

                            ByteArrayOutputStream blob = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, blob);
                            byte[] bitmapdata = blob.toByteArray();
                            params.putByteArray("picture", bitmapdata);

                            new GraphRequest(
                                    new AccessToken(MenuActivity.pageToken, AccessToken.getCurrentAccessToken().getApplicationId(), AccessToken.getCurrentAccessToken().getUserId(), AccessToken.getCurrentAccessToken().getPermissions(), AccessToken.getCurrentAccessToken().getDeclinedPermissions(), AccessToken.getCurrentAccessToken().getSource(), AccessToken.getCurrentAccessToken().getExpires(), AccessToken.getCurrentAccessToken().getLastRefresh()),
                                    "/"+MenuActivity.pageId+"/photos",
                                    params,
                                    HttpMethod.POST,
                                    new GraphRequest.Callback() {
                                        public void onCompleted(GraphResponse response) {
                                            if (response.getError() == null) {
                                                hideLoading();
                                                Toast.makeText(context, "Post notified successfully", Toast.LENGTH_LONG).show();
                                            } else {
                                                hideLoading();
                                                Toast.makeText(context, "Unable to notify Post, Please try again", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                            ).executeAsync();
                        }
                    }
                }
        ).executeAsync();
    }
/*

    public void postPostLink(final Bitmap bitmap, String post, final String user, final String caption, final String message, final String userName, final int check){

        showLoading(context);
        Bundle params = new Bundle();

        if(PrefManager.getInstance(context).isAnonymous()){
            if(check==0) {
                if (caption == null || caption.equals(""))
                    params.putString("message", " A comment of " + userName + " ( https://web.facebook.com/" + user + " )" + " has been notified : " + message + "\n Visit post " + "https://web.facebook.com/" + post);
                else
                    params.putString("message", " A comment of " +
                            userName + " ( https://web.facebook.com/" + user + " ) " + "has been notified : " + message + "\nNotifier's remarks : " + caption + " \n " + "Visit post " + "https://web.facebook.com/" + post);
            }
            else if(check ==5)
            {
                if(caption==null||caption.equals(""))
                    params.putString("message", " A comment of "+userName+" ( https://twitter.com/"+user+" )"+" has been notified : "+message+"\n Visit post "+"https://twitter.com/"+post);
                else
                    params.putString("message", " A comment of "+
                            userName+" ( https://twitter.com/"+user+" ) "+"has been notified : "+message+"\nNotifier's remarks : "+caption+" \n "+"Visit post "+"https://twitter.com/"+post);
            }
        }
        else{
            String[] arr = {User.getLoggedInUserInformation().id};
            params.putStringArray("tags", arr);

            if(check==0) {
                if (caption == null || caption.equals(""))
                    params.putString("message", User.getLoggedInUserInformation().name + " ( https://web.facebook.com/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " + userName + " ( https://web.facebook.com/" + user + " )" + " : " + message + "\n Visit post " + "https://web.facebook.com/" + post);
                else
                    params.putString("message", User.getLoggedInUserInformation().name + " ( https://web.facebook.com/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " +
                            userName + " ( https://web.facebook.com/" + user + " ) " + " : " + message + "\nNotifier's remarks : " + caption + " \n " + "Visit post " + "https://web.facebook.com/" + post);
            }
            else if(check ==5)
            {
                if(caption==null||caption.equals(""))
                    params.putString("message", User.getLoggedInUserInformation().name +" ( https://twitter.com/"+User.getLoggedInUserInformation().id+ " ) has notified a comment of "+userName+" ( https://twitter.com/"+user+" )"+" : "+message+"\n Visit post "+"https://twitter.com/"+post);
                else
                    params.putString("message", User.getLoggedInUserInformation().name +" ( https://twitter.com/"+User.getLoggedInUserInformation().id+ " ) has notified a comment of "+
                            userName+" ( https://twitter.com/"+user+" ) "+" : "+message+"\nNotifier's remarks : "+caption+" \n "+"Visit post "+"https://twitter.com/"+post);
            }
        }
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, blob);
        byte[] bitmapdata = blob.toByteArray();
        params.putByteArray("picture", bitmapdata);
        params.putString("link", "https://www.google.com/");
        new GraphRequest(
                new AccessToken(MenuActivity.pageToken, AccessToken.getCurrentAccessToken().getApplicationId(), AccessToken.getCurrentAccessToken().getUserId(), AccessToken.getCurrentAccessToken().getPermissions(), AccessToken.getCurrentAccessToken().getDeclinedPermissions(), AccessToken.getCurrentAccessToken().getSource(), AccessToken.getCurrentAccessToken().getExpires(), AccessToken.getCurrentAccessToken().getLastRefresh()),
                "/"+ MenuActivity.pageId+"/photos",
                params,
                HttpMethod.POST,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        if (response.getError() == null) {
                            hideLoading();
                            Toast.makeText(context, "Post notified successfully", Toast.LENGTH_LONG).show();

                        } else {
                            Bundle params = new Bundle();
*/
/*
                            if(caption.equals("")||caption==null)
                                params.putString("caption", User.getLoggedInUserInformation().name+" ( https://web.facebook.com/"+User.getLoggedInUserInformation().id+ " ) has reported the following: \n Offender Details -> "+userName+" ( https://web.facebook.com/"+user+" ) "+" \n Comment -> "+message);
                            else
                                params.putString("caption", User.getLoggedInUserInformation().name+" ( https://web.facebook.com/"+User.getLoggedInUserInformation().id+ " ) has reported the following:\n"+
                                        caption+"\n" + "Offender Details -> "+userName+" ( https://web.facebook.com/"+user+" ) "+" \n Comment -> "+message);
*//*

                            if(PrefManager.getInstance(context).isAnonymous())
                            {
                                if(check==0) {
                                    if (caption == null || caption.equals(""))
                                        params.putString("message", " A comment of " + userName + " ( https://web.facebook.com/" + user + " )" + " has been notified : " + message);
                                    else
                                        params.putString("message", " A comment of " +
                                                userName + " ( https://web.facebook.com/" + user + " ) " + "has been notified : " + message + "\nNotifier's remarks : " + caption);
                                }
                                else if(check==5)
                                {
                                    if (caption == null || caption.equals(""))
                                        params.putString("message", " A comment of " + userName + " ( https://twitter.com/" + user + " )" + " has been notified : " + message);
                                    else
                                        params.putString("message",  " A comment of " +
                                                userName + " ( https://twitter.com/" + user + " ) " + "has been notified : " + message + "\nNotifier's remarks : " + caption);
                                }}
                            else
                            {
                                String[] arr = {User.getLoggedInUserInformation().id};
                                params.putStringArray("tags", arr);
                                if(check==0) {
                                    if (caption == null || caption.equals(""))
                                        params.putString("message", User.getLoggedInUserInformation().name + " ( https://web.facebook.com/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " + userName + " ( https://web.facebook.com/" + user + " )" + " : " + message);
                                    else
                                        params.putString("message", User.getLoggedInUserInformation().name + " ( https://web.facebook.com/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " +
                                                userName + " ( https://web.facebook.com/" + user + " ) " + " : " + message + "\nNotifier's remarks : " + caption);
                                }
                                else if(check==5)
                                {
                                    if (caption == null || caption.equals(""))
                                        params.putString("message", User.getLoggedInUserInformation().name + " ( https://twitter.com/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " + userName + " ( https://twitter.com/" + user + " )" + " : " + message);
                                    else
                                        params.putString("message", User.getLoggedInUserInformation().name + " ( https://twitter.com/" + User.getLoggedInUserInformation().id + " ) has notified a comment of " +
                                                userName + " ( https://twitter.com/" + user + " ) " + " : " + message + "\nNotifier's remarks : " + caption);
                                }
                            }
//                            params.putString("link", "https://web.facebook.com/" + User.getLoggedInUserInformation().name);

                            ByteArrayOutputStream blob = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, blob);
                            byte[] bitmapdata = blob.toByteArray();
                            params.putByteArray("picture", bitmapdata);

                            new GraphRequest(
                                    new AccessToken(MenuActivity.pageToken, AccessToken.getCurrentAccessToken().getApplicationId(), AccessToken.getCurrentAccessToken().getUserId(), AccessToken.getCurrentAccessToken().getPermissions(), AccessToken.getCurrentAccessToken().getDeclinedPermissions(), AccessToken.getCurrentAccessToken().getSource(), AccessToken.getCurrentAccessToken().getExpires(), AccessToken.getCurrentAccessToken().getLastRefresh()),
                                    "/"+MenuActivity.pageId+"/photos",
                                    params,
                                    HttpMethod.POST,
                                    new GraphRequest.Callback() {
                                        public void onCompleted(GraphResponse response) {
                                            if (response.getError() == null) {
                                                hideLoading();
                                                Toast.makeText(context, "Post notified successfully", Toast.LENGTH_LONG).show();
                                            } else {
                                                hideLoading();
                                                Toast.makeText(context, "Unable to notify Post, Please try again", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                            ).executeAsync();
                        }
                    }
                }
        ).executeAsync();
    }
*/

    public void showLoading(Context context) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.show();
        progressDialog.getWindow().setBackgroundDrawable
                (new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(new ProgressBar(context));
    }

    public void hideLoading() {
        progressDialog.setCancelable(true);
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
