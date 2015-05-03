package com.example.janebabra.beentheredonethat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class QuizGameActivity extends QuizActivity {

	SharedPreferences mGameSettings;
	Hashtable<Integer, Question> mQuestions;

	private TextSwitcher mQuestionText;
	private ImageSwitcher mQuestionImage;

	QuizTask downloader;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game);

		// Set up Text Switcher
		mQuestionText = (TextSwitcher) findViewById(R.id.TextSwitcher_QuestionText);
		mQuestionText.setFactory(new MyTextSwitcherFactory());

		// Set up Image Switcher
		mQuestionImage = (ImageSwitcher) findViewById(R.id.ImageSwitcher_QuestionImage);
		mQuestionImage.setFactory(new MyImageSwitcherFactory());

		// Retrieve the shared preferences
		mGameSettings = getSharedPreferences(GAME_PREFERENCES,
				Context.MODE_PRIVATE);

		// Initialize question batch
		mQuestions = new Hashtable<Integer, Question>(QUESTION_BATCH_SIZE);

		// Load the questions
		int startingQuestionNumber = mGameSettings.getInt(
				GAME_PREFERENCES_CURRENT_QUESTION, 0);

		// If we're at the beginning of the quiz, initialize the Shared
		// preferences
		if (startingQuestionNumber == 0) {
			Editor editor = mGameSettings.edit();
			editor.putInt(GAME_PREFERENCES_CURRENT_QUESTION, 1);
			editor.commit();
			startingQuestionNumber = 1;
		}

		// Start loading the questions in the background
		downloader = new QuizTask();
		downloader.execute(TRIVIA_SERVER_QUESTIONS, startingQuestionNumber);

	}

	@Override
	protected void onPause() {
		if (downloader != null
				&& downloader.getStatus() != AsyncTask.Status.FINISHED) {
			Log.d(DEBUG_TAG, "downloader.cancel");
			// if we don't call this before onPause() returns, we'll leak the dialog as the systems cleans up View objects before the onCancelled() of the AsyncTask is actually
			// executed. Calling pleaseWaitDialog.dismiss() a second time does nothing. You could check to see if it's showing with .isShowing().
			pleaseWaitDialog.dismiss();
			downloader.cancel(true);
		}
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.gameoptions, menu);
		menu.findItem(R.id.help_menu_item).setIntent(
				new Intent(this, QuizHelpActivity.class));
		menu.findItem(R.id.settings_menu_item).setIntent(
				new Intent(this, QuizSettingsActivity.class));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		startActivity(item.getIntent());
		return true;
	}

	public void onNoButton(View v) {
		handleAnswerAndShowNextQuestion(false);
	}

	public void onYesButton(View v) {
		handleAnswerAndShowNextQuestion(true);
	}

	/**
	 * Called when question loading is complete
	 * 
	 * @param startingQuestionNumber
	 *            The first question number that should be available
	 */
	private void displayCurrentQuestion(int startingQuestionNumber) {
		// If the question was loaded properly, display it
		if (mQuestions.containsKey(startingQuestionNumber) == true) {
			// Set the text of the textswitcher
			mQuestionText
					.setCurrentText(getQuestionText(startingQuestionNumber));

			// Set the image of the imageswitcher
			Drawable image = getQuestionImageDrawable(startingQuestionNumber);
			mQuestionImage.setImageDrawable(image);
		} else {
			// Tell the user we don't have any new questions at this time
			handleNoQuestions();
		}
	}

	/**
	 * 
	 * Helper method to record the answer the user gave and load up the next
	 * question.
	 * 
	 * @param bAnswer
	 *            The answer the user gave
	 */
	private void handleAnswerAndShowNextQuestion(boolean bAnswer) {
		int curScore = mGameSettings.getInt(GAME_PREFERENCES_SCORE, 0);
		int nextQuestionNumber = mGameSettings.getInt(
				GAME_PREFERENCES_CURRENT_QUESTION, 1) + 1;

		Editor editor = mGameSettings.edit();
		editor.putInt(GAME_PREFERENCES_CURRENT_QUESTION, nextQuestionNumber);

		// Log the number of "yes" answers only
		if (bAnswer == true) {
			editor.putInt(GAME_PREFERENCES_SCORE, curScore + 1);
		}
		editor.commit();

		if (mQuestions.containsKey(nextQuestionNumber) == false) {

			downloader = new QuizTask();
			downloader.execute(TRIVIA_SERVER_QUESTIONS, nextQuestionNumber);

			// current question display gets deferred until this is done
		} else {

			displayCurrentQuestion(nextQuestionNumber);
		}

	}

	/**
	 * Helper method to configure the question screen when no questions were
	 * found. Could be called for a variety of error cases, including no new
	 * questions, IO failures, or parser failures.
	 */
	private void handleNoQuestions() {
		TextSwitcher questionTextSwitcher = (TextSwitcher) findViewById(R.id.TextSwitcher_QuestionText);
		questionTextSwitcher.setText(getResources().getText(
				R.string.no_questions));
		ImageSwitcher questionImageSwitcher = (ImageSwitcher) findViewById(R.id.ImageSwitcher_QuestionImage);
		questionImageSwitcher.setImageResource(R.drawable.noquestion);

		// Disable yes button
		Button yesButton = (Button) findViewById(R.id.Button_Yes);
		yesButton.setEnabled(false);

		// Disable no button
		Button noButton = (Button) findViewById(R.id.Button_No);
		noButton.setEnabled(false);
	}

	/**
	 * Returns a {@code String} representing the text for a particular question
	 * number
	 * 
	 * @param questionNumber
	 *            The question number to get the text for
	 * @return The text of the question, or null if {@code questionNumber} not
	 *         found
	 */
	private String getQuestionText(Integer questionNumber) {
		String text = null;
		Question curQuestion = (Question) mQuestions.get(questionNumber);
		if (curQuestion != null) {
			text = curQuestion.mText;
		}
		return text;
	}

	/**
	 * Returns a {@code String} representing the URL to an image for a
	 * particular question
	 * 
	 * @param questionNumber
	 *            The question to get the URL for
	 * @return A {@code String} for the URL or null if none found
	 */
	private String getQuestionImageUrl(Integer questionNumber) {
		String url = null;
		Question curQuestion = (Question) mQuestions.get(questionNumber);
		if (curQuestion != null) {
			url = curQuestion.mImageUrl;
		}
		return url;
	}

	/**
	 * Retrieves a {@code Drawable} object for a particular question
	 * 
	 * @param questionNumber
	 *            The question number to get the {@code Drawable} for
	 * @return A {@code Drawable} for the particular question, or a placeholder
	 *         image if the loading failed or the question doesn't exist
	 */
	private Drawable getQuestionImageDrawable(int questionNumber) {
		Drawable image;
		URL imageUrl;

		try {
			// Create a Drawable by decoding a stream from a remote URL
			imageUrl = new URL(getQuestionImageUrl(questionNumber));
			InputStream stream = imageUrl.openStream();
			Bitmap bitmap = BitmapFactory.decodeStream(stream);
			image = new BitmapDrawable(getResources(), bitmap);
		} catch (Exception e) {
			Log.e(DEBUG_TAG, "Decoding Bitmap stream failed");
			image = getResources().getDrawable(R.drawable.noquestion);
		}
		return image;
	}

	/**
	 * A switcher factory for use with the question image. Creates the next
	 * {@code ImageView} object to animate to
	 * 
	 */
	private class MyImageSwitcherFactory implements ViewSwitcher.ViewFactory {
		public View makeView() {
			ImageView imageView = (ImageView) LayoutInflater.from(
					getApplicationContext()).inflate(
					R.layout.image_switcher_view, mQuestionImage, false);
			return imageView;
		}
	}

	/**
	 * A switcher factory for use with the question text. Creates the next
	 * {@code TextView} object to animate to
	 * 
	 */
	private class MyTextSwitcherFactory implements ViewSwitcher.ViewFactory {
		public View makeView() {
			TextView textView = (TextView) LayoutInflater.from(
					getApplicationContext()).inflate(
					R.layout.text_switcher_view, mQuestionText, false);
			return textView;
		}
	}

	/**
	 * Object to manage the data for a single quiz question
	 * 
	 */
	private class Question {
		@SuppressWarnings("unused")
		int mNumber;
		String mText;
		String mImageUrl;

		/**
		 * 
		 * Constructs a new question object
		 * 
		 * @param questionNum
		 *            The number of this question
		 * @param questionText
		 *            The text for this question
		 * @param questionImageUrl
		 *            A valid image Url to display with this question
		 */
		public Question(int questionNum, String questionText,
				String questionImageUrl) {
			mNumber = questionNum;
			mText = questionText;
			mImageUrl = questionImageUrl;
		}
	}

	ProgressDialog pleaseWaitDialog;

	private class QuizTask extends AsyncTask<Object, String, Boolean> {
		private static final String DEBUG_TAG = "QuizGameActivity$QuizTask";

		int startingNumber;

		@Override
		protected void onCancelled() {
			Log.i(DEBUG_TAG, "onCancelled");
			handleNoQuestions();
			pleaseWaitDialog.dismiss();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (!isCancelled()) {

				Log.d(DEBUG_TAG, "Download task complete.");
				if (result) {
					displayCurrentQuestion(startingNumber);
				} else {
					handleNoQuestions();
				}

				pleaseWaitDialog.dismiss();
			} else {
				Log.d(DEBUG_TAG, "onPostExecute, but cancelled.");
			}
		}


		@Override
		protected void onPreExecute() {
			pleaseWaitDialog = ProgressDialog.show(QuizGameActivity.this,
					"Trivia Quiz", "Downloading trivia questions", true, true);
			pleaseWaitDialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Log.d(DEBUG_TAG, "onCancel -- dialog");
					QuizTask.this.cancel(true);
				}
			});
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected Boolean doInBackground(Object... params) {
			boolean result = false;
			try {
				// must put parameters in correct order and correct type,
				// otherwise a ClassCastException will be thrown
				startingNumber = (Integer) params[1];
				String pathToQuestions = params[0] + "?max="
						+ QUESTION_BATCH_SIZE + "&start=" + startingNumber;

				// update score if account is registered
                // we do this in the same request to reduce latency and increase network efficiency
                SharedPreferences settings = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);

                Integer playerId = settings.getInt(GAME_PREFERENCES_PLAYER_ID, -1);
                if (playerId != -1) {
                    Log.d(DEBUG_TAG, "Updating score");
                    Integer score = settings.getInt(GAME_PREFERENCES_SCORE, -1);
                    if (score != -1) {
                        pathToQuestions += "&updateScore=yes&updateId=" + playerId + "&score=" + score;
                    }
                }
				
				
				Log.d(DEBUG_TAG, "path: " + pathToQuestions + " -- Num: "
						+ startingNumber);

				result = loadQuestionBatch(startingNumber, pathToQuestions);

			} catch (Exception e) {
				Log.e(DEBUG_TAG,
						"Unexpected failure in XML downloading and parsing", e);
			}

			return result;
		}

		/**
		 * Parses the XML questions to {@see mQuestions}. They're preloaded into
		 * an XmlPullParser (questionBatch)
		 * 
		 * @param questionBatch
		 *            The incoming XmlPullParser
		 * @throws org.xmlpull.v1.XmlPullParserException
		 *             Thrown if XML parsing errors
		 * @throws java.io.IOException
		 *             Thrown if IO exceptions
		 */
		private void parseXMLQuestionBatch(XmlPullParser questionBatch)
				throws XmlPullParserException, IOException {
			int eventType = -1;

			// Find Score records from XML
			while (eventType != XmlResourceParser.END_DOCUMENT
					&& !isCancelled()) {
				if (eventType == XmlResourceParser.START_TAG) {

					// Get the name of the tag (eg questions or question)
					String strName = questionBatch.getName();

					if (strName.equals(XML_TAG_QUESTION)) {

						String questionNumber = questionBatch
								.getAttributeValue(null,
										XML_TAG_QUESTION_ATTRIBUTE_NUMBER);
						Integer questionNum = new Integer(questionNumber);
						String questionText = questionBatch.getAttributeValue(
								null, XML_TAG_QUESTION_ATTRIBUTE_TEXT);
						String questionImageUrl = questionBatch
								.getAttributeValue(null,
										XML_TAG_QUESTION_ATTRIBUTE_IMAGEURL);

						// Save data to our hashtable
						mQuestions.put(questionNum, new Question(questionNum,
								questionText, questionImageUrl));
					}
				}
				eventType = questionBatch.next();
			}
		}

		/**
		 * Loads the XML into the {@see mQuestions} class member variable
		 * 
		 * @param startQuestionNumber
		 *            first question to load
		 */
		private boolean loadQuestionBatch(int startQuestionNumber,
				String xmlSource) {
			boolean result = false;
			// Remove old batch
			mQuestions.clear();

			// Contact the server and retrieve a batch of question data,
			// beginning at startQuestionNumber
			XmlPullParser questionBatch;
			try {
				URL xmlUrl = new URL(xmlSource);
				questionBatch = XmlPullParserFactory.newInstance()
						.newPullParser();
				questionBatch.setInput(xmlUrl.openStream(), null);
			} catch (XmlPullParserException e1) {
				questionBatch = null;
				Log.e(DEBUG_TAG, "Failed to initialize pull parser", e1);
			} catch (IOException e) {
				questionBatch = null;
				Log.e(DEBUG_TAG,
						"IO Failure during pull parser initialization", e);
			}

			// Parse the XML
			if (questionBatch != null) {
				try {
					parseXMLQuestionBatch(questionBatch);
					result = true;
				} catch (XmlPullParserException e) {
					Log.e(DEBUG_TAG, "Pull Parser failure", e);
				} catch (IOException e) {
					Log.e(DEBUG_TAG, "IO Exception parsing XML", e);
				}
			}

			return result;

		}

	}

}