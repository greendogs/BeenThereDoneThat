package com.example.janebabra.beentheredonethat;

import java.io.IOException;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class QuizScoresActivity extends QuizActivity {
	
    int mProgressCounter = 0;
    ScoreDownloaderTask allScoresDownloader;
    ScoreDownloaderTask friendScoresDownloader;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.scores);
        
        // Set up the tabs
        TabHost host = (TabHost) findViewById(R.id.TabHost1);
        host.setup();
        
        // All Scores tab
        TabSpec allScoresTab = host.newTabSpec("allTab");
        allScoresTab.setIndicator(getResources().getString(R.string.all_scores), getResources().getDrawable(
                android.R.drawable.star_on));
        allScoresTab.setContent(R.id.ScrollViewAllScores);
        host.addTab(allScoresTab);
        
        // Friends Scores tab
        TabSpec friendScoresTab = host.newTabSpec("friendsTab");
        friendScoresTab.setIndicator(getResources().getString(R.string.friends_scores), getResources().getDrawable(
                android.R.drawable.star_on));
        friendScoresTab.setContent(R.id.ScrollViewFriendScores);
        host.addTab(friendScoresTab);
        
        // Set the default tab
        host.setCurrentTabByTag("allTab");
        
        // Retrieve the TableLayout references
        TableLayout allScoresTable = (TableLayout) findViewById(R.id.TableLayout_AllScores);
        TableLayout friendScoresTable = (TableLayout) findViewById(R.id.TableLayout_FriendScores);
        
        // Give each TableLayout a yellow header row with the column names
        initializeHeaderRow(allScoresTable);
        initializeHeaderRow(friendScoresTable);
         
        allScoresDownloader = new ScoreDownloaderTask();
        allScoresDownloader.execute(TRIVIA_SERVER_SCORES, allScoresTable);

        SharedPreferences prefs = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);
        Integer playerId = prefs.getInt(GAME_PREFERENCES_PLAYER_ID, -1);

        if (playerId != -1) {
            friendScoresDownloader = new ScoreDownloaderTask();
            friendScoresDownloader.execute(TRIVIA_SERVER_SCORES + "?playerId=" + playerId, friendScoresTable);
        }
 
    }

    @Override
    protected void onPause() {
        if (allScoresDownloader != null && allScoresDownloader.getStatus() != AsyncTask.Status.FINISHED) {
            allScoresDownloader.cancel(true);
        }
        if (friendScoresDownloader != null && friendScoresDownloader.getStatus() != AsyncTask.Status.FINISHED) {
            friendScoresDownloader.cancel(true);
        }
        super.onPause();
    }
    
    /**
     * Add a header {@code TableRow} to the {@code TableLayout} (styled)
     * 
     * @param scoreTable
     *            the {@code TableLayout} that the header row will be added to
     */
    private void initializeHeaderRow(TableLayout scoreTable) {
        // Create the Table header row
        TableRow headerRow = new TableRow(this);
        int textColor = getResources().getColor(R.color.logo_color);
        float textSize = getResources().getDimension(R.dimen.help_text_size);
        addTextToRowWithValues(headerRow, getResources().getString(R.string.username), textColor, textSize);
        addTextToRowWithValues(headerRow, getResources().getString(R.string.score), textColor, textSize);
        addTextToRowWithValues(headerRow, getResources().getString(R.string.rank), textColor, textSize);
        scoreTable.addView(headerRow);
    }

    /**
     * Churn through an XML score information and populate a {@code TableLayout}
     * 
     * @param scoreTable
     *            The {@code TableLayout} to populate
     * @param scores
     *            A standard {@code XmlResourceParser} containing the scores
     * @throws org.xmlpull.v1.XmlPullParserException
     *             Thrown on XML errors
     * @throws java.io.IOException
     *             Thrown on IO errors reading the XML
     */

    /**
     * {@code insertScoreRow()} helper method -- Populate a {@code TableRow} with
     * three columns of {@code TextView} data (styled)
     * 
     * @param tableRow
     *            The {@code TableRow} the text is being added to
     * @param text
     *            The text to add
     * @param textColor
     *            The color to make the text
     * @param textSize
     *            The size to make the text
     */
    private void addTextToRowWithValues(final TableRow tableRow, String text, int textColor, float textSize) {
        TextView textView = new TextView(this);
        textView.setTextSize(textSize);
        textView.setTextColor(textColor);
        textView.setText(text);
        tableRow.addView(textView);
    }
    
    private class ScoreDownloaderTask extends AsyncTask<Object, String, Boolean> {
        private static final String DEBUG_TAG = "ScoreDownloaderTask";
        TableLayout table;

        @Override
        protected void onCancelled() {
            Log.i(DEBUG_TAG, "onCancelled");
            mProgressCounter--;
            if (mProgressCounter <= 0) {
                mProgressCounter = 0;
                QuizScoresActivity.this.setProgressBarIndeterminateVisibility(false);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.i(DEBUG_TAG, "onPostExecute");
            mProgressCounter--;
            if (mProgressCounter <= 0) {
                mProgressCounter = 0;
                QuizScoresActivity.this.setProgressBarIndeterminateVisibility(false);
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressCounter++;
            QuizScoresActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onProgressUpdate(String... values) {

            if (values.length == 3) {
                String scoreValue = values[0];
                String scoreRank = values[1];
                String scoreUserName = values[2];
                insertScoreRow(table, scoreValue, scoreRank, scoreUserName);
            } else {
                final TableRow newRow = new TableRow(QuizScoresActivity.this);
                TextView noResults = new TextView(QuizScoresActivity.this);
                noResults.setText(getResources().getString(R.string.no_scores));
                newRow.addView(noResults);
                table.addView(newRow);
            }

        }

        @Override
        protected Boolean doInBackground(Object... params) {
            boolean result = false;
            String pathToScores = (String) params[0];
            table = (TableLayout) params[1];

            XmlPullParser scores;
            try {
                URL xmlUrl = new URL(pathToScores);
                scores = XmlPullParserFactory.newInstance().newPullParser();
                scores.setInput(xmlUrl.openStream(), null);
            } catch (XmlPullParserException e) {
                scores = null;
            } catch (IOException e) {
                scores = null;
            }

            if (scores != null) {
                try {
                    processScores(scores);
                } catch (XmlPullParserException e) {
                    Log.e(DEBUG_TAG, "Pull Parser failure", e);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "IO Exception parsing XML", e);
                }
            }

            return result;
        }

        /**
         * 
         * {@code processScores()} helper method -- Inserts a new score {@code TableRow} in the {@code TableLayout}
         * 
         * @param scoreTable
         *            The {@code TableLayout} to add the score to
         * @param scoreValue
         *            The value of the score
         * @param scoreRank
         *            The ranking of the score
         * @param scoreUserName
         *            The user who made the score
         */
        private void insertScoreRow(final TableLayout scoreTable, String scoreValue, String scoreRank, String scoreUserName) {
            final TableRow newRow = new TableRow(QuizScoresActivity.this);

            int textColor = getResources().getColor(R.color.title_color);
            float textSize = getResources().getDimension(R.dimen.help_text_size);

            addTextToRowWithValues(newRow, scoreUserName, textColor, textSize);
            addTextToRowWithValues(newRow, scoreValue, textColor, textSize);
            addTextToRowWithValues(newRow, scoreRank, textColor, textSize);
            scoreTable.addView(newRow);
        }

        /**
         * Churn through an XML score information and populate a {@code TableLayout}
         * 
         * @param scores
         *            A standard {@code XmlPullParser} containing the scores
         * @throws org.xmlpull.v1.XmlPullParserException
         *             Thrown on XML errors
         * @throws java.io.IOException
         *             Thrown on IO errors reading the XML
         */
        private void processScores(XmlPullParser scores) throws XmlPullParserException, IOException {
            int eventType = -1;
            boolean bFoundScores = false;

            // Find Score records from XML
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG) {

                    // Get the name of the tag (eg scores or score)
                    String strName = scores.getName();

                    if (strName.equals("score")) {
                        bFoundScores = true;
                        String scoreValue = scores.getAttributeValue(null, "score");
                        String scoreRank = scores.getAttributeValue(null, "rank");
                        String scoreUserName = scores.getAttributeValue(null, "username");
                        publishProgress(scoreValue, scoreRank, scoreUserName);
                    }
                }
                eventType = scores.next();
            }

            // Handle no scores available
            if (bFoundScores == false) {
                publishProgress();
            }
        }

    }
    
}