package tk.eatheat.demo.widget;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import tk.eatheat.demo.widget.R;

public class MainActivity extends Activity implements OnClickListener {

	private static final String LOG_TAG = "MainActivity";
	private static final int APPWIDGET_HOST_ID = 2048;
	private static final int REQUEST_WIDGET = 0;
	private static final int REQUEST_CONFIGURE = 1;

	private AppWidgetHost mAppWidgetHost = null;
	private FrameLayout mAppWidgetFrame = null;
	private AppWidgetHostView mAppWidgetView = null;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private String mAppWidgetName;
	private int mPreviewWidth;
	private int mPreviewHeight;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mAppWidgetFrame = (FrameLayout) findViewById(R.id.widgets_here);

		mAppWidgetHost = new AppWidgetHost(getApplicationContext(),
				APPWIDGET_HOST_ID);

		Button button = (Button) findViewById(R.id.add);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				chooseWidgets();

			}

		});
	}

	@Override
	public void onStart() {
		super.onStart();
		mAppWidgetHost.startListening();
	}

	private void chooseWidgets() {
		int id = mAppWidgetHost.allocateAppWidgetId();
		Intent selectIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
		selectIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
		startActivityForResult(selectIntent, REQUEST_WIDGET);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_WIDGET) {
			if (data != null) {
				int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
				if (data.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
					appWidgetId = data.getExtras().getInt(
							AppWidgetManager.EXTRA_APPWIDGET_ID);
				}

				if (resultCode == RESULT_OK) {
					setAppWidget(appWidgetId);
				} else {
					mAppWidgetHost.deleteAppWidgetId(appWidgetId);
					finish();
				}
			} else {
				finish();
			}
		} else if (requestCode == REQUEST_CONFIGURE) {
			if (data != null) {
				int appWidgetId = data.getExtras().getInt(
						AppWidgetManager.EXTRA_APPWIDGET_ID,
						AppWidgetManager.INVALID_APPWIDGET_ID);
				if (resultCode == RESULT_OK) {
					finishSetAppWidget(appWidgetId);
				} else {
					mAppWidgetHost.deleteAppWidgetId(appWidgetId);
				}
			}
		}
	}

	private void setAppWidget(int appWidgetId) {
		if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			mAppWidgetHost.deleteAppWidgetId(mAppWidgetId);
		}

		/* Check for configuration */
		AppWidgetProviderInfo providerInfo = AppWidgetManager.getInstance(
				getBaseContext()).getAppWidgetInfo(appWidgetId);

		if (providerInfo.configure != null) {
			Intent configureIntent = new Intent(
					AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			configureIntent.setComponent(providerInfo.configure);
			configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetId);

			if (configureIntent != null) {
				try {
					startActivityForResult(configureIntent, REQUEST_CONFIGURE);
				} catch (ActivityNotFoundException e) {
					Log.d(LOG_TAG, "Configuration activity not found: " + e);
					Toast errorToast = Toast.makeText(getBaseContext(),
							R.string.configure_error, Toast.LENGTH_SHORT);
					errorToast.show();
				}
			}
		} else {
			finishSetAppWidget(appWidgetId);
		}
	}

	private void finishSetAppWidget(int appWidgetId) {
		AppWidgetProviderInfo providerInfo = AppWidgetManager.getInstance(
				getBaseContext()).getAppWidgetInfo(appWidgetId);
		if (providerInfo != null) {
			mAppWidgetView = mAppWidgetHost.createView(getBaseContext(),
					appWidgetId, providerInfo);

			int[] dimensions = getLauncherCellDimensions(providerInfo.minWidth,
					providerInfo.minHeight);

			mPreviewWidth = dimensions[0];
			mPreviewHeight = dimensions[1];

			mAppWidgetName = AppWidgetManager.getInstance(getBaseContext())
					.getAppWidgetInfo(appWidgetId).label;
			mAppWidgetName = mAppWidgetName.replaceAll("[^a-zA-Z0-9]", "_");

			ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
			mAppWidgetView.setLayoutParams(p);
			mAppWidgetFrame.removeAllViews();
			mAppWidgetHost.deleteAppWidgetId(mAppWidgetId);
			mAppWidgetFrame.addView(mAppWidgetView, mPreviewWidth,
					mPreviewHeight);
			mAppWidgetId = appWidgetId;
		}
	}

	public int[] getLauncherCellDimensions(int width, int height) {

		Resources resources = getResources();
		int cellWidth = resources
				.getDimensionPixelSize(R.dimen.workspace_cell_width);
		int cellHeight = resources
				.getDimensionPixelSize(R.dimen.workspace_cell_height);
		int widthGap = resources
				.getDimensionPixelSize(R.dimen.workspace_width_gap);
		int heightGap = resources
				.getDimensionPixelSize(R.dimen.workspace_height_gap);
		int previewCellSize = resources
				.getDimensionPixelSize(R.dimen.preview_cell_size);

		int smallerSize = Math.min(cellWidth, cellHeight);
		int spanX = (width + smallerSize) / smallerSize;
		int spanY = (height + smallerSize) / smallerSize;

		width = spanX * previewCellSize + ((spanX - 1) * widthGap);
		height = spanY * previewCellSize + ((spanY - 1) * heightGap);
		return new int[] { width, height };
	}

	public Bitmap getPreviewBitmap() {
		mAppWidgetView.invalidate();
		Bitmap bmp = Bitmap.createBitmap(mAppWidgetView.getWidth(),
				mAppWidgetView.getHeight(), Config.ARGB_8888);
		Canvas c = new Canvas(bmp);
		mAppWidgetView.draw(c);
		return bmp;
	}

	@Override
	public void onClick(View v) {

	}
}
