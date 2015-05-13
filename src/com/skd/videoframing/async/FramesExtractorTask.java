package com.skd.videoframing.async;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.lang.Object;
import javax.crypto.CipherOutputStream;
import wseemann.media.FFmpegMediaMetadataRetriever;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.Activity;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Bundle;
import android.view.View;

import com.skd.videoframing.Frame;
import com.skd.videoframing.OnFrameClickListener;
import com.skd.videoframing.R;
import com.skd.videoframing.utils.ImageUtils;
import com.skd.videoframing.utils.SharedPrefsUtils;

public class FramesExtractorTask extends AsyncTask<String, Float, ArrayList<Frame>> {

	int delta_time; //in microsecs

	private final WeakReference<LinearLayout> framesViewReference;
	private final WeakReference<OnFrameClickListener> listenerReference;
	private ProgressDialog progressDlg;

	public FramesExtractorTask(LinearLayout framesView, OnFrameClickListener listener) {
		this.delta_time = SharedPrefsUtils.getFramesFrequency(framesView.getContext())*1000000; //in microsecs
		System.out.println(delta_time);
		framesViewReference = new WeakReference<LinearLayout>(framesView);
		listenerReference = new WeakReference<OnFrameClickListener>(listener);
		createDialog(framesView.getContext());
	}

	@Override
	protected void onPreExecute() {
		showProgress();
	}

