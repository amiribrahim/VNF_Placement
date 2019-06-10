import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import gurobi.GRBException;

public class Experiment1 {
	
	private static int[] getNodes_Links(int[][] conMat) {
		int[] NL= {conMat.length,0};
		int numLinks=0;
		for (int i = 0; i < conMat.length; i++) {
			for (int j = 0; j < conMat[i].length; j++) {
				if(conMat[i][j]==1)
					numLinks++;
			}
		}
		NL[1]=numLinks/2;
		
		return NL;
	}

	public static void main(String[] args) {
		
		double UcCPU = 2.5;
		double UcRAM = 1.7;
		double UcBW = 2;
		

		
		
		
		try {
			System.setOut(new PrintStream(new FileOutputStream("experiment1-.txt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		int numberOfSFCRequests = 20;

		//NSFNET network connectivity matrix
		int[][] conMat = {{0,1,0,1,0,0,0,0,0,0,0,0,0},
				{1,0,1,0,0,0,1,0,0,0,0,0,0},
				{0,1,0,0,1,0,0,0,0,0,0,0,0},
				{1,0,0,0,0,1,0,0,0,0,0,0,0},
				{0,0,1,0,0,1,0,0,1,0,0,0,0},
				{0,0,0,1,1,0,1,0,0,0,0,0,0},
				{0,1,0,0,0,1,0,1,0,0,0,0,0},
				{0,0,0,0,0,0,1,0,0,0,0,1,0},
				{0,0,0,0,1,0,0,0,0,1,0,0,1},
				{0,0,0,0,0,0,0,0,1,0,1,0,0},
				{0,0,0,0,0,0,0,0,0,1,0,1,0},
				{0,0,0,0,0,0,0,1,0,0,1,0,1},
				{0,0,0,0,0,0,0,0,1,0,0,1,0}};
		int[] NL = Experiment1.getNodes_Links(conMat);

		HashMap<SFCRequest, int[][]> sfcRequests_Sol1 = new HashMap<SFCRequest, int[][]>();
		HashMap<SFCRequest, int[][]> sfcRequests_Sol2 = new HashMap<SFCRequest, int[][]>();
		
		//[o][sfc_no] = cpu=RAM utilization after placement of SFC_no
		//[1][sfc_no] = BW utilization after placement of SFC_no
		//[2][sfc_no] = SFC_no length
		//[3][sfc_no] = 1 if solved, 0 otherwise 
		//[4][sfc_no] = number of shareable VNFs
		double[][] model1Util = new double[5][numberOfSFCRequests]; 
		double[][] model2Util = new double[5][numberOfSFCRequests];
		
		NetworkModel model1 = new NetworkModel(NL[0], NL[1], conMat);
		NetworkModel model2 = new NetworkModel(model1);
		System.out.println("///////////////////////////////////before Optimization//////////////////////////////");
		System.out.println("Model1: \n"+model1.displayNodes());
		System.out.println("Model2: \n"+model2.displayNodes());
		ArrayList<VNF> availableVNFs = new ArrayList<VNF>();
		
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

		int sfcLen = 0;
		SFCRequest sfc1,sfc2;
		Placement p;
		int[][] sol;
		boolean solved;
		for (int s = 0; s < numberOfSFCRequests; s++) {
			System.out.println("***********************************************Working on SFC_"+s+"***************************************************************");
			sfcLen = Utils.getSFCLen(availableVNFs.size());
			System.out.println("Random SFC-LEN="+sfcLen);
			sfc1 = SFCRequest.createSFCRequest(availableVNFs, sfcLen, (sfcLen-0.5)*model1.getAvgDelay());
			sfc2 = new SFCRequest(sfc1);
			sfc2.setAllVNFsUnshareable();

			try {
				//begin{SFC1 for Model1}
				System.out.println("/////////////////////////////////////////////////// Model_1: \n"+sfc1.toString()+"\n\n");
				p = new Placement(model1, sfc1);
				p.createObjective(UcCPU, UcRAM, UcBW);
				p.addConstraints();
				solved = p.optimize();
				if(solved) {
					model1Util[3][s]=1;
					sol=p.getSolution();
					sfcRequests_Sol1.put(sfc1, sol);
					if(p.NM.placeSFCRequest(p.sfcj, sol)) {
						System.out.println("SFC-request placed");
						System.out.println(p.NM.displayNodes());
						System.out.println(p.NM.displayLinks());
					}else
						System.err.println("Something wrong happend while deploying the SFC");
				}else {
					sfcRequests_Sol1.put(sfc1, null);
					model1Util[3][s]=0;
				}
				p.disposeModel();
				model1Util[0][s]=((double)(model1.getCpuCapacity()-model1.getCpuAvailable())/model1.getCpuCapacity())*100;
				model1Util[1][s]=((double)(model1.getBwCapacity()-model1.getBwAvailable())/model1.getBwCapacity())*100;
				model1Util[2][s]=sfc1.length();
				model1Util[4][s]=sfc1.numberOfShareableVNFs();
				//End{SFC1 for Model1}
				
				//begin{SFC2 for Model2}
				System.out.println("################################################### Model_2: \n"+sfc2.toString()+"\n\n");
				p = new Placement(model2, sfc2);
				p.createObjective(UcCPU, UcRAM, UcBW);
				p.addConstraints();
				solved = p.optimize();
				if(solved) {
					model2Util[3][s]=1;
					sol=p.getSolution();
					sfcRequests_Sol2.put(sfc2, sol);
					if(p.NM.placeSFCRequest(p.sfcj, sol)) {
						System.out.println("SFC-request placed");
						System.out.println(p.NM.displayNodes());
						System.out.println(p.NM.displayLinks());
					}else
						System.err.println("Something wrong happend while deploying the SFC");
				}else {
					sfcRequests_Sol2.put(sfc2, null);
					model2Util[3][s]=0;
				}
				p.disposeModel();
				model2Util[0][s]=((double)(model2.getCpuCapacity()-model2.getCpuAvailable())/model2.getCpuCapacity())*100;
				model2Util[1][s]=((double)(model2.getBwCapacity()-model2.getBwAvailable())/model2.getBwCapacity())*100;
				model2Util[2][s]=sfc2.length();
				model2Util[4][s]=sfc2.numberOfShareableVNFs();
				//End{SFC2 for Model2}
			}catch (GRBException e) {
				// TODO Auto-generated catch block

				System.out.println("Error code: " + e.getErrorCode() + ". " +
						e.getMessage());
				e.printStackTrace();
			}
			System.out.println("***********************************************Finished SFC_"+s+"*************************************************************** \n");
		}
//		System.out.println("model_1 Utilization: SFC_Len |   #shared-VNFS   | REQ_realized |       CPU        |       BW");
//		for (int i = 0; i < model1Util[0].length; i++) {
//	    System.out.println("After SFC_"+i+"           "+model1Util[2][i]+"    |  "+model1Util[4][i]+"             |  "+model1Util[3][i]+"      |  "+model1Util[0][i]+"  |  "+model1Util[1][i]);
//		}
//		
//		System.out.println("model_2 Utilization: SFC_Len |   #shared-VNFS   | REQ_realized |       CPU        |       BW");
//		for (int i = 0; i < model2Util[0].length; i++) {
//	    System.out.println("After SFC_"+i+"           "+model2Util[2][i]+"    |  "+model2Util[4][i]+"             |  "+model2Util[3][i]+"      |  "+model2Util[0][i]+"  |  "+model2Util[1][i]);
//		}
		System.out.println("model_1 Utilization");
		System.out.println("SFC,SFC_Len,#shared-VNFS,REQ_realized,CPU,BW");
		for (int i = 0; i < model1Util[0].length; i++) {
	    System.out.println("SFC_"+i+","+model1Util[2][i]+","+model1Util[4][i]+","+model1Util[3][i]+","+model1Util[0][i]+","+model1Util[1][i]);
		}
		
		System.out.println("model_2 Utilization");
		System.out.println("SFC,SFC_Len,#shared-VNFS,REQ_realized,CPU,BW");
		for (int i = 0; i < model2Util[0].length; i++) {
	    System.out.println("SFC_"+i+","+model2Util[2][i]+","+model2Util[4][i]+","+model2Util[3][i]+","+model2Util[0][i]+","+model2Util[1][i]);
		}
		
	}

}
