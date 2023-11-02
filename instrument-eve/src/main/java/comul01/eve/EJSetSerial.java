package comul01.eve;

import java.io.*;

/**
  * This class encapsulates EJSettings objects for Saving in serialized form
  * in to a file
  *
  * @version 1.0 01/01/01
  * @author Jim O'Mulloy
  */
public class EJSetSerial implements Serializable {
	
	public EJSetSerial(EJSettings ejSettings) {
		this.ejSettings = ejSettings;
	}
		
	EJSettings ejSettings;
			
}