	@Override
	protected ArrayList<Frame> doInBackground(String... params) {

		FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
		mmr.setDataSource(params[0]);

		String s_duration = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
		int duration = getVideoDuration(s_duration);

		ArrayList<Frame> frames = new ArrayList<Frame>();
		int previous=-1;
		//	    boolean dynamicFacial=true;
		int totalFrames=0;
		/*Parameters
		 * k: sampling rate  (delta_time)
		 * alpha: increasing adjusting parameter
		 * beta: decreasing adjusting parameter
		 * x_d (y_d): threshold for decreasing the k
		 * x_u (y_u): threshold for increasing the k 
		 */
		int k = delta_time; //change to number of frames instead of time
		double alpha = 0.5;
		double beta = 2;
		double x_d = 0.2;
		double x_u = 0.8;

		/*Test strategies:
		 * 1. Default implementation, static k
		 * 2. Adjust k depends on the color difference
		 * 3. Adjust k depends on the # of faces difference
		 * 
		 */
		int strategyChosen = 1; // 1, 2 or 3
		long startTimeWhole = System.currentTimeMillis();
		for (int i=0; i<=duration; i += k) {
			Bitmap frame_orig = mmr.getFrameAtTime(i, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
			if (frame_orig == null) { 
				setProgress(i, duration);
				continue; 
			}
			Frame frame = new Frame();
			frame.setBm(ImageUtils.getScaledBitmap(frame_orig));
			frame.setTime(i);
			/*get Brightness of the picture*/
			double luminance=0;
			for (int x = 0; x < frame.getBm().getWidth(); x++) {
				for (int y = 0; y < frame.getBm().getHeight(); y++) {
					int clr = frame.getBm().getPixel(x, y);
					int red = (clr & 0x00ff0000) >> 16;
				int green = (clr & 0x0000ff00) >> 8;
		int blue = clr & 0x000000ff;
		luminance += (red * 0.2126f + green * 0.7152f + blue * 0.0722f);
				}
			}
			luminance/=(frame.getBm().getWidth()*frame.getBm().getHeight());
			frame.setBright(luminance);


			if(strategyChosen==1){		
				long startTimeDefault = System.currentTimeMillis();
				/*
				 * default program working
				 * following code to extract out the frames from the default method
				 */
				long time = System.currentTimeMillis();
				String filename = "/sdcard/VideoFraming/" + time + ".jpg";
				OutputStream stream;
				try {
					stream = new FileOutputStream(filename);
					black(frame_orig);
					frame_orig.compress(CompressFormat.JPEG, 30, stream);
					stream.close();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				long endTimeDefault = System.currentTimeMillis();
				long durationDefault = (endTimeDefault - startTimeDefault);

				try {
					FileWriter Default = new FileWriter("/sdcard/VideoFraming/Default.txt");
					Default.write(Long.toString(durationDefault));
					Default.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}else if(strategyChosen==2){
				long startTimeColor = System.currentTimeMillis();
				/*Compare the # of faces/ brightness to eliminate some figures*/
				if(previous!=-1 && Math.abs((frames.get(previous).getBright()-luminance)/luminance)<=x_d){
					k*=beta;
				}else if(previous!=-1 && Math.abs((frames.get(previous).getBright()-luminance)/luminance)>=x_u){
					k*=alpha;
					if(k==0) k=1;
				}
				long time = System.currentTimeMillis();
				String filename = "/sdcard/VideoFraming/" + time + ".jpg";
				OutputStream stream;
				try {
					stream = new FileOutputStream(filename);
					black(frame_orig);
					frame_orig.compress(CompressFormat.JPEG, 30, stream);
					stream.close();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				long endTimeColor = System.currentTimeMillis();
				long durationColor = (endTimeColor - startTimeColor);
				try {
					FileWriter color = new FileWriter("/sdcard/VideoFraming/color.text");
					color.write(Long.toString(durationColor));
					color.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}else{
				long startTimeFace = System.currentTimeMillis();
				int numberOfFaces = 5;
				int numberOfFaceDetected;
				float myEyesDistance;
				Bitmap myBitmap = convert(frame.getBm(), Bitmap.Config.RGB_565);
				FaceDetector FD = new FaceDetector(myBitmap.getWidth(),myBitmap.getHeight(), numberOfFaces);
				Face[] facesContainer = new FaceDetector.Face[numberOfFaces];
				int faces = FD.findFaces(myBitmap, facesContainer);
				frame.setFaces(faces);
				numberOfFaceDetected = FD.findFaces(myBitmap, facesContainer);

				long time = System.currentTimeMillis();

				String filename = "/sdcard/VideoFraming/" + time + ".jpg";
				OutputStream stream;

				try {
					stream = new FileOutputStream(filename);
					//BlackOut(myBitmap);
					black(myBitmap);
					
					myBitmap.compress(CompressFormat.JPEG, 30, stream);
					stream.close();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if(previous!=-1 && frames.get(previous).getFaces()!=faces){
					//only when the # of faces are different, extract the frames
					k*=alpha;
					if(k==0) k=1;
				}else if(previous!=-1 && frames.get(previous).getFaces()==faces){
					k*=beta;

				}	
				long endTimeFace = System.currentTimeMillis();
				long durationFace = (endTimeFace - startTimeFace);
				try {
					FileWriter DurFace = new FileWriter("/sdcard/VideoFraming/DurFace.text");
					DurFace.write(Long.toString(durationFace));
					DurFace.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
			previous++;
			frames.add(frame);
			setProgress(i, duration);
			totalFrames++;

			long endTimeWhole = System.currentTimeMillis();
			long durationWhole = (endTimeWhole - startTimeWhole);
			try {
				FileWriter Whole = new FileWriter("/sdcard/VideoFraming/whole.text");
				Whole.write(Long.toString(durationWhole));
				Whole.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		publishProgress((float)totalFrames);
		return frames;	
	}
	
	//new thread for blacking out instead of running it on the main thread
	private void black(final Bitmap bitmap){
		new Thread (new Runnable(){
			public void run(){
				BlackOut(bitmap);
				}
		}).start();
	} 

	private void BlackOut(Bitmap bitmap){
		int numberofFaces = 10;
		int NumberFaceDetected;
		float myEyesDistance;
		int imageWidth;
		int imageHeight;
		FaceDetector myFaceDetect;
		FaceDetector.Face[]	myFace;

		//BitmapFactory.Options BitmapFactoryOptionsbfo = new BitmapFactory.Options();
		//BitmapFactoryOptionsbfo.inPreferredConfig = Bitmap.Config.RGB_565;
		//bitmap = BitmapFactory.decodeFile(bitmapfile, BitmapFactoryOptionsbfo);
		
		Bitmap mbitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
		
		imageWidth = mbitmap.getWidth();
		imageHeight = mbitmap.getHeight();
		myFace = new FaceDetector.Face[numberofFaces];
		myFaceDetect = new FaceDetector(imageWidth, imageHeight, numberofFaces);
		NumberFaceDetected = myFaceDetect.findFaces(mbitmap, myFace);
		
		Canvas c = new Canvas(mbitmap);
		Paint myPaint = new Paint();
		myPaint.setColor(Color.GREEN);
		myPaint.setStyle(Paint.Style.FILL);
		myPaint.setStrokeWidth(3);

		for (int ii = 0; ii < NumberFaceDetected; ii++) {
			Face face = myFace[ii];
			PointF myMidPoint = new PointF();
			face.getMidPoint(myMidPoint);
			myEyesDistance = face.eyesDistance();
			c.drawRect((int) (myMidPoint.x - myEyesDistance * 2),
					(int) (myMidPoint.y - myEyesDistance * 2),
					(int) (myMidPoint.x + myEyesDistance * 2),
					(int) (myMidPoint.y + myEyesDistance * 2), myPaint);
		}
		//ouput the the picture modified
		long time = System.currentTimeMillis();
		String Filename = "/sdcard/VideoFraming/" + time + ".jpg";
		try {
			FileOutputStream stream = new FileOutputStream(Filename);
			mbitmap.compress(CompressFormat.JPEG, 50, stream);
			stream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private Bitmap convert(Bitmap bitmap, Bitmap.Config config) {
		Bitmap convertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config);
		Canvas canvas = new Canvas(convertedBitmap);
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		canvas.drawBitmap(bitmap, 0, 0, paint);
		return convertedBitmap;
	}

	private int getVideoDuration(String s_duration) {
		int duration = 0;
		try {
			duration = Integer.parseInt(s_duration); //in millisecs
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		duration *= 1000; //in microsecs
		return duration;
	}

	@Override
	protected void onPostExecute(ArrayList<Frame> result) {
		if (isCancelled() && result != null) {
			for (int i=0, len=result.size(); i<len; i++) {
				result.get(i).getBm().recycle();
			}
			result.clear();
			result = null;
		}
		if (result == null || result.size() <= 0) { 
			hideProgress();
			return; 
		}

		OnFrameClickListener listener = null;
		if (listenerReference != null) {
			listener = listenerReference.get();
		}

		if (framesViewReference != null) {
			LinearLayout framesView = framesViewReference.get();
			for (int i=0, len=result.size(); i<len; i++) {
				framesView.addView(ImageUtils.createFrameImage(framesView.getContext(),
						result.get(i).getBm(),
						result.get(i).getTime(),
						listener));
			}
		}

		hideProgress();
	}

	private void setProgress(int cur, int duration) {
		publishProgress((float)(cur + delta_time) / duration * 100);
	}

	@Override
	protected void onProgressUpdate(Float... values) {
		if (progressDlg != null) {
			progressDlg.setProgress(values[0].intValue());
		}
	}

	private Dialog createDialog(Context ctx) {
		progressDlg = new ProgressDialog(ctx);
		progressDlg.setMessage("Fetching video frames..");
		progressDlg.setMax(100);
		progressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDlg.setCancelable(false);
		progressDlg.getWindow().setGravity(Gravity.CENTER);
		return progressDlg;
	}

	private void showProgress() {
		if (progressDlg != null) {
			progressDlg.show();
		}
	}

	private void hideProgress() {
		if (progressDlg != null && progressDlg.isShowing()) {
			progressDlg.dismiss();


		}
	}

}
