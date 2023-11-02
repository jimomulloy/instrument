package comul01.eve;

import java.awt.*;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.Format;
import javax.media.format.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.media.util.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

/*
* Class EFrameSet
*/
public class EFrameSet {
	
   	EFrame[] eFrameArray = null;
  	int head ;
	int tail ;
	int size ;
	int count ;
	int now ;

    public EFrameSet() {
		super();
	} 
	
	public EFrameSet(int size) {
		super();
		this.size = size; 
		eFrameArray = new EFrame[size];
		head = -1;
		tail = -1;
		now = 0;
		
	} 
	
	public void put(EFrame eFrame){
		
		if (head == (size-1)) {
			head = 0;
		} else {
			head++;
		}
		
		if (head == tail) {
			if (tail == (size-1)) {
				tail = 0;
			} else {
				tail++;
			}
		}
		
		if (tail == -1) tail = 0;
		eFrameArray[head] = eFrame;
	}
	 
	public EFrame get(){
		return eFrameArray[head];
	}
	
	public int getSize(){
		return size;
	}
	
	
	public EFrame get(int offset) {
		if (head == -1) return null;
		if (offset >= getCount()) return null;
		if (head >= offset) return eFrameArray[head-offset];
		return eFrameArray[size+(head-offset)];
	}
	
	public int getCount() {
		if (head >= tail) return (head - tail + 1);
		return ((tail-head-1)+size);
	}
	
	public void clear () {
		eFrameArray = new EFrame[size];
		head = -1;
		tail = -1;
		now = 0;
	}
	
	public void copy(EFrameSet eFrameSet) {
		EFrame oldFrame = null;
		EFrame newFrame = null;
		this.size = eFrameSet.getSize();
		clear();
		int count = eFrameSet.getCount();
		for (int i = 0; i < count; i++){
			oldFrame = eFrameSet.get(i);
			if (oldFrame == null) break;
			newFrame = oldFrame.copy();
			put(newFrame);
		}
	}
		
}


