package com.example.janebabra.beentheredonethat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class QuizWidgetProvider extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		Intent serviceIntent = new Intent(context, WidgetUpdateService.class);
		context.startService(serviceIntent);

	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		// Note: Ignoring the appWidgetids is safe, but could stop an update for
		// instance
		// of this app widget if more than one is running. This widget is not
		// designed to be
		// a multi-instance widget.
		Intent serviceIntent = new Intent(context, WidgetUpdateService.class);
		context.stopService(serviceIntent);

		super.onDeleted(context, appWidgetIds);

	}

	public static class WidgetUpdateService extends Service {

		WidgetUpdateTask updater;
		private static final String DEBUG_TAG = "WidgetUpdateService";

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			updater = new WidgetUpdateTask();
			updater.execute(startId);
			// if we're killed, restart us with the original Intent so we get an
			// extra data again, should we choose to use it later
			return START_REDELIVER_INTENT;
		}

		@Override
		public void onDestroy() {
			updater.cancel(true);
			super.onDestroy();
		}

		@Override
		public IBinder onBind(Intent intent) {
			// no binding; can't from an App Widget
			return null;
		}

		private class WidgetUpdateTask extends
				AsyncTask<Integer, Void, Boolean> {

			@Override
			protected Boolean doInBackground(Integer... startIds) {
				return widgetUpdate(startIds[0]);
			}

			/**
			 * The widget update code
			 * 
			 * @param startId
			 *            the id of the widget we're dealing with
			 * @return boolean , false on any error
			 */
			private boolean widgetUpdate(int startId) {
				boolean succeeded = false;
				Context context = WidgetUpdateService.this;
				SharedPreferences prefs = getSharedPreferences(
						QuizActivity.GAME_PREFERENCES, Context.MODE_PRIVATE);
				Integer playerId = prefs.getInt(
						QuizActivity.GAME_PREFERENCES_PLAYER_ID, -1);

				WidgetData playerData = getWidgetData(playerId);
				WidgetData friendData = getTopFriendWidgetData(playerId);

				// prep the RemoteView
				String packageName = context.getPackageName();
				Log.d(DEBUG_TAG, "packageName: " + packageName);
				RemoteViews remoteView = new RemoteViews(
						context.getPackageName(), R.layout.widget);

				remoteView.setTextViewText(R.id.widget_left_nickname,
						playerData.nickname);
				remoteView.setTextViewText(R.id.widget_left_score, "Score: "
						+ playerData.score);

				remoteView.setTextViewText(R.id.widget_right_nickname,
						friendData.nickname);
				remoteView.setTextViewText(R.id.widget_right_score, "Score: "
						+ friendData.score);

				setWidgetAvatar(remoteView, playerData.avatarUrl,
						R.id.widget_left_image);
				setWidgetAvatar(remoteView, friendData.avatarUrl,
						R.id.widget_right_image);

				try {

					// add click handling
					Intent launchAppIntent = new Intent(context,
							QuizMenuActivity.class);
					PendingIntent launchAppPendingIntent = PendingIntent
							.getActivity(context, 0, launchAppIntent,
									PendingIntent.FLAG_UPDATE_CURRENT);
					remoteView.setOnClickPendingIntent(R.id.widget_view,
							launchAppPendingIntent);

					// get the Android component name for the QuizWidgetProvider
					ComponentName quizWidget = new ComponentName(context,
							QuizWidgetProvider.class);

					// get the instance of the AppWidgetManager
					AppWidgetManager appWidgetManager = AppWidgetManager
							.getInstance(context);

					// update the widget
					appWidgetManager.updateAppWidget(quizWidget, remoteView);
					succeeded = true;

				} catch (Exception e) {
					Log.e(DEBUG_TAG, "Failed to update widget", e);
				}

				if (!WidgetUpdateService.this.stopSelfResult(startId)) {
					Log.e(DEBUG_TAG, "Failed to stop service");
				}

				return succeeded;

			}

			/**
			 * @param remoteView
			 * @param avatarUrl
			 * @param imageId
			 */
			private void setWidgetAvatar(RemoteViews remoteView,
					String avatarUrl, int imageId) {
				if (avatarUrl != null && avatarUrl.length() > 0) {
					URL image;
					try {
						image = new URL(avatarUrl);
						Log.d(DEBUG_TAG, "avatarUrl: " + avatarUrl);

						BufferedInputStream stream = new BufferedInputStream(
								image.openStream());
						Bitmap bitmap = BitmapFactory.decodeStream(stream);

						if (bitmap == null) {
							Log.w(DEBUG_TAG, "Failed to decode image");
							remoteView.setImageViewResource(imageId,
									R.drawable.avatar);
						} else {
							remoteView.setImageViewBitmap(imageId, bitmap);
						}
					} catch (MalformedURLException e) {
						Log.e(DEBUG_TAG, "Bad url in image", e);
					} catch (IOException e) {
						Log.e(DEBUG_TAG, "IO failure for image", e);
					}

				} else {
					remoteView.setImageViewResource(imageId, R.drawable.avatar);
				}
			}

			/**
			 * Download data for displaying in the Widget
			 * 
			 * @param widgetData
			 * @return
			 */
			private WidgetData getWidgetData(int playerId) {
				boolean succeeded = false;
				String nickname = null;
				String score = null;
				String avatarUrl = null;

				try {
					URL userInfo = new URL(QuizActivity.TRIVIA_SERVER_BASE
							+ "getplayer?playerId=" + playerId);
					XmlPullParser parser = XmlPullParserFactory.newInstance()
							.newPullParser();
					parser.setInput(userInfo.openStream(), null);

					int eventType = -1;
					while (eventType != XmlPullParser.END_DOCUMENT) {
						if (eventType == XmlPullParser.START_TAG) {
							String strName = parser.getName();

							if (strName.equals("nickname")) {
								nickname = parser.nextText();
							} else if (strName.equals("score")) {
								score = parser.nextText();
							} else if (strName.equals("avatarUrl")) {
								avatarUrl = parser.nextText();
							}
						}
						eventType = parser.next();
					}
					succeeded = true;
				} catch (MalformedURLException e) {
					Log.e(DEBUG_TAG, "Bad URL", e);
				} catch (XmlPullParserException e) {
					Log.e(DEBUG_TAG, "Parser exception", e);
				} catch (IOException e) {
					Log.e(DEBUG_TAG, "IO Exception", e);
				}
				if (succeeded) {
					return new WidgetData(nickname, score, avatarUrl);
				} else {
					return new WidgetData("Unknown", "N/A", null);
				}
			}

			/**
			 * Get the info for the highest scoring friend (of playerId)
			 * 
			 * @return WidgetData containing info from the highest scoring
			 *         friend :)
			 */
			public WidgetData getTopFriendWidgetData(int playerId) {
				URL friendScoresUrl;
				int eventType = -1;
				String scoreValue = "N/A";
				String scoreAvatarUrl = null;
				String scoreUserName = "Unknown";
				try {
					friendScoresUrl = new URL(QuizActivity.TRIVIA_SERVER_SCORES
							+ "?playerId=" + playerId);
					XmlPullParser friendScores = XmlPullParserFactory
							.newInstance().newPullParser();
					friendScores.setInput(friendScoresUrl.openStream(), null);

					// Find Score records from XML
					while (eventType != XmlResourceParser.END_DOCUMENT) {
						if (eventType == XmlResourceParser.START_TAG) {

							// Get the name of the tag (eg scores or score)
							String strName = friendScores.getName();

							if (strName.equals("score")) {
								scoreValue = friendScores.getAttributeValue(
										null, "score");
								scoreAvatarUrl = friendScores
										.getAttributeValue(null, "avatarUrl");
								scoreUserName = friendScores.getAttributeValue(
										null, "username");
								// we only want the first one
								break;
							}
						}
						eventType = friendScores.next();
					}

				} catch (MalformedURLException e) {
					Log.e(DEBUG_TAG, "Bad friend score URL", e);
				} catch (XmlPullParserException e) {
					Log.e(DEBUG_TAG, "Pull parser failure", e);
				} catch (IOException e) {
					Log.e(DEBUG_TAG, "IO failure reading friend scores", e);
				}
				return new WidgetData(scoreUserName, scoreValue, scoreAvatarUrl);
			}

		}

		public class WidgetData {
			String nickname;
			String score;
			String avatarUrl;

			public WidgetData(String nickname, String score, String avatarUrl) {
				super();
				this.nickname = nickname;
				this.score = score;
				this.avatarUrl = avatarUrl;
			}

			public String getNickname() {
				return nickname;
			}

			public void setNickname(String nickname) {
				this.nickname = nickname;
			}

			public String getScore() {
				return score;
			}

			public void setScore(String score) {
				this.score = score;
			}

			public String getAvatarUrl() {
				return avatarUrl;
			}

			public void setAvatarUrl(String avatarUrl) {
				this.avatarUrl = avatarUrl;
			}
		}

	}

}
