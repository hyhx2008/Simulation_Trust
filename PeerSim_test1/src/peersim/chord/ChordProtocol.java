/**
 * 
 */
package peersim.chord;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import java.math.*;

/**
 * @author Andrea
 * 
 */
public class ChordProtocol implements EDProtocol {

	private static final String PAR_TRANSPORT = "transport";

	private Parameters p;

	private int[] lookupMessage;    // �����洢�ɸýڵ㷢����lookupmessage

	public int index = 0;           // �������������±�

	public Node predecessor;

	public Node[] fingerTable;

	public Node[] successorList;

	public BigInteger chordId;
	//��� �޸�
	public BigInteger maxChordId; 
	//end
	public int m;

	public int succLSize;

	public String prefix;

	private int next = 0;

	// campo x debug
	private int currentNode = 0;

	public int varSuccList = 0;    //successorList��varSuccList��

	public int stabilizations = 0;

	public int fails = 0;

	/**
	 * 
	 */
	public ChordProtocol(String prefix) {
		this.prefix = prefix;
		lookupMessage = new int[1];
		lookupMessage[0] = 0;
		p = new Parameters();
		p.tid = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see peersim.edsim.EDProtocol#processEvent(peersim.core.Node, int,
	 *      java.lang.Object)
	 */
	public void processEvent(Node node, int pid, Object event) {
		// processare le richieste a seconda della routing table del nodo
		p.pid = pid;
		currentNode = node.getIndex();
		if (event.getClass() == LookUpMessage.class) {
			LookUpMessage message = (LookUpMessage) event;
			message.increaseHopCounter();
			BigInteger target = message.getTarget();
			Transport t = (Transport) node.getProtocol(p.tid);
			Node n = message.getSender();
			if (target == ((ChordProtocol) node.getProtocol(pid)).chordId) {  // message ����Ŀ��ڵ�
				// mandare mess di tipo final
				t.send(node, n, new FinalMessage(message.getHopCounter()), pid);
			}
			if (target != ((ChordProtocol) node.getProtocol(pid)).chordId) {  // message δ�ﵽĿ��ڵ�
				// funzione lookup sulla fingertabable
				Node dest = find_successor(target);       //��һ��Ŀ��
				if (dest.isUp() == false) {               //����ҵ�����һ�������ߣ�����chord���ˣ�ֱ���ҵ������һ�� (����find_successor��ʵϰ���������Ǻ�̣�ֻ������finger�б�Ľڵ�)
					do {
						varSuccList = 0;  //successorList��varSuccList��
						stabilize(node);  //�ȶ���         �ȶ������������ҵ�����һ��������ʱ�� �ȶ�����fixfingerһֱ���е���һ������Ϊֹ   ���� periodically????
						stabilizations++; //�ȶ�������
						fixFingers();     //����·�ɱ�
						dest = find_successor(target);
					} while (dest.isUp() == false);
				}
				/*ԭΪ
				if (dest.getID() == successorList[0].getID() //����ҵ�����һ���ڵ�Ϊֱ�Ӻ���ҳ���target��֤������Ľڵ�Ҳ������target����ôʧ�ܴ�����һ��???????????????target ��Ȧ��ô��?
						&& (target.compareTo(((ChordProtocol) dest
								.getProtocol(p.pid)).chordId) < 0)) {
					fails++;
				}*/
				if (dest.getID() == successorList[0].getID()           //ΪʲôҪ��������� ??
						&& (idInab(target,this.chordId, ((ChordProtocol) dest.getProtocol(p.pid)).chordId))) {
					fails++;
				}
				else {                                     //�����������Ѱ��
					t.send(message.getSender(), dest, message, pid);
				}
			}
		}
		if (event.getClass() == FinalMessage.class) {
			FinalMessage message = (FinalMessage) event;
			//ԭΪ
			//lookupMessage = new int[index + 1];                      //?wrong!!!??????????
			//lookupMessage[index] = message.getHopCounter();
			//�޸�
			int [] tmp = new int [index];
			System.arraycopy(lookupMessage, 0, tmp, 0, index);
			lookupMessage = new int[index + 1]; 
			System.arraycopy(tmp, 0, lookupMessage, 0, index);
			lookupMessage[index] = message.getHopCounter();
			//end
			if (message.getHopCounter() == 0)
				System.out.println("bbbb");
			index++;
		}
	}

