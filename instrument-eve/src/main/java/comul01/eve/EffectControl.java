 /*
 * EffectControl.java	
*/
package comul01.eve;


import java.awt.Dimension;

import javax.media.Buffer;
import javax.media.Control;
import javax.media.Effect;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;




    public class EffectControl implements Effect {
	
    	private EJMain ejMain = null;
		private EJSettings ejSettings = null;
		private EJEffects ejEffects = null;
		
		public int seqNumber = 0;   
		
		/** The effect name **/
        private String effectName="Effect7";

        /** chosen input Format **/
        protected VideoFormat inputFormat;

        /** chosen output Format **/
        protected VideoFormat outputFormat;

        /** supported input Formats **/
        protected Format[] supportedInputFormats=new Format[0];

        /** supported output Formats **/
        protected Format[] supportedOutputFormats=new Format[0];

        /** selected Parameter1 **/
        protected float parameter1 = 1.2F;


        /** 
         * initialize the formats 
        */
        public EffectControl(EJMain ejMain) {
		
		  this.ejMain = ejMain;
		  ejSettings = ejMain.getEJSettings();
		  ejEffects = new EJEffects(ejMain);
		  
	      supportedInputFormats = new Format [] {
		   new RGBFormat(null,
                              Format.NOT_SPECIFIED,
                              Format.byteArray,
                              Format.NOT_SPECIFIED,
                              24,
                              3, 2, 1,
                              3, Format.NOT_SPECIFIED,
                              Format.TRUE,
                              Format.NOT_SPECIFIED)
	    };
              supportedOutputFormats = new Format [] {
		    new RGBFormat(null,
                              Format.NOT_SPECIFIED,
                              Format.byteArray,
                              Format.NOT_SPECIFIED,
                              24,
                              3, 2, 1,
                              3, Format.NOT_SPECIFIED,
                              Format.TRUE,
                              Format.NOT_SPECIFIED)
              };
  	    }


        /** 
         * get the resources needed by this effect 
        */
        public void open() throws ResourceUnavailableException {
        }


        /** 
         * free the resources allocated by this codec 
        */
        public void close() {
        }


        /** 
         * reset the codec 
        */
        public void reset() {
       }


        /**
         * no controls for this simple effect 
        */
        public Object[] getControls() {
            return (Object[]) new Control[0];
        }


        /**
         * Return the control based on a control type for the effect.
        */
        public Object getControl(String controlType) {
            try {
                Class cls = Class.forName(controlType);
                Object cs[] = getControls();
                for (int i = 0; i < cs.length; i++) {
                    if (cls.isInstance(cs[i]))
                    return cs[i];
                }
                return null;
            } catch (Exception e) { // no such controlType or such control
                return null;
            }
        }

        /************** format methods *************/

        /** 
         * set the input format 
         */
        public Format setInputFormat(Format input) {
            // the following code assumes valid Format
            inputFormat = (VideoFormat)input;
            return (Format)inputFormat;
        }

		/*
		  public Format setOutputFormat(Format output) {
            // the following code assumes valid Format
            outputFormat = (VideoFormat)output;
            return (Format)outputFormat;
        }

		*/
		
        /** 
         * get the input format 
         */
        protected Format getInputFormat() {
            return inputFormat;
        }


        /** 
         * get the output format 
         */
        protected Format getOutputFormat() {
            return outputFormat;
        }


        /** 
         * supported input formats 
         */
        public Format [] getSupportedInputFormats() {
            return supportedInputFormats;
        }
	
        public Format [] getSupportedOutputFormats(Format in) {
	      if (in == null)
	          return supportedOutputFormats;
	      else {
		    // If an input format is given, we use that input format
		    // as the output since we are not modifying the bit stream
		    // at all.
		    Format outs[] = new Format[1];
		    outs[0] = in;
		    return outs;
	      }
        }
      
        /** 
         * parameter1 accessor method 
         */
        public void setParmeter1(float newParameter1){
            parameter1=newParameter1;
        }


        /** 
         * return effect name 
         */
        public String getName() {
            return effectName;
        }

		 public Format setOutputFormat(Format output) {
			if (output == null || matches(output, supportedOutputFormats) == null)
		    return null;
			RGBFormat incoming = (RGBFormat) output;
	
			Dimension size = incoming.getSize();
			int maxDataLength = incoming.getMaxDataLength();
			int lineStride = incoming.getLineStride();
			float frameRate = incoming.getFrameRate();
			int flipped = incoming.getFlipped();
			int endian = incoming.getEndian();

			if (size == null)
			    return null;
			if (maxDataLength < size.width * size.height * 3)
			    maxDataLength = size.width * size.height * 3;
			if (lineStride < size.width * 3)
			    lineStride = size.width * 3;
			if (flipped != Format.FALSE)
			    flipped = Format.FALSE;
	
			outputFormat = (RGBFormat)supportedOutputFormats[0].intersects(new RGBFormat(size,
							        maxDataLength,
								null,
								frameRate,
								Format.NOT_SPECIFIED,
								Format.NOT_SPECIFIED,
								Format.NOT_SPECIFIED,
								Format.NOT_SPECIFIED,
								Format.NOT_SPECIFIED,
								lineStride,
								Format.NOT_SPECIFIED,
								Format.NOT_SPECIFIED));

			//System.out.println("final outputformat = " + outputFormat);
			return outputFormat;
		}

        /** 
         * do the processing 
         */
        
        public int process(Buffer in, Buffer out) {
	       // This is the "Callback" to access individual frames.
	       //accessFrame(in);
		   ejEffects.processFrame(in);
		   
		   // !!!!

	       // Swap the data between the input & output.
	       Object data = in.getData();
	       in.setData(out.getData());
	       out.setData(data);

	       // Copy the input attributes to the output
	       out.setFormat(in.getFormat());
	       out.setLength(in.getLength());
	       out.setOffset(in.getOffset());

	       return BUFFER_PROCESSED_OK;
        }


    // Utility methods.
    Format matches(Format in, Format outs[]) {
	for (int i = 0; i < outs.length; i++) {
	    if (in.matches(outs[i]))
		return outs[i];
	}
	
	return null;
    }
    
    
    byte[] validateByteArraySize(Buffer buffer,int newSize) {
        Object objectArray=buffer.getData();
        byte[] typedArray;

        if (objectArray instanceof byte[]) {     // is correct type AND not null
            typedArray=(byte[])objectArray;
            if (typedArray.length >= newSize ) { // is sufficient capacity
                return typedArray;
            }

            byte[] tempArray=new byte[newSize];  // re-alloc array
            System.arraycopy(typedArray,0,tempArray,0,typedArray.length);
            typedArray = tempArray;
        } else {
            typedArray = new byte[newSize];
        }

        buffer.setData(typedArray);
        return typedArray;
    }
        /** 
         * utility: update the output buffer fields 
         */
        protected void updateOutput(Buffer outputBuffer,
                                    Format format,int length, int offset) {

            outputBuffer.setFormat(format);
            outputBuffer.setLength(length);
            outputBuffer.setOffset(offset);
        }
    
}


