package com.example.janebabra.beentheredonethat;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore.Images.Media;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

public class QuizSettingsActivity extends QuizActivity {
	SharedPreferences mGameSettings;
	GPSCoords mFavPlaceCoords;
    FriendRequestTask friendRequest;
    
	static final int DATE_DIALOG_ID = 0;
	static final int PASSWORD_DIALOG_ID = 1;
	static final int PLACE_DIALOG_ID = 2;
    static final int FRIEND_EMAIL_DIALOG_ID = 3;

	static final int TAKE_AVATAR_CAMERA_REQUEST = 1;
	static final int TAKE_AVATAR_GALLERY_REQUEST = 2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(false);

		setContentView(R.layout.settings);
		// Retrieve the shared preferences
		mGameSettings = getSharedPreferences(GAME_PREFERENCES,
				Context.MODE_PRIVATE);

		// Initialize the avatar button
		initAvatar();
		// Initialize the nickname entry
		initNicknameEntry();
		// Initialize the email entry
		initEmailEntry();
		// Initialize the Password chooser
		initPasswordChooser();
		// Initialize the Date picker
		initDatePicker();
		// Initialize the spinner
		initGenderSpinner();
		// Initialize the favorite place picker
		initFavoritePlacePicker();

	}

	@Override
	protected void onPause() {
		super.onPause();

		
        if (friendRequest != null) {
            friendRequest.cancel(true);
        }
        
		EditText nicknameText = (EditText) findViewById(R.id.EditText_Nickname);
		EditText emailText = (EditText) findViewById(R.id.EditText_Email);

		String strNickname = nicknameText.getText().toString();
		String strEmail = emailText.getText().toString();

		Editor editor = mGameSettings.edit();
		editor.putString(GAME_PREFERENCES_NICKNAME, strNickname);
		editor.putString(GAME_PREFERENCES_EMAIL, strEmail);

		editor.commit();

		Intent uploadService = new Intent(getApplicationContext(),
				UploaderService.class);
		startService(uploadService);
	}

	@Override
	protected void onDestroy() {
		Log.d(DEBUG_TAG, "SHARED PREFERENCES");
		Log.d(DEBUG_TAG,
				"Nickname is: "
						+ mGameSettings.getString(GAME_PREFERENCES_NICKNAME,
								"Not set"));
		Log.d(DEBUG_TAG,
				"Email is: "
						+ mGameSettings.getString(GAME_PREFERENCES_EMAIL,
								"Not set"));
		Log.d(DEBUG_TAG,
				"Gender (M=1, F=2, U=0) is: "
						+ mGameSettings.getInt(GAME_PREFERENCES_GENDER, 0));
		// We are not saving the password yet
		Log.d(DEBUG_TAG,
				"Password is: "
						+ mGameSettings.getString(GAME_PREFERENCES_PASSWORD,
								"Not set"));
		// We are not saving the date of birth yet
		Log.d(DEBUG_TAG,
				"DOB is: "
						+ DateFormat.format("MMMM dd, yyyy",
								mGameSettings.getLong(GAME_PREFERENCES_DOB, 0)));

		Log.d(DEBUG_TAG,
				"Avatar is: "
						+ mGameSettings.getString(GAME_PREFERENCES_AVATAR,
								"Not set"));

		Log.d(DEBUG_TAG,
				"Fav Place Name is: "
						+ mGameSettings.getString(
								GAME_PREFERENCES_FAV_PLACE_NAME, "Not set"));
		Log.d(DEBUG_TAG,
				"Fav Place GPS Lat is: "
						+ mGameSettings.getFloat(
								GAME_PREFERENCES_FAV_PLACE_LAT, 0));
		Log.d(DEBUG_TAG,
				"Fav Place GPS Lon is: "
						+ mGameSettings.getFloat(
								GAME_PREFERENCES_FAV_PLACE_LONG, 0));

		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case TAKE_AVATAR_CAMERA_REQUEST:

			if (resultCode == Activity.RESULT_CANCELED) {
				// Avatar camera mode was canceled.
			} else if (resultCode == Activity.RESULT_OK) {

				// Took a picture, use the downsized camera image provided by
				// default
				Bitmap cameraPic = (Bitmap) data.getExtras().get("data");
				if (cameraPic != null) {
					try {
						saveAvatar(cameraPic);
					} catch (Exception e) {
						Log.e(DEBUG_TAG,
								"saveAvatar() with camera image failed.", e);
					}
				}
			}
			break;
		case TAKE_AVATAR_GALLERY_REQUEST:

			if (resultCode == Activity.RESULT_CANCELED) {
				// Avatar gallery request mode was canceled.
			} else if (resultCode == Activity.RESULT_OK) {

				// Get image picked
				Uri photoUri = data.getData();
				if (photoUri != null) {
					try {
						int maxLength = 75;
						// Full size image likely will be large. Let's scale the
						// graphic to a more appropriate size for an avatar
						Bitmap galleryPic = Media.getBitmap(
								getContentResolver(), photoUri);
						Bitmap scaledGalleryPic = createScaledBitmapKeepingAspectRatio(
								galleryPic, maxLength);
						saveAvatar(scaledGalleryPic);
					} catch (Exception e) {
						Log.e(DEBUG_TAG,
								"saveAvatar() with gallery picker failed.", e);
					}
				}
			}
			break;
		}
	}

	public void onLaunchCamera(View v) {
		String strAvatarPrompt = "Take your picture to store as your avatar!";
		Intent pictureIntent = new Intent(
				android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(
				Intent.createChooser(pictureIntent, strAvatarPrompt),
				TAKE_AVATAR_CAMERA_REQUEST);
	}

	/**
	 * Scale a Bitmap, keeping its aspect ratio
	 * 
	 * @param bitmap
	 *            Bitmap to scale
	 * @param maxSide
	 *            Maximum length of either side
	 * @return a new, scaled Bitmap
	 */
	private Bitmap createScaledBitmapKeepingAspectRatio(Bitmap bitmap,
			int maxSide) {
		int orgHeight = bitmap.getHeight();
		int orgWidth = bitmap.getWidth();

		// scale to no longer any either side than 75px
		int scaledWidth = (orgWidth >= orgHeight) ? maxSide
				: (int) ((float) maxSide * ((float) orgWidth / (float) orgHeight));
		int scaledHeight = (orgHeight >= orgWidth) ? maxSide
				: (int) ((float) maxSide * ((float) orgHeight / (float) orgWidth));

		// create the scaled bitmap
		Bitmap scaledGalleryPic = Bitmap.createScaledBitmap(bitmap,
				scaledWidth, scaledHeight, true);
		return scaledGalleryPic;
	}

	private void saveAvatar(Bitmap avatar) {
		String strAvatarFilename = "avatar.jpg";
		try {
			avatar.compress(CompressFormat.JPEG, 100,
					openFileOutput(strAvatarFilename, MODE_PRIVATE));
		} catch (Exception e) {
			Log.e(DEBUG_TAG, "Avatar compression and save failed.", e);
		}

		Uri imageUriToSaveCameraImageTo = Uri.fromFile(new File(
				QuizSettingsActivity.this.getFilesDir(), strAvatarFilename));

		Editor editor = mGameSettings.edit();
		editor.putString(GAME_PREFERENCES_AVATAR,
				imageUriToSaveCameraImageTo.getPath());
		editor.commit();

		// Update the settings screen
		ImageButton avatarButton = (ImageButton) findViewById(R.id.ImageButton_Avatar);
		String strAvatarUri = mGameSettings
				.getString(GAME_PREFERENCES_AVATAR,
						"android.resource://com.example.janebabra.beentheredonethat/drawable/avatar");
		Uri imageUri = Uri.parse(strAvatarUri);
		avatarButton.setImageURI(null); // Workaround for refreshing an
										// ImageButton, which tries to cache the
										// previous image Uri. Passing null
										// effectively resets it.
		avatarButton.setImageURI(imageUri);
	}

	/**
	 * Initialize the Avatar
	 */
	private void initAvatar() {
		// Handle password setting dialog
		ImageButton avatarButton = (ImageButton) findViewById(R.id.ImageButton_Avatar);

		if (mGameSettings.contains(GAME_PREFERENCES_AVATAR)) {
			String strAvatarUri = mGameSettings
					.getString(GAME_PREFERENCES_AVATAR,
							"android.resource://com.example.janebabra.beentheredonethat/drawable/avatar");
			Uri imageUri = Uri.parse(strAvatarUri);
			avatarButton.setImageURI(imageUri);
		} else {
			avatarButton.setImageResource(R.drawable.avatar);
		}

		avatarButton.setOnLongClickListener(new View.OnLongClickListener() {

			public boolean onLongClick(View v) {
				String strAvatarPrompt = "Choose a picture to use as your avatar!";
				Intent pickPhoto = new Intent(Intent.ACTION_PICK);
				pickPhoto.setType("image/*");
				startActivityForResult(
						Intent.createChooser(pickPhoto, strAvatarPrompt),
						TAKE_AVATAR_GALLERY_REQUEST);
				return true;
			}
		});

	}

	/**
	 * Initialize the nickname entry
	 */
	private void initNicknameEntry() {
		EditText nicknameText = (EditText) findViewById(R.id.EditText_Nickname);
		if (mGameSettings.contains(GAME_PREFERENCES_NICKNAME)) {
			nicknameText.setText(mGameSettings.getString(
					GAME_PREFERENCES_NICKNAME, ""));
		}
	}

	/**
	 * Initialize the email entry
	 */
	private void initEmailEntry() {
		EditText emailText = (EditText) findViewById(R.id.EditText_Email);
		if (mGameSettings.contains(GAME_PREFERENCES_EMAIL)) {
			emailText.setText(mGameSettings.getString(GAME_PREFERENCES_EMAIL,
					""));
		}
	}

	/**
	 * Initialize the Password chooser
	 */
	private void initPasswordChooser() {
		// Set password info
		TextView passwordInfo = (TextView) findViewById(R.id.TextView_Password_Info);
		if (mGameSettings.contains(GAME_PREFERENCES_PASSWORD)) {
			passwordInfo.setText(R.string.settings_pwd_set);
		} else {
			passwordInfo.setText(R.string.settings_pwd_not_set);
		}
	}

	/**
	 * Called when the user presses the Set Password button
	 * 
	 * @param view
	 *            the button
	 */
	public void onSetPasswordButtonClick(View view) {
		showDialog(PASSWORD_DIALOG_ID);
	}
	
	/**
	 * Called when the user presses the Add Friend button
	 * 
	 * @param view
	 *            the button
	 */
	public void onAddFriendButtonClick(View view) {
		showDialog(FRIEND_EMAIL_DIALOG_ID);
	}

	/**
	 * Initialize the Date picker
	 */
	private void initDatePicker() {
		// Set password info
		TextView dobInfo = (TextView) findViewById(R.id.TextView_DOB_Info);
		if (mGameSettings.contains(GAME_PREFERENCES_DOB)) {
			dobInfo.setText(DateFormat.format("MMMM dd, yyyy",
					mGameSettings.getLong(GAME_PREFERENCES_DOB, 0)));
		} else {
			dobInfo.setText(R.string.settings_dob_not_set);
		}
	}

	/**
	 * Called when the user presses the Pick Date button
	 * 
	 * @param view
	 *            The button
	 */
	public void onPickDateButtonClick(View view) {
		showDialog(DATE_DIALOG_ID);
	}

	/**
	 * Initialize the spinner
	 */
	private void initGenderSpinner() {
		// Populate Spinner control with genders
		final Spinner spinner = (Spinner) findViewById(R.id.Spinner_Gender);
		ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(this,
				R.array.genders, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		if (mGameSettings.contains(GAME_PREFERENCES_GENDER)) {
			spinner.setSelection(mGameSettings.getInt(GAME_PREFERENCES_GENDER,
					0));
		}
		// Handle spinner selections
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent,
					View itemSelected, int selectedItemPosition, long selectedId) {
				Editor editor = mGameSettings.edit();
				editor.putInt(GAME_PREFERENCES_GENDER, selectedItemPosition);
				editor.commit();
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	/**
	 * Called when the user presses the Favorite Location button
	 * 
	 * @param view
	 *            The button
	 */
	public void onPickPlaceButtonClick(View view) {
		showDialog(PLACE_DIALOG_ID);
	}

	/**
	 * Initialize the Favorite Place picker
	 */
	private void initFavoritePlacePicker() {
		// Set place info
		TextView placeInfo = (TextView) findViewById(R.id.TextView_FavoritePlace_Info);

		if (mGameSettings.contains(GAME_PREFERENCES_FAV_PLACE_NAME)) {
			placeInfo.setText(mGameSettings.getString(
					GAME_PREFERENCES_FAV_PLACE_NAME, ""));
		} else {
			placeInfo.setText(R.string.settings_favoriteplace_not_set);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PLACE_DIALOG_ID:

			LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View dialogLayout = layoutInflater.inflate(
					R.layout.fav_place_dialog,
					(ViewGroup) findViewById(R.id.root));

			final TextView placeCoordinates = (TextView) dialogLayout
					.findViewById(R.id.TextView_FavPlaceCoords_Info);
			final EditText placeName = (EditText) dialogLayout
					.findViewById(R.id.EditText_FavPlaceName);
			placeName.setOnKeyListener(new View.OnKeyListener() {

				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if ((event.getAction() == KeyEvent.ACTION_DOWN)
							&& (keyCode == KeyEvent.KEYCODE_ENTER)) {

						String strPlaceName = placeName.getText().toString();
						if ((strPlaceName != null)
								&& (strPlaceName.length() > 0)) {
							// Try to resolve string into GPS coords
							resolveLocation(strPlaceName);

							Editor editor = mGameSettings.edit();
							editor.putString(GAME_PREFERENCES_FAV_PLACE_NAME,
									placeName.getText().toString());
							editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LONG,
									mFavPlaceCoords.mLon);
							editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LAT,
									mFavPlaceCoords.mLat);
							editor.commit();

							placeCoordinates
									.setText(formatCoordinates(
											mFavPlaceCoords.mLat,
											mFavPlaceCoords.mLon));
							return true;
						}
					}
					return false;
				}
			});

			final Button mapButton = (Button) dialogLayout
					.findViewById(R.id.Button_MapIt);
			mapButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {

					// Try to resolve string into GPS coords
					String placeToFind = placeName.getText().toString();
					resolveLocation(placeToFind);

					Editor editor = mGameSettings.edit();
					editor.putString(GAME_PREFERENCES_FAV_PLACE_NAME,
							placeToFind);
					editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LONG,
							mFavPlaceCoords.mLon);
					editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LAT,
							mFavPlaceCoords.mLat);
					editor.commit();

					placeCoordinates.setText(formatCoordinates(
							mFavPlaceCoords.mLat, mFavPlaceCoords.mLon));

					// Launch map with gps coords
					String geoURI = String.format("geo:%f,%f?z=10",
							mFavPlaceCoords.mLat, mFavPlaceCoords.mLon);
					Uri geo = Uri.parse(geoURI);
					Intent geoMap = new Intent(Intent.ACTION_VIEW, geo);
					startActivity(geoMap);
				}
			});

			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
			dialogBuilder.setView(dialogLayout);

			// Now configure the AlertDialog
			dialogBuilder.setTitle(R.string.settings_button_favoriteplace);

			dialogBuilder.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// We forcefully dismiss and remove the Dialog, so
							// it cannot be used again (no cached info)
							QuizSettingsActivity.this
									.removeDialog(PLACE_DIALOG_ID);
						}
					});

			dialogBuilder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							TextView placeInfo = (TextView) findViewById(R.id.TextView_FavoritePlace_Info);
							String strPlaceName = placeName.getText()
									.toString();

							if ((strPlaceName != null)
									&& (strPlaceName.length() > 0)) {
								Editor editor = mGameSettings.edit();
								editor.putString(
										GAME_PREFERENCES_FAV_PLACE_NAME,
										strPlaceName);
								editor.putFloat(
										GAME_PREFERENCES_FAV_PLACE_LONG,
										mFavPlaceCoords.mLon);
								editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LAT,
										mFavPlaceCoords.mLat);
								editor.commit();

								placeInfo.setText(strPlaceName);
							}

							// We forcefully dismiss and remove the Dialog, so
							// it cannot be used again
							QuizSettingsActivity.this
									.removeDialog(PLACE_DIALOG_ID);
						}
					});

			// Create the AlertDialog and return it
			AlertDialog placeDialog = dialogBuilder.create();
			return placeDialog;

		case DATE_DIALOG_ID:
			final TextView dob = (TextView) findViewById(R.id.TextView_DOB_Info);
			Calendar now = Calendar.getInstance();

			DatePickerDialog dateDialog = new DatePickerDialog(this,
					new DatePickerDialog.OnDateSetListener() {
						public void onDateSet(DatePicker view, int year,
								int monthOfYear, int dayOfMonth) {

							Time dateOfBirth = new Time();
							dateOfBirth.set(dayOfMonth, monthOfYear, year);
							long dtDob = dateOfBirth.toMillis(true);
							dob.setText(DateFormat.format("MMMM dd, yyyy",
									dtDob));

							Editor editor = mGameSettings.edit();
							editor.putLong(GAME_PREFERENCES_DOB, dtDob);
							editor.commit();
						}
					}, now.get(Calendar.YEAR), now.get(Calendar.MONTH),
					now.get(Calendar.DAY_OF_MONTH));
			return dateDialog;
		case PASSWORD_DIALOG_ID:
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View layout = inflater.inflate(R.layout.password_dialog,
					(ViewGroup) findViewById(R.id.root));
			final EditText p1 = (EditText) layout
					.findViewById(R.id.EditText_Pwd1);
			final EditText p2 = (EditText) layout
					.findViewById(R.id.EditText_Pwd2);
			final TextView error = (TextView) layout
					.findViewById(R.id.TextView_PwdProblem);
			p2.addTextChangedListener(new TextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					String strPass1 = p1.getText().toString();
					String strPass2 = p2.getText().toString();
					if (strPass1.equals(strPass2)) {
						error.setText(R.string.settings_pwd_equal);
					} else {
						error.setText(R.string.settings_pwd_not_equal);
					}
				}

				// ... other required overrides need not be implemented
				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
				}
			});
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView(layout);
			// Now configure the AlertDialog
			builder.setTitle(R.string.settings_button_pwd);
			builder.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// We forcefully dismiss and remove the Dialog, so
							// it
							// cannot be used again (no cached info)
							QuizSettingsActivity.this
									.removeDialog(PASSWORD_DIALOG_ID);
						}
					});
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							TextView passwordInfo = (TextView) findViewById(R.id.TextView_Password_Info);
							String strPassword1 = p1.getText().toString();
							String strPassword2 = p2.getText().toString();
							if (strPassword1.equals(strPassword2)) {
								Editor editor = mGameSettings.edit();
								editor.putString(GAME_PREFERENCES_PASSWORD,
										strPassword1);
								editor.commit();
								passwordInfo.setText(R.string.settings_pwd_set);
							} else {
								Log.d(DEBUG_TAG,
										"Passwords do not match. Not saving. Keeping old password (if set).");
							}
							// We forcefully dismiss and remove the Dialog, so
							// it
							// cannot be used again
							QuizSettingsActivity.this
									.removeDialog(PASSWORD_DIALOG_ID);
						}
					});
			// Create the AlertDialog and return it
			AlertDialog passwordDialog = builder.create();
			return passwordDialog;
			
		 case FRIEND_EMAIL_DIALOG_ID:
			    LayoutInflater infl = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            final View friendDialogLayout = infl.inflate(R.layout.friend_dialog, (ViewGroup) findViewById(R.id.root));

	            AlertDialog.Builder friendDialogBuilder = new AlertDialog.Builder(this);
	            friendDialogBuilder.setView(friendDialogLayout);
	            final TextView emailText = (TextView) friendDialogLayout.findViewById(R.id.EditText_Friend_Email);

	            friendDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

	                public void onClick(DialogInterface dialog, int which) {

	                    String friendEmail = emailText.getText().toString();
	                    if (friendEmail != null && friendEmail.length() > 0) {
	                        doFriendRequest(friendEmail);
	                    }
	                }
	            });
	            return friendDialogBuilder.create();
		}
		
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		switch (id) {
		case PLACE_DIALOG_ID:

			// Handle any Favorite Place Dialog initialization here
			AlertDialog placeDialog = (AlertDialog) dialog;

			String strFavPlaceName;

			// Check for favorite place preference
			if (mGameSettings.contains(GAME_PREFERENCES_FAV_PLACE_NAME)) {

				// Retrieve favorite place from preferences
				strFavPlaceName = mGameSettings.getString(
						GAME_PREFERENCES_FAV_PLACE_NAME, "");
				mFavPlaceCoords = new GPSCoords(mGameSettings.getFloat(
						GAME_PREFERENCES_FAV_PLACE_LAT, 0),
						mGameSettings.getFloat(GAME_PREFERENCES_FAV_PLACE_LONG,
								0));

			} else {

				// No favorite place set, set coords to current location
				strFavPlaceName = getResources().getString(
						R.string.settings_favplace_currentlocation); // We do
																		// not
																		// name
																		// this
																		// place
																		// ("here"),
																		// but
																		// use
																		// it as
																		// a map
																		// point.
																		// User
																		// can
																		// supply
																		// the
																		// name
																		// they
																		// like
				calculateCurrentCoordinates();

			}

			// Set the placename text and coordinates either to the saved
			// values, or just set the GPS coords to the current location
			final EditText placeName = (EditText) placeDialog
					.findViewById(R.id.EditText_FavPlaceName);
			placeName.setText(strFavPlaceName);

			final TextView placeCoordinates = (TextView) placeDialog
					.findViewById(R.id.TextView_FavPlaceCoords_Info);
			placeCoordinates.setText(formatCoordinates(mFavPlaceCoords.mLat,
					mFavPlaceCoords.mLon));

			return;

		case DATE_DIALOG_ID:
			// Handle any DatePickerDialog initialization here
			DatePickerDialog dateDialog = (DatePickerDialog) dialog;
			int iDay,
			iMonth,
			iYear;
			// Check for date of birth preference
			if (mGameSettings.contains(GAME_PREFERENCES_DOB)) {
				// Retrieve Birth date setting from preferences
				long msBirthDate = mGameSettings.getLong(GAME_PREFERENCES_DOB,
						0);
				Time dateOfBirth = new Time();
				dateOfBirth.set(msBirthDate);

				iDay = dateOfBirth.monthDay;
				iMonth = dateOfBirth.month;
				iYear = dateOfBirth.year;
			} else {
				Calendar cal = Calendar.getInstance();
				// Today's date fields
				iDay = cal.get(Calendar.DAY_OF_MONTH);
				iMonth = cal.get(Calendar.MONTH);
				iYear = cal.get(Calendar.YEAR);
			}
			// Set the date in the DatePicker to the date of birth OR to the
			// current date
			dateDialog.updateDate(iYear, iMonth, iDay);
			return;
		case PASSWORD_DIALOG_ID:
			// Handle any Password Dialog initialization here
			// Since we don't want to show old password dialogs, just set new
			// ones, we need not do anything here
			// Because we are not "reusing" password dialogs once they have
			// finished, but removing them from
			// the Activity Dialog pool explicitly with removeDialog() and
			// recreating them as needed.
			return;
		 case FRIEND_EMAIL_DIALOG_ID:
			 return;
		}
	}

	/**
	 * Helper to format coordinates for screen display
	 * 
	 * @param lat
	 * @param lon
	 * @return A string formatted accordingly
	 */
	private String formatCoordinates(float lat, float lon) {
		StringBuilder strCoords = new StringBuilder();
		strCoords.append(lat).append(",").append(lon);
		return strCoords.toString();
	}

	/**
	 * 
	 * If location name can't be determined, try to determine location based on
	 * current coords
	 * 
	 * @param strLocation
	 *            Location or place name to try
	 */
	private void resolveLocation(String strLocation) {
		boolean bResolvedAddress = false;

		if (strLocation.equalsIgnoreCase(getResources().getString(
				R.string.settings_favplace_currentlocation)) == false) {
			bResolvedAddress = lookupLocationByName(strLocation);
		}

		if (bResolvedAddress == false) {
			// If String place name could not be determined (or matches the
			// string for "current location", assume this is a custom name of
			// the current location
			calculateCurrentCoordinates();
		}
	}

	/**
	 * Attempt to get the last known location of the device. Usually this is the
	 * last value that a location provider set
	 */
	private void calculateCurrentCoordinates() {
		float lat = 0, lon = 0;

		try {
			LocationManager locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
			Location recentLoc = locMgr
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			lat = (float) recentLoc.getLatitude();
			lon = (float) recentLoc.getLongitude();
		} catch (Exception e) {
			Log.e(DEBUG_TAG, "Location failed", e);
		}

		mFavPlaceCoords = new GPSCoords(lat, lon);
	}

	/**
	 * 
	 * Take a description of a location, store the coordinates in
	 * mFavPlaceCoords
	 * 
	 * @param strLocation
	 *            The location or placename to look up
	 * @return true if the address or place was recognized, otherwise false
	 */
	private boolean lookupLocationByName(String strLocation) {
		final Geocoder coder = new Geocoder(getApplicationContext());
		boolean bResolvedAddress = false;

		try {

			List<Address> geocodeResults = coder.getFromLocationName(
					strLocation, 1);
			Iterator<Address> locations = geocodeResults.iterator();

			while (locations.hasNext()) {
				Address loc = locations.next();
				mFavPlaceCoords = new GPSCoords((float) loc.getLatitude(),
						(float) loc.getLongitude());
				bResolvedAddress = true;
			}
		} catch (Exception e) {
			Log.e(DEBUG_TAG, "Failed to geocode location", e);
		}
		return bResolvedAddress;
	}

	private class GPSCoords {
		float mLat, mLon;

		GPSCoords(float lat, float lon) {
			mLat = lat;
			mLon = lon;

		}
	}

	public static class UploaderService extends Service {
		private static final String DEBUG_TAG = "QuizSettingsActivity$UploaderService";

		private UploadTask uploader;

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			uploader = new UploadTask();
			uploader.execute();
			Log.d(DEBUG_TAG, "Settings and image upload requested");
			return START_REDELIVER_INTENT;
		}

		@Override
		public IBinder onBind(Intent intent) {
			// no binding
			return null;
		}

		private class UploadTask extends AsyncTask<Object, String, Boolean> {
			SharedPreferences mGameSettings;

			@Override
			protected void onPostExecute(Boolean result) {
				// perhaps notify the user?
			}

			@Override
			protected void onPreExecute() {
				mGameSettings = getSharedPreferences(GAME_PREFERENCES,
						Context.MODE_PRIVATE);
			}

			@Override
			protected Boolean doInBackground(Object... params) {
				boolean result = postSettingsToServer();
				if (result && !isCancelled()) {
					result = postAvatarToServer();
				}
				Log.d(DEBUG_TAG, "Done uploading settings and image");
				return result;
			}

			private boolean postSettingsToServer() {
				boolean succeeded = false;

				// an example of using HttpClient with HTTP GET and form
				// variables

				Integer playerId = mGameSettings.getInt(
						GAME_PREFERENCES_PLAYER_ID, -1);
				String nickname = mGameSettings.getString(
						GAME_PREFERENCES_NICKNAME, "");
				String email = mGameSettings.getString(GAME_PREFERENCES_EMAIL,
						"");
				String password = mGameSettings.getString(
						GAME_PREFERENCES_PASSWORD, "");
				Integer score = mGameSettings
						.getInt(GAME_PREFERENCES_SCORE, -1);
				Integer gender = mGameSettings.getInt(GAME_PREFERENCES_GENDER,
						-1);
				Long birthdate = mGameSettings.getLong(GAME_PREFERENCES_DOB, 0);
				String favePlaceName = mGameSettings.getString(
						GAME_PREFERENCES_FAV_PLACE_NAME, "");

				Vector<NameValuePair> vars = new Vector<NameValuePair>();

				if (playerId == -1) {
					// if we don't have a playerId yet, we must pass up a
					// uniqueId
					// a good enough way to generate one is using the UUID
					// class:
					String uniqueId = UUID.randomUUID().toString();
					Log.d(DEBUG_TAG, "Unique ID: " + uniqueId);

					// if you want the user to be able to restore data by using
					// this, you could email it to them

					// why not use getDeviceId from TelephonyManager?
					// See: http://goo.gl/sAbV2
					// In short, it only works on phones. Got a wifi only
					// tablet? A TV? forget it.

					vars.add(new BasicNameValuePair("uniqueId", uniqueId));

				} else {
					// otherwise, we use the playerId to update data
					vars.add(new BasicNameValuePair("updateId", playerId
							.toString()));
					// and we go ahead and push up the latest score
					vars.add(new BasicNameValuePair("score", score.toString()));
				}
				vars.add(new BasicNameValuePair("nickname", nickname));
				vars.add(new BasicNameValuePair("email", email));
				vars.add(new BasicNameValuePair("password", password));
				vars.add(new BasicNameValuePair("gender", gender.toString()));
				vars.add(new BasicNameValuePair("faveplace", favePlaceName));
				vars.add(new BasicNameValuePair("dob", birthdate.toString()));

				String url = TRIVIA_SERVER_ACCOUNT_EDIT + "?"
						+ URLEncodedUtils.format(vars, null);

				HttpGet request = new HttpGet(url);

				try {

					ResponseHandler<String> responseHandler = new BasicResponseHandler();
					HttpClient client = new DefaultHttpClient();
					String responseBody = client.execute(request,
							responseHandler);

					if (responseBody != null && responseBody.length() > 0) {
						Integer resultId = Integer.parseInt(responseBody);
						Editor editor = mGameSettings.edit();
						editor.putInt(GAME_PREFERENCES_PLAYER_ID, resultId);
						editor.commit();
					}
					succeeded = true;

				} catch (ClientProtocolException e) {
					Log.e(DEBUG_TAG, "Failed to get playerId (protocol): ", e);
				} catch (IOException e) {
					Log.e(DEBUG_TAG, "Failed to get playerId (io): ", e);
				}
				return succeeded;
			}

			private boolean postAvatarToServer() {
				boolean succeeded = false;
				// an example using HttpClient and HttpMime to upload a file via
				// HTTP POST in the same
				// way a web browser might, using multipart MIME encoding
				String avatar = mGameSettings.getString(
						GAME_PREFERENCES_AVATAR, "");
				Integer playerId = mGameSettings.getInt(
						GAME_PREFERENCES_PLAYER_ID, -1);

				MultipartEntity entity = new MultipartEntity(
						HttpMultipartMode.BROWSER_COMPATIBLE);
				File file = new File(avatar);
				if (file.exists()) {
					FileBody encFile = new FileBody(file);

					entity.addPart("avatar", encFile);
					try {
						entity.addPart("updateId",
								new StringBody(playerId.toString()));
					} catch (UnsupportedEncodingException e) {
						Log.e(DEBUG_TAG, "Failed to add form field.", e);
					}

					HttpPost request = new HttpPost(TRIVIA_SERVER_ACCOUNT_EDIT);
					request.setEntity(entity);

					HttpClient client = new DefaultHttpClient();

					try {
						ResponseHandler<String> responseHandler = new BasicResponseHandler();
						String responseBody = client.execute(request,
								responseHandler);

						if (responseBody != null && responseBody.length() > 0) {
							Log.w(DEBUG_TAG,
									"Unexpected response from avatar upload: "
											+ responseBody);
						}
						succeeded = true;

					} catch (ClientProtocolException e) {
						Log.e(DEBUG_TAG, "Unexpected ClientProtocolException",
								e);
					} catch (IOException e) {
						Log.e(DEBUG_TAG, "Unexpected IOException", e);
					}
				} else {
					Log.d(DEBUG_TAG, "No avatar to upload");
					succeeded = true;
				}

				return succeeded;

			}

		}

	}


    private void doFriendRequest(String friendEmail) {
        // make sure we don't collide with another pending update
        if (friendRequest == null || friendRequest.getStatus() == AsyncTask.Status.FINISHED || friendRequest.isCancelled()) {
            friendRequest = new FriendRequestTask();
            friendRequest.execute(friendEmail);
        } else {
            Log.w(DEBUG_TAG, "Warning: friendRequestTask already going");
        }
    }
    
    private class FriendRequestTask extends AsyncTask<String, Object, Boolean> {
        @Override
        protected void onPostExecute(Boolean result) {
            QuizSettingsActivity.this.setProgressBarIndeterminateVisibility(false);
        }

        @Override
        protected void onPreExecute() {
            QuizSettingsActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Boolean succeeded = false;
            try {
                String friendEmail = params[0];

                SharedPreferences prefs = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);
                Integer playerId = prefs.getInt(GAME_PREFERENCES_PLAYER_ID, -1);

                Vector<NameValuePair> vars = new Vector<NameValuePair>();
                vars.add(new BasicNameValuePair("command", "add"));
                vars.add(new BasicNameValuePair("playerId", playerId.toString()));
                vars.add(new BasicNameValuePair("friend", friendEmail));

                HttpClient client = new DefaultHttpClient();

                // an example of using HttpClient with HTTP POST and form variables
                HttpPost request = new HttpPost(TRIVIA_SERVER_FRIEND_ADD);
                request.setEntity(new UrlEncodedFormEntity(vars));

                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = client.execute(request, responseHandler);

                Log.d(DEBUG_TAG, "Add friend result: " + responseBody);
                if (responseBody != null) {
                    succeeded = true;
                }

            } catch (MalformedURLException e) {
                Log.e(DEBUG_TAG, "Failed to add friend", e);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Failed to add friend", e);
            }

            return succeeded;
        }

    }
}