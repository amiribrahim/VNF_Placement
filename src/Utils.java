import java.security.SecureRandom;


public class Utils {

	final static int VNF_MAX_CPU = 8;
	final static int VNF_MIN_CPU = 2;
	final static int VNF_MAX_RAM = 16; //in GBs
	final static int VNF_MIN_RAM = 1;
	final static int VNF_MAX_InFlow = 300; //in Mbps
	final static int VNF_MIN_InFlow = 10;
	
	final static int NODE_MAX_CPU = 64;
	final static int NODE_MIN_CPU = 8;
	final static int NODE_MAX_RAM = 128;
	final static int NODE_MIN_RAM = 16;
	
	final static int LINK_MAX_BW = 1000; //in Mbps
	final static int LINK_MIN_BW = 100;
	final static int LINK_MAX_LENGTH = 1000; //in meters
	final static int LINK_MIN_LENGTH = 50;
	
	final static int SFC_MIN_Len = 2;
	
	final static SecureRandom ran = new SecureRandom();
	
	/**
	 * Returns random VNF CPU cores in the range [2 - 8]
	 * @return
	 */
	public static int getVNFCores() {
		return ran.nextInt(VNF_MAX_CPU-VNF_MIN_CPU)+VNF_MIN_CPU;
	}
	
	/**
	 * Returns random VNF Memory (in GB) in the range [1 - 15]
	 * @return
	 */
	public static int getVNFRAM() {
		return ran.nextInt(VNF_MAX_RAM-VNF_MIN_RAM)+VNF_MIN_RAM;
	}
	public static int getVNFRAMControlled(int vnfcores) {
		return 2*vnfcores;
	}
	
	/**
	 * Returns random VNF Ingress flow (in Mbps) to in the range [10 - 300]
	 * @return
	 */
	public static int getVNFMaxInFlow() {
		return ran.nextInt(VNF_MAX_InFlow-VNF_MIN_InFlow)+VNF_MIN_InFlow;
	}
	/**
	 * Returns fixed VNF Ingress flow (in Mbps) as a function of 
	 * @return
	 */
	public static int getVNFMaxInFlowFixed(int cpu,int ram) {
		return (int)((0.5*cpu*(VNF_MAX_InFlow/VNF_MAX_CPU))+(0.5*ram*(VNF_MAX_InFlow/VNF_MAX_RAM)));
	}
	
	public static int getVNFActualInFlow(int maxInFlow) {
		int t = ran.nextInt(maxInFlow);
		if(t<maxInFlow*0.15)
			t = (int)(maxInFlow*0.15);
		return t;
	}
	
	public static int getVNFActualOutFlow(int actualInFlow,boolean dropsCompresses) {
		if(dropsCompresses) {
			int t = ran.nextInt(actualInFlow);
			if(t<actualInFlow*0.40)
				t=(int)(actualInFlow*0.40);
			return t;
		}
		return actualInFlow;
	}
	
	/**
	 * Returns random Node CPU cores in the range [10 - 80]
	 * @return
	 */
	public static int getNodeCores() {
		return ran.nextInt(Utils.NODE_MAX_CPU-Utils.NODE_MIN_CPU)+Utils.NODE_MIN_CPU;
	}
	
	/**
	 * Returns random Node Memory (in GB) in the range [16 - 128]
	 * @return
	 */
	public static int getNodeRAM() {
		return ran.nextInt(Utils.NODE_MAX_RAM-Utils.NODE_MIN_RAM)+Utils.NODE_MIN_RAM;
	}
	
	public static int getNodeRAMControlled(int nodeCores) {
		return 2*nodeCores;
	}
	
	/**
	 * Returns random Link Bandwidth (in Mbps) in the range [100 - 2000]
	 * @return
	 */
	public static int getLinkBW() {
		return ran.nextInt(Utils.LINK_MAX_BW-Utils.LINK_MIN_BW)+Utils.LINK_MIN_BW;
	}
	
	/**
	 * Returns link length in meters, in the range [50 - 1000]
	 * @return
	 */
	public static int getLinkLength() {
		return ran.nextInt(Utils.LINK_MAX_LENGTH-Utils.LINK_MIN_LENGTH)+Utils.LINK_MIN_LENGTH;
	}
	
	public static int getRandInt(int bound) {
		return ran.nextInt(bound);
	}
	
	public static boolean getRandBool() {
		return ran.nextBoolean();
	}
	
	public static double getRandDouble() {
		return ran.nextDouble();
	}
	
	public static int getSFCLen(int availableVNFsLen) {
		return ran.nextInt(availableVNFsLen-Utils.SFC_MIN_Len)+Utils.SFC_MIN_Len;
	}
}
