import java.time.LocalTime;
import java.util.ArrayList;

import javax.swing.text.StyleContext.SmallAttributeSet;


public class SFCRequest {
	
	private static int currentMaxID=-1;
	
	private int ID;
	private LocalTime timeReceived; 
	private LocalTime timeRealized;
	private LocalTime timeEnded;
	private int sourceNodeID;
	private int destinationNodeID;
	private boolean realized;
	private double e2eMaxLatency; //in sec
	private long duration;//time span of this SFC request in sec
	private ArrayList<VNF> vnfList; //holds ordered SFC's VNFs 
	private VirtLink[] vLinks;
	private int[] cfFlowShare; //chain function flow share in a shareable VNF
	private int totalCPU;
	private int totalRAM;
	private int totalBW;
	
	public static int smallestCPU = Utils.VNF_MAX_CPU;
	public static int largestCPU = Utils.VNF_MIN_CPU;
	/**
	 * Creates  an SFC request object 
	 * @param sourceID SFC request flow origin
	 * @param destinationID SFC request flow resort
	 * @param e2eLatency SFC request max E2E Latency in msec 
	 * @param timeSpan expected duration of SFC request
	 * @param vnfList SFC ordered components list 
	 */
	private SFCRequest(int sourceID, int destinationID,double e2eLatency, long timeSpan, ArrayList<VNF> vnfList) {
		this.ID = ++SFCRequest.currentMaxID;
		this.timeReceived = LocalTime.now();
		this.sourceNodeID = sourceID;
		this.destinationNodeID = destinationID;
		this.realized = false;
		this.e2eMaxLatency = e2eLatency;
		this.duration = timeSpan;
		this.vnfList = vnfList;
		vLinks = new VirtLink[this.vnfList.size()-1];
		this.cfFlowShare = new int[this.vnfList.size()];
		this.timeRealized = LocalTime.now();//for testing and will be overridden when sfc is realized by Placement class methods.
		this.timeEnded = LocalTime.now();//for testing and will be overridden when sfc ended in the terminateSFC() method.
		totalCPU=0;
		totalRAM=0;
		totalBW=0;
	}
	
	/**
	 * Creates  an SFC request object 
	 * @param e2eLatency SFC request max E2E Latency in msec 
	 * @param vnfList SFC ordered components list 
	 */
	private SFCRequest(double e2eLatency, ArrayList<VNF> vnfList) {
		this.ID = ++SFCRequest.currentMaxID;
		this.timeReceived = LocalTime.now();
		this.sourceNodeID = 0; //has no meaning
		this.destinationNodeID = 0; //has no meaning
		this.realized = false;
		this.e2eMaxLatency = e2eLatency;
		this.duration = 0;
		this.vnfList = vnfList;
		vLinks = new VirtLink[this.vnfList.size()-1];
		for (int i = 0; i < vLinks.length; i++) {
			vLinks[i] = new VirtLink(vnfList.get(i), vnfList.get(i+1));
		}
		this.cfFlowShare = new int[this.vnfList.size()];
		this.timeRealized = LocalTime.now();//for testing and will be overridden when sfc is realized by Placement class methods.
		this.timeEnded = LocalTime.now();//for testing and will be overridden when sfc ended in the terminateSFC() method.
		totalCPU=0;
		totalRAM=0;
		totalBW=0;
	}
	
	public SFCRequest(SFCRequest sfc) {
		this.ID = sfc.getID();
		this.timeReceived = LocalTime.now();
		this.sourceNodeID=0;
		this.destinationNodeID=0;
		this.realized=sfc.isRealized();
		this.e2eMaxLatency = sfc.getE2eMaxLatency();
		this.duration = sfc.getDuration();
		this.vnfList = new ArrayList<VNF>();
		for (int i = 0; i < sfc.getVnfList().size(); i++) {
			this.vnfList.add(new DeployedVNF(sfc.getVnfList().get(i),true));
		}
		this.cfFlowShare = new int[this.vnfList.size()];
		this.timeRealized = LocalTime.now();//for testing and will be overridden when sfc is realized by Placement class methods.
		this.timeEnded = LocalTime.now();//for testing and will be overridden when sfc ended in the terminateSFC() method.
		this.totalCPU = sfc.totalCPU;
		this.totalRAM = sfc.totalRAM;
		this.totalBW = sfc.totalBW;
	}

	
	public static SFCRequest createSFCRequest(ArrayList<VNF> availableVNFs, int sfcLen, double maxLatency) {
		int avVNFs = availableVNFs.size();
		if (sfcLen > avVNFs) {
			System.err.println("SFC request length: "+sfcLen+" can't be longer than number of available VNFs "+avVNFs);
			return null;
		}
		
		ArrayList<VNF> sfc = new ArrayList<VNF>();
		DeployedVNF vnf;
		String[] alreadyselectedVNF = new String[sfcLen];
		int completed = 0,index=0;
		while(completed<sfcLen) {
			index = Utils.getRandInt(avVNFs);
			if(!contains(alreadyselectedVNF, availableVNFs.get(index).getName())) {
				vnf = new DeployedVNF(availableVNFs.get(index));
				if(vnf.getCPU()<smallestCPU) 
					smallestCPU = vnf.getCPU();
				if(vnf.getCPU()>largestCPU) 
					largestCPU = vnf.getCPU();
				sfc.add(vnf);
				alreadyselectedVNF[completed++] = availableVNFs.get(index).getName();
			}
				
		}
		
		for(int i=1; i<sfc.size(); i++) {
			System.out.println("out flow of VNF_"+(i-1)+((DeployedVNF)sfc.get(i-1)).getOutFlow());
			((DeployedVNF)sfc.get(i)).setInFlow(((DeployedVNF)sfc.get(i-1)).getOutFlow());
		}
		
		
		return new SFCRequest(maxLatency,sfc);
	}
	
	
	
