package comul01.eve;

import java.io.*;
import java.util.*;
import java.net.*;
	
	

	public class EffectContext implements Serializable, Comparable  {
	
		public int seq;
		public int type;
		public int index;
		public String name;
		public boolean enabled;
		public boolean compo;
		public int mode;
		public String convolve;
		public int count;
		public int depth;
		public int step;
		public boolean option1;
		public boolean option2;
		public boolean option3;
		public boolean option4;
		public boolean option5;
		public boolean option6;
		public int seIndex;
		public int sqIndex;
		public int coIndex;
		public int ctIndex;
		public int deIndex;
		public int stIndex;
		public ArrayList params;
		
	public int compareTo(Object object) {
	
		if (((EffectContext)object).seq > this.seq) return 1;
		else return -1;
	
	}

	}


	