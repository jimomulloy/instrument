package comul01.eve;

/**
 * This class encapsulates the Status and Error message code information 
 * as used on the ToneMapFrame Status panel.
 * Implementations of this class are collected in the ToneMapStatus class
 * for look up access 
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public class StatusInfo implements Comparable, EJConstants  {
 
	int sc;
   	int type;
   	String message;
   	
   
   	public StatusInfo() {
	  	super();
   	}                        
   
   	public StatusInfo(int sc, int type, String message) {
	  	super();
	  	this.sc = sc;
	  	this.type = type;
	  	this.message = message;
	}   

   	/**
   	* Compares this object with the specified object for order.
   	*
   	* @param   o the Object to be compared.
   	* @return  a negative integer, zero, or a positive integer as this object
   	*		is less than, equal to, or greater than the specified object.
   	* 
   	* @throws ClassCastException if the specified object's type prevents it
   	*         from being compared to this Object.
   	*/
   	public int compareTo(Object o) {
	  	StatusInfo e = (StatusInfo) o;

	  	if (sc > e.sc)
		 	return 1;

	  	if (sc < e.sc)
		 	return -1;

	  	return 0;
   	}                        

	/**
	 * A simpler form of constructor for the error code search.
	*/
	public StatusInfo(int sc) {
	  super();
	  this.sc = sc;
	  this.type = 0;
	  this.message = "";
	}

	/**
	 * Returns a String that represents the value of this object.
	 */
	public String toString() {
		String typeString;
		if (type == ST_INFO) typeString = "Status Code: ";
		else typeString = "Error Code: ";
		return typeString + sc + ", " + message;
	}
}