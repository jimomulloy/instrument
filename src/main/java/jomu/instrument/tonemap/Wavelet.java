package jomu.instrument.tonemap;

import jomu.instrument.tonemap.filters.*;


/**
 *  Wavelet analysis class
 */

public final class Wavelet {

	static final double pi = 3.14159265;

	public static final void convert(	double[] wave,
					double[] s,
	   	 			double[] freq,
	   	 			double dt,   
	   				int	skip,
	   				int mm,
	   				int	nn,
	   				ProgressListener progressListener,
					double pFactor,
	   				double tFactor,
					double pOffset,
	   				double t1Setting,
					double t2Setting,
	   				double t3Setting,
					double t4Setting,
					boolean t1Switch,
					boolean t2Switch,
					boolean t3Switch,
					boolean t4Switch
	  	   		
	  	   		) {
	
		int i, j, index=0, index1=0, ii=0, lmin=0, 
			lmax=0, offset=0, offsetmin=0, offsetmax=0, step=0, steplast=0;
		
		double tval, t, k, nan, t1, t2, amount;
		double[] period;
		double[] kvals;
		double ps, ctotr, ctoti;
		double z0;
		IIRLowpassFilterDesign lpfd = null;
		IIRLowpassFilter lowPassShelf = null;
		IIRLowpassFilter lowPassShelf1 = null;
		IIRBandpassFilterDesign bpfd = null;
		IIRBandpassFilter bandPassShelf = null;
		z0 = (double)nn;
		
		period = new double[nn];
		kvals = new double[nn];
	
		nan=0; 
		double frequency;
		frequency = freq[0]*(Math.pow(2.0, ((pOffset/100.0)/12.0)));
	
		for (j=0; j<nn; j++) {
			period[j]=1.0/frequency;
			kvals[j]=frequency;
			frequency = frequency*(Math.pow(2.0, ((pFactor/100.0)/12.0)));
			System.out.println("Frequencies are: "+j+", "+frequency); 
		} 
			
		for (i=0; i<wave.length; i++) {
			wave[i]=nan;
		}
		
		double[] filterBuffer1 = new double[mm];
		double[] filterBuffer2 = new double[mm];		
		double[] filterBuffer3 = new double[mm];
		double[] filterBuffer4 = new double[mm];

		for (i=0; i<mm; i++) {
			filterBuffer1[i] = s[i];
		}
		
		int firstflag = 0;
		int firstflag2 = 0;
		int firstflag3 = 0;
		
		
		for (i=nn-1; i>=0; i--) {
		    
			progressListener.setProgress((int)(((double)(index+1)/(double)wave.length)*100.0));
			
			offset=(int)Math.floor(((z0+1.0)/2.0)*period[i]/dt);
		
			k=kvals[i];
			
			System.out.println("K1 "+k);
			
			step = (int)Math.floor(Math.pow(2.0, ((tFactor/100.0)*((double)(nn - 1 - i))/12.0)));
			if (t1Switch) {
				if (firstflag==0) {				
					lpfd = new IIRLowpassFilterDesign((int)(k*(double)t2Setting/(double)t3Setting), (int)(1.0/dt), (double)t1Setting);	
					lpfd.doFilterDesign();	
					lowPassShelf = new IIRLowpassFilter(lpfd);
					lowPassShelf.doFilterNoSum(filterBuffer1, filterBuffer2, mm);
				}
			
				bpfd = new IIRBandpassFilterDesign((int)(k), (int)(1.0/dt), (double)t4Setting);
				bpfd.doFilterDesign();
				bandPassShelf = new IIRBandpassFilter(bpfd);
				bandPassShelf.doFilter(filterBuffer1, filterBuffer2, mm);
			
				firstflag=1;			
			}
		
			steplast = step;
		
			if (offset*2 < mm ) {
			//	for (j=offset; j<(mm-offset); j++) {
				for (j=0; j<mm; j++) {
					index=(i*mm+j)/skip;
					if (j % skip == 0) {
						
						t=j*dt;
						lmin=Math.max(j-(int)Math.floor(1.5*offset),0);
						lmax=Math.min(j+(int)Math.ceil(1.5*offset),(mm-1));
						ctotr=0;
						ctoti=0;
							
						for (ii=lmin; ii<=lmax; ii+=step) {
						/* integrate s*conj(psi) - note s is always real. We can
			 		 	speed things up a bit if we calculate the terms used in
						 the real and imaginary parts of ctot in one go... not
			 			very neat though*/
			 				tval = ii*dt;
			 				 
			 				if (ii>=0 && ii<mm) {
								if(t1Switch){
									t1=filterBuffer2[ii]*Math.exp(-2.0*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
								} else {
									t1=filterBuffer1[ii]*Math.exp(-2.0*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
								}
								
								t2=Math.exp((-z0*z0 / 2.0) - 2*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
								ctotr=ctotr+(Math.sin(2.0* pi*k*(tval-t))*t1 - t2);
								ctoti=ctoti+(Math.cos(2.0* pi*k*(tval-t))*t1 - t2);
			 				} else {
				 				t1 = 0;
				 				t2=Math.exp((-z0*z0 / 2.0) - 2*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
				 				ctotr=ctotr-t2;
								ctoti=ctoti-t2;
			 				}		 				
				 		}
												
						wave[index]+=step*Math.sqrt(k)*Math.sqrt(ctotr*ctotr+ctoti*ctoti);
					}
				
				}
				
			}
			
			if(t2Switch) {
			
			k=kvals[i];

			k = k*(Math.pow(2.0, ((pFactor/100.0)/24.0)));

			offset=(int)Math.floor(((z0+1.0)/2.0)/(k*dt));
			
			System.out.println("K2 "+k);
			
			step = (int)Math.floor(Math.pow(2.0, ((tFactor/100.0)*((double)(nn - 1 - i))/12.0)));
			if (t1Switch) {
				if (firstflag2==0) {	
					lpfd = new IIRLowpassFilterDesign((int)(k*(double)t2Setting/(double)t3Setting), (int)(1.0/dt), (double)t1Setting);	
					lpfd.doFilterDesign();	
					lowPassShelf = new IIRLowpassFilter(lpfd);
					lowPassShelf.doFilterNoSum(filterBuffer1, filterBuffer2, mm);
				}	
				bpfd = new IIRBandpassFilterDesign((int)(k), (int)(1.0/dt), (double)t4Setting);
				bpfd.doFilterDesign();
				bandPassShelf = new IIRBandpassFilter(bpfd);
				bandPassShelf.doFilter(filterBuffer1, filterBuffer3, mm);
			
				firstflag2=1;			
			}
		
			steplast = step;
		
			if (offset*2 < mm ) {
			//	for (j=offset; j<(mm-offset); j++) {
				for (j=0; j<mm; j++) {
					index=(i*mm+j)/skip;
					if (j % skip == 0) {
						
						t=j*dt;
						lmin=Math.max(j-(int)Math.floor(1.5*offset),0);
						lmax=Math.min(j+(int)Math.ceil(1.5*offset),(mm-1));
						ctotr=0;
						ctoti=0;
							
						for (ii=lmin; ii<=lmax; ii+=step) {
						/* integrate s*conj(psi) - note s is always real. We can
			 		 	speed things up a bit if we calculate the terms used in
						 the real and imaginary parts of ctot in one go... not
			 			very neat though*/
			 				tval = ii*dt;
			 				 
			 				if (ii>=0 && ii<mm) {
								if(t1Switch){
									t1=filterBuffer3[ii]*Math.exp(-2.0*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
								} else {
									t1=filterBuffer1[ii]*Math.exp(-2.0*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
								}
								
								t2=Math.exp((-z0*z0 / 2.0) - 2*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
								ctotr=ctotr+(Math.sin(2.0* pi*k*(tval-t))*t1 - t2);
								ctoti=ctoti+(Math.cos(2.0* pi*k*(tval-t))*t1 - t2);
			 				} else {
				 				t1 = 0;
				 				t2=Math.exp((-z0*z0 / 2.0) - 2*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
				 				ctotr=ctotr-t2;
								ctoti=ctoti-t2;
			 				}		 				
				 		}
												
						wave[index]+=step*Math.sqrt(k)*Math.sqrt(ctotr*ctotr+ctoti*ctoti);
					}
				
				}
				
			}
			
			k=kvals[i];
	
			k = k*(Math.pow(2.0, (-(pFactor/100.0)/24.0)));
	
			offset=(int)Math.floor(((z0+1.0)/2.0)/(k*dt));
				
			System.out.println("K3 "+k);
				
			step = (int)Math.floor(Math.pow(2.0, ((tFactor/100.0)*((double)(nn - 1 - i))/12.0)));
			
			if (t1Switch) {
				if (firstflag3==0) {	
					lpfd = new IIRLowpassFilterDesign((int)(k*(double)t2Setting/(double)t3Setting), (int)(1.0/dt), (double)t1Setting);	
					lpfd.doFilterDesign();	
					lowPassShelf = new IIRLowpassFilter(lpfd);
					lowPassShelf.doFilterNoSum(filterBuffer1, filterBuffer4, mm);
				}
				bpfd = new IIRBandpassFilterDesign((int)(k), (int)(1.0/dt), (double)t4Setting);
				bpfd.doFilterDesign();
				bandPassShelf = new IIRBandpassFilter(bpfd);
				bandPassShelf.doFilter(filterBuffer1, filterBuffer2, mm);
			
				firstflag3=1;			
			}
			
			steplast = step;
			if (offset*2 < mm ) {
			//	for (j=offset; j<(mm-offset); j++) {
				for (j=0; j<mm; j++) {
					index=(i*mm+j)/skip;
					if (j % skip == 0) {
						
						t=j*dt;
						lmin=Math.max(j-(int)Math.floor(1.5*offset),0);
						lmax=Math.min(j+(int)Math.ceil(1.5*offset),(mm-1));
						ctotr=0;
						ctoti=0;
							
						for (ii=lmin; ii<=lmax; ii+=step) {
						/* integrate s*conj(psi) - note s is always real. We can
			 		 	speed things up a bit if we calculate the terms used in
						 the real and imaginary parts of ctot in one go... not
			 			very neat though*/
			 				tval = ii*dt;
			 				 
			 				if (ii>=0 && ii<mm) {
								if(t1Switch){
									t1=filterBuffer4[ii]*Math.exp(-2.0*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
								} else {
									t1=filterBuffer1[ii]*Math.exp(-2.0*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
								}
								
								t2=Math.exp((-z0*z0 / 2.0) - 2*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
								ctotr=ctotr+(Math.sin(2.0* pi*k*(tval-t))*t1 - t2);
								ctoti=ctoti+(Math.cos(2.0* pi*k*(tval-t))*t1 - t2);
			 				} else {
				 				t1 = 0;
				 				t2=Math.exp((-z0*z0 / 2.0) - 2*k*(tval-t)*k*(tval-t)*pi*pi/(z0*z0));
				 				ctotr=ctotr-t2;
								ctoti=ctoti-t2;
			 				}		 				
				 		}
												
						wave[index]+=step*Math.sqrt(k)*Math.sqrt(ctotr*ctotr+ctoti*ctoti);
					}
				
				}
				
			}
			
			}
		}
		
	progressListener.setProgress(100);
	return ;
	}

}