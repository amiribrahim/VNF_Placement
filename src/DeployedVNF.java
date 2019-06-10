//import java.util.ArrayList;
import java.util.ArrayList;
import java.util.HashMap;
//import java.security.SecureRandom;
import java.util.Map.Entry;

public class DeployedVNF extends VNF{
	
	private static int currentMaxID=-1;
	//private String name; inherited from super
	//boolean shareable,dropsCompresses;
	private int ID;
	
	//All below commented fields are inherited from super
	/*//private int CPU; //CPU cores required for this VNF to operate 
	//private int RAM; //Memory in MB required for this VNF to operate 
	
	//private int maxInFlow; //maximum flow this VNF can take and generate given the allotted resources*/
	private int actualInFlow; //actual flow this VNF is getting
	private int outFlow; //actual flow this VNF is generating
	
	//Freshly deployed and/or non-shareable  
	private float utilization; //If deployed, percentage of already used/utilized resources. Could be calculated as (inFlow/maxInFlow)
	private HashMap<Integer, int[]> sfcRequestsServed;//Holds SFC-Requests served by this VNF as: Key = SFCRequest_ID and Value: flow Share
	private int nodeID; //substrate node at which this VNF is placed.
	
	//utilizing/sharing other already deployed VNF
	private boolean usingOtherVNF; //true if this VNF isn't deployed, rather it shares 
	private int amountShared; //this VNF share of the shared/utilized VNF (in terms of actual flow amount shared)
		
	//Creating Freshly deployed VNF
	public DeployedVNF(String name, boolean shareable, boolean dropsCompresses) {
		
		this.ID = ++DeployedVNF.currentMaxID;
		
		//to reflect that this is deployed/not sharing VNF 
		this.usingOtherVNF = false;
		this.amountShared = -1;
		
		this.name = name;
		this.shareable = shareable;				
		
		if(this.shareable)
			this.sfcRequestsServed = new HashMap<Integer, int[]>();
				
		this.dropsCompresses = dropsCompresses;
		this.CPU = Utils.getVNFCores();
		//this.RAM = Utils.getVNFRAM();
		this.RAM = Utils.getVNFRAMControlled(this.CPU);
		this.maxInFlow = Utils.getVNFMaxInFlowFixed(this.CPU,this.RAM);
		this.actualInFlow = Utils.getVNFActualInFlow(this.maxInFlow);
		this.utilization = (float)this.actualInFlow / (float)this.maxInFlow;
		this.outFlow = Utils.getVNFActualOutFlow(this.actualInFlow, dropsCompresses);
	}
	
	public DeployedVNF(VNF vnf) {
		
		this.ID = ++DeployedVNF.currentMaxID;
		
		//to reflect that this is deployed/not sharing VNF 
		this.usingOtherVNF = false;
		this.amountShared = -1;
		
		this.name = vnf.getName();
		if(vnf.isShareable()==1)
			this.shareable = true;
		else
			this.shareable = false;
		
		//if(this.shareable) we decided to register sfc requests served even of this vnf isn't shareable
		this.sfcRequestsServed = new HashMap<Integer, int[]>();
				
		this.dropsCompresses = vnf.isDropsCompresses();
		this.CPU = vnf.getCPU();
		this.RAM = vnf.getRAM();
		this.maxInFlow = vnf.getMaxInFlow();
		this.actualInFlow = Utils.getVNFActualInFlow(this.maxInFlow);
		//System.out.println("VNF_"+this.name+" actual_in_flow: "+this.actualInFlow);
		this.utilization = (float)this.actualInFlow / (float)this.maxInFlow;
		this.outFlow = Utils.getVNFActualOutFlow(this.actualInFlow, dropsCompresses);
	}
	
