package com.skd.videoframing;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.LinearLayout;

import com.skd.videoframing.async.FramesExtractorTask;
import com.skd.videoframing.utils.FileUtils;

public class MainActivity extends ActionBarActivity {

	private static final int VIDEO_PICK_INTENT = 1;
	private static final int FRAME_PREVIEW_INTENT = 2;

	private String path;

	private LinearLayout framesBar;

	private OnFrameClickListener listener = new OnFrameClickListener() {
		@Override
		public void onFrameClicked(int time) {
			showVideoFrame(time);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		framesBar = (LinearLayout) findViewById(R.id.framesBar);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);

		MenuItem selectItem = menu.findItem(R.id.action_select);
		selectItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				chooseFile();
				return true;
			}
		});

		MenuItem settingsItem = menu.findItem(R.id.action_settings);
		settingsItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				toSettings();
				return true;
			}
		});
		return true;
	}

	/*
	 *Allows us to pick the video file that need frames to be extracted
	 */
	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
		if (arg0 == VIDEO_PICK_INTENT && arg1 == RESULT_OK) {
			Uri uri = arg2.getData();
			//System.out.println(uri.getPath());
			if (uri != null) {
				path = FileUtils.getRealPath(MainActivity.this, uri);
				Log.i("path2", path); //path of the video being imported from for video extraction
				clearVideoFrames();
				loadVideoFrames(path);

				long startTimeAES = System.currentTimeMillis();
				try {
					AESenc(path); //encrypting the video being imported in the same thread. need to make a new thread.
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				long endTimeAES = System.currentTimeMillis();
				long durationAES = (endTimeAES - startTimeAES);
				try {
					FileWriter aes = new FileWriter("/sdcard/VideoFraming/aess.text");
					aes.write(Long.toString(durationAES));
					aes.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}
		}
	}

	private void chooseFile() {
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.setType("video/*");
		startActivityForResult(i, VIDEO_PICK_INTENT);
	}

	private void loadVideoFrames(String path) {
		/*
		 * this takes the frames after the user presses the button on the frame to open a new activity to save the frames manually
		 */
		FramesExtractorTask task = new FramesExtractorTask(framesBar, listener); 
		task.execute(path);
	}

	private void AESenc(final String path){
		new Thread (new Runnable(){
			public void run(){
				try {
					AESencrypt(path);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void AESencrypt(String path) throws Exception{
		Cipher cipher = Cipher.getInstance("AES");
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey secKey = keyGen.generateKey();
		byte[] encoded = secKey.getEncoded();
		String output = new BigInteger(1, encoded).toString(16);

		FileWriter fw = new FileWriter("/sdcard/VideoFraming/1enckey.txt");
		fw.write(output);
		fw.close();

		cipher.init(Cipher.ENCRYPT_MODE, secKey);
		FileInputStream fin = new FileInputStream(path);
		FileOutputStream fos = new FileOutputStream("/sdcard/VideoFraming/EncryptedVideo.mp4");
		CipherOutputStream cos = new CipherOutputStream(fos, cipher);
		byte[] block = new byte[1024];
		int i;
		while ((i = fin.read(block)) !=-1){
			cos.write(block, 0, i);
		}
		cos.close();

		/*
		 * Decryption Part if needed
		 */
		/*		cipher.init(Cipher.DECRYPT_MODE, secKey);
		fin = new FileInputStream(VideoEnc);
		CipherInputStream cis = new CipherInputStream(fin, cipher);
		fos = new FileOutputStream(VideoDec);

		while((i = cis.read(block)) !=-1){
			fos.write(block, 0, i);
		}
		fos.close(); */
	}

	private void clearVideoFrames() {
		framesBar.removeAllViews();
	}

	/*
	 * this opens the new frame activity to select the frame
	 */
	private void showVideoFrame(int time) {
		Intent i = new Intent(MainActivity.this, FrameActivity.class);
		i.putExtra(FrameActivity.PATH_ARG, path);
		i.putExtra(FrameActivity.TIME_ARG, time);
		startActivityForResult(i, FRAME_PREVIEW_INTENT);
	}

	private void toSettings() {
		Intent i = new Intent(MainActivity.this, SettingsActivity.class);
		startActivity(i);
	}
}
