package com.skd.videoframing;

import android.graphics.Bitmap;

public class Frame {
	
	private Bitmap bm;
	private int time;
	private int faces;
	private double bright;
	
	public Bitmap getBm() {
		return bm;
	}
	public void setBm(Bitmap bm) {
		this.bm = bm;
	}
	public void setFaces(int faces){
		this.faces=faces;
	}
	public int getFaces(){
		return faces;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public void setBright(double bright){
		this.bright=bright;
	}
	public double getBright(){
		return bright;
	}
	
}