	//cc = carbon copy 
	public DeployedVNF(VNF vnf,boolean cc) {

		this.ID = ++DeployedVNF.currentMaxID;

		//to reflect that this is deployed/not sharing VNF 
		this.usingOtherVNF = false;
		this.amountShared = -1;

		this.name = vnf.getName();
		if(vnf.isShareable()==1)
			this.shareable = true;
		else
			this.shareable = false;

		//if(this.shareable) we decided to register sfc requests served even of this vnf isn't shareable
		this.sfcRequestsServed = new HashMap<Integer, int[]>();

		this.dropsCompresses = vnf.isDropsCompresses();
		this.CPU = vnf.getCPU();
		this.RAM = vnf.getRAM();
		this.maxInFlow = vnf.getMaxInFlow();
		this.actualInFlow = ((DeployedVNF)vnf).getInFlow();
		//System.out.println("VNF_"+this.name+" actual_in_flow: "+this.actualInFlow);
		this.utilization = (float)this.actualInFlow / (float)this.maxInFlow;
		this.outFlow = ((DeployedVNF)vnf).getOutFlow();
	}
	
	
	//Creating VNF that shares/utilizes an already deployed VNF 
	public DeployedVNF(String name, int cpu, int ram, int maxInFlow, int actualInFlow, int amountShared) {
		this.usingOtherVNF = true;
		this.name = name;
		this.CPU = cpu;
		this.RAM = ram;
		this.maxInFlow = maxInFlow;
		this.actualInFlow = actualInFlow;
		this.amountShared = amountShared;
	}
	
	// Creating VNF that shares/utilizes an already deployed VNF
	public DeployedVNF(DeployedVNF vnf) {
		this.ID = vnf.getID();
		this.name = vnf.getName();
		this.CPU = vnf.getCPU();
		this.RAM = vnf.getRAM();
		this.maxInFlow = vnf.getMaxInFlow();
		this.actualInFlow = vnf.getInFlow();
		this.outFlow = vnf.getOutFlow();
		this.usingOtherVNF = vnf.isUsingOtherVNF();
		if(vnf.isShareable()==1)
			this.shareable = true;
		else
			this.shareable = false;
		this.utilization = vnf.getUtilization();
		this.amountShared = vnf.getAmountShared();
		this.nodeID = vnf.getNodeID();
		this.sfcRequestsServed = this.getSfcRequestsServed();
		//if(this.shareable) we decided to register sfc requests served even of this vnf isn't shareable
	    this.sfcRequestsServed = new HashMap<Integer, int[]>();
	}
	
	/**
	 * Addds more flow to the actual in flow to reflect serving the new SFC-request
	 * @param sfcRequestID
	 * @param flowShare 
	 * @return
	 */
	public void addSFCRequest(int sfcRequestID, int inFlow,int outFlow) {
		int[] flows= {inFlow,outFlow};
		this.sfcRequestsServed.put(sfcRequestID, flows);
		this.actualInFlow += inFlow;
		this.outFlow += outFlow;
		this.recalcUtilization();
	}
	
	public boolean removeSFCRequest(int sfcRequestID) {
		int[] flowShare = this.sfcRequestsServed.get(sfcRequestID);
		if(flowShare!=null) {
			this.actualInFlow -= flowShare[0];
			this.outFlow -= flowShare[1];
			this.recalcUtilization();
			return true;
		}
		
		return false;
	}
	
	public void recalcUtilization() {
		this.utilization = (float)this.actualInFlow / (float)this.maxInFlow;
	}
	
	@Override
	public String toString(){
		String s="Unshareable",d="No-Drops";
		if (shareable)
			s = "shareable";
		if(dropsCompresses)
			d="Drops";
		return "VNF: "+this.name+" | VNF-ID: "+this.ID+" |"+s+" | "+d+" | CPU: "+this.CPU+" | RAM: "+this.RAM+" | MaxFlow: "+this.maxInFlow+" | Actual-In-Flow: "+
	this.actualInFlow+" | Utilization: "+this.utilization+" | Out-Flow: "+this.outFlow+"\n"+displayServedSFCs(); 
	}
	
	public String displayServedSFCs() {
		StringBuilder s = new StringBuilder();
		int sfcID=0;int[] flow;
		s.append("\n");
		for(Entry<Integer, int[]> sfc : sfcRequestsServed.entrySet()) {
		    sfcID = sfc.getKey();
		    flow = sfc.getValue();

		    s.append("- Serving SFC_"+sfcID+" with inFlow: "+flow[0]+" and outFlow= "+flow[1]+"\n");
		}
		return s.toString();
	}
	