	public Object clone() {
		ChordProtocol cp = new ChordProtocol(prefix);
		String val = BigInteger.ZERO.toString();
		cp.chordId = new BigInteger(val);
		cp.fingerTable = new Node[m];
		cp.successorList = new Node[succLSize];
		cp.currentNode = 0;
		return cp;
	}

	public int[] getLookupMessage() {
		return lookupMessage;
	}

	public void stabilize(Node myNode) {
		try {
			Node node = ((ChordProtocol) successorList[0].getProtocol(p.pid)).predecessor;  // nodeΪ myNode �ĺ�̵�ǰ��
			if (node != null) {
				if (this.chordId == ((ChordProtocol) node.getProtocol(p.pid)).chordId) //�����̵�ǰ�������Լ�������
					return;
				BigInteger remoteID = ((ChordProtocol) node.getProtocol(p.pid)).chordId;
				if (idInab(remoteID, chordId, ((ChordProtocol) successorList[0]
						.getProtocol(p.pid)).chordId)) //�����̵�ǰ�����Լ��ͺ��֮�����޸ĺ��  ������������Ȧ����ô�죿������
					successorList[0] = node;
				((ChordProtocol) successorList[0].getProtocol(p.pid))
						.notify(myNode);
			}
			updateSuccessorList();
		} catch (Exception e1) {
			e1.printStackTrace();
			updateSuccessor();
		}
	}

