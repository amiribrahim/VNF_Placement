import java.util.ArrayList;

public class Test {

	public ArrayList<VNF> availableVNFs;
	
	public Test() {
		availableVNFs = new ArrayList<VNF>();
	}
	
	public void addVNF(VNF vnf) {
		availableVNFs.add(vnf);
	}
	
	/*
	private  boolean contains(String[] vnfs, String vnfName) {
		for (String vnf : vnfs)
			if(vnf!=null)
				if(vnfName.compareToIgnoreCase(vnf)==0)
					return true;
		return false;
	}
	
	public SFCRequest createSFCRequest(int sfcLen, double maxLatency) {
		int avVNFs = availableVNFs.size();
		if (sfcLen > avVNFs) {
			System.err.println("SFC request length: "+sfcLen+" can't be longer than number of available VNFs "+avVNFs);
			return null;
		}
		
		ArrayList<VNF> sfc = new ArrayList<VNF>();
		String[] alreadyselectedVNF = new String[sfcLen];
		int completed = 0,index=0;
		while(completed<sfcLen) {
			index = Utils.getRandInt(avVNFs);
			if(!contains(alreadyselectedVNF, this.availableVNFs.get(index).getName())) {
				sfc.add(this.availableVNFs.get(index));
				alreadyselectedVNF[completed++] = this.availableVNFs.get(index).getName();
			}
				
		}
		return new SFCRequest(maxLatency,sfc);
	}*/
	
	public static void main(String[] args) {
		
		Test t =new Test();
		// TODO Auto-generated method stub
		t.availableVNFs.add(new VNF("NAT", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		t.availableVNFs.add(new VNF("FW", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		t.availableVNFs.add(new VNF("VID", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		t.availableVNFs.add(new VNF("OPT", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		t.availableVNFs.add(new VNF("MMS", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		t.availableVNFs.add(new VNF("PAR", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		t.availableVNFs.add(new VNF("SGW", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		t.availableVNFs.add(new VNF("PGW", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		t.availableVNFs.add(new VNF("HSS", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		t.availableVNFs.add(new VNF("RRU", Utils.getVNFCores(),Utils.getRandBool(),Utils.getRandBool()));
		
		System.out.println(SFCRequest.createSFCRequest(t.availableVNFs, 5, 100.5).toString());
		
	}

}
