package comul01.eve;

import java.awt.*;
import javax.media.*;

public interface EJFrameGrabber {

	public Image grabImage(long time);

	public Image grabFrame(int frame);

	public Time getBeginTime();
	
	public Time getEndTime();

	public int getBeginFrame();
	
	public int getEndFrame();
	
	public boolean testMediaFile();


}