	private void updateSuccessorList() throws Exception {
		try {
			while (successorList[0] == null || successorList[0].isUp() == false) {
				updateSuccessor();
			}
			System.arraycopy(((ChordProtocol) successorList[0].getProtocol(p.pid)).successorList, 0, 
					successorList, 1,
					succLSize - 2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void notify(Node node) throws Exception {  //֪ͨ����޸�ǰ��
		BigInteger nodeId = ((ChordProtocol) node.getProtocol(p.pid)).chordId;
		if ((predecessor == null)
				|| (idInab(nodeId, ((ChordProtocol) predecessor
						.getProtocol(p.pid)).chordId, this.chordId))) {
			predecessor = node;
		}
	}

	private void updateSuccessor() {
		boolean searching = true;
		while (searching) {
			try {
				Node node = successorList[varSuccList];
				varSuccList++;
				successorList[0] = node;
				if (successorList[0] == null
						|| successorList[0].isUp() == false) {
					if (varSuccList >= succLSize - 1) {
						searching = false;
						varSuccList = 0;         // successorList��û��һ����Ľڵ㣡�� ��ô�죿��������������
					} else
						updateSuccessor();       // �ݹ� ��successor list ���ҵ�һ����Ľڵ���Ϊsuccessor����successorList��0��
				}
				updateSuccessorList();           // ����successorList�� ����ֱ�Ӻ��successorList��0���е�successorList������Ϊ��successorList
				searching = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	//�ж�id�Ƿ���a��b֮�䣬�Ƿ���true
	private boolean idInab(BigInteger id, BigInteger a, BigInteger b) { // �ж��Ƿ� a < id < b��������Ȧ�����
		/*ԭΪ
		if ((a.compareTo(id) == -1) && (id.compareTo(b) == -1)) {        //// û�п�����Ȧ������
			return true;
		}
		*/
		//�޸�
		if (a.compareTo(b)==-1)  // a < b ˵��a��b����ͬһ�࣬��a��b֮��û��ԭ��
		{
			if ((a.compareTo(id) == -1) && (id.compareTo(b) < 0)) {     
				return true;
			}
		}
		if (a.compareTo(b)==1)  // a > b ˵��a�벻��ͬһ�࣬��a��b֮����ԭ��, a��ԭ����࣬b��ԭ���Ҳ�
		{
			if ((a.compareTo(id) == -1) || (id.compareTo(b) < 0)) {     // id > a ����   id < b�� �� idλ��a b֮��.
				return true;
			}
		}
		
		//end
		return false;
	}

	public Node find_successor(BigInteger id) {  //ע��ú�������ֻ���ҵ���һ������chord�����е�find_successor���岻̫һ��
		try {														
			if (successorList[0] == null || successorList[0].isUp() == false) { // ���ٱ�֤��̵���ȷ�ԣ�chord������ȷ����
				updateSuccessor();
			}
			if (idInab(id, this.chordId, ((ChordProtocol) successorList[0]
					.getProtocol(p.pid)).chordId)) {
				return successorList[0];                          //��ǰ�ڵ�ΪĿ��id��ǰ������Ŀ��idΪ��ǰ�ڵ�ĺ�̣����غ��  ????ΪʲôҪ�����,�����������,��Ȼ����ʧ��?????????????????
			} else {
				Node tmp = closest_preceding_node(id);            //����fingertable�о���Ŀ��id�����������Ŀ��id�Ľڵ�
				return tmp;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return successorList[0];
	}

	//���� id ֮ǰ�����finger
	private Node closest_preceding_node(BigInteger id) {
		for (int i = m; i > 0; i--) {  // �±�   m-1 downto 0
			try {
				if (fingerTable[i - 1] == null
						|| fingerTable[i - 1].isUp() == false) {   // ��finger������
					continue;    
				}
				BigInteger fingerId = ((ChordProtocol) (fingerTable[i - 1]
						.getProtocol(p.pid))).chordId;
				if ((idInab(fingerId, this.chordId, id))  // this.chordId < fingerId <= id
						|| (id.compareTo(fingerId) == 0)) {
					return fingerTable[i - 1];
				}
				
				/*ԭΪ
				if (fingerId.compareTo(this.chordId) == -1) { //fingerId����һȦ�����
					// sono nel caso in cui ho fatto un giro della rete
					// circolare
					if (idInab(id, fingerId, this.chordId)) {  // fingerId < id < this.chordID
						return fingerTable[i - 1];
					}
				}
				
				if ((id.compareTo(fingerId) == -1)
						&& (id.compareTo(this.chordId) == -1)) { //id����һȦ�����
					if (i == 1)
						return successorList[0];
					BigInteger lowId = ((ChordProtocol) fingerTable[i - 2]
							.getProtocol(p.pid)).chordId;
					if (idInab(id, lowId, fingerId))
						return fingerTable[i - 2];
					else if (fingerId.compareTo(this.chordId) == -1)
						continue;
					else if (fingerId.compareTo(this.chordId) == 1)
						return fingerTable[i - 1];
				}
				*/
				//�޸�
				//end
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (fingerTable[m - 1] == null)
			return successorList[0];
		return successorList[0];
	}

	// debug function
	private void printFingers() {
		for (int i = fingerTable.length - 1; i > 0; i--) {
			if (fingerTable[i] == null) {
				System.out.println("Finger " + i + " is null");
				continue;
			}
			if ((((ChordProtocol) fingerTable[i].getProtocol(p.pid)).chordId)
					.compareTo(this.chordId) == 0)
				break;
			System.out
					.println("Finger["
							+ i
							+ "] = "
							+ fingerTable[i].getIndex()
							+ " chordId "
							+ ((ChordProtocol) fingerTable[i]
									.getProtocol(p.pid)).chordId);
		}
	}

	public void fixFingers() {    // ���н���������next��finger
		if (next >= m - 1)
			next = 0;
		if (fingerTable[next] != null && fingerTable[next].isUp()) {  //��fingertable������
			next++;
			return;
		}//��next�����
		BigInteger base;
		if (next == 0)
			base = BigInteger.ONE;
		else {
			base = BigInteger.valueOf(2);
			for (int exp = 1; exp < next; exp++) {
				base = base.multiply(BigInteger.valueOf(2));  //base = 2^next
			}
		}
		BigInteger pot = this.chordId.add(base);
		BigInteger idFirst = ((ChordProtocol) Network.get(0).getProtocol(p.pid)).chordId;
		BigInteger idLast = ((ChordProtocol) Network.get(Network.size() - 1)
				.getProtocol(p.pid)).chordId;
		/*ԭΪ
		if (pot.compareTo(idLast) == 1) {  // pot > idLast   Ϊʲô��Ҫ��δ��룿����
			
			pot = (pot.mod(idLast));         
			
			if (pot.compareTo(this.chordId) != -1) { //pot >= this.chordId
				next++;
				return;                               // ???????????
			}
			
			if (pot.compareTo(idFirst) == -1) { //pot < idFirst
				this.fingerTable[next] = Network.get(Network.size() - 1);  //?????????????
				next++;
				return;
			}
		}
		*/
		//�޸�
		if (pot.compareTo(maxChordId) >= 0)
		{
			pot = (pot.mod(maxChordId));
		}
		//end
		
		//����·�ɱ� �ݹ�
		do {
			//ԭΪ
			//fingerTable[next] = ((ChordProtocol) successorList[0].getProtocol(p.pid)).find_successor(pot);   //����ô??????�����find_successor���صĽ��ֻ�Ǿ���pot�����һ����㣬������������finger
			//�޸�
			fingerTable[next] = findId(pot, 0, Network.size() - 1);
			//end
			pot = pot.subtract(BigInteger.ONE);    // ����һ���ҵ��Ľ�㲻����ʱ��pot��1������һ�Ρ�  ���ｫ�ǳ���ʱ����������
			((ChordProtocol) successorList[0].getProtocol(p.pid)).fixFingers();  //
		} while (fingerTable[next] == null || fingerTable[next].isUp() == false);
		next++;
	}

	/**
	 */
	public void emptyLookupMessage() {
		index = 0;
		//ԭΪ
		//this.lookupMessage = new int[0];
		//�޸�
		lookupMessage = new int[1];
		lookupMessage[0] = 0;
		//end
	}
	
	//��� �޸�
	public Node findId(BigInteger id, int nodeOne, int nodeTwo) {  //���ַ����ң��豣֤chordId��˳���index��˳��һ��
		
//		if (Flag.sort_flag == 1)
		{
			NodeComparator nc = new NodeComparator(p.pid); 
			Network.sort(nc);        //����chordID ����˳����Network��node�����±�һ��
			Flag.reset_Sort_Flag();
		}
		
		if (nodeOne >= (nodeTwo - 1)) 
			return Network.get(nodeOne);
		
		int middle = (nodeOne + nodeTwo) / 2;
		
		if (((middle) >= Network.size() - 1))
			System.out.print("ERROR: Middle is bigger than Network.size");
		
		if (((middle) <= 0))
			return Network.get(0);
		
		try {
			BigInteger newId = ((ChordProtocol) ((Node) Network.get(middle))
					.getProtocol(p.pid)).chordId; // newId �±�Ϊmiddle��node��chordId
			
			BigInteger lowId;
			
			if (middle > 0)
				lowId = ((ChordProtocol) ((Node) Network.get(middle - 1))
						.getProtocol(p.pid)).chordId;
			else
				lowId = newId;
			
			BigInteger highId = ((ChordProtocol) ((Node) Network
					.get(middle + 1)).getProtocol(p.pid)).chordId;
			
			if (id.compareTo(newId) == 0
					|| ((id.compareTo(newId) == 1) && (id.compareTo(highId) == -1))) {
				return Network.get(middle);
			}
			
			if ((id.compareTo(newId) == -1) && (id.compareTo(lowId) == 1)) {
				if (middle > 0)
					return Network.get(middle - 1);
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
	//end
}