	public void setAllVNFsUnshareable() {
		for(VNF vnf: this.vnfList)
			vnf.setShareable(false);
	}
	
	private static boolean contains(String[] vnfs, String vnfName) {
		for (String vnf : vnfs)
			if(vnf!=null)
				if(vnfName.compareToIgnoreCase(vnf)==0)
					return true;
		return false;
	}
	
	public int length() {
		return this.vnfList.size();
	}
	
	/*reThink this method
	public void terminateSFC() {
		this.timeEnded = LocalTime.now();
		for (int i = 0; i < vnfList.length; i++) {
			VNF vnf = vnfList[i];
			vnf.setInFlow(vnf.getInFlow()-this.cfFlowShare[i]);
			vnf.setOutFlow(vnf.getOutFlow()-this.cfFlowShare[i]);
			vnf.recalcUtilization();
		}
	}*/
	
	public void sumTotalResources() {
		for (int i = 0; i < vnfList.size(); i++) {
			this.totalCPU+=vnfList.get(i).getCPU();
			this.totalRAM+=vnfList.get(i).getRAM();
			this.totalBW+=((DeployedVNF)vnfList.get(i)).getOutFlow();
		}
	}
	
	@Override
	public String toString() {
		sumTotalResources();
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < vnfList.size(); i++) {
			VNF vnf = vnfList.get(i);
			s.append(vnf.toString()+"\n");
			//s.append("This SFC shares "+this.cfFlowShare[i]+" of "+vnf.name+"\n");
		}
		return "SFC-ID: "+this.ID+
				" | From: "+this.sourceNodeID+
				" | To: "+this.destinationNodeID+
				" | Time-Received: "+this.timeReceived.toString()+
				" | Time-realized: "+this.timeRealized.toString()+
				" | Time-Ended: "+this.timeEnded+
				" | E2E-latency: "+this.e2eMaxLatency+
				" msec | Duration: "+this.duration+
				" sec \n"+
				"Total:  CPU  |  RAM  |  BW \n"+
				"        "+totalCPU+"   |  "+totalRAM+"  |  "+totalBW+"\n"+
				"VNF-List:"+"\n"+s;
	}
	
	public int numberOfShareableVNFs() {
		int num=0;
		for (int i = 0; i < this.vnfList.size(); i++) {
			if(vnfList.get(i).isShareable()==1)
				num++;
		}
		return num;
	}
	
	public int[] getCfFlowShare() {
		return cfFlowShare;
	}

//	public void setCfFlowShare(int[] cfFlowShare) {
//		this.cfFlowShare = cfFlowShare;
//	}
	
	public int getID() {
		return ID;
	}

	public void setID(int sfcRequestID) {
		this.ID = sfcRequestID;
	}

	public LocalTime getTimeReceived() {
		return timeReceived;
	}

	public void setTimeReceived(LocalTime timeReceived) {
		this.timeReceived = timeReceived;
	}

	public LocalTime getTimeRealized() {
		return timeRealized;
	}

	public void setTimeRealized(LocalTime timeRealized) {
		this.timeRealized = timeRealized;
	}

	public int getSourceNodeID() {
		return sourceNodeID;
	}

	public void setSourceNodeID(int sourceNodeID) {
		this.sourceNodeID = sourceNodeID;
	}

	public int getDestinationNodeID() {
		return destinationNodeID;
	}

	public void setDestinationNodeID(int destinationNodeID) {
		this.destinationNodeID = destinationNodeID;
	}

	public boolean isRealized() {
		return realized;
	}

	public void setRealized(boolean realized) {
		this.realized = realized;
	}

	public double getE2eMaxLatency() {
		return e2eMaxLatency;
	}

	public void setE2eMaxLatency(double e2eMaxLatency) {
		this.e2eMaxLatency = e2eMaxLatency;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public ArrayList<VNF> getVnfList() {
		return vnfList;
	}

	public void setVnfList(ArrayList<VNF> vnfList) {
		this.vnfList = vnfList;
	}

	public static int getCurrentMaxID() {
		return currentMaxID;
	}
	
	public VirtLink[] getvLinks() {
		return vLinks;
	}

	public void setvLinks(VirtLink[] vLinks) {
		this.vLinks = vLinks;
	}

	public static void main(String[] args) {ArrayList<VNF> availableVNFs = new ArrayList<VNF>();
	availableVNFs.add(new VNF("NAT", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	availableVNFs.add(new VNF("FW", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	availableVNFs.add(new VNF("VID", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	availableVNFs.add(new VNF("OPT", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	availableVNFs.add(new VNF("MMS", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	availableVNFs.add(new VNF("PAR", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	availableVNFs.add(new VNF("SGW", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	availableVNFs.add(new VNF("PGW", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	availableVNFs.add(new VNF("HSS", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	availableVNFs.add(new VNF("RRU", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
	
	
	SFCRequest sfc = SFCRequest.createSFCRequest(availableVNFs, 4, 80.5);
	System.out.println(sfc.toString());
	
	SFCRequest sfc2 = new SFCRequest(sfc);
	System.out.println(sfc2.toString());
	
	sfc2.setAllVNFsUnshareable();
	System.out.println("######################################################################");
	System.out.println(sfc.toString());
	System.out.println("######################################################################");
	System.out.println(sfc2.toString());
	
	}

}
