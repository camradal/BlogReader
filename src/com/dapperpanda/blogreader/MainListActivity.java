package com.dapperpanda.blogreader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainListActivity extends ListActivity {
	
	protected JSONObject mBlogData;
	protected ProgressBar mProgressBar;
	
	public static final int NUMBER_OF_POSTS = 20;
	public static final String TAG = MainListActivity.class.getSimpleName();
	
	private final String key_title = "title";
	private final String key_author = "author";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_list);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
		
		if (isNetworkAvailable()) {
			mProgressBar.setVisibility(View.VISIBLE);
			GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
			getBlogPostsTask.execute();
		}
		else {
			Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		try {
			JSONArray jsonPosts = mBlogData.getJSONArray("posts");
			JSONObject jsonPost = jsonPosts.getJSONObject(position);
			String blogUrl = jsonPost.getString("url");
			Boolean useBrowser = false;
			if (useBrowser) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(blogUrl));
				startActivity(intent);
			}
			else {
				Intent intent = new Intent(this, BlogWebViewActivity.class);
				intent.setData(Uri.parse(blogUrl));
				startActivity(intent);
			}
		} catch (JSONException e) {
			logException(e);
		}
	}

	private void logException(Exception e) {
		Log.e(TAG, "Exception caught", e);
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		boolean isAvailable = false;
		if (networkInfo != null && networkInfo.isConnected()) {
			isAvailable = true;
		}
		
		return isAvailable;
	}
	
	public void handleBlogResponse() {
		mProgressBar.setVisibility(View.INVISIBLE);
		
		if (mBlogData == null) {
			updateDisplayForError();
		}
		else {
			try {
				JSONArray jsonPosts = mBlogData.getJSONArray("posts");
				ArrayList<HashMap<String, String>> blogPosts = new ArrayList<HashMap<String, String>>(); 
				for	(int i = 0; i < jsonPosts.length(); i++) {
					JSONObject post = jsonPosts.getJSONObject(i);
					String title = post.getString("title");
					title = Html.fromHtml(title).toString();
					String author = post.getString("author");
					author = Html.fromHtml(author).toString();
					
					HashMap<String, String> blogPost = new HashMap<String, String>();
					blogPost.put(key_title, title);
					blogPost.put(key_author, author);
					blogPosts.add(blogPost);
				}
				
				String[] keys = { key_title, key_author };
				int[] ids = { android.R.id.text1, android.R.id.text2 };
				SimpleAdapter adapter = new SimpleAdapter(this, blogPosts, android.R.layout.simple_list_item_2, keys, ids);
				setListAdapter(adapter);
			} catch (JSONException e) {
				
				logException(e);
			}
		}
	}

	private void updateDisplayForError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.error_title));
		builder.setMessage(getString(R.string.error_msg));
		builder.setPositiveButton(android.R.string.ok, null);
		AlertDialog dialog = builder.create();
		dialog.show();
		
		TextView emptyTextView = (TextView)getListView().getEmptyView();
		emptyTextView.setText(getString(R.string.no_items));
	}
	
	private class GetBlogPostsTask extends AsyncTask<Object, Void, JSONObject>{
		@Override
		protected JSONObject doInBackground(Object... arg0) {
			int responsecode = -1;
			JSONObject jsonResponse = null;
			
			try {
				URL blogFeedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POSTS);
				HttpURLConnection connection = (HttpURLConnection)blogFeedUrl.openConnection();
				connection.connect();
				
				responsecode = connection.getResponseCode();
				if (responsecode == HttpURLConnection.HTTP_OK) {
					jsonResponse = readFromConnection(connection);
				}
				else {
					Log.i(TAG, "Unsuccessful HTTP code: " + responsecode);
				}
			}
			catch (MalformedURLException e) {
				logException(e);
			}
			catch (IOException e) {
				logException(e);
			}
			catch (Exception e) {
				logException(e);
			}
			
			return jsonResponse;
		}
		
		@Override
		protected void onPostExecute(JSONObject result) {
			mBlogData = result;
			handleBlogResponse();
		};

		private JSONObject readFromConnection(HttpURLConnection connection)
				throws IOException, JSONException {
			InputStream stream = connection.getInputStream();
			Reader reader = new InputStreamReader(stream);
			int contentLength = connection.getContentLength();
			char[] chars = new char[contentLength];
			reader.read(chars);
			String response = new String(chars);
			JSONObject jsonResponse = new JSONObject(response);
			String status = jsonResponse.getString("status");
			Log.v(TAG, status);
			JSONArray array = jsonResponse.getJSONArray("posts");
			for (int i = 0; i < array.length(); i++) {
				JSONObject post = array.getJSONObject(i);
				Log.v(TAG, post.getString("title"));
				Log.i("CodeChallenge", post.getString("title") + ", " + "blah");
			}
			return jsonResponse;
		}
	}
}
