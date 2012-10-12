/**
 * 
 */
package peersim.chord;

import peersim.core.*;
import peersim.config.Configuration;
import java.math.*;

/**
 * @author Andrea
 * 
 */
public class CreateNw implements Control {

	public static int pid = 0;

	private static final String PAR_IDLENGTH = "idLength";

	private static final String PAR_PROT = "protocol";

	private static final String PAR_SUCCSIZE = "succListSize";

	int idLength = 0;

	int successorLsize = 0;

	int fingSize = 0;
	//campo x debug
	boolean verbose = false;
	
	//添加 修改
	public static BigInteger maxChordId = BigInteger.ONE;
	//end
	
	/**
	 * 
	 */
	public CreateNw(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		idLength = Configuration.getInt(prefix + "." + PAR_IDLENGTH); 
		successorLsize = Configuration.getInt(prefix + "." + PAR_SUCCSIZE); 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see peersim.core.Control#execute()
	 */

	public boolean execute() {
		//添加 修改
		for (int exp = 0; exp < idLength; exp++) 
			maxChordId = maxChordId.multiply(BigInteger.valueOf(2));
		//end
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = (Node) Network.get(i);
			ChordProtocol cp = (ChordProtocol) node.getProtocol(pid);
			cp.m = idLength;
			cp.succLSize = successorLsize;
			cp.varSuccList = 0;
			cp.chordId = new BigInteger(idLength, CommonState.r);
			//添加 修改
			cp.maxChordId = maxChordId;
			//end
			cp.fingerTable = new Node[idLength];
			cp.successorList = new Node[successorLsize];   // 各个参数初始化
		}
		NodeComparator nc = new NodeComparator(pid); 
		Network.sort(nc);        //根据chordID 排序，顺序与Network中node数组下标一致
		
//test	
		/*	
		for (int i=0;i<Network.size();++i)
		{
			System.out.println(i+": "+((ChordProtocol)Network.get(i).getProtocol(pid)).chordId);
		}
		*/
		createFingerTable();
		return false;
	}

	public Node findId(BigInteger id, int nodeOne, int nodeTwo) {  //二分法查找，需保证chordId的顺序和index的顺序一致
		if (nodeOne >= (nodeTwo - 1)) 
			return Network.get(nodeOne);
		
		int middle = (nodeOne + nodeTwo) / 2;
		
		if (((middle) >= Network.size() - 1))
			System.out.print("ERROR: Middle is bigger than Network.size");
		
		if (((middle) <= 0))
			return Network.get(0);
		
		try {
			BigInteger newId = ((ChordProtocol) ((Node) Network.get(middle))
					.getProtocol(pid)).chordId; // newId 下标为middle的node的chordId
			
			BigInteger lowId;
			
			if (middle > 0)
				lowId = ((ChordProtocol) ((Node) Network.get(middle - 1))
						.getProtocol(pid)).chordId;
			else
				lowId = newId;
			
			BigInteger highId = ((ChordProtocol) ((Node) Network
					.get(middle + 1)).getProtocol(pid)).chordId;
			
			if (id.compareTo(newId) == 0
					|| ((id.compareTo(newId) == 1) && (id.compareTo(highId) == -1))) {
				//原为
				//return Network.get(middle);
				//修改
				return Network.get(middle+1);
				//end
			}
			
			if ((id.compareTo(newId) == -1) && (id.compareTo(lowId) == 1)) {
				if (middle > 0)
					//原为
					//return Network.get(middle - 1);
					//修改
					return Network.get(middle);
					//end
				else
					return Network.get(0);
			}
			
			if (id.compareTo(newId) == -1) {
				return findId(id, nodeOne, middle);
			} else if (id.compareTo(newId) == 1) {
				return findId(id, middle, nodeTwo);
			}
			
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void createFingerTable() {
		BigInteger idFirst = ((ChordProtocol) Network.get(0).getProtocol(pid)).chordId;
		BigInteger idLast = ((ChordProtocol) Network.get(Network.size() - 1)
				.getProtocol(pid)).chordId;
		for (int i = 0; i < Network.size(); i++) {
			Node node = (Node) Network.get(i);
			ChordProtocol cp = (ChordProtocol) node.getProtocol(pid);
			
			for (int a = 0; a < successorLsize; a++) {
				if (a + i < (Network.size() - 1))
					cp.successorList[a] = Network.get(a + i + 1);
				else
				{
					//原为
					//cp.successorList[a] = Network.get(a);
					//修改
					cp.successorList[a] = Network.get((a+i+1)%Network.size()); 
					//end
				}
			} //建立successor List
			
			if (i > 0)  // 建立  predecessor
				cp.predecessor = (Node) Network.get(i - 1);
			else
				cp.predecessor = (Node) Network.get(Network.size() - 1);
			
			int j = 0;
			for (j = 0; j < idLength; j++) {
				BigInteger base;
				if (j == 0)
					base = BigInteger.ONE;
				else {
					base = BigInteger.valueOf(2);
					for (int exp = 1; exp < j; exp++) {
						base = base.multiply(BigInteger.valueOf(2));
					}
				}
				BigInteger pot = cp.chordId.add(base);
				/*原为
				if (pot.compareTo(idLast) == 1) {           // pot > idLast
					pot = (pot.mod(idLast));
					if (pot.compareTo(cp.chordId) != -1) {  // pot >= cp.chordId 
						break;                              // ??????????????
					}
					if (pot.compareTo(idFirst) == -1) {     // pot < idFirst
						cp.fingerTable[j] = Network.get(Network.size() - 1);
						continue;
					}
				}
				*/
				
				//修改
				if (pot.compareTo(maxChordId) >= 0)
				{
					pot = (pot.mod(maxChordId));
				}
				//end
				
				cp.fingerTable[j] = findId(pot, 0, Network.size() - 1);  // 二分法查找
			}
		}
	}
}
