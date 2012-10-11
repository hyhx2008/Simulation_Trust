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

	private int[] lookupMessage;    // 用来存储由该节点发出的lookupmessage

	public int index = 0;           // 上面数组的最大下标

	public Node predecessor;

	public Node[] fingerTable;

	public Node[] successorList;

	public BigInteger chordId;
	//添加 修改
	public BigInteger maxChordId; 
	//end
	public int m;

	public int succLSize;

	public String prefix;

	private int next = 0;

	// campo x debug
	private int currentNode = 0;

	public int varSuccList = 0;    //successorList【varSuccList】

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
			if (target == ((ChordProtocol) node.getProtocol(pid)).chordId) {  // message 到达目标节点
				// mandare mess di tipo final
				t.send(node, n, new FinalMessage(message.getHopCounter()), pid);
			}
			if (target != ((ChordProtocol) node.getProtocol(pid)).chordId) {  // message 未达到目标节点
				// funzione lookup sulla fingertabable
				Node dest = find_successor(target);       //下一跳目标
				if (dest.isUp() == false) {               //如果找到的下一跳不在线，修正chord拓扑，直到找到活动的下一跳 (根据find_successor的实习，不可能是后继，只可能是finger中别的节点)
					do {
						varSuccList = 0;  //successorList【varSuccList】
						stabilize(node);  //稳定化         稳定化发生在所找到的下一跳不在线时， 稳定化和fixfinger一直进行到下一跳在线为止   不是 periodically????
						stabilizations++; //稳定化次数
						fixFingers();     //修正路由表
						dest = find_successor(target);
					} while (dest.isUp() == false);
				}
				/*原为
				if (dest.getID() == successorList[0].getID() //如果找到的下一跳节点为直接后继且超过target，证明最近的节点也超过了target，那么失败次数加一；???????????????target 绕圈怎么办?
						&& (target.compareTo(((ChordProtocol) dest
								.getProtocol(p.pid)).chordId) < 0)) {
					fails++;
				}*/
				if (dest.getID() == successorList[0].getID()           //为什么要有这个限制 ??
						&& (idInab(target,this.chordId, ((ChordProtocol) dest.getProtocol(p.pid)).chordId))) {
					fails++;
				}
				else {                                     //否则继续往后寻找
					t.send(message.getSender(), dest, message, pid);
				}
			}
		}
		if (event.getClass() == FinalMessage.class) {
			FinalMessage message = (FinalMessage) event;
			//原为
			//lookupMessage = new int[index + 1];                      //?wrong!!!??????????
			//lookupMessage[index] = message.getHopCounter();
			//修改
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
			Node node = ((ChordProtocol) successorList[0].getProtocol(p.pid)).predecessor;  // node为 myNode 的后继的前驱
			if (node != null) {
				if (this.chordId == ((ChordProtocol) node.getProtocol(p.pid)).chordId) //如果后继的前驱就是自己，返回
					return;
				BigInteger remoteID = ((ChordProtocol) node.getProtocol(p.pid)).chordId;
				if (idInab(remoteID, chordId, ((ChordProtocol) successorList[0]
						.getProtocol(p.pid)).chordId)) //如果后继的前驱在自己和后继之间则修改后继  ？？？？？绕圈了怎么办？？？？
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

	public void notify(Node node) throws Exception {  //通知后继修改前驱
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
						varSuccList = 0;         // successorList中没有一个活动的节点！！ 怎么办？？？？？？？？
					} else
						updateSuccessor();       // 递归 在successor list 中找到一个活动的节点作为successor放在successorList【0】
				}
				updateSuccessorList();           // 更新successorList， 将新直接后继successorList【0】中的successorList拷贝作为新successorList
				searching = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	//判断id是否在a，b之间，是返回true
	private boolean idInab(BigInteger id, BigInteger a, BigInteger b) { // 判断是否 a < id < b，考虑绕圈的情况
		/*原为
		if ((a.compareTo(id) == -1) && (id.compareTo(b) == -1)) {        //// 没有考虑绕圈的问题
			return true;
		}
		*/
		//修改
		if (a.compareTo(b)==-1)  // a < b 说明a与b都在同一侧，即a和b之间没有原点
		{
			if ((a.compareTo(id) == -1) && (id.compareTo(b) < 0)) {     
				return true;
			}
		}
		if (a.compareTo(b)==1)  // a > b 说明a与不在同一侧，即a和b之间有原点, a在原点左侧，b在原点右侧
		{
			if ((a.compareTo(id) == -1) || (id.compareTo(b) < 0)) {     // id > a 或者   id < b， 则 id位于a b之间.
				return true;
			}
		}
		
		//end
		return false;
	}

	public Node find_successor(BigInteger id) {  //注意该函数作用只是找到下一跳，与chord论文中的find_successor含义不太一样
		try {														
			if (successorList[0] == null || successorList[0].isUp() == false) { // 至少保证后继的正确性，chord才能正确工作
				updateSuccessor();
			}
			if (idInab(id, this.chordId, ((ChordProtocol) successorList[0]
					.getProtocol(p.pid)).chordId)) {
				return successorList[0];                          //当前节点为目标id的前驱，即目标id为当前节点的后继，返回后继  ????为什么要加这句,如果条件成立,必然查找失败?????????????????
			} else {
				Node tmp = closest_preceding_node(id);            //返回fingertable中距离目标id最近但不大于目标id的节点
				return tmp;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return successorList[0];
	}

	//返回 id 之前最近的finger
	private Node closest_preceding_node(BigInteger id) {
		for (int i = m; i > 0; i--) {  // 下标   m-1 downto 0
			try {
				if (fingerTable[i - 1] == null
						|| fingerTable[i - 1].isUp() == false) {   // 该finger不在线
					continue;    
				}
				BigInteger fingerId = ((ChordProtocol) (fingerTable[i - 1]
						.getProtocol(p.pid))).chordId;
				if ((idInab(fingerId, this.chordId, id))  // this.chordId < fingerId <= id
						|| (id.compareTo(fingerId) == 0)) {
					return fingerTable[i - 1];
				}
				
				/*原为
				if (fingerId.compareTo(this.chordId) == -1) { //fingerId绕了一圈的情况
					// sono nel caso in cui ho fatto un giro della rete
					// circolare
					if (idInab(id, fingerId, this.chordId)) {  // fingerId < id < this.chordID
						return fingerTable[i - 1];
					}
				}
				
				if ((id.compareTo(fingerId) == -1)
						&& (id.compareTo(this.chordId) == -1)) { //id绕了一圈的情况
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
				//修改
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

	public void fixFingers() {    // 所有结点修正其第next项finger
		if (next >= m - 1)
			next = 0;
		if (fingerTable[next] != null && fingerTable[next].isUp()) {  //该fingertable项正常
			next++;
			return;
		}//若next项不正常
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
		/*原为
		if (pot.compareTo(idLast) == 1) {  // pot > idLast   为什么需要这段代码？？？
			
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
		//修改
		if (pot.compareTo(maxChordId) >= 0)
		{
			pot = (pot.mod(maxChordId));
		}
		//end
		
		//修正路由表 递归
		do {
			//原为
			//fingerTable[next] = ((ChordProtocol) successorList[0].getProtocol(p.pid)).find_successor(pot);   //合适么??????这里的find_successor返回的结果只是距离pot最近的一个结点，而不是真正的finger
			//修改
			fingerTable[next] = findId(pot, 0, Network.size() - 1);
			//end
			pot = pot.subtract(BigInteger.ONE);    // 当上一句找到的结点不在线时，pot加1后再找一次。  这里将非常耗时！！！！！
			((ChordProtocol) successorList[0].getProtocol(p.pid)).fixFingers();  //
		} while (fingerTable[next] == null || fingerTable[next].isUp() == false);
		next++;
	}

	/**
	 */
	public void emptyLookupMessage() {
		index = 0;
		//原为
		//this.lookupMessage = new int[0];
		//修改
		lookupMessage = new int[1];
		lookupMessage[0] = 0;
		//end
	}
	
	//添加 修改
	public Node findId(BigInteger id, int nodeOne, int nodeTwo) {  //二分法查找，需保证chordId的顺序和index的顺序一致
		
//		if (Flag.sort_flag == 1)
		{
			NodeComparator nc = new NodeComparator(p.pid); 
			Network.sort(nc);        //根据chordID 排序，顺序与Network中node数组下标一致
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
					.getProtocol(p.pid)).chordId; // newId 下标为middle的node的chordId
			
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