	public boolean isUsingOtherVNF() {
		return usingOtherVNF;
	}

	public void setUsingOtherVNF(boolean usingOtherVNF) {
		this.usingOtherVNF = usingOtherVNF;
	}

	public int getAmountShared() {
		return amountShared;
	}

	public void setAmountShared(int amountShared) {
		this.amountShared = amountShared;
	}
	
	public void setInFlow(int inFlow) {
		if(inFlow > maxInFlow) {
			System.out.println("Actual inFlow= ("+inFlow+") can't be greater than Max_Flow"+maxInFlow+", so will set maxFlow = inFlow");
			this.maxInFlow = inFlow;
		}
		if(inFlow<=1) {
			this.actualInFlow=1;
			this.outFlow=1;
		}else {
		this.actualInFlow = inFlow;
		this.outFlow = Utils.getVNFActualOutFlow(this.actualInFlow, dropsCompresses);
		}
		this.recalcUtilization();
		
	}
	public int getCPU() {
		return CPU;
	}
	public void setCPU(int cPU) {
		CPU = cPU;
	}
	public int getRAM() {
		return RAM;
	}
	public void setRAM(int rAM) {
		RAM = rAM;
	}
	public int getMaxInFlow() {
		return maxInFlow;
	}
	public void setMaxInFlow(int maxInFlow) {
		this.maxInFlow = maxInFlow;
	}
	
	public int getInFlow() {
		return actualInFlow;
	}
	
	public int getOutFlow() {
		return outFlow;
	}
	public void setOutFlow(int outFlow) {
		this.outFlow = outFlow;
	}
	public float getUtilization() {
		return utilization;
	}
	
	
	/*public void setUtilization(float utilization) {
		this.utilization = utilization;
	}*/
	
	public int getNodeID() {
		return nodeID;
	}

	public void setNodeID(int nodeID) {
		this.nodeID = nodeID;
	}
	
	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
	}
	
	public String getName() {
		return name;
	}

	public HashMap<Integer, int[]> getSfcRequestsServed() {
		return sfcRequestsServed;
	}
	
	public int getAvailableFlow() {
		return this.maxInFlow-this.actualInFlow;
	}

	public void setSfcRequestsServed(HashMap<Integer, int[]> sfcRequestsServed) {
		this.sfcRequestsServed = sfcRequestsServed;
	}
//================================================================ TESTING ==================================================	
	public static void main(String[] args) {

		boolean shareable,dropsCompresses;
//		for (int i = 1; i < 11; i++) {
//			shareable = Math.random() < 0.5;
//			dropsCompresses = Math.random() < 0.5;
//			VNF tvnf = new VNF("VNF"+i,shareable,dropsCompresses,false);
//			//tvnf.setInFlow(Utils.getActualInFlow(tvnf.getMaxInFlow()));
//			System.out.println(tvnf.toString());
//		}
//		System.out.println("==============================================================================================================");
		
		for (int i = 1; i < 11; i++) {
			shareable = Math.random() < 0.5;
			dropsCompresses = Math.random() < 0.5;
			DeployedVNF tvnf = new DeployedVNF("VNF"+i,shareable,dropsCompresses);
			//tvnf.setInFlow(Utils.getActualInFlow(tvnf.getMaxInFlow()));
			//System.out.print(tvnf.toString());
		}
		
		ArrayList<DeployedVNF> vnfs = new ArrayList<DeployedVNF>();
		vnfs.add(new DeployedVNF("NAT", true, false));//ID=1
		vnfs.add(new DeployedVNF("FW", false, true));//ID=2
		vnfs.add(new DeployedVNF("Router", true, false));//ID=3
		
		System.out.println(vnfs.get(0).toString());
		DeployedVNF vnf= vnfs.get(0);
		vnf.setInFlow(110);
		System.out.println(vnfs.get(0).toString());
	}
